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

package com.tencent.bk.job.analysis.api.esb.v4.impl;

import com.tencent.bk.job.analysis.approval.consts.ApprovalChannelEnum;
import com.tencent.bk.job.analysis.model.esb.v4.req.V4CreateJobPlanWithApprovalRequest;
import com.tencent.bk.job.analysis.model.esb.v4.req.V4ExecuteJobPlanWithApprovalRequest;
import com.tencent.bk.job.analysis.model.esb.v4.req.V4FastExecuteScriptWithApprovalRequest;
import com.tencent.bk.job.analysis.model.esb.v4.req.V4FastTransferFileWithApprovalRequest;
import com.tencent.bk.job.analysis.model.esb.v4.req.V4SaveCronWithApprovalRequest;
import com.tencent.bk.job.analysis.model.esb.v4.req.V4UpdateCronStatusWithApprovalRequest;
import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.toggle.feature.FeatureManager;
import com.tencent.bk.job.crontab.model.esb.v4.req.V4SaveCronRequest;
import com.tencent.bk.job.crontab.model.esb.v4.req.V4UpdateCronStatusRequest;
import com.tencent.bk.job.execute.model.esb.v4.req.V4ExecuteJobPlanRequest;
import com.tencent.bk.job.execute.model.esb.v4.req.V4FastExecuteScriptRequest;
import com.tencent.bk.job.execute.model.esb.v4.req.V4FastTransferFileRequest;
import com.tencent.bk.job.manage.model.esb.v4.req.V4CreateJobPlanRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 单元测试 - 带审批请求体的分组校验继承。
 * <p>
 * Hibernate 的 {@code @GroupSequenceProvider} 没有 {@code @Inherited}，"子类会不会丢掉父类的分组校验"
 * 不是看一眼代码就能确定的事。资源范围的必填校验全都挂在 {@code UseScopeParam} / {@code UseBkBizIdParam}
 * 分组上，分组序列一丢这些校验就静默失效，空 scope 会一路走到审批内容里。
 * <p>
 * 这里跑真实的 Validator，逐个断言子类与原请求体在资源范围字段上产生<b>完全相同的违反项</b>。
 * 只校验单个属性而非整个对象：部分请求体上的约束校验器（如回调地址校验）需要 Spring 注入，
 * 整体校验在纯单测环境里起不来。
 */
class ApprovalV4RequestValidationTest {

