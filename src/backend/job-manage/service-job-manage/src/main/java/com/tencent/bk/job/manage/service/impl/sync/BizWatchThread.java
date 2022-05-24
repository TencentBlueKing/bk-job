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

import com.tencent.bk.job.common.cc.model.req.ResourceWatchReq;
import com.tencent.bk.job.common.cc.model.result.BizEventDetail;
import com.tencent.bk.job.common.cc.model.result.ResourceEvent;
import com.tencent.bk.job.common.cc.model.result.ResourceWatchResult;
import com.tencent.bk.job.common.cc.sdk.CmdbClientFactory;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.redis.util.RedisKeyHeartBeatThread;
import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jooq.exception.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StopWatch;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * cmdb 业务事件监听
 */
@Slf4j
public class BizWatchThread extends Thread {

    private static final String REDIS_KEY_RESOURCE_WATCH_APP_BIZ_LOCK = "resource-watch-app-job-lock";
    private static final String machineIp = IpUtils.getFirstMachineIP();
    private static final AtomicInteger instanceNum = new AtomicInteger(1);

    static {
        try {
            //进程重启首先尝试释放上次加上的锁避免死锁
            LockUtils.releaseDistributedLock(REDIS_KEY_RESOURCE_WATCH_APP_BIZ_LOCK, machineIp);
        } catch (Throwable t) {
            log.info("Redis key:" + REDIS_KEY_RESOURCE_WATCH_APP_BIZ_LOCK + " does not need to be released, ignore");
        }
    }

    private final ApplicationService applicationService;
    private final RedisTemplate<String, String> redisTemplate;
    private final AtomicBoolean bizWatchFlag = new AtomicBoolean(true);

    public BizWatchThread(ApplicationService applicationService,
                          RedisTemplate<String, String> redisTemplate) {
        this.applicationService = applicationService;
        this.redisTemplate = redisTemplate;
        this.setName("[" + getId() + "]-bizWatchThread-" + instanceNum.getAndIncrement());
    }

    public void setWatchFlag(boolean value) {
        bizWatchFlag.set(value);
    }

    @Override
    public void run() {
        log.info("BizWatch arranged");
        while (true) {
            String cursor = null;
            long startTime = System.currentTimeMillis() / 1000 - 10 * 60;
            try {
                boolean lockGotten = LockUtils.tryGetDistributedLock(REDIS_KEY_RESOURCE_WATCH_APP_BIZ_LOCK, machineIp,
                    50);
                if (!lockGotten) {
                    log.info("BizWatch lock not gotten, wait 100ms and retry");
                    ThreadUtils.sleep(100);
                    continue;
                }
                String REDIS_KEY_RESOURCE_WATCH_BIZ_JOB_RUNNING_MACHINE = "resource-watch-app-job-running-machine";
                String runningMachine =
                    redisTemplate.opsForValue().get(REDIS_KEY_RESOURCE_WATCH_BIZ_JOB_RUNNING_MACHINE);
                if (StringUtils.isNotBlank(runningMachine)) {
                    //已有同步线程在跑，不再同步
                    log.info("BizWatch thread already running on {}", runningMachine);
                    ThreadUtils.sleep(30000);
                    continue;
                }
                // 开一个心跳子线程，维护当前机器正在WatchResource的状态
                RedisKeyHeartBeatThread bizWatchRedisKeyHeartBeatThread = new RedisKeyHeartBeatThread(
                    redisTemplate,
                    REDIS_KEY_RESOURCE_WATCH_BIZ_JOB_RUNNING_MACHINE,
                    machineIp,
                    3000L,
                    2000L
                );
                bizWatchRedisKeyHeartBeatThread.setName(
                    "[" + bizWatchRedisKeyHeartBeatThread.getId()
                        + "]-bizWatchRedisKeyHeartBeatThread"
                );
                bizWatchRedisKeyHeartBeatThread.start();
                log.info("Start watch biz resource at {},{}", TimeUtil.getCurrentTimeStr("HH:mm:ss"),
                    System.currentTimeMillis());
                StopWatch watch = new StopWatch("bizWatch");
                watch.start("total");
                try {
                    IBizCmdbClient bizCmdbClient = CmdbClientFactory.getCmdbClient();
                    ResourceWatchResult<BizEventDetail> bizWatchResult;
                    while (bizWatchFlag.get()) {
                        if (cursor == null) {
                            bizWatchResult = bizCmdbClient.getAppEvents(startTime, cursor);
                        } else {
                            bizWatchResult = bizCmdbClient.getAppEvents(null, cursor);
                        }
                        log.info("BizWatchResult={}", JsonUtils.toJson(bizWatchResult));
                        cursor = handleBizWatchResult(bizWatchResult);
                        // 1s/watch一次
                        ThreadUtils.sleep(1000);
                    }
                } catch (Throwable t) {
                    log.error("BizWatchThread fail", t);
                } finally {
                    bizWatchRedisKeyHeartBeatThread.setRunFlag(false);
                    watch.stop();
                    log.info("BizWatch time consuming:" + watch.getTotalTimeMillis() + "ms");
                }
            } catch (Throwable t) {
                log.error("BizWatchThread quit unexpectedly", t);
            } finally {
                do {
                    // 1s/watch一次
                    ThreadUtils.sleep(1000);
                } while (!bizWatchFlag.get());
            }
        }
    }

