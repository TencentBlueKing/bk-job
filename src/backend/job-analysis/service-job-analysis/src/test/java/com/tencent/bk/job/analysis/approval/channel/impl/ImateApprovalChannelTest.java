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

package com.tencent.bk.job.analysis.approval.channel.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tencent.bk.job.analysis.approval.channel.model.ApprovalResult;
import com.tencent.bk.job.analysis.approval.consts.ApprovalChannelEnum;
import com.tencent.bk.job.analysis.approval.consts.ApprovalResultStatusEnum;
import com.tencent.bk.job.analysis.config.ApprovalProperties;
import com.tencent.bk.job.analysis.model.dto.ApprovalTaskDTO;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.util.http.HttpHelper;
import com.tencent.bk.job.common.util.http.HttpRequest;
import com.tencent.bk.job.common.util.http.HttpResponse;
import org.apache.http.Header;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 单元测试 - IMate 审批渠道真实实现。
 * <p>
 * 重点锁定四件事：只有 IMate 返回 APPROVED 才算通过（EXPIRED / CANCELED 一律不放行）、
 * 绑定证明原样取自响应而不是用请求参数自证、回查用的是 IMate 颁发的凭证而非蓝鲸凭证，
 * 以及<b>日志既够对账又不泄密</b>——审批正文里的脚本明文与 x-secret 绝不落盘。
 */
class ImateApprovalChannelTest {

    private static final String TASK_ID = "3f8a9b1c2d3e4f5061728394a5b6c7d8";
    private static final String TICKET_ID = "IMATE-0001";
    private static final String ROOT_URL = "https://imate.example.com/ai/api";
    private static final String OPEN_API_APP_ID = "imate-issued-app-id";
    private static final String OPEN_API_SECRET = "imate-issued-secret";

    /**
     * 审批正文里的脚本明文，出现在任何一条日志里都是泄露
     */
    private static final String SCRIPT_IN_APPROVAL_CONTENT = "rm -rf /data/should-never-appear-in-log";

    private HttpHelper httpHelper;
    private ApprovalProperties properties;
    private ImateApprovalChannel channel;
    private Logger channelLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        httpHelper = mock(HttpHelper.class);
        properties = new ApprovalProperties();
        ApprovalProperties.ImateConfig imate = properties.getChannels().getImate();
        imate.setUrl(ROOT_URL);
        // 蓝鲸侧的 appCode，只用于校验来取审批内容的调用方，不应出现在回查请求里
        imate.setAppCode("bk_imate");
        imate.getOpenApi().setAppId(OPEN_API_APP_ID);
        imate.getOpenApi().setSecret(OPEN_API_SECRET);
        channelLogger = (Logger) LoggerFactory.getLogger(ImateApprovalChannel.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        channelLogger.addAppender(logAppender);
        channel = new ImateApprovalChannel(properties, httpHelper);
    }

    @AfterEach
    void tearDown() {
        channelLogger.detachAppender(logAppender);
        logAppender.stop();
    }

