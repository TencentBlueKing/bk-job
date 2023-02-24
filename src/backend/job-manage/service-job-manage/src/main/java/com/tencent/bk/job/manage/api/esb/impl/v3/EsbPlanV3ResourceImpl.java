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

package com.tencent.bk.job.manage.api.esb.impl.v3;

import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.job.v3.EsbPageDataV3;
import com.tencent.bk.job.common.esb.util.EsbDTOAppScopeMappingHelper;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.BusinessAuthService;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.manage.api.esb.v3.EsbPlanV3Resource;
import com.tencent.bk.job.manage.auth.PlanAuthService;
import com.tencent.bk.job.manage.manager.variable.StepRefVariableParser;
import com.tencent.bk.job.manage.model.dto.TaskPlanQueryDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetPlanDetailV3Request;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetPlanListV3Request;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbPlanBasicInfoV3DTO;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbPlanInfoV3DTO;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

/**
 * @since 15/10/2020 18:08
 */
@Slf4j
@RestController
public class EsbPlanV3ResourceImpl implements EsbPlanV3Resource {

    private final TaskPlanService taskPlanService;
    private final BusinessAuthService businessAuthService;
    private final PlanAuthService planAuthService;
    private final AppScopeMappingService appScopeMappingService;

    @Autowired
    public EsbPlanV3ResourceImpl(TaskPlanService taskPlanService,
                                 BusinessAuthService businessAuthService,
                                 PlanAuthService planAuthService,
                                 AppScopeMappingService appScopeMappingService) {
        this.taskPlanService = taskPlanService;
        this.businessAuthService = businessAuthService;
        this.planAuthService = planAuthService;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    public EsbResp<EsbPageDataV3<EsbPlanBasicInfoV3DTO>> getPlanList(String username,
                                                                     String appCode,
                                                                     Long bizId,
                                                                     String scopeType,
                                                                     String scopeId,
                                                                     Long templateId,
                                                                     String creator,
                                                                     String name,
                                                                     Long createTimeStart,
                                                                     Long createTimeEnd,
                                                                     String lastModifyUser,
                                                                     Long lastModifyTimeStart,
                                                                     Long lastModifyTimeEnd,
                                                                     Integer start,
                                                                     Integer length) {
        EsbGetPlanListV3Request request = new EsbGetPlanListV3Request();
        request.setUserName(username);
        request.setAppCode(appCode);
        request.setBizId(bizId);
        request.setScopeType(scopeType);
        request.setScopeId(scopeId);
        request.setTemplateId(templateId);
        request.setCreator(creator);
        request.setName(name);
        request.setCreateTimeStart(createTimeStart);
        request.setCreateTimeEnd(createTimeEnd);
        request.setLastModifyUser(lastModifyUser);
        request.setLastModifyTimeStart(lastModifyTimeStart);
        request.setLastModifyTimeEnd(lastModifyTimeEnd);
        request.setStart(start);
        request.setLength(length);
        return getPlanListUsingPost(request);
    }

    @Override
    public EsbResp<EsbPlanInfoV3DTO> getPlanDetail(String username, String appCode, Long bizId,
                                                   String scopeType, String scopeId, Long planId) {
        EsbGetPlanDetailV3Request request = new EsbGetPlanDetailV3Request();
        request.setUserName(username);
        request.setAppCode(appCode);
        request.setBizId(bizId);
        request.setScopeType(scopeType);
        request.setScopeId(scopeId);
        request.setPlanId(planId);
        return getPlanDetailUsingPost(request);
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_get_job_plan_list"})
    public EsbResp<EsbPageDataV3<EsbPlanBasicInfoV3DTO>> getPlanListUsingPost(EsbGetPlanListV3Request request) {
        request.fillAppResourceScope(appScopeMappingService);
        ValidateResult checkResult = checkRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Get plan list, request is illegal!");
            return EsbResp.buildCommonFailResp(checkResult);
        }

        long appId = request.getAppId();

        AuthResult authResult =
            businessAuthService.authAccessBusiness(
                request.getUserName(), request.getAppResourceScope());
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        TaskPlanQueryDTO taskPlanQueryDTO = new TaskPlanQueryDTO();
        taskPlanQueryDTO.setAppId(appId);
        taskPlanQueryDTO.setName(request.getName());
        taskPlanQueryDTO.setTemplateId(request.getTemplateId());

        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        if (request.getStart() != null) {
            baseSearchCondition.setStart(request.getStart());
        } else {
            baseSearchCondition.setStart(0);
        }
        if (request.getLength() != null) {
            baseSearchCondition.setLength(request.getLength());
        } else {
            baseSearchCondition.setLength(20);
        }
        baseSearchCondition.setCreator(request.getCreator());
        baseSearchCondition.setLastModifyUser(request.getLastModifyUser());

        baseSearchCondition.setCreateTimeStart(request.getCreateTimeStart());
        baseSearchCondition.setCreateTimeEnd(request.getCreateTimeEnd());
        baseSearchCondition.setLastModifyTimeStart(request.getLastModifyTimeStart());
        baseSearchCondition.setLastModifyTimeEnd(request.getLastModifyTimeEnd());

        PageData<TaskPlanInfoDTO> pageTaskPlans = taskPlanService.listPageTaskPlansBasicInfo(taskPlanQueryDTO,
            baseSearchCondition, null);
        EsbPageDataV3<EsbPlanBasicInfoV3DTO> esbPageData = EsbPageDataV3.from(pageTaskPlans,
            this::convertToEsbPlanBasicInfo);
        return EsbResp.buildSuccessResp(esbPageData);
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_get_job_plan_detail"})
    public EsbResp<EsbPlanInfoV3DTO> getPlanDetailUsingPost(EsbGetPlanDetailV3Request request) {
        request.fillAppResourceScope(appScopeMappingService);
        ValidateResult validateResult = request.validate();
        if (validateResult.isPass()) {
            TaskPlanInfoDTO taskPlanInfo = taskPlanService.getTaskPlanById(request.getAppId(), request.getPlanId());
            if (taskPlanInfo != null) {
                AuthResult authResult =
                    planAuthService.authViewJobPlan(request.getUserName(),
                        request.getAppResourceScope(),
                        taskPlanInfo.getTemplateId(), request.getPlanId(), taskPlanInfo.getName());
                if (!authResult.isPass()) {
                    throw new PermissionDeniedException(authResult);
                }

                // 解析步骤引用全局变量的信息
                StepRefVariableParser.parseStepRefVars(taskPlanInfo.getStepList(), taskPlanInfo.getVariableList());
                return EsbResp.buildSuccessResp(TaskPlanInfoDTO.toEsbPlanInfoV3(taskPlanInfo));
            }
            return EsbResp.buildSuccessResp(null);
        } else {
            log.warn("Get plan detail request is illegal!");
            return EsbResp.buildCommonFailResp(validateResult);
        }
    }

    private ValidateResult checkRequest(EsbGetPlanListV3Request request) {
        // TODO 暂不校验，后面补上
        return ValidateResult.pass();
    }

    private EsbPlanBasicInfoV3DTO convertToEsbPlanBasicInfo(TaskPlanInfoDTO taskPlan) {
        EsbPlanBasicInfoV3DTO result = new EsbPlanBasicInfoV3DTO();
        result.setId(taskPlan.getId());
        EsbDTOAppScopeMappingHelper.fillEsbAppScopeDTOByAppId(taskPlan.getAppId(), result);
        result.setName(taskPlan.getName());
        result.setTemplateId(taskPlan.getTemplateId());
        result.setCreator(taskPlan.getCreator());
        result.setLastModifyUser(taskPlan.getLastModifyUser());
        result.setCreateTime(taskPlan.getCreateTime());
        result.setLastModifyTime(taskPlan.getLastModifyTime());
        return result;
    }
}
