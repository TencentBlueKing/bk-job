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
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.manager.app.ApplicationCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StopWatch;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
            log.info("Redis key:" + REDIS_KEY_RESOURCE_WATCH_BIZ_SET_JOB_LOCK + " does not need to be released, " +
                "ignore");
        }
    }

    private final DSLContext dslContext;
    private final RedisTemplate<String, String> redisTemplate;
    private final ApplicationCache applicationCache;
    private final IBizSetCmdbClient bizSetCmdbClient;
    private final ApplicationDAO applicationDAO;
    private final Tracing tracing;
    private final AtomicBoolean bizSetWatchFlag = new AtomicBoolean(true);

    public BizSetWatchThread(DSLContext dslContext,
                             RedisTemplate<String, String> redisTemplate,
                             ApplicationCache applicationCache,
                             IBizSetCmdbClient bizSetCmdbClient,
                             ApplicationDAO applicationDAO, Tracing tracing) {
        this.dslContext = dslContext;
        this.redisTemplate = redisTemplate;
        this.applicationCache = applicationCache;
        this.bizSetCmdbClient = bizSetCmdbClient;
        this.applicationDAO = applicationDAO;
        this.tracing = tracing;
        this.setName("[" + getId() + "]-BizSetWatchThread-");
    }

    public void setWatchFlag(boolean value) {
        bizSetWatchFlag.set(value);
    }

    @Override
    public void run() {
        log.info("BizSetWatch arranged");
        while (true) {
            String cursor = null;
            // 从10分钟前开始watch
            long startTime = System.currentTimeMillis() / 1000 - 10 * 60;
            try {
                boolean lockGotten = LockUtils.tryGetDistributedLock(REDIS_KEY_RESOURCE_WATCH_BIZ_SET_JOB_LOCK,
                    machineIp, 50);
                if (!lockGotten) {
                    log.info("bizSetWatch lock not gotten, wait 100ms and retry");
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        log.warn("Sleep interrupted", e);
                    }
                    continue;
                }
                String REDIS_KEY_RESOURCE_WATCH_BIZ_SET_JOB_RUNNING_MACHINE =
                    "resource-watch-biz-set-job-running-machine";
                String runningMachine =
                    redisTemplate.opsForValue().get(REDIS_KEY_RESOURCE_WATCH_BIZ_SET_JOB_RUNNING_MACHINE);
                if (StringUtils.isNotBlank(runningMachine)) {
                    //已有bizSetWatch线程在跑，不再重复Watch
                    log.info("bizSetWatch thread already running on {}", runningMachine);
                    try {
                        sleep(30000);
                    } catch (InterruptedException e) {
                        log.warn("Sleep interrupted", e);
                    }
                    continue;
                }
                // 开一个心跳子线程，维护当前机器正在WatchResource的状态
                RedisKeyHeartBeatThread bizSetWatchRedisKeyHeartBeatThread = new RedisKeyHeartBeatThread(
                    redisTemplate,
                    REDIS_KEY_RESOURCE_WATCH_BIZ_SET_JOB_RUNNING_MACHINE,
                    machineIp,
                    3000L,
                    2000L
                );
                bizSetWatchRedisKeyHeartBeatThread.setName("[" + bizSetWatchRedisKeyHeartBeatThread.getId() +
                    "]-bizSetWatchRedisKeyHeartBeatThread");
                bizSetWatchRedisKeyHeartBeatThread.start();
                log.info("start watch biz_set resource at {},{}", TimeUtil.getCurrentTimeStr("HH:mm:ss"),
                    System.currentTimeMillis());
                StopWatch watch = new StopWatch("bizSetWatch");
                watch.start("total");
                Span span = null;
                try {
                    ResourceWatchResult<BizSetEventDetail> bizSetWatchResult;
                    while (bizSetWatchFlag.get()) {
                        span = this.tracing.tracer().newTrace();
                        if (cursor == null) {
                            log.info("Start watch from startTime:{}", TimeUtil.formatTime(startTime * 1000));
                            bizSetWatchResult = bizSetCmdbClient.getBizSetEvents(startTime, null);
                        } else {
                            bizSetWatchResult = bizSetCmdbClient.getBizSetEvents(null, cursor);
                        }
                        log.info("bizSetWatchResult={}", JsonUtils.toJson(bizSetWatchResult));
                        cursor = handleBizSetWatchResult(bizSetWatchResult);
                        // 10s/watch一次
                        sleep(10000);
                    }
                } catch (Throwable t) {
                    if (span != null) {
                        span.error(t);
                    }
                    log.error("bizSetWatch thread fail", t);
                    // 重置Watch起始位置为10分钟前
                    startTime = System.currentTimeMillis() / 1000 - 10 * 60;
                    cursor = null;
                } finally {
                    bizSetWatchRedisKeyHeartBeatThread.setRunFlag(false);
                    watch.stop();
                    log.info("bizSetWatch time consuming:" + watch.toString());
                    if (span != null) {
                        span.finish();
                    }
                }
            } catch (Throwable t) {
                log.error("BizSetWatchThread quit unexpectedly", t);
                startTime = System.currentTimeMillis() / 1000 - 10 * 60;
                cursor = null;
            } finally {
                try {
                    do {
                        // 5s/重试一次
                        sleep(5000);
                    } while (!bizSetWatchFlag.get());
                } catch (InterruptedException e) {
                    log.error("sleep interrupted", e);
                }
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
                log.info("events.size={},events={}", events.size(), JsonUtils.toJson(events));
                cursor = events.get(events.size() - 1).getCursor();
                log.info("refresh cursor(success):{}", cursor);
            } else {
                log.info("events.size==0");
            }
        } else {
            // 只有一个无实际意义的事件，用于换取bk_cursor
            List<ResourceEvent<BizSetEventDetail>> events = bizSetWatchResult.getEvents();
            if (events != null && events.size() > 0) {
                cursor = events.get(0).getCursor();
                log.info("refresh cursor(fail):{}", cursor);
            } else {
                log.warn("CMDB event error:no refresh event data when watched==false");
            }
        }
        return cursor;
    }

    private void handleEvent(ResourceEvent<BizSetEventDetail> event) {
        ApplicationDTO application = event.getDetail().toApplicationDTO();
        String eventType = event.getEventType();
        switch (eventType) {
            case ResourceWatchReq.EVENT_TYPE_CREATE:
            case ResourceWatchReq.EVENT_TYPE_UPDATE:
                try {
                    ApplicationDTO cachedAppInfoDTO =
                        applicationDAO.getAppByScopeIncludingDeleted(application.getScope());
                    if (cachedAppInfoDTO != null) {
                        if (!cachedAppInfoDTO.isDeleted()) {
                            applicationDAO.updateApp(dslContext, application);
                        } else {
                            log.info("Restore deleted application: {}", application);
                            applicationDAO.updateApp(dslContext, application);
                            applicationDAO.restoreApp(application.getId());
                        }
                    } else {
                        try {
                            applicationDAO.insertApp(dslContext, application);
                        } catch (DataAccessException e) {
                            // 若已存在则忽略
                            log.error("Insert app fail", e);
                        }
                    }
                    applicationCache.addOrUpdateApp(application);
                } catch (Throwable t) {
                    log.error("Handle biz_set event fail", t);
                }
                break;
            case ResourceWatchReq.EVENT_TYPE_DELETE:
                applicationDAO.deleteAppByIdSoftly(dslContext, application.getId());
                applicationCache.deleteApp(application.getScope());
                break;
            default:
                log.info("No need to handle event: {}", event);
                break;
        }
    }

}
