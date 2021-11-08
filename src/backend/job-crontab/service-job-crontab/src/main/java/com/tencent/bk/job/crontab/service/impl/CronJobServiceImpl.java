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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.AlreadyExistsException;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.iam.constant.ResourceId;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.crontab.dao.CronJobDAO;
import com.tencent.bk.job.crontab.exception.TaskExecuteAuthFailedException;
import com.tencent.bk.job.crontab.model.BatchUpdateCronJobReq;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.model.dto.CronJobVariableDTO;
import com.tencent.bk.job.crontab.model.dto.InnerCronJobInfoDTO;
import com.tencent.bk.job.crontab.model.dto.QuartzJobInfoDTO;
import com.tencent.bk.job.crontab.model.inner.ServiceInnerCronJobInfoDTO;
import com.tencent.bk.job.crontab.model.inner.request.ServiceAddInnerCronJobRequestDTO;
import com.tencent.bk.job.crontab.service.CronJobService;
import com.tencent.bk.job.crontab.service.ExecuteTaskService;
import com.tencent.bk.job.crontab.service.TaskExecuteResultService;
import com.tencent.bk.job.crontab.service.TaskPlanService;
import com.tencent.bk.job.crontab.timer.AbstractQuartzTaskHandler;
import com.tencent.bk.job.crontab.timer.QuartzJob;
import com.tencent.bk.job.crontab.timer.QuartzJobBuilder;
import com.tencent.bk.job.crontab.timer.QuartzTrigger;
import com.tencent.bk.job.crontab.timer.QuartzTriggerBuilder;
import com.tencent.bk.job.crontab.timer.executor.InnerJobExecutor;
import com.tencent.bk.job.crontab.timer.executor.NotifyJobExecutor;
import com.tencent.bk.job.crontab.timer.executor.SimpleJobExecutor;
import com.tencent.bk.job.execute.model.inner.ServiceCronTaskExecuteResultStatistics;
import com.tencent.bk.job.execute.model.inner.ServiceTaskVariable;
import com.tencent.bk.job.manage.model.inner.ServiceTaskPlanDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.quartz.CronTrigger;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @since 2/1/2020 12:18
 */
@Slf4j
@Component
public class CronJobServiceImpl implements CronJobService {

    private final CronJobDAO cronJobDAO;
    private final TaskExecuteResultService taskExecuteResultService;

    private final AbstractQuartzTaskHandler quartzTaskHandler;
    private final TaskPlanService taskPlanService;

    private final AuthService authService;

    private final ExecuteTaskService executeTaskService;

    @Autowired
    public CronJobServiceImpl(CronJobDAO cronJobDAO, TaskExecuteResultService taskExecuteResultService,
                              AbstractQuartzTaskHandler quartzTaskHandler, TaskPlanService taskPlanService,
                              AuthService authService, ExecuteTaskService executeTaskService) {
        this.cronJobDAO = cronJobDAO;
        this.taskExecuteResultService = taskExecuteResultService;
        this.quartzTaskHandler = quartzTaskHandler;
        this.taskPlanService = taskPlanService;
        this.authService = authService;
        this.executeTaskService = executeTaskService;
    }

    private static String getJobName(long appId, long cronJobId) {
        return "job_" + cronJobId;
    }

    private static String getJobGroup(long appId, long cronJobId) {
        return "bk_app_" + appId;
    }

    private static String getNotifyJobName(long appId, long cronJobId) {
        return getJobName(appId, cronJobId) + "_notify";
    }

    @Override
    public PageData<CronJobInfoDTO> listPageCronJobInfos(CronJobInfoDTO cronJobCondition,
                                                         BaseSearchCondition baseSearchCondition) {
        return cronJobDAO.listPageCronJobsByCondition(cronJobCondition, baseSearchCondition);
    }

    @Override
    public CronJobInfoDTO getCronJobInfoById(Long cronJobId) {
        return cronJobDAO.getCronJobById(cronJobId);
    }

    @Override
    public List<CronJobInfoDTO> getOrderedCronJobInfoByIds(List<Long> cronJobIdList) {
        Map<Long, CronJobInfoDTO> map = getCronJobInfoMapByIds(cronJobIdList);
        List<CronJobInfoDTO> cronJobInfoDTOList = new ArrayList<>();
        for (Long id : cronJobIdList) {
            CronJobInfoDTO cronJobInfoDTO = map.get(id);
            if (cronJobInfoDTO != null) {
                cronJobInfoDTOList.add(cronJobInfoDTO);
            }
        }
        return cronJobInfoDTOList;
    }

