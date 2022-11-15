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

package com.tencent.bk.job.manage.api.web.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.BusinessAuthService;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import com.tencent.bk.job.common.model.vo.TaskTargetVO;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.check.IlegalCharChecker;
import com.tencent.bk.job.common.util.check.MaxLengthChecker;
import com.tencent.bk.job.common.util.check.NotEmptyChecker;
import com.tencent.bk.job.common.util.check.StringCheckHelper;
import com.tencent.bk.job.common.util.check.TrimChecker;
import com.tencent.bk.job.common.util.check.exception.StringCheckException;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.crontab.model.CronJobVO;
import com.tencent.bk.job.manage.api.web.WebTaskPlanResource;
import com.tencent.bk.job.manage.auth.PlanAuthService;
import com.tencent.bk.job.manage.auth.TemplateAuthService;
import com.tencent.bk.job.manage.manager.variable.StepRefVariableParser;
import com.tencent.bk.job.manage.model.dto.TaskPlanQueryDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import com.tencent.bk.job.manage.model.query.TaskTemplateQuery;
import com.tencent.bk.job.manage.model.web.request.TaskPlanCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.request.TaskVariableValueUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.task.TaskFileSourceInfoVO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskFileStepVO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskPlanSyncInfoVO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskPlanVO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskStepVO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskVariableVO;
import com.tencent.bk.job.manage.service.CronJobService;
import com.tencent.bk.job.manage.service.TaskFavoriteService;
import com.tencent.bk.job.manage.service.host.HostService;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @since 19/11/2019 16:30
 */
@Slf4j
@RestController
public class WebTaskPlanResourceImpl implements WebTaskPlanResource {

    private final TaskPlanService planService;
    private final TaskTemplateService templateService;
    private final TaskFavoriteService taskFavoriteService;
    private final CronJobService cronJobService;
    private final BusinessAuthService businessAuthService;
    private final TemplateAuthService templateAuthService;
    private final PlanAuthService planAuthService;
    private final HostService hostService;

    @Autowired
    public WebTaskPlanResourceImpl(TaskPlanService planService,
                                   TaskTemplateService templateService,
                                   @Qualifier("TaskPlanFavoriteServiceImpl") TaskFavoriteService taskFavoriteService,
                                   CronJobService cronJobService,
                                   BusinessAuthService businessAuthService,
                                   TemplateAuthService templateAuthService,
                                   PlanAuthService planAuthService,
                                   AppScopeMappingService appScopeMappingService,
                                   HostService hostService) {
        this.planService = planService;
        this.templateService = templateService;
        this.taskFavoriteService = taskFavoriteService;
        this.cronJobService = cronJobService;
        this.businessAuthService = businessAuthService;
        this.templateAuthService = templateAuthService;
        this.planAuthService = planAuthService;
        this.hostService = hostService;
    }

