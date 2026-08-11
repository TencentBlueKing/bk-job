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

package com.tencent.bk.job.crontab.api.inner.impl;

import com.tencent.bk.job.common.api.model.DryRunResult;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.error.ErrorType;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.service.CommonAppService;
import com.tencent.bk.job.common.tenant.TenantService;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.toggle.feature.FeatureManager;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.model.esb.v4.req.V4SaveCronRequest;
import com.tencent.bk.job.crontab.model.esb.v4.req.V4UpdateCronStatusRequest;
import com.tencent.bk.job.crontab.model.esb.v4.resp.V4CronJobDTO;
import com.tencent.bk.job.crontab.model.inner.request.ServiceApprovalSaveCronRequest;
import com.tencent.bk.job.crontab.model.inner.request.ServiceApprovalUpdateCronStatusRequest;
import com.tencent.bk.job.crontab.service.CronJobService;
import com.tencent.bk.job.crontab.service.V4SaveCronRequestConverter;
import com.tencent.bk.job.execute.model.esb.v4.req.V4GlobalVarDTO;
import com.tencent.bk.job.manage.api.inner.ServiceTaskPlanResource;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 带审批的定时任务 inner 接口单测。
 * <p>
 * 定时任务这一侧的校验分布与另外两个 Impl 不同，这也是本类存在的理由：
 * <ul>
 *     <li>{@code V4SaveCronRequest} 上只有<b>字段级注解</b>（名称长度、NoXss、全局变量嵌套校验），
 *     由 Impl 显式跑的 Validator 负责；</li>
 *     <li>"新建必填项""更新至少改一项""表达式与单次执行时间不可同时为空"这类<b>字段间联合校验</b>
 *     不在注解里，而在 {@link V4SaveCronRequestConverter} 中 —— 因此它只能在业务动作里抛异常，
 *     必须由 DryRunResultUtil 转成 DryRunResult，否则错误信息会被 FeignErrorDecoder 吞成"内部错误"。</li>
 * </ul>
 * 两条路径都得覆盖：只测其中一条，另一条失效时不会有任何用例报警。
 */
class ServiceApprovalCronResourceImplTest {

    private static final long APP_ID = 2L;
    private static final long CRON_ID = 300L;
    private static final String OPERATOR = "admin";

    private CronJobService cronJobService;