    @Test
    @DisplayName("APPROVED 时返回通过，并带出审批人、审批时间与绑定证明")
    void givenApprovedThenReturnApproved() {
        mockResponse(buildDetailJson(TASK_ID, "APPROVED", "bob", "同意", "2026-08-17T15:30:20"));

        ApprovalResult result = channel.queryResult(buildTask(), TICKET_ID);

        assertThat(result.getStatus()).isEqualTo(ApprovalResultStatusEnum.APPROVED);
        assertThat(result.getApprovalTaskId()).isEqualTo(TASK_ID);
        assertThat(result.getApprover()).isEqualTo("bob");
        assertThat(result.getComment()).isEqualTo("同意");
        assertThat(result.getApprovedAt()).isEqualTo(LocalDateTime.parse("2026-08-17T15:30:20")
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        assertThat(channel.getChannelType()).isEqualTo(ApprovalChannelEnum.IMATE);
    }

    @Test
    @DisplayName("PENDING 时保持待审批")
    void givenPendingThenReturnPending() {
        mockResponse(buildDetailJson(TASK_ID, "PENDING", null, null, null));

        ApprovalResult result = channel.queryResult(buildTask(), TICKET_ID);

        assertThat(result.getStatus()).isEqualTo(ApprovalResultStatusEnum.PENDING);
        assertThat(result.getApprovedAt()).isNull();
    }

    @Test
    @DisplayName("REJECTED 时返回拒绝，并带出审批意见")
    void givenRejectedThenReturnRejected() {
        mockResponse(buildDetailJson(TASK_ID, "REJECTED", "bob", "风险太大", "2026-08-17T15:30:20"));

        ApprovalResult result = channel.queryResult(buildTask(), TICKET_ID);

        assertThat(result.getStatus()).isEqualTo(ApprovalResultStatusEnum.REJECTED);
        assertThat(result.getComment()).isEqualTo("风险太大");
    }

    @Test
    @DisplayName("EXPIRED 落成终态拒绝，原始状态写进 comment 备查")
    void givenExpiredThenReturnRejectedWithOriginalStatus() {
        mockResponse(buildDetailJson(TASK_ID, "EXPIRED", null, null, null));

        ApprovalResult result = channel.queryResult(buildTask(), TICKET_ID);

        assertThat(result.getStatus()).isEqualTo(ApprovalResultStatusEnum.REJECTED);
        assertThat(result.getComment()).contains("EXPIRED");
    }

    @Test
    @DisplayName("CANCELED 同样落成终态拒绝，绝不按待审批处理")
    void givenCanceledThenReturnRejected() {
        mockResponse(buildDetailJson(TASK_ID, "CANCELED", null, "会话被中断", null));

        ApprovalResult result = channel.queryResult(buildTask(), TICKET_ID);

        assertThat(result.getStatus()).isEqualTo(ApprovalResultStatusEnum.REJECTED);
        assertThat(result.getComment()).contains("CANCELED").contains("会话被中断");
    }

    @Test
    @DisplayName("绑定证明原样取自响应：响应里是别的任务 ID 时不得替换成请求的任务 ID")
    void givenOtherTaskIdInResponseThenKeepItForBindingCheck() {
        mockResponse(buildDetailJson("another_task_id", "APPROVED", "bob", null, null));

        ApprovalResult result = channel.queryResult(buildTask(), TICKET_ID);

        // 交由上层比对，这里必须如实回传，否则绑定校验会被自证通过
        assertThat(result.getApprovalTaskId()).isEqualTo("another_task_id");
    }

    @Test
    @DisplayName("未知审批状态一律抛异常，不猜测也不默认待审批")
    void givenUnknownStatusThenThrow() {
        mockResponse(buildDetailJson(TASK_ID, "SOMETHING_NEW", null, null, null));

        assertThatThrownBy(() -> channel.queryResult(buildTask(), TICKET_ID))
            .isInstanceOf(InternalException.class)
            .hasMessageContaining("SOMETHING_NEW");
    }

    @Test
    @DisplayName("IMate 返回非 0 状态码时抛异常，由上层 fail-closed")
    void givenNonZeroStatusThenThrow() {
        mockResponse("{\"status\":2302081,\"message\":\"审批单不存在\"}");

        assertThatThrownBy(() -> channel.queryResult(buildTask(), TICKET_ID))
            .isInstanceOf(InternalException.class)
            .hasMessageContaining("2302081");
    }

    @Test
    @DisplayName("响应成功但数据为空时抛异常，绝不当成待审批")
    void givenEmptyDataThenThrow() {
        mockResponse("{\"status\":0,\"data\":null}");

        assertThatThrownBy(() -> channel.queryResult(buildTask(), TICKET_ID))
            .isInstanceOf(InternalException.class)
            .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("凭证未配置时直接失败，不发出必然被拒的请求")
    void givenCredentialNotConfiguredThenThrowWithoutRequest() {
        properties.getChannels().getImate().getOpenApi().setSecret(null);

        assertThatThrownBy(() -> channel.queryResult(buildTask(), TICKET_ID))
            .isInstanceOf(InternalException.class)
            .hasMessageContaining("credential");
        verify(httpHelper, never()).requestForSuccessResp(any());
    }

    @Test
    @DisplayName("回查以审批任务 ID 为 taskId，且只带 IMate 颁发的凭证")
    void thenRequestWithImateCredentialAndApprovalTaskId() {
        mockResponse(buildDetailJson(TASK_ID, "APPROVED", "bob", null, null));

        channel.queryResult(buildTask(), TICKET_ID);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpHelper).requestForSuccessResp(captor.capture());
        HttpRequest request = captor.getValue();
        assertThat(request.getUrl()).isEqualTo(ROOT_URL + "/open/approval/detail?taskId=" + TASK_ID);
        assertThat(headerValue(request, "x-app-id")).isEqualTo(OPEN_API_APP_ID);
        assertThat(headerValue(request, "x-secret")).isEqualTo(OPEN_API_SECRET);
        // 蓝鲸侧的 appCode 是入站方向用的，不能出现在回查请求里
        assertThat(Arrays.stream(request.getHeaders()).map(Header::getValue))
            .doesNotContain("bk_imate");
    }

    @Test
    @DisplayName("渠道地址结尾带斜杠时不拼出双斜杠")
    void givenRootUrlEndsWithSlashThenBuildValidUrl() {
        properties.getChannels().getImate().setUrl(ROOT_URL + "/");
        mockResponse(buildDetailJson(TASK_ID, "PENDING", null, null, null));

        channel.queryResult(buildTask(), TICKET_ID);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpHelper).requestForSuccessResp(captor.capture());
        assertThat(captor.getValue().getUrl()).isEqualTo(ROOT_URL + "/open/approval/detail?taskId=" + TASK_ID);
    }

