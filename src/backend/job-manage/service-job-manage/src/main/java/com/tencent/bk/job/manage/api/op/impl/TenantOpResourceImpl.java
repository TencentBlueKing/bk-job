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

package com.tencent.bk.job.manage.api.op.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.redis.util.DistributedUniqueTask;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.manage.api.op.TenantOpResource;
import com.tencent.bk.job.manage.background.ha.BackGroundTaskDaemon;
import com.tencent.bk.job.manage.background.sync.BizSetSyncService;
import com.tencent.bk.job.manage.background.sync.BizSyncService;
import com.tencent.bk.job.manage.background.sync.TenantHostSyncService;
import com.tencent.bk.job.manage.background.sync.TenantSetSyncService;
import com.tencent.bk.job.manage.model.op.req.InitTenantReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.RestController;

/**
 * 多租户OP接口实现类
 */
@Slf4j
@RestController
public class TenantOpResourceImpl implements TenantOpResource {
    private static final String machineIp = IpUtils.getFirstMachineIP();
    private static final String REDIS_KEY_INIT_TENANT_PREFIX = "initTenant-";
    private final RedisTemplate<String, String> redisTemplate;
    private final BizSyncService bizSyncService;
    private final BizSetSyncService bizSetSyncService;
    private final TenantSetSyncService tenantSetSyncService;
    private final TenantHostSyncService tenantHostSyncService;
    private final BackGroundTaskDaemon backGroundTaskDaemon;

    @Autowired
    public TenantOpResourceImpl(RedisTemplate<String, String> redisTemplate,
                                BizSyncService bizSyncService,
                                BizSetSyncService bizSetSyncService,
                                TenantSetSyncService tenantSetSyncService,
                                TenantHostSyncService tenantHostSyncService,
                                BackGroundTaskDaemon backGroundTaskDaemon) {
        this.redisTemplate = redisTemplate;
        this.bizSyncService = bizSyncService;
        this.bizSetSyncService = bizSetSyncService;
        this.tenantSetSyncService = tenantSetSyncService;
        this.tenantHostSyncService = tenantHostSyncService;
        this.backGroundTaskDaemon = backGroundTaskDaemon;
    }

    @Override
    public Response<Object> initTenant(InitTenantReq req) {
        String tenantId = req.getTenantId();
        log.info("initTenantTask(tenantId={}) start", tenantId);
        StopWatch watch = new StopWatch();
        Object taskResult = false;
        try {
            // 分布式唯一性保证
            taskResult = new DistributedUniqueTask<>(
                redisTemplate,
                "InitTenant-" + tenantId,
                REDIS_KEY_INIT_TENANT_PREFIX + tenantId,
                machineIp,
                () -> doInitTenant(tenantId, watch)
            ).execute();
            if (taskResult == null) {
                // 任务已在其他实例执行
                return Response.buildCommonFailResp(ErrorCode.INIT_TENANT_TASK_ALREADY_RUNNING, new String[]{tenantId});
            }
            return Response.buildSuccessResp(taskResult);
        } catch (Exception e) {
            log.error("initTenantTask failed", e);
            return Response.buildCommonFailResp(ErrorCode.INIT_TENANT_ERROR, new String[]{tenantId});
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
            if (taskResult != null) {
                log.info("initTenantTask finished, timeConsuming={}", watch.prettyPrint());
            }
        }
    }

    private Object doInitTenant(String tenantId, StopWatch watch) {
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
        return true;
    }
}
