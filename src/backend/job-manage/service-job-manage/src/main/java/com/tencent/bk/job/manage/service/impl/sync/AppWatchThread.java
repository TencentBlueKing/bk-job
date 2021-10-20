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
import com.tencent.bk.job.common.cc.model.result.AppEventDetail;
import com.tencent.bk.job.common.cc.model.result.ResourceEvent;
import com.tencent.bk.job.common.cc.model.result.ResourceWatchResult;
import com.tencent.bk.job.common.cc.sdk.CcClient;
import com.tencent.bk.job.common.cc.sdk.CcClientFactory;
import com.tencent.bk.job.common.model.dto.ApplicationInfoDTO;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.redis.util.RedisKeyHeartBeatThread;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.dao.ApplicationInfoDAO;
import com.tencent.bk.job.manage.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StopWatch;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class AppWatchThread extends Thread {

    private static final String REDIS_KEY_RESOURCE_WATCH_APP_JOB_LOCK = "resource-watch-app-job-lock";
    private static final String machineIp = IpUtils.getFirstMachineIP();
    private static final AtomicInteger instanceNum = new AtomicInteger(1);

    static {
        try {
            //进程重启首先尝试释放上次加上的锁避免死锁
            LockUtils.releaseDistributedLock(REDIS_KEY_RESOURCE_WATCH_APP_JOB_LOCK, machineIp);
        } catch (Throwable t) {
            log.info("Redis key:" + REDIS_KEY_RESOURCE_WATCH_APP_JOB_LOCK + " does not need to be released, ignore");
        }
    }

    private final DSLContext dslContext;
    private final ApplicationInfoDAO applicationInfoDAO;
    private final ApplicationService applicationService;
    private final RedisTemplate<String, String> redisTemplate;
    private final String REDIS_KEY_RESOURCE_WATCH_APP_JOB_RUNNING_MACHINE = "resource-watch-app-job-running-machine";
    private final AtomicBoolean appWatchFlag = new AtomicBoolean(true);

    public AppWatchThread(DSLContext dslContext, ApplicationInfoDAO applicationInfoDAO,
                          ApplicationService applicationService, RedisTemplate<String, String> redisTemplate) {
        this.dslContext = dslContext;
        this.applicationInfoDAO = applicationInfoDAO;
        this.applicationService = applicationService;
        this.redisTemplate = redisTemplate;
        this.setName("[" + getId() + "]-AppWatchThread-" + instanceNum.getAndIncrement());
    }

    public void setWatchFlag(boolean value) {
        appWatchFlag.set(value);
    }

    private void handleOneEvent(ResourceEvent<AppEventDetail> event) {
        String eventType = event.getEventType();
        ApplicationInfoDTO appInfoDTO = AppEventDetail.toAppInfoDTO(event.getDetail());
        switch (eventType) {
            case ResourceWatchReq.EVENT_TYPE_CREATE:
            case ResourceWatchReq.EVENT_TYPE_UPDATE:
                try {
                    ApplicationInfoDTO oldAppInfoDTO = applicationInfoDAO.getAppInfoById(appInfoDTO.getId());
                    if (oldAppInfoDTO != null) {
                        applicationInfoDAO.updateAppInfo(dslContext, appInfoDTO);
                    } else {
                        try {
                            applicationService.createApp(appInfoDTO);
                        } catch (DataAccessException e) {
                            String errorMessage = e.getMessage();
                            if (errorMessage.contains("Duplicate entry") && errorMessage.contains("PRIMARY")) {
                                // 若已存在则忽略
                            } else {
                                log.error("insertApp fail:appInfo=" + appInfoDTO, e);
                            }
                        }
                    }
                } catch (Throwable t) {
                    log.error("handle app event fail", t);
                }
                break;
            case ResourceWatchReq.EVENT_TYPE_DELETE:
                applicationInfoDAO.deleteAppInfoById(dslContext, appInfoDTO.getId());
                break;
            default:
                break;
        }
        AppEventDetail detail = event.getDetail();
        log.debug("eventType=" + eventType);
        log.debug(JsonUtils.toJson(detail));
    }

    public String handleAppWatchResult(ResourceWatchResult<AppEventDetail> appWatchResult) {
        String cursor = null;
        boolean isWatched = appWatchResult.getWatched();
        if (isWatched) {
            List<ResourceEvent<AppEventDetail>> events = appWatchResult.getEvents();
            //解析事件，进行处理
            for (ResourceEvent<AppEventDetail> event : events) {
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
            List<ResourceEvent<AppEventDetail>> events = appWatchResult.getEvents();
            if (events != null && events.size() > 0) {
                cursor = events.get(0).getCursor();
                log.info("refresh cursor(fail):{}", cursor);
            } else {
                log.warn("CMDB event error:no refresh event data when watched==false");
            }
        }
        return cursor;
    }

    @Override
    public void run() {
        log.info("appWatch arranged");
        while (true) {
            String cursor = null;
            long startTime = System.currentTimeMillis() / 1000 - 10 * 60;
            try {
                boolean lockGotten = LockUtils.tryGetDistributedLock(REDIS_KEY_RESOURCE_WATCH_APP_JOB_LOCK, machineIp
                    , 50);
                if (!lockGotten) {
                    log.info("appWatch lock not gotten, wait 100ms and retry");
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        log.error("sleep interrupted", e);
                    }
                    continue;
                }
                String runningMachine =
                    redisTemplate.opsForValue().get(REDIS_KEY_RESOURCE_WATCH_APP_JOB_RUNNING_MACHINE);
                if (StringUtils.isNotBlank(runningMachine)) {
                    //已有同步线程在跑，不再同步
                    log.info("appWatch thread already running on {}", runningMachine);
                    try {
                        sleep(30000);
                    } catch (InterruptedException e) {
                        log.error("sleep interrupted", e);
                    }
                    continue;
                }
                // 开一个心跳子线程，维护当前机器正在WatchResource的状态
                RedisKeyHeartBeatThread appWatchRedisKeyHeartBeatThread = new RedisKeyHeartBeatThread(
                    redisTemplate,
                    REDIS_KEY_RESOURCE_WATCH_APP_JOB_RUNNING_MACHINE,
                    machineIp,
                    3000L,
                    2000L
                );
                appWatchRedisKeyHeartBeatThread.setName(
                    "[" + appWatchRedisKeyHeartBeatThread.getId()
                        + "]-appWatchRedisKeyHeartBeatThread"
                );
                appWatchRedisKeyHeartBeatThread.start();
                log.info("start watch app resource at {},{}", TimeUtil.getCurrentTimeStr("HH:mm:ss"),
                    System.currentTimeMillis());
                StopWatch watch = new StopWatch("appWatch");
                watch.start("total");
                try {
                    CcClient ccClient = CcClientFactory.getCcClient();
                    ResourceWatchResult<AppEventDetail> appWatchResult;
                    while (appWatchFlag.get()) {
                        if (cursor == null) {
                            appWatchResult = ccClient.getAppEvents(startTime, cursor);
                        } else {
                            appWatchResult = ccClient.getAppEvents(null, cursor);
                        }
                        log.info("appWatchResult={}", JsonUtils.toJson(appWatchResult));
                        cursor = handleAppWatchResult(appWatchResult);
                        // 1s/watch一次
                        sleep(1000);
                    }
                } catch (Throwable t) {
                    log.error("appWatch thread fail", t);
                } finally {
                    appWatchRedisKeyHeartBeatThread.setRunFlag(false);
                    watch.stop();
                    log.info("appWatch time consuming:" + watch.toString());
                }
            } catch (Throwable t) {
                log.error("HostRelationWatchThread quit unexpectedly", t);
            } finally {
                try {
                    do {
                        // 1s/watch一次
                        sleep(1000);
                    } while (!appWatchFlag.get());
                } catch (InterruptedException e) {
                    log.error("sleep interrupted", e);
                }
            }
        }
    }
}