    @Test
    @DisplayName("成功回查的日志够对账：请求、响应体、耗时都在，taskId 与状态可查")
    void givenSuccessThenLogRequestResponseAndCostTime() {
        mockResponse(buildDetailJson(TASK_ID, "APPROVED", "bob", "同意", "2026-08-17T15:30:20"));

        channel.queryResult(buildTask(), TICKET_ID);

        assertThat(logLine("Request|"))
            .contains("method=GET")
            .contains(ROOT_URL + "/open/approval/detail?taskId=" + TASK_ID)
            .contains("x-app-id=" + OPEN_API_APP_ID);
        assertThat(logLine("Response|"))
            .contains("success=true")
            .contains("costTime=")
            .contains("\"taskId\":\"" + TASK_ID + "\"")
            .contains("\"status\":\"APPROVED\"")
            .contains("\"approver\":\"bob\"")
            .contains("\"approveTime\":\"2026-08-17T15:30:20\"");
    }

    @Test
    @DisplayName("响应体照打，但审批正文换成长度摘要：approvalContent 含脚本明文，不能落盘")
    void givenApprovalContentInResponseThenMaskItInLog() {
        mockResponse(buildDetailJson(TASK_ID, "APPROVED", "bob", null, null));

        channel.queryResult(buildTask(), TICKET_ID);

        assertThat(allLogs())
            .as("审批正文里的脚本明文出现在任何一条日志里都是泄露")
            .noneMatch(line -> line.contains(SCRIPT_IN_APPROVAL_CONTENT));
        assertThat(logLine("Response|")).contains("\"approvalContent\":\"<masked,length=");
    }

    @Test
    @DisplayName("x-secret 是 IMate 颁发的凭证，任何一条日志里都不能出现它的取值")
    void thenNeverLogSecret() {
        mockResponse(buildDetailJson(TASK_ID, "APPROVED", "bob", null, null));

        channel.queryResult(buildTask(), TICKET_ID);

        assertThat(allLogs()).noneMatch(line -> line.contains(OPEN_API_SECRET));
        assertThat(logLine("Request|")).contains("x-secret=<masked>");
    }

