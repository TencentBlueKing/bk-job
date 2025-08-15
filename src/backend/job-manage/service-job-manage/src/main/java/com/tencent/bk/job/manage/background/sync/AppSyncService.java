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

import com.tencent.bk.job.common.paas.model.OpenApiTenant;
import com.tencent.bk.job.common.paas.user.IUserApiClient;
import com.tencent.bk.job.common.redis.util.HeartBeatRedisLock;
import com.tencent.bk.job.common.redis.util.HeartBeatRedisLockConfig;
import com.tencent.bk.job.common.redis.util.LockResult;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.manage.background.sync.tenantset.ITenantSetSyncService;
import com.tencent.bk.job.manage.config.JobManageConfig;
import com.tencent.bk.job.manage.manager.app.ApplicationCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Job业务对应的CMDB资源（业务、业务集、租户集）同步服务
 */
@SuppressWarnings("FieldCanBeLocal")
@Slf4j
@Service
public class AppSyncService {

    private static final String machineIp = IpUtils.getFirstMachineIP();

    private final ThreadPoolExecutor syncAppExecutor;
    private final RedisTemplate<String, String> redisTemplate;
    private final String REDIS_KEY_SYNC_APP_MACHINE = "sync-app-machine";
    private volatile boolean enableSyncApp;
    private final ApplicationCache applicationCache;
    private final BizSyncService bizSyncService;
    private final BizSetSyncService bizSetSyncService;
    private final ITenantSetSyncService tenantSetSyncService;
    private final IUserApiClient userMgrApiClient;

    @Autowired
    public AppSyncService(BizSyncService bizSyncService,
                          BizSetSyncService bizSetSyncService,
                          ITenantSetSyncService tenantSetSyncService,
                          JobManageConfig jobManageConfig,
                          RedisTemplate<String, String> redisTemplate,
                          ApplicationCache applicationCache,
                          @Qualifier("syncAppExecutor")
                          ThreadPoolExecutor syncAppExecutor,
                          IUserApiClient userMgrApiClient) {
        this.redisTemplate = redisTemplate;
        this.enableSyncApp = jobManageConfig.isEnableSyncApp();
        this.applicationCache = applicationCache;
        this.bizSyncService = bizSyncService;
        this.bizSetSyncService = bizSetSyncService;
        this.tenantSetSyncService = tenantSetSyncService;
        // 同步业务的线程池配置
        this.syncAppExecutor = syncAppExecutor;
        this.userMgrApiClient = userMgrApiClient;
    }

    public Boolean syncApp() {
        if (!enableSyncApp) {
            log.info("syncApp not enabled, skip, you can enable it in config file");
            return false;
        }
        log.info("syncApp arranged");
        HeartBeatRedisLockConfig config = HeartBeatRedisLockConfig.getDefault();
        config.setHeartBeatThreadName("SyncAppRedisKeyHeartBeatThread");
        config.setExpireTimeMillis(5000L);
        config.setPeriodMillis(4000L);
        HeartBeatRedisLock lock = new HeartBeatRedisLock(
            redisTemplate,
            REDIS_KEY_SYNC_APP_MACHINE,
            machineIp,
            config
        );
        String lockKeyValue = lock.peekLockKeyValue();
        if (StringUtils.isNotBlank(lockKeyValue)) {
            //已有同步线程在跑，不再同步
            log.info("syncApp thread already running on {}", lockKeyValue);
            return false;
        }
        syncAppExecutor.submit(() -> tryToSyncApp(lock));
        return true;
    }

    private void tryToSyncApp(HeartBeatRedisLock lock) {
        LockResult lockResult = null;
        StopWatch watch = new StopWatch("syncApp");
        watch.start("total");
        try {
            lockResult = lock.lock();
            if (!lockResult.isLockGotten()) {
                //已有同步线程在跑，不再同步
                log.info("syncApp thread already running on {}", lockResult.getLockValue());
                return;
            }
            doSyncApp();
        } catch (Throwable t) {
            log.error("Fail to syncApp", t);
        } finally {
            applicationCache.refreshCache();
            if (watch.isRunning()) {
                watch.stop();
            }
            if (lockResult != null) {
                lockResult.tryToRelease();
            }
            log.info("syncAppFinish: timeConsuming={}", watch.prettyPrint());
        }
    }

    private void doSyncApp() {
        log.info("startSyncApp at {}", TimeUtil.getCurrentTimeStrWithMs());
        List<OpenApiTenant> tenantList = userMgrApiClient.listAllTenant();
        tryToSyncTenantSetFromCMDB();
        // 遍历所有租户
        for (OpenApiTenant openApiTenant : tenantList) {
            // 从CMDB同步业务信息
            tryToSyncBizFromCMDB(openApiTenant.getId());
            // 从CMDB同步业务集信息
            tryToSyncBizSetFromCMDB(openApiTenant.getId());
        }
    }

    /**
     * 尝试从CMDB同步租户集信息
     */
    private void tryToSyncTenantSetFromCMDB() {
        try {
            tenantSetSyncService.syncTenantSetFromCMDB();
        } catch (Throwable t) {
            log.error("Fail to syncTenantSetFromCMDB", t);
        }
    }

    /**
     * 尝试从CMDB同步业务信息
     *
     * @param tenantId 租户ID
     */
    private void tryToSyncBizFromCMDB(String tenantId) {
        try {
            bizSyncService.syncBizFromCMDB(tenantId);
        } catch (Throwable t) {
            String message = MessageFormatter.format(
                "Fail to syncBizFromCMDB, tenantId={}",
                tenantId
            ).getMessage();
            log.error(message, t);
        }
    }

    /**
     * 尝试从CMDB同步业务集信息
     *
     * @param tenantId 租户ID
     */
    private void tryToSyncBizSetFromCMDB(String tenantId) {
        try {
            bizSetSyncService.syncBizSetFromCMDB(tenantId);
        } catch (Throwable t) {
            String message = MessageFormatter.format(
                "Fail to syncBizSetFromCMDB, tenantId={}",
                tenantId
            ).getMessage();
            log.error(message, t);
        }
    }

    public Boolean enableSyncApp() {
        enableSyncApp = true;
        return true;
    }

    public Boolean disableSyncApp() {
        enableSyncApp = false;
        return true;
    }

}
