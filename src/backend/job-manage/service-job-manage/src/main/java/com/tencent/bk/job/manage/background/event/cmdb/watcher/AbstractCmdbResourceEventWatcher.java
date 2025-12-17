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

package com.tencent.bk.job.manage.background.event.cmdb.watcher;

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
import com.tencent.bk.job.manage.background.event.cmdb.CmdbEventCursorManager;
import com.tencent.bk.job.manage.background.event.cmdb.CmdbEventWatcher;
import com.tencent.bk.job.manage.background.event.cmdb.consts.EventConsts;
import com.tencent.bk.job.manage.background.event.cmdb.handler.CmdbEventHandler;
import com.tencent.bk.job.manage.background.ha.AbstractBackGroundTask;
import com.tencent.bk.job.manage.metrics.CmdbEventSampler;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

/**
 * 抽象的CMDB资源事件监听器，封装事件监听核心循环等公共逻辑
 *
 * @param <E> 具体某种类型资源的CMDB事件详情实体类
 */
@Slf4j
public abstract class AbstractCmdbResourceEventWatcher<E> extends AbstractBackGroundTask implements CmdbEventWatcher {

    /**
     * 单个Watcher自身使用的线程数量（不含事件处理线程）
     */
    public static final int SINGLE_WATCHER_THREAD_NUM = 1;
    /**
     * 日志调用链tracer
     */
    private final Tracer tracer;
    /**
     * CMDB事件指标数据采样器
     */
    private final CmdbEventSampler cmdbEventSampler;
    /**
     * CMDB事件游标管理器
     */
    private final CmdbEventCursorManager cmdbEventCursorManager;
    /**
     * CMDB事件处理器
     */
    private final CmdbEventHandler<E> cmdbEventHandler;
    /**
     * 租户服务
     */
    private final TenantService tenantService;
    /**
     * 租户ID
     */
    protected final String tenantId;
    /**
     * 监听的资源名称
     */
    protected final String watcherResourceName;
    /**
     * Redis心跳锁
     */
    private final HeartBeatRedisLock redisLock;

    /**
     * 监听器是否开启，用于上层服务动态控制监听器启停
     */
    protected volatile boolean enabled = true;
    /**
     * 监听器是否活跃，关闭监听器时该值被置为false
     */
    protected volatile boolean active = true;
    /**
     * 监听器是否优雅关闭完成
     */
    protected volatile boolean finished = false;

