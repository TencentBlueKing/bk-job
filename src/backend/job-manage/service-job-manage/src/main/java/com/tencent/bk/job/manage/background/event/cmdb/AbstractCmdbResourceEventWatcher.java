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

package com.tencent.bk.job.manage.background.event.cmdb;

import com.tencent.bk.job.common.cc.model.result.ResourceEvent;
import com.tencent.bk.job.common.cc.model.result.ResourceWatchResult;
import com.tencent.bk.job.common.redis.util.HeartBeatRedisLock;
import com.tencent.bk.job.common.redis.util.LockResult;
import com.tencent.bk.job.common.tenant.TenantService;
import com.tencent.bk.job.common.tracing.util.SpanUtil;
import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.background.ha.AbstractBackGroundTask;
import com.tencent.bk.job.manage.metrics.CmdbEventSampler;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

/**
 * cmdb 事件监听
 *
 * @param <E> cmdb事件
 */
@Slf4j
public abstract class AbstractCmdbResourceEventWatcher<E> extends AbstractBackGroundTask {

    /**
     * 单个Watcher自身的线程资源成本（不含事件处理线程）
     */
    public static final int SINGLE_WATCHER_THREAD_RESOURCE_COST = 1;

    protected final String tenantId;
    private final TenantService tenantService;
    private final Tracer tracer;
    private final CmdbEventSampler cmdbEventSampler;

    /**
     * 监听的资源名称
     */
    protected final String watcherResourceName;
    private final HeartBeatRedisLock redisLock;

    /**
     * 监听事件前是否已执行初始化操作
     */
    private boolean initedBeforeWatch = false;
    protected volatile boolean active = true;
    protected volatile boolean finished = false;

    public AbstractCmdbResourceEventWatcher(String tenantId,
                                            String watcherResourceName,
                                            RedisTemplate<String, String> redisTemplate,
                                            TenantService tenantService,
                                            Tracer tracer,
                                            CmdbEventSampler cmdbEventSampler) {
        this.tenantId = tenantId;
        this.tenantService = tenantService;
        this.tracer = tracer;
        this.cmdbEventSampler = cmdbEventSampler;
        this.watcherResourceName = watcherResourceName;
        this.setName(getUniqueCode());
        this.redisLock = new HeartBeatRedisLock(redisTemplate, getUniqueCode(), IpUtils.getFirstMachineIP());
    }

    /**
     * 检查当前监听器是否处于活跃状态
     *
     * @return 布尔值
     */
    private boolean checkActive() {
        if (!checkTenantSafely()) {
            log.info("tenant {} is not enabled, stop watch {}", tenantId, watcherResourceName);
            active = false;
        }
        return active;
    }

    /**
     * 安全地检查当前租户是否启用，接口调用异常视为启用，不停止事件监听线程
     *
     * @return 布尔值
     */
    private boolean checkTenantSafely() {
        try {
            return tenantService.isTenantEnabledPreferCache(tenantId);
        } catch (Throwable t) {
            String message = MessageFormatter.format(
                "Fail to check tenant({}) enabled status, regard as true",
                tenantId
            ).getMessage();
            log.warn(message, t);
            return true;
        }
    }

    @NewSpan
    @Override
    public final void run() {
        try {
            doRun();
        } catch (Throwable t) {
            log.error("Exception occurred when running", t);
        } finally {
            onFinish();
        }
    }

    /**
     * 判断是否有其他实例在运行
     *
     * @return 是否有其他实例在运行
     */
    public boolean hasRunningInstance() {
        String lockKeyValue = redisLock.peekLockKeyValue();
        return StringUtils.isNotBlank(lockKeyValue);
    }

    private void doRun() {
        if (hasRunningInstance()) {
            log.info(
                "{} already running on {}, ignore",
                getUniqueCode(),
                redisLock.peekLockKeyValue()
            );
            return;
        }
        log.info("{} start", getUniqueCode());
        LockResult lockResult = null;
        while (checkActive()) {
            Span span = SpanUtil.buildNewSpan(this.tracer, this.watcherResourceName + "WatchOuterLoop");
            try (Tracer.SpanInScope ignored = this.tracer.withSpan(span.start())) {
                lockResult = redisLock.lock();
                if (!lockResult.isLockGotten()) {
                    // 5s之后重试
                    ThreadUtils.sleep(5_000);
                    continue;
                }

                // 事件处理前的初始化
                tryToInitBeforeWatch();

                // 监听并处理事件
                watchAndHandleEvent();
            } catch (Throwable t) {
                log.error("Watching event caught exception", t);
                span.error(t);
            } finally {
                span.end();
                if (lockResult != null) {
                    lockResult.tryToRelease();
                }
                if (checkActive()) {
                    // 过5s后重新尝试监听事件
                    ThreadUtils.sleep(5000);
                }
            }
        }
    }