    @Override
    public Response<PageData<TaskPlanVO>> listAllPlans(String username,
                                                       AppResourceScope appResourceScope,
                                                       String scopeType,
                                                       String scopeId,
                                                       Long planId,
                                                       String templateName,
                                                       Long templateId,
                                                       String planName,
                                                       String creator,
                                                       String lastModifyUser,
                                                       Integer start,
                                                       Integer pageSize) {
        AuthResult authResult = businessAuthService.authAccessBusiness(username, appResourceScope);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        List<Long> favoriteList = taskFavoriteService.listFavorites(appResourceScope.getAppId(), username);
        TaskPlanQueryDTO taskPlanQueryDTO = new TaskPlanQueryDTO();
        taskPlanQueryDTO.setAppId(appResourceScope.getAppId());
        if (planId != null && planId > 0) {
            taskPlanQueryDTO.setPlanId(planId);
            templateName = null;
            planName = null;
            creator = null;
            lastModifyUser = null;
        }

        if (templateId != null && templateId > 0) {
            TaskTemplateInfoDTO templateInfo =
                templateService.getTaskTemplateBasicInfoById(appResourceScope.getAppId(), templateId);
            if (templateInfo != null) {
                taskPlanQueryDTO.setTemplateId(templateId);
            } else {
                return Response.buildSuccessResp(makeEmptyResponse(start, pageSize));
            }
        } else {
            if (StringUtils.isNotBlank(templateName)) {
                BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
                baseSearchCondition.setStart(-1);
                baseSearchCondition.setLength(-1);
                TaskTemplateQuery query =
                    TaskTemplateQuery.builder().appId(appResourceScope.getAppId()).name(templateName)
                        .baseSearchCondition(baseSearchCondition).build();

                PageData<TaskTemplateInfoDTO> taskTemplateInfoPageData =
                    templateService.listPageTaskTemplatesBasicInfo(query, null);
                if (taskTemplateInfoPageData != null
                    && CollectionUtils.isNotEmpty(taskTemplateInfoPageData.getData())) {
                    List<Long> templateIdList = new ArrayList<>();
                    taskTemplateInfoPageData.getData().forEach(taskTemplateInfo ->
                        templateIdList.add(taskTemplateInfo.getId()));
                    taskPlanQueryDTO.setTemplateIdList(templateIdList);
                } else {
                    return Response.buildSuccessResp(makeEmptyResponse(start, pageSize));
                }
            }
        }

        if (StringUtils.isNotBlank(planName)) {
            taskPlanQueryDTO.setName(planName);
        }
        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        if (StringUtils.isNotBlank(creator)) {
            baseSearchCondition.setCreator(creator);
        }
        if (StringUtils.isNotBlank(lastModifyUser)) {
            baseSearchCondition.setLastModifyUser(lastModifyUser);
        }

        baseSearchCondition.setStart(start);
        baseSearchCondition.setLength(pageSize);

        PageData<TaskPlanInfoDTO> taskPlanInfoPageData =
            planService.listPageTaskPlansBasicInfo(taskPlanQueryDTO, baseSearchCondition, favoriteList);
        List<TaskPlanVO> resultPlans = new ArrayList<>();
        if (taskPlanInfoPageData != null) {
            fillCronInfo(appResourceScope.getAppId(), taskPlanInfoPageData.getData());
            taskPlanInfoPageData.getData().forEach(taskPlanInfo -> resultPlans.add(TaskPlanInfoDTO.toVO(taskPlanInfo)));
        } else {

            return Response.buildSuccessResp(PageData.emptyPageData(start, pageSize));
        }

        resultPlans.forEach(taskPlan -> taskPlan.setFavored(favoriteList.contains(taskPlan.getId()) ? 1 : 0));
        processPlanPermission(username, appResourceScope, resultPlans);

        PageData<TaskPlanVO> taskPlanPageData = new PageData<>();
        taskPlanPageData.setStart(taskPlanInfoPageData.getStart());
        taskPlanPageData.setPageSize(taskPlanInfoPageData.getPageSize());
        taskPlanPageData.setTotal(taskPlanInfoPageData.getTotal());
        taskPlanPageData.setData(resultPlans);
        taskPlanPageData.setExistAny(planService.isExistAnyAppPlan(appResourceScope.getAppId()));
        return Response.buildSuccessResp(taskPlanPageData);
    }

    @Override
    public Response<List<TaskPlanVO>> listPlans(String username,
                                                AppResourceScope appResourceScope,
                                                String scopeType,
                                                String scopeId,
                                                Long templateId) {
        AuthResult authResult = businessAuthService.authAccessBusiness(username, appResourceScope);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        List<TaskPlanVO> taskPlanList = listPlansByTemplateId(username, appResourceScope, templateId);
        return Response.buildSuccessResp(taskPlanList);
    }

    private List<TaskPlanVO> listPlansByTemplateId(String username, AppResourceScope appResourceScope,
                                                   Long templateId) {
        Long appId = appResourceScope.getAppId();
        TaskTemplateInfoDTO taskTemplateBasicInfo = templateService.getTaskTemplateBasicInfoById(appId, templateId);
        if (taskTemplateBasicInfo == null) {
            throw new NotFoundException(ErrorCode.TEMPLATE_NOT_EXIST);
        }

        List<TaskPlanInfoDTO> taskPlanInfoList = planService.listTaskPlansBasicInfo(appId, templateId);
        fillCronInfo(appId, taskPlanInfoList);
        final String templateVersion = taskTemplateBasicInfo.getVersion();
        List<TaskPlanVO> taskPlanList = taskPlanInfoList.stream().map(taskPlanInfo -> {
            taskPlanInfo.setTemplateVersion(templateVersion);
            taskPlanInfo.setNeedUpdate(!templateVersion.equals(taskPlanInfo.getVersion()));
            return TaskPlanInfoDTO.toVO(taskPlanInfo);
        }).collect(Collectors.toList());
        processPlanPermission(username, appResourceScope, taskPlanList);
        return taskPlanList;
    }