    public AbstractCmdbResourceEventWatcher(String tenantId,
                                            String watcherResourceName,
                                            RedisTemplate<String, String> redisTemplate,
                                            TenantService tenantService,
                                            Tracer tracer,
                                            CmdbEventSampler cmdbEventSampler,
                                            CmdbEventCursorManager cmdbEventCursorManager,
                                            CmdbEventHandler<E> cmdbEventHandler) {
        this.tenantId = tenantId;
        this.tenantService = tenantService;
        this.tracer = tracer;
        this.cmdbEventSampler = cmdbEventSampler;
        this.cmdbEventCursorManager = cmdbEventCursorManager;
        this.cmdbEventHandler = cmdbEventHandler;
        this.watcherResourceName = watcherResourceName;
        this.setName(getUniqueCode());
        this.redisLock = new HeartBeatRedisLock(redisTemplate, getUniqueCode(), IpUtils.getFirstMachineIP());
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public final void run() {
        try {
            mainActiveStatusCheckLoop();
        } catch (Throwable t) {
            log.error("Exception occurred during mainActiveStatusCheckLoop", t);
        } finally {
            onFinish();
        }
    }

    /**
     * 外层主循环，每间隔一定时间检查监听器活跃状态，只要状态活跃就尝试进入内层循环启动事件监听处理；
     * 内层循环可在用户通过OP接口暂时关闭事件监听或抛出异常时退出，但外层循环只在进程结束主动关闭时才退出，确保事件监听机制可被动态启停。
     */
    private void mainActiveStatusCheckLoop() {
        if (hasRunningInstance()) {
            // 其他实例上已经有当前监听目标的实例在运行，直接退出
            log.info("{} already running on {}, ignore", getUniqueCode(), redisLock.peekLockKeyValue());
            return;
        }
        log.info("{} start", getUniqueCode());
        boolean needToContinue = true;
        while (checkActive() && needToContinue) {
            needToContinue = startNewWatchLoopAndHandleResult();
        }
    }

    /**
     * 开启一次新的事件监听循环并处理结果（记录错误信息并等待一段时间，便于外层循环开启下一次）
     *
     * @return 是否需要继续开启监听
     */
    private boolean startNewWatchLoopAndHandleResult() {
        LockResult lockResult = null;
        Span span = SpanUtil.buildNewSpan(this.tracer, this.watcherResourceName + "WatchOuterLoop");
        try (Tracer.SpanInScope ignored = this.tracer.withSpan(span.start())) {
            // 尝试获取Redis心跳锁，确保当前实例正在监听目标租户事件的分布式唯一性
            lockResult = redisLock.lock();
            if (!lockResult.isLockGotten()) {
                // 其他实例上已经有当前监听目标的实例在运行，无需再重试
                return false;
            }

            // 监听并处理事件
            watchAndHandleEvent();

            // 监听被OP接口暂停，后续可能再开启，需要延时后继续检测
            return true;
        } catch (InterruptedException e) {
            log.info("EventWatch thread interrupted");
            return false;
        } catch (Throwable t) {
            log.error("Watching event caught exception", t);
            span.error(t);
            // 监听过程中发生异常，需要延时后继续检测
            return true;
        } finally {
            span.end();
            if (lockResult != null) {
                lockResult.tryToRelease();
            }
            if (checkActive()) {
                // 等待检测间隔后外层循环继续检测并决定是否继续开启监听
                ThreadUtils.sleep(EventConsts.EVENT_ENABLED_CHECK_INTERVAL_MILLIS);
            }
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

    /**
     * 事件监听处理内层循环，从上一次监听结束的游标位置开始监听并处理事件
     *
     * @throws InterruptedException Sleep被中断时抛出
     */
    @SuppressWarnings("BusyWait")
    private void watchAndHandleEvent() throws InterruptedException {
        log.info("Start watch {} resource", watcherResourceName);
        // 加载上一次监听结束时的事件游标
        String cursor = cmdbEventCursorManager.tryToLoadLatestCursor(tenantId, watcherResourceName);
        while (checkActive() && isWatchingEnabled()) {
            ResourceWatchResult<E> watchResult = fetchEventsFromCmdb(cursor);
            log.info("WatchResult[{}]: {}", this.watcherResourceName, JsonUtils.toJson(watchResult));
            cursor = handleWatchResult(watchResult, cursor);
            // 保存游标用于下次监听时从断点恢复
            cmdbEventCursorManager.tryToSaveLatestCursor(tenantId, watcherResourceName, cursor);
            if (watchResult.getWatched() == null || !watchResult.getWatched()) {
                // 如果没有监听到事件，1s/watch一次，保证性能的同时避免CMDB接口立即返回导致打印大量日志
                Thread.sleep(EventConsts.NO_EVENT_WATCHED_WAIT_INTERVAL_MILLIS);
            }
        }
    }

    /**
     * 从CMDB接口监听事件
     *
     * @param cursor 事件游标
     * @return 监听结果
     */
    private ResourceWatchResult<E> fetchEventsFromCmdb(String cursor) {
        if (cursor != null) {
            return fetchEventsByCursor(cursor);
        }
        // 游标为空，则从回溯时间前开始监听
        long startTimeMillis = System.currentTimeMillis() - EventConsts.EVENT_BACK_TRACK_TIME_MILLIS;
        log.info("Start watch {} from startTime:{}", this.watcherResourceName, TimeUtil.formatTime(startTimeMillis));
        return fetchEventsByStartTime(startTimeMillis / 1000);
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
                log.warn("CMDB event error, no refresh event data when watched is false");
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
        } else {
            // 只有一个无实际意义的事件，用于换取bk_cursor
            latestCursor = events.get(0).getCursor();
        }
        log.info("Refresh {} watch cursor: {}", this.watcherResourceName, latestCursor);
        return latestCursor;
    }

    /**
     * 尝试记录事件数量指标数据，异常时打印日志，不影响后续事件处理
     *
     * @param eventNum 事件数量
     */
    private void tryToRecordEvents(int eventNum) {
        try {
            cmdbEventSampler.recordWatchedEvents(eventNum, getEventMetricTags());
        } catch (Throwable t) {
            log.warn("Fail to recordEvents", t);
        }
    }

    /**
     * 检查事件数据有效性并使用事件处理器处理事件
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
        try {
            cmdbEventHandler.handleEvent(event);
        } catch (Throwable t) {
            String message = MessageFormatter.format(
                "Fail to handle {} event",
                watcherResourceName
            ).getMessage();
            log.error(message, t);
        }
    }

    /**
     * 事件监听开关
     */
    protected boolean isWatchingEnabled() {
        return enabled;
    }

    @Override
    public void setWatchEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 监听器正常关闭时的动作
     */
    private void onFinish() {
        // 取消后台任务注册
        deregister();
        // 关闭事件处理器
        cmdbEventHandler.close();
        // 状态切换
        this.finished = true;
    }

    /**
     * 优雅关闭
     */
    @Override
    public void shutdownGracefully() {
        this.active = false;
        waitUntilFinish();
    }

    /**
     * 阻塞等待，直到关闭完成
     */
    private void waitUntilFinish() {
        int waitMills = 0;
        while (!finished) {
            ThreadUtils.sleep(10);
            waitMills += 10;
            // 每5s打印一次等待时间
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
     * 获取CMDB事件指标标签
     *
     * @return 事件指标标签
     */
    protected abstract Tags getEventMetricTags();
}
