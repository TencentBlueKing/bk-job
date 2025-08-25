/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.audit.annotations.AuditRequestBody;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.job.v3.EsbPageDataV3;
import com.tencent.bk.job.common.esb.util.EsbDTOAppScopeMappingHelper;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.api.esb.v3.EsbPlanV3Resource;
import com.tencent.bk.job.manage.manager.variable.StepRefVariableParser;
import com.tencent.bk.job.manage.model.dto.TaskPlanQueryDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetPlanDetailV3Request;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetPlanListV3Request;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbPlanBasicInfoV3DTO;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbPlanInfoV3DTO;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 15/10/2020 18:08
 */
@Slf4j
@RestController
public class EsbPlanV3ResourceImpl implements EsbPlanV3Resource {

    private final TaskPlanService taskPlanService;
    private final AppScopeMappingService appScopeMappingService;

    @Autowired
    public EsbPlanV3ResourceImpl(TaskPlanService taskPlanService,
                                 AppScopeMappingService appScopeMappingService) {
        this.taskPlanService = taskPlanService;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_get_job_plan_list"})
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
        request.fillAppResourceScope(appScopeMappingService);
        return getPlanListUsingPost(username, appCode, request);
    }

    @Override
    @AuditEntry(actionId = ActionId.VIEW_JOB_PLAN)
    public EsbResp<EsbPlanInfoV3DTO> getPlanDetail(String username,
                                                   String appCode,
                                                   Long bizId,
                                                   String scopeType,
                                                   String scopeId,
                                                   Long planId) {
        EsbGetPlanDetailV3Request request = new EsbGetPlanDetailV3Request();
        request.setBizId(bizId);
        request.setScopeType(scopeType);
        request.setScopeId(scopeId);
        request.setPlanId(planId);
        request.fillAppResourceScope(appScopeMappingService);
        return getPlanDetailUsingPost(username, appCode, request);
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_get_job_plan_list"})
    public EsbResp<EsbPageDataV3<EsbPlanBasicInfoV3DTO>> getPlanListUsingPost(String username,
                                                                              String appCode,
                                                                              EsbGetPlanListV3Request request) {
        ValidateResult checkResult = checkRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Get plan list, request is illegal!");
            return EsbResp.buildCommonFailResp(checkResult);
        }

        long appId = request.getAppId();

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
    @AuditEntry(actionId = ActionId.VIEW_JOB_PLAN)
    public EsbResp<EsbPlanInfoV3DTO> getPlanDetailUsingPost(String username,
                                                            String appCode,
                                                            @AuditRequestBody EsbGetPlanDetailV3Request request) {
        request.validate();

        User user = JobContextUtil.getUser();
        TaskPlanInfoDTO taskPlanInfo = taskPlanService.getTaskPlan(user,
            request.getAppId(), request.getPlanId());

        List<TaskStepDTO> enabledTaskStepList = taskPlanInfo.getStepList()
                .stream()
                .filter(taskStep -> taskStep.getEnable() != 0)
                .collect(Collectors.toList());

        // 解析启用的步骤引用全局变量的信息
        StepRefVariableParser.parseStepRefVars(enabledTaskStepList, taskPlanInfo.getVariableList());
        return EsbResp.buildSuccessResp(TaskPlanInfoDTO.toEsbPlanInfoV3(taskPlanInfo));
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
