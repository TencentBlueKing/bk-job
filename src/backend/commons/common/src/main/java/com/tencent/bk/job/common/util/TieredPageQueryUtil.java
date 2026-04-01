/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.common.util;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 三区分页查询工具类。
 * <p>
 * 根据数据总量，将数据按区间划分为三个区域并行拉取（三区叠加，而非三选一）：
 * <ul>
 *   <li><b>浅层区</b>（Shallow，0 ~ shallowThreshold）：使用 offset 完全并行分页，每页一个任务并行拉取</li>
 *   <li><b>中层区</b>（Middle，shallowThreshold ~ deepThreshold）：段间并行 + 段内串行 keyset 分页。
 *       每个任务先通过 start + limit=1 获取该段的起始基准 ID，然后在段内通过 lastId 条件串行拉取</li>
 *   <li><b>深层区</b>（Deep，deepThreshold ~ 末尾）：单任务串行 keyset 分页</li>
 * </ul>
 * <p>
 * 当数据总量超过 deepThreshold 时，三个区域的任务会同时启动并行执行，最后合并结果。
 * <p>
 * 使用 {@link TieredPageQueryConfig} 配置各区域的阈值和并发参数。
 */
@Slf4j
public class TieredPageQueryUtil {

    /**
     * 三区分页查询全量数据。
     * <p>
     * 先通过 countQuery 获取总数，再根据总数将数据按区间划分为三个区域（三区叠加），
     * 各区域的任务同时启动并行执行，最后按顺序合并结果。
     * <ul>
     *   <li>totalCount ≤ shallowThreshold：只启动浅层区</li>
     *   <li>shallowThreshold < totalCount ≤ deepThreshold：同时启动浅层区 + 中层区</li>
     *   <li>totalCount > deepThreshold：同时启动浅层区 + 中层区 + 深层区</li>
     * </ul>
     *
     * @param config          三区分页查询配置
     * @param tracer          日志调用链tracer，用于在异步线程中传递trace上下文（可为null，为null时不传递trace）
     * @param countQuery      查询总数的函数
     * @param pageReqBuilder  根据 (start, limit) 构造分页请求的函数
     * @param pageQuery       执行分页查询的函数
     * @param resultExtractor 从查询结果中提取数据列表的函数
     * @param lastIdExtractor 从数据元素中提取 ID 的函数（用于 keyset 分页）
     * @param keysetReqBuilder 根据 (lastId, limit) 构造 keyset 分页请求的函数（lastId > 条件）
     * @param <Q>             分页请求类型
     * @param <R>             分页查询结果类型
     * @param <T>             数据元素类型
     * @return 全量数据列表
     */
    public static <Q, R, T> List<T> queryAll(
        TieredPageQueryConfig config,
        Tracer tracer,
        Supplier<Integer> countQuery,
        BiFunction<Integer, Integer, Q> pageReqBuilder,
        Function<Q, R> pageQuery,
        Function<R, List<T>> resultExtractor,
        Function<T, Long> lastIdExtractor,
        BiFunction<Long, Integer, Q> keysetReqBuilder
    ) {
        long startTime = System.currentTimeMillis();

        // 1. 查询总数
        int totalCount;
        try {
            totalCount = countQuery.get();
        } catch (Exception e) {
            log.error("Failed to query total count", e);
            return Collections.emptyList();
        }

        if (totalCount <= 0) {
            log.info("Total count is {}, return empty list", totalCount);
            return Collections.emptyList();
        }

        // 2. 根据总数确定需要激活的区域
        TieredPageQueryConfig.Tier maxTier = config.getTier(totalCount);
        log.info("TieredPageQuery start: totalCount={}, maxTier={}, config={}", totalCount, maxTier, config);

        // 在主线程捕获当前 Span，用于传递给异步线程
        Span parentSpan = (tracer != null) ? tracer.currentSpan() : null;

        int pageSize = config.getPageSize();
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);

        // 计算各区域的页范围（向上取整，确保阈值边界处的数据不会被遗漏）
        int shallowEndPage = Math.min((int) Math.ceil((double) config.getShallowThreshold() / pageSize), totalPages);
        int middleEndPage = Math.min((int) Math.ceil((double) config.getDeepThreshold() / pageSize), totalPages);

