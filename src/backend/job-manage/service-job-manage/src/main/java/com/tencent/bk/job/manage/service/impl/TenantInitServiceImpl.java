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

package com.tencent.bk.job.manage.service.impl;

import com.tencent.bk.job.common.constant.TenantIdConstants;
import com.tencent.bk.job.common.redis.util.DistributedUniqueTask;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.background.ha.BackGroundTaskDaemon;
import com.tencent.bk.job.manage.background.sync.BizSetSyncService;
import com.tencent.bk.job.manage.background.sync.BizSyncService;
import com.tencent.bk.job.manage.background.sync.TenantHostSyncService;
import com.tencent.bk.job.manage.background.sync.tenantset.ITenantSetSyncService;
import com.tencent.bk.job.manage.model.dto.TenantInitDoneMark;
import com.tencent.bk.job.manage.service.TenantInitResult;
import com.tencent.bk.job.manage.service.TenantInitService;
import com.tencent.bk.job.manage.service.impl.notify.NotifyChannelInitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;

@Slf4j
@Service
public class TenantInitServiceImpl implements TenantInitService {

    private static final String machineIp = IpUtils.getFirstMachineIP();
    /**
     * 租户初始化分布式锁的Key前缀，真实的锁Key为job:util:lock:initTenant-{tenantId}。
     * 该值必须保持稳定，否则升级期间新老版本实例会抢到不同的锁而并发执行初始化。
     */
    private static final String REDIS_KEY_INIT_TENANT_PREFIX = "initTenant-";
    /**
     * 租户初始化完成标记的Key前缀，标记永不过期，value为{@link TenantInitDoneMark}的JSON串，
     * 记录执行初始化的实例IP与初始化的开始、结束时间。
     * 标记仅代表「自动初始化任务成功执行过」：通过OP接口人工初始化不会写入标记，
     * 因此标记不存在并不等价于租户未初始化，部署验收时需结合业务/主机数据是否非空一并判断。
     */
    private static final String REDIS_KEY_TENANT_INIT_DONE_PREFIX = "job:manage:tenant:init:done:";

    private final RedisTemplate<String, String> redisTemplate;
    private final BizSyncService bizSyncService;
    private final BizSetSyncService bizSetSyncService;
    private final ITenantSetSyncService tenantSetSyncService;
    private final TenantHostSyncService tenantHostSyncService;
    private final NotifyChannelInitService notifyChannelInitService;
    private final BackGroundTaskDaemon backGroundTaskDaemon;

    @Autowired
    public TenantInitServiceImpl(RedisTemplate<String, String> redisTemplate,
                                 BizSyncService bizSyncService,
                                 BizSetSyncService bizSetSyncService,
                                 ITenantSetSyncService tenantSetSyncService,
                                 TenantHostSyncService tenantHostSyncService,
                                 NotifyChannelInitService notifyChannelInitService,
                                 BackGroundTaskDaemon backGroundTaskDaemon) {
        this.redisTemplate = redisTemplate;
        this.bizSyncService = bizSyncService;
        this.bizSetSyncService = bizSetSyncService;
        this.tenantSetSyncService = tenantSetSyncService;
        this.tenantHostSyncService = tenantHostSyncService;
        this.notifyChannelInitService = notifyChannelInitService;
        this.backGroundTaskDaemon = backGroundTaskDaemon;
    }

    @Override
    public Boolean initTenant(String tenantId) throws Exception {
        log.info("initTenantTask(tenantId={}) start", tenantId);
        StopWatch watch = new StopWatch();
        Boolean taskResult = null;
        try {
            taskResult = this.<Boolean>newInitTenantTask(tenantId, () -> {
                doInitTenant(tenantId, watch);
                return true;
            }).execute();
            return taskResult;
        } finally {
            logTimeConsuming("initTenantTask", tenantId, watch, taskResult != null);
        }
    }