    @Test
    @DisplayName("请求抛异常时同样有日志与耗时：超时最需要的就是耗时数据")
    void givenRequestExceptionThenLogCostTime() {
        when(httpHelper.requestForSuccessResp(any()))
            .thenThrow(new InternalException("read timed out", ErrorCode.INTERNAL_ERROR));

        assertThatThrownBy(() -> channel.queryResult(buildTask(), TICKET_ID))
            .isInstanceOf(InternalException.class);

        assertThat(logLine("Response|"))
            .contains("success=false")
            .contains("costTime=");
    }

    @Test
    @DisplayName("对端返回非 0 状态码时也有日志与耗时，且响应体里的正文仍是脱敏的")
    void givenErrorResponseThenLogCostTimeWithMaskedContent() {
        mockResponse("{\"status\":2302081,\"message\":\"审批单不存在\",\"data\":{\"approvalContent\":\""
            + SCRIPT_IN_APPROVAL_CONTENT + "\"}}");

        assertThatThrownBy(() -> channel.queryResult(buildTask(), TICKET_ID))
            .isInstanceOf(InternalException.class);

        assertThat(logLine("Response|"))
            .contains("success=false")
            .contains("costTime=")
            .contains("2302081");
        assertThat(allLogs()).noneMatch(line -> line.contains(SCRIPT_IN_APPROVAL_CONTENT));
    }

    @Test
    @DisplayName("返回结构与预期不符、取不出正文取值时整体只打长度，不赌里面没有脚本")
    void givenUnparsableApprovalContentThenMaskWholeBody() {
        mockResponse("{\"status\":0,\"data\":{\"approvalContent\":[\"" + SCRIPT_IN_APPROVAL_CONTENT + "\"]}}");

        assertThatThrownBy(() -> channel.queryResult(buildTask(), TICKET_ID))
            .isInstanceOf(Exception.class);

        assertThat(allLogs()).noneMatch(line -> line.contains(SCRIPT_IN_APPROVAL_CONTENT));
        assertThat(logLine("Response|")).contains("resp=<masked,length=");
    }

    private void mockResponse(String body) {
        when(httpHelper.requestForSuccessResp(any())).thenReturn(new HttpResponse(200, body, null));
    }

    private List<String> allLogs() {
        return logAppender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .collect(Collectors.toList());
    }

    private String logLine(String keyword) {
        return allLogs().stream()
            .filter(line -> line.contains(keyword))
            .findFirst()
            .orElseThrow(() -> new AssertionError("未找到包含 " + keyword + " 的日志，实际日志：" + allLogs()));
    }

    private String headerValue(HttpRequest request, String name) {
        return Arrays.stream(request.getHeaders())
            .filter(header -> name.equals(header.getName()))
            .map(Header::getValue)
            .findFirst()
            .orElse(null);
    }

    private String buildDetailJson(String taskId,
                                   String status,
                                   String approver,
                                   String approveComment,
                                   String approveTime) {
        return "{\"status\":0,\"data\":{"
            + "\"taskId\":\"" + taskId + "\","
            + "\"saasId\":\"" + OPEN_API_APP_ID + "\","
            + "\"title\":\"关于执行脚本的审批\","
            + "\"approvalContent\":\"## 脚本内容\\n " + SCRIPT_IN_APPROVAL_CONTENT + "\","
            + "\"status\":\"" + status + "\","
            + jsonField("approver", approver)
            + jsonField("approveComment", approveComment)
            + jsonField("approveTime", approveTime)
            + "\"createTime\":\"2026-08-17T15:00:00\"}}";
    }

    private String jsonField(String name, String value) {
        return value == null ? "" : "\"" + name + "\":\"" + value + "\",";
    }

    private ApprovalTaskDTO buildTask() {
        ApprovalTaskDTO task = new ApprovalTaskDTO();
        task.setApprovalTaskId(TASK_ID);
        task.setCreator("admin");
        task.setApprovalChannel(ApprovalChannelEnum.IMATE.name());
        return task;
    }
}
