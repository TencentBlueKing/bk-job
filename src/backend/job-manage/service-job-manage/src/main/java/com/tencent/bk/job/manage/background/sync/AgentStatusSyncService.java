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

package com.tencent.bk.job.manage.background.sync;

import com.tencent.bk.job.common.model.dto.HostSimpleDTO;
import com.tencent.bk.job.common.redis.util.HeartBeatRedisLock;
import com.tencent.bk.job.common.redis.util.HeartBeatRedisLockConfig;
import com.tencent.bk.job.common.redis.util.LockResult;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.manage.config.JobManageConfig;
import com.tencent.bk.job.manage.dao.NoTenantHostDAO;
import com.tencent.bk.job.manage.service.host.NoTenantHostService;
import com.tencent.bk.job.manage.service.impl.agent.AgentStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.List;

/**
 * 机器Agent状态同步逻辑
 */
@Slf4j
@Service
public class AgentStatusSyncService {

    private static final String machineIp = IpUtils.getFirstMachineIP();
    private volatile boolean enableSyncAgentStatus;

    @SuppressWarnings("FieldCanBeLocal")
    private final String REDIS_KEY_SYNC_AGENT_STATUS_MACHINE = "sync-agent-status-machine";
    private final NoTenantHostDAO noTenantHostDAO;
    private final NoTenantHostService noTenantHostService;
    private final AgentStatusService agentStatusService;
    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public AgentStatusSyncService(JobManageConfig jobManageConfig,
                                  NoTenantHostDAO noTenantHostDAO,
                                  NoTenantHostService noTenantHostService,
                                  AgentStatusService agentStatusService,
                                  RedisTemplate<String, String> redisTemplate) {
        this.noTenantHostDAO = noTenantHostDAO;
        this.noTenantHostService = noTenantHostService;
        this.agentStatusService = agentStatusService;
        this.redisTemplate = redisTemplate;
        this.enableSyncAgentStatus = jobManageConfig.isEnableSyncAgentStatus();
    }

    public void syncAgentStatus() {
        if (!enableSyncAgentStatus) {
            log.info("syncAgentStatus not enabled, skip, you can enable it in config file");
            return;
        }
        log.info("syncAgentStatus arranged");
        HeartBeatRedisLockConfig config = HeartBeatRedisLockConfig.getDefault();
        config.setHeartBeatThreadName("SyncAppRedisKeyHeartBeatThread");
        config.setExpireTimeMillis(5000L);
        config.setPeriodMillis(4000L);
        HeartBeatRedisLock lock = new HeartBeatRedisLock(
            redisTemplate,
            REDIS_KEY_SYNC_AGENT_STATUS_MACHINE,
            machineIp,
            config
        );
        LockResult lockResult = lock.lock();
        if (!lockResult.isLockGotten()) {
            //已有同步线程在跑，不再同步
            log.info("syncAgentStatus thread already running on {}", lockResult.getLockValue());
            return;
        }
        tryToSyncAgentStatus(lockResult);
    }

    private void tryToSyncAgentStatus(LockResult lockResult) {
        StopWatch watch = new StopWatch("syncAgentStatus");
        watch.start("total");
        try {
            // 从GSE同步Agent状态
            syncAgentStatusFromGSE();
        } catch (Throwable t) {
            log.error("Fail to syncAgentStatus", t);
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
            if (lockResult != null) {
                lockResult.tryToRelease();
            }
            log.info("syncAgentStatusFinish: timeConsuming={}", watch.prettyPrint());
        }
    }

    private Pair<Long, Long> syncHostAgentStatus() {
        long gseInterfaceTimeConsuming = 0L;
        long writeToDBTimeConsuming = 0L;
        StopWatch hostAgentStatusWatch = new StopWatch();
        hostAgentStatusWatch.start("listAllHostInfo");
        List<HostSimpleDTO> localHosts = noTenantHostDAO.listAllHostSimpleInfo();
        hostAgentStatusWatch.stop();
        hostAgentStatusWatch.start("getAgentStatus from GSE");
        long startTime = System.currentTimeMillis();
        List<HostSimpleDTO> statusChangedHosts = agentStatusService.findStatusChangedHosts(localHosts);
        gseInterfaceTimeConsuming += (System.currentTimeMillis() - startTime);
        hostAgentStatusWatch.stop();
        hostAgentStatusWatch.start("updateHosts to local DB");
        startTime = System.currentTimeMillis();
        int updatedHostNum = noTenantHostService.updateHostsStatus(statusChangedHosts);
        writeToDBTimeConsuming += (System.currentTimeMillis() - startTime);
        hostAgentStatusWatch.stop();
        if (hostAgentStatusWatch.getTotalTimeMillis() > 180000) {
            log.warn(
                "syncHostAgentStatus too slow, totalHosts={}, statusChangedHosts={}, "
                    + "updatedHostNum={}, timeConsume={}",
                localHosts.size(),
                statusChangedHosts.size(),
                updatedHostNum,
                hostAgentStatusWatch.prettyPrint()
            );
        }
        if (statusChangedHosts.size() == updatedHostNum) {
            log.info(
                "syncHostAgentStatus Performance,totalHosts={}, " +
                    "statusChangedHosts={}, updatedHostNum={}, timeConsume={}",
                localHosts.size(),
                statusChangedHosts.size(),
                updatedHostNum,
                hostAgentStatusWatch
            );
        } else {
            log.warn(
                "syncHostAgentStatus Performance,totalHosts={}, " +
                    "statusChangedHosts={}, updatedHostNum={}, timeConsume={}",
                localHosts.size(),
                statusChangedHosts.size(),
                updatedHostNum,
                hostAgentStatusWatch
            );
        }
        return Pair.of(gseInterfaceTimeConsuming, writeToDBTimeConsuming);
    }

    private void syncAgentStatusFromGSE() {
        log.info(Thread.currentThread().getName() + ":begin to sync agentStatus from GSE");
        long gseInterfaceTimeConsuming = 0L;
        long writeToDBTimeConsuming = 0L;
        Pair<Long, Long> timeConsumingPair = syncHostAgentStatus();
        gseInterfaceTimeConsuming += timeConsumingPair.getFirst();
        writeToDBTimeConsuming += timeConsumingPair.getSecond();
        log.info(
            "syncAgentStatusFinish: gseInterfaceTimeConsuming={}ms, writeToDBTimeConsuming={}ms",
            gseInterfaceTimeConsuming,
            writeToDBTimeConsuming
        );
    }

    public Boolean enableSyncAgentStatus() {
        enableSyncAgentStatus = true;
        return true;
    }

    public Boolean disableSyncAgentStatus() {
        enableSyncAgentStatus = false;
        return true;
    }
}