    @Override
    public Map<Long, CronJobInfoDTO> getCronJobInfoMapByIds(List<Long> cronJobIdList) {
        List<CronJobInfoDTO> cronJobInfoDTOList = cronJobDAO.getCronJobByIds(cronJobIdList);
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
    public CronJobInfoDTO getCronJobErrorInfoById(Long appId, Long cronJobId) {
        return cronJobDAO.getCronJobErrorById(appId, cronJobId);
    }

    @Override
    public boolean updateCronJobErrorById(CronJobInfoDTO cronJobErrorInfo) {
        return cronJobDAO.updateCronJobErrorById(cronJobErrorInfo);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class, Error.class})
    public Long saveCronJobInfo(CronJobInfoDTO cronJobInfo) {
        checkCronJobPlanOrScript(cronJobInfo);
        if (cronJobInfo.getId() == null || cronJobInfo.getId() == 0) {
            cronJobInfo.setCreateTime(DateUtils.currentTimeSeconds());
            cronJobInfo.setEnable(false);
            Long id = cronJobDAO.insertCronJob(cronJobInfo);
            authService.registerResource(id.toString(), cronJobInfo.getName(), ResourceId.CRON,
                cronJobInfo.getCreator(), null);
            return id;
        } else {
            checkCronJobVariableValue(cronJobInfo);
            if (cronJobInfo.getEnable()) {
                try {
                    List<ServiceTaskVariable> taskVariables = null;
                    if (CollectionUtils.isNotEmpty(cronJobInfo.getVariableValue())) {
                        taskVariables =
                            cronJobInfo.getVariableValue().parallelStream()
                                .map(CronJobVariableDTO::toServiceTaskVariable).collect(Collectors.toList());
                    }
                    executeTaskService.authExecuteTask(cronJobInfo.getAppId(), cronJobInfo.getTaskPlanId(),
                        cronJobInfo.getId(), cronJobInfo.getName(), taskVariables, cronJobInfo.getLastModifyUser());
                    if (cronJobDAO.updateCronJobById(cronJobInfo)) {
                        addJob(cronJobInfo.getAppId(), cronJobInfo.getId());
                    } else {
                        throw new InternalException(ErrorCode.UPDATE_CRON_JOB_FAILED);
                    }
                } catch (TaskExecuteAuthFailedException e) {
                    log.error("Error while pre auth cron execute!", e);
                    throw e;
                }
            } else {
                if (cronJobDAO.updateCronJobById(cronJobInfo)) {
                    deleteJob(cronJobInfo.getAppId(), cronJobInfo.getId());
                } else {
                    throw new InternalException(ErrorCode.UPDATE_CRON_JOB_FAILED);
                }
            }
            return cronJobInfo.getId();
        }
    }

