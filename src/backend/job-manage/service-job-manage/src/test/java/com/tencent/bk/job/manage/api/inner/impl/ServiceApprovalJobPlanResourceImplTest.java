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
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.AlreadyExistsException;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.error.ErrorType;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.tenant.TenantService;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.toggle.feature.FeatureManager;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.esb.v4.OpenApiV4JobPlanDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4CreateJobPlanRequest;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobPlanVariableItem;
import com.tencent.bk.job.manage.model.inner.request.ServiceApprovalCreateJobPlanRequest;
import com.tencent.bk.job.manage.service.plan.V4JobPlanCreateService;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 带审批的创建执行方案 inner 接口单测。
 * <p>
 * 与 job-execute 侧同一套契约，重点仍是"inner 路径不经网关"带来的两个缺口：
 * <ol>
 *     <li><b>Bean Validation 必须在 Impl 里显式跑</b>。本接口的 v4 请求体没有 GroupSequenceProvider，
 *     生效的是字段注解与 {@code @Valid} 的嵌套校验 —— 嵌套校验最容易被漏掉，
 *     因为它在 ESB 路径由网关触发的 MVC 校验里"自动"生效，看起来不需要谁负责。</li>
 *     <li><b>校验失败必须以 HTTP 200 + DryRunResult 返回</b>，否则错误信息会被 FeignErrorDecoder 吞掉。</li>
 * </ol>
 */
class ServiceApprovalJobPlanResourceImplTest {

    private static final long APP_ID = 2L;
    private static final String OPERATOR = "admin";

    private V4JobPlanCreateService jobPlanCreateService;

    private ServiceApprovalJobPlanResourceImpl resource;

