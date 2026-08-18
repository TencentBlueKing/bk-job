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

package com.tencent.bk.job.execute.api.inner;

import com.tencent.bk.job.common.api.model.DryRunResult;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.model.error.ErrorType;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.tenant.TenantService;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.toggle.feature.FeatureManager;
import com.tencent.bk.job.execute.common.constants.FileTransferModeEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.FastTaskDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.OpenApiV4HostDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4FastTransferFileRequest;
import com.tencent.bk.job.execute.model.esb.v4.resp.V4JobExecuteDTO;
import com.tencent.bk.job.execute.model.inner.request.ServiceApprovalFastTransferFileRequest;
import com.tencent.bk.job.execute.service.TaskExecuteService;
import com.tencent.bk.job.execute.service.V4FastTransferFileRequestConverter;
import com.tencent.bk.job.execute.validate.ValidCallbackUrlValidator;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 带审批的执行类 inner 接口单测。
 * <p>
 * 重点锁定两条容易失效的性质：
 * <ol>
 *     <li><b>Bean Validation 缺口</b>：v4 请求体的必填与分组校验（GroupSequenceProvider）在 ESB 路径由网关
 *     触发的 Spring MVC 校验完成，而 inner 路径不经网关。若不在 Impl 里显式跑一次 Validator，就会出现
 *     "ESB 拦得住、审批预检拦不住"的不对称 —— 审批预检形同虚设，非法参数要等审批通过后放行时才炸。</li>
 *     <li><b>校验失败必须以 HTTP 200 + DryRunResult 返回</b>：一旦以异常传播，FeignErrorDecoder 会把它吞成
 *     InternalException，具体校验信息全丢，调用方只能看到一句"内部错误"。</li>
 * </ol>
 */
class ServiceApprovalExecuteResourceImplTest {

    private static final long APP_ID = 2L;
    private static final String OPERATOR = "admin";

    private TaskExecuteService taskExecuteService;

    private AppScopeMappingService appScopeMappingService;

    private V4FastTransferFileRequestConverter fastTransferFileRequestConverter;

    private ServiceApprovalExecuteResourceImpl resource;