    private void processPlanPermission(String username, AppResourceScope appResourceScope,
                                       List<TaskPlanVO> taskPlanList) {
        List<Long> jobTemplateIdList =
            taskPlanList.parallelStream().map(TaskPlanVO::getTemplateId).collect(Collectors.toList());
        List<Long> jobPlanIdList =
            taskPlanList.parallelStream().map(TaskPlanVO::getId).collect(Collectors.toList());
        List<Long> allowedViewPlan =
            planAuthService.batchAuthViewJobPlan(username, appResourceScope, jobTemplateIdList,
                jobPlanIdList);
        List<Long> allowedEditPlan =
            planAuthService.batchAuthEditJobPlan(username, appResourceScope, jobTemplateIdList,
                jobPlanIdList);
        List<Long> allowedDeletePlan =
            planAuthService.batchAuthDeleteJobPlan(username, appResourceScope, jobTemplateIdList,
                jobPlanIdList);

        taskPlanList.forEach(plan -> {
            plan.setCanView(allowedViewPlan.contains(plan.getId()));
            plan.setCanEdit(allowedEditPlan.contains(plan.getId()));
            plan.setCanDelete(allowedDeletePlan.contains(plan.getId()));
            if (!plan.getCanView()) {
                plan.setVariableList(null);
            }
        });
    }

    @Override
    public Response<List<TaskPlanVO>> batchGetPlans(String username,
                                                    AppResourceScope appResourceScope,
                                                    String scopeType,
                                                    String scopeId,
                                                    String templateIds) {
        if (StringUtils.isEmpty(templateIds)) {
            log.warn("TemplateIds is empty!");
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM);
        }