        // 3. 创建各区域的工作线程池，主线程直接向各线程池提交所有任务
        List<ExecutorService> executorsToShutdown = new ArrayList<>();
        // 收集所有 Future，用于主线程统一等待
        List<Future<?>> allFutures = new ArrayList<>();

        // 浅层区结果容器：按页号索引存储，保证顺序
        List<List<T>> shallowPageResults = new ArrayList<>(Collections.nCopies(shallowEndPage, null));
        // 中层区结果容器：按任务索引存储，保证顺序
        int middleTaskCount = 0;
        // 深层区结果容器
        List<T> deepResult = Collections.synchronizedList(new ArrayList<>());

        // ===== 浅层区：主线程直接提交每页任务到浅层区线程池 =====
        if (shallowEndPage > 0) {
            ExecutorService shallowExecutor = createExecutor(config.getShallowConcurrency(), "shallow-worker");
            executorsToShutdown.add(shallowExecutor);
            submitShallowTasks(
                config, tracer, parentSpan, shallowEndPage,
                pageReqBuilder, pageQuery, resultExtractor,
                shallowExecutor, shallowPageResults, allFutures);
        }

        // ===== 中层区：主线程直接提交每段任务到中层区线程池 =====
        if (maxTier == TieredPageQueryConfig.Tier.MIDDLE || maxTier == TieredPageQueryConfig.Tier.DEEP) {
            int middlePageCount = middleEndPage - shallowEndPage;
            if (middlePageCount > 0) {
                int middlePagesPerTask = config.getMiddlePagesPerTask();
                middleTaskCount = (int) Math.ceil((double) middlePageCount / middlePagesPerTask);
            }
        }
        List<List<T>> middleTaskResults = new ArrayList<>(Collections.nCopies(middleTaskCount, null));
        if (middleTaskCount > 0) {
            ExecutorService middleExecutor = createExecutor(config.getMiddleConcurrency(), "middle-worker");
            executorsToShutdown.add(middleExecutor);
            submitMiddleTasks(
                config, tracer, parentSpan, shallowEndPage, middleEndPage,
                pageReqBuilder, pageQuery, resultExtractor, lastIdExtractor, keysetReqBuilder,
                middleExecutor, middleTaskResults, allFutures);
        }

        // ===== 深层区：主线程直接提交一个串行任务到深层区线程池 =====
        if (maxTier == TieredPageQueryConfig.Tier.DEEP) {
            int deepStartPage = middleEndPage;
            if (deepStartPage < totalPages) {
                ExecutorService deepExecutor = createExecutor(1, "deep-worker");
                executorsToShutdown.add(deepExecutor);
                submitDeepTask(
                    config, tracer, parentSpan, deepStartPage,
                    pageReqBuilder, pageQuery, resultExtractor, lastIdExtractor, keysetReqBuilder,
                    deepExecutor, deepResult, allFutures);
            }
        }

        // 4. 主线程统一等待所有任务完成
        waitForFutures(allFutures, "AllTasks");

        // 5. 关闭所有线程池
        for (ExecutorService executor : executorsToShutdown) {
            shutdownExecutor(executor, "tiered");
        }

        // 6. 按区域顺序合并结果：浅层区 -> 中层区 -> 深层区
        List<T> result = new ArrayList<>(totalCount);
        // 浅层区：按页号顺序合并
        for (List<T> pageResult : shallowPageResults) {
            if (CollectionUtils.isNotEmpty(pageResult)) {
                result.addAll(pageResult);
            }
        }
        // 中层区：按任务顺序合并
        for (List<T> taskResult : middleTaskResults) {
            if (CollectionUtils.isNotEmpty(taskResult)) {
                result.addAll(taskResult);
            }
        }
        // 深层区
        result.addAll(deepResult);