    @Override
    public TenantInitResult initTenantOnce(String tenantId) {
        String doneMark = getTenantInitDoneMark(tenantId);
        if (doneMark != null) {
            // 先于抢锁判断，避免每次进程启动都去争抢Redis锁
            log.info("Tenant {} has been initialized before({}), skip init", tenantId, doneMark);
            return TenantInitResult.ALREADY_DONE;
        }
        log.info("initTenantOnceTask(tenantId={}) start", tenantId);
        StopWatch watch = new StopWatch();
        TenantInitResult taskResult = null;
        try {
            // 异常必须在execute()之外捕获：既保证写标记语句在异常时不可达，又保证execute()内finally的锁释放照常执行
            taskResult = this.<TenantInitResult>newInitTenantTask(tenantId, () -> {
                String markInLock = getTenantInitDoneMark(tenantId);
                if (markInLock != null) {
                    // 同批启动的其他实例可能已在本实例等待期间完成初始化并释放了锁，故需在锁内复查
                    log.info("Tenant {} has been initialized by another instance({}), skip init", tenantId,
                        markInLock);
                    return TenantInitResult.ALREADY_DONE;
                }
                // 开始时间取初始化步骤真正开始执行的时刻，不含抢锁与锁内复查标记的等待耗时
                long startTimeMillis = System.currentTimeMillis();
                doInitTenant(tenantId, watch);
                markTenantInitDone(tenantId, startTimeMillis);
                return TenantInitResult.SUCCESS;
            }).execute();
            if (taskResult == null) {
                // 加锁失败：可能是其他实例正在执行，也可能是Redis锁层自身出错（HeartBeatRedisLock.lock()
                // 内部catch了Throwable并同样返回加锁失败），两者不可区分，故用WARN级别
                log.warn("Lock of init tenant {} not gotten, another instance may be initializing it, "
                    + "or redis lock error occurred, skip init", tenantId);
                return TenantInitResult.RUNNING_ON_OTHER_INSTANCE;
            }
            return taskResult;
        } catch (Exception e) {
            log.error("Failed to init tenant {} automatically, the application keeps running but the tenant is "
                + "NOT initialized. Retry will happen on next pod restart, or trigger it manually via OP API "
                + "POST /op/tenant/init with tenantId={}", tenantId, tenantId, e);
            return TenantInitResult.FAILED;
        } finally {
            logTimeConsuming("initTenantOnceTask", tenantId, watch, taskResult == TenantInitResult.SUCCESS);
        }
    }

    private <V> DistributedUniqueTask<V> newInitTenantTask(String tenantId, Callable<V> task) {
        return new DistributedUniqueTask<>(
            redisTemplate,
            "InitTenant-" + tenantId,
            REDIS_KEY_INIT_TENANT_PREFIX + tenantId,
            machineIp,
            task
        );
    }

    private void doInitTenant(String tenantId, StopWatch watch) {
        // 1.同步业务
        watch.start("syncBizFromCMDB");
        bizSyncService.syncBizFromCMDB(tenantId);
        watch.stop();

        // 2.同步业务集
        watch.start("syncBizSetFromCMDB");
        bizSetSyncService.syncBizSetFromCMDB(tenantId);
        watch.stop();

        // 3.同步租户集
        watch.start("syncTenantSetFromCMDB");
        tenantSetSyncService.syncTenantSetFromCMDB();
        watch.stop();

        // 4.同步租户下所有业务的主机
        watch.start("syncAllBizHostsAtOnce");
        tenantHostSyncService.syncAllBizHostsAtOnce(tenantId);
        watch.stop();

        // 5.启动该租户下的CMDB事件监听后台任务
        watch.start("checkAndResumeTaskForTenant");
        backGroundTaskDaemon.checkAndResumeTaskForTenant(tenantId);
        watch.stop();

        // 6.启用默认消息渠道
        if (TenantIdConstants.SYSTEM_TENANT_ID.equals(tenantId)) {
            // system租户下没有可用的CMSI消息渠道，初始化时跳过，后续由用户在页面上配置或由普通租户初始化时补齐
            log.info("Skip default notify channel init for system tenant");
        } else {
            notifyChannelInitService.tryToInitDefaultNotifyChannelsWithSingleTenant(tenantId);
        }
    }

    private String getTenantInitDoneMark(String tenantId) {
        return redisTemplate.opsForValue().get(buildTenantInitDoneKey(tenantId));
    }

    private void markTenantInitDone(String tenantId, long startTimeMillis) {
        String key = buildTenantInitDoneKey(tenantId);
        TenantInitDoneMark mark = new TenantInitDoneMark(
            machineIp,
            formatTime(startTimeMillis),
            formatTime(System.currentTimeMillis())
        );
        String value = JsonUtils.toJson(mark);
        redisTemplate.opsForValue().set(key, value);
        log.info("Tenant init done mark written, {}={}", key, value);
    }

    private String formatTime(long timeMillis) {
        return DateUtils.formatUnixTimestamp(timeMillis, ChronoUnit.MILLIS);
    }

    private String buildTenantInitDoneKey(String tenantId) {
        return REDIS_KEY_TENANT_INIT_DONE_PREFIX + tenantId;
    }

    private void logTimeConsuming(String taskName, String tenantId, StopWatch watch, boolean executed) {
        if (watch.isRunning()) {
            watch.stop();
        }
        if (executed) {
            log.info("{}(tenantId={}) finished, timeConsuming={}", taskName, tenantId, watch.prettyPrint());
        }
    }
}