    /**
     * 资源范围相关属性。它们的约束都受分组序列控制，因此最能反映分组序列有没有丢
     */
    private static final List<String> SCOPE_PROPERTIES = Arrays.asList("scopeType", "scopeId");

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        // EsbAppScopeReq 的分组序列提供者会查特性开关，纯单测里得先把静态容器喂上
        FeatureManager featureManager = mock(FeatureManager.class);
        when(featureManager.checkFeature(anyString(), any())).thenReturn(false);
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBean(FeatureManager.class)).thenReturn(featureManager);
        new ApplicationContextRegister().setApplicationContext(applicationContext);

        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
    }

    static Stream<Arguments> requestPairs() {
        return Stream.of(
            Arguments.of("fast_execute_script",
                (Supplier<EsbAppScopeReq>) V4FastExecuteScriptRequest::new,
                (Supplier<EsbAppScopeReq>) V4FastExecuteScriptWithApprovalRequest::new),
            Arguments.of("fast_transfer_file",
                (Supplier<EsbAppScopeReq>) V4FastTransferFileRequest::new,
                (Supplier<EsbAppScopeReq>) V4FastTransferFileWithApprovalRequest::new),
            Arguments.of("execute_job_plan",
                (Supplier<EsbAppScopeReq>) V4ExecuteJobPlanRequest::new,
                (Supplier<EsbAppScopeReq>) V4ExecuteJobPlanWithApprovalRequest::new),
            Arguments.of("create_job_plan",
                (Supplier<EsbAppScopeReq>) V4CreateJobPlanRequest::new,
                (Supplier<EsbAppScopeReq>) V4CreateJobPlanWithApprovalRequest::new),
            Arguments.of("save_cron",
                (Supplier<EsbAppScopeReq>) V4SaveCronRequest::new,
                (Supplier<EsbAppScopeReq>) V4SaveCronWithApprovalRequest::new),
            Arguments.of("update_cron_status",
                (Supplier<EsbAppScopeReq>) V4UpdateCronStatusRequest::new,
                (Supplier<EsbAppScopeReq>) V4UpdateCronStatusWithApprovalRequest::new)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("requestPairs")
    @DisplayName("缺资源范围时，带审批请求体与原请求体报出完全相同的违反项")
    void withApprovalRequestKeepsScopeConstraints(String name,
                                                  Supplier<EsbAppScopeReq> parentSupplier,
                                                  Supplier<EsbAppScopeReq> childSupplier) {
        Set<String> parentViolations = scopeViolationsOf(parentSupplier.get());
        Set<String> childViolations = scopeViolationsOf(childSupplier.get());

        assertThat(childViolations).isEqualTo(parentViolations);
    }

    @Test
    @DisplayName("未声明分组序列的请求体：资源范围必填校验在子类上照常报错")
    void inheritedGroupSequenceStillReportsMissingScope() {
        Set<String> violations = scopeViolationsOf(new V4FastTransferFileWithApprovalRequest());

        // 非空是必要前提：否则父子类"都为空"也能让上面的相等断言通过，测试就成了空转
        assertThat(violations).isNotEmpty();
        assertThat(violations).isEqualTo(scopeViolationsOf(new V4FastTransferFileRequest()));
    }

    @Test
    @DisplayName("自带分组序列的请求体：子类重新声明后，脚本与账号的动态分组校验照常生效")
    void redeclaredGroupSequenceKeepsDynamicGroups() {
        List<String> dynamicGroupProperties = Arrays.asList("content", "accountId");

        Set<String> childViolations = violationsOf(new V4FastExecuteScriptWithApprovalRequest(),
            dynamicGroupProperties);

        assertThat(childViolations).isNotEmpty();
        assertThat(childViolations).isEqualTo(violationsOf(new V4FastExecuteScriptRequest(), dynamicGroupProperties));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("requestPairs")
    @DisplayName("补全资源范围后不再报违反项")
    void scopeFilledRequestPassesScopeValidation(String name,
                                                 Supplier<EsbAppScopeReq> parentSupplier,
                                                 Supplier<EsbAppScopeReq> childSupplier) {
        EsbAppScopeReq child = childSupplier.get();
        child.setScopeType("biz");
        child.setScopeId("2");

        assertThat(scopeViolationsOf(child)).isEmpty();
    }

    @Test
    @DisplayName("非法审批渠道被拒；合法渠道与不传都放行")
    void approvalChannelIsValidatedAsEnum() {
        V4FastExecuteScriptWithApprovalRequest request = new V4FastExecuteScriptWithApprovalRequest();
        request.setScopeType("biz");
        request.setScopeId("2");

        request.setApprovalChannel("NOT_A_CHANNEL");
        assertThat(violationsOf(request, "approvalChannel"))
            .containsExactly("approvalChannel|{validation.constraints.ApprovalChannel_illegal.message}");

        request.setApprovalChannel(ApprovalChannelEnum.IMATE.name());
        assertThat(violationsOf(request, "approvalChannel")).isEmpty();

        // 不传渠道走服务端默认渠道，不是校验错误
        request.setApprovalChannel(null);
        assertThat(violationsOf(request, "approvalChannel")).isEmpty();
    }

    private Set<String> scopeViolationsOf(Object request) {
        return violationsOf(request, SCOPE_PROPERTIES);
    }

    private Set<String> violationsOf(Object request, List<String> properties) {
        return properties.stream()
            .flatMap(property -> violationsOf(request, property).stream())
            .collect(Collectors.toSet());
    }

    /**
     * 违反项归一化成"属性路径|消息模板"，便于逐项比对父子类的校验结果
     */
    private Set<String> violationsOf(Object request, String property) {
        return validator.validateProperty(request, property).stream()
            .map(this::describe)
            .collect(Collectors.toSet());
    }

    private String describe(ConstraintViolation<Object> violation) {
        return violation.getPropertyPath() + "|" + violation.getMessageTemplate();
    }
}
