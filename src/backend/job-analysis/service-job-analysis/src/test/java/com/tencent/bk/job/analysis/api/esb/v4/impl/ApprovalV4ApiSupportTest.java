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

import com.tencent.bk.job.analysis.approval.ApprovalTaskService;
import com.tencent.bk.job.analysis.approval.channel.model.ApprovalTicket;
import com.tencent.bk.job.analysis.approval.consts.ApprovalChannelEnum;
import com.tencent.bk.job.analysis.approval.consts.ApprovalOperationTypeEnum;
import com.tencent.bk.job.analysis.approval.consts.ApprovalStatusEnum;
import com.tencent.bk.job.analysis.approval.model.ApprovalCallerContext;
import com.tencent.bk.job.analysis.model.dto.ApprovalTaskDTO;
import com.tencent.bk.job.analysis.model.esb.v4.req.V4FastExecuteScriptWithApprovalRequest;
import com.tencent.bk.job.analysis.model.esb.v4.resp.V4ApprovalTaskCreatedDTO;
import com.tencent.bk.job.analysis.model.esb.v4.resp.V4ApprovalTaskDTO;
import com.tencent.bk.job.analysis.model.esb.v4.resp.V4ApprovalTicketDTO;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.JobContextUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 单元测试 - 审批 v4 接口的公共装配逻辑。
 * <p>
 * 重点在两处容易悄悄错的地方：发起接口必须把本次请求解析出的 appId 带进调用上下文（否则跨业务发起也能过归属校验），
 * 流转接口必须<b>不</b>带 appId（它的请求体没有资源范围可解析，硬填只会把所有流转请求判成"任务不存在"）。
 */
class ApprovalV4ApiSupportTest {

    private static final String TENANT_ID = "tenant-a";
    private static final String USERNAME = "admin";
    private static final String APP_CODE = "bk-imate";

    private AppScopeMappingService appScopeMappingService;
    private ApprovalTaskService approvalTaskService;
    private ApprovalV4ApiSupport support;

    @BeforeEach
    void setUp() {
        appScopeMappingService = mock(AppScopeMappingService.class);
        approvalTaskService = mock(ApprovalTaskService.class);
        MessageI18nService i18nService = mock(MessageI18nService.class);
        when(i18nService.getI18n(anyString())).thenAnswer(invocation -> "i18n:" + invocation.getArgument(0));
        support = new ApprovalV4ApiSupport(appScopeMappingService, approvalTaskService, i18nService);
        JobContextUtil.setUser(new User(TENANT_ID, USERNAME, USERNAME));
    }

    @AfterEach
    void tearDown() {
        JobContextUtil.unsetContext();
    }

