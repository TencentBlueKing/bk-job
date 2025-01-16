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

package com.tencent.bk.job.crontab.service.impl;

import com.tencent.bk.audit.context.ActionAuditContext;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.mysql.JobTransactional;
import com.tencent.bk.job.crontab.auth.CronAuthService;
import com.tencent.bk.job.crontab.dao.CronJobDAO;
import com.tencent.bk.job.crontab.exception.TaskExecuteAuthFailedException;
import com.tencent.bk.job.crontab.model.BatchUpdateCronJobReq;
import com.tencent.bk.job.crontab.model.CronJobCreateUpdateReq;
import com.tencent.bk.job.crontab.model.dto.AddJobToQuartzResult;
import com.tencent.bk.job.crontab.model.dto.BatchAddResult;
import com.tencent.bk.job.crontab.model.dto.CronJobBasicInfoDTO;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.model.dto.CronJobVariableDTO;
import com.tencent.bk.job.crontab.model.dto.NeedScheduleCronInfo;
import com.tencent.bk.job.crontab.service.BatchCronJobService;
import com.tencent.bk.job.crontab.service.ExecuteTaskService;
import com.tencent.bk.job.crontab.service.QuartzService;
import com.tencent.bk.job.execute.model.inner.ServiceTaskVariable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 批量操作定时任务 Service 实现
 */
@Slf4j
@Service
public class BatchCronJobServiceImpl implements BatchCronJobService {

    private final CronJobDAO cronJobDAO;
    private final CronAuthService cronAuthService;
    private final ExecuteTaskService executeTaskService;
    private final QuartzService quartzService;

    @Autowired
    public BatchCronJobServiceImpl(CronJobDAO cronJobDAO,
                                   CronAuthService cronAuthService,
                                   ExecuteTaskService executeTaskService, QuartzService quartzService) {
        this.cronJobDAO = cronJobDAO;
        this.cronAuthService = cronAuthService;
        this.executeTaskService = executeTaskService;
        this.quartzService = quartzService;
    }

    /**
     * 批量添加定时任务到Quartz
     *
     * @param cronJobBasicInfoList 定时任务列表
     * @return 批量添加结果
     */
    @Override
    public BatchAddResult batchAddJobToQuartz(List<CronJobBasicInfoDTO> cronJobBasicInfoList) {
        List<Long> cronJobIdList = cronJobBasicInfoList.stream()
            .map(CronJobBasicInfoDTO::getId)
            .distinct()
            .collect(Collectors.toList());
        // 1.批量获取定时任务信息
        List<CronJobInfoDTO> cronJobList = cronJobDAO.listCronJobByIds(cronJobIdList);
        Map<Long, CronJobInfoDTO> cronJobMap = cronJobList.stream()
            .collect(
                Collectors.toMap(
                    CronJobInfoDTO::getId,
                    cronJobInfoDTO -> cronJobInfoDTO
                )
            );
        BatchAddResult finalBatchResult = new BatchAddResult();
        BatchAddResult notFoundBatchResult = recordNotFoundCronJobs(
            cronJobBasicInfoList,
            cronJobMap
        );
        finalBatchResult.merge(notFoundBatchResult);
        // 2.过滤出开启的定时任务进行触发
        BatchAddResult addToQuartzBatchResult = addEnabledCronJobToQuartz(cronJobList);
        finalBatchResult.merge(addToQuartzBatchResult);
        return finalBatchResult;
    }

    /**
     * 批量添加开启的定时任务到Quartz
     *
     * @param cronJobList 定时任务列表
     * @return 批量添加结果
     */
    private BatchAddResult addEnabledCronJobToQuartz(List<CronJobInfoDTO> cronJobList) {
        BatchAddResult batchAddResult = new BatchAddResult();
        for (CronJobInfoDTO cronJobInfoDTO : cronJobList) {
            if (!cronJobInfoDTO.getEnable()) {
                batchAddResult.addResult(AddJobToQuartzResult.failResult(
                    cronJobInfoDTO.toBasicInfoDTO(),
                    String.format("CronJob(id=%s) is not enabled, ignore", cronJobInfoDTO.getId())
                ));
                continue;
            }
            try {
                quartzService.tryToAddJobToQuartz(cronJobInfoDTO);
                batchAddResult.addResult(AddJobToQuartzResult.successResult(cronJobInfoDTO.toBasicInfoDTO()));
            } catch (Exception e) {
                String message = MessageFormatter.format(
                    "Add cronJob(id={}) to quartz failed",
                    cronJobInfoDTO.getId()
                ).getMessage();
                batchAddResult.addResult(
                    AddJobToQuartzResult.failResult(
                        cronJobInfoDTO.toBasicInfoDTO(),
                        message,
                        e
                    )
                );
            }
        }
        return batchAddResult;
    }

