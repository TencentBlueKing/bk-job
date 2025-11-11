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

import com.tencent.bk.job.common.redis.util.DistributedUniqueTask;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.crontab.config.JobCrontabProperties;
import com.tencent.bk.job.crontab.dao.CronJobDAO;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.service.QuartzService;
import com.tencent.bk.job.manage.api.inner.ServiceApplicationResource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 业务(集)被归档了，禁用其关联的定时任务，避免消耗调度资源/产生错误日志
 */
@Slf4j
@Component
public class DisableCronJobOfArchivedScopeTask {
    private static final String DISABLE_CRON_JOB_TASK_RUNNING_MACHINE = "DisableCronOfArchivedApp";

    private final RedisTemplate<String, String> redisTemplate;
    private final ServiceApplicationResource serviceApplicationResource;
    private final CronJobDAO cronJobDAO;
    private final QuartzService quartzService;
    private final JobCrontabProperties jobCrontabProperties;

    @Autowired
    public DisableCronJobOfArchivedScopeTask(RedisTemplate<String, String> redisTemplate,
                                             ServiceApplicationResource serviceApplicationResource,
                                             CronJobDAO cronJobDAO,
                                             QuartzService quartzService,
                                             JobCrontabProperties jobCrontabProperties) {
        this.redisTemplate = redisTemplate;
        this.serviceApplicationResource = serviceApplicationResource;
        this.cronJobDAO = cronJobDAO;
        this.quartzService = quartzService;
        this.jobCrontabProperties = jobCrontabProperties;
    }

    public boolean execute() {
        if (!jobCrontabProperties.getDisableCronJobOfArchivedScope().getEnabled()) {
            log.info("disableCronJobOfArchivedScopeTask not enabled, skip, you can enable it in config file");
            return false;
        }
        log.info("disableCronJobOfArchivedScopeTask arranged");
        String machineIp = IpUtils.getFirstMachineIP();
        Integer taskResult;
        try {
            // 分布式唯一性保证
            taskResult = new DistributedUniqueTask<>(
                redisTemplate,
                this.getClass().getSimpleName(),
                DISABLE_CRON_JOB_TASK_RUNNING_MACHINE,
                machineIp,
                this::disableCronJobOfArchivedScope
            ).execute();
            if (taskResult == null) {
                // 任务已在其他实例执行
                log.info("disableCronJobTask already executed by another instance");
                return false;
            }
            return true;
        } catch (Throwable t) {
            log.warn("Fail to disableCronJob", t);
            return false;
        }
    }

    /**
     * 禁用已归档业务下的定时任务，禁用条件（且）：
     * 1. 业务在作业平台中是软删除状态；
     * 2. 查询配置平台业务接口，业务不在返回列表中。
     */
    private int disableCronJobOfArchivedScope() {
        List<Long> archivedAppIds = serviceApplicationResource.listAllAppIdOfArchivedScope().getData();
        log.info("finally find archived appIds={}", archivedAppIds);
        int disabledNum = 0;
        for (Long appId : archivedAppIds) {
            disabledNum += disableCronJobByAppId(appId);
        }
        if (disabledNum > 0) {
            log.info("{} cronJob disabled", disabledNum);
        }
        return disabledNum;
    }

    /**
     * 通过Job业务ID禁用定时任务
     */
    private int disableCronJobByAppId(Long appId) {
        CronJobInfoDTO cronJobInfoDTO = new CronJobInfoDTO();
        cronJobInfoDTO.setAppId(appId);
        cronJobInfoDTO.setEnable(true);
        List<Long> cronJobIdList = cronJobDAO.listCronJobIds(cronJobInfoDTO);
        if (CollectionUtils.isEmpty(cronJobIdList)) {
            return 0;
        }
        int affectedNum = cronJobDAO.disableCronJob(appId, cronJobIdList);
        for (Long cronJobId : cronJobIdList) {
            quartzService.deleteJobFromQuartz(appId, cronJobId);
        }
        log.info("appId={}, {} cronJob disabled, cronJobIdList={}", appId, affectedNum, cronJobIdList);
        return affectedNum;
    }
}
