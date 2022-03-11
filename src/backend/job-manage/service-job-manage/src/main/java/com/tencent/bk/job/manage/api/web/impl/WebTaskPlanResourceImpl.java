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
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.model.permission.AuthResultVO;
import com.tencent.bk.job.common.util.check.*;
import com.tencent.bk.job.common.util.check.exception.StringCheckException;
import com.tencent.bk.job.crontab.model.CronJobVO;
import com.tencent.bk.job.manage.api.web.WebTaskPlanResource;
import com.tencent.bk.job.manage.common.util.IamPathUtil;
import com.tencent.bk.job.manage.model.dto.TaskPlanQueryDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import com.tencent.bk.job.manage.model.web.request.TaskPlanCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.request.TaskVariableValueUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.task.TaskPlanSyncInfoVO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskPlanVO;
import com.tencent.bk.job.manage.service.CronJobService;
import com.tencent.bk.job.manage.service.TaskFavoriteService;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import com.tencent.bk.sdk.iam.util.PathBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
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
    private final WebAuthService authService;

    private final MessageI18nService i18nService;

    @Autowired
    public WebTaskPlanResourceImpl(
        TaskPlanService planService,
        TaskTemplateService templateService,
        @Qualifier("TaskPlanFavoriteServiceImpl") TaskFavoriteService taskFavoriteService,
        CronJobService cronJobService,
        WebAuthService webAuthService,
        MessageI18nService i18nService
    ) {
        this.planService = planService;
        this.templateService = templateService;
        this.taskFavoriteService = taskFavoriteService;
        this.cronJobService = cronJobService;
        this.authService = webAuthService;
        this.i18nService = i18nService;
    }

    @Override
    public ServiceResponse<PageData<TaskPlanVO>> listAllPlans(
        String username,
        Long appId,
        Long planId,
        String templateName,
        Long templateId,
        String planName,
        String creator,
        String lastModifyUser,
        Integer start,
        Integer pageSize
    ) {
        AuthResultVO authResultVO = authService.auth(true, username, ActionId.LIST_BUSINESS,
            ResourceTypeEnum.BUSINESS, appId.toString(), null);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }

        List<Long> favoriteList = taskFavoriteService.listFavorites(appId, username);
        TaskPlanQueryDTO taskPlanQueryDTO = new TaskPlanQueryDTO();
        taskPlanQueryDTO.setAppId(appId);
        if (planId != null && planId > 0) {
            taskPlanQueryDTO.setPlanId(planId);
            templateName = null;
            planName = null;
            creator = null;
            lastModifyUser = null;
        }

        if (templateId != null && templateId > 0) {
            TaskTemplateInfoDTO templateInfo = templateService.getTaskTemplateBasicInfoById(appId, templateId);
            if (templateInfo != null) {
                taskPlanQueryDTO.setTemplateId(templateId);
            } else {
                return ServiceResponse.buildSuccessResp(makeEmptyResponse(start, pageSize));
            }
        } else {
            if (StringUtils.isNotBlank(templateName)) {
                TaskTemplateInfoDTO templateQuery = new TaskTemplateInfoDTO();
                templateQuery.setAppId(appId);
                templateQuery.setName(templateName);

                BaseSearchCondition templateSearchCondition = new BaseSearchCondition();
                templateSearchCondition.setStart(-1);
                templateSearchCondition.setLength(-1);

                PageData<TaskTemplateInfoDTO> taskTemplateInfoPageData =
                    templateService.listPageTaskTemplatesBasicInfo(
                        templateQuery,
                        templateSearchCondition,
                        null
                    );
                if (taskTemplateInfoPageData != null
                    && CollectionUtils.isNotEmpty(taskTemplateInfoPageData.getData())) {
                    List<Long> templateIdList = new ArrayList<>();
                    taskTemplateInfoPageData.getData().forEach(taskTemplateInfo ->
                        templateIdList.add(taskTemplateInfo.getId()));
                    taskPlanQueryDTO.setTemplateIdList(templateIdList);
                } else {
                    return ServiceResponse.buildSuccessResp(makeEmptyResponse(start, pageSize));
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
            fillCronInfo(appId, taskPlanInfoPageData.getData());
            taskPlanInfoPageData.getData().forEach(taskPlanInfo -> resultPlans.add(TaskPlanInfoDTO.toVO(taskPlanInfo)));
        } else {
            return ServiceResponse.buildCommonFailResp("No plan info found!");
        }

        resultPlans.forEach(taskPlan -> taskPlan.setFavored(favoriteList.contains(taskPlan.getId()) ? 1 : 0));
        processPlanPermission(username, appId, resultPlans);

        PageData<TaskPlanVO> taskPlanPageData = new PageData<>();
        taskPlanPageData.setStart(taskPlanInfoPageData.getStart());
        taskPlanPageData.setPageSize(taskPlanInfoPageData.getPageSize());
        taskPlanPageData.setTotal(taskPlanInfoPageData.getTotal());
        taskPlanPageData.setData(resultPlans);
        taskPlanPageData.setExistAny(planService.isExistAnyAppPlan(appId));
        return ServiceResponse.buildSuccessResp(taskPlanPageData);
    }

    @Override
    public ServiceResponse<List<TaskPlanVO>> listPlans(String username, Long appId, Long templateId) {
        AuthResultVO authResultVO = authService.auth(true, username, ActionId.LIST_BUSINESS,
            ResourceTypeEnum.BUSINESS, appId.toString(), null);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }

        List<TaskPlanVO> taskPlanList = listPlansByTemplateId(username, appId, templateId);
        return ServiceResponse.buildSuccessResp(taskPlanList);
    }

    private List<TaskPlanVO> listPlansByTemplateId(String username, Long appId, Long templateId) {
        TaskTemplateInfoDTO taskTemplateBasicInfo = templateService.getTaskTemplateBasicInfoById(appId, templateId);
        if (taskTemplateBasicInfo == null) {
            throw new ServiceException(ErrorCode.TEMPLATE_NOT_EXIST);
        }

        List<TaskPlanInfoDTO> taskPlanInfoList = planService.listTaskPlansBasicInfo(appId, templateId);
        fillCronInfo(appId, taskPlanInfoList);
        final String templateVersion = taskTemplateBasicInfo.getVersion();
        List<TaskPlanVO> taskPlanList = taskPlanInfoList.stream().map(taskPlanInfo -> {
            taskPlanInfo.setTemplateVersion(templateVersion);
            taskPlanInfo.setNeedUpdate(!templateVersion.equals(taskPlanInfo.getVersion()));
            return TaskPlanInfoDTO.toVO(taskPlanInfo);
        }).collect(Collectors.toList());
        processPlanPermission(username, appId, taskPlanList);
        return taskPlanList;
    }

    private void processPlanPermission(String username, Long appId, List<TaskPlanVO> taskPlanList) {
        List<PermissionResource> planIdResource = new ArrayList<>();
        taskPlanList.forEach(plan -> {
            PermissionResource resource = new PermissionResource();
            resource.setResourceId(plan.getId().toString());
            resource.setResourceType(ResourceTypeEnum.PLAN);
            resource.setPathInfo(
                PathBuilder.newBuilder(
                    ResourceTypeEnum.BUSINESS.getId(),
                    appId.toString()
                ).child(ResourceTypeEnum.TEMPLATE.getId(),
                    plan.getTemplateId().toString()).build()
            );
            planIdResource.add(resource);
        });

        List<Long> allowedViewPlan =
            authService.batchAuth(username, ActionId.VIEW_JOB_PLAN, appId, planIdResource)
                .parallelStream().map(Long::valueOf).collect(Collectors.toList());
        List<Long> allowedEditPlan =
            authService.batchAuth(username, ActionId.EDIT_JOB_PLAN, appId, planIdResource)
                .parallelStream().map(Long::valueOf).collect(Collectors.toList());
        List<Long> allowedDeletePlan =
            authService.batchAuth(username, ActionId.DELETE_JOB_PLAN, appId, planIdResource)
                .parallelStream().map(Long::valueOf).collect(Collectors.toList());

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
    public ServiceResponse<List<TaskPlanVO>> batchGetPlans(String username, Long appId,
                                                           String templateIds) {
        if (StringUtils.isEmpty(templateIds)) {
            log.warn("TemplateIds is empty!");
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM);
        }

        String[] templateIdArray = templateIds.split(",");
        List<Long> templateIdList = new ArrayList<>();
        for (String templateIdStr : templateIdArray) {
            templateIdList.add(Long.parseLong(templateIdStr));
        }
        if (CollectionUtils.isEmpty(templateIdList)) {
            log.warn("TemplateIdList is empty!");
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM);
        }

        AuthResultVO authResultVO = authService.auth(true, username, ActionId.LIST_BUSINESS,
            ResourceTypeEnum.BUSINESS, appId.toString(), null);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }

        List<TaskPlanVO> planList = new ArrayList<>();
        for (Long templateId : templateIdList) {
            List<TaskPlanVO> templatePlanList = listPlansByTemplateId(username, appId, templateId);
            if (CollectionUtils.isNotEmpty(templatePlanList)) {
                planList.addAll(templatePlanList);
            }
        }

        return ServiceResponse.buildSuccessResp(planList);
    }

    @Override
    public ServiceResponse<TaskPlanVO> getPlanById(String username, Long appId, Long templateId, Long planId) {
        AuthResultVO authResultVO = authService.auth(true, username, ActionId.VIEW_JOB_PLAN,
            ResourceTypeEnum.PLAN, planId.toString(), IamPathUtil.buildPlanPathInfo(appId, templateId));
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        TaskTemplateInfoDTO taskTemplateBasicInfo = templateService.getTaskTemplateBasicInfoById(appId, templateId);
        if (taskTemplateBasicInfo == null) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.TASK_PLAN_NOT_EXIST, i18nService);
        }
        TaskPlanInfoDTO taskPlan = planService.getTaskPlanById(appId, templateId, planId);
        if (taskPlan != null) {
            final String templateVersion = taskTemplateBasicInfo.getVersion();
            if (StringUtils.isNotEmpty(templateVersion)) {
                taskPlan.setTemplateVersion(templateVersion);
                taskPlan.setNeedUpdate(!templateVersion.equals(taskPlan.getVersion()));
            }
            fillCronInfo(appId, taskPlan);
            TaskPlanVO taskPlanVO = TaskPlanInfoDTO.toVO(taskPlan);
            taskPlanVO.setCanView(true);
            taskPlanVO.setCanEdit(authService
                .auth(false, username, ActionId.EDIT_JOB_PLAN, ResourceTypeEnum.PLAN, planId.toString(),
                    IamPathUtil.buildPlanPathInfo(appId, templateId))
                .isPass());
            taskPlanVO.setCanDelete(authService
                .auth(false, username, ActionId.DELETE_JOB_PLAN, ResourceTypeEnum.PLAN, planId.toString(),
                    IamPathUtil.buildPlanPathInfo(appId, templateId))
                .isPass());
            return ServiceResponse.buildSuccessResp(taskPlanVO);
        } else {
            return ServiceResponse.buildCommonFailResp(ErrorCode.TASK_PLAN_NOT_EXIST, i18nService);
        }
    }

    @Override
    public ServiceResponse<TaskPlanVO> getDebugPlan(String username, Long appId, Long templateId) {
        AuthResultVO authResultVO = authService.auth(true, username, ActionId.VIEW_JOB_TEMPLATE,
            ResourceTypeEnum.TEMPLATE, templateId.toString(), IamPathUtil.buildPlanPathInfo(appId, templateId));
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        TaskTemplateInfoDTO taskTemplateBasicInfo = templateService.getTaskTemplateBasicInfoById(appId, templateId);
        if (taskTemplateBasicInfo == null) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.TASK_PLAN_NOT_EXIST, i18nService);
        }
        TaskPlanInfoDTO taskPlan = planService.getDebugTaskPlan(username, appId, templateId);
        TaskPlanVO taskPlanVO = TaskPlanInfoDTO.toVO(taskPlan);
        taskPlanVO.setCanView(true);
        taskPlanVO.setCanEdit(true);
        taskPlanVO.setCanDelete(false);
        return ServiceResponse.buildSuccessResp(taskPlanVO);
    }

    @Override
    public ServiceResponse<Long> savePlan(String username, Long appId, Long templateId, Long planId,
                                          TaskPlanCreateUpdateReq taskPlanCreateUpdateReq) {
        taskPlanCreateUpdateReq.setTemplateId(templateId);
        AuthResultVO authResultVO;
        if (planId > 0) {
            if (planService.isDebugPlan(appId, templateId, planId)) {
                authResultVO = authService.auth(true, username, ActionId.EDIT_JOB_TEMPLATE,
                    ResourceTypeEnum.TEMPLATE, templateId.toString(), IamPathUtil.buildPlanPathInfo(appId, templateId));
            } else {
                authResultVO = authService.auth(true, username, ActionId.EDIT_JOB_PLAN,
                    ResourceTypeEnum.PLAN, planId.toString(), IamPathUtil.buildPlanPathInfo(appId, templateId));
            }
            taskPlanCreateUpdateReq.setId(planId);
        } else {
            authResultVO = authService.auth(true, username, ActionId.CREATE_JOB_PLAN,
                ResourceTypeEnum.TEMPLATE, templateId.toString(), IamPathUtil.buildPlanPathInfo(appId, templateId));
        }
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        // 检查执行方案名称
        try {
            StringCheckHelper stringCheckHelper = new StringCheckHelper(new TrimChecker(), new NotEmptyChecker(),
                new IlegalCharChecker(), new MaxLengthChecker(60));
            taskPlanCreateUpdateReq.setName(stringCheckHelper.checkAndGetResult(taskPlanCreateUpdateReq.getName()));
        } catch (StringCheckException e) {
            log.warn("TaskPlan name is invalid:", e);
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM, i18nService);
        }
        Long savedPlanId = planService.saveTaskPlan(TaskPlanInfoDTO.fromReq(username, appId, taskPlanCreateUpdateReq));
        if (planId == 0) {
            authService.registerResource(savedPlanId.toString(), taskPlanCreateUpdateReq.getName(), ResourceId.PLAN,
                username, null);
        }
        return ServiceResponse.buildSuccessResp(savedPlanId);
    }

    @Override
    public ServiceResponse<Boolean> deletePlan(String username, Long appId, Long templateId, Long planId) {
        AuthResultVO authResultVO = authService.auth(true, username, ActionId.DELETE_JOB_PLAN,
            ResourceTypeEnum.PLAN, planId.toString(), IamPathUtil.buildPlanPathInfo(appId, templateId));
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        return ServiceResponse.buildSuccessResp(planService.deleteTaskPlan(appId, templateId, planId));
    }

    @Override
    public ServiceResponse<List<TaskPlanVO>> listPlanBasicInfoByIds(String username, Long appId, String planIds) {
        AuthResultVO authResultVO = authService.auth(true, username, ActionId.LIST_BUSINESS,
            ResourceTypeEnum.BUSINESS, appId.toString(), null);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        if (StringUtils.isNotEmpty(planIds)) {
            List<Long> planIdList;
            try {
                planIdList = Arrays.stream(planIds.split(",")).filter(Objects::nonNull).map(Long::valueOf)
                    .filter(id -> id > 0).collect(Collectors.toList());
            } catch (Exception e) {
                return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM, i18nService);
            }
            List<TaskPlanInfoDTO> taskPlanInfoList = planService.listPlanBasicInfoByIds(appId, planIdList);
            fillCronInfo(appId, taskPlanInfoList);
            List<TaskPlanVO> taskPlanList =
                taskPlanInfoList.stream().map(TaskPlanInfoDTO::toVO).collect(Collectors.toList());
            processPlanPermission(username, appId, taskPlanList);
            return ServiceResponse.buildSuccessResp(taskPlanList);
        } else {
            return ServiceResponse.buildSuccessResp(new ArrayList<>());
        }
    }

    @Override
    public ServiceResponse<Boolean> checkPlanName(String username, Long appId, Long templateId, Long planId,
                                                  String name) {
        AuthResultVO authResultVO = authService.auth(true, username, ActionId.VIEW_JOB_TEMPLATE,
            ResourceTypeEnum.TEMPLATE, templateId.toString(), IamPathUtil.buildPlanPathInfo(appId, templateId));
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        return ServiceResponse.buildSuccessResp(planService.checkPlanName(appId, templateId, planId, name));
    }

    @Override
    public ServiceResponse<TaskPlanSyncInfoVO> syncInfo(String username, Long appId, Long templateId, Long planId) {
        AuthResultVO authResultVO = authService.auth(true, username, ActionId.SYNC_JOB_PLAN,
            ResourceTypeEnum.PLAN, planId.toString(), IamPathUtil.buildPlanPathInfo(appId, templateId));
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        TaskPlanInfoDTO taskPlan = planService.getTaskPlanById(appId, templateId, planId);
        if (taskPlan != null) {
            TaskTemplateInfoDTO taskTemplate = templateService.getTaskTemplateById(appId, templateId);
            if (taskTemplate == null) {
                return ServiceResponse.buildCommonFailResp(ErrorCode.DB_ERROR, i18nService);
            }
            TaskPlanSyncInfoVO taskPlanSyncInfoVO = new TaskPlanSyncInfoVO();
            taskPlanSyncInfoVO.setPlanInfo(TaskPlanInfoDTO.toVO(taskPlan));
            taskPlanSyncInfoVO.setTemplateInfo(TaskTemplateInfoDTO.toVO(taskTemplate));
            taskPlanSyncInfoVO.getTemplateInfo().setVersion(taskTemplate.getVersion());
            return ServiceResponse.buildSuccessResp(taskPlanSyncInfoVO);
        } else {
            log.debug("Cannot find plan {} for template {}", planId, templateId);
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM, i18nService);
        }
    }

    @Override
    public ServiceResponse<Boolean> syncConfirm(String username, Long appId, Long templateId, Long planId,
                                                String templateVersion) {
        AuthResultVO authResultVO = authService.auth(true, username, ActionId.SYNC_JOB_PLAN,
            ResourceTypeEnum.PLAN, planId.toString(), IamPathUtil.buildPlanPathInfo(appId, templateId));
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        return ServiceResponse.buildSuccessResp(planService.sync(appId, templateId, planId, templateVersion));
    }

    @Override
    public ServiceResponse<Boolean> addFavorite(String username, Long appId, Long templateId, Long planId) {
        AuthResultVO authResultVO = authService.auth(true, username, ActionId.LIST_BUSINESS,
            ResourceTypeEnum.BUSINESS, appId.toString(), null);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        return ServiceResponse.buildSuccessResp(taskFavoriteService.addFavorite(appId, username, planId));
    }

    @Override
    public ServiceResponse<Boolean> removeFavorite(String username, Long appId, Long templateId, Long planId) {
        AuthResultVO authResultVO = authService.auth(true, username, ActionId.LIST_BUSINESS,
            ResourceTypeEnum.BUSINESS, appId.toString(), null);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        return ServiceResponse.buildSuccessResp(taskFavoriteService.deleteFavorite(appId, username, planId));
    }

    @Override
    public ServiceResponse<TaskPlanVO> getPlanBasicInfoById(String username, Long planId) {
        if (planId == null || planId <= 0) {
            return ServiceResponse.buildSuccessResp(null);
        }
        TaskPlanInfoDTO taskPlanInfo = planService.getTaskPlanById(planId);
        if (taskPlanInfo != null) {
            AuthResultVO authResultVO = authService.auth(true, username, ActionId.LIST_BUSINESS,
                ResourceTypeEnum.BUSINESS, taskPlanInfo.getAppId().toString(), null);
            if (!authResultVO.isPass()) {
                return ServiceResponse.buildSuccessResp(null);
            }
            taskPlanInfo.setStepList(null);
            taskPlanInfo.setVariableList(null);
            return ServiceResponse.buildSuccessResp(TaskPlanInfoDTO.toVO(taskPlanInfo));
        }
        return ServiceResponse.buildSuccessResp(null);
    }

    @Override
    public ServiceResponse<Boolean> batchUpdatePlanVariableValueByName(
        String username,
        Long appId,
        List<TaskVariableValueUpdateReq> planVariableInfoList
    ) {
        if (CollectionUtils.isEmpty(planVariableInfoList)) {
            return ServiceResponse.buildSuccessResp(true);
        }
        List<PermissionResource> permissionResources = new ArrayList<>();
        Set<Long> planIdSet = new HashSet<>();
        for (TaskVariableValueUpdateReq taskVariableValueUpdateReq : planVariableInfoList) {
            if (planIdSet.contains(taskVariableValueUpdateReq.getPlanId())) {
                continue;
            }
            planIdSet.add(taskVariableValueUpdateReq.getPlanId());

            PermissionResource resource = new PermissionResource();
            resource.setResourceId(taskVariableValueUpdateReq.getPlanId().toString());
            resource.setResourceType(ResourceTypeEnum.PLAN);
            resource.setPathInfo(
                PathBuilder.newBuilder(
                    ResourceTypeEnum.BUSINESS.getId(),
                    appId.toString()
                ).child(ResourceTypeEnum.TEMPLATE.getId(),
                    taskVariableValueUpdateReq.getTemplateId().toString()
                ).build()
            );
            permissionResources.add(resource);
        }

        List<String> canEditPlanIdList = authService.batchAuth(username, ActionId.EDIT_JOB_PLAN, appId,
            permissionResources);
        if (CollectionUtils.isEmpty(canEditPlanIdList) || (canEditPlanIdList.size() != planIdSet.size())) {
            log.warn("Batch update variable failed! Auth plan perm failed!|{}|{}", planIdSet, canEditPlanIdList);
            return ServiceResponse.buildSuccessResp(false);
        }

        Long modifyTime = System.currentTimeMillis();

        List<TaskPlanInfoDTO> planInfoList = planVariableInfoList.parallelStream().map(planVariableInfo -> {
            TaskPlanInfoDTO planInfo = new TaskPlanInfoDTO();
            planInfo.setAppId(appId);
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

        return ServiceResponse.buildSuccessResp(planService.batchUpdatePlanVariable(planInfoList));
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
            if (MapUtils.isNotEmpty(cronJobByPlanIds)) {
                planInfo.setHasCronJob(cronJobByPlanIds.containsKey(planInfo.getId()));
            } else {
                planInfo.setHasCronJob(false);
            }
        } catch (Exception e) {
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
