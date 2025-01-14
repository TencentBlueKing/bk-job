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

import com.tencent.bk.audit.annotations.ActionAuditRecord;
import com.tencent.bk.audit.annotations.AuditInstanceRecord;
import com.tencent.bk.audit.context.ActionAuditContext;
import com.tencent.bk.job.common.audit.constants.EventContentConstants;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.exception.AlreadyExistsException;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.mysql.JobTransactional;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.crontab.auth.CronAuthService;
import com.tencent.bk.job.crontab.constant.CronConstants;
import com.tencent.bk.job.crontab.dao.CronJobDAO;
import com.tencent.bk.job.crontab.exception.TaskExecuteAuthFailedException;
import com.tencent.bk.job.crontab.listener.event.CrontabEvent;
import com.tencent.bk.job.crontab.model.BatchUpdateCronJobReq;
import com.tencent.bk.job.crontab.model.dto.CronJobBasicInfoDTO;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.model.dto.CronJobVariableDTO;
import com.tencent.bk.job.crontab.model.dto.InnerCronJobInfoDTO;
import com.tencent.bk.job.crontab.model.dto.NeedScheduleCronInfo;
import com.tencent.bk.job.crontab.model.dto.QuartzJobInfoDTO;
import com.tencent.bk.job.crontab.model.inner.ServerDTO;
import com.tencent.bk.job.crontab.model.inner.ServiceInnerCronJobInfoDTO;
import com.tencent.bk.job.crontab.model.inner.request.ServiceAddInnerCronJobRequestDTO;
import com.tencent.bk.job.crontab.mq.CrontabMQEventDispatcher;
import com.tencent.bk.job.crontab.service.BatchCronJobService;
import com.tencent.bk.job.crontab.service.CronJobService;
import com.tencent.bk.job.crontab.service.ExecuteTaskService;
import com.tencent.bk.job.crontab.service.HostService;
import com.tencent.bk.job.crontab.service.QuartzService;
import com.tencent.bk.job.crontab.service.TaskPlanService;
import com.tencent.bk.job.crontab.timer.AbstractQuartzTaskHandler;
import com.tencent.bk.job.crontab.timer.QuartzJob;
import com.tencent.bk.job.crontab.timer.QuartzJobBuilder;
import com.tencent.bk.job.crontab.timer.QuartzTrigger;
import com.tencent.bk.job.crontab.timer.QuartzTriggerBuilder;
import com.tencent.bk.job.crontab.timer.executor.InnerJobExecutor;
import com.tencent.bk.job.execute.model.inner.ServiceTaskVariable;
import com.tencent.bk.job.manage.model.inner.ServiceTaskPlanDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.quartz.CronTrigger;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tencent.bk.job.common.audit.JobAuditAttributeNames.OPERATION;

/**
 * 定时任务 Service 实现
 */
@Slf4j
@Service
public class CronJobServiceImpl implements CronJobService {

    private final CronJobDAO cronJobDAO;

    private final AbstractQuartzTaskHandler quartzTaskHandler;
    private final QuartzService quartzService;
    private final TaskPlanService taskPlanService;
    private final CronAuthService cronAuthService;
    private final ExecuteTaskService executeTaskService;
    private final HostService hostService;
    private final CrontabMQEventDispatcher crontabMQEventDispatcher;
    private final BatchCronJobService batchCronJobService;

    @Autowired
    public CronJobServiceImpl(CronJobDAO cronJobDAO,
                              AbstractQuartzTaskHandler quartzTaskHandler,
                              QuartzService quartzService,
                              TaskPlanService taskPlanService,
                              CronAuthService cronAuthService,
                              ExecuteTaskService executeTaskService,
                              HostService hostService,
                              CrontabMQEventDispatcher crontabMQEventDispatcher,
                              BatchCronJobServiceImpl batchCronJobService) {
        this.cronJobDAO = cronJobDAO;
        this.quartzTaskHandler = quartzTaskHandler;
        this.quartzService = quartzService;
        this.taskPlanService = taskPlanService;
        this.cronAuthService = cronAuthService;
        this.executeTaskService = executeTaskService;
        this.hostService = hostService;
        this.crontabMQEventDispatcher = crontabMQEventDispatcher;
        this.batchCronJobService = batchCronJobService;
    }

