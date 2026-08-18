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

import com.tencent.bk.job.analysis.approval.channel.model.ApprovalResult;
import com.tencent.bk.job.analysis.approval.consts.ApprovalChannelEnum;
import com.tencent.bk.job.analysis.approval.consts.ApprovalResultStatusEnum;
import com.tencent.bk.job.analysis.config.ApprovalProperties;
import com.tencent.bk.job.analysis.model.dto.ApprovalTaskDTO;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.util.http.HttpHelper;
import com.tencent.bk.job.common.util.http.HttpRequest;
import com.tencent.bk.job.common.util.http.HttpResponse;
import org.apache.http.Header;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;

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
 * 重点锁定三件事：只有 IMate 返回 APPROVED 才算通过（EXPIRED / CANCELED 一律不放行）、
 * 绑定证明原样取自响应而不是用请求参数自证、以及回查用的是 IMate 颁发的凭证而非蓝鲸凭证。
 */
class ImateApprovalChannelTest {

    private static final String TASK_ID = "3f8a9b1c2d3e4f5061728394a5b6c7d8";
    private static final String TICKET_ID = "IMATE-0001";
    private static final String ROOT_URL = "https://imate.example.com/ai/api";
    private static final String OPEN_API_APP_ID = "imate-issued-app-id";
    private static final String OPEN_API_SECRET = "imate-issued-secret";

    private HttpHelper httpHelper;
    private ApprovalProperties properties;
    private ImateApprovalChannel channel;

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
        channel = new ImateApprovalChannel(properties, httpHelper);
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

    private void mockResponse(String body) {
        when(httpHelper.requestForSuccessResp(any())).thenReturn(new HttpResponse(200, body, null));
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
            + "\"approvalContent\":\"## 脚本内容\\n echo hello\","
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