    /**
     * InternalResponse 构造时会取国际化服务渲染 errorMsg，单测里注册一个空实现即可
     */
    @BeforeAll
    static void registerApplicationContext() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBean(MessageI18nService.class)).thenReturn(mock(MessageI18nService.class));
        // EsbAppScopeReq 的分组校验要查特性开关；关闭 bk_biz_id 兼容即走 bk_scope_type + bk_scope_id 这套校验
        FeatureManager featureManager = mock(FeatureManager.class);
        when(featureManager.checkFeature(anyString(), any())).thenReturn(false);
        when(applicationContext.getBean(FeatureManager.class)).thenReturn(featureManager);
        new ApplicationContextRegister().setApplicationContext(applicationContext);
    }

    @BeforeEach
    void setUp() {
        taskExecuteService = mock(TaskExecuteService.class);
        appScopeMappingService = mock(AppScopeMappingService.class);
        fastTransferFileRequestConverter = mock(V4FastTransferFileRequestConverter.class);
        TenantService tenantService = mock(TenantService.class);
        when(appScopeMappingService.getAppIdByScope(anyString(), anyString())).thenReturn(APP_ID);
        when(tenantService.getTenantIdByAppId(APP_ID)).thenReturn("tenant_a");
        when(fastTransferFileRequestConverter.convert(any(), any(), anyString(), anyBoolean()))
            .thenAnswer(invocation -> buildFastTask());
        resource = new ServiceApprovalExecuteResourceImpl(
            taskExecuteService,
            fastTransferFileRequestConverter,
            appScopeMappingService,
            tenantService,
            buildValidator()
        );
    }

    @Test
    @DisplayName("operator 缺失时预检拦住，不以调用方身份兜底")
    void givenBlankOperatorThenReturnInvalid() {
        DryRunResult<V4JobExecuteDTO> result = callFastTransferFile(baseRequest(), " ", true);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME);
        assertThat(result.getErrorParams()).containsExactly("operator");
        verifyNoInteractions(taskExecuteService);
    }

    @Test
    @DisplayName("v4 请求体缺失时预检拦住")
    void givenNullRequestThenReturnInvalid() {
        DryRunResult<V4JobExecuteDTO> result = callFastTransferFile(null, OPERATOR, true);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME);
        assertThat(result.getErrorParams()).containsExactly("request");
        verifyNoInteractions(taskExecuteService);
    }

    @Test
    @DisplayName("资源范围缺失时由 @ValidBkScope 拦住，并以 HTTP 200 + DryRunResult 返回")
    void givenMissingScopeThenReturnInvalidWithoutException() {
        V4FastTransferFileRequest v4Request = baseRequest();
        v4Request.setScopeType(null);
        v4Request.setScopeId(null);

        DryRunResult<V4JobExecuteDTO> result = callFastTransferFile(v4Request, OPERATOR, true);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON);
        assertThat(result.getErrorType()).isEqualTo(ErrorType.INVALID_PARAM.getType());
        // 校验没通过就绝不能碰执行链路，否则预检会穿透成真实执行
        verifyNoInteractions(taskExecuteService);
    }

    @Test
    @DisplayName("业务校验不通过时转成 DryRunResult，保留下游错误码与错误参数")
    void givenBusinessRejectionThenReturnInvalidWithErrorCode() {
        when(taskExecuteService.executeFastTask(any())).thenThrow(
            new InvalidParamException(ErrorCode.ACCOUNT_NOT_EXIST, new Object[]{"root"}));

        DryRunResult<V4JobExecuteDTO> result = callFastTransferFile(baseRequest(), OPERATOR, true);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_NOT_EXIST);
        assertThat(result.getErrorParams()).containsExactly("root");
    }

    @Test
    @DisplayName("内部错误继续以异常传播，不伪装成参数校验不通过")
    void givenInternalErrorThenPropagate() {
        when(taskExecuteService.executeFastTask(any())).thenThrow(new InternalException(ErrorCode.INTERNAL_ERROR));

        ServiceApprovalFastTransferFileRequest request = buildWrapper(baseRequest(), OPERATOR, true);
        assertThatThrownBy(() -> resource.fastTransferFile(request))
            .isInstanceOf(InternalException.class);
    }

    @Test
    @DisplayName("预检通过时返回概要，并标出按默认生效的超时时间与分发模式")
    void givenValidDryRunThenReturnSummary() {
        when(taskExecuteService.executeFastTask(any())).thenReturn(buildResolvedTaskInstance());

        DryRunResult<V4JobExecuteDTO> result = callFastTransferFile(baseRequest(), OPERATOR, true);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getResolvedSummary()).isNotNull();
        assertThat(result.getResolvedSummary().getTotalExecuteObjectCount()).isEqualTo(1);
        // 超时时间与分发模式都没传，实际会按默认值执行，单据必须逐项显式说明
        assertThat(result.getResolvedSummary().getDefaultsApplied()).hasSize(2);
        assertThat(result.getResolvedSummary().getDefaultsApplied().get(0).getValue())
            .isEqualTo(JobConstants.DEFAULT_JOB_TIMEOUT_SECONDS + "s");
        assertThat(result.getResolvedSummary().getDefaultsApplied().get(1).getValue())
            .isEqualTo(FileTransferModeEnum.FORCE.name());
        assertThat(result.getExecuteResult()).isNull();
        verify(taskExecuteService).executeFastTask(any());
    }

    @Test
    @DisplayName("放行执行时返回作业实例信息而非概要")
    void givenNotDryRunThenReturnTaskInstance() {
        when(taskExecuteService.executeFastTask(any())).thenAnswer(invocation -> {
            // 真实执行链路会把作业实例 ID 回填到入参上
            invocation.getArgument(0, FastTaskDTO.class).getTaskInstance().setId(1000L);
            return null;
        });

        DryRunResult<V4JobExecuteDTO> result = callFastTransferFile(baseRequest(), OPERATOR, false);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getResolvedSummary()).isNull();
        assertThat(result.getExecuteResult().getTaskInstanceId()).isEqualTo(1000L);
    }

    private DryRunResult<V4JobExecuteDTO> callFastTransferFile(V4FastTransferFileRequest v4Request,
                                                               String operator,
                                                               boolean dryRun) {
        InternalResponse<DryRunResult<V4JobExecuteDTO>> response =
            resource.fastTransferFile(buildWrapper(v4Request, operator, dryRun));
        // inner 接口一律返回成功响应：校验结果放在 DryRunResult 里，不走异常通道
        assertThat(response.isSuccess()).isTrue();
        return response.getData();
    }

    private ServiceApprovalFastTransferFileRequest buildWrapper(V4FastTransferFileRequest v4Request,
                                                                String operator,
                                                                boolean dryRun) {
        ServiceApprovalFastTransferFileRequest request = new ServiceApprovalFastTransferFileRequest();
        request.setRequest(v4Request);
        request.setOperator(operator);
        request.setAppCode("bk_ai");
        request.setDryRun(dryRun);
        return request;
    }

    private V4FastTransferFileRequest baseRequest() {
        V4FastTransferFileRequest request = new V4FastTransferFileRequest();
        request.setScopeType("biz");
        request.setScopeId("2");
        request.setName("test_task");
        request.setTargetPath("/tmp/");
        request.setAccountAlias("root");
        request.setExecuteTarget(buildV4ExecuteTarget());
        return request;
    }

    /**
     * 转换器在单测里被 mock，需自行提供一个结构完整的执行任务，供放行分支回填作业实例 ID
     */
    private FastTaskDTO buildFastTask() {
        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setName("test_task");
        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setName("test_task");
        stepInstance.setExecuteType(StepExecuteTypeEnum.SEND_FILE);
        return FastTaskDTO.builder()
            .taskInstance(taskInstance)
            .stepInstance(stepInstance)
            .build();
    }

    private V4ExecuteTargetDTO buildV4ExecuteTarget() {
        OpenApiV4HostDTO host = new OpenApiV4HostDTO();
        host.setBkHostId(101L);
        V4ExecuteTargetDTO executeTarget = new V4ExecuteTargetDTO();
        executeTarget.setHostList(Collections.singletonList(host));
        return executeTarget;
    }

    /**
     * 模拟预检返回的作业实例：执行对象已在 dryRun 返回点之前解析完成
     */
    private TaskInstanceDTO buildResolvedTaskInstance() {
        HostDTO host = new HostDTO();
        host.setHostId(101L);
        host.setBkCloudId(0L);
        host.setIp("127.0.0.1");
        ExecuteTargetDTO target = new ExecuteTargetDTO();
        target.setExecuteObjects(Collections.singletonList(new ExecuteObject(host)));

        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setName("test_task");
        stepInstance.setExecuteType(StepExecuteTypeEnum.SEND_FILE);
        stepInstance.setAccountAlias("root");
        stepInstance.setTargetExecuteObjects(target);

        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setName("test_task");
        taskInstance.setStepInstances(Collections.singletonList(stepInstance));
        return taskInstance;
    }

    /**
     * 用 ParameterMessageInterpolator 构造校验器，避免单测依赖 EL 实现
     */
    private Validator buildValidator() {
        return Validation.byProvider(HibernateValidator.class)
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .constraintValidatorFactory(new TestConstraintValidatorFactory())
            .buildValidatorFactory()
            .getValidator();
    }

    /**
     * 生产环境注入的是 Spring 的 Validator，能为依赖 Bean 的校验器完成注入；
     * 单测里只需让这类校验器可实例化，其判定逻辑由各自的单测覆盖
     */
    private static class TestConstraintValidatorFactory implements ConstraintValidatorFactory {

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
            if (key == ValidCallbackUrlValidator.class) {
                // Service 取不到时校验器 fail-open 放行
                return (T) new ValidCallbackUrlValidator(mock(ObjectProvider.class));
            }
            try {
                return key.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Can not instantiate validator " + key.getName(), e);
            }
        }

        @Override
        public void releaseInstance(ConstraintValidator<?, ?> instance) {
        }
    }
}