    @Test
    @DisplayName("发起审批：解析资源范围后把appId带进调用上下文，渠道只按枚举路由")
    void initiateFillsScopeAndCaller() {
        when(appScopeMappingService.getAppIdByScope(ResourceScopeTypeEnum.BIZ.getValue(), "2")).thenReturn(2L);
        ApprovalTaskDTO created = new ApprovalTaskDTO();
        created.setApprovalTaskId("task-1");
        created.setTenantId(TENANT_ID);
        created.setStatus(ApprovalStatusEnum.PENDING.name());
        created.setApprovalChannel(ApprovalChannelEnum.IMATE.name());
        created.setExpireAt(1000L);
        when(approvalTaskService.create(any(), any(), any(), any())).thenReturn(created);

        V4FastExecuteScriptWithApprovalRequest request = new V4FastExecuteScriptWithApprovalRequest();
        request.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
        request.setScopeId("2");
        request.setApprovalChannel(ApprovalChannelEnum.IMATE.name());

        EsbV4Response<V4ApprovalTaskCreatedDTO> response =
            support.initiate(ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT, request, APP_CODE);

        ArgumentCaptor<ApprovalCallerContext> callerCaptor = ArgumentCaptor.forClass(ApprovalCallerContext.class);
        ArgumentCaptor<ApprovalChannelEnum> channelCaptor = ArgumentCaptor.forClass(ApprovalChannelEnum.class);
        verify(approvalTaskService).create(
            eq(ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT),
            same(request),
            channelCaptor.capture(),
            callerCaptor.capture()
        );
        assertThat(channelCaptor.getValue()).isEqualTo(ApprovalChannelEnum.IMATE);
        ApprovalCallerContext caller = callerCaptor.getValue();
        assertThat(caller.getAppId()).isEqualTo(2L);
        assertThat(caller.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(caller.getUsername()).isEqualTo(USERNAME);
        assertThat(caller.getAppCode()).isEqualTo(APP_CODE);

        V4ApprovalTaskCreatedDTO data = response.getData();
        // 渠道取单要带租户头，发起返回体必须给出 tenant_id
        assertThat(data.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(data.getApprovalTaskId()).isEqualTo("task-1");
        assertThat(data.getStatus()).isEqualTo(ApprovalStatusEnum.PENDING.name());
        assertThat(data.getExpireAt()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("流转接口的调用上下文不带appId，归属由租户/发起人/应用编码比对")
    void workflowCallerCarriesNoAppId() {
        ApprovalCallerContext caller = support.workflowCaller(APP_CODE);

        assertThat(caller.getAppId()).isNull();
        assertThat(caller.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(caller.getUsername()).isEqualTo(USERNAME);
        assertThat(caller.getAppCode()).isEqualTo(APP_CODE);
    }

    @Test
    @DisplayName("已下发但无执行结果时标记result_unknown，提示用户去执行历史确认")
    void dispatchedWithoutResultIsUnknown() {
        ApprovalTaskDTO task = executingTask();
        task.setDispatchedAt(2000L);

        V4ApprovalTaskDTO taskDTO = support.toTaskDTO(task);

        assertThat(taskDTO.getResultUnknown()).isTrue();
        assertThat(taskDTO.getExecuteResult()).isNull();
        assertThat(taskDTO.getMessage()).isEqualTo("i18n:task.approval.message.resultUnknown");
    }

    @Test
    @DisplayName("尚未下发就停在EXECUTING的任务不算结果未知：作业确定没产生")
    void notDispatchedIsNotUnknown() {
        ApprovalTaskDTO task = executingTask();

        V4ApprovalTaskDTO taskDTO = support.toTaskDTO(task);

        assertThat(taskDTO.getResultUnknown()).isFalse();
        assertThat(taskDTO.getMessage()).isEqualTo("i18n:task.approval.message.notDispatched");
    }

    @Test
    @DisplayName("执行结果以对象返回，与直接执行接口的data结构一致")
    void executeResultIsReturnedAsObject() {
        ApprovalTaskDTO task = executingTask();
        task.setStatus(ApprovalStatusEnum.EXECUTED.name());
        task.setDispatchedAt(2000L);
        task.setExecuteResult("{\"job_instance_id\":100}");
        task.setApprover("approver-1");
        task.setApprovedAt(3000L);

        V4ApprovalTaskDTO taskDTO = support.toTaskDTO(task);

        assertThat(taskDTO.getResultUnknown()).isFalse();
        assertThat(taskDTO.getExecuteResult()).isInstanceOf(Map.class);
        Map<String, Object> executeResult = castToMap(taskDTO.getExecuteResult());
        assertThat(executeResult).containsEntry("job_instance_id", 100);
        assertThat(taskDTO.getApprover()).isEqualTo("approver-1");
        assertThat(taskDTO.getApprovedAt()).isEqualTo(3000L);
        assertThat(taskDTO.getMessage()).isEqualTo("i18n:task.approval.message.executed");
    }

    @Test
    @DisplayName("执行结果无法解析时降级为null，不让流转接口整体报错")
    void brokenExecuteResultDegradesToNull() {
        ApprovalTaskDTO task = executingTask();
        task.setStatus(ApprovalStatusEnum.EXECUTED.name());
        task.setExecuteResult("not-a-json");

        assertThat(support.toTaskDTO(task).getExecuteResult()).isNull();
    }

    @Test
    @DisplayName("取单返回体保留敏感与高危标记，供渠道按自身能力渲染")
    void ticketKeepsFieldFlags() {
        ApprovalTicket ticket = new ApprovalTicket();
        ticket.setApprovalTaskId("task-1");
        ticket.setTitle("快速执行脚本");
        ticket.setRiskLevel("HIGH");
        ticket.setOperationType(ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT.name());
        ticket.setScope(new ResourceScope(ResourceScopeTypeEnum.BIZ, "2"));
        ticket.setCreator(USERNAME);
        ticket.setExpireAt(1000L);
        ApprovalTicket.Section section = ticket.addSection(ApprovalTicket.SECTION_SUMMARY, "操作概要", false);
        section.addField(ApprovalTicket.Field.highlighted("账号", "root"));
        section.addField(ApprovalTicket.Field.sensitive("主机账号密码", "******"));

        V4ApprovalTicketDTO ticketDTO = support.toTicketDTO(ticket);

        assertThat(ticketDTO.getApprovalTaskId()).isEqualTo("task-1");
        assertThat(ticketDTO.getScopeType()).isEqualTo(ResourceScopeTypeEnum.BIZ.getValue());
        assertThat(ticketDTO.getScopeId()).isEqualTo("2");
        assertThat(ticketDTO.getSections()).hasSize(1);
        V4ApprovalTicketDTO.Section sectionDTO = ticketDTO.getSections().get(0);
        assertThat(sectionDTO.getKey()).isEqualTo(ApprovalTicket.SECTION_SUMMARY);
        assertThat(sectionDTO.getCollapsed()).isFalse();
        assertThat(sectionDTO.getFields()).hasSize(2);
        assertThat(sectionDTO.getFields().get(0).getHighlight()).isTrue();
        V4ApprovalTicketDTO.Field sensitiveField = sectionDTO.getFields().get(1);
        assertThat(sensitiveField.getSensitive()).isTrue();
        // 敏感字段只允许是占位符：单据里绝不能出现明文或密文
        assertThat(sensitiveField.getValue()).doesNotContain("root");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castToMap(Object value) {
        return (Map<String, Object>) value;
    }

    private ApprovalTaskDTO executingTask() {
        ApprovalTaskDTO task = new ApprovalTaskDTO();
        task.setApprovalTaskId("task-1");
        task.setTenantId(TENANT_ID);
        task.setStatus(ApprovalStatusEnum.EXECUTING.name());
        task.setOperationType(ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT.name());
        task.setApprovalChannel(ApprovalChannelEnum.IMATE.name());
        task.setApprovalTicketId("ticket-1");
        task.setCreator(USERNAME);
        task.setCreateTime(1L);
        task.setExpireAt(1000L);
        return task;
    }
}