    /**
     * 记录DB中不存在的定时任务
     *
     * @param cronJobBasicInfoList 需要添加到Quartz的定时任务基础信息列表
     * @param cronJobMap           DB中存在的定时任务信息
     * @return 批量添加失败的定时任务信息
     */
    private BatchAddResult recordNotFoundCronJobs(List<CronJobBasicInfoDTO> cronJobBasicInfoList,
                                                  Map<Long, CronJobInfoDTO> cronJobMap) {
        BatchAddResult batchAddResult = new BatchAddResult();
        for (CronJobBasicInfoDTO cronJobBasicInfoDTO : cronJobBasicInfoList) {
            Long cronJobId = cronJobBasicInfoDTO.getId();
            if (!cronJobMap.containsKey(cronJobId)) {
                batchAddResult.addResult(
                    AddJobToQuartzResult.failResult(
                        cronJobBasicInfoDTO,
                        "Cannot find cronJob in DB by id: " + cronJobId
                    )
                );
            }
        }
        return batchAddResult;
    }

    /**
     * 批量更新定时任务DB数据，返回需要重新进行调度的任务信息，支持事务
     *
     * @param username              用户名
     * @param appId                 Job业务ID
     * @param batchUpdateCronJobReq 批量更新定时任务请求
     * @return 需要重新进行调度的任务信息
     */
    @JobTransactional(transactionManager = "jobCrontabTransactionManager")
    public NeedScheduleCronInfo batchUpdateCronJob(String username,
                                                   Long appId,
                                                   BatchUpdateCronJobReq batchUpdateCronJobReq) {
        List<CronJobCreateUpdateReq> cronJobReqList = batchUpdateCronJobReq.getCronJobInfoList();
        if (CollectionUtils.isEmpty(cronJobReqList)) {
            return NeedScheduleCronInfo.emptyInstance();
        }
        List<Long> cronJobInstanceList = new ArrayList<>();
        cronJobReqList.forEach(cronJobCreateUpdateReq -> cronJobInstanceList.add(cronJobCreateUpdateReq.getId()));
        cronAuthService.batchAuthManageCron(username, new AppResourceScope(appId), cronJobInstanceList);

        List<Long> needAddCronIdList = new ArrayList<>();
        List<Long> needDeleteCronIdList = new ArrayList<>();
        cronJobReqList.forEach(cronJobReq ->
            updateCronJob(username, appId, cronJobReq, needAddCronIdList, needDeleteCronIdList)
        );
        return new NeedScheduleCronInfo(needAddCronIdList, needDeleteCronIdList);
    }

    private void updateCronJob(String username,
                               Long appId,
                               CronJobCreateUpdateReq cronJobReq,
                               List<Long> needAddCronIdList,
                               List<Long> needDeleteCronIdList) {
        Long cronJobId = cronJobReq.getId();
        CronJobInfoDTO cronJobInfoFromReq = CronJobInfoDTO.fromReq(username, appId, cronJobReq);
        cronJobInfoFromReq.setEnable(cronJobReq.getEnable());
        CronJobInfoDTO originCronJobInfo = cronJobDAO.getCronJobById(appId, cronJobId);
        if (cronJobReq.getEnable()) {
            try {
                List<ServiceTaskVariable> taskVariables = null;
                if (CollectionUtils.isNotEmpty(originCronJobInfo.getVariableValue())) {
                    taskVariables = originCronJobInfo.getVariableValue().stream()
                        .map(CronJobVariableDTO::toServiceTaskVariable)
                        .collect(Collectors.toList());
                }
                executeTaskService.authExecuteTask(
                    appId,
                    originCronJobInfo.getTaskPlanId(),
                    cronJobId,
                    originCronJobInfo.getName(),
                    taskVariables,
                    username
                );
                if (cronJobDAO.updateCronJobById(cronJobInfoFromReq)) {
                    needAddCronIdList.add(cronJobId);
                }
            } catch (TaskExecuteAuthFailedException e) {
                log.error("Error while pre auth cron execute!", e);
                throw e;
            }
        } else {
            if (cronJobDAO.updateCronJobById(cronJobInfoFromReq)) {
                needDeleteCronIdList.add(cronJobId);
            }
        }
        CronJobInfoDTO updateCronJobInfo = cronJobDAO.getCronJobById(appId, cronJobId);
        ActionAuditContext.current().addInstanceInfo(
            String.valueOf(cronJobId),
            cronJobReq.getName(),
            originCronJobInfo,
            updateCronJobInfo
        );
    }
}