    @SuppressWarnings("BusyWait")
    private void watchAndHandleEvent() {
        log.info(
            "Start watch {} resource at {},{}",
            watcherResourceName,
            TimeUtil.getCurrentTimeStr("HH:mm:ss"),
            System.currentTimeMillis()
        );
        String cursor = null;
        while (checkActive() && isWatchingEnabled()) {
            Span span = SpanUtil.buildNewSpan(
                this.tracer,
                "watchAndHandle-" + this.watcherResourceName + "Events"
            );
            try (Tracer.SpanInScope ignored = this.tracer.withSpan(span.start())) {
                ResourceWatchResult<E> watchResult;
                if (cursor == null) {
                    // 从10分钟前开始watch
                    long startTime = System.currentTimeMillis() / 1000 - 10 * 60;
                    log.info("Start watch {} from startTime:{}", this.watcherResourceName,
                        TimeUtil.formatTime(startTime * 1000));
                    watchResult = fetchEventsByStartTime(startTime);
                } else {
                    watchResult = fetchEventsByCursor(cursor);
                }
                log.info("WatchResult[{}]: {}", this.watcherResourceName, JsonUtils.toJson(watchResult));
                cursor = handleWatchResult(watchResult, cursor);
                // 1s/watch一次
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                log.info("EventWatch thread interrupted");
            } catch (Throwable t) {
                span.error(t);
                log.error("EventWatch thread fail", t);
                // 如果处理事件过程中碰到异常，但监听未被主动关闭，等待5s重试
                if (checkActive()) {
                    ThreadUtils.sleep(5_000);
                }
            } finally {
                span.end();
            }
        }
        if (checkActive()) {
            // 如果事件开关为disabled，但监听未被主动关闭，间隔30s重新判断是否开启
            ThreadUtils.sleep(30_000L);
        }
    }

    private String handleWatchResult(ResourceWatchResult<E> watchResult,
                                     String startCursor) {
        String latestCursor = startCursor;
        boolean isWatched = watchResult.getWatched();
        List<ResourceEvent<E>> events = watchResult.getEvents();
        if (CollectionUtils.isEmpty(events)) {
            if (isWatched) {
                log.info("Handle {} watch events, events is empty", this.watcherResourceName);
            } else {
                log.warn("CMDB event error:no refresh event data when watched==false");
            }
            return latestCursor;
        }

        if (isWatched) {
            // 记录事件数量，暴露出Metrics指标
            tryToRecordEvents(events.size());
            // 解析事件，进行处理
            for (ResourceEvent<E> event : events) {
                checkAndHandleEvent(event);
            }
            latestCursor = events.get(events.size() - 1).getCursor();
            log.info("Handle {} watch events successfully! events.size: {}", this.watcherResourceName, events.size());
        } else {
            // 只有一个无实际意义的事件，用于换取bk_cursor
            latestCursor = events.get(0).getCursor();
        }
        log.info("Refresh {} watch cursor: {}", this.watcherResourceName, latestCursor);
        return latestCursor;
    }

    private void tryToRecordEvents(int eventNum) {
        try {
            cmdbEventSampler.recordWatchedEvents(eventNum, getEventMetricTags());
        } catch (Throwable t) {
            log.warn("Fail to recordEvents", t);
        }
    }

    private void tryToInitBeforeWatch() {
        if (!initedBeforeWatch) {
            initBeforeWatch();
            initedBeforeWatch = true;
        }
    }

    /**
     * 线程启动后的初始化操作
     */
    protected void initBeforeWatch() {
    }

    /**
     * 检查事件数据有效性并处理事件
     *
     * @param event 事件数据
     */
    private void checkAndHandleEvent(ResourceEvent<E> event) {
        log.info("Handle {} event: {}", watcherResourceName, event);
        String eventType = event.getEventType();
        if (StringUtils.isBlank(eventType)) {
            log.warn("Event type is blank, ignore");
            return;
        }
        E eventDetail = event.getDetail();
        if (eventDetail == null) {
            log.warn("Event detail is null, ignore");
            return;
        }
        handleEvent(event);
    }

    /**
     * 事件监听开关
     */
    protected boolean isWatchingEnabled() {
        return true;
    }

    private void onFinish() {
        deregister();
        this.finished = true;
    }

    /**
     * 优雅关闭
     */
    public void shutdownGracefully() {
        this.active = false;
        waitUntilFinish();
    }

    private void waitUntilFinish() {
        int waitMills = 0;
        while (!finished) {
            ThreadUtils.sleep(10);
            waitMills += 10;
            if (waitMills % 5000 == 0) {
                log.info("waited {}s to shutdown, still running", waitMills / 1000);
            }
        }
        log.info("waited {}s to shutdown finished", waitMills / 1000.0);
    }

    /**
     * 根据事件游标或者时间获取事件
     *
     * @param startCursor 事件起始游标
     * @return 监听事件结果
     */
    protected abstract ResourceWatchResult<E> fetchEventsByCursor(String startCursor);

    /**
     * 根据事件游标或者时间获取事件
     *
     * @param startTime 事件起始时间
     * @return 监听事件结果
     */
    protected abstract ResourceWatchResult<E> fetchEventsByStartTime(Long startTime);

    /**
     * 处理cmdb事件
     *
     * @param event cmdb事件
     */
    public abstract void handleEvent(ResourceEvent<E> event);

    /**
     * 获取CMDB事件指标标签
     *
     * @return 事件指标标签
     */
    protected abstract Tags getEventMetricTags();
}
