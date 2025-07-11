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

package com.tencent.bk.job.manage.service.impl.sync;

import com.tencent.bk.job.common.cc.model.result.ResourceEvent;
import com.tencent.bk.job.common.cc.model.result.ResourceWatchResult;
import com.tencent.bk.job.common.redis.util.HeartBeatRedisLock;
import com.tencent.bk.job.common.redis.util.LockResult;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.tracing.util.SpanUtil;
import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.metrics.CmdbEventSampler;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
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
@SuppressWarnings("InfiniteLoopStatement")
@Slf4j
public abstract class AbstractCmdbResourceEventWatcher<E> extends Thread {
    /**
     * 节点IP
     */
    private final String machineIp;
    private final RedisTemplate<String, String> redisTemplate;
    private final Tracer tracer;
    private final CmdbEventSampler cmdbEventSampler;
    /**
     * 事件监听任务分布式锁KEY
     */
    private final String redisLockKey;

    /**
     * 监听的资源名称
     */
    protected final String watcherResourceName;

    /**
     * 监听事件前是否已执行初始化操作
     */
    private boolean initedBeforeWatch = false;

    public AbstractCmdbResourceEventWatcher(String watcherResourceName,
                                            RedisTemplate<String, String> redisTemplate,
                                            Tracer tracer,
                                            CmdbEventSampler cmdbEventSampler) {
        this.machineIp = IpUtils.getFirstMachineIP();
        this.redisTemplate = redisTemplate;
        this.tracer = tracer;
        this.cmdbEventSampler = cmdbEventSampler;
        this.watcherResourceName = watcherResourceName;
        this.setName(watcherResourceName);
        this.redisLockKey = "watch-cmdb-" + this.watcherResourceName + "-lock";
    }

    @NewSpan
    @Override
    public final void run() {
        log.info("Watching {} event start", this.watcherResourceName);
        LockResult lockResult = null;
        while (true) {
            Span span = SpanUtil.buildNewSpan(this.tracer, this.watcherResourceName + "WatchOuterLoop");
            try (Tracer.SpanInScope ignored = this.tracer.withSpan(span.start())) {
                HeartBeatRedisLock redisLock = new HeartBeatRedisLock(redisTemplate, redisLockKey, machineIp);
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
                LockUtils.releaseDistributedLock(redisLockKey, machineIp);
                // 过5s后重新尝试监听事件
                ThreadUtils.sleep(5000);
            }
        }
    }

    private void watchAndHandleEvent() {
        log.info("Start watch {} resource at {},{}", watcherResourceName, TimeUtil.getCurrentTimeStr("HH:mm:ss"),
            System.currentTimeMillis());
        String cursor = null;
        while (isWatchingEnabled()) {
            Span span = SpanUtil.buildNewSpan(this.tracer, "watchAndHandle-" + this.watcherResourceName + "Events");
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
                ThreadUtils.sleep(1_000);
            } catch (Throwable t) {
                span.error(t);
                log.error("EventWatch thread fail", t);
                // 如果处理事件过程中碰到异常，等待5s重试
                ThreadUtils.sleep(5_000);
            } finally {
                span.end();
            }
        }
        // 如果事件开关为disabled，间隔30s重新判断是否开启
        ThreadUtils.sleep(30_000L);
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
                handleEvent(event);
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
     * 事件监听开关
     */
    protected boolean isWatchingEnabled() {
        return true;
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
