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

package com.tencent.bk.job.manage.api.inner.impl;

import com.tencent.bk.job.common.api.model.DryRunResult;
import com.tencent.bk.job.common.api.model.ResolvedSummary;
import com.tencent.bk.job.common.api.util.DryRunResultUtil;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.model.error.ErrorType;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.tenant.TenantService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.api.inner.ServiceApprovalJobPlanResource;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.esb.v4.OpenApiV4JobPlanDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4CreateJobPlanRequest;
import com.tencent.bk.job.manage.model.inner.request.ServiceApprovalCreateJobPlanRequest;
import com.tencent.bk.job.manage.service.plan.V4JobPlanCreateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Validator;

@RestController
@Slf4j
public class ServiceApprovalJobPlanResourceImpl implements ServiceApprovalJobPlanResource {

    private final V4JobPlanCreateService jobPlanCreateService;

    private final AppScopeMappingService appScopeMappingService;

    private final TenantService tenantService;

    /**
     * inner 路径不经过网关，Spring MVC 的自动校验也不会作用于 @RequestBody 内嵌的 v4 请求体，
     * 必须显式持有 Validator 手动跑一次，否则 v4 请求体上的注解约束在审批预检时全部失效
     */
    private final Validator validator;

    @Autowired
    public ServiceApprovalJobPlanResourceImpl(V4JobPlanCreateService jobPlanCreateService,
                                              AppScopeMappingService appScopeMappingService,
                                              TenantService tenantService,
                                              Validator validator) {
        this.jobPlanCreateService = jobPlanCreateService;
        this.appScopeMappingService = appScopeMappingService;
        this.tenantService = tenantService;
        this.validator = validator;
    }

    @Override
    public InternalResponse<DryRunResult<OpenApiV4JobPlanDTO>> createJobPlan(
        ServiceApprovalCreateJobPlanRequest request) {

        log.info("Approval create job plan, operator: {}, dryRun: {}", request.getOperator(), request.isDryRun());
        V4CreateJobPlanRequest v4Request = request.getRequest();
        if (v4Request == null) {
            return InternalResponse.buildSuccessResp(invalidParam("request"));
        }
        if (StringUtils.isBlank(request.getOperator())) {
            // 操作人缺失绝不能兜底成当前调用方：创建人与后续鉴权都取该值
            return InternalResponse.buildSuccessResp(invalidParam("operator"));
        }
        return InternalResponse.buildSuccessResp(DryRunResultUtil.call(v4Request, validator, () -> {
            User operator = prepareOperator(v4Request, request.getOperator());
            TaskPlanInfoDTO plan = jobPlanCreateService.createJobPlan(operator, v4Request, request.isDryRun());
            if (!request.isDryRun()) {
                return DryRunResult.valid(null, buildJobPlanResult(v4Request.getAppId(), operator, plan));
            }
            return DryRunResult.valid(buildSummary(plan), null);
        }));
    }

    private <T> DryRunResult<T> invalidParam(String paramName) {
        return DryRunResult.invalid(
            ErrorCode.MISSING_PARAM_WITH_PARAM_NAME,
            new Object[]{paramName},
            ErrorType.INVALID_PARAM.getType()
        );
    }

    /**
     * 补全资源范围并构造操作人。
     * <p>
     * ESB 路径下 appId 由 EsbAppResourceScopeReqAspect 按请求路径切面补全、操作人由网关鉴权后注入，
     * 两者在 inner 路径都不生效，必须在此显式完成。租户由 appId 反查而非取调用方传值，避免上游传错租户导致越权。
     * V4JobPlanCreateService 内部还会再补一次资源范围，该操作是幂等的。
     */
    private User prepareOperator(V4CreateJobPlanRequest v4Request, String operator) {
        v4Request.fillAppResourceScope(appScopeMappingService);
        if (v4Request.getAppId() == null) {
            throw new InvalidParamException(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME,
                new Object[]{"bk_scope_type|bk_scope_id"});
        }
        String tenantId = tenantService.getTenantIdByAppId(v4Request.getAppId());
        User user = new User(tenantId, operator, operator);
        JobContextUtil.setUser(user);
        return user;
    }

    /**
     * 创建执行方案没有执行目标，概要区展示的是"将基于哪个模板、启用哪些步骤建出什么方案"
     */
    private ResolvedSummary buildSummary(TaskPlanInfoDTO plan) {
        ResolvedSummary summary = new ResolvedSummary();
        summary.setName(plan.getName());
        summary.addField("job_template_id", String.valueOf(plan.getTemplateId()));
        if (CollectionUtils.isNotEmpty(plan.getEnableStepList())) {
            summary.addField("enable_steps", StringUtils.join(plan.getEnableStepList(), ","));
        }
        if (CollectionUtils.isNotEmpty(plan.getVariableList())) {
            summary.addField("variable_count", String.valueOf(plan.getVariableList().size()));
        }
        return summary;
    }

    private OpenApiV4JobPlanDTO buildJobPlanResult(Long appId, User operator, TaskPlanInfoDTO plan) {
        OpenApiV4JobPlanDTO result = new OpenApiV4JobPlanDTO();
        ResourceScope scope = appScopeMappingService.getScopeByAppId(appId);
        if (scope != null) {
            result.setScopeType(scope.getType().getValue());
            result.setScopeId(scope.getId());
        }
        result.setJobPlanId(plan.getId());
        result.setJobPlanName(plan.getName());
        result.setJobTemplateId(plan.getTemplateId());
        result.setCreator(plan.getCreator() != null ? plan.getCreator() : operator.getUsername());
        Long createTimeSeconds = plan.getCreateTime();
        result.setCreateTime(createTimeSeconds == null ? null : createTimeSeconds * 1000L);
        result.setNeedUpdate(Boolean.TRUE.equals(plan.getNeedUpdate()));
        return result;
    }
}