        long costTime = System.currentTimeMillis() - startTime;
        int shallowSize = 0;
        for (List<T> pageResult : shallowPageResults) {
            if (pageResult != null) shallowSize += pageResult.size();
        }
        int middleSize = 0;
        for (List<T> taskResult : middleTaskResults) {
            if (taskResult != null) middleSize += taskResult.size();
        }
        log.info("TieredPageQuery finished: totalCount={}, maxTier={}, " +
                "shallowResults={}, middleResults={}, deepResults={}, totalResults={}, costTime={}ms",
            totalCount, maxTier,
            shallowSize, middleSize, deepResult.size(), result.size(), costTime);

        return result;
    }

    /**
     * 浅层区：主线程直接向线程池提交每页的查询任务。
     * <p>
     * 每页一个任务，所有任务并行执行。结果按页号索引存入 pageResults 数组，保证顺序。
     *
     * @param endPage      浅层区结束页号（不含），即拉取 [0, endPage) 范围的页
     * @param executor     浅层区工作线程池
     * @param pageResults  按页号索引存储每页结果的列表（由调用方创建，已用null填充到endPage大小）
     * @param allFutures   所有任务的 Future 列表（由调用方收集，用于统一等待）
     */
    private static <Q, R, T> void submitShallowTasks(
        TieredPageQueryConfig config,
        Tracer tracer,
        Span parentSpan,
        int endPage,
        BiFunction<Integer, Integer, Q> pageReqBuilder,
        Function<Q, R> pageQuery,
        Function<R, List<T>> resultExtractor,
        ExecutorService executor,
        List<List<T>> pageResults,
        List<Future<?>> allFutures
    ) {
        int pageSize = config.getPageSize();
        log.info("[ShallowTier] pages=[0, {}), concurrency={}", endPage, config.getShallowConcurrency());

        for (int page = 0; page < endPage; page++) {
            int start = page * pageSize;
            int pageIndex = page;
            Future<?> future = executor.submit(() -> {
                runWithTrace(tracer, parentSpan, "shallow-page-" + pageIndex, () -> {
                    try {
                        Q req = pageReqBuilder.apply(start, pageSize);
                        R result = pageQuery.apply(req);
                        List<T> data = resultExtractor.apply(result);
                        pageResults.set(pageIndex, data);
                    } catch (Exception e) {
                        log.error("[ShallowTier] Failed to query page, start={}, limit={}", start, pageSize, e);
                    }
                });
            });
            allFutures.add(future);
        }
    }

    /**
     * 中层区：主线程直接向线程池提交每段的查询任务。
     * <p>
     * 将 [startPage, endPage) 范围的页划分为多个任务（每个任务包含 middlePagesPerTask 页），
     * 每个任务先通过 start=任务第一页页号*pageSize, limit=1 获取该段的起始基准 ID，
     * 然后在段内通过 lastId > 条件串行 keyset 分页拉取数据。
     * <p>
     * 段间并行（不同段的任务在线程池中并行执行），段内串行（同一段内的 keyset 分页顺序执行）。
     *
     * @param startPage       中层区起始页号（含）
     * @param endPage         中层区结束页号（不含）
     * @param executor        中层区工作线程池
     * @param middleTaskResults 按任务索引存储每段结果的列表（由调用方创建，已用null填充到任务数大小）
     * @param allFutures      所有任务的 Future 列表（由调用方收集，用于统一等待）
     */
    private static <Q, R, T> void submitMiddleTasks(
        TieredPageQueryConfig config,
        Tracer tracer,
        Span parentSpan,
        int startPage,
        int endPage,
        BiFunction<Integer, Integer, Q> pageReqBuilder,
        Function<Q, R> pageQuery,
        Function<R, List<T>> resultExtractor,
        Function<T, Long> lastIdExtractor,
        BiFunction<Long, Integer, Q> keysetReqBuilder,
        ExecutorService executor,
        List<List<T>> middleTaskResults,
        List<Future<?>> allFutures
    ) {
        int pageSize = config.getPageSize();
        int middlePagesPerTask = config.getMiddlePagesPerTask();
        int middlePageCount = endPage - startPage;
        int middleTaskCount = (int) Math.ceil((double) middlePageCount / middlePagesPerTask);

        log.info("[MiddleTier] pages=[{}, {}), middleTaskCount={}, concurrency={}",
            startPage, endPage, middleTaskCount, config.getMiddleConcurrency());

        for (int taskIndex = 0; taskIndex < middleTaskCount; taskIndex++) {
            int taskStartPage = startPage + taskIndex * middlePagesPerTask;
            int taskEndPage = Math.min(taskStartPage + middlePagesPerTask, endPage);
            int taskPageCount = taskEndPage - taskStartPage;
            int tIdx = taskIndex;

            Future<?> future = executor.submit(() -> {
                runWithTrace(tracer, parentSpan, "middle-task-" + tIdx, () -> {
                    try {
                        List<T> taskData = new ArrayList<>(taskPageCount * pageSize);

                        // 步骤1：通过 start=taskStartPage*pageSize, limit=1 获取该段的起始基准 ID
                        int anchorStart = taskStartPage * pageSize;
                        Q anchorReq = pageReqBuilder.apply(anchorStart, 1);
                        R anchorResult = pageQuery.apply(anchorReq);
                        List<T> anchorData = resultExtractor.apply(anchorResult);

                        if (CollectionUtils.isEmpty(anchorData)) {
                            log.warn("[MiddleTier-Keyset] Anchor query returned empty, taskStartPage={}",
                                taskStartPage);
                            middleTaskResults.set(tIdx, taskData);
                            return;
                        }

                        // 获取基准元素的 ID，作为 keyset 分页的起始 lastId
                        // 注意：基准元素本身不包含在后续 keyset 查询中（因为 keyset 用的是 > lastId），
                        // 所以需要先把基准元素加入结果
                        T anchorElement = anchorData.get(0);
                        taskData.add(anchorElement);
                        long lastId = lastIdExtractor.apply(anchorElement);

                        // 步骤2：段内串行 keyset 分页拉取剩余数据
                        // 第一页已经拉了1条（基准元素），还需要拉 pageSize-1 条来补齐第一页，
                        // 之后每页拉 pageSize 条
                        int remainingForFirstPage = pageSize - 1;
                        if (remainingForFirstPage > 0) {
                            Q firstKeysetReq = keysetReqBuilder.apply(lastId, remainingForFirstPage);
                            R firstKeysetResult = pageQuery.apply(firstKeysetReq);
                            List<T> firstKeysetData = resultExtractor.apply(firstKeysetResult);
                            if (CollectionUtils.isNotEmpty(firstKeysetData)) {
                                taskData.addAll(firstKeysetData);
                                lastId = lastIdExtractor.apply(firstKeysetData.get(firstKeysetData.size() - 1));

                                if (firstKeysetData.size() < remainingForFirstPage) {
                                    // 数据已拉完
                                    middleTaskResults.set(tIdx, taskData);
                                    return;
                                }
                            } else {
                                // 没有更多数据
                                middleTaskResults.set(tIdx, taskData);
                                return;
                            }
                        }

                        // 拉取剩余页（从第2页开始到任务结束）
                        for (int p = 1; p < taskPageCount; p++) {
                            Q keysetReq = keysetReqBuilder.apply(lastId, pageSize);
                            R keysetResult = pageQuery.apply(keysetReq);
                            List<T> keysetData = resultExtractor.apply(keysetResult);

                            if (CollectionUtils.isEmpty(keysetData)) {
                                break;
                            }
                            taskData.addAll(keysetData);
                            lastId = lastIdExtractor.apply(keysetData.get(keysetData.size() - 1));

                            if (keysetData.size() < pageSize) {
                                break;
                            }
                        }

                        middleTaskResults.set(tIdx, taskData);
                    } catch (Exception e) {
                        log.error("[MiddleTier-Keyset] Failed to query task, taskIndex={}", tIdx, e);
                    }
                });
            });
            allFutures.add(future);
        }
    }

    /**
     * 深层区：主线程直接向线程池提交一个串行 keyset 分页任务。
     * <p>
     * 先通过 start=deepStartPage*pageSize, limit=1 获取深层区起始基准 ID，
     * 然后从该基准 ID 开始，逐页通过 lastId > 条件串行拉取所有剩余数据。
     *
     * @param deepStartPage 深层区起始页号（含），用于定位起始基准 ID
     * @param executor      深层区工作线程池（单线程）
     * @param deepResult    深层区结果容器（由调用方创建）
     * @param allFutures    所有任务的 Future 列表（由调用方收集，用于统一等待）
     */
    private static <Q, R, T> void submitDeepTask(
        TieredPageQueryConfig config,
        Tracer tracer,
        Span parentSpan,
        int deepStartPage,
        BiFunction<Integer, Integer, Q> pageReqBuilder,
        Function<Q, R> pageQuery,
        Function<R, List<T>> resultExtractor,
        Function<T, Long> lastIdExtractor,
        BiFunction<Long, Integer, Q> keysetReqBuilder,
        ExecutorService executor,
        List<T> deepResult,
        List<Future<?>> allFutures
    ) {
        int pageSize = config.getPageSize();
        log.info("[DeepTier] Using serial keyset pagination from page {}, pageSize={}", deepStartPage, pageSize);

        Future<?> future = executor.submit(() -> {
            runWithTrace(tracer, parentSpan, "deep-tier", () -> {
                try {
                    // 步骤1：通过 start=deepStartPage*pageSize, limit=1 获取深层区起始基准 ID
                    int anchorStart = deepStartPage * pageSize;
                    Q anchorReq = pageReqBuilder.apply(anchorStart, 1);
                    R anchorResult = pageQuery.apply(anchorReq);
                    List<T> anchorData = resultExtractor.apply(anchorResult);

                    if (CollectionUtils.isEmpty(anchorData)) {
                        log.warn("[DeepTier] Anchor query returned empty, deepStartPage={}", deepStartPage);
                        return;
                    }

                    T anchorElement = anchorData.get(0);
                    deepResult.add(anchorElement);
                    long lastId = lastIdExtractor.apply(anchorElement);

                    // 步骤2：逐页串行 keyset 拉取剩余数据
                    while (true) {
                        Q req = keysetReqBuilder.apply(lastId, pageSize);
                        R result = pageQuery.apply(req);
                        List<T> data = resultExtractor.apply(result);

                        if (CollectionUtils.isEmpty(data)) {
                            break;
                        }

                        deepResult.addAll(data);
                        lastId = lastIdExtractor.apply(data.get(data.size() - 1));

                        if (data.size() < pageSize) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.error("[DeepTier] Failed to query deep tier, deepStartPage={}", deepStartPage, e);
                }
            });
        });
        allFutures.add(future);
    }

    /**
     * 在异步线程中执行任务，并传递主线程的 trace 上下文。
     * <p>
     * 基于主线程捕获的 parentSpan 创建 child Span，使异步线程的日志与主线程保持同一条 trace 链路。
     *
     * @param tracer     日志调用链tracer，为null时直接执行任务不创建Span
     * @param parentSpan 主线程捕获的父Span
     * @param spanName   子Span名称
     * @param task       要执行的任务
     */
    private static void runWithTrace(Tracer tracer, Span parentSpan, String spanName, Runnable task) {
        if (tracer == null) {
            task.run();
            return;
        }
        Span childSpan = tracer.nextSpan(parentSpan).name(spanName);
        try (Tracer.SpanInScope ignored = tracer.withSpan(childSpan.start())) {
            task.run();
        } catch (Throwable e) {
            childSpan.error(e);
            throw e;
        } finally {
            childSpan.end();
        }
    }

    /**
     * 创建线程池
     */
    private static ExecutorService createExecutor(int concurrency, String name) {
        return new ThreadPoolExecutor(
            concurrency,
            concurrency,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(concurrency * 10),
            r -> {
                Thread t = new Thread(r, "tiered-page-query-" + name + "-" + System.nanoTime());
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 等待所有 Future 完成
     */
    private static void waitForFutures(List<Future<?>> futures, String tierName) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                log.error("[{}] Error waiting for page query task to complete", tierName, e);
            }
        }
    }

    /**
     * 关闭线程池
     */
    private static void shutdownExecutor(ExecutorService executor, String name) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
                log.warn("Thread pool [{}] did not terminate within 5 minutes, forcing shutdown", name);
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for thread pool [{}] to terminate", name, e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
