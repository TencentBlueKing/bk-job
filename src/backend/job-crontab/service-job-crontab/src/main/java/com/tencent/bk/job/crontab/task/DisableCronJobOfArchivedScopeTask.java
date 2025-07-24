/*
 *
 *  * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *  *
 *  * Copyright (C) 2021 Tencent.  All rights reserved.
 *  *
 *  * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *  *
 *  * License for BK-JOB蓝鲸智云作业平台:
 *  * --------------------------------------------------------------------
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  * the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 *  * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 *  * IN THE SOFTWARE.
 *
 */

package com.tencent.bk.job.crontab.task;

import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.cc.sdk.IBizSetCmdbClient;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.redis.util.RedisKeyHeartBeatThread;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.crontab.service.CronJobService;
import com.tencent.bk.job.manage.api.inner.ServiceApplicationResource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 业务(集)被归档了，禁用其关联的定时任务
 */
@Slf4j
@Component
public class DisableCronJobOfArchivedScopeTask {
    private static final String DISABLE_CRON_JOB_TASK_RUNNING_MACHINE = "disable:appId-not-exist:cron";

    private final RedisTemplate<String, String> redisTemplate;
    private final IBizCmdbClient bizCmdbClient;
    private final IBizSetCmdbClient bizSetCmdbClient;
    private final CronJobService cronJobService;
    private final ServiceApplicationResource serviceApplicationResource;

    @Value("${job.crontab.autoDisableCronOfArchivedScope.enabled:true}")
    private Boolean enabled;

    public DisableCronJobOfArchivedScopeTask(RedisTemplate<String, String> redisTemplate,
                                             IBizCmdbClient bizCmdbClient,
                                             IBizSetCmdbClient bizSetCmdbClient,
                                             CronJobService cronJobService,
                                             ServiceApplicationResource serviceApplicationResource){
        this.redisTemplate = redisTemplate;
        this.bizCmdbClient = bizCmdbClient;
        this.bizSetCmdbClient = bizSetCmdbClient;
        this.cronJobService = cronJobService;
        this.serviceApplicationResource = serviceApplicationResource;
    }

    public boolean execute() {
        if (!enabled) {
            log.info("disableCronJobOfArchivedScopeTask not enabled, skip, you can enable it in config file");
            return false;
        }
        log.info("disableCronJobOfArchivedScopeTask arranged");
        String machineIp = IpUtils.getFirstMachineIP();
        boolean lockGotten = LockUtils.tryGetDistributedLock(
            DISABLE_CRON_JOB_TASK_RUNNING_MACHINE,
            machineIp,
            5000
        );
        if (!lockGotten) {
            String runningMachine = redisTemplate.opsForValue().get(DISABLE_CRON_JOB_TASK_RUNNING_MACHINE);
            if (StringUtils.isNotBlank(runningMachine)) {
                //已有线程在跑
                log.warn("disable cron job thread already running on {}", runningMachine);
            } else {
                log.info("disable cron job lock not gotten, return");
            }
            return false;
        }

        // 开一个心跳子线程，维护当前机器的状态
        RedisKeyHeartBeatThread disableCronJobRedisKeyHeartBeatThread = new RedisKeyHeartBeatThread(
            redisTemplate,
            DISABLE_CRON_JOB_TASK_RUNNING_MACHINE,
            machineIp,
            5000L,
            4000L
        );
        disableCronJobRedisKeyHeartBeatThread.setName("disableCronJobRedisKeyHeartBeatThread");
        disableCronJobRedisKeyHeartBeatThread.start();
        try {
            disableCronJobOfArchivedScope();
            return true;
        } catch (Throwable t) {
            log.warn("Fail to disableCronJob", t);
            return false;
        } finally {
            disableCronJobRedisKeyHeartBeatThread.stopAtOnce();
        }
    }

    /**
     * 禁用条件：1. 业务在作业平台中是软删除状态， 2. 查询配置平台业务接口，业务不在返回列表中
     */
    private void disableCronJobOfArchivedScope() {
        List<Long> archivedAppIds = serviceApplicationResource.listAllAppIdOfArchivedScope().getData();
        log.info("finally find archived appIds={}", archivedAppIds);
        List<Long> failedAppIds = new ArrayList<>();
        for (Long appId : archivedAppIds) {
            boolean result = cronJobService.disableCronJobByAppId(appId);
            if (!result) {
                failedAppIds.add(appId);
            }
        }
        if (failedAppIds.isEmpty()) {
            log.info("all cron jobs with archived apps have been successfully disabled.");
        } else {
            log.warn("failed to disable cron jobs for the following archived appIds: {}}", failedAppIds);
        }
    }

}
