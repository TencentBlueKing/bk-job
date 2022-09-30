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

import com.tencent.bk.job.common.cc.model.result.HostEventDetail;
import com.tencent.bk.job.common.cc.model.result.ResourceEvent;
import com.tencent.bk.job.common.cc.model.result.ResourceWatchResult;
import com.tencent.bk.job.common.cc.sdk.CmdbClientFactory;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.gse.service.QueryAgentStatusClient;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.redis.util.RedisKeyHeartBeatThread;
import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.manager.host.HostCache;
import com.tencent.bk.job.manage.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StopWatch;

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

    private final ApplicationService applicationService;
    private final ApplicationHostDAO applicationHostDAO;
    private final QueryAgentStatusClient queryAgentStatusClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final HostCache hostCache;
    private final String REDIS_KEY_RESOURCE_WATCH_HOST_JOB_RUNNING_MACHINE = "resource-watch-host-job-running-machine";
    private final HostEventsHandler eventsHandler;
    private final BlockingQueue<ResourceEvent<HostEventDetail>> appHostEventQueue = new LinkedBlockingQueue<>(10000);
    private final AtomicBoolean hostWatchFlag = new AtomicBoolean(true);
    private String cursor = null;

    public HostWatchThread(ApplicationService applicationService,
                           ApplicationHostDAO applicationHostDAO,
                           QueryAgentStatusClient queryAgentStatusClient,
                           RedisTemplate<String, String> redisTemplate,
                           HostCache hostCache) {
        this.applicationService = applicationService;
        this.applicationHostDAO = applicationHostDAO;
        this.queryAgentStatusClient = queryAgentStatusClient;
        this.redisTemplate = redisTemplate;
        this.hostCache = hostCache;
        this.setName("[" + getId() + "]-HostWatchThread-" + instanceNum.getAndIncrement());
        this.eventsHandler = buildHostEventsHandler();
        this.eventsHandler.setName("[" + eventsHandler.getId() + "]-HostEventsHandler");
    }

    private HostEventsHandler buildHostEventsHandler() {
        return new HostEventsHandler(
            appHostEventQueue,
            applicationService,
            applicationHostDAO,
            queryAgentStatusClient,
            hostCache
        );
    }

    private void init() {
        eventsHandler.start();
    }

    public void setWatchFlag(boolean value) {
        hostWatchFlag.set(value);
    }

    private void dispatchEvent(ResourceEvent<HostEventDetail> event) {
        ApplicationHostDTO hostInfoDTO = HostEventDetail.toHostInfoDTO(event.getDetail());
        Long hostId = hostInfoDTO.getHostId();
        ApplicationHostDTO oldHostInfoDTO = applicationHostDAO.getHostById(hostId);
        eventsHandler.commitEvent(oldHostInfoDTO == null ? null : oldHostInfoDTO.getBizId(), event);
    }

    public String handleHostWatchResult(ResourceWatchResult<HostEventDetail> hostWatchResult) {
        String cursor = null;
        boolean isWatched = hostWatchResult.getWatched();
        List<ResourceEvent<HostEventDetail>> events = hostWatchResult.getEvents();
        if (isWatched) {
            //解析事件，进行处理
            for (ResourceEvent<HostEventDetail> event : events) {
                dispatchEvent(event);
            }
            if (events.size() > 0) {
                log.info("events.size={}", events.size());
                cursor = events.get(events.size() - 1).getCursor();
                log.info("refresh cursor(success):{}", cursor);
            } else {
                log.info("events.size==0");
            }
        } else {
            // 只有一个无实际意义的事件，用于换取bk_cursor
            if (events != null && events.size() > 0) {
                cursor = events.get(0).getCursor();
                log.info("refresh cursor(fail):{}", cursor);
            } else {
                log.warn("CMDB event error:no refresh event data when watched==false");
            }
        }
        return cursor;
    }

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        log.info("hostWatch arranged");
        init();
        while (true) {
            // 从10分钟前开始watch
            long startTime = System.currentTimeMillis() / 1000 - 10 * 60;
            try {
                if (!getRedisLockOrWait100ms()) {
                    continue;
                }
                String runningMachine =
                    redisTemplate.opsForValue().get(REDIS_KEY_RESOURCE_WATCH_HOST_JOB_RUNNING_MACHINE);
                if (StringUtils.isNotBlank(runningMachine)) {
                    //已有hostWatch线程在跑，不再重复Watch
                    log.info("hostWatch thread already running on {}", runningMachine);
                    ThreadUtils.sleep(30000);
                    continue;
                }
                // 开一个心跳子线程，维护当前机器正在WatchResource的状态
                RedisKeyHeartBeatThread hostWatchRedisKeyHeartBeatThread = startRedisKeyHeartBeatThread();
                log.info("start watch host resource at {},{}", TimeUtil.getCurrentTimeStr("HH:mm:ss"),
                    System.currentTimeMillis());
                StopWatch watch = new StopWatch("hostWatch");
                watch.start("total");
                try {
                    watchInLoop(startTime);
                } catch (Throwable t) {
                    log.error("hostWatch thread fail", t);
                } finally {
                    hostWatchRedisKeyHeartBeatThread.setRunFlag(false);
                    watch.stop();
                    log.info("hostWatch time consuming:" + watch.toString());
                }
            } catch (Throwable t) {
                log.error("HostWatchThread quit unexpectedly", t);
            } finally {
                waitUtilHostWatchFlagSet();
            }
        }
    }

    private boolean getRedisLockOrWait100ms() {
        boolean lockGotten = LockUtils.tryGetDistributedLock(REDIS_KEY_RESOURCE_WATCH_HOST_JOB_LOCK,
            machineIp, 50);
        if (!lockGotten) {
            log.info("hostWatch lock not gotten, wait 100ms");
            ThreadUtils.sleep(100);
        }
        return lockGotten;
    }

    private RedisKeyHeartBeatThread startRedisKeyHeartBeatThread() {
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
        return hostWatchRedisKeyHeartBeatThread;
    }

    private void watchInLoop(long startTime) {
        IBizCmdbClient bizCmdbClient = CmdbClientFactory.getCmdbClient();
        ResourceWatchResult<HostEventDetail> hostWatchResult;
        while (hostWatchFlag.get()) {
            if (cursor == null) {
                log.info("Start watch from startTime:{}", TimeUtil.formatTime(startTime * 1000));
                hostWatchResult = bizCmdbClient.getHostEvents(startTime, null);
            } else {
                hostWatchResult = bizCmdbClient.getHostEvents(null, cursor);
            }
            log.info("hostWatchResult={}", JsonUtils.toJson(hostWatchResult));
            cursor = handleHostWatchResult(hostWatchResult);
            // 1s/watch一次
            ThreadUtils.sleep(1000);
        }
    }

    private void waitUtilHostWatchFlagSet() {
        do {
            // 5s/重试一次
            ThreadUtils.sleep(5000);
        } while (!hostWatchFlag.get());
    }
}
