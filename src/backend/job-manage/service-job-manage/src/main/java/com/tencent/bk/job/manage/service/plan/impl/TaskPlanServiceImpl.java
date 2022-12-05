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

package com.tencent.bk.job.manage.service.plan.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.AlreadyExistsException;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.PageUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.crontab.model.CronJobVO;
import com.tencent.bk.job.manage.common.consts.task.TaskPlanTypeEnum;
import com.tencent.bk.job.manage.dao.plan.TaskPlanDAO;
import com.tencent.bk.job.manage.model.dto.TaskPlanQueryDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanBasicInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import com.tencent.bk.job.manage.service.AbstractTaskStepService;
import com.tencent.bk.job.manage.service.AbstractTaskVariableService;
import com.tencent.bk.job.manage.service.CronJobService;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @since 19/11/2019 16:43
 */
@Slf4j
@Service("TaskPlanServiceImpl")
public class TaskPlanServiceImpl implements TaskPlanService {

    private final TaskPlanDAO taskPlanDAO;
    private final MessageI18nService i18nService;
    private final CronJobService cronJobService;
    private final AbstractTaskStepService taskPlanStepService;
    private final AbstractTaskVariableService taskTemplateVariableService;
    private final AbstractTaskVariableService taskPlanVariableService;

    @Autowired
    private TaskTemplateService taskTemplateService;
    @Autowired
    private TaskPlanService taskPlanService;

    @Autowired
    public TaskPlanServiceImpl(
        TaskPlanDAO taskPlanDAO,
        @Qualifier("TaskPlanStepServiceImpl") AbstractTaskStepService taskPlanStepService,
        @Qualifier("TaskTemplateVariableServiceImpl") AbstractTaskVariableService taskTemplateVariableService,
        @Qualifier("TaskPlanVariableServiceImpl") AbstractTaskVariableService taskPlanVariableService,
        MessageI18nService i18nService, CronJobService cronJobService
    ) {
        this.taskPlanDAO = taskPlanDAO;
        this.taskPlanStepService = taskPlanStepService;
        this.taskTemplateVariableService = taskTemplateVariableService;
        this.taskPlanVariableService = taskPlanVariableService;
        this.i18nService = i18nService;
        this.cronJobService = cronJobService;
    }

    /**
     * Sync plan variable to task variable
     *
     * @param templateVariable 模版中的变量信息
     * @param planVariable     执行方案中的变量信息
     */
    private static void processVariableSync(TaskVariableDTO templateVariable, TaskVariableDTO planVariable) {
        if (planVariable != null) {
            planVariable.setDescription(templateVariable.getDescription());
            planVariable.setChangeable(templateVariable.getChangeable());
            planVariable.setRequired(templateVariable.getRequired());

            templateVariable.setId(planVariable.getId());
            templateVariable.setPlanId(planVariable.getPlanId());
            templateVariable.setTemplateId(null);
            templateVariable.setDefaultValue(planVariable.getDefaultValue());
        }
    }

    @Override
    public List<Long> listTaskPlanIds(Long templateId) {
        return taskPlanDAO.listTaskPlanIds(templateId);
    }

    @Override
    public List<TaskPlanInfoDTO> listTaskPlansBasicInfo(Long appId, Long templateId) {
        checkTemplateExist(appId, templateId);
        return taskPlanDAO.listTaskPlans(appId, templateId);
    }

    @Override
    public PageData<TaskPlanInfoDTO> listPageTaskPlansBasicInfo(TaskPlanQueryDTO taskPlanQuery,
                                                                BaseSearchCondition baseSearchCondition,
                                                                List<Long> favoritePlanIdList) {
        int start = baseSearchCondition.getStartOrDefault(0);
        int length = baseSearchCondition.getLengthOrDefault(10);
        boolean getAll = baseSearchCondition.isGetAll();

        List<TaskPlanInfoDTO> favoredPlanInfos = null;
        if (CollectionUtils.isNotEmpty(favoritePlanIdList)) {
            favoredPlanInfos = taskPlanDAO.listTaskPlanByIds(taskPlanQuery.getAppId(),
                favoritePlanIdList, taskPlanQuery, baseSearchCondition);
        }

        PageData<TaskPlanInfoDTO> taskPlanInfoPageData = PageUtil.pageQuery(getAll, favoredPlanInfos, start, length,
            finalStart -> {
                baseSearchCondition.setStart(finalStart);
                return taskPlanDAO.listPageTaskPlans(taskPlanQuery,
                    baseSearchCondition, favoritePlanIdList);
            }
        );

        if (CollectionUtils.isNotEmpty(taskPlanInfoPageData.getData())) {
            taskPlanInfoPageData.setData(fillTemplateInfo(taskPlanInfoPageData.getData().get(0).getAppId(),
                taskPlanInfoPageData.getData()));
        }
        return taskPlanInfoPageData;
    }