    private void checkCronJobVariableValue(CronJobInfoDTO cronJobInfo) {
        if (CollectionUtils.isNotEmpty(cronJobInfo.getVariableValue())) {
            List<CronJobVariableDTO> hasMaskVariableList = new ArrayList<>();
            for (CronJobVariableDTO cronJobVariable : cronJobInfo.getVariableValue()) {
                if (cronJobVariable.getType().needMask()) {
                    if (cronJobVariable.getValue().equals(cronJobVariable.getType().getMask())) {
                        hasMaskVariableList.add(cronJobVariable);
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(hasMaskVariableList)) {
                CronJobInfoDTO originCronJob =
                    cronJobDAO.getCronJobById(cronJobInfo.getAppId(), cronJobInfo.getId());
                if (CollectionUtils.isNotEmpty(originCronJob.getVariableValue())) {
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
    public Boolean deleteCronJobInfo(Long appId, Long cronJobId) {
        if (cronJobDAO.deleteCronJobById(appId, cronJobId)) {
            deleteJob(appId, cronJobId);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = {Exception.class, Error.class})
    public Boolean changeCronJobEnableStatus(String username, Long appId, Long cronJobId, Boolean enable) {
        CronJobInfoDTO cronJobInfo = new CronJobInfoDTO();
        cronJobInfo.setAppId(appId);
        cronJobInfo.setId(cronJobId);
        cronJobInfo.setEnable(enable);
        cronJobInfo.setLastModifyUser(username);
        cronJobInfo.setLastModifyTime(DateUtils.currentTimeSeconds());
        if (enable) {
            try {
                CronJobInfoDTO originCronJobInfo = cronJobDAO.getCronJobById(appId, cronJobId);
                List<ServiceTaskVariable> taskVariables = null;
                if (CollectionUtils.isNotEmpty(originCronJobInfo.getVariableValue())) {
                    taskVariables =
                        originCronJobInfo.getVariableValue().parallelStream()
                            .map(CronJobVariableDTO::toServiceTaskVariable).collect(Collectors.toList());
                }
                executeTaskService.authExecuteTask(appId, originCronJobInfo.getTaskPlanId(),
                    cronJobId, originCronJobInfo.getName(), taskVariables, username);
                if (cronJobDAO.updateCronJobById(cronJobInfo)) {
                    return addJob(appId, cronJobId);
                } else {
                    return false;
                }
            } catch (TaskExecuteAuthFailedException e) {
                log.error("Error while pre auth cron execute!", e);
                throw e;
            }
        } else {
            if (cronJobDAO.updateCronJobById(cronJobInfo)) {
                return deleteJob(appId, cronJobId);
            } else {
                return false;
            }
        }

    }

    @Override
    @Transactional(rollbackFor = {Exception.class, Error.class})
    public Boolean disableExpiredCronJob(Long appId, Long cronJobId, String lastModifyUser, Long lastModifyTime) {
        CronJobInfoDTO cronJobInfo = new CronJobInfoDTO();
        cronJobInfo.setAppId(appId);
        cronJobInfo.setId(cronJobId);
        cronJobInfo.setLastModifyUser(lastModifyUser);
        cronJobInfo.setLastModifyTime(lastModifyTime);
        cronJobInfo.setEnable(false);
        if (cronJobDAO.updateCronJobById(cronJobInfo)) {
            return deleteJob(appId, cronJobId);
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
    @Transactional(rollbackFor = {Error.class, Exception.class})
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
            .forJob(InnerJobExecutor.class).usingJobData("cronJobInfoStr", innerCronJobInfoStr).withTrigger(trigger)
            .build();

        try {
            if (quartzTaskHandler.checkExists(job.getKey())) {
                quartzTaskHandler.deleteJob(job.getKey());
            }
            quartzTaskHandler.addJob(job);
        } catch (SchedulerException e) {
            log.error("Error while add job to quartz!", e);
            throw new InternalException(e, ErrorCode.INTERNAL_ERROR, "Add to quartz failed!");
        }

        return true;
    }

    @Override
    public ServiceInnerCronJobInfoDTO getInnerJobInfo(String systemId, String jobKey) {
        try {
            QuartzJobInfoDTO jobInfo = quartzTaskHandler.getJobInfo(systemId, jobKey);
            return fromQuartzJob(jobInfo);
        } catch (SchedulerException e) {
            log.error("Error while get job info from scheduler|{}|{}", systemId, jobKey, e);
            return null;
        }
    }

    @Override
    public Boolean deleteInnerCronJob(String systemId, String jobKey) {
        try {
            quartzTaskHandler.deleteJob(JobKey.jobKey(jobKey, systemId));
            return true;
        } catch (SchedulerException e) {
            log.error("Error while delete inner cron job|{}|{}", systemId, jobKey, e);
            return false;
        }
    }

    @Override
    public Map<Long, ServiceCronTaskExecuteResultStatistics> getCronJobExecuteHistory(Long appId,
                                                                                      List<Long> cronIdList) {
        return taskExecuteResultService.getCronTaskExecuteResultStatistics(appId, cronIdList);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class, Error.class})
    public Boolean batchUpdateCronJob(Long appId, BatchUpdateCronJobReq batchUpdateCronJobReq) {
        if (batchUpdateCronJobReq != null) {
            if (CollectionUtils.isNotEmpty(batchUpdateCronJobReq.getCronJobInfoList())) {
                batchUpdateCronJobReq.getCronJobInfoList().forEach(cronJobInfo -> {
                    CronJobInfoDTO cronJobInfoFromReq =
                        CronJobInfoDTO.fromReq(JobContextUtil.getUsername(), appId, cronJobInfo);
                    cronJobInfoFromReq.setEnable(cronJobInfo.getEnable());
                    if (cronJobInfo.getEnable()) {
                        try {
                            CronJobInfoDTO originCronJobInfo = cronJobDAO.getCronJobById(appId, cronJobInfo.getId());
                            List<ServiceTaskVariable> taskVariables = null;
                            if (CollectionUtils.isNotEmpty(originCronJobInfo.getVariableValue())) {
                                taskVariables =
                                    originCronJobInfo.getVariableValue().parallelStream()
                                        .map(CronJobVariableDTO::toServiceTaskVariable).collect(Collectors.toList());
                            }
                            executeTaskService.authExecuteTask(appId, originCronJobInfo.getTaskPlanId(),
                                cronJobInfo.getId(), originCronJobInfo.getName(), taskVariables,
                                JobContextUtil.getUsername());
                            if (cronJobDAO.updateCronJobById(cronJobInfoFromReq)) {
                                addJob(appId, cronJobInfo.getId());
                            }
                        } catch (TaskExecuteAuthFailedException e) {
                            log.error("Error while pre auth cron execute!", e);
                            throw e;
                        }
                    } else {
                        if (cronJobDAO.updateCronJobById(cronJobInfoFromReq)) {
                            deleteJob(appId, cronJobInfo.getId());
                        }
                    }
                });
            }
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
        cronJobIdList = cronJobIdList.parallelStream().filter(id -> id != null && id > 0).collect(Collectors.toList());
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

    private boolean addJob(long appId, long cronJobId) throws ServiceException {
        if (appId <= 0 || cronJobId <= 0) {
            return false;
        }
        String lockKey = appId + ":" + cronJobId;
        if (LockUtils.tryGetDistributedLock(lockKey, JobContextUtil.getRequestId(), 60_000)) {
            try {
                CronJobInfoDTO cronJobInfo = getCronJobInfoById(appId, cronJobId);
                if (StringUtils.isBlank(cronJobInfo.getCronExpression())
                    && cronJobInfo.getExecuteTime() < DateUtils.currentTimeSeconds()) {
                    throw new FailedPreconditionException(ErrorCode.CRON_JOB_TIME_PASSED);
                }
                checkCronRelatedPlan(cronJobInfo.getAppId(), cronJobInfo.getTaskPlanId());
                QuartzTrigger trigger = null;
                if (StringUtils.isNotBlank(cronJobInfo.getCronExpression())) {
                    QuartzTriggerBuilder cronTriggerBuilder =
                        QuartzTriggerBuilder.newTrigger().ofType(QuartzTrigger.TriggerType.CRON)
                            .withIdentity(getJobName(appId, cronJobId), getJobGroup(appId, cronJobId))
                            .withCronExpression(cronJobInfo.getCronExpression())
                            .withMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
                    if (cronJobInfo.getEndTime() > 0) {
                        if (cronJobInfo.getEndTime() < DateUtils.currentTimeSeconds()) {
                            throw new FailedPreconditionException(ErrorCode.END_TIME_OR_NOTIFY_TIME_ALREADY_PASSED);
                        } else {
                            cronTriggerBuilder =
                                cronTriggerBuilder.endAt(Date.from(Instant.ofEpochSecond(cronJobInfo.getEndTime())));
                        }
                    }
                    trigger = cronTriggerBuilder.build();
                } else if (cronJobInfo.getExecuteTime() > DateUtils.currentTimeSeconds()) {
                    trigger = QuartzTriggerBuilder.newTrigger().ofType(QuartzTrigger.TriggerType.SIMPLE)
                        .withIdentity(getJobName(appId, cronJobId), getJobGroup(appId, cronJobId))
                        .startAt(Date.from(Instant.ofEpochSecond(cronJobInfo.getExecuteTime()))).withRepeatCount(0)
                        .withIntervalInHours(1)
                        .withMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT)
                        .build();
                }
                if (trigger == null) {
                    throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
                }

                QuartzJob job =
                    QuartzJobBuilder.newJob().withIdentity(getJobName(appId, cronJobId), getJobGroup(appId, cronJobId))
                        .forJob(SimpleJobExecutor.class).usingJobData("appIdStr", String.valueOf(appId))
                        .usingJobData("cronJobIdStr", String.valueOf(cronJobId)).withTrigger(trigger).build();

                try {
                    quartzTaskHandler
                        .deleteJob(JobKey.jobKey(getJobName(appId, cronJobId), getJobGroup(appId, cronJobId)));
                    quartzTaskHandler.addJob(job);
                } catch (SchedulerException e) {
                    log.error("Error while add job to quartz!", e);
                    throw new InternalException(e, ErrorCode.INTERNAL_ERROR, "Add to quartz failed!");
                }

                if (cronJobInfo.getNotifyOffset() > 0) {
                    long notifyTime = 0L;
                    if (StringUtils.isNotBlank(cronJobInfo.getCronExpression())) {
                        if (cronJobInfo.getEndTime() > 0) {
                            notifyTime = cronJobInfo.getEndTime() - cronJobInfo.getNotifyOffset();
                        }
                    } else {
                        notifyTime = cronJobInfo.getExecuteTime() - cronJobInfo.getNotifyOffset();
                    }
                    if (notifyTime < DateUtils.currentTimeSeconds()) {
                        throw new FailedPreconditionException(ErrorCode.END_TIME_OR_NOTIFY_TIME_ALREADY_PASSED);
                    }

                    QuartzTrigger notifyTrigger = QuartzTriggerBuilder.newTrigger()
                        .ofType(QuartzTrigger.TriggerType.SIMPLE)
                        .withIdentity(getNotifyJobName(appId, cronJobId), getJobGroup(appId, cronJobId))
                        .startAt(Date.from(Instant.ofEpochSecond(notifyTime))).withRepeatCount(0).withIntervalInHours(1)
                        .withMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT)
                        .build();

                    QuartzJob notifyJob = QuartzJobBuilder.newJob()
                        .withIdentity(getNotifyJobName(appId, cronJobId), getJobGroup(appId, cronJobId))
                        .forJob(NotifyJobExecutor.class).usingJobData("appIdStr", String.valueOf(appId))
                        .usingJobData("cronJobIdStr", String.valueOf(cronJobId)).withTrigger(notifyTrigger).build();

                    try {
                        quartzTaskHandler.deleteJob(
                            JobKey.jobKey(getNotifyJobName(appId, cronJobId), getJobGroup(appId, cronJobId)));
                        quartzTaskHandler.addJob(notifyJob);
                    } catch (SchedulerException e) {
                        log.error("Error while add job to quartz!", e);
                        throw new InternalException(e, ErrorCode.INTERNAL_ERROR, "Add to quartz failed!");
                    }
                } else {
                    try {
                        quartzTaskHandler.deleteJob(
                            JobKey.jobKey(getNotifyJobName(appId, cronJobId), getJobGroup(appId, cronJobId)));
                    } catch (SchedulerException e) {
                        log.error("Error while add job to quartz!", e);
                        throw new InternalException(e, ErrorCode.INTERNAL_ERROR, "Add to quartz failed!");
                    }
                }
                return true;
            } catch (ServiceException e) {
                deleteJob(appId, cronJobId);
                log.debug("Error while schedule job", e);
                throw e;
            } catch (Exception e) {
                deleteJob(appId, cronJobId);
                log.error("Unknown exception while process cron status change!", e);
                throw new InternalException(ErrorCode.UPDATE_CRON_JOB_FAILED);
            } finally {
                LockUtils.releaseDistributedLock(lockKey, JobContextUtil.getRequestId());
            }
        } else {
            throw new InternalException(ErrorCode.ACQUIRE_CRON_JOB_LOCK_FAILED);
        }
    }

    private void checkCronRelatedPlan(Long appId, Long taskPlanId) throws ServiceException {
        if (taskPlanService.getPlanBasicInfoById(appId, taskPlanId) == null) {
            throw new NotFoundException(ErrorCode.TASK_PLAN_NOT_EXIST);
        }
    }

    private boolean deleteJob(long appId, long cronJobId) {
        if (appId <= 0 || cronJobId <= 0) {
            return false;
        }
        String lockKey = appId + ":" + cronJobId;
        if (LockUtils.tryGetDistributedLock(lockKey, JobContextUtil.getRequestId(), 60_000)) {
            try {
                quartzTaskHandler.deleteJob(JobKey.jobKey(getJobName(appId, cronJobId), getJobGroup(appId, cronJobId)));
                quartzTaskHandler
                    .deleteJob(JobKey.jobKey(getNotifyJobName(appId, cronJobId), getJobGroup(appId, cronJobId)));
                return true;
            } catch (SchedulerException e) {
                log.error("Error while delete job!", e);
            } finally {
                LockUtils.releaseDistributedLock(lockKey, JobContextUtil.getRequestId());
            }
        }
        return false;
    }
}
