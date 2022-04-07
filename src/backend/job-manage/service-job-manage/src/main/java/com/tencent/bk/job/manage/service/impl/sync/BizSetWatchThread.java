/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

import brave.Span;
import brave.Tracing;
import com.tencent.bk.job.common.cc.model.req.ResourceWatchReq;
import com.tencent.bk.job.common.cc.model.result.BizSetEventDetail;
import com.tencent.bk.job.common.cc.model.result.ResourceEvent;
import com.tencent.bk.job.common.cc.model.result.ResourceWatchResult;
import com.tencent.bk.job.common.cc.sdk.IBizSetCmdbClient;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.redis.util.RedisKeyHeartBeatThread;
import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.impl.BizSetService;
import lombok.extern.slf4j.Slf4j;
import org.jooq.exception.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

/**
 * 业务集事件监听
 */
@Slf4j
public class BizSetWatchThread extends Thread {

    private static final String REDIS_KEY_RESOURCE_WATCH_BIZ_SET_JOB_LOCK = "resource-watch-biz-set-job-lock";
    private static final String machineIp = IpUtils.getFirstMachineIP();

    static {
        try {
            //进程重启首先尝试释放上次加上的锁避免死锁
            LockUtils.releaseDistributedLock(REDIS_KEY_RESOURCE_WATCH_BIZ_SET_JOB_LOCK, machineIp);
        } catch (Throwable t) {
            log.info("Redis key:" + REDIS_KEY_RESOURCE_WATCH_BIZ_SET_JOB_LOCK +
                " does not need to be released ignore");
        }
    }

    private final RedisTemplate<String, String> redisTemplate;
    private final ApplicationService applicationService;
    private final IBizSetCmdbClient bizSetCmdbClient;
    private final BizSetService bizSetService;
    private final Tracing tracing;

    public BizSetWatchThread(RedisTemplate<String, String> redisTemplate,
                             ApplicationService applicationService,
                             IBizSetCmdbClient bizSetCmdbClient,
                             BizSetService bizSetService,
                             Tracing tracing) {
        this.redisTemplate = redisTemplate;
        this.applicationService = applicationService;
        this.bizSetCmdbClient = bizSetCmdbClient;
        this.bizSetService = bizSetService;
        this.tracing = tracing;
        this.setName("[" + getId() + "]-BizSetWatchThread-");
    }

    @Override
    public void run() {
        log.info("BizSetWatch arranged");
        RedisKeyHeartBeatThread bizSetWatchRedisKeyHeartBeatThread = null;
        try {
            while (true) {
                try {
                    if (!bizSetService.isBizSetMigratedToCMDB()) {
                        log.warn("Job BizSets have not been migrated to CMDB, " +
                            "do not watch bizSet event from CMDB, " +
                            "please use upgrader in package to migrate as soon as possible"
                        );
                        ThreadUtils.sleep(60_000);
                        continue;
                    }

                    boolean lockGotten = tryGetTaskLockPeriodically();
                    if (!lockGotten) {
                        // 30s之后重试
                        ThreadUtils.sleep(30_000);
                        continue;
                    }

                    // 获取任务锁之后通过心跳线程维持锁的占有
                    bizSetWatchRedisKeyHeartBeatThread = startBizSetWatchRedisKeyHeartBeatThread();

                    watchEvent();
                } catch (Throwable t) {
                    log.error("Watching event caught exception", t);
                    if (bizSetWatchRedisKeyHeartBeatThread != null) {
                        bizSetWatchRedisKeyHeartBeatThread.stopAtOnce();
                    }
                    LockUtils.releaseDistributedLock(REDIS_KEY_RESOURCE_WATCH_BIZ_SET_JOB_LOCK, machineIp);
                    // 过5s后重新尝试监听事件
                    ThreadUtils.sleep(5000);
                }
            }
        } finally {
            // 正常退出监听处理
            log.info("Quit watching bizSet event, release task lock");
            if (bizSetWatchRedisKeyHeartBeatThread != null) {
                bizSetWatchRedisKeyHeartBeatThread.stopAtOnce();
            }
            LockUtils.releaseDistributedLock(REDIS_KEY_RESOURCE_WATCH_BIZ_SET_JOB_LOCK, machineIp);
        }
    }

    private boolean tryGetTaskLockPeriodically() {
        boolean lockGotten = false;
        try {
            lockGotten = LockUtils.tryGetReentrantLock(REDIS_KEY_RESOURCE_WATCH_BIZ_SET_JOB_LOCK,
                machineIp, 5_000);
            if (!lockGotten) {
                log.info("Get BizSetWatch lock fail");
                return false;
            }
            lockGotten = true;
        } catch (Throwable t) {
            log.error("BizSetWatchThread quit unexpectedly", t);
        }
        return lockGotten;
    }

