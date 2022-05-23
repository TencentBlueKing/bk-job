package com.tencent.bk.job.manage.service.impl.sync;

import brave.Span;
import brave.Tracing;
import com.tencent.bk.job.common.cc.model.result.ResourceEvent;
import com.tencent.bk.job.common.cc.model.result.ResourceWatchResult;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.redis.util.RedisKeyHeartBeatThread;
import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

/**
 * cmdb 事件监听
 *
 * @param <E> cmdb事件
 */
@Slf4j
public abstract class AbstractCmdbResourceEventWatcher<E> extends Thread {
    /**
     * 节点IP
     */
    private final String machineIp;
    private final RedisTemplate<String, String> redisTemplate;
    private final Tracing tracing;
    /**
     * 事件监听任务分布式锁KEY
     */
    private final String redisLockKey;

    /**
     * 监听的资源名称
     */
    protected final String watcherResourceName;

    public AbstractCmdbResourceEventWatcher(String watcherResourceName,
                                            RedisTemplate<String, String> redisTemplate,
                                            Tracing tracing) {
        this.machineIp = IpUtils.getFirstMachineIP();
        this.redisTemplate = redisTemplate;
        this.tracing = tracing;
        this.watcherResourceName = watcherResourceName;
        this.setName(watcherResourceName);
        this.redisLockKey = "watch-cmdb-" + this.watcherResourceName + "-lock";
    }

    @Override
    public final void run() {
        log.info("Watching {} event start", this.watcherResourceName);
        RedisKeyHeartBeatThread redisKeyHeartBeatThread = null;
        try {
            while (true) {
                try {
                    boolean lockGotten = tryGetTaskLockPeriodically();
                    if (!lockGotten) {
                        // 30s之后重试
                        ThreadUtils.sleep(30_000);
                        continue;
                    }

                    // 获取任务锁之后通过心跳线程维持锁的占有
                    redisKeyHeartBeatThread = startRedisKeyHeartBeatThread();

                    // 监听并处理事件
                    watchEvent();
                } catch (Throwable t) {
                    log.error("Watching event caught exception", t);
                    if (redisKeyHeartBeatThread != null) {
                        redisKeyHeartBeatThread.stopAtOnce();
                    }
                    LockUtils.releaseDistributedLock(redisLockKey, machineIp);
                    // 过5s后重新尝试监听事件
                    ThreadUtils.sleep(5000);
                }
            }
        } finally {
            // 正常退出监听处理
            log.info("Quit watching {} event, release task lock", this.watcherResourceName);
            if (redisKeyHeartBeatThread != null) {
                redisKeyHeartBeatThread.stopAtOnce();
            }
            LockUtils.releaseDistributedLock(redisLockKey, machineIp);
        }
    }

    private boolean tryGetTaskLockPeriodically() {
        boolean lockGotten = false;
        try {
            lockGotten = LockUtils.tryGetReentrantLock(redisLockKey, machineIp, 5_000);
            if (!lockGotten) {
                log.info("Get lock {} fail", this.redisLockKey);
                return false;
            }
            lockGotten = true;
        } catch (Throwable t) {
            log.error("Get lock caught exception", t);
        }
        return lockGotten;
    }

    private RedisKeyHeartBeatThread startRedisKeyHeartBeatThread() {
        // 开一个心跳子线程，维护当前机器正在WatchResource的状态
        RedisKeyHeartBeatThread redisKeyHeartBeatThread = new RedisKeyHeartBeatThread(
            redisTemplate,
            redisLockKey,
            machineIp,
            5_000L,
            2_000L
        );
        redisKeyHeartBeatThread.setName("[" + watcherResourceName + "]-redisKeyHeartBeatThread");
        redisKeyHeartBeatThread.start();
        return redisKeyHeartBeatThread;
    }

    private void watchEvent() {
        log.info("Start watch {} resource at {},{}", watcherResourceName, TimeUtil.getCurrentTimeStr("HH:mm:ss"),
            System.currentTimeMillis());
        String cursor = null;
        while (true) {
            while (isWatchingEnabled()) {
                Span span = null;
                try {
                    ResourceWatchResult<E> watchResult;
                    span = this.tracing.tracer().newTrace();
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
                    if (span != null) {
                        span.error(t);
                    }
                    log.error("EventWatch thread fail", t);
                    // 如果处理事件过程中碰到异常，等待30s重试
                    ThreadUtils.sleep(30_000);
                }
            }
            // 如果事件开关为disabled，间隔30s重新判断是否开启
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
            //解析事件，进行处理
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
    protected abstract void handleEvent(ResourceEvent<E> event);
}