    private ServiceApprovalCronResourceImpl resource;

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
        cronJobService = mock(CronJobService.class);
        AppScopeMappingService appScopeMappingService = mock(AppScopeMappingService.class);
        TenantService tenantService = mock(TenantService.class);
        when(appScopeMappingService.getAppIdByScope(anyString(), anyString())).thenReturn(APP_ID);
        when(tenantService.getTenantIdByAppId(APP_ID)).thenReturn("tenant_a");
        resource = new ServiceApprovalCronResourceImpl(
            cronJobService,
            new V4SaveCronRequestConverter(mock(ServiceTaskPlanResource.class), mock(CommonAppService.class)),
            appScopeMappingService,
            tenantService,
            buildValidator()
        );
    }

    @Nested
    @DisplayName("保存定时任务")
    class SaveCronTest {

        @Test
        @DisplayName("字段级注解校验：名称超长时预检拦住，不进入保存链路")
        void givenTooLongNameThenReturnInvalid() {
            V4SaveCronRequest v4Request = baseCreateRequest();
            v4Request.setName(repeat("a", 61));

            DryRunResult<V4CronJobDTO> result = callSaveCron(v4Request, OPERATOR, true);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON);
            assertThat(result.getErrorType()).isEqualTo(ErrorType.INVALID_PARAM.getType());
            assertThat((String) result.getErrorParams()[0]).contains("name");
            verifyNoInteractions(cronJobService);
        }

        @Test
        @DisplayName("全局变量既没给 ID 也没给名称时预检拦住：该校验在 Converter 里，走的是异常转返回值这条路")
        void givenInvalidGlobalVarThenReturnInvalid() {
            V4SaveCronRequest v4Request = baseCreateRequest();
            v4Request.setGlobalVarList(Collections.singletonList(new V4GlobalVarDTO()));

            DryRunResult<V4CronJobDTO> result = callSaveCron(v4Request, OPERATOR, true);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ErrorCode.ILLEGAL_PARAM_WITH_REASON);
            assertThat((String) result.getErrorParams()[0]).contains("id/name");
            verifyNoInteractions(cronJobService);
        }

        @Test
        @DisplayName("联合校验在业务动作里抛异常，同样以 HTTP 200 + DryRunResult 返回：新建缺执行方案 ID")
        void givenCreateWithoutPlanIdThenReturnInvalid() {
            V4SaveCronRequest v4Request = baseCreateRequest();
            v4Request.setPlanId(null);

            DryRunResult<V4CronJobDTO> result = callSaveCron(v4Request, OPERATOR, true);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON);
            assertThat(result.getErrorParams()[0]).isEqualTo("job_plan_id");
            verify(cronJobService, never()).dryRunCreateCronJobInfo(any(), any());
        }

        @Test
        @DisplayName("联合校验：表达式与单次执行时间都没传时预检拦住")
        void givenCreateWithoutExpressionAndExecuteTimeThenReturnInvalid() {
            V4SaveCronRequest v4Request = baseCreateRequest();
            v4Request.setCronExpression(null);
            v4Request.setExecuteTime(null);

            DryRunResult<V4CronJobDTO> result = callSaveCron(v4Request, OPERATOR, true);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorParams()[0]).isEqualTo("expression/execute_time");
        }

        @Test
        @DisplayName("联合校验：更新时一项都没改时预检拦住")
        void givenUpdateWithoutAnyChangeThenReturnInvalid() {
            V4SaveCronRequest v4Request = new V4SaveCronRequest();
            v4Request.setScopeType("biz");
            v4Request.setScopeId("2");
            v4Request.setId(CRON_ID);

            DryRunResult<V4CronJobDTO> result = callSaveCron(v4Request, OPERATOR, true);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorParams()[0]).isEqualTo("job_plan_id/name/expression/execute_time");
            verify(cronJobService, never()).dryRunUpdateCronJobInfo(any(), any());
        }

        @Test
        @DisplayName("非法 cron 表达式在预检阶段就被拦住")
        void givenIllegalCronExpressionThenReturnInvalid() {
            V4SaveCronRequest v4Request = baseCreateRequest();
            v4Request.setCronExpression("not-a-cron");

            DryRunResult<V4CronJobDTO> result = callSaveCron(v4Request, OPERATOR, true);

            assertThat(result.isValid()).isFalse();
            verifyNoInteractions(cronJobService);
        }

        @Test
        @DisplayName("operator 缺失时预检拦住，不以调用方身份兜底")
        void givenBlankOperatorThenReturnInvalid() {
            DryRunResult<V4CronJobDTO> result = callSaveCron(baseCreateRequest(), " ", true);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME);
            assertThat(result.getErrorParams()).containsExactly("operator");
            verifyNoInteractions(cronJobService);
        }

        @Test
        @DisplayName("v4 请求体缺失时预检拦住")
        void givenNullRequestThenReturnInvalid() {
            DryRunResult<V4CronJobDTO> result = callSaveCron(null, OPERATOR, true);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorParams()).containsExactly("request");
            verifyNoInteractions(cronJobService);
        }

        @Test
        @DisplayName("新建预检通过时概要区标明是新建，并带上周期与执行方案")
        void givenValidCreateDryRunThenReturnSummary() {
            when(cronJobService.dryRunCreateCronJobInfo(any(), any())).thenReturn(buildCronJobInfo());

            DryRunResult<V4CronJobDTO> result = callSaveCron(baseCreateRequest(), OPERATOR, true);

            assertThat(result.isValid()).isTrue();
            assertThat(result.getResolvedSummary().getName()).isEqualTo("cron-1");
            assertThat(fieldValue(result, "operation")).isEqualTo("CREATE");
            assertThat(fieldValue(result, "job_plan_id")).isEqualTo("100");
            assertThat(fieldValue(result, "cron_expression")).isEqualTo("0/5 * * * *");
            assertThat(result.getExecuteResult()).isNull();
        }

        @Test
        @DisplayName("更新预检通过时概要区标明是更新，并带上定时任务 ID")
        void givenValidUpdateDryRunThenReturnSummary() {
            when(cronJobService.dryRunUpdateCronJobInfo(any(), any())).thenReturn(buildCronJobInfo());
            V4SaveCronRequest v4Request = baseCreateRequest();
            v4Request.setId(CRON_ID);

            DryRunResult<V4CronJobDTO> result = callSaveCron(v4Request, OPERATOR, true);

            assertThat(result.isValid()).isTrue();
            assertThat(fieldValue(result, "operation")).isEqualTo("UPDATE");
            assertThat(fieldValue(result, "cron_id")).isEqualTo(String.valueOf(CRON_ID));
        }

        @Test
        @DisplayName("放行执行时返回定时任务信息而非概要")
        void givenNotDryRunThenReturnCronJob() {
            when(cronJobService.createCronJobInfo(any(), any())).thenReturn(buildCronJobInfo());

            DryRunResult<V4CronJobDTO> result = callSaveCron(baseCreateRequest(), OPERATOR, false);

            assertThat(result.isValid()).isTrue();
            assertThat(result.getResolvedSummary()).isNull();
            assertThat(result.getExecuteResult().getId()).isEqualTo(CRON_ID);
        }

        @Test
        @DisplayName("内部错误继续以异常传播，不伪装成参数校验不通过")
        void givenInternalErrorThenPropagate() {
            when(cronJobService.dryRunCreateCronJobInfo(any(), any()))
                .thenThrow(new InternalException(ErrorCode.INTERNAL_ERROR));

            ServiceApprovalSaveCronRequest request = buildSaveWrapper(baseCreateRequest(), OPERATOR, true);
            assertThatThrownBy(() -> resource.saveCron(request)).isInstanceOf(InternalException.class);
        }
    }

    @Nested
    @DisplayName("启停定时任务")
    class UpdateCronStatusTest {

        @Test
        @DisplayName("定时任务 ID 非法时预检拦住")
        void givenIllegalIdThenReturnInvalid() {
            V4UpdateCronStatusRequest v4Request = baseStatusRequest();
            v4Request.setId(0L);

            DryRunResult<V4CronJobDTO> result = callUpdateCronStatus(v4Request, OPERATOR, true);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorParams()[0]).isEqualTo("id");
            assertThat(result.getErrorType()).isEqualTo(ErrorType.INVALID_PARAM.getType());
        }

        @Test
        @DisplayName("目标状态取值非法时预检拦住")
        void givenIllegalStatusThenReturnInvalid() {
            V4UpdateCronStatusRequest v4Request = baseStatusRequest();
            v4Request.setStatus(9);

            DryRunResult<V4CronJobDTO> result = callUpdateCronStatus(v4Request, OPERATOR, true);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorParams()[0]).isEqualTo("status");
        }

        @Test
        @DisplayName("定时任务不存在时拒绝，绝不产出「只剩 id 与目标状态」的概要")
        void givenCronNotExistThenReject() {
            when(cronJobService.getCronJobInfoById(anyLong(), anyLong()))
                .thenThrow(new NotFoundException(ErrorCode.CRON_JOB_NOT_EXIST));

            DryRunResult<V4CronJobDTO> result = callUpdateCronStatus(baseStatusRequest(), OPERATOR, true);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ErrorCode.CRON_JOB_NOT_EXIST);
            assertThat(result.getErrorType()).isEqualTo(ErrorType.NOT_FOUND.getType());
        }

        @Test
        @DisplayName("预检通过时补出定时任务名与执行方案，审批人不必对着一个数字 ID 盲签")
        void givenValidDryRunThenReturnEnrichedSummary() {
            when(cronJobService.getCronJobInfoById(anyLong(), anyLong())).thenReturn(buildCronJobInfo());

            DryRunResult<V4CronJobDTO> result = callUpdateCronStatus(baseStatusRequest(), OPERATOR, true);

            assertThat(result.isValid()).isTrue();
            assertThat(result.getResolvedSummary().getName()).isEqualTo("cron-1");
            assertThat(fieldValue(result, "target_status")).isEqualTo("RUNNING");
            assertThat(fieldValue(result, "job_plan_id")).isEqualTo("100");
            verify(cronJobService).dryRunChangeCronJobEnableStatus(any(), eq(APP_ID), eq(CRON_ID), eq(true));
            verify(cronJobService, never()).changeCronJobEnableStatus(any(), anyLong(), anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("放行执行时真正改状态并返回结果")
        void givenNotDryRunThenChangeStatus() {
            DryRunResult<V4CronJobDTO> result = callUpdateCronStatus(baseStatusRequest(), OPERATOR, false);

            assertThat(result.isValid()).isTrue();
            assertThat(result.getResolvedSummary()).isNull();
            assertThat(result.getExecuteResult().getId()).isEqualTo(CRON_ID);
            verify(cronJobService).changeCronJobEnableStatus(any(), eq(APP_ID), eq(CRON_ID), eq(true));
        }

        @Test
        @DisplayName("operator 缺失时预检拦住")
        void givenBlankOperatorThenReturnInvalid() {
            DryRunResult<V4CronJobDTO> result = callUpdateCronStatus(baseStatusRequest(), " ", true);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorParams()).containsExactly("operator");
            verifyNoInteractions(cronJobService);
        }

        @Test
        @DisplayName("资源范围缺失时以 HTTP 200 + DryRunResult 返回，不抛异常")
        void givenMissingScopeThenReturnInvalidWithoutException() {
            V4UpdateCronStatusRequest v4Request = baseStatusRequest();
            v4Request.setScopeType(null);
            v4Request.setScopeId(null);

            DryRunResult<V4CronJobDTO> result = callUpdateCronStatus(v4Request, OPERATOR, true);

            // 资源范围由 EsbAppScopeReq 继承来的分组校验拦住，这正是 inner 路径最容易漏掉的一层
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON);
            assertThat((String) result.getErrorParams()[0]).contains("scopeType");
            verifyNoInteractions(cronJobService);
        }
    }

    private String fieldValue(DryRunResult<V4CronJobDTO> result, String label) {
        return result.getResolvedSummary().getFields().stream()
            .filter(field -> label.equals(field.getLabel()))
            .map(field -> field.getValue())
            .findFirst()
            .orElse(null);
    }

    private DryRunResult<V4CronJobDTO> callSaveCron(V4SaveCronRequest v4Request, String operator, boolean dryRun) {
        InternalResponse<DryRunResult<V4CronJobDTO>> response =
            resource.saveCron(buildSaveWrapper(v4Request, operator, dryRun));
        assertThat(response.isSuccess()).isTrue();
        return response.getData();
    }

    private DryRunResult<V4CronJobDTO> callUpdateCronStatus(V4UpdateCronStatusRequest v4Request,
                                                            String operator,
                                                            boolean dryRun) {
        ServiceApprovalUpdateCronStatusRequest request = new ServiceApprovalUpdateCronStatusRequest();
        request.setRequest(v4Request);
        request.setOperator(operator);
        request.setAppCode("bk_ai");
        request.setDryRun(dryRun);
        InternalResponse<DryRunResult<V4CronJobDTO>> response = resource.updateCronStatus(request);
        assertThat(response.isSuccess()).isTrue();
        return response.getData();
    }

    private ServiceApprovalSaveCronRequest buildSaveWrapper(V4SaveCronRequest v4Request,
                                                            String operator,
                                                            boolean dryRun) {
        ServiceApprovalSaveCronRequest request = new ServiceApprovalSaveCronRequest();
        request.setRequest(v4Request);
        request.setOperator(operator);
        request.setAppCode("bk_ai");
        request.setDryRun(dryRun);
        return request;
    }

    private V4SaveCronRequest baseCreateRequest() {
        V4SaveCronRequest request = new V4SaveCronRequest();
        request.setScopeType("biz");
        request.setScopeId("2");
        request.setName("cron-1");
        request.setPlanId(100L);
        // v4 只收 5 段 Linux cron，带 ? 的 Quartz 写法会被 CronCheckUtil 拒掉
        request.setCronExpression("0/5 * * * *");
        return request;
    }

    private V4UpdateCronStatusRequest baseStatusRequest() {
        V4UpdateCronStatusRequest request = new V4UpdateCronStatusRequest();
        request.setScopeType("biz");
        request.setScopeId("2");
        request.setId(CRON_ID);
        request.setStatus(1);
        return request;
    }

    private CronJobInfoDTO buildCronJobInfo() {
        CronJobInfoDTO cronJobInfo = new CronJobInfoDTO();
        cronJobInfo.setId(CRON_ID);
        cronJobInfo.setAppId(APP_ID);
        cronJobInfo.setName("cron-1");
        cronJobInfo.setTaskPlanId(100L);
        cronJobInfo.setCronExpression("0/5 * * * *");
        return cronJobInfo;
    }

    private static String repeat(String value, int times) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < times; i++) {
            builder.append(value);
        }
        return builder.toString();
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