    private RedisKeyHeartBeatThread startBizSetWatchRedisKeyHeartBeatThread() {
        // 开一个心跳子线程，维护当前机器正在WatchResource的状态
        RedisKeyHeartBeatThread bizSetWatchRedisKeyHeartBeatThread = new RedisKeyHeartBeatThread(
            redisTemplate,
            REDIS_KEY_RESOURCE_WATCH_BIZ_SET_JOB_LOCK,
            machineIp,
            5_000L,
            2_000L
        );
        bizSetWatchRedisKeyHeartBeatThread.setName("[" + bizSetWatchRedisKeyHeartBeatThread.getId() +
            "]-bizSetWatchRedisKeyHeartBeatThread");
        bizSetWatchRedisKeyHeartBeatThread.start();
        return bizSetWatchRedisKeyHeartBeatThread;
    }

    private void watchEvent() {
        log.info("Start watch bizSet resource at {},{}", TimeUtil.getCurrentTimeStr("HH:mm:ss"),
            System.currentTimeMillis());
        String cursor = null;
        while (true) {
            Span span = null;
            try {
                ResourceWatchResult<BizSetEventDetail> bizSetWatchResult;
                span = this.tracing.tracer().newTrace();
                if (cursor == null) {
                    // 从10分钟前开始watch
                    long startTime = System.currentTimeMillis() / 1000 - 10 * 60;
                    log.info("Start watch from startTime:{}", TimeUtil.formatTime(startTime * 1000));
                    bizSetWatchResult = bizSetCmdbClient.getBizSetEvents(startTime, null);
                } else {
                    bizSetWatchResult = bizSetCmdbClient.getBizSetEvents(null, cursor);
                }
                log.info("BizSetWatchResult={}", JsonUtils.toJson(bizSetWatchResult));
                cursor = handleBizSetWatchResult(bizSetWatchResult);
                // 10s/watch一次
                ThreadUtils.sleep(10_000);
            } catch (Throwable t) {
                if (span != null) {
                    span.error(t);
                }
                log.error("BizSetWatch thread fail", t);
                cursor = null;
            }
        }
    }


    private String handleBizSetWatchResult(ResourceWatchResult<BizSetEventDetail> bizSetWatchResult) {
        String cursor = null;
        boolean isWatched = bizSetWatchResult.getWatched();
        if (isWatched) {
            List<ResourceEvent<BizSetEventDetail>> events = bizSetWatchResult.getEvents();
            //解析事件，进行处理
            for (ResourceEvent<BizSetEventDetail> event : events) {
                handleEvent(event);
            }
            if (events.size() > 0) {
                log.info("Handle bizSet watch events, events.size: {},events: {}",
                    events.size(), JsonUtils.toJson(events));
                cursor = events.get(events.size() - 1).getCursor();
                log.info("Refresh cursor:{}", cursor);
            } else {
                log.info("Handle bizSet watch events, events is empty");
            }
        } else {
            // 只有一个无实际意义的事件，用于换取bk_cursor
            List<ResourceEvent<BizSetEventDetail>> events = bizSetWatchResult.getEvents();
            if (events != null && events.size() > 0) {
                cursor = events.get(0).getCursor();
                log.info("Refresh cursor:{}", cursor);
            } else {
                log.warn("CMDB event error:no refresh event data when watched==false");
            }
        }
        return cursor;
    }

    private void handleEvent(ResourceEvent<BizSetEventDetail> event) {
        log.info("Handle BizSetEvent: {}", event);
        ApplicationDTO latestApp = event.getDetail().toApplicationDTO();
        String eventType = event.getEventType();
        ApplicationDTO cachedApp =
            applicationService.getAppByScopeIncludingDeleted(latestApp.getScope());
        switch (eventType) {
            case ResourceWatchReq.EVENT_TYPE_CREATE:
            case ResourceWatchReq.EVENT_TYPE_UPDATE:
                try {
                    if (cachedApp != null) {
                        updateBizSetProps(cachedApp, latestApp);
                        if (!cachedApp.isDeleted()) {
                            log.info("Update bizSet: {}", cachedApp);
                            applicationService.updateApp(cachedApp);
                        } else {
                            log.info("Restore deleted latestApp: {}", latestApp);
                            applicationService.updateApp(latestApp);
                            applicationService.restoreDeletedApp(latestApp.getId());
                        }
                    } else {
                        try {
                            applicationService.createApp(latestApp);
                        } catch (DataAccessException e) {
                            // 若已存在则忽略
                            log.error("Insert app fail", e);
                        }
                    }
                } catch (Throwable t) {
                    log.error("Handle biz_set event fail", t);
                }
                break;
            case ResourceWatchReq.EVENT_TYPE_DELETE:
                if (cachedApp != null) {
                    applicationService.deleteApp(cachedApp.getId());
                }
                break;
            default:
                log.info("No need to handle event: {}", event);
                break;
        }
    }

    private void updateBizSetProps(ApplicationDTO originApp, ApplicationDTO updateApp) {
        originApp.setName(updateApp.getName());
        originApp.setBkSupplierAccount(updateApp.getBkSupplierAccount());
        originApp.setLanguage(updateApp.getLanguage());
        originApp.setMaintainers(updateApp.getMaintainers());
        originApp.setTimeZone(updateApp.getTimeZone());
    }

}
