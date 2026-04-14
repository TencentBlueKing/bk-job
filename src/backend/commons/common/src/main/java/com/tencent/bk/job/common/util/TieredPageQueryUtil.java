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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.context.JobContext;
import com.tencent.bk.job.common.context.JobContextThreadLocal;
import com.tencent.bk.job.common.exception.TieredPageQueryException;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
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
        int totalCount = queryTotalCount(countQuery);
        if (totalCount <= 0) {
            return Collections.emptyList();
        }

        // 2. 根据总数确定需要激活的区域
        TieredPageQueryConfig.Tier maxTier = config.getTier(totalCount);
        log.info("TieredPageQuery start: totalCount={}, maxTier={}, config={}", totalCount, maxTier, config);

        // 在主线程捕获当前 Span 和 JobContext，用于传递给异步线程
        Span parentSpan = (tracer != null) ? tracer.currentSpan() : null;
        JobContext parentJobContext = JobContextThreadLocal.get();

        int pageSize = config.getPageSize();
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);

        // 计算各区域的页范围（向上取整，确保阈值边界处的数据不会被遗漏）
        int shallowEndPage = Math.min((int) Math.ceil((double) config.getShallowThreshold() / pageSize), totalPages);
        int middleEndPage = Math.min((int) Math.ceil((double) config.getDeepThreshold() / pageSize), totalPages);

        // 3. 创建各区域的工作线程池，主线程直接向各线程池提交所有任务
        List<ExecutorService> executorsToShutdown = new ArrayList<>();
        // 收集所有 Future，用于主线程统一等待
        List<Future<?>> allFutures = new ArrayList<>();
        // 各区域结果容器
        List<List<T>> shallowPageResults = new ArrayList<>(Collections.nCopies(shallowEndPage, null));
        int middleTaskCount = computeMiddleTaskCount(config, maxTier, shallowEndPage, middleEndPage);
        List<List<T>> middleTaskResults = new ArrayList<>(Collections.nCopies(middleTaskCount, null));
        List<T> deepResult = Collections.synchronizedList(new ArrayList<>());

        try {
            // 4. 提交所有区域的任务
            // ===== 浅层区：主线程直接提交每页任务到浅层区线程池 =====
            if (shallowEndPage > 0) {
                ExecutorService shallowExecutor = createExecutor(config.getShallowConcurrency(), "shallow-worker");
                executorsToShutdown.add(shallowExecutor);
                submitShallowTasks(
                    config, tracer, parentSpan, parentJobContext, shallowEndPage,
                    pageReqBuilder, pageQuery, resultExtractor,
                    shallowExecutor, shallowPageResults, allFutures);
            }

            // ===== 中层区：主线程直接提交每段任务到中层区线程池 =====
            if (middleTaskCount > 0) {
                ExecutorService middleExecutor = createExecutor(config.getMiddleConcurrency(), "middle-worker");
                executorsToShutdown.add(middleExecutor);
                submitMiddleTasks(
                    config, tracer, parentSpan, parentJobContext, shallowEndPage, middleEndPage,
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
                        config, tracer, parentSpan, parentJobContext, deepStartPage,
                        pageReqBuilder, pageQuery, resultExtractor, lastIdExtractor, keysetReqBuilder,
                        deepExecutor, deepResult, allFutures);
                }
            }

            // 5. 主线程统一等待所有任务完成
            waitForFutures(allFutures, "AllTasks");
        } finally {
            // 6. 在 finally 块中关闭所有线程池，防止异常时线程泄露
            for (ExecutorService executor : executorsToShutdown) {
                shutdownExecutor(executor, "tiered");
            }
        }

        // 7. 按区域顺序合并结果：浅层区 -> 中层区 -> 深层区
        List<T> result = mergeResults(totalCount, shallowPageResults, middleTaskResults, deepResult);

        // 8. 打印汇总日志
        logSummary(startTime, totalCount, maxTier, shallowPageResults, middleTaskResults, deepResult, result);

        return result;
    }

    /**
     * 查询总数
     */
    private static int queryTotalCount(Supplier<Integer> countQuery) {
        int totalCount;
        try {
            totalCount = countQuery.get();
        } catch (Exception e) {
            log.warn("Failed to query total count", e);
            throw new TieredPageQueryException("Failed to query total count", e, ErrorCode.INTERNAL_ERROR);
        }
        if (totalCount <= 0) {
            log.info("Total count is {}, return empty list", totalCount);
        }
        return totalCount;
    }

    /**
     * 计算中层区任务数
     */
    private static int computeMiddleTaskCount(TieredPageQueryConfig config,
                                              TieredPageQueryConfig.Tier maxTier,
                                              int shallowEndPage,
                                              int middleEndPage) {
        if (maxTier == TieredPageQueryConfig.Tier.MIDDLE || maxTier == TieredPageQueryConfig.Tier.DEEP) {
            int middlePageCount = middleEndPage - shallowEndPage;
            if (middlePageCount > 0) {
                return (int) Math.ceil((double) middlePageCount / config.getMiddlePagesPerTask());
            }
        }
        return 0;
    }

    /**
     * 按区域顺序合并结果：浅层区 -> 中层区 -> 深层区
     */
    private static <T> List<T> mergeResults(int totalCount,
                                            List<List<T>> shallowPageResults,
                                            List<List<T>> middleTaskResults,
                                            List<T> deepResult) {
        List<T> result = new ArrayList<>(totalCount);
        for (List<T> pageResult : shallowPageResults) {
            if (CollectionUtils.isNotEmpty(pageResult)) {
                result.addAll(pageResult);
            }
        }
        for (List<T> taskResult : middleTaskResults) {
            if (CollectionUtils.isNotEmpty(taskResult)) {
                result.addAll(taskResult);
            }
        }
        result.addAll(deepResult);
        return result;
    }

    /**
     * 打印汇总日志
     */
    private static <T> void logSummary(long startTime,
                                       int totalCount,
                                       TieredPageQueryConfig.Tier maxTier,
                                       List<List<T>> shallowPageResults,
                                       List<List<T>> middleTaskResults,
                                       List<T> deepResult,
                                       List<T> result) {
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
        JobContext parentJobContext,
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
                runWithContext(tracer, parentSpan, parentJobContext, "shallow-page-" + pageIndex, () -> {
                    Q req = pageReqBuilder.apply(start, pageSize);
                    R result = pageQuery.apply(req);
                    List<T> data = resultExtractor.apply(result);
                    pageResults.set(pageIndex, data);
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
        JobContext parentJobContext,
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

        log.info("[MiddleTier] startOffset={}, endOffset={}, middleTaskCount={}, concurrency={}",
            startPage * pageSize, endPage * pageSize, middleTaskCount, config.getMiddleConcurrency());

        for (int taskIndex = 0; taskIndex < middleTaskCount; taskIndex++) {
            int taskStartPage = startPage + taskIndex * middlePagesPerTask;
            int taskEndPage = Math.min(taskStartPage + middlePagesPerTask, endPage);
            int taskPageCount = taskEndPage - taskStartPage;
            int tIdx = taskIndex;

            Future<?> future = executor.submit(() -> {
                runWithContext(tracer, parentSpan, parentJobContext, "middle-task-" + tIdx, () -> {
                    List<T> taskData = executeMiddleTask(
                        taskStartPage, taskPageCount, pageSize,
                        pageReqBuilder, pageQuery, resultExtractor, lastIdExtractor, keysetReqBuilder);
                    middleTaskResults.set(tIdx, taskData);
                });
            });
            allFutures.add(future);
        }
    }

    /**
     * 执行中层区单个任务的核心逻辑。
     * <p>
     * 先通过 offset 定位该段的起始基准 ID，然后在段内通过 keyset 串行分页拉取数据。
     *
     * @param taskStartPage  任务起始页号
     * @param taskPageCount  任务包含的页数
     * @param pageSize       每页大小
     * @return 该任务拉取到的所有数据
     */
    private static <Q, R, T> List<T> executeMiddleTask(
        int taskStartPage,
        int taskPageCount,
        int pageSize,
        BiFunction<Integer, Integer, Q> pageReqBuilder,
        Function<Q, R> pageQuery,
        Function<R, List<T>> resultExtractor,
        Function<T, Long> lastIdExtractor,
        BiFunction<Long, Integer, Q> keysetReqBuilder
    ) {
        List<T> taskData = new ArrayList<>(taskPageCount * pageSize);

        // 步骤1：通过 start=taskStartPage*pageSize, limit=1 获取该段的起始基准 ID
        int anchorStart = taskStartPage * pageSize;
        Q anchorReq = pageReqBuilder.apply(anchorStart, 1);
        R anchorResult = pageQuery.apply(anchorReq);
        List<T> anchorData = resultExtractor.apply(anchorResult);

        if (CollectionUtils.isEmpty(anchorData)) {
            log.info("[MiddleTier-Keyset] Anchor query returned empty, anchorStart={}", anchorStart);
            return taskData;
        }

        // 获取基准元素的 ID，作为 keyset 分页的起始 lastId
        // 注意：基准元素本身不包含在后续 keyset 查询中（因为 keyset 用的是 > lastId），
        // 所以需要先把基准元素加入结果
        T anchorElement = anchorData.get(0);
        taskData.add(anchorElement);
        long lastId = lastIdExtractor.apply(anchorElement);

        // 步骤2：段内串行 keyset 分页拉取剩余数据
        // 第一页已经拉了1条（基准元素），还需要拉 pageSize-1 条来补齐第一页
        int remainingForFirstPage = pageSize - 1;
        if (remainingForFirstPage > 0) {
            Q firstKeysetReq = keysetReqBuilder.apply(lastId, remainingForFirstPage);
            R firstKeysetResult = pageQuery.apply(firstKeysetReq);
            List<T> firstKeysetData = resultExtractor.apply(firstKeysetResult);
            if (CollectionUtils.isNotEmpty(firstKeysetData)) {
                taskData.addAll(firstKeysetData);
                lastId = lastIdExtractor.apply(firstKeysetData.get(firstKeysetData.size() - 1));

                if (firstKeysetData.size() < remainingForFirstPage) {
                    return taskData;
                }
            } else {
                return taskData;
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

        return taskData;
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
        JobContext parentJobContext,
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
        int deepStartOffset = deepStartPage * pageSize;
        log.info("[DeepTier] Using serial keyset pagination from startOffset={}, pageSize={}", deepStartOffset, pageSize);

        Future<?> future = executor.submit(() -> {
            runWithContext(tracer, parentSpan, parentJobContext, "deep-tier", () -> {
                List<T> data = executeDeepTask(
                    deepStartPage, pageSize,
                    pageReqBuilder, pageQuery, resultExtractor, lastIdExtractor, keysetReqBuilder);
                deepResult.addAll(data);
            });
        });
        allFutures.add(future);
    }

    /**
     * 执行深层区串行 keyset 分页的核心逻辑。
     *
     * @param deepStartPage 深层区起始页号
     * @param pageSize      每页大小
     * @return 深层区拉取到的所有数据
     */
    private static <Q, R, T> List<T> executeDeepTask(
        int deepStartPage,
        int pageSize,
        BiFunction<Integer, Integer, Q> pageReqBuilder,
        Function<Q, R> pageQuery,
        Function<R, List<T>> resultExtractor,
        Function<T, Long> lastIdExtractor,
        BiFunction<Long, Integer, Q> keysetReqBuilder
    ) {
        List<T> result = new ArrayList<>();

        // 步骤1：通过 start=deepStartPage*pageSize, limit=1 获取深层区起始基准 ID
        int anchorStart = deepStartPage * pageSize;
        Q anchorReq = pageReqBuilder.apply(anchorStart, 1);
        R anchorResult = pageQuery.apply(anchorReq);
        List<T> anchorData = resultExtractor.apply(anchorResult);

        if (CollectionUtils.isEmpty(anchorData)) {
            log.info("[DeepTier] Anchor query returned empty, anchorStart={}", anchorStart);
            return result;
        }

        T anchorElement = anchorData.get(0);
        result.add(anchorElement);
        long lastId = lastIdExtractor.apply(anchorElement);

        // 步骤2：逐页串行 keyset 拉取剩余数据
        while (true) {
            Q req = keysetReqBuilder.apply(lastId, pageSize);
            R queryResult = pageQuery.apply(req);
            List<T> data = resultExtractor.apply(queryResult);

            if (CollectionUtils.isEmpty(data)) {
                break;
            }

            result.addAll(data);
            lastId = lastIdExtractor.apply(data.get(data.size() - 1));

            if (data.size() < pageSize) {
                break;
            }
        }

        return result;
    }

    /**
     * 在异步线程中执行任务，并传递主线程的 trace 上下文和 JobContext。
     * <p>
     * 基于主线程捕获的 parentSpan 创建 child Span，使异步线程的日志与主线程保持同一条 trace 链路。
     * 同时将主线程的 JobContext（包含 tenantId 等信息）传递到异步线程的 ThreadLocal 中，
     * 确保异步线程中能正确获取上下文信息（如 tenantId）。
     * <p>
     * 每个任务执行前 set，执行后 unset，避免线程池复用线程时上下文串扰。
     * <p>
     * 任务中抛出的异常会被捕获并向上传播（通过 Future.get() 抛出），不会被静默吞掉。
     *
     * @param tracer           日志调用链tracer，为null时不创建Span
     * @param parentSpan       主线程捕获的父Span
     * @param parentJobContext 主线程捕获的JobContext（包含tenantId等），为null时不传递
     * @param spanName         子Span名称
     * @param task             要执行的任务
     */
    private static void runWithContext(Tracer tracer,
                                       Span parentSpan,
                                       JobContext parentJobContext,
                                       String spanName,
                                       Runnable task) {
        // 设置 JobContext 到异步线程的 ThreadLocal
        if (parentJobContext != null) {
            JobContextThreadLocal.set(parentJobContext);
        }
        try {
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
        } finally {
            // 清理异步线程的 JobContext，防止线程池复用时上下文串扰
            JobContextThreadLocal.unset();
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
     * 等待所有 Future 完成，支持快速失败。
     * <p>
     * 采用非阻塞轮询方式：每隔 {@code POLL_INTERVAL_SECONDS} 秒扫描一遍所有 Future，
     * 一旦发现某个子任务抛出异常，立即取消所有剩余 Future 并抛出异常。
     * 线程池的关闭由外层 try-catch-finally 统一负责。
     * <p>
     * 相比阻塞式 future.get()，非阻塞轮询能更快地发现任意位置的子任务异常，
     * 不会因为前面的 Future 仍在执行而阻塞对后面已失败 Future 的检测。
     */
    private static final int POLL_INTERVAL_SECONDS = 2;

    private static void waitForFutures(List<Future<?>> futures,
                                       String tierName) {
        while (true) {
            boolean allDone = true;
            for (Future<?> future : futures) {
                if (future.isDone()) {
                    // 已完成的 Future，检查是否有异常
                    try {
                        future.get();
                    } catch (ExecutionException e) {
                        // 子任务异常，使用 warn 级别记录（上层业务方会打 error）
                        log.warn("[{}] Sub-task failed, cancelling remaining futures", tierName, e);
                        cancelAllFutures(futures);
                        throw new TieredPageQueryException(
                            "TieredPageQuery sub-task failed", e.getCause(), ErrorCode.INTERNAL_ERROR);
                    } catch (InterruptedException e) {
                        log.warn("[{}] Interrupted while checking future result", tierName, e);
                        Thread.currentThread().interrupt();
                        cancelAllFutures(futures);
                        throw new TieredPageQueryException(
                            "TieredPageQuery interrupted", e, ErrorCode.INTERNAL_ERROR);
                    }
                } else {
                    allDone = false;
                }
            }
            if (allDone) {
                return;
            }
            // 等待一个轮询间隔后再次扫描
            try {
                TimeUnit.SECONDS.sleep(POLL_INTERVAL_SECONDS);
            } catch (InterruptedException e) {
                log.warn("[{}] Interrupted while polling futures", tierName, e);
                Thread.currentThread().interrupt();
                cancelAllFutures(futures);
                throw new TieredPageQueryException(
                    "TieredPageQuery interrupted", e, ErrorCode.INTERNAL_ERROR);
            }
        }
    }

    /**
     * 取消所有未完成的 Future
     */
    private static void cancelAllFutures(List<Future<?>> futures) {
        for (Future<?> f : futures) {
            f.cancel(true);
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
