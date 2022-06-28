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
import com.tencent.bk.job.common.cc.model.result.HostRelationEventDetail;
import com.tencent.bk.job.common.cc.model.result.ResourceEvent;
import com.tencent.bk.job.common.cc.model.result.ResourceWatchResult;
import com.tencent.bk.job.common.cc.sdk.CmdbClientFactory;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.redis.util.RedisKeyHeartBeatThread;
import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import com.tencent.bk.job.manage.manager.host.HostCache;
import com.tencent.bk.job.manage.model.dto.HostTopoDTO;
import com.tencent.bk.job.manage.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class HostRelationWatchThread extends Thread {

    private static final String REDIS_KEY_RESOURCE_WATCH_HOST_RELATION_JOB_LOCK = "resource-watch-host-relation-job" +
        "-lock";
    private static final String machineIp = IpUtils.getFirstMachineIP();
    private static final AtomicInteger instanceNum = new AtomicInteger(1);
    private static final AtomicBoolean hostRelationWatchFlag = new AtomicBoolean(true);

    static {
        try {
            //进程重启首先尝试释放上次加上的锁避免死锁
            LockUtils.releaseDistributedLock(REDIS_KEY_RESOURCE_WATCH_HOST_RELATION_JOB_LOCK, machineIp);
        } catch (Throwable t) {
            log.info("Redis key:" + REDIS_KEY_RESOURCE_WATCH_HOST_RELATION_JOB_LOCK + " does not need to be released," +
                " ignore");
        }
    }

    private final DSLContext dslContext;
    private final ApplicationService applicationService;
    private final ApplicationHostDAO applicationHostDAO;
    private final HostTopoDAO hostTopoDAO;
    private final RedisTemplate<String, String> redisTemplate;
    private final List<AppHostRelationEventsHandler> eventsHandlerList;
    private final HostCache hostCache;

    private final BlockingQueue<ResourceEvent<HostRelationEventDetail>> appHostRelationEventQueue =
        new LinkedBlockingQueue<>(10000);

    private final Integer MAX_HANDLER_NUM = 1;
    private final String REDIS_KEY_RESOURCE_WATCH_HOST_RELATION_JOB_RUNNING_MACHINE = "resource-watch-host-relation" +
        "-job-running-machine";

    public HostRelationWatchThread(DSLContext dslContext,
                                   ApplicationService applicationService,
                                   ApplicationHostDAO applicationHostDAO,
                                   HostTopoDAO hostTopoDAO,
                                   RedisTemplate<String, String> redisTemplate,
                                   HostCache hostCache) {
        this.dslContext = dslContext;
        this.applicationService = applicationService;
        this.applicationHostDAO = applicationHostDAO;
        this.hostTopoDAO = hostTopoDAO;
        this.redisTemplate = redisTemplate;
        this.hostCache = hostCache;
        this.setName("[" + getId() + "]-HostRelationWatchThread-" + instanceNum.getAndIncrement());
        this.eventsHandlerList = new ArrayList<>();
        // 初始内置1个Handler
        for (int i = 0; i < 1; i++) {
            AppHostRelationEventsHandler handler = new AppHostRelationEventsHandler(appHostRelationEventQueue);
            handler.setName("[" + handler.getId() + "]-AppHostRelationEventsHandler-" + (i + 1));
            eventsHandlerList.add(handler);
        }
    }

    private void init() {
        for (AppHostRelationEventsHandler handler : eventsHandlerList) {
            handler.start();
        }
    }

    public void setWatchFlag(boolean value) {
        hostRelationWatchFlag.set(value);
    }

    private void dispatchEvent(ResourceEvent<HostRelationEventDetail> event) {
        HostTopoDTO hostTopoDTO = HostTopoDTO.fromHostRelationEvent(event.getDetail());
        Long appId = hostTopoDTO.getBizId();
        List<AppHostRelationEventsHandler> idleHandlerList = new ArrayList<>();
        for (AppHostRelationEventsHandler handler : eventsHandlerList) {
            if (appId.equals(handler.getAppId())) {
                handler.commitEvent(appId, event);
                return;
            } else if (handler.isIdle()) {
                idleHandlerList.add(handler);
            }
        }
        if (!idleHandlerList.isEmpty()) {
            AppHostRelationEventsHandler handler = idleHandlerList.get((int) (Math.random() * idleHandlerList.size()));
            handler.commitEvent(appId, event);
        } else if (eventsHandlerList.size() < MAX_HANDLER_NUM) {
            AppHostRelationEventsHandler handler = new AppHostRelationEventsHandler(appHostRelationEventQueue);
            handler.setName("AppHostRelationEventsHandler-" + (eventsHandlerList.size() + 1));
            handler.start();
            eventsHandlerList.add(handler);
            handler.commitEvent(appId, event);
        } else {
            AppHostRelationEventsHandler handler =
                eventsHandlerList.get((int) (Math.random() * eventsHandlerList.size()));
            handler.commitEvent(appId, event);
        }
    }

    private void handleOneEvent(ResourceEvent<HostRelationEventDetail> event) {
        HostTopoDTO hostTopoDTO = HostTopoDTO.fromHostRelationEvent(event.getDetail());
        Long appId = hostTopoDTO.getBizId();
        try {
            StopWatch watch = new StopWatch();
            watch.start("handleOneEventIndeed");
            handleOneEventIndeed(event);
            watch.stop();
            if (watch.getTotalTimeMillis() > 3000) {
                log.warn("PERF:SLOW:handle hostRelationEvent:" + watch.prettyPrint());
            } else {
                log.debug("handle hostRelationEvent:" + watch.prettyPrint());
            }
        } catch (Throwable t) {
            log.error(String.format("Fail to handle hostRelationEvent of appId %d, event:%s", appId, event), t);
        }
    }

    /**
     * 将主机拓扑表中的拓扑数据同步至主机表
     *
     * @param hostTopoDTO 主机拓扑信息
     */
    private void updateTopoToHost(HostTopoDTO hostTopoDTO) {
        // 若主机存在需将拓扑信息同步至主机信息冗余字段
        long affectedNum = applicationHostDAO.syncHostTopo(dslContext, hostTopoDTO.getHostId());
        if (affectedNum == 0) {
            log.info("no host topo synced");
        } else if (affectedNum < 0) {
            FormattingTuple msg = MessageFormatter.format(
                "cannot find hostInfo by hostId:{}, wait for host event or sync",
                hostTopoDTO.getHostId()
            );
            log.warn(msg.getMessage());
        }
    }

    /**
     * 当主机关系被创建时，更新缓存中的主机信息
     *
     * @param hostTopoDTO 主机拓扑信息
     */
    private void updateHostCacheWhenRelCreated(HostTopoDTO hostTopoDTO) {
        ApplicationHostDTO host = applicationHostDAO.getHostById(hostTopoDTO.getHostId());
        if (host != null && applicationService.existBiz(host.getBizId())) {
            hostCache.addOrUpdateHost(host);
        }
    }

    /**
     * 当主机关系被删除时，更新缓存中的主机信息
     *
     * @param hostTopoDTO 主机拓扑信息
     */
    private void updateHostCacheWhenRelationDeleted(HostTopoDTO hostTopoDTO) {
        ApplicationHostDTO host = applicationHostDAO.getHostById(hostTopoDTO.getHostId());
        if (host == null) {
            return;
        }
        int curAppRelationCount = hostTopoDAO.countHostTopo(
            dslContext, hostTopoDTO.getBizId(), hostTopoDTO.getHostId()
        );
        int hostRelationCount = hostTopoDAO.countHostTopo(
            dslContext, null, hostTopoDTO.getHostId()
        );
        if (curAppRelationCount != 0) {
            return;
        }
        if (hostRelationCount == 0) {
            // 主机被移除
            hostCache.deleteHost(host);
        } else {
            // 主机被转移到其他业务下
            if (applicationService.existBiz(host.getBizId())) {
                hostCache.addOrUpdateHost(host);
            }
        }
    }

    private void handleOneEventIndeed(ResourceEvent<HostRelationEventDetail> event) {
        String eventType = event.getEventType();
        HostTopoDTO hostTopoDTO = HostTopoDTO.fromHostRelationEvent(event.getDetail());
        switch (eventType) {
            case ResourceWatchReq.EVENT_TYPE_CREATE:
                // 插入拓扑数据
                hostTopoDAO.insertHostTopo(dslContext, hostTopoDTO);
                // 同步拓扑数据至主机表冗余字段
                updateTopoToHost(hostTopoDTO);
                // 更新主机缓存
                updateHostCacheWhenRelCreated(hostTopoDTO);
                break;
            case ResourceWatchReq.EVENT_TYPE_UPDATE:
                log.warn("Unexpected event:hostRelation Update");
                break;
            case ResourceWatchReq.EVENT_TYPE_DELETE:
                // 删除拓扑数据
                hostTopoDAO.deleteHostTopo(
                    dslContext,
                    hostTopoDTO.getHostId(),
                    hostTopoDTO.getBizId(),
                    hostTopoDTO.getSetId(),
                    hostTopoDTO.getModuleId()
                );
                // 同步拓扑数据至主机表冗余字段
                updateTopoToHost(hostTopoDTO);
                // 更新主机缓存
                updateHostCacheWhenRelationDeleted(hostTopoDTO);
                break;
            default:
                break;
        }
    }

    public String handleHostRelationWatchResult(ResourceWatchResult<HostRelationEventDetail> hostRelationWatchResult) {
        String cursor = null;
        boolean isWatched = hostRelationWatchResult.getWatched();
        if (isWatched) {
            List<ResourceEvent<HostRelationEventDetail>> events = hostRelationWatchResult.getEvents();
            //解析事件，进行处理
            for (ResourceEvent<HostRelationEventDetail> event : events) {
                dispatchEvent(event);
            }
            if (events.size() > 0) {
                log.info("events.size={},events={}", events.size(), JsonUtils.toJson(events));
                cursor = events.get(events.size() - 1).getCursor();
            } else {
                log.warn("Unexpected:events.size==0");
            }
        } else {
            // 只有一个无实际意义的事件，用于换取bk_cursor
            List<ResourceEvent<HostRelationEventDetail>> events = hostRelationWatchResult.getEvents();
            if (events != null && events.size() > 0) {
                cursor = events.get(0).getCursor();
            } else {
                log.warn("CMDB event error:no refresh event data when watched==false");
            }
        }
        return cursor;
    }

    @Override
    public void run() {
        log.info("hostRelationWatch arranged");
        init();
        while (true) {
            String cursor = null;
            long startTime = System.currentTimeMillis() / 1000 - 10 * 60;
            try {
                boolean lockGotten = LockUtils.tryGetDistributedLock(REDIS_KEY_RESOURCE_WATCH_HOST_RELATION_JOB_LOCK,
                    machineIp, 50);
                if (!lockGotten) {
                    ThreadUtils.sleep(100);
                    continue;
                }
                String runningMachine =
                    redisTemplate.opsForValue().get(REDIS_KEY_RESOURCE_WATCH_HOST_RELATION_JOB_RUNNING_MACHINE);
                if (StringUtils.isNotBlank(runningMachine)) {
                    //已有同步线程在跑，不再同步
                    log.info("hostRelationWatch thread already running on {}", runningMachine);
                    ThreadUtils.sleep(30000);
                    continue;
                }
                // 开一个心跳子线程，维护当前机器正在WatchResource的状态
                RedisKeyHeartBeatThread hostRelationWatchRedisKeyHeartBeatThread = new RedisKeyHeartBeatThread(
                    redisTemplate,
                    REDIS_KEY_RESOURCE_WATCH_HOST_RELATION_JOB_RUNNING_MACHINE,
                    machineIp,
                    3000L,
                    2000L
                );
                hostRelationWatchRedisKeyHeartBeatThread.setName("hostRelationWatchRedisKeyHeartBeatThread");
                hostRelationWatchRedisKeyHeartBeatThread.start();
                log.info("start watch hostRelation at {},{}", TimeUtil.getCurrentTimeStr("HH:mm:ss"),
                    System.currentTimeMillis());
                StopWatch watch = new StopWatch("hostRelationWatch");
                watch.start("total");
                try {
                    IBizCmdbClient bizCmdbClient = CmdbClientFactory.getCmdbClient();
                    ResourceWatchResult<HostRelationEventDetail> hostRelationWatchResult;
                    while (hostRelationWatchFlag.get()) {
                        if (cursor == null) {
                            hostRelationWatchResult = bizCmdbClient.getHostRelationEvents(startTime, cursor);
                        } else {
                            hostRelationWatchResult = bizCmdbClient.getHostRelationEvents(null, cursor);
                        }
                        log.info("hostRelationWatchResult={}", JsonUtils.toJson(hostRelationWatchResult));
                        cursor = handleHostRelationWatchResult(hostRelationWatchResult);
                        // 1s/watch一次
                        ThreadUtils.sleep(1000);
                    }
                } catch (Throwable t) {
                    log.error("hostRelationWatch thread fail", t);
                } finally {
                    hostRelationWatchRedisKeyHeartBeatThread.setRunFlag(false);
                    watch.stop();
                    log.info("hostRelationWatch time consuming:" + watch.toString());
                }
            } catch (Throwable t) {
                log.error("HostRelationWatchThread quit unexpectedly", t);
            } finally {
                do {
                    // 5s/重试一次
                    ThreadUtils.sleep(5000);
                } while (!hostRelationWatchFlag.get());
            }
        }
    }

    /**
     * 处理同一个业务的多个事件
     */
    class AppHostRelationEventsHandler extends EventsHandler<HostRelationEventDetail> {

        public AppHostRelationEventsHandler(BlockingQueue<ResourceEvent<HostRelationEventDetail>> queue) {
            super(queue);
        }

        @Override
        void handleEvent(ResourceEvent<HostRelationEventDetail> event) {
            handleOneEvent(event);
        }
    }
}