    @Override
    public TaskPlanInfoDTO getTaskPlanById(Long appId, Long templateId, Long planId) {
        checkTemplateExist(appId, templateId);
        TaskPlanInfoDTO taskPlan = taskPlanDAO.getTaskPlanById(appId, templateId, planId, TaskPlanTypeEnum.NORMAL);
        if (taskPlan != null) {
            taskPlan.setStepList(taskPlanStepService.listStepsByParentId(taskPlan.getId()));
            taskPlan.setVariableList(taskPlanVariableService.listVariablesByParentId(taskPlan.getId()));
            return taskPlan;
        } else {
            return null;
        }
    }

    @Override
    public TaskPlanInfoDTO getTaskPlanById(Long planId) {
        TaskPlanInfoDTO taskPlan = taskPlanDAO.getTaskPlanById(planId);
        if (taskPlan != null) {
            checkTemplateExist(taskPlan.getAppId(), taskPlan.getTemplateId());
            taskPlan.setStepList(taskPlanStepService.listStepsByParentId(taskPlan.getId()));
            taskPlan.setVariableList(taskPlanVariableService.listVariablesByParentId(taskPlan.getId()));
            return taskPlan;
        } else {
            return null;
        }
    }

    @Override
    public List<TaskPlanBasicInfoDTO> listTaskPlanByIds(Collection<Long> planIds) {
        return taskPlanDAO.listTaskPlanBasicInfoByIds(planIds);
    }