    /**
     * v4 请求体继承 EsbAppScopeReq，其分组校验会查特性开关；国际化服务则用于渲染 errorMsg。
     * 两者都取自静态的 ApplicationContextRegister，单测里注册桩实现即可。
     * FeatureManager 桩默认返回 false（不兼容 bk_biz_id），与 v4 只认 scope 参数的协议一致
     */
    @BeforeAll
    static void registerApplicationContext() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBean(MessageI18nService.class)).thenReturn(mock(MessageI18nService.class));
        when(applicationContext.getBean(FeatureManager.class)).thenReturn(mock(FeatureManager.class));
        new ApplicationContextRegister().setApplicationContext(applicationContext);
    }

    @BeforeEach
    void setUp() {
        jobPlanCreateService = mock(V4JobPlanCreateService.class);
        AppScopeMappingService appScopeMappingService = mock(AppScopeMappingService.class);
        TenantService tenantService = mock(TenantService.class);
        when(appScopeMappingService.getAppIdByScope(anyString(), anyString())).thenReturn(APP_ID);
        when(tenantService.getTenantIdByAppId(APP_ID)).thenReturn("tenant_a");
        resource = new ServiceApprovalJobPlanResourceImpl(
            jobPlanCreateService, appScopeMappingService, tenantService, buildValidator());
    }

    @Test
    @DisplayName("inner 路径显式跑 Bean Validation：作业模板 ID 缺失时预检拦住，不进入创建链路")
    void givenMissingTemplateIdThenReturnInvalid() {
        V4CreateJobPlanRequest v4Request = baseRequest();
        v4Request.setJobTemplateId(null);

        DryRunResult<OpenApiV4JobPlanDTO> result = callCreateJobPlan(v4Request, OPERATOR, true);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON);
        assertThat(result.getErrorType()).isEqualTo(ErrorType.INVALID_PARAM.getType());
        assertThat((String) result.getErrorParams()[0]).contains("jobTemplateId");
        verifyNoInteractions(jobPlanCreateService);
    }

    @Test
    @DisplayName("inner 路径显式跑 Bean Validation：方案名为空时预检拦住")
    void givenBlankNameThenReturnInvalid() {
        V4CreateJobPlanRequest v4Request = baseRequest();
        v4Request.setName(" ");

        DryRunResult<OpenApiV4JobPlanDTO> result = callCreateJobPlan(v4Request, OPERATOR, true);

        assertThat(result.isValid()).isFalse();
        assertThat((String) result.getErrorParams()[0]).contains("name");
        verifyNoInteractions(jobPlanCreateService);
    }

    @Test
    @DisplayName("嵌套的变量列表约束同样生效：变量名为空时预检拦住")
    void givenBlankVariableNameThenReturnInvalid() {
        V4CreateJobPlanRequest v4Request = baseRequest();
        V4JobPlanVariableItem variable = new V4JobPlanVariableItem();
        variable.setName(" ");
        variable.setValue("v");
        v4Request.setVariables(Collections.singletonList(variable));

        DryRunResult<OpenApiV4JobPlanDTO> result = callCreateJobPlan(v4Request, OPERATOR, true);

        assertThat(result.isValid()).isFalse();
        assertThat((String) result.getErrorParams()[0]).contains("variables[0].name");
        verifyNoInteractions(jobPlanCreateService);
    }

    @Test
    @DisplayName("多个字段同时不合法时错误信息按属性路径排序，保证返回稳定")
    void givenMultipleViolationsThenReportSortedParams() {
        V4CreateJobPlanRequest v4Request = baseRequest();
        v4Request.setJobTemplateId(0L);
        v4Request.setName(null);
        v4Request.setEnableSteps(Collections.emptyList());

        DryRunResult<OpenApiV4JobPlanDTO> result = callCreateJobPlan(v4Request, OPERATOR, true);

        assertThat(result.isValid()).isFalse();
        assertThat((String) result.getErrorParams()[0]).isEqualTo("enableSteps,jobTemplateId,name");
    }

    @Test
    @DisplayName("operator 缺失时预检拦住，不以调用方身份兜底")
    void givenBlankOperatorThenReturnInvalid() {
        DryRunResult<OpenApiV4JobPlanDTO> result = callCreateJobPlan(baseRequest(), " ", true);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME);
        assertThat(result.getErrorParams()).containsExactly("operator");
        verifyNoInteractions(jobPlanCreateService);
    }

    @Test
    @DisplayName("v4 请求体缺失时预检拦住")
    void givenNullRequestThenReturnInvalid() {
        DryRunResult<OpenApiV4JobPlanDTO> result = callCreateJobPlan(null, OPERATOR, true);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME);
        assertThat(result.getErrorParams()).containsExactly("request");
        verifyNoInteractions(jobPlanCreateService);
    }

    @Test
    @DisplayName("资源范围缺失时以 HTTP 200 + DryRunResult 返回，不抛异常")
    void givenMissingScopeThenReturnInvalidWithoutException() {
        V4CreateJobPlanRequest v4Request = baseRequest();
        v4Request.setScopeType(null);
        v4Request.setScopeId(null);

        DryRunResult<OpenApiV4JobPlanDTO> result = callCreateJobPlan(v4Request, OPERATOR, true);

        // 资源范围由 EsbAppScopeReq 继承来的分组校验拦住，这正是 inner 路径最容易漏掉的一层
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON);
        assertThat((String) result.getErrorParams()[0]).contains("scopeType");
        verifyNoInteractions(jobPlanCreateService);
    }

    @Test
    @DisplayName("业务校验不通过时转成 DryRunResult，保留下游错误码与错误参数")
    void givenBusinessRejectionThenReturnInvalidWithErrorCode() {
        when(jobPlanCreateService.createJobPlan(any(), any(), anyBoolean()))
            .thenThrow(new AlreadyExistsException(ErrorCode.PLAN_NAME_EXIST, new Object[]{"plan-1"}));

        DryRunResult<OpenApiV4JobPlanDTO> result = callCreateJobPlan(baseRequest(), OPERATOR, true);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(ErrorCode.PLAN_NAME_EXIST);
        assertThat(result.getErrorParams()).containsExactly("plan-1");
    }

    @Test
    @DisplayName("内部错误继续以异常传播，不伪装成参数校验不通过")
    void givenInternalErrorThenPropagate() {
        when(jobPlanCreateService.createJobPlan(any(), any(), anyBoolean()))
            .thenThrow(new InternalException(ErrorCode.INTERNAL_ERROR));

        ServiceApprovalCreateJobPlanRequest request = buildWrapper(baseRequest(), OPERATOR, true);
        assertThatThrownBy(() -> resource.createJobPlan(request)).isInstanceOf(InternalException.class);
    }

    @Test
    @DisplayName("预检通过时返回概要：审批人能看到基于哪个模板、启用哪些步骤")
    void givenValidDryRunThenReturnSummary() {
        when(jobPlanCreateService.createJobPlan(any(), any(), anyBoolean())).thenReturn(buildPlan());

        DryRunResult<OpenApiV4JobPlanDTO> result = callCreateJobPlan(baseRequest(), OPERATOR, true);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getResolvedSummary()).isNotNull();
        assertThat(result.getResolvedSummary().getName()).isEqualTo("plan-1");
        assertThat(result.getResolvedSummary().getFields())
            .anySatisfy(field -> assertThat(field.getLabel()).isEqualTo("job_template_id"));
        assertThat(result.getExecuteResult()).isNull();
    }

    @Test
    @DisplayName("放行执行时返回执行方案信息而非概要")
    void givenNotDryRunThenReturnJobPlan() {
        when(jobPlanCreateService.createJobPlan(any(), any(), anyBoolean())).thenReturn(buildPlan());

        DryRunResult<OpenApiV4JobPlanDTO> result = callCreateJobPlan(baseRequest(), OPERATOR, false);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getResolvedSummary()).isNull();
        assertThat(result.getExecuteResult().getJobPlanId()).isEqualTo(500L);
    }

    private DryRunResult<OpenApiV4JobPlanDTO> callCreateJobPlan(V4CreateJobPlanRequest v4Request,
                                                                String operator,
                                                                boolean dryRun) {
        InternalResponse<DryRunResult<OpenApiV4JobPlanDTO>> response =
            resource.createJobPlan(buildWrapper(v4Request, operator, dryRun));
        // inner 接口一律返回成功响应：校验结果放在 DryRunResult 里，不走异常通道
        assertThat(response.isSuccess()).isTrue();
        return response.getData();
    }

    private ServiceApprovalCreateJobPlanRequest buildWrapper(V4CreateJobPlanRequest v4Request,
                                                             String operator,
                                                             boolean dryRun) {
        ServiceApprovalCreateJobPlanRequest request = new ServiceApprovalCreateJobPlanRequest();
        request.setRequest(v4Request);
        request.setOperator(operator);
        request.setAppCode("bk_ai");
        request.setDryRun(dryRun);
        return request;
    }

    private V4CreateJobPlanRequest baseRequest() {
        V4CreateJobPlanRequest request = new V4CreateJobPlanRequest();
        request.setScopeType("biz");
        request.setScopeId("2");
        request.setJobTemplateId(100L);
        request.setName("plan-1");
        return request;
    }

    private TaskPlanInfoDTO buildPlan() {
        TaskPlanInfoDTO plan = new TaskPlanInfoDTO();
        plan.setId(500L);
        plan.setName("plan-1");
        plan.setTemplateId(100L);
        plan.setEnableStepList(Arrays.asList(1L, 2L));
        plan.setCreator(OPERATOR);
        return plan;
    }

    /**
     * 用 ParameterMessageInterpolator 构造校验器，避免单测依赖 EL 实现
     */
    private Validator buildValidator() {
        return Validation.byProvider(HibernateValidator.class)
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();
    }
}