    public String handleBizWatchResult(ResourceWatchResult<BizEventDetail> bizWatchResult) {
        String cursor = null;
        boolean isWatched = bizWatchResult.getWatched();
        if (isWatched) {
            List<ResourceEvent<BizEventDetail>> events = bizWatchResult.getEvents();
            //解析事件，进行处理
            for (ResourceEvent<BizEventDetail> event : events) {
                handleOneEvent(event);
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
            List<ResourceEvent<BizEventDetail>> events = bizWatchResult.getEvents();
            if (events != null && events.size() > 0) {
                cursor = events.get(0).getCursor();
                log.info("refresh cursor(fail):{}", cursor);
            } else {
                log.warn("CMDB event error:no refresh event data when watched==false");
            }
        }
        return cursor;
    }

    private void tryToCreateApp(ApplicationDTO app) {
        try {
            applicationService.createApp(app);
        } catch (DataAccessException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("Duplicate entry") && errorMessage.contains("PRIMARY")) {
                // 若已存在则忽略
            } else {
                log.error("insertApp fail:appInfo=" + app, e);
            }
        }
    }

    private void handleOneEvent(ResourceEvent<BizEventDetail> event) {
        String eventType = event.getEventType();
        ApplicationDTO newestApp = BizEventDetail.toAppInfoDTO(event.getDetail());
        ApplicationDTO cachedApp = null;
        try {
            cachedApp = applicationService.getAppByScope(newestApp.getScope());
        } catch (NotFoundException e) {
            log.debug("cannot find app by scope:{}, need to create", newestApp.getScope());
        }
        switch (eventType) {
            case ResourceWatchReq.EVENT_TYPE_CREATE:
            case ResourceWatchReq.EVENT_TYPE_UPDATE:
                try {
                    if (cachedApp != null) {
                        updateBizProps(cachedApp, newestApp);
                        applicationService.updateApp(cachedApp);
                    } else {
                        if (ResourceWatchReq.EVENT_TYPE_CREATE.equals(eventType)) {
                            tryToCreateApp(newestApp);
                        } else {
                            // 不存在的业务（已归档）的Update事件，忽略
                            if (log.isDebugEnabled()) {
                                log.debug("ignore update event of invalid app:{}", JsonUtils.toJson(event));
                            }
                        }
                    }
                } catch (Throwable t) {
                    log.error("handle app event fail", t);
                }
                break;
            case ResourceWatchReq.EVENT_TYPE_DELETE:
                applicationService.deleteApp(cachedApp.getId());
                break;
            default:
                break;
        }
    }

    private void updateBizProps(ApplicationDTO originApp, ApplicationDTO updateApp) {
        originApp.setName(updateApp.getName());
        originApp.setBkSupplierAccount(updateApp.getBkSupplierAccount());
        originApp.setLanguage(updateApp.getLanguage());
        originApp.setMaintainers(updateApp.getMaintainers());
        originApp.setTimeZone(updateApp.getTimeZone());
    }
}