    @Override
    public PageData<CronJobInfoDTO> listPageCronJobInfosWithoutVars(CronJobInfoDTO cronJobCondition,
                                                                    BaseSearchCondition baseSearchCondition) {
        return cronJobDAO.listPageCronJobsWithoutVarsByCondition(cronJobCondition, baseSearchCondition);
    }

    @Override
    public CronJobInfoDTO getCronJobInfoById(Long cronJobId) {
        return cronJobDAO.getCronJobById(cronJobId);
    }

    @Override
    public Map<Long, CronJobInfoDTO> getCronJobInfoMapByIds(List<Long> cronJobIdList) {
        List<CronJobInfoDTO> cronJobInfoDTOList = cronJobDAO.listCronJobByIds(cronJobIdList);
        Map<Long, CronJobInfoDTO> map = new HashMap<>();
        for (CronJobInfoDTO cronJobInfoDTO : cronJobInfoDTOList) {
            map.put(cronJobInfoDTO.getId(), cronJobInfoDTO);
        }
        return map;
    }

    @Override
    public CronJobInfoDTO getCronJobInfoById(Long appId, Long cronJobId) {
        return cronJobDAO.getCronJobById(appId, cronJobId);
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_CRON,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.CRON,
            instanceIds = "#cronJobId",
            instanceNames = "#$?.name"
        ),
        content = EventContentConstants.VIEW_CRON_JOB
    )
    public CronJobInfoDTO getCronJobInfoById(String username, Long appId, Long cronJobId) {
        CronJobInfoDTO cronJob = getCronJobInfoById(appId, cronJobId);
        if (cronJob == null) {
            throw new NotFoundException(ErrorCode.CRON_JOB_NOT_EXIST);
        }
        cronAuthService.authManageCron(username,
            new AppResourceScope(appId), cronJobId, null).denyIfNoPermission();
        return cronJob;
    }

    @Override
    public CronJobInfoDTO getCronJobErrorInfoById(Long appId, Long cronJobId) {
        return cronJobDAO.getCronJobErrorById(appId, cronJobId);
    }

    @Override
    public boolean updateCronJobErrorById(CronJobInfoDTO cronJobErrorInfo) {
        return cronJobDAO.updateCronJobErrorById(cronJobErrorInfo);
    }

    @Override
    @JobTransactional(transactionManager = "jobCrontabTransactionManager")
    @ActionAuditRecord(
        actionId = ActionId.CREATE_CRON,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.CRON,
            instanceIds = "#$?.id",
            instanceNames = "#$?.name"
        ),
        content = EventContentConstants.CREATE_CRON_JOB
    )
    public CronJobInfoDTO createCronJobInfo(String username, CronJobInfoDTO cronJobInfo) {
        cronAuthService.authCreateCron(username,
            new AppResourceScope(cronJobInfo.getAppId())).denyIfNoPermission();

        checkCronJobPlanOrScript(cronJobInfo);
        saveSnapShotForHostVaiableValue(cronJobInfo);

        // 有执行方案运行权限，才能用该执行方案创建定时任务
        authExecuteTask(cronJobInfo);
        cronJobInfo.setCreateTime(DateUtils.currentTimeSeconds());
        cronJobInfo.setEnable(false);

        Long id = cronJobDAO.insertCronJob(cronJobInfo);
        cronAuthService.registerCron(id, cronJobInfo.getName(), cronJobInfo.getCreator());

        return getCronJobInfoById(id);
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_CRON,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.CRON,
            instanceIds = "#cronJobInfo?.id",
            instanceNames = "#$?.name"
        ),
        content = EventContentConstants.EDIT_CRON_JOB
    )
    public CronJobInfoDTO updateCronJobInfo(String username, CronJobInfoDTO cronJobInfo) {
        cronAuthService.authManageCron(username,
            new AppResourceScope(cronJobInfo.getAppId()), cronJobInfo.getId(), null).denyIfNoPermission();

        CronJobInfoDTO originCron = getCronJobInfoById(cronJobInfo.getId());
        if (originCron == null) {
            throw new NotFoundException(ErrorCode.CRON_JOB_NOT_EXIST);
        }

        checkCronJobPlanOrScript(cronJobInfo);
        processCronJobVariableValueMask(cronJobInfo);

        if (cronJobInfo.getEnable()) {
            authExecuteTask(cronJobInfo);
            if (cronJobDAO.updateCronJobById(cronJobInfo)) {
                informAllToAddJobToQuartz(cronJobInfo.getAppId(), cronJobInfo.getId());
            } else {
                throw new InternalException(ErrorCode.UPDATE_CRON_JOB_FAILED);
            }
        } else {
            if (cronJobDAO.updateCronJobById(cronJobInfo)) {
                informAllToDeleteJobFromQuartz(cronJobInfo.getAppId(), cronJobInfo.getId());
            } else {
                throw new InternalException(ErrorCode.UPDATE_CRON_JOB_FAILED);
            }
        }

        CronJobInfoDTO updateCron = getCronJobInfoById(cronJobInfo.getId());

        // 审计
        ActionAuditContext.current()
            .setOriginInstance(CronJobInfoDTO.toEsbCronInfoV3(originCron))
            .setInstance(CronJobInfoDTO.toEsbCronInfoV3(updateCron));

        return updateCron;
    }

    private void authExecuteTask(CronJobInfoDTO cronJobInfo) {
        try {
            List<ServiceTaskVariable> taskVariables = null;
            if (CollectionUtils.isNotEmpty(cronJobInfo.getVariableValue())) {
                taskVariables =
                    cronJobInfo.getVariableValue().parallelStream()
                        .map(CronJobVariableDTO::toServiceTaskVariable).collect(Collectors.toList());
            }
            executeTaskService.authExecuteTask(cronJobInfo.getAppId(), cronJobInfo.getTaskPlanId(),
                cronJobInfo.getId(), cronJobInfo.getName(), taskVariables, cronJobInfo.getLastModifyUser());
        } catch (TaskExecuteAuthFailedException e) {
            log.error("Error while pre auth cron execute!", e);
            throw e;
        }
    }

    /**
     * 保存定时任务主机变量中的IP等快照信息
     *
     * @param cronJobInfo 定时任务信息
     */
    private void saveSnapShotForHostVaiableValue(CronJobInfoDTO cronJobInfo) {
        List<CronJobVariableDTO> variableValue = cronJobInfo.getVariableValue();
        if (CollectionUtils.isEmpty(variableValue)) {
            return;
        }
        for (CronJobVariableDTO variable : variableValue) {
            if (variable.getType() != TaskVariableTypeEnum.HOST_LIST) {
                continue;
            }
            ServerDTO serverDTO = variable.getServer();
            if (serverDTO == null) {
                continue;
            }
            List<HostDTO> hostByIpList = serverDTO.getIps();
            if (CollectionUtils.isNotEmpty(hostByIpList)) {
                hostService.fillHosts(hostByIpList);
            }
        }
    }

    /**
     * 更新定时任务时将使用mask指代的密文值设置为其真实值
     *
     * @param cronJobInfo 定时任务信息
     */
    private void processCronJobVariableValueMask(CronJobInfoDTO cronJobInfo) {
        if (CollectionUtils.isEmpty(cronJobInfo.getVariableValue())) {
            return;
        }
        List<CronJobVariableDTO> hasMaskVariableList = new ArrayList<>();
        for (CronJobVariableDTO cronJobVariable : cronJobInfo.getVariableValue()) {
            if (cronJobVariable.getType().needMask()) {
                if (cronJobVariable.getValue().equals(cronJobVariable.getType().getMask())) {
                    hasMaskVariableList.add(cronJobVariable);
                }
            }
        }
        if (CollectionUtils.isEmpty(hasMaskVariableList)) {
            return;
        }
        CronJobInfoDTO originCronJob =
            cronJobDAO.getCronJobById(cronJobInfo.getAppId(), cronJobInfo.getId());
        if (CollectionUtils.isEmpty(originCronJob.getVariableValue())) {
            return;
        }
        for (CronJobVariableDTO cronJobVariable : originCronJob.getVariableValue()) {
            Iterator<CronJobVariableDTO> newCronJobVariableIterator = hasMaskVariableList.iterator();
            while (newCronJobVariableIterator.hasNext()) {
                CronJobVariableDTO newCronJobVariable = newCronJobVariableIterator.next();
                if (newCronJobVariable.getId().equals(cronJobVariable.getId())) {
                    newCronJobVariable.setValue(cronJobVariable.getValue());
                    newCronJobVariableIterator.remove();
                    break;
                }
            }
            if (CollectionUtils.isEmpty(hasMaskVariableList)) {
                break;
            }
        }
    }

    private void checkCronJobPlanOrScript(CronJobInfoDTO cronJobInfo) {
        if (cronJobInfo.getTaskPlanId() != null && cronJobInfo.getTaskPlanId() > 0) {
            ServiceTaskPlanDTO planBasicInfoById =
                taskPlanService.getPlanBasicInfoById(cronJobInfo.getAppId(), cronJobInfo.getTaskPlanId());
            if (planBasicInfoById != null) {
                cronJobInfo.setTaskTemplateId(planBasicInfoById.getTaskTemplateId());
            } else {
                throw new NotFoundException(ErrorCode.TASK_PLAN_NOT_EXIST);
            }
        }
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_CRON,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.CRON,
            instanceIds = "#cronJobId"
        ),
        content = EventContentConstants.DELETE_CRON_JOB
    )
    public Boolean deleteCronJobInfo(String username, Long appId, Long cronJobId) {
        cronAuthService.authManageCron(username,
            new AppResourceScope(appId), cronJobId, null).denyIfNoPermission();

        CronJobInfoDTO cron = getCronJobInfoById(cronJobId);
        if (cron == null) {
            throw new NotFoundException(ErrorCode.CRON_JOB_NOT_EXIST);
        }

        // 审计
        ActionAuditContext.current().setInstanceName(cron.getName());

        if (cronJobDAO.deleteCronJobById(appId, cronJobId)) {
            informAllToDeleteJobFromQuartz(appId, cronJobId);
            return true;
        }
        return false;
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_CRON,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.CRON,
            instanceIds = "#cronJobId"
        ),
        content = EventContentConstants.SWITCH_CRON_JOB_STATUS
    )
    public Boolean changeCronJobEnableStatus(String username, Long appId, Long cronJobId, Boolean enable) {
        cronAuthService.authManageCron(username,
            new AppResourceScope(appId), cronJobId, null).denyIfNoPermission();

        CronJobInfoDTO originCronJobInfo = getCronJobInfoById(appId, cronJobId);
        if (originCronJobInfo == null) {
            throw new NotFoundException(ErrorCode.CRON_JOB_NOT_EXIST);
        }

        // 审计
        ActionAuditContext.current()
            .setInstanceName(originCronJobInfo.getName())
            .addAttribute(OPERATION, enable ? "Switch on" : "Switch off");

        CronJobInfoDTO cronJobInfo = new CronJobInfoDTO();
        cronJobInfo.setAppId(appId);
        cronJobInfo.setId(cronJobId);
        cronJobInfo.setEnable(enable);
        cronJobInfo.setLastModifyUser(username);
        cronJobInfo.setLastModifyTime(DateUtils.currentTimeSeconds());
        if (enable) {
            try {
                List<ServiceTaskVariable> taskVariables = null;
                if (CollectionUtils.isNotEmpty(originCronJobInfo.getVariableValue())) {
                    taskVariables =
                        originCronJobInfo.getVariableValue().stream()
                            .map(CronJobVariableDTO::toServiceTaskVariable).collect(Collectors.toList());
                }
                executeTaskService.authExecuteTask(appId, originCronJobInfo.getTaskPlanId(),
                    cronJobId, originCronJobInfo.getName(), taskVariables, username);
                if (cronJobDAO.updateCronJobById(cronJobInfo)) {
                    return informAllToAddJobToQuartz(appId, cronJobId);
                } else {
                    return false;
                }
            } catch (TaskExecuteAuthFailedException e) {
                log.error("Error while pre auth cron execute!", e);
                throw e;
            }
        } else {
            if (cronJobDAO.updateCronJobById(cronJobInfo)) {
                return informAllToDeleteJobFromQuartz(appId, cronJobId);
            } else {
                return false;
            }
        }

    }

    @Override
    public Boolean disableExpiredCronJob(Long appId, Long cronJobId, String lastModifyUser, Long lastModifyTime) {
        CronJobInfoDTO cronJobInfo = new CronJobInfoDTO();
        cronJobInfo.setAppId(appId);
        cronJobInfo.setId(cronJobId);
        cronJobInfo.setLastModifyUser(lastModifyUser);
        cronJobInfo.setLastModifyTime(lastModifyTime);
        cronJobInfo.setEnable(false);
        if (cronJobDAO.updateCronJobById(cronJobInfo)) {
            return informAllToDeleteJobFromQuartz(appId, cronJobId);
        } else {
            return false;
        }
    }

    @Override
    public Boolean checkCronJobName(Long appId, Long cronJobId, String name) {
        if (cronJobId == null) {
            return false;
        }
        name = name.trim();
        if (StringUtils.isEmpty(name)) {
            return false;
        }
        return cronJobDAO.checkCronJobName(appId, cronJobId, name);
    }

    @Override
    public List<CronJobInfoDTO> listCronJobByPlanId(Long appId, Long planId) {
        return cronJobDAO.listCronJobByPlanId(appId, planId);
    }

    @Override
    public Map<Long, List<CronJobInfoDTO>> listCronJobByPlanIds(Long appId, List<Long> planIds) {
        if (appId <= 0) {
            return null;
        }
        if (CollectionUtils.isNotEmpty(planIds)) {
            Map<Long, List<CronJobInfoDTO>> cronJobInfoMap = new HashMap<>(planIds.size());
            for (Long planId : planIds) {
                if (planId > 0) {
                    ServiceTaskPlanDTO planBasicInfoById = taskPlanService.getPlanBasicInfoById(appId, planId);
                    if (planBasicInfoById != null && planId.equals(planBasicInfoById.getId())) {
                        List<CronJobInfoDTO> cronJobInfos = cronJobDAO.listCronJobByPlanId(appId, planId);
                        if (CollectionUtils.isNotEmpty(cronJobInfos)) {
                            cronJobInfoMap.put(planId, cronJobInfos);
                        }
                    }
                }
            }
            return cronJobInfoMap;
        }
        return null;
    }

    @Override
    @JobTransactional(transactionManager = "jobCrontabTransactionManager")
    public Boolean addInnerJob(ServiceAddInnerCronJobRequestDTO request) {
        if (!request.validate()) {
            return false;
        }
        QuartzTrigger trigger = null;
        if (StringUtils.isNotBlank(request.getCronExpression())) {
            trigger = QuartzTriggerBuilder.newTrigger().ofType(QuartzTrigger.TriggerType.CRON)
                .withIdentity(request.getJobKey(), request.getSystemId()).withDescription(request.getDescription())
                .withCronExpression(request.getCronExpression())
                .withMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT).build();
        } else if (request.getExecuteTime() > DateUtils.currentTimeSeconds()) {
            QuartzTriggerBuilder triggerBuilder =
                QuartzTriggerBuilder.newTrigger().ofType(QuartzTrigger.TriggerType.SIMPLE)
                    .withIdentity(request.getJobKey(), request.getSystemId()).withDescription(request.getDescription())
                    .startAt(Date.from(Instant.ofEpochSecond(request.getExecuteTime())))
                    .withMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
            if (request.getRepeatCount() > 0 && request.getRepeatInterval() > 0) {
                triggerBuilder.withRepeatCount(request.getRepeatCount());
                triggerBuilder.withIntervalInSeconds(request.getRepeatInterval());
            }
            trigger = triggerBuilder.build();
        }

        if (trigger == null) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }

        InnerCronJobInfoDTO innerCronJobInfoDTO = new InnerCronJobInfoDTO();
        innerCronJobInfoDTO.setSystemId(request.getSystemId());
        innerCronJobInfoDTO.setJobKey(request.getJobKey());
        innerCronJobInfoDTO.setRetry(request.getRetryCount());
        innerCronJobInfoDTO.setCallbackUri(request.getCallbackUri());
        innerCronJobInfoDTO.setCallbackData(request.getCallbackBody());
        String innerCronJobInfoStr = JsonUtils.toJson(innerCronJobInfoDTO);

        QuartzJob job = QuartzJobBuilder.newJob().withIdentity(request.getJobKey(), request.getSystemId())
            .forJob(InnerJobExecutor.class)
            .usingJobData(CronConstants.JOB_DATA_KEY_CRON_JOB_INFO_STR, innerCronJobInfoStr)
            .withTrigger(trigger)
            .build();

        try {
            if (quartzTaskHandler.checkExists(job.getKey())) {
                quartzTaskHandler.deleteJob(job.getKey());
            }
            quartzTaskHandler.addJob(job);
        } catch (SchedulerException e) {
            log.error("Error while add job to quartz!", e);
            throw new InternalException("Add to quartz failed!", e, ErrorCode.INTERNAL_ERROR);
        }

        return true;
    }

    @Override
    public ServiceInnerCronJobInfoDTO getInnerJobInfo(String systemId, String jobKey) {
        try {
            QuartzJobInfoDTO jobInfo = quartzTaskHandler.getJobInfo(systemId, jobKey);
            return fromQuartzJob(jobInfo);
        } catch (SchedulerException e) {
            String msg = MessageFormatter.arrayFormat(
                "Error while get job info from scheduler|{}|{}",
                new String[]{
                    systemId,
                    jobKey
                }
            ).getMessage();
            log.error(msg, e);
            return null;
        }
    }

    @Override
    public Boolean deleteInnerCronJob(String systemId, String jobKey) {
        try {
            quartzTaskHandler.deleteJob(JobKey.jobKey(jobKey, systemId));
            return true;
        } catch (SchedulerException e) {
            String msg = MessageFormatter.arrayFormat(
                "Error while delete inner cron job|{}|{}",
                new String[]{
                    systemId,
                    jobKey
                }
            ).getMessage();
            log.error(msg, e);
            return false;
        }
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_CRON,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.CRON
        ),
        content = EventContentConstants.EDIT_CRON_JOB
    )
    public Boolean batchUpdateCronJob(String username,
                                      Long appId,
                                      BatchUpdateCronJobReq batchUpdateCronJobReq) {
        // 更新DB中的数据
        NeedScheduleCronInfo needScheduleCronInfo = batchCronJobService.batchUpdateCronJob(
            username,
            appId,
            batchUpdateCronJobReq
        );
        // 更新Quartz调度
        List<Long> needAddCronIdList = needScheduleCronInfo.getNeedAddCronIdList();
        List<Long> needDeleteCronIdList = needScheduleCronInfo.getNeedDeleteCronIdList();
        if (CollectionUtils.isNotEmpty(needAddCronIdList)) {
            needAddCronIdList.forEach(cronId -> informAllToAddJobToQuartz(appId, cronId));
        }
        if (CollectionUtils.isNotEmpty(needDeleteCronIdList)) {
            needDeleteCronIdList.forEach(cronId -> informAllToDeleteJobFromQuartz(appId, cronId));
        }
        return true;
    }

    @Override
    public Long insertCronJobInfoWithId(CronJobInfoDTO cronJobInfo) {
        checkCronJobPlanOrScript(cronJobInfo);
        CronJobInfoDTO cronJobById = cronJobDAO.getCronJobById(cronJobInfo.getAppId(), cronJobInfo.getId());
        if (cronJobById != null) {
            throw new AlreadyExistsException(ErrorCode.CRON_JOB_ALREADY_EXIST);
        }
        if (cronJobDAO.insertCronJobWithId(cronJobInfo)) {
            return cronJobInfo.getId();
        } else {
            throw new InternalException(ErrorCode.INSERT_CRON_JOB_FAILED);
        }
    }

    @Override
    public String getCronJobNameById(long id) {
        return cronJobDAO.getCronJobNameById(id);
    }

    @Override
    public List<CronJobInfoDTO> listCronJobByIds(Long appId, List<Long> cronJobIdList) {
        cronJobIdList = cronJobIdList.stream().filter(id -> id != null && id > 0).collect(Collectors.toList());
        if (appId != null && appId > 0 && CollectionUtils.isNotEmpty(cronJobIdList)) {
            return cronJobDAO.listCronJobByIds(appId, cronJobIdList);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isExistAnyAppCronJob(Long appId) {
        return cronJobDAO.isExistAnyAppCronJob(appId);
    }

    @Override
    public Integer countCronJob(Long appId, Boolean active, Boolean cron) {
        return cronJobDAO.countCronJob(appId, active, cron);
    }

    private ServiceInnerCronJobInfoDTO fromQuartzJob(QuartzJobInfoDTO jobInfo) {
        if (jobInfo == null) {
            return null;
        }
        ServiceInnerCronJobInfoDTO innerCronJobInfoDTO = new ServiceInnerCronJobInfoDTO();
        innerCronJobInfoDTO.setSystemId(jobInfo.getGroup());
        innerCronJobInfoDTO.setJobKey(jobInfo.getName());
        innerCronJobInfoDTO.setDescription(jobInfo.getDescription());
        innerCronJobInfoDTO.setCronExpression(jobInfo.getCronExpression());
        innerCronJobInfoDTO.setTimeZone(jobInfo.getTimeZone());
        innerCronJobInfoDTO.setLastFiredTime(jobInfo.getLastFiredTime());
        innerCronJobInfoDTO.setNextFiredTime(jobInfo.getNextFiredTime());
        innerCronJobInfoDTO.setStartAt(jobInfo.getStartAt());
        innerCronJobInfoDTO.setEndAt(jobInfo.getEndAt());

        String cronJobIdStr = jobInfo.getJobDataMap().getString("cronJobInfoStr");
        if (StringUtils.isNotBlank(cronJobIdStr)) {
            InnerCronJobInfoDTO innerJobInfo = JsonUtils.fromJson(cronJobIdStr, InnerCronJobInfoDTO.class);
            innerCronJobInfoDTO.setCallbackUri(innerJobInfo.getCallbackUri());
            innerCronJobInfoDTO.setCallbackBody(innerJobInfo.getCallbackData());
            innerCronJobInfoDTO.setRetryCount(innerJobInfo.getRetry());
        }

        return innerCronJobInfoDTO;
    }

    private boolean informAllToAddJobToQuartz(long appId, long cronJobId) throws ServiceException {
        try {
            crontabMQEventDispatcher.broadCastCrontabEvent(CrontabEvent.addCron(appId, cronJobId));
            return true;
        } catch (Exception e) {
            log.error("Fail to broadCast addCronEvent", e);
            return false;
        }
    }

    private void checkCronRelatedPlan(Long appId, Long taskPlanId) throws ServiceException {
        if (taskPlanService.getPlanBasicInfoById(appId, taskPlanId) == null) {
            throw new NotFoundException(ErrorCode.TASK_PLAN_NOT_EXIST);
        }
    }

    private boolean informAllToDeleteJobFromQuartz(long appId, long cronJobId) {
        try {
            crontabMQEventDispatcher.broadCastCrontabEvent(CrontabEvent.deleteCron(appId, cronJobId));
            return true;
        } catch (Exception e) {
            log.error("Fail to broadCast deleteCronEvent", e);
            return false;
        }
    }

    @Override
    public boolean checkAndAddJobToQuartz(long appId, long cronJobId) throws ServiceException {
        if (appId <= 0 || cronJobId <= 0) {
            return false;
        }
        CronJobInfoDTO cronJobInfo = getCronJobInfoById(appId, cronJobId);
        if (StringUtils.isBlank(cronJobInfo.getCronExpression())
            && cronJobInfo.getExecuteTime() < DateUtils.currentTimeSeconds()) {
            throw new FailedPreconditionException(ErrorCode.CRON_JOB_TIME_PASSED);
        }
        checkCronRelatedPlan(cronJobInfo.getAppId(), cronJobInfo.getTaskPlanId());
        quartzService.tryToAddJobToQuartz(cronJobInfo);
        return true;
    }


    @Override
    public List<CronJobBasicInfoDTO> listEnabledCronBasicInfoForUpdate(int start, int limit) {
        return cronJobDAO.listEnabledCronBasicInfoForUpdate(start, limit);
    }

    @Override
    public boolean disableCronJobByAppId(Long appId) {
        CronJobInfoDTO cronJobInfoDTO = new CronJobInfoDTO();
        cronJobInfoDTO.setAppId(appId);
        cronJobInfoDTO.setEnable(true);
        List<Long> cronJobIdList = cronJobDAO.listCronJobIds(cronJobInfoDTO);
        if (CollectionUtils.isEmpty(cronJobIdList)) {
            return true;
        }
        List<Long> failedCronJobIds = new ArrayList<>();
        log.info("cron job will be disabled, appId:{}, cronJobIds:{}", appId, cronJobIdList);
        for (Long cronJobId : cronJobIdList) {
            try {
                Boolean disableResult = changeCronJobEnableStatus(JobConstants.DEFAULT_SYSTEM_USER_ADMIN, appId,
                    cronJobId, false);
                log.debug("disable cron job, result:{}, appId:{}, cronId:{}", disableResult, appId, cronJobId);
                if (!disableResult) {
                    failedCronJobIds.add(cronJobId);
                }
            } catch (Exception e) {
                log.error("Failed to disable cron job with appId:{} and cronId:{}", appId, cronJobId, e);
                failedCronJobIds.add(cronJobId);
            }
        }
        if (!failedCronJobIds.isEmpty()) {
            log.warn("Failed to disable cron jobs for appId:{} with cronJobIds:{}", appId, failedCronJobIds);
        }
        return failedCronJobIds.isEmpty();
    }
}
