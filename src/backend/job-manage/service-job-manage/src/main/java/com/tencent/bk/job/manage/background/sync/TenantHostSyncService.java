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

import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.BasicHostDTO;
import com.tencent.bk.job.common.redis.util.HeartBeatRedisLock;
import com.tencent.bk.job.common.redis.util.HeartBeatRedisLockConfig;
import com.tencent.bk.job.common.redis.util.LockResult;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.task.ClearNotInCmdbHostsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 单个租户的主机同步逻辑
 */
@SuppressWarnings("FieldCanBeLocal")
@Slf4j
@Service
public class TenantHostSyncService {

    private final String REDIS_KEY_SYNC_HOST_MACHINE_PREFIX = "sync-host-machine:";

    private static final String machineIp = IpUtils.getFirstMachineIP();

    private final ApplicationDAO applicationDAO;
    private final ClearNotInCmdbHostsService clearNotInCmdbHostsService;
    private final BizHostSyncService bizHostSyncService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ThreadPoolExecutor syncHostExecutor;

    @Autowired
    public TenantHostSyncService(ApplicationDAO applicationDAO,
                                 ClearNotInCmdbHostsService clearNotInCmdbHostsService,
                                 BizHostSyncService bizHostSyncService,
                                 RedisTemplate<String, String> redisTemplate,
                                 @Qualifier("syncHostExecutor")
                                 ThreadPoolExecutor syncHostExecutor) {
        this.applicationDAO = applicationDAO;
        this.clearNotInCmdbHostsService = clearNotInCmdbHostsService;
        this.bizHostSyncService = bizHostSyncService;
        this.redisTemplate = redisTemplate;
        this.syncHostExecutor = syncHostExecutor;
    }

    /**
     * 立即同步租户下所有业务的主机
     *
     * @param tenantId 租户ID
     */
    public void syncAllBizHostsAtOnce(String tenantId) {
        log.info("syncAllBizHosts: tenantId={}", tenantId);
        List<ApplicationDTO> appList = applicationDAO.listAllAppsForTenant(tenantId);
        List<ApplicationDTO> failedAppList = new ArrayList<>();
        for (ApplicationDTO applicationDTO : appList) {
            Triple<Set<BasicHostDTO>, Long, Long> triple = bizHostSyncService.syncBizHostsAtOnce(applicationDTO);
            if (triple == null) {
                failedAppList.add(applicationDTO);
            }
        }
        if (!failedAppList.isEmpty()) {
            log.warn(
                "syncAllBizHosts: tenantId={}, failedAppList={}",
                tenantId,
                failedAppList.stream().map(
                    applicationDTO -> "(" + applicationDTO.getBizIdIfBizApp() + "," + applicationDTO.getName() + ")"
                ).collect(Collectors.toList())
            );
        }
        log.info("syncAllBizHosts end: tenantId={}", tenantId);
    }

    @NewSpan
    public void addSyncHostTaskIfNotExist(String tenantId) {
        log.info("syncHost(tenantId={}) arranged", tenantId);
        HeartBeatRedisLockConfig config = HeartBeatRedisLockConfig.getDefault();
        config.setHeartBeatThreadName("SyncHostRedisKeyHeartBeatThread-" + tenantId);
        config.setExpireTimeMillis(5000L);
        config.setPeriodMillis(4000L);
        HeartBeatRedisLock lock = new HeartBeatRedisLock(
            redisTemplate,
            REDIS_KEY_SYNC_HOST_MACHINE_PREFIX + tenantId,
            machineIp,
            config
        );
        String lockKeyValue = lock.peekLockKeyValue();
        if (StringUtils.isNotBlank(lockKeyValue)) {
            //已有同步线程在跑，不再同步
            log.info("syncHost(tenantId={}) thread already running on {}", tenantId, lockKeyValue);
            return;
        }
        tryToSyncTenantHostWithLock(tenantId, lock);
    }

    private Future<Triple<Set<BasicHostDTO>, Long, Long>> arrangeSyncBizHostsTask(ApplicationDTO bizApp) {
        return syncHostExecutor.submit(() ->
            bizHostSyncService.syncBizHostsAtOnce(bizApp)
        );
    }

    private void tryToSyncTenantHostWithLock(String tenantId, HeartBeatRedisLock lock) {
        LockResult lockResult = null;
        try {
            lockResult = lock.lock();
            if (!lockResult.isLockGotten()) {
                //已有同步线程在跑，不再同步
                log.info("syncHost(tenantId={}) thread already running on {}", tenantId, lockResult.getLockValue());
                return;
            }
            log.info("start syncHost(tenantId={}) at {}", tenantId, TimeUtil.getCurrentTimeStrWithMs());
            doSyncTenantHost(tenantId);
        } catch (Throwable t) {
            String message = MessageFormatter.format(
                "syncHost(tenantId={}) thread fail",
                tenantId
            ).getMessage();
            log.error(message, t);
        } finally {
            if (lockResult != null) {
                lockResult.tryToRelease();
            }
        }
    }

    private void doSyncTenantHost(String tenantId) {
        StopWatch watch = new StopWatch("syncHost");
        watch.start("total");
        List<ApplicationDTO> localBizApps = applicationDAO.listAllBizApps(tenantId);
        Set<Long> localBizAppIds = localBizApps.stream().map(ApplicationDTO::getId).collect(Collectors.toSet());
        log.info("localBizAppIds={}", StringUtil.concatCollection(localBizAppIds));
        long cmdbInterfaceTime = 0L;
        long writeToDBTime = 0L;
        List<Pair<ApplicationDTO, Future<Triple<Set<BasicHostDTO>, Long, Long>>>> bizAppFutureList =
            new ArrayList<>();
        int failedBizNum = 0;
        for (ApplicationDTO bizApp : localBizApps) {
            Future<Triple<Set<BasicHostDTO>, Long, Long>> future = arrangeSyncBizHostsTask(bizApp);
            bizAppFutureList.add(Pair.of(bizApp, future));
        }
        for (Pair<ApplicationDTO, Future<Triple<Set<BasicHostDTO>, Long, Long>>> bizAppFuture :
            bizAppFutureList) {
            ApplicationDTO bizApp = bizAppFuture.getFirst();
            Future<Triple<Set<BasicHostDTO>, Long, Long>> future = bizAppFuture.getSecond();
            try {
                Triple<Set<BasicHostDTO>, Long, Long> timeConsumingPair = future.get(30, TimeUnit.MINUTES);
                cmdbInterfaceTime += timeConsumingPair.getMiddle();
                writeToDBTime += timeConsumingPair.getRight();
            } catch (Throwable t) {
                log.error("syncHost of biz fail:bizId=" + bizApp.getBizIdIfBizApp(), t);
                failedBizNum += 1;
            }
        }
        // 删除CMDB中不存在的主机
        clearNotInCmdbHostsService.clearHostNotInCmdb(tenantId);
        if (failedBizNum == 0) {
            log.info(
                "syncHostFinish: bizNum={}, failedBizNum={}, " +
                    "cmdbInterfaceTime={}ms, writeToDBTime={}ms",
                localBizApps.size(),
                failedBizNum,
                cmdbInterfaceTime,
                writeToDBTime
            );
        } else {
            log.warn(
                "syncHostFinish: bizNum={}, failedBizNum={}, " +
                    "cmdbInterfaceTime={}ms, writeToDBTime={}ms",
                localBizApps.size(),
                failedBizNum,
                cmdbInterfaceTime,
                writeToDBTime
            );
        }
    }

}