    @Override
    public TaskPlanInfoDTO getTaskPlanById(Long appId, Long planId) {
        TaskPlanInfoDTO taskPlan = taskPlanDAO.getTaskPlanById(appId, 0L, planId, null);
        if (taskPlan != null) {
            checkTemplateExist(appId, taskPlan.getTemplateId());
            taskPlan.setStepList(taskPlanStepService.listStepsByParentId(taskPlan.getId()));
            taskPlan.setVariableList(taskPlanVariableService.listVariablesByParentId(taskPlan.getId()));
            return taskPlan;
        } else {
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Long saveTaskPlan(TaskPlanInfoDTO taskPlanInfo) {
        boolean insert = false;
        if (taskPlanInfo.getId() == null || taskPlanInfo.getId() <= 0) {
            TaskTemplateInfoDTO taskTemplate =
                taskTemplateService.getTaskTemplateById(taskPlanInfo.getAppId(), taskPlanInfo.getTemplateId());
            TaskPlanInfoDTO.buildPlanInfo(taskPlanInfo, taskTemplate);
            insert = true;
        }
        // process plan id
        Long planId;
        taskPlanInfo.setLastModifyUser(taskPlanInfo.getLastModifyUser());
        taskPlanInfo.setLastModifyTime(taskPlanInfo.getLastModifyTime());

        if (insert) {
            taskPlanInfo.setCreateTime(DateUtils.currentTimeSeconds());
            planId = taskPlanDAO.insertTaskPlan(taskPlanInfo);
            if (planId == null) {
                throw new InternalException(ErrorCode.INSERT_TASK_PLAN_FAILED);
            }
            taskPlanInfo.setId(planId);
        } else {
            if (!taskPlanDAO.updateTaskPlanById(taskPlanInfo)) {
                throw new InternalException(ErrorCode.UPDATE_TASK_PLAN_FAILED);
            }
            planId = taskPlanInfo.getId();
        }

        if (insert) {
            // Save step
            for (TaskStepDTO taskStep : taskPlanInfo.getStepList()) {
                taskStep.setPlanId(planId);
                // Insert give template step id
                if (taskPlanInfo.getEnableStepList().contains(taskStep.getTemplateStepId())) {
                    taskStep.setEnable(1);
                } else {
                    taskStep.setEnable(0);
                }
                // new step, insert to get id
                taskStep.setId(taskPlanStepService.insertStep(taskStep));
            }
        } else {
            List<TaskStepDTO> taskStepList = taskPlanStepService.listStepsByParentId(planId);
            if (CollectionUtils.isNotEmpty(taskStepList)) {
                for (TaskStepDTO taskStep : taskStepList) {
                    // Update give step id
                    if (taskPlanInfo.getEnableStepList().contains(taskStep.getId())) {
                        taskStep.setEnable(1);
                    } else {
                        taskStep.setEnable(0);
                    }
                    taskPlanStepService.updateStepById(taskStep);
                }
            }
        }

        if (insert) {
            // Insert new variable
            taskPlanInfo.getVariableList().forEach(variable -> variable.setPlanId(planId));
            taskPlanVariableService.batchInsertVariable(taskPlanInfo.getVariableList());
        } else {
            for (TaskVariableDTO taskVariable : taskPlanInfo.getVariableList()) {
                taskVariable.setPlanId(planId);
                // Update exist variable
                taskPlanVariableService.updateVariableById(taskVariable);
            }
        }
        return planId;
    }

    @Override
    public Boolean deleteTaskPlan(Long appId, Long templateId, Long planId) {
        Map<Long, List<CronJobVO>> cronJobMap =
            cronJobService.batchListCronJobByPlanIds(appId, Collections.singletonList(planId));
        if (MapUtils.isNotEmpty(cronJobMap) && CollectionUtils.isNotEmpty(cronJobMap.get(planId))) {
            throw new FailedPreconditionException(ErrorCode.DELETE_PLAN_FAILED_USING_BY_CRON);
        }
        return taskPlanDAO.deleteTaskPlanById(appId, templateId, planId);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public TaskPlanInfoDTO getDebugTaskPlan(String username, Long appId, Long templateId) {
        TaskPlanInfoDTO taskPlan = taskPlanDAO.getDebugTaskPlan(appId, templateId);
        TaskTemplateInfoDTO taskTemplateInfo = taskTemplateService.getTaskTemplateBasicInfoById(appId, templateId);
        if (taskTemplateInfo == null) {
            throw new NotFoundException(ErrorCode.TEMPLATE_NOT_EXIST);
        }
        if (taskPlan != null) {
            if (!taskTemplateInfo.getVersion().equals(taskPlan.getVersion())) {
                // 作业模板有更新，需要同步到调试执行方案（不含变量）
                taskPlanService.sync(appId, templateId, taskPlan.getId(), taskTemplateInfo.getVersion());
                taskPlan = taskPlanDAO.getDebugTaskPlan(appId, templateId);
            }
            List<TaskVariableDTO> templateVariableList =
                taskTemplateVariableService.listVariablesByParentId(taskPlan.getTemplateId());
            // 始终使用模板最新变量作为执行方案变量并更新到DB
            setPlanIdForVariables(taskPlan.getId(), templateVariableList);
            int deletedVarNum = taskPlanVariableService.deleteVariableByParentId(taskPlan.getId());
            List<Long> insertedVarIds = taskPlanVariableService.batchInsertVariable(templateVariableList);
            log.debug(
                "sync template variable to debug plan:{} taskPlan variable deleted, {} variable inserted:{}",
                deletedVarNum,
                insertedVarIds.size(),
                insertedVarIds
            );
            taskPlan.setStepList(taskPlanStepService.listStepsByParentId(taskPlan.getId()));
            taskPlan.setVariableList(taskPlanVariableService.listVariablesByParentId(taskPlan.getId()));
            return taskPlan;
        }

        taskPlan = new TaskPlanInfoDTO();
        taskPlan.setAppId(appId);
        taskPlan.setTemplateId(templateId);
        taskPlan.setName(taskTemplateInfo.getName() +
            "_" + i18nService.getI18n("job.task.plan.debug")
            + "_" + System.currentTimeMillis() / 1000L
        );
        taskPlan.setCreator(username);
        taskPlan.setLastModifyUser(username);
        taskPlan.setLastModifyTime(DateUtils.currentTimeSeconds());
        taskPlan.setDebug(true);
        taskPlan.setId(taskPlanService.saveTaskPlan(taskPlan));

        taskPlan = taskPlanDAO.getTaskPlanById(appId, templateId, taskPlan.getId(), TaskPlanTypeEnum.DEBUG);
        taskPlan.setStepList(taskPlanStepService.listStepsByParentId(taskPlan.getId()));
        taskPlan.setVariableList(taskPlanVariableService.listVariablesByParentId(taskPlan.getId()));
        taskPlan.setVersion(taskTemplateInfo.getVersion());

        for (TaskStepDTO taskStep : taskPlan.getStepList()) {
            taskStep.setEnable(1);
            taskPlan.getEnableStepList().add(taskStep.getId());
        }
        taskPlanService.saveTaskPlan(taskPlan);
        return taskPlan;
    }

    private void setPlanIdForVariables(Long planId, List<TaskVariableDTO> variableList) {
        if (CollectionUtils.isEmpty(variableList)) {
            return;
        }
        variableList.forEach(variable -> variable.setPlanId(planId));
    }

    @Override
    public List<TaskPlanInfoDTO> listPlanBasicInfoByIds(Long appId, List<Long> planIdList) {
        List<TaskPlanInfoDTO> taskPlanInfoList = taskPlanDAO.listTaskPlanByIds(
            appId,
            planIdList,
            null,
            null
        );
        taskPlanInfoList.parallelStream().forEach(taskPlan ->
            taskPlan.setVariableList(taskPlanVariableService.listVariablesByParentId(taskPlan.getId())));
        return fillTemplateInfo(appId, taskPlanInfoList);
    }

    private List<TaskPlanInfoDTO> fillTemplateInfo(Long appId, List<TaskPlanInfoDTO> planList) {
        List<Long> templateIdList = planList.parallelStream().map(TaskPlanInfoDTO::getTemplateId).distinct()
            .collect(Collectors.toList());
        List<TaskTemplateInfoDTO> templateInfoList =
            taskTemplateService.listTaskTemplateBasicInfoByIds(appId, templateIdList);

        Map<Long, TaskTemplateInfoDTO> templateInfoMap = new ConcurrentHashMap<>(templateInfoList.size());
        templateInfoList.parallelStream()
            .forEach(taskTemplateInfoDTO -> templateInfoMap.put(taskTemplateInfoDTO.getId(), taskTemplateInfoDTO));
        return planList.parallelStream()
            .filter(planInfo -> templateInfoMap.containsKey(planInfo.getTemplateId())).peek(taskPlanInfoDTO -> {
                taskPlanInfoDTO.setTemplateName(templateInfoMap.get(taskPlanInfoDTO.getTemplateId()).getName());
                taskPlanInfoDTO.setTemplateVersion(templateInfoMap.get(taskPlanInfoDTO.getTemplateId()).getVersion());
                taskPlanInfoDTO.setNeedUpdate(
                    !taskPlanInfoDTO.getVersion().equals(taskPlanInfoDTO.getTemplateVersion())
                );
            }).collect(Collectors.toList());
    }

    @Override
    public Boolean checkPlanName(Long appId, Long templateId, Long planId, String name) {
        if (templateId == null || templateId <= 0) {
            return false;
        }
        if (planId == null || planId < 0) {
            return false;
        }
        name = name.trim();
        return taskPlanDAO.checkPlanName(appId, templateId, planId, name);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class, Error.class})
    public Boolean sync(Long appId, Long templateId, Long planId, String templateVersion) {
        TaskTemplateInfoDTO taskTemplate = taskTemplateService.getTaskTemplateById(appId, templateId);
        if (taskTemplate == null) {
            return false;
        }
        if (!templateVersion.equals(taskTemplate.getVersion())) {
            return false;
        }
        TaskPlanInfoDTO taskPlan = taskPlanService.getTaskPlanById(appId, planId);
        if (taskPlan == null) {
            return false;
        }
        if (!taskPlan.getTemplateId().equals(templateId)) {
            return false;
        }
        taskPlan.setVersion(taskTemplate.getVersion());
        taskPlan.setLastModifyTime(DateUtils.currentTimeSeconds());
        taskPlan.setLastModifyUser(JobContextUtil.getUsername());

        if (taskPlan.getStepList() != null) {
            taskPlan.setEnableStepList(new ArrayList<>());
            taskPlan.getStepList().forEach(taskStep -> {
                if (taskStep.getEnable() == 1) {
                    taskPlan.getEnableStepList().add(taskStep.getTemplateStepId());
                }
            });
        }
        taskTemplate.getStepList().forEach(taskStep -> {
            taskStep.setTemplateStepId(taskStep.getId());
            taskStep.setId(null);
            taskStep.setTemplateId(null);
            taskStep.setScriptStepId(null);
            taskStep.setFileStepId(null);
            taskStep.setApprovalStepId(null);
            taskStep.setPlanId(planId);
            taskStep.setEnable(taskPlan.getEnableStepList().contains(taskStep.getId()) ? 1 : 0);
        });
        taskPlan.setStepList(taskTemplate.getStepList());

        Map<Long, TaskVariableDTO> originPlanVariableMap;
        if (taskPlan.getVariableList() != null) {
            originPlanVariableMap = new HashMap<>(taskPlan.getVariableList().size());
            taskPlan.getVariableList().forEach(planVariable -> {
                originPlanVariableMap.put(planVariable.getId(), planVariable);
            });
        } else {
            originPlanVariableMap = null;
        }
        if (CollectionUtils.isNotEmpty(taskTemplate.getVariableList())) {
            taskTemplate.getVariableList().forEach(templateVariable -> {
                templateVariable.setPlanId(planId);
                if (originPlanVariableMap != null) {
                    TaskVariableDTO originPlanVariable = originPlanVariableMap.get(templateVariable.getId());
                    processVariableSync(templateVariable, originPlanVariable);
                    originPlanVariableMap.remove(templateVariable.getId());
                }
            });
            taskPlan.setVariableList(taskTemplate.getVariableList());
        } else {
            taskPlan.setVariableList(Collections.emptyList());
        }

        taskPlanService.syncPlan(taskPlan);
        return true;
    }

    @Override
    @Transactional(rollbackFor = {Exception.class, Error.class})
    public void syncPlan(TaskPlanInfoDTO taskPlanInfo) {
        try {
            // process plan id
            Long planId = taskPlanInfo.getId();

            if (!taskPlanDAO.updateTaskPlanById(taskPlanInfo)) {
                throw new InternalException(ErrorCode.UPDATE_TASK_PLAN_FAILED);
            }

            List<TaskStepDTO> taskStepList = taskPlanStepService.listStepsByParentId(planId);
            if (CollectionUtils.isNotEmpty(taskStepList)) {
                taskStepList.forEach(taskStep -> taskPlanStepService.deleteStepById(planId, taskStep.getId()));
            }

            // Save step
            for (TaskStepDTO taskStep : taskPlanInfo.getStepList()) {
                taskStep.setPlanId(planId);
                // Insert give template step id
                if (taskPlanInfo.getEnableStepList().contains(taskStep.getTemplateStepId())) {
                    taskStep.setEnable(1);
                } else {
                    taskStep.setEnable(0);
                }
                // new step, insert to get id
                taskStep.setId(taskPlanStepService.insertStep(taskStep));
            }

            List<TaskVariableDTO> oldVariableList = taskPlanVariableService.listVariablesByParentId(planId);
            List<Long> oldVariableIdList;
            if (CollectionUtils.isNotEmpty(oldVariableList)) {
                oldVariableIdList =
                    oldVariableList.parallelStream().map(TaskVariableDTO::getId).collect(Collectors.toList());
            } else {
                oldVariableIdList = null;
            }

            if (CollectionUtils.isNotEmpty(oldVariableIdList)) {
                List<TaskVariableDTO> newVariable = new ArrayList<>();
                for (TaskVariableDTO taskVariable : taskPlanInfo.getVariableList()) {
                    taskVariable.setPlanId(planId);
                    if (oldVariableIdList.contains(taskVariable.getId())) {
                        // Update exist variable
                        taskPlanVariableService.updateVariableById(taskVariable);
                        oldVariableIdList.remove(taskVariable.getId());
                    } else {
                        newVariable.add(taskVariable);
                    }
                }
                taskPlanVariableService.batchInsertVariable(newVariable);
            } else {
                taskPlanVariableService.batchInsertVariable(taskPlanInfo.getVariableList());
            }

            if (CollectionUtils.isNotEmpty(oldVariableIdList)) {
                oldVariableIdList.forEach(variableId ->
                    taskPlanVariableService.deleteVariableById(planId, variableId));
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unknown exception while sync plan!", e);
            throw new InternalException(ErrorCode.SYNC_TASK_PLAN_UNKNOWN_ERROR);
        }
    }

    @Override
    public Long saveTaskPlanForMigration(String username, Long appId, Long templateId, Long planId, Long createTime,
                                         Long lastModifyTime, String lastModifyUser) {
        try {
            TaskTemplateInfoDTO taskTemplateInfo = taskTemplateService.getTaskTemplateById(appId, templateId);
            TaskPlanInfoDTO taskPlan = new TaskPlanInfoDTO();

            taskPlan.setId(planId);
            taskPlan.setAppId(appId);
            taskPlan.setTemplateId(templateId);
            if (taskTemplateInfo != null) {
                taskPlan.setName(taskTemplateInfo.getName());
            } else {
                throw new NotFoundException(ErrorCode.TEMPLATE_NOT_EXIST);
            }
            taskPlan.setCreator(username);
            if (createTime != null && createTime > 0) {
                taskPlan.setCreateTime(createTime);
            } else {
                taskPlan.setCreateTime(DateUtils.currentTimeSeconds());
            }
            if (StringUtils.isNotBlank(lastModifyUser)) {
                taskPlan.setLastModifyUser(lastModifyUser);
            } else {
                taskPlan.setLastModifyUser(username);
            }
            if (lastModifyTime != null && lastModifyTime > 0) {
                taskPlan.setLastModifyTime(lastModifyTime);
            } else {
                taskPlan.setLastModifyTime(DateUtils.currentTimeSeconds());
            }
            taskPlan.setDebug(false);
            taskPlan.setFirstStepId(taskTemplateInfo.getFirstStepId());
            taskPlan.setLastStepId(taskTemplateInfo.getLastStepId());
            taskPlan.setNeedUpdate(false);
            taskPlan.setVersion(taskTemplateInfo.getVersion());
            taskPlanService.saveTaskPlanWithId(taskPlan);

            taskPlan = taskPlanDAO.getTaskPlanById(appId, templateId, taskPlan.getId(), TaskPlanTypeEnum.NORMAL);
            taskPlan.setStepList(taskTemplateInfo.getStepList());
            taskPlan.setVariableList(taskTemplateInfo.getVariableList());
            taskPlan.getVariableList().forEach(taskVariable -> {
                taskVariable.setTemplateId(null);
                taskVariable.setPlanId(planId);
            });
            taskPlanVariableService.batchInsertVariable(taskPlan.getVariableList());

            for (TaskStepDTO taskStep : taskPlan.getStepList()) {
                taskStep.setTemplateStepId(taskStep.getId());
                taskStep.setId(null);
                taskStep.setEnable(1);
                taskStep.setPlanId(planId);

                taskStep.setId(taskPlanStepService.insertStep(taskStep));

                taskPlan.getEnableStepList().add(taskStep.getId());
            }
            taskPlanService.saveTaskPlan(taskPlan);
            return planId;
        } catch (Exception e) {
            log.error("Error while creating debug plan", e);
            throw new InternalException(ErrorCode.CREATE_DEBUG_PLAN_ERROR);
        }
    }

    @Override
    public Boolean saveTaskPlanWithId(TaskPlanInfoDTO planInfo) {
        return taskPlanDAO.insertTaskPlanWithId(planInfo);
    }

    @Override
    public String getPlanName(long planId) {
        return taskPlanDAO.getPlanName(planId);
    }

    @Override
    public boolean isDebugPlan(Long appId, Long templateId, Long planId) {
        return taskPlanDAO.isDebugPlan(appId, templateId, planId);
    }

    @Override
    public boolean checkPlanId(Long planId) {
        return taskPlanDAO.checkPlanId(planId);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class, Error.class})
    public Long saveTaskPlanForBackup(TaskPlanInfoDTO taskPlanInfo) {
        try {
            TaskPlanInfoDTO taskPlanByName = taskPlanDAO.getTaskPlanByName(taskPlanInfo.getAppId(),
                taskPlanInfo.getTemplateId(), taskPlanInfo.getName());
            if (taskPlanByName != null) {
                throw new AlreadyExistsException(ErrorCode.PLAN_NAME_EXIST);
            }

            // process template id
            Long planId;
            if (taskPlanInfo.getId() == null || taskPlanInfo.getId() <= 0) {
                planId = taskPlanDAO.insertTaskPlan(taskPlanInfo);
            } else {
                if (taskPlanDAO.checkPlanId(taskPlanInfo.getId())) {
                    if (taskPlanDAO.insertTaskPlanWithId(taskPlanInfo)) {
                        planId = taskPlanInfo.getId();
                    } else {
                        throw new InternalException(ErrorCode.INSERT_TASK_PLAN_FAILED);
                    }
                } else {
                    throw new AlreadyExistsException(ErrorCode.PLAN_ID_EXIST);
                }
            }
            taskPlanInfo.setId(planId);
            TaskStepDTO preStep = null;
            for (TaskStepDTO taskStepDTO : taskPlanInfo.getStepList()) {
                taskStepDTO.setPlanId(planId);
                if (preStep != null) {
                    taskStepDTO.setPreviousStepId(preStep.getTemplateStepId());
                    preStep.setNextStepId(taskStepDTO.getTemplateStepId());
                    taskPlanStepService.insertStep(preStep);
                } else {
                    taskStepDTO.setPreviousStepId(0L);
                }
                preStep = taskStepDTO;
            }
            if (preStep == null) {
                throw new InternalException(ErrorCode.INSERT_TASK_PLAN_FAILED);
            }
            preStep.setNextStepId(0L);
            taskPlanStepService.insertStep(preStep);

            // Insert new variable
            List<TaskVariableDTO> variableList = taskPlanInfo.getVariableList();
            if (CollectionUtils.isNotEmpty(variableList)) {
                variableList.forEach(taskVariableDTO -> taskVariableDTO.setPlanId(planId));
                taskPlanVariableService.batchInsertVariable(variableList);
            }

            return planId;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unknown exception while insert template!", e);
            throw new InternalException(ErrorCode.SAVE_TEMPLATE_UNKNOWN_ERROR);
        }
    }

    @Override
    public boolean deleteTaskPlanByTemplate(Long appId, Long templateId) {
        return taskPlanDAO.deleteTaskPlanByTemplate(appId, templateId);
    }

    @Override
    public boolean isExistAnyAppPlan(Long appId) {
        return taskPlanDAO.isExistAnyAppPlan(appId);
    }

    @Override
    public Integer countTaskPlans(Long appId) {
        return taskPlanDAO.countTaskPlans(appId);
    }

    @Override
    public Set<String> listLocalFiles() {
        List<Long> planIdList = taskPlanDAO.listAllPlanId();
        if (CollectionUtils.isEmpty(planIdList)) {
            return new HashSet<>();
        }
        List<Long> stepIdList = taskPlanStepService.listStepIdByParentId(planIdList);
        if (CollectionUtils.isEmpty(stepIdList)) {
            return new HashSet<>();
        }
        List<String> localFileList = taskPlanStepService.listLocalFileByStepId(stepIdList);
        if (CollectionUtils.isEmpty(localFileList)) {
            return new HashSet<>();
        }
        return new HashSet<>(localFileList);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class, Error.class})
    public boolean batchUpdatePlanVariable(List<TaskPlanInfoDTO> planInfoList) {
        if (CollectionUtils.isEmpty(planInfoList)) {
            return true;
        }
        for (TaskPlanInfoDTO planInfo : planInfoList) {
            try {
                if (taskPlanVariableService.batchUpdateVariableByName(planInfo.getVariableList())) {
                    if (taskPlanDAO.updateTaskPlanById(planInfo)) {
                        continue;
                    } else {
                        log.error("Error while updating plan info after batch update variable value!|{}", planInfo);
                    }
                } else {
                    log.error("Error while batch update variable value!|{}", planInfo);
                }
                throw new InternalException(ErrorCode.BATCH_UPDATE_PLAN_VARIABLE_FAILED);
            } catch (Exception e) {
                log.error("Error while batch update plan variable value!|{}", planInfo, e);
                throw e;
            }
        }
        return true;
    }

    private void checkTemplateExist(Long appId, Long templateId) {
        TaskTemplateInfoDTO taskTemplateBasicInfo = taskTemplateService.getTaskTemplateBasicInfoById(appId, templateId);
        if (taskTemplateBasicInfo == null) {
            throw new NotFoundException(ErrorCode.TEMPLATE_NOT_EXIST);
        }
    }
}
