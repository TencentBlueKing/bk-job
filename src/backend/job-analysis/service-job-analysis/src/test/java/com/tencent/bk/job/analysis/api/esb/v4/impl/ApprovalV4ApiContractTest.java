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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.job.analysis.api.esb.v4.OpenApiApprovalTaskV4Resource;
import com.tencent.bk.job.analysis.api.esb.v4.OpenApiApprovalTicketV4Resource;
import com.tencent.bk.job.analysis.api.esb.v4.OpenApiCreateJobPlanWithApprovalV4Resource;
import com.tencent.bk.job.analysis.api.esb.v4.OpenApiExecuteJobPlanWithApprovalV4Resource;
import com.tencent.bk.job.analysis.api.esb.v4.OpenApiFastExecuteScriptWithApprovalV4Resource;
import com.tencent.bk.job.analysis.api.esb.v4.OpenApiFastTransferFileWithApprovalV4Resource;
import com.tencent.bk.job.analysis.api.esb.v4.OpenApiSaveCronWithApprovalV4Resource;
import com.tencent.bk.job.analysis.api.esb.v4.OpenApiUpdateCronStatusWithApprovalV4Resource;
import com.tencent.bk.job.analysis.model.esb.v4.req.V4CancelApprovalTaskRequest;
import com.tencent.bk.job.analysis.model.esb.v4.req.V4CreateJobPlanWithApprovalRequest;
import com.tencent.bk.job.analysis.model.esb.v4.req.V4ExecuteJobPlanWithApprovalRequest;
import com.tencent.bk.job.analysis.model.esb.v4.req.V4FastExecuteScriptWithApprovalRequest;
import com.tencent.bk.job.analysis.model.esb.v4.req.V4FastTransferFileWithApprovalRequest;
import com.tencent.bk.job.analysis.model.esb.v4.req.V4RefreshApprovalTaskRequest;
import com.tencent.bk.job.analysis.model.esb.v4.req.V4SaveCronWithApprovalRequest;
import com.tencent.bk.job.analysis.model.esb.v4.req.V4UpdateCronStatusWithApprovalRequest;
import com.tencent.bk.job.analysis.model.esb.v4.req.V4WithApprovalRequest;
import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.job.common.validation.CheckEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 单元测试 - 审批相关 v4 接口的安全契约。
 * <p>
 * 这些断言守的都是"改错了不会编译失败、但会让整套审批机制失效"的地方：放行接口一旦多出审批结论字段，
 * 服务层的放行校验链就形同虚设；流转接口一旦写死 actionId，其余 5 种操作类型的审计事件会被 SDK 丢弃；
 * 应用态取单路径一旦带上 system 段，联调必然 404。用反射把它们钉住，比靠 Review 记住更可靠。
 */
class ApprovalV4ApiContractTest {

    /**
     * 审批结论相关的字段名特征。放行接口的请求体里出现任何一个都意味着"调用方可以自己声明审批结果"
     */
    private static final List<String> CONCLUSION_KEYWORDS = Arrays.asList(
        "status", "state", "approver", "approve", "approved", "conclusion", "opinion",
        "comment", "remark", "reason", "pass", "reject", "result", "creator", "operator",
        "username", "tenant", "expire", "channel"
    );

    @Test
    @DisplayName("放行接口只接受两个ID，不接受任何审批结论字段")
    void refreshRequestAcceptsOnlyTwoIds() {
        List<String> fieldNames = declaredFieldNames(V4RefreshApprovalTaskRequest.class);

        assertThat(fieldNames).containsExactlyInAnyOrder("approvalTaskId", "approvalTicketId");
        assertNoConclusionField(V4RefreshApprovalTaskRequest.class);
        // 请求体不得继承出额外字段：审批结论也不能从父类偷偷带进来
        assertThat(V4RefreshApprovalTaskRequest.class.getSuperclass()).isEqualTo(Object.class);
    }

    @Test
    @DisplayName("作废接口只接受审批任务ID")
    void cancelRequestAcceptsOnlyTaskId() {
        assertThat(declaredFieldNames(V4CancelApprovalTaskRequest.class)).containsExactly("approvalTaskId");
        assertNoConclusionField(V4CancelApprovalTaskRequest.class);
        assertThat(V4CancelApprovalTaskRequest.class.getSuperclass()).isEqualTo(Object.class);
    }

    @Test
    @DisplayName("流转接口的@AuditEntry不写死actionId，由operation_type在运行时决定")
    void workflowAuditEntryDoesNotHardcodeActionId() {
        List<Method> auditedMethods = Arrays.stream(OpenApiApprovalTaskV4ResourceImpl.class.getDeclaredMethods())
            .filter(method -> method.getAnnotation(AuditEntry.class) != null)
            .collect(Collectors.toList());

        assertThat(auditedMethods)
            .extracting(Method::getName)
            .containsExactlyInAnyOrder("refreshApprovalTask", "cancelApprovalTask");
        assertThat(auditedMethods).allSatisfy(method -> {
            AuditEntry auditEntry = method.getAnnotation(AuditEntry.class);
            assertThat(auditEntry.actionId()).isEmpty();
            assertThat(auditEntry.subActionIds()).isEmpty();
        });
    }

