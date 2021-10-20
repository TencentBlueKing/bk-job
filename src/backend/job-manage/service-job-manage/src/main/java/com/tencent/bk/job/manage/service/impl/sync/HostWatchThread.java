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
import com.tencent.bk.job.common.cc.model.result.HostEventDetail;
import com.tencent.bk.job.common.cc.model.result.ResourceEvent;
import com.tencent.bk.job.common.cc.model.result.ResourceWatchResult;
import com.tencent.bk.job.common.cc.sdk.CcClient;
import com.tencent.bk.job.common.cc.sdk.CcClientFactory;
import com.tencent.bk.job.common.gse.service.QueryAgentStatusClient;
import com.tencent.bk.job.common.model.dto.ApplicationHostInfoDTO;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.redis.util.RedisKeyHeartBeatThread;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class HostWatchThread extends Thread {

    private static final String REDIS_KEY_RESOURCE_WATCH_HOST_JOB_LOCK = "resource-watch-host-job-lock";
    private static final String machineIp = IpUtils.getFirstMachineIP();
    private static final AtomicInteger instanceNum = new AtomicInteger(1);

    static {
        try {
            //进程重启首先尝试释放上次加上的锁避免死锁
            LockUtils.releaseDistributedLock(REDIS_KEY_RESOURCE_WATCH_HOST_JOB_LOCK, machineIp);
        } catch (Throwable t) {
            log.info("Redis key:" + REDIS_KEY_RESOURCE_WATCH_HOST_JOB_LOCK + " does not need to be released, ignore");
        }
    }

    private final DSLContext dslContext;
    private final ApplicationHostDAO applicationHostDAO;
    private final QueryAgentStatusClient queryAgentStatusClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final AppHostsUpdateHelper appHostsUpdateHelper;
    private final String REDIS_KEY_RESOURCE_WATCH_HOST_JOB_RUNNING_MACHINE = "resource-watch-host-job-running-machine";
    private final List<AppHostEventsHandler> eventsHandlerList;
    private final BlockingQueue<ResourceEvent<HostEventDetail>> appHostEventQueue = new LinkedBlockingQueue<>(10000);
    private final Integer MAX_HANDLER_NUM = 1;
    private final AtomicBoolean hostWatchFlag = new AtomicBoolean(true);

    public HostWatchThread(DSLContext dslContext, ApplicationHostDAO applicationHostDAO,
                           QueryAgentStatusClient queryAgentStatusClient, RedisTemplate<String, String> redisTemplate
        , AppHostsUpdateHelper appHostsUpdateHelper) {
        this.dslContext = dslContext;
        this.applicationHostDAO = applicationHostDAO;
        this.queryAgentStatusClient = queryAgentStatusClient;
        this.redisTemplate = redisTemplate;
        this.appHostsUpdateHelper = appHostsUpdateHelper;
        this.setName("[" + getId() + "]-HostWatchThread-" + instanceNum.getAndIncrement());
        this.eventsHandlerList = new ArrayList<>();
        // 初始内置1个Handler
        for (int i = 0; i < 1; i++) {
            AppHostEventsHandler handler = new AppHostEventsHandler(appHostEventQueue);
            handler.setName("[" + handler.getId() + "]-AppHostEventsHandler-" + (i + 1));
            eventsHandlerList.add(handler);
        }
    }

    private void init() {
        for (AppHostEventsHandler appHostEventsHandler : eventsHandlerList) {
            appHostEventsHandler.start();
        }
    }

    public void setWatchFlag(boolean value) {
        hostWatchFlag.set(value);
    }

    private void dispatchEvent(ResourceEvent<HostEventDetail> event) {
        ApplicationHostInfoDTO hostInfoDTO = HostEventDetail.toHostInfoDTO(event.getDetail());
        Long hostId = hostInfoDTO.getHostId();
        ApplicationHostInfoDTO oldHostInfoDTO = applicationHostDAO.getHostById(hostId);
        Long appId = oldHostInfoDTO.getAppId();
        List<AppHostEventsHandler> idleHandlerList = new ArrayList<>();
        for (AppHostEventsHandler handler : eventsHandlerList) {
            if (appId.equals(handler.getAppId())) {
                handler.commitEvent(appId, event);
                return;
            } else if (handler.isIdle()) {
                idleHandlerList.add(handler);
            }
        }
        if (!idleHandlerList.isEmpty()) {
            AppHostEventsHandler handler = idleHandlerList.get((int) (Math.random() * idleHandlerList.size()));
            handler.commitEvent(appId, event);
        } else if (eventsHandlerList.size() < MAX_HANDLER_NUM) {
            AppHostEventsHandler handler = new AppHostEventsHandler(appHostEventQueue);
            handler.setName("[" + handler.getId() + "]-AppHostEventsHandler-" + (eventsHandlerList.size() + 1));
            handler.start();
            eventsHandlerList.add(handler);
            handler.commitEvent(appId, event);
        } else {
            AppHostEventsHandler handler = eventsHandlerList.get((int) (Math.random() * eventsHandlerList.size()));
            handler.commitEvent(appId, event);
        }
    }

    private void handleOneEventRelatedToApp(ResourceEvent<HostEventDetail> event) {
        ApplicationHostInfoDTO hostInfoDTO = HostEventDetail.toHostInfoDTO(event.getDetail());
        Long hostId = hostInfoDTO.getHostId();
        ApplicationHostInfoDTO oldHostInfoDTO = applicationHostDAO.getHostById(hostId);
        Long appId = oldHostInfoDTO.getAppId();
        try {
            appHostsUpdateHelper.waitAndStartAppHostsUpdating(appId);
            handleOneEventIndeed(event);
        } catch (Throwable t) {
            log.error(String.format("Fail to handle hostEvent of appId %d, event:%s", appId, event), t);
        } finally {
            appHostsUpdateHelper.endToUpdateAppHosts(appId);
        }
    }

    private void handleOneEvent(ResourceEvent<HostEventDetail> event) {
        ApplicationHostInfoDTO hostInfoDTO = HostEventDetail.toHostInfoDTO(event.getDetail());
        Long hostId = hostInfoDTO.getHostId();
        ApplicationHostInfoDTO oldHostInfoDTO = applicationHostDAO.getHostById(hostId);
        if (oldHostInfoDTO != null && oldHostInfoDTO.getAppId() != null) {
            dispatchEvent(event);
        } else {
            handleOneEventIndeed(event);
        }
    }

    private void handleOneEventIndeed(ResourceEvent<HostEventDetail> event) {
        String eventType = event.getEventType();
        ApplicationHostInfoDTO hostInfoDTO = HostEventDetail.toHostInfoDTO(event.getDetail());
        switch (eventType) {
            case ResourceWatchReq.EVENT_TYPE_CREATE:
            case ResourceWatchReq.EVENT_TYPE_UPDATE:
                //去除没有IP的主机信息
                if (StringUtils.isBlank(hostInfoDTO.getDisplayIp())) {
                    applicationHostDAO.deleteAppHostInfoById(dslContext, null, hostInfoDTO.getHostId());
                    break;
                }
                //找出Agent有效的IP，并设置Agent状态
                Long cloudAreaId = hostInfoDTO.getCloudAreaId();
                String ip = queryAgentStatusClient.getHostIpByAgentStatus(hostInfoDTO.getDisplayIp(), cloudAreaId);
                hostInfoDTO.setIp(ip);
                if (!ip.contains(":")) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(cloudAreaId);
                    sb.append(":");
                    sb.append(ip);
                    hostInfoDTO.setGseAgentAlive(queryAgentStatusClient.getAgentStatus(sb.toString()).status == 1);
                } else {
                    hostInfoDTO.setGseAgentAlive(queryAgentStatusClient.getAgentStatus(ip).status == 1);
                }
                try {
                    if (applicationHostDAO.existAppHostInfoByHostId(dslContext, hostInfoDTO.getHostId())) {
                        ApplicationHostInfoDTO oldHostInfoDTO = applicationHostDAO.getHostById(hostInfoDTO.getHostId());
                        // 不变化的字段需要原样保留
                        hostInfoDTO.setAppId(oldHostInfoDTO.getAppId());
                        hostInfoDTO.setSetId(oldHostInfoDTO.getSetId());
                        hostInfoDTO.setModuleId(oldHostInfoDTO.getModuleId());
                        hostInfoDTO.setModuleType(oldHostInfoDTO.getModuleType());
                        applicationHostDAO.updateAppHostInfoByHostId(dslContext, oldHostInfoDTO.getAppId(),
                            hostInfoDTO);
                    } else {
                        hostInfoDTO.setAppId(-1L);
                        try {
                            applicationHostDAO.insertAppHostWithoutTopo(dslContext, hostInfoDTO);
                        } catch (DataAccessException e) {
                            String errorMessage = e.getMessage();
                            if (errorMessage.contains("Duplicate entry") && errorMessage.contains("PRIMARY")) {
                                // 若已存在则忽略
                            } else {
                                log.error("insertHost fail:hostInfo=" + hostInfoDTO, e);
                            }
                        }
                    }
                } catch (Throwable t) {
                    log.error("handle host event fail", t);
                } finally {
                    // 从拓扑表向主机表同步拓扑数据
                    applicationHostDAO.syncHostTopo(dslContext, hostInfoDTO.getHostId());
                }
                break;
            case ResourceWatchReq.EVENT_TYPE_DELETE:
                applicationHostDAO.deleteAppHostInfoById(dslContext, null, hostInfoDTO.getHostId());
                break;
            default:
                break;
        }
        HostEventDetail detail = event.getDetail();
        log.debug("eventType=" + eventType);
        log.debug(JsonUtils.toJson(detail));
    }

    public String handleHostWatchResult(ResourceWatchResult<HostEventDetail> hostWatchResult) {
        String cursor = null;
        boolean isWatched = hostWatchResult.getWatched();
        if (isWatched) {
            List<ResourceEvent<HostEventDetail>> events = hostWatchResult.getEvents();
            //解析事件，进行处理
            for (ResourceEvent<HostEventDetail> event : events) {
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
            List<ResourceEvent<HostEventDetail>> events = hostWatchResult.getEvents();
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
        log.info("hostWatch arranged");
        init();
        while (true) {
            String cursor = null;
            // 从10分钟前开始watch
            long startTime = System.currentTimeMillis() / 1000 - 10 * 60;
            try {
                boolean lockGotten = LockUtils.tryGetDistributedLock(REDIS_KEY_RESOURCE_WATCH_HOST_JOB_LOCK,
                    machineIp, 50);
                if (!lockGotten) {
                    log.info("hostWatch lock not gotten, wait 100ms and retry");
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        log.warn("Sleep interrupted", e);
                    }
                    continue;
                }
                String runningMachine =
                    redisTemplate.opsForValue().get(REDIS_KEY_RESOURCE_WATCH_HOST_JOB_RUNNING_MACHINE);
                if (StringUtils.isNotBlank(runningMachine)) {
                    //已有hostWatch线程在跑，不再重复Watch
                    log.info("hostWatch thread already running on {}", runningMachine);
                    try {
                        sleep(30000);
                    } catch (InterruptedException e) {
                        log.warn("Sleep interrupted", e);
                    }
                    continue;
                }
                // 开一个心跳子线程，维护当前机器正在WatchResource的状态
                RedisKeyHeartBeatThread hostWatchRedisKeyHeartBeatThread = new RedisKeyHeartBeatThread(
                    redisTemplate,
                    REDIS_KEY_RESOURCE_WATCH_HOST_JOB_RUNNING_MACHINE,
                    machineIp,
                    3000L,
                    2000L
                );
                hostWatchRedisKeyHeartBeatThread.setName("[" + hostWatchRedisKeyHeartBeatThread.getId() +
                    "]-hostWatchRedisKeyHeartBeatThread");
                hostWatchRedisKeyHeartBeatThread.start();
                log.info("start watch host resource at {},{}", TimeUtil.getCurrentTimeStr("HH:mm:ss"),
                    System.currentTimeMillis());
                StopWatch watch = new StopWatch("hostWatch");
                watch.start("total");
                try {
                    CcClient ccClient = CcClientFactory.getCcClient();
                    ResourceWatchResult<HostEventDetail> hostWatchResult;
                    while (hostWatchFlag.get()) {
                        if (cursor == null) {
                            log.info("Start watch from startTime:{}", TimeUtil.formatTime(startTime * 1000));
                            hostWatchResult = ccClient.getHostEvents(startTime, cursor);
                        } else {
                            hostWatchResult = ccClient.getHostEvents(null, cursor);
                        }
                        log.info("hostWatchResult={}", JsonUtils.toJson(hostWatchResult));
                        cursor = handleHostWatchResult(hostWatchResult);
                        // 1s/watch一次
                        sleep(1000);
                    }
                } catch (Throwable t) {
                    log.error("hostWatch thread fail", t);
                    // 重置Watch起始位置为10分钟前
                    startTime = System.currentTimeMillis() / 1000 - 10 * 60;
                    cursor = null;
                } finally {
                    hostWatchRedisKeyHeartBeatThread.setRunFlag(false);
                    watch.stop();
                    log.info("hostWatch time consuming:" + watch.toString());
                }
            } catch (Throwable t) {
                log.error("HostWatchThread quit unexpectedly", t);
                startTime = System.currentTimeMillis() / 1000 - 10 * 60;
                cursor = null;
            } finally {
                try {
                    do {
                        // 5s/重试一次
                        sleep(5000);
                    } while (!hostWatchFlag.get());
                } catch (InterruptedException e) {
                    log.error("sleep interrupted", e);
                }
            }
        }
    }

    /**
     * 处理同一个业务的多个事件
     */
    class AppHostEventsHandler extends EventsHandler<HostEventDetail> {

        public AppHostEventsHandler(BlockingQueue<ResourceEvent<HostEventDetail>> queue) {
            super(queue);
        }

        @Override
        void handleEvent(ResourceEvent<HostEventDetail> event) {
            handleOneEventRelatedToApp(event);
        }
    }
}