        String[] templateIdArray = templateIds.split(",");
        List<Long> templateIdList = new ArrayList<>();
        for (String templateIdStr : templateIdArray) {
            templateIdList.add(Long.parseLong(templateIdStr));
        }
        if (CollectionUtils.isEmpty(templateIdList)) {
            log.warn("TemplateIdList is empty!");
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM);
        }

        AuthResult authResult = businessAuthService.authAccessBusiness(username, appResourceScope);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        List<TaskPlanVO> planList = new ArrayList<>();
        for (Long templateId : templateIdList) {
            List<TaskPlanVO> templatePlanList = listPlansByTemplateId(username, appResourceScope,
                templateId);
            if (CollectionUtils.isNotEmpty(templatePlanList)) {
                planList.addAll(templatePlanList);
            }
        }

        return Response.buildSuccessResp(planList);
    }

    @Override
    public Response<TaskPlanVO> getPlanById(String username,
                                            AppResourceScope appResourceScope,
                                            String scopeType,
                                            String scopeId,
                                            Long templateId,
                                            Long planId) {
        TaskTemplateInfoDTO taskTemplateBasicInfo =
            templateService.getTaskTemplateBasicInfoById(appResourceScope.getAppId(), templateId);
        if (taskTemplateBasicInfo == null) {
            throw new NotFoundException(ErrorCode.TASK_PLAN_NOT_EXIST);
        }
        TaskPlanInfoDTO taskPlan = planService.getTaskPlanById(appResourceScope.getAppId(), templateId, planId);
        if (taskPlan == null) {
            throw new NotFoundException(ErrorCode.TASK_PLAN_NOT_EXIST);
        }
        AuthResult authResult = planAuthService.authViewJobPlan(username, appResourceScope, templateId,
            planId, taskPlan.getName());
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        StepRefVariableParser.parseStepRefVars(taskPlan.getStepList(), taskPlan.getVariableList());

        final String templateVersion = taskTemplateBasicInfo.getVersion();
        if (StringUtils.isNotEmpty(templateVersion)) {
            taskPlan.setTemplateVersion(templateVersion);
            taskPlan.setNeedUpdate(!templateVersion.equals(taskPlan.getVersion()));
        }
        fillCronInfo(appResourceScope.getAppId(), taskPlan);
        TaskPlanVO taskPlanVO = TaskPlanInfoDTO.toVO(taskPlan);
        taskPlanVO.setCanView(true);
        taskPlanVO.setCanEdit(planAuthService.authEditJobPlan(username, appResourceScope, templateId,
            planId, taskPlan.getName())
            .isPass());
        taskPlanVO.setCanDelete(planAuthService.authDeleteJobPlan(username, appResourceScope, templateId,
            planId, taskPlan.getName())
            .isPass());

        fillTaskPlanHostIdIfMissing(taskPlanVO);

        return Response.buildSuccessResp(taskPlanVO);
    }

    /**
     * 填充主机Id
     * tmp: 发布兼容代码。由于前段无法兼容没有hostId的主机信息，所以这里需要通过云区域+ip获取到hostId并设置。发布完成后可以删除
     *
     * @param taskPlan 执行方案
     */
    private void fillTaskPlanHostIdIfMissing(TaskPlanVO taskPlan) {
        boolean isMissingHostId = false;
        for (TaskStepVO step : taskPlan.getStepList()) {
            if (step.getScriptStepInfo() != null) {
                isMissingHostId = isMissingHostId || fillHostId(step.getScriptStepInfo().getExecuteTarget());
            } else if (step.getFileStepInfo() != null) {
                TaskFileStepVO fileStep = step.getFileStepInfo();
                fillHostId(fileStep.getFileDestination().getServer());
                if (CollectionUtils.isNotEmpty(fileStep.getFileSourceList())) {
                    for (TaskFileSourceInfoVO source : fileStep.getFileSourceList()) {
                        if (source.getHost() != null) {
                            isMissingHostId = isMissingHostId || fillHostId(source.getHost());
                        }
                    }
                }
            }
        }
        if (CollectionUtils.isNotEmpty(taskPlan.getVariableList())) {
            for (TaskVariableVO var : taskPlan.getVariableList()) {
                if (var.getDefaultTargetValue() != null) {
                    isMissingHostId = isMissingHostId || fillHostId(var.getDefaultTargetValue());
                }
            }
        }
        if (isMissingHostId) {
            log.warn("Task plan missing hostId, planId: {}", taskPlan.getId());
        }
    }

    private boolean fillHostId(TaskTargetVO executeTarget) {
        boolean isMissingHostId = false;
        if (executeTarget != null && executeTarget.getHostNodeInfo() != null &&
            CollectionUtils.isNotEmpty(executeTarget.getHostNodeInfo().getHostList())) {
            for (HostInfoVO host : executeTarget.getHostNodeInfo().getHostList()) {
                if (host.getHostId() == null) {
                    isMissingHostId = true;
                    ApplicationHostDTO hostInfo =
                        hostService.getHostByIp(host.getCloudArea().getId() + ":" + host.getIp());
                    if (hostInfo != null) {
                        host.setHostId(hostInfo.getHostId());
                    }
                }
            }
        }
        return isMissingHostId;
    }

    @Override
    public Response<TaskPlanVO> getDebugPlan(String username,
                                             AppResourceScope appResourceScope,
                                             String scopeType,
                                             String scopeId,
                                             Long templateId) {
        AuthResult authResult = templateAuthService.authViewJobTemplate(username, appResourceScope, templateId);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        TaskTemplateInfoDTO taskTemplateBasicInfo =
            templateService.getTaskTemplateBasicInfoById(appResourceScope.getAppId(), templateId);
        if (taskTemplateBasicInfo == null) {
            throw new NotFoundException(ErrorCode.TASK_PLAN_NOT_EXIST);
        }
        TaskPlanInfoDTO taskPlan = planService.getDebugTaskPlan(username, appResourceScope.getAppId(), templateId);
        TaskPlanVO taskPlanVO = null;
        if (taskPlan != null) {
            StepRefVariableParser.parseStepRefVars(taskPlan.getStepList(), taskPlan.getVariableList());
            taskPlanVO = TaskPlanInfoDTO.toVO(taskPlan);
            taskPlanVO.setCanView(true);
            taskPlanVO.setCanEdit(true);
            taskPlanVO.setCanDelete(false);
        }
        return Response.buildSuccessResp(taskPlanVO);
    }

    @Override
    public Response<Long> savePlan(String username,
                                   AppResourceScope appResourceScope,
                                   String scopeType,
                                   String scopeId,
                                   Long templateId,
                                   Long planId,
                                   TaskPlanCreateUpdateReq taskPlanCreateUpdateReq) {
        taskPlanCreateUpdateReq.setTemplateId(templateId);
        AuthResult authResult;
        if (planId > 0) {
            if (planService.isDebugPlan(appResourceScope.getAppId(), templateId, planId)) {
                authResult = planAuthService.authEditJobPlan(username, appResourceScope, templateId,
                    planId, null);
            } else {
                authResult = planAuthService.authEditJobPlan(username, appResourceScope, templateId,
                    planId, null);
            }
            taskPlanCreateUpdateReq.setId(planId);
        } else {
            authResult = planAuthService.authCreateJobPlan(username, appResourceScope, templateId, null);
        }
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        // 检查执行方案名称
        try {
            StringCheckHelper stringCheckHelper = new StringCheckHelper(new TrimChecker(), new NotEmptyChecker(),
                new IlegalCharChecker(), new MaxLengthChecker(60));
            taskPlanCreateUpdateReq.setName(stringCheckHelper.checkAndGetResult(taskPlanCreateUpdateReq.getName()));
        } catch (StringCheckException e) {
            log.warn("TaskPlan name is invalid:", e);
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        Long savedPlanId = planService.saveTaskPlan(TaskPlanInfoDTO.fromReq(username, appResourceScope.getAppId(),
            taskPlanCreateUpdateReq));
        if (planId == 0) {
            planAuthService.registerPlan(savedPlanId, taskPlanCreateUpdateReq.getName(), username);
        }
        return Response.buildSuccessResp(savedPlanId);
    }

    @Override
    public Response<Boolean> deletePlan(String username,
                                        AppResourceScope appResourceScope,
                                        String scopeType,
                                        String scopeId,
                                        Long templateId,
                                        Long planId) {
        AuthResult authResult = planAuthService.authDeleteJobPlan(username, appResourceScope, templateId,
            planId, null);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        return Response.buildSuccessResp(planService.deleteTaskPlan(appResourceScope.getAppId(), templateId, planId));
    }

    @Override
    public Response<List<TaskPlanVO>> listPlanBasicInfoByIds(String username,
                                                             AppResourceScope appResourceScope,
                                                             String scopeType,
                                                             String scopeId,
                                                             String planIds) {

        AuthResult authResult = businessAuthService.authAccessBusiness(username, appResourceScope);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        if (StringUtils.isNotEmpty(planIds)) {
            List<Long> planIdList = Arrays.stream(planIds.split(",")).filter(Objects::nonNull).map(Long::valueOf)
                .filter(id -> id > 0).collect(Collectors.toList());
            List<TaskPlanInfoDTO> taskPlanInfoList = planService.listPlanBasicInfoByIds(appResourceScope.getAppId(),
                planIdList);
            fillCronInfo(appResourceScope.getAppId(), taskPlanInfoList);
            List<TaskPlanVO> taskPlanList =
                taskPlanInfoList.stream().map(TaskPlanInfoDTO::toVO).collect(Collectors.toList());
            processPlanPermission(username, appResourceScope, taskPlanList);
            return Response.buildSuccessResp(taskPlanList);
        } else {
            return Response.buildSuccessResp(new ArrayList<>());
        }
    }

    @Override
    public Response<Boolean> checkPlanName(String username,
                                           AppResourceScope appResourceScope,
                                           String scopeType,
                                           String scopeId,
                                           Long templateId,
                                           Long planId,
                                           String name) {
        AuthResult authResult = templateAuthService.authViewJobTemplate(username, appResourceScope,
            templateId);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        return Response.buildSuccessResp(planService.checkPlanName(appResourceScope.getAppId(), templateId, planId,
            name));
    }

    @Override
    public Response<TaskPlanSyncInfoVO> syncInfo(String username,
                                                 AppResourceScope appResourceScope,
                                                 String scopeType,
                                                 String scopeId,
                                                 Long templateId,
                                                 Long planId) {
        AuthResult authResult = planAuthService.authSyncJobPlan(username, appResourceScope, templateId, planId, null);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        TaskPlanInfoDTO taskPlan = planService.getTaskPlanById(appResourceScope.getAppId(), templateId, planId);
        if (taskPlan != null) {
            TaskTemplateInfoDTO taskTemplate = templateService.getTaskTemplateById(appResourceScope.getAppId(),
                templateId);
            if (taskTemplate == null) {
                throw new InternalException(ErrorCode.INTERNAL_ERROR);
            }
            TaskPlanSyncInfoVO taskPlanSyncInfoVO = new TaskPlanSyncInfoVO();
            taskPlanSyncInfoVO.setPlanInfo(TaskPlanInfoDTO.toVO(taskPlan));
            taskPlanSyncInfoVO.setTemplateInfo(TaskTemplateInfoDTO.toVO(taskTemplate));
            taskPlanSyncInfoVO.getTemplateInfo().setVersion(taskTemplate.getVersion());
            return Response.buildSuccessResp(taskPlanSyncInfoVO);
        } else {
            log.debug("Cannot find plan {} for template {}", planId, templateId);
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
    }

    @Override
    public Response<Boolean> syncConfirm(String username,
                                         AppResourceScope appResourceScope,
                                         String scopeType,
                                         String scopeId,
                                         Long templateId,
                                         Long planId,
                                         String templateVersion) {
        AuthResult authResult = planAuthService.authSyncJobPlan(username, appResourceScope, templateId,
            planId, null);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        return Response.buildSuccessResp(planService.sync(appResourceScope.getAppId(), templateId, planId,
            templateVersion));
    }

    @Override
    public Response<Boolean> addFavorite(String username,
                                         AppResourceScope appResourceScope,
                                         String scopeType,
                                         String scopeId,
                                         Long templateId,
                                         Long planId) {
        AuthResult authResult = businessAuthService.authAccessBusiness(username, appResourceScope);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        return Response.buildSuccessResp(taskFavoriteService.addFavorite(appResourceScope.getAppId(), username,
            planId));
    }

    @Override
    public Response<Boolean> removeFavorite(String username,
                                            AppResourceScope appResourceScope,
                                            String scopeType,
                                            String scopeId,
                                            Long templateId,
                                            Long planId) {
        AuthResult authResult = businessAuthService.authAccessBusiness(username, appResourceScope);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        return Response.buildSuccessResp(taskFavoriteService.deleteFavorite(appResourceScope.getAppId(), username,
            planId));
    }

    @Override
    public Response<TaskPlanVO> getPlanBasicInfoById(String username, Long planId) {
        if (planId == null || planId <= 0) {
            return Response.buildSuccessResp(null);
        }
        TaskPlanInfoDTO taskPlanInfo = planService.getTaskPlanById(planId);
        if (taskPlanInfo != null) {
            AuthResult authResult = businessAuthService.authAccessBusiness(
                username, new AppResourceScope(taskPlanInfo.getAppId()));
            if (!authResult.isPass()) {
                throw new PermissionDeniedException(authResult);
            }
            taskPlanInfo.setStepList(null);
            taskPlanInfo.setVariableList(null);
            return Response.buildSuccessResp(TaskPlanInfoDTO.toVO(taskPlanInfo));
        }
        return Response.buildSuccessResp(null);
    }

    @Override
    public Response<Boolean> batchUpdatePlanVariableValueByName(String username,
                                                                AppResourceScope appResourceScope,
                                                                String scopeType,
                                                                String scopeId,
                                                                List<TaskVariableValueUpdateReq> planVariableInfoList) {
        if (CollectionUtils.isEmpty(planVariableInfoList)) {
            return Response.buildSuccessResp(true);
        }
        List<Long> templateIdList = new ArrayList<>();
        List<Long> planIdList = new ArrayList<>();
        Set<Long> planIdSet = new HashSet<>();
        for (TaskVariableValueUpdateReq taskVariableValueUpdateReq : planVariableInfoList) {
            if (planIdSet.contains(taskVariableValueUpdateReq.getPlanId())) {
                continue;
            }
            planIdSet.add(taskVariableValueUpdateReq.getPlanId());
            templateIdList.add(taskVariableValueUpdateReq.getTemplateId());
            planIdList.add(taskVariableValueUpdateReq.getPlanId());
        }

        List<Long> canEditPlanIdList = planAuthService.batchAuthEditJobPlan(username, appResourceScope,
            templateIdList, planIdList);
        if (CollectionUtils.isEmpty(canEditPlanIdList) || (canEditPlanIdList.size() != planIdSet.size())) {
            log.warn("Batch update variable failed! Auth plan perm failed!|{}|{}", planIdSet, canEditPlanIdList);
            return Response.buildSuccessResp(false);
        }

        Long modifyTime = DateUtils.currentTimeSeconds();

        List<TaskPlanInfoDTO> planInfoList = planVariableInfoList.parallelStream().map(planVariableInfo -> {
            TaskPlanInfoDTO planInfo = new TaskPlanInfoDTO();
            planInfo.setAppId(appResourceScope.getAppId());
            planInfo.setLastModifyUser(username);
            planInfo.setLastModifyTime(modifyTime);
            planInfo.setId(planVariableInfo.getPlanId());
            planInfo.setTemplateId(planVariableInfo.getTemplateId());
            if (CollectionUtils.isNotEmpty(planVariableInfo.getVariableInfoList())) {
                planInfo.setVariableList(planVariableInfo.getVariableInfoList().parallelStream()
                    .map(variableVO -> {
                        variableVO.setRequired(0);
                        variableVO.setChangeable(0);
                        TaskVariableDTO variableDTO = TaskVariableDTO.fromVO(variableVO);
                        variableDTO.setPlanId(planInfo.getId());
                        return variableDTO;
                    }).collect(Collectors.toList()));
            } else {
                planInfo.setVariableList(Collections.emptyList());
            }
            return planInfo;
        }).collect(Collectors.toList());

        return Response.buildSuccessResp(planService.batchUpdatePlanVariable(planInfoList));
    }

    private void fillCronInfo(Long appId, List<TaskPlanInfoDTO> planInfoList) {
        try {
            List<Long> planIds = planInfoList.parallelStream()
                .map(TaskPlanInfoDTO::getId).collect(Collectors.toList());
            Map<Long, List<CronJobVO>> cronJobByPlanIds = cronJobService.batchListCronJobByPlanIds(appId, planIds);
            if (MapUtils.isNotEmpty(cronJobByPlanIds)) {
                planInfoList.forEach(planInfo -> {
                    if (cronJobByPlanIds.containsKey(planInfo.getId())) {
                        planInfo.setHasCronJob(true);
                        planInfo.setCronJobCount((long) cronJobByPlanIds.get(planInfo.getId()).size());
                    } else {
                        planInfo.setHasCronJob(false);
                        planInfo.setCronJobCount(0L);
                    }
                });
            } else {
                planInfoList.forEach(planInfo -> {
                    planInfo.setHasCronJob(false);
                    planInfo.setCronJobCount(0L);
                });
            }
        } catch (Exception e) {
            log.error("Error while process plan's cronjob", e);
        }
    }

    private void fillCronInfo(Long appId, TaskPlanInfoDTO planInfo) {
        try {
            List<Long> planIds = Collections.singletonList(planInfo.getId());
            Map<Long, List<CronJobVO>> cronJobByPlanIds = cronJobService.batchListCronJobByPlanIds(appId, planIds);
            List<CronJobVO> cronJobs = cronJobByPlanIds != null ? cronJobByPlanIds.get(planInfo.getId()) : null;
            if (CollectionUtils.isNotEmpty(cronJobs)) {
                planInfo.setHasCronJob(true);
                planInfo.setCronJobCount((long) cronJobs.size());
            } else {
                planInfo.setHasCronJob(false);
                planInfo.setCronJobCount(0L);
            }
        } catch (Throwable e) {
            log.error("Error while process plan's cronjob", e);
        }
    }

    private PageData<TaskPlanVO> makeEmptyResponse(Integer start, Integer pageSize) {
        PageData<TaskPlanVO> taskPlanPageData = new PageData<>();
        taskPlanPageData.setStart(start != null ? start : 0);
        taskPlanPageData.setPageSize(pageSize != null ? pageSize : 10);
        taskPlanPageData.setTotal(0L);
        taskPlanPageData.setData(Collections.emptyList());
        return taskPlanPageData;
    }

}