    @Test
    @DisplayName("应用态取单的后端路径不含system段，且不读取用户身份")
    void ticketResourceUsesSingleSegmentPath() {
        RequestMapping classMapping = OpenApiApprovalTicketV4Resource.class.getAnnotation(RequestMapping.class);
        assertThat(classMapping.value()).containsExactly("/esb/api/v4");

        Method method = findMethod(OpenApiApprovalTicketV4Resource.class, "getApprovalTicket");
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertThat(getMapping.value()).containsExactly("/get_approval_ticket");
        assertThat(classMapping.value()[0] + getMapping.value()[0]).doesNotContain("system");

        // 应用态调用没有 USERNAME 头，接口签名里出现它就意味着实现会去读不存在的用户身份
        assertThat(requestHeaderNames(method))
            .contains(JobCommonHeaders.APP_CODE, JobCommonHeaders.BK_TENANT_ID)
            .doesNotContain(JobCommonHeaders.USERNAME);
    }

    @Test
    @DisplayName("应用态取单强制要求租户头：多租户环境下缺失该头网关会直接401")
    void ticketResourceRequiresTenantHeader() {
        Method method = findMethod(OpenApiApprovalTicketV4Resource.class, "getApprovalTicket");
        RequestHeader tenantHeader = Arrays.stream(method.getParameterAnnotations())
            .flatMap(Arrays::stream)
            .filter(annotation -> annotation instanceof RequestHeader)
            .map(annotation -> (RequestHeader) annotation)
            .filter(header -> JobCommonHeaders.BK_TENANT_ID.equals(header.value()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("tenant header not declared"));

        assertThat(tenantHeader.required()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(classes = {
        V4FastExecuteScriptWithApprovalRequest.class,
        V4FastTransferFileWithApprovalRequest.class,
        V4ExecuteJobPlanWithApprovalRequest.class,
        V4CreateJobPlanWithApprovalRequest.class,
        V4SaveCronWithApprovalRequest.class,
        V4UpdateCronStatusWithApprovalRequest.class
    })
    @DisplayName("发起接口复用原请求体，只多一个枚举校验的approval_channel")
    void initiateRequestOnlyAddsApprovalChannel(Class<?> requestClass) throws NoSuchFieldException {
        assertThat(declaredFieldNames(requestClass)).containsExactly("approvalChannel");
        assertThat(V4WithApprovalRequest.class).isAssignableFrom(requestClass);
        // 原请求体是父类，字段与校验规则因此只有一份定义
        assertThat(requestClass.getSuperclass().getSimpleName())
            .isEqualTo(requestClass.getSimpleName().replace("WithApproval", ""));

        Field field = requestClass.getDeclaredField("approvalChannel");
        assertThat(field.getAnnotation(JsonProperty.class).value()).isEqualTo("approval_channel");
        // 渠道只能选枚举：允许传地址就等于让调用方指定"谁来审批"
        assertThat(field.getAnnotation(CheckEnum.class)).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(classes = {
        OpenApiFastExecuteScriptWithApprovalV4Resource.class,
        OpenApiFastTransferFileWithApprovalV4Resource.class,
        OpenApiExecuteJobPlanWithApprovalV4Resource.class,
        OpenApiCreateJobPlanWithApprovalV4Resource.class,
        OpenApiSaveCronWithApprovalV4Resource.class,
        OpenApiUpdateCronStatusWithApprovalV4Resource.class,
        OpenApiApprovalTaskV4Resource.class
    })
    @DisplayName("用户态接口都挂在/esb/api/v4下，且都带用户身份头")
    void userModeResourcesDeclareUserHeader(Class<?> resourceClass) {
        assertThat(resourceClass.getAnnotation(RequestMapping.class).value()).containsExactly("/esb/api/v4");
        assertThat(Arrays.stream(resourceClass.getDeclaredMethods()).collect(Collectors.toList()))
            .allSatisfy(method -> assertThat(requestHeaderNames(method))
                .contains(JobCommonHeaders.USERNAME, JobCommonHeaders.APP_CODE));
    }

    private void assertNoConclusionField(Class<?> requestClass) {
        for (String fieldName : declaredFieldNames(requestClass)) {
            String normalized = fieldName.toLowerCase(Locale.ROOT);
            boolean isTaskOrTicketId = "approvaltaskid".equals(normalized) || "approvalticketid".equals(normalized);
            if (isTaskOrTicketId) {
                continue;
            }
            assertThat(CONCLUSION_KEYWORDS)
                .withFailMessage("字段 %s 疑似审批结论字段，审批结论只能由作业平台回查渠道得出", fieldName)
                .noneMatch(normalized::contains);
        }
    }

    private List<String> declaredFieldNames(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
            .filter(field -> !field.isSynthetic())
            .filter(field -> !Modifier.isStatic(field.getModifiers()))
            .map(Field::getName)
            .collect(Collectors.toList());
    }

    private List<String> requestHeaderNames(Method method) {
        return Arrays.stream(method.getParameterAnnotations())
            .flatMap(Arrays::stream)
            .filter(annotation -> annotation instanceof RequestHeader)
            .map(annotation -> ((RequestHeader) annotation).value())
            .collect(Collectors.toList());
    }

    private Method findMethod(Class<?> clazz, String name) {
        return Arrays.stream(clazz.getDeclaredMethods())
            .filter(method -> method.getName().equals(name))
            .findFirst()
            .orElseThrow(() -> new AssertionError("method not found: " + name));
    }
}
