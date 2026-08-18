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

package com.tencent.bk.job.analysis.approval.impl;

import com.tencent.bk.job.analysis.approval.ApprovalAuditor;
import com.tencent.bk.job.analysis.approval.ApprovalContentRenderer;
import com.tencent.bk.job.analysis.approval.ApprovalMetrics;
import com.tencent.bk.job.analysis.approval.channel.ApprovalChannel;
import com.tencent.bk.job.analysis.approval.channel.ApprovalChannelRegistry;
import com.tencent.bk.job.analysis.approval.channel.model.ApprovalContent;
import com.tencent.bk.job.analysis.approval.channel.model.ApprovalResult;
import com.tencent.bk.job.analysis.approval.consts.ApprovalChannelEnum;
import com.tencent.bk.job.analysis.approval.consts.ApprovalOperationTypeEnum;
import com.tencent.bk.job.analysis.approval.consts.ApprovalResultStatusEnum;
import com.tencent.bk.job.analysis.approval.consts.ApprovalStatusEnum;
import com.tencent.bk.job.analysis.approval.executor.OperationExecutor;
import com.tencent.bk.job.analysis.approval.executor.OperationExecutorRegistry;
import com.tencent.bk.job.analysis.approval.model.ApprovalCallerContext;
import com.tencent.bk.job.analysis.config.ApprovalProperties;
import com.tencent.bk.job.analysis.dao.ApprovalTaskDAO;
import com.tencent.bk.job.analysis.model.dto.ApprovalTaskDTO;
import com.tencent.bk.job.common.api.model.DryRunResult;
import com.tencent.bk.job.common.model.ResolvedSummary;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.crontab.model.inner.request.ServiceApprovalSaveCronRequest;
import com.tencent.bk.job.crontab.model.inner.request.ServiceApprovalUpdateCronStatusRequest;
import com.tencent.bk.job.execute.model.esb.v4.req.V4FastExecuteScriptRequest;
import com.tencent.bk.job.execute.model.inner.request.ServiceApprovalExecuteJobPlanRequest;
import com.tencent.bk.job.execute.model.inner.request.ServiceApprovalFastTransferFileRequest;
import com.tencent.bk.job.manage.model.esb.v4.req.V4CreateJobPlanRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 单元测试 - 审批任务核心编排。
 * <p>
 * 本类的重点全部落在"破了就等于没做"的性质上：放行校验链的每一步、CAS 之前不写终态、
 * 回查失败 fail-closed、放行执行的 dryRun/operator 取值。
 */
class ApprovalTaskServiceImplTest {

    private static final String TASK_ID = "e2a1c0d4111122223333444455556666";
    private static final String TICKET_ID = "IMATE-20260811-0001";
    private static final String CREATOR = "admin";
    private static final String APP_CODE = "bk_ai";
    private static final String TENANT_ID = "default";
    private static final Long APP_ID = 2L;

    private static final String CHANNEL_APP_CODE = "bk_imate";

    private ApprovalTaskDAO approvalTaskDAO;
    private ApprovalChannelRegistry channelRegistry;
    private OperationExecutorRegistry executorRegistry;
    private ApprovalParamsCryptoServiceStub paramsCryptoService;
    private ApprovalChannel approvalChannel;
    private RecordingOperationExecutor executor;
    private ApprovalProperties approvalProperties;
    private ApprovalContentRenderer contentRenderer;
    private MeterRegistry meterRegistry;
    private ApprovalAuditor approvalAuditor;
    private ApprovalTaskServiceImpl approvalTaskService;

    @BeforeEach
    void setUp() {
        approvalTaskDAO = mock(ApprovalTaskDAO.class);
        channelRegistry = mock(ApprovalChannelRegistry.class);
        executorRegistry = mock(OperationExecutorRegistry.class);
        approvalChannel = mock(ApprovalChannel.class);
        paramsCryptoService = new ApprovalParamsCryptoServiceStub();
        executor = new RecordingOperationExecutor();
        approvalProperties = new ApprovalProperties();
        contentRenderer = mock(ApprovalContentRenderer.class);
        meterRegistry = new SimpleMeterRegistry();
        approvalAuditor = mock(ApprovalAuditor.class);

        when(channelRegistry.getChannelByName(anyString())).thenReturn(approvalChannel);
        when(channelRegistry.getChannel(any())).thenReturn(approvalChannel);
        when(channelRegistry.getChannelAppCodes(anyString())).thenReturn(Collections.singleton(CHANNEL_APP_CODE));
        // getExecutor 返回带通配符的泛型，用 doReturn 绕开捕获类型不可赋值的限制
        doReturn(executor).when(executorRegistry).getExecutor(any());

        approvalTaskService = new ApprovalTaskServiceImpl(
            approvalTaskDAO, channelRegistry, executorRegistry, paramsCryptoService, approvalProperties,
            contentRenderer, new ApprovalMetrics(meterRegistry, approvalProperties), approvalAuditor);
    }

    @Nested
    @DisplayName("创建审批任务")
    class CreateTest {

        @Test
        @DisplayName("先以 dryRun=true 预检，通过后才把加密快照与概要落库")
        void givenValidRequestThenPersistEncryptedSnapshot() {
            ResolvedSummary summary = new ResolvedSummary();
            summary.setName("test-script-task");
            executor.result = DryRunResult.valid(summary, null);

            long before = System.currentTimeMillis();
            ApprovalTaskDTO task = approvalTaskService.create(
                ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT, buildParams(), ApprovalChannelEnum.IMATE, caller());

            assertThat(executor.dryRunFlags).containsExactly(true);
            // 32 位无连字符 UUID：便于复制粘贴
            assertThat(task.getApprovalTaskId()).isNotBlank().hasSize(32).doesNotContain("-");
            assertThat(task.getStatus()).isEqualTo(ApprovalStatusEnum.PENDING.name());
            assertThat(task.getCreator()).isEqualTo(CREATOR);
            assertThat(task.getApprovalChannel()).isEqualTo(ApprovalChannelEnum.IMATE.name());
            // 固定 8 小时 TTL
            assertThat(task.getExpireAt() - task.getCreateTime()).isEqualTo(TimeUnit.HOURS.toMillis(8));
            assertThat(task.getCreateTime()).isGreaterThanOrEqualTo(before);
            // 落库的参数快照必须是经过加密服务处理后的内容（真实加密效果见 ApprovalParamsCryptoServiceImplTest）
            assertThat(task.getOperationParams()).startsWith(ApprovalParamsCryptoServiceStub.PREFIX);
            // operationType 由 job-analysis 侧填充
            ResolvedSummary stored = JsonUtils.fromJson(task.getResolvedSummary(), ResolvedSummary.class);
            assertThat(stored.getOperationType())
                .isEqualTo(ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT.name());
            assertThat(stored.getName()).isEqualTo("test-script-task");
            verify(approvalTaskDAO).insertApprovalTask(task);
        }

        @Test
        @DisplayName("预检不通过时抛出下游错误且不落库")
        void givenInvalidRequestThenRejectWithoutPersist() {
            executor.result = DryRunResult.invalid(ErrorCode.SCRIPT_NOT_EXIST, null);

            assertThatThrownBy(() -> approvalTaskService.create(
                ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT, buildParams(), null, caller()))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getErrorCode())
                    .isEqualTo(ErrorCode.SCRIPT_NOT_EXIST));

            verify(approvalTaskDAO, never()).insertApprovalTask(any());
        }

        @Test
        @DisplayName("未指定渠道时使用服务端默认渠道")
        void givenNoChannelThenUseDefaultChannel() {
            executor.result = DryRunResult.valid(new ResolvedSummary(), null);

            ApprovalTaskDTO task = approvalTaskService.create(
                ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT, buildParams(), null, caller());

            assertThat(task.getApprovalChannel()).isEqualTo(ApprovalChannelEnum.IMATE.name());
        }
    }

    @Nested
    @DisplayName("审计事件")
    class AuditTest {

        @Test
        @DisplayName("只在审批通过并放行时审计一次：这条事件已回答了谁发起、谁审批、批的是什么")
        void givenReleasedThenAuditExactlyOnce() {
            givenPendingTask();
            givenChannelApproved(TASK_ID, CREATOR);
            when(approvalTaskDAO.bindTicketIdIfAbsent(TASK_ID, TICKET_ID)).thenReturn(1);
            when(approvalTaskDAO.casConsumeToExecuting(eq(TASK_ID), eq(CREATOR), anyLong(), anyLong(), anyLong()))
                .thenReturn(1);
            executor.result = DryRunResult.valid(null, "job-instance-1");

            approvalTaskService.refresh(TASK_ID, TICKET_ID, caller());

            verify(approvalAuditor, times(1)).auditRelease(any());
        }

        @Test
        @DisplayName("发起、驳回、作废都不审计：它们不曾真正改变系统，记下来只会淹没放行事件")
        void givenNotReleasedThenNoAudit() {
            executor.result = DryRunResult.valid(new ResolvedSummary(), null);
            approvalTaskService.create(
                ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT, buildParams(), ApprovalChannelEnum.IMATE, caller());

            givenPendingTask();
            ApprovalResult rejected = new ApprovalResult();
            rejected.setStatus(ApprovalResultStatusEnum.REJECTED);
            rejected.setApprover(CREATOR);
            rejected.setApprovedAt(System.currentTimeMillis());
            when(approvalChannel.queryResult(any(), anyString())).thenReturn(rejected);
            approvalTaskService.refresh(TASK_ID, TICKET_ID, caller());

            when(approvalTaskDAO.markCanceled(TASK_ID)).thenReturn(1);
            approvalTaskService.cancel(TASK_ID, caller());

            verify(approvalAuditor, never()).auditRelease(any());
        }
    }

    @Nested
    @DisplayName("放行校验链")
    class RefreshTest {

        @Test
        @DisplayName("全部校验通过后 CAS 消费，并以 dryRun=false、operator=DB 中的 creator 执行")
        void givenAllChecksPassedThenExecuteWithDryRunFalse() {
            givenPendingTask();
            givenChannelApproved(TASK_ID, CREATOR);
            when(approvalTaskDAO.bindTicketIdIfAbsent(TASK_ID, TICKET_ID)).thenReturn(1);
            when(approvalTaskDAO.casConsumeToExecuting(eq(TASK_ID), eq(CREATOR), anyLong(), anyLong(), anyLong()))
                .thenReturn(1);
            executor.result = DryRunResult.valid(null, "job-instance-1");

            approvalTaskService.refresh(TASK_ID, TICKET_ID, caller());

            assertThat(executor.dryRunFlags).containsExactly(false);
            assertThat(executor.operators).containsExactly(CREATOR);
            verify(approvalTaskDAO).markDispatched(eq(TASK_ID), anyLong());
            verify(approvalTaskDAO).markExecuted(eq(TASK_ID), anyString());
            verify(approvalTaskDAO, never()).markFailed(anyString(), anyString());
        }

        @Test
        @DisplayName("并发放行只有一个执行成功：CAS 返回 0 的一方不下发")
        void givenConcurrentRefreshThenOnlyOneExecutes() {
            givenPendingTask();
            givenChannelApproved(TASK_ID, CREATOR);
            when(approvalTaskDAO.bindTicketIdIfAbsent(TASK_ID, TICKET_ID)).thenReturn(1);
            when(approvalTaskDAO.casConsumeToExecuting(eq(TASK_ID), eq(CREATOR), anyLong(), anyLong(), anyLong()))
                .thenReturn(1, 0);
            executor.result = DryRunResult.valid(null, "job-instance-1");

            approvalTaskService.refresh(TASK_ID, TICKET_ID, caller());
            approvalTaskService.refresh(TASK_ID, TICKET_ID, caller());

            assertThat(executor.dryRunFlags).containsExactly(false);
            verify(approvalTaskDAO, times(1)).markDispatched(eq(TASK_ID), anyLong());
        }

        @Test
        @DisplayName("回查抛异常时 fail-closed：不放行、不改状态")
        void givenChannelQueryFailThenFailClosed() {
            givenPendingTask();
            when(approvalChannel.queryResult(any(), anyString()))
                .thenThrow(new InternalException("connect timeout", ErrorCode.INTERNAL_ERROR));

            assertApprovalRejected(ErrorCode.APPROVAL_CHANNEL_QUERY_FAIL);
            assertNoExecuteAndNoStatusWrite();
        }

        @Test
        @DisplayName("回查返回空结论时同样 fail-closed，不当成通过")
        void givenChannelReturnsNullThenFailClosed() {
            givenPendingTask();
            when(approvalChannel.queryResult(any(), anyString())).thenReturn(null);

            assertApprovalRejected(ErrorCode.APPROVAL_CHANNEL_QUERY_FAIL);
            assertNoExecuteAndNoStatusWrite();
        }

        @Test
        @DisplayName("回查回带的 approval_task_id 与请求不一致时拒绝：防止拿另一个任务批过的单据放行")
        void givenBindingProofMismatchThenReject() {
            givenPendingTask();
            givenChannelApproved("another-approval-task-id", CREATOR);

            assertApprovalRejected(ErrorCode.APPROVAL_TASK_ID_BINDING_MISMATCH);
            assertNoExecuteAndNoStatusWrite();
        }

        @Test
        @DisplayName("审批人不是发起人时拒绝")
        void givenApproverNotCreatorThenReject() {
            givenPendingTask();
            givenChannelApproved(TASK_ID, "someone_else");

            assertApprovalRejected(ErrorCode.APPROVAL_APPROVER_NOT_CREATOR);
            assertNoExecuteAndNoStatusWrite();
        }

        @Test
        @DisplayName("回查未回带审批人时拒绝：校验链不为渠道兜底，缺失即失败")
        void givenBlankApproverThenReject() {
            givenPendingTask();
            givenChannelApproved(TASK_ID, null);

            assertApprovalRejected(ErrorCode.APPROVAL_APPROVER_NOT_CREATOR);
            assertNoExecuteAndNoStatusWrite();
        }

        @Test
        @DisplayName("单据ID与已绑定的不一致时拒绝，且不回查渠道")
        void givenTicketIdMismatchThenReject() {
            ApprovalTaskDTO task = buildPendingTask();
            task.setApprovalTicketId("IMATE-BOUND-0001");
            when(approvalTaskDAO.getByApprovalTaskId(TASK_ID)).thenReturn(task);

            assertApprovalRejected(ErrorCode.APPROVAL_TICKET_ID_MISMATCH);
            verify(approvalChannel, never()).queryResult(any(), anyString());
            assertNoExecuteAndNoStatusWrite();
        }

        @Test
        @DisplayName("任务已过期时返回 EXPIRED 呈现状态，不回查、不落库")
        void givenExpiredTaskThenRejectWithoutWrite() {
            ApprovalTaskDTO task = buildPendingTask();
            task.setExpireAt(System.currentTimeMillis() - 1000);
            when(approvalTaskDAO.getByApprovalTaskId(TASK_ID)).thenReturn(task);

            ApprovalTaskDTO result = approvalTaskService.refresh(TASK_ID, TICKET_ID, caller());

            assertThat(result.getStatus()).isEqualTo(ApprovalStatusEnum.EXPIRED.name());
            verify(approvalChannel, never()).queryResult(any(), anyString());
            assertNoExecuteAndNoStatusWrite();
        }

        @Test
        @DisplayName("任务已被消费（EXECUTING）时拒绝放行，不回查、不重复下发")
        void givenConsumedTaskThenRejectWithoutExecute() {
            ApprovalTaskDTO task = buildPendingTask();
            task.setStatus(ApprovalStatusEnum.EXECUTING.name());
            task.setConsumedAt(System.currentTimeMillis() - 100);
            when(approvalTaskDAO.getByApprovalTaskId(TASK_ID)).thenReturn(task);

            ApprovalTaskDTO result = approvalTaskService.refresh(TASK_ID, TICKET_ID, caller());

            assertThat(result.getStatus()).isEqualTo(ApprovalStatusEnum.EXECUTING.name());
            verify(approvalChannel, never()).queryResult(any(), anyString());
            assertNoExecuteAndNoStatusWrite();
        }

        @Test
        @DisplayName("回查得到 REJECTED 时落 REJECTED 终态，这是 CAS 之前唯一允许的状态变更")
        void givenChannelRejectedThenMarkRejected() {
            givenPendingTask();
            ApprovalResult result = new ApprovalResult();
            result.setStatus(ApprovalResultStatusEnum.REJECTED);
            result.setApprover(CREATOR);
            result.setApprovedAt(System.currentTimeMillis());
            result.setComment("风险太高");
            when(approvalChannel.queryResult(any(), anyString())).thenReturn(result);

            approvalTaskService.refresh(TASK_ID, TICKET_ID, caller());

            verify(approvalTaskDAO).markRejected(eq(TASK_ID), eq(CREATOR), anyLong(), eq("风险太高"));
            verify(approvalTaskDAO, never()).casConsumeToExecuting(anyString(), anyString(),
                anyLong(), anyLong(), anyLong());
            assertThat(executor.dryRunFlags).isEmpty();
        }

        @Test
        @DisplayName("渠道仍在审批中时原样返回 PENDING，不改状态")
        void givenChannelPendingThenKeepPending() {
            givenPendingTask();
            ApprovalResult result = new ApprovalResult();
            result.setStatus(ApprovalResultStatusEnum.PENDING);
            when(approvalChannel.queryResult(any(), anyString())).thenReturn(result);

            ApprovalTaskDTO task = approvalTaskService.refresh(TASK_ID, TICKET_ID, caller());

            assertThat(task.getStatus()).isEqualTo(ApprovalStatusEnum.PENDING.name());
            assertNoExecuteAndNoStatusWrite();
        }

        @Test
        @DisplayName("已是终态时幂等短路：不回查、不执行、不改状态")
        void givenFinalStatusThenShortCircuit() {
            ApprovalTaskDTO task = buildPendingTask();
            task.setStatus(ApprovalStatusEnum.EXECUTED.name());
            when(approvalTaskDAO.getByApprovalTaskId(TASK_ID)).thenReturn(task);

            ApprovalTaskDTO result = approvalTaskService.refresh(TASK_ID, TICKET_ID, caller());

            assertThat(result.getStatus()).isEqualTo(ApprovalStatusEnum.EXECUTED.name());
            verify(approvalChannel, never()).queryResult(any(), anyString());
            assertNoExecuteAndNoStatusWrite();
        }

        @Test
        @DisplayName("下游明确业务失败时落 FAILED 终态")
        void givenDownstreamBusinessFailureThenMarkFailed() {
            givenPendingTask();
            givenChannelApproved(TASK_ID, CREATOR);
            when(approvalTaskDAO.bindTicketIdIfAbsent(TASK_ID, TICKET_ID)).thenReturn(1);
            when(approvalTaskDAO.casConsumeToExecuting(eq(TASK_ID), eq(CREATOR), anyLong(), anyLong(), anyLong()))
                .thenReturn(1);
            executor.result = DryRunResult.invalid(ErrorCode.SCRIPT_NOT_EXIST, null);

            assertThatThrownBy(() -> approvalTaskService.refresh(TASK_ID, TICKET_ID, caller()))
                .isInstanceOf(ServiceException.class);

            verify(approvalTaskDAO).markFailed(eq(TASK_ID), anyString());
            verify(approvalTaskDAO, never()).markExecuted(anyString(), anyString());
        }

        @Test
        @DisplayName("下发后结果未知时保持 EXECUTING 且 execute_result 为空，不重试")
        void givenUnknownResultThenKeepExecuting() {
            givenPendingTask();
            givenChannelApproved(TASK_ID, CREATOR);
            when(approvalTaskDAO.bindTicketIdIfAbsent(TASK_ID, TICKET_ID)).thenReturn(1);
            when(approvalTaskDAO.casConsumeToExecuting(eq(TASK_ID), eq(CREATOR), anyLong(), anyLong(), anyLong()))
                .thenReturn(1);
            executor.exceptionToThrow = new InternalException("read timeout", ErrorCode.INTERNAL_ERROR);

            approvalTaskService.refresh(TASK_ID, TICKET_ID, caller());

            assertThat(executor.dryRunFlags).containsExactly(false);
            verify(approvalTaskDAO).markDispatched(eq(TASK_ID), anyLong());
            verify(approvalTaskDAO, never()).markExecuted(anyString(), anyString());
            verify(approvalTaskDAO, never()).markFailed(anyString(), anyString());
        }

        @Test
        @DisplayName("任务归属与调用方不一致时统一返回任务不存在")
        void givenOtherOwnerThenNotFound() {
            ApprovalTaskDTO task = buildPendingTask();
            task.setCreator("another_user");
            when(approvalTaskDAO.getByApprovalTaskId(TASK_ID)).thenReturn(task);

            assertThatThrownBy(() -> approvalTaskService.refresh(TASK_ID, TICKET_ID, caller()))
                .isInstanceOf(NotFoundException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getErrorCode())
                    .isEqualTo(ErrorCode.APPROVAL_TASK_NOT_EXIST));
            assertNoExecuteAndNoStatusWrite();
        }

        @Test
        @DisplayName("任务不存在时返回任务不存在")
        void givenNoTaskThenNotFound() {
            when(approvalTaskDAO.getByApprovalTaskId(TASK_ID)).thenReturn(null);

            assertThatThrownBy(() -> approvalTaskService.refresh(TASK_ID, TICKET_ID, caller()))
                .isInstanceOf(NotFoundException.class);
            assertNoExecuteAndNoStatusWrite();
        }

        private void assertApprovalRejected(int expectedErrorCode) {
            assertThatThrownBy(() -> approvalTaskService.refresh(TASK_ID, TICKET_ID, caller()))
                .isInstanceOf(FailedPreconditionException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getErrorCode()).isEqualTo(expectedErrorCode));
        }
    }

    @Nested
    @DisplayName("参数与执行入参的结构性约束")
    class StructuralConstraintTest {

        /**
         * 放行时"不得 skipAuth"这条性质由结构保证而非运行期断言：审批下发用的请求体里根本没有
         * skipAuth / cronTaskId 字段，因此调用方无从传入。
         * 一旦有人为了"复用"给它们加上这两个字段，本用例立刻失败。
         */
        @Test
        @DisplayName("审批下发用的请求体不含 skipAuth / cronTaskId 字段")
        void givenApprovalRequestsThenNoSkipAuthField() {
            List<Class<?>> requestClasses = Arrays.asList(
                ServiceApprovalFastTransferFileRequest.class,
                ServiceApprovalExecuteJobPlanRequest.class,
                ServiceApprovalSaveCronRequest.class,
                ServiceApprovalUpdateCronStatusRequest.class
            );
            for (Class<?> requestClass : requestClasses) {
                List<String> fieldNames = declaredFieldNames(requestClass);
                assertThat(fieldNames)
                    .as("%s 不得出现 skipAuth / cronTaskId", requestClass.getSimpleName())
                    .doesNotContain("skipAuth", "cronTaskId");
                assertThat(fieldNames).contains("dryRun", "operator");
            }
        }

        /**
         * 已改走 OpenAPI 的操作没有 inner 包装体，同一条性质落在对外请求体上：
         * 它们是公开契约，本就不该出现这两个内部字段，出现即意味着内部开关被暴露给了外部调用方
         */
        @Test
        @DisplayName("改走 OpenAPI 的操作，其对外请求体同样不含 skipAuth / cronTaskId 字段")
        void givenOpenApiRequestsThenNoSkipAuthField() {
            List<Class<?>> requestClasses = Arrays.asList(
                V4FastExecuteScriptRequest.class,
                V4CreateJobPlanRequest.class
            );
            for (Class<?> requestClass : requestClasses) {
                assertThat(declaredFieldNames(requestClass))
                    .as("%s 不得出现 skipAuth / cronTaskId", requestClass.getSimpleName())
                    .doesNotContain("skipAuth", "cronTaskId");
            }
        }

        private List<String> declaredFieldNames(Class<?> clazz) {
            List<String> fieldNames = new ArrayList<>();
            for (Field field : clazz.getDeclaredFields()) {
                fieldNames.add(field.getName());
            }
            return fieldNames;
        }

        @Test
        @DisplayName("放行执行的参数只来自 DB 快照，app_id 以任务为准")
        void givenSnapshotThenParamsComeFromDbOnly() {
            ApprovalTaskDTO task = buildPendingTask();
            V4FastExecuteScriptRequest snapshotParams = buildParams();
            snapshotParams.setContent("echo from-snapshot");
            task.setOperationParams(ApprovalParamsCryptoServiceStub.PREFIX + JsonUtils.toJson(snapshotParams));
            when(approvalTaskDAO.getByApprovalTaskId(TASK_ID)).thenReturn(task);
            givenChannelApproved(TASK_ID, CREATOR);
            when(approvalTaskDAO.bindTicketIdIfAbsent(TASK_ID, TICKET_ID)).thenReturn(1);
            when(approvalTaskDAO.casConsumeToExecuting(eq(TASK_ID), eq(CREATOR), anyLong(), anyLong(), anyLong()))
                .thenReturn(1);
            executor.result = DryRunResult.valid(null, "job-instance-1");

            approvalTaskService.refresh(TASK_ID, TICKET_ID, caller());

            assertThat(executor.paramsSeen).hasSize(1);
            V4FastExecuteScriptRequest used = executor.paramsSeen.get(0);
            assertThat(used.getContent()).isEqualTo("echo from-snapshot");
            // appId 是 @JsonIgnore 字段，快照里不存在，必须由任务的 app_id 补齐
            assertThat(used.getAppId()).isEqualTo(APP_ID);
        }
    }

    @Nested
    @DisplayName("取审批内容")
    class GetApprovalContentTest {

        @Test
        @DisplayName("调用方是该任务指派的渠道且为发起人本人时返回内容，并记录拉取时间")
        void givenAssignedChannelThenReturnContent() {
            ApprovalTaskDTO task = buildPendingTask();
            when(approvalTaskDAO.getByApprovalTaskId(TASK_ID)).thenReturn(task);
            ApprovalContent rendered = new ApprovalContent();
            rendered.setApprovalTaskId(TASK_ID);
            when(contentRenderer.render(task)).thenReturn(rendered);

            ApprovalContent content = approvalTaskService.getApprovalContent(TASK_ID, channelCaller(CREATOR));

            assertThat(content).isSameAs(rendered);
            verify(approvalTaskDAO).updateTicketFetchedAt(eq(TASK_ID), anyLong());
        }

        @Test
        @DisplayName("调用方不是该任务指派的渠道时按任务不存在处理，且不渲染内容")
        void givenOtherAppCodeThenNotFound() {
            when(approvalTaskDAO.getByApprovalTaskId(TASK_ID)).thenReturn(buildPendingTask());
            ApprovalCallerContext caller = ApprovalCallerContext.builder()
                .tenantId(TENANT_ID).username(CREATOR).appCode("bk_other").build();

            assertThatThrownBy(() -> approvalTaskService.getApprovalContent(TASK_ID, caller))
                .isInstanceOf(NotFoundException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getErrorCode())
                    .isEqualTo(ErrorCode.APPROVAL_TASK_NOT_EXIST));
            verify(contentRenderer, never()).render(any());
            verify(approvalTaskDAO, never()).updateTicketFetchedAt(anyString(), anyLong());
        }

        @Test
        @DisplayName("非发起人本人取内容按任务不存在处理：内容里有脚本明文")
        void givenOtherUsernameThenNotFound() {
            when(approvalTaskDAO.getByApprovalTaskId(TASK_ID)).thenReturn(buildPendingTask());

            assertThatThrownBy(() -> approvalTaskService.getApprovalContent(TASK_ID, channelCaller("other_user")))
                .isInstanceOf(NotFoundException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getErrorCode())
                    .isEqualTo(ErrorCode.APPROVAL_TASK_NOT_EXIST));
            verify(contentRenderer, never()).render(any());
        }

        @Test
        @DisplayName("跨租户取内容按任务不存在处理")
        void givenOtherTenantThenNotFound() {
            when(approvalTaskDAO.getByApprovalTaskId(TASK_ID)).thenReturn(buildPendingTask());
            ApprovalCallerContext caller = ApprovalCallerContext.builder()
                .tenantId("other_tenant").username(CREATOR).appCode(CHANNEL_APP_CODE).build();

            assertThatThrownBy(() -> approvalTaskService.getApprovalContent(TASK_ID, caller))
                .isInstanceOf(NotFoundException.class);
            verify(contentRenderer, never()).render(any());
        }

        @Test
        @DisplayName("渠道未配置 appCode 时不得放过任何调用方")
        void givenChannelAppCodeNotConfiguredThenNotFound() {
            when(approvalTaskDAO.getByApprovalTaskId(TASK_ID)).thenReturn(buildPendingTask());
            when(channelRegistry.getChannelAppCodes(anyString())).thenReturn(Collections.emptySet());
            ApprovalCallerContext caller = ApprovalCallerContext.builder()
                .tenantId(TENANT_ID).username(CREATOR).appCode("").build();

            assertThatThrownBy(() -> approvalTaskService.getApprovalContent(TASK_ID, caller))
                .isInstanceOf(NotFoundException.class);
            verify(contentRenderer, never()).render(any());
        }

        @Test
        @DisplayName("渠道配置多个 appCode 时命中其中任意一个即可取到内容")
        void givenCallerAppCodeMatchesOneOfConfiguredThenReturnContent() {
            ApprovalTaskDTO task = buildPendingTask();
            ApprovalContent rendered = new ApprovalContent();
            when(approvalTaskDAO.getByApprovalTaskId(TASK_ID)).thenReturn(task);
            when(contentRenderer.render(task)).thenReturn(rendered);
            when(channelRegistry.getChannelAppCodes(anyString()))
                .thenReturn(new HashSet<>(Arrays.asList("other_app", CHANNEL_APP_CODE)));
            ApprovalCallerContext caller = ApprovalCallerContext.builder()
                .tenantId(TENANT_ID).username(CREATOR).appCode(CHANNEL_APP_CODE).build();

            assertThat(approvalTaskService.getApprovalContent(TASK_ID, caller)).isSameAs(rendered);
        }

        @Test
        @DisplayName("调用方 appCode 不在渠道配置的多个 appCode 中时按任务不存在处理")
        void givenCallerAppCodeNotInConfiguredThenNotFound() {
            when(approvalTaskDAO.getByApprovalTaskId(TASK_ID)).thenReturn(buildPendingTask());
            when(channelRegistry.getChannelAppCodes(anyString()))
                .thenReturn(new HashSet<>(Arrays.asList("app_a", "app_b")));
            ApprovalCallerContext caller = ApprovalCallerContext.builder()
                .tenantId(TENANT_ID).username(CREATOR).appCode("app_c").build();

            assertThatThrownBy(() -> approvalTaskService.getApprovalContent(TASK_ID, caller))
                .isInstanceOf(NotFoundException.class);
            verify(contentRenderer, never()).render(any());
        }

        @Test
        @DisplayName("记录拉取时间失败不影响取内容结果")
        void givenUpdateFetchedAtFailThenStillReturnContent() {
            ApprovalTaskDTO task = buildPendingTask();
            when(approvalTaskDAO.getByApprovalTaskId(TASK_ID)).thenReturn(task);
            when(contentRenderer.render(task)).thenReturn(new ApprovalContent());
            when(approvalTaskDAO.updateTicketFetchedAt(anyString(), anyLong()))
                .thenThrow(new InternalException("db error", ErrorCode.INTERNAL_ERROR));

            assertThat(approvalTaskService.getApprovalContent(TASK_ID, channelCaller(CREATOR))).isNotNull();
        }
    }

    @Nested
    @DisplayName("放行路径的可观测指标")
    class MetricsTest {

        @Test
        @DisplayName("渠道从未取过审批内容就放行时计数：这是发现单据未经作业平台生成的唯一手段")
        void givenNoTicketFetchThenCountDispatchedWithoutTicketFetch() {
            long createTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10);
            givenDispatchedTask(createTime, createTime + TimeUnit.MINUTES.toMillis(9), null);

            approvalTaskService.refresh(TASK_ID, TICKET_ID, caller());

            assertThat(counterCount(ApprovalMetrics.NAME_DISPATCHED_WITHOUT_CONTENT_FETCH)).isEqualTo(1.0);
            assertThat(counterCount(ApprovalMetrics.NAME_FAST_APPROVED)).isZero();
        }

        @Test
        @DisplayName("秒批时计数：审批耗时低于阈值即视为疑似自动审批")
        void givenFastApprovedThenCountFastApproved() {
            long createTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10);
            givenDispatchedTask(createTime, createTime + 1000, createTime + 500);

            approvalTaskService.refresh(TASK_ID, TICKET_ID, caller());

            assertThat(counterCount(ApprovalMetrics.NAME_FAST_APPROVED)).isEqualTo(1.0);
            assertThat(counterCount(ApprovalMetrics.NAME_DISPATCHED_WITHOUT_CONTENT_FETCH)).isZero();
        }

        @Test
        @DisplayName("指标带 operation_type / approval_channel / app_code 标签")
        void givenDispatchedThenTagsCarryOperationChannelAndAppCode() {
            long createTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10);
            givenDispatchedTask(createTime, createTime + 1000, createTime + 500);

            approvalTaskService.refresh(TASK_ID, TICKET_ID, caller());

            Counter counter = meterRegistry.find(ApprovalMetrics.NAME_FAST_APPROVED).counter();
            assertThat(counter).isNotNull();
            assertThat(counter.getId().getTag("operation_type"))
                .isEqualTo(ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT.name());
            assertThat(counter.getId().getTag("approval_channel")).isEqualTo(ApprovalChannelEnum.IMATE.name());
            assertThat(counter.getId().getTag("app_code")).isEqualTo(APP_CODE);
        }

        @Test
        @DisplayName("正常取过单且审批耗时超过阈值时两个指标都不计数")
        void givenNormalApprovalThenNoCount() {
            long createTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10);
            givenDispatchedTask(createTime, createTime + TimeUnit.MINUTES.toMillis(9),
                createTime + TimeUnit.SECONDS.toMillis(30));

            approvalTaskService.refresh(TASK_ID, TICKET_ID, caller());

            assertThat(counterCount(ApprovalMetrics.NAME_FAST_APPROVED)).isZero();
            assertThat(counterCount(ApprovalMetrics.NAME_DISPATCHED_WITHOUT_CONTENT_FETCH)).isZero();
        }

        @Test
        @DisplayName("下发结果未知也不影响计数：指标反映的是「已放行」这一事实")
        void givenUnknownDispatchResultThenStillCount() {
            long createTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10);
            givenDispatchedTask(createTime, createTime + 1000, null);
            executor.exceptionToThrow = new InternalException("read timeout", ErrorCode.INTERNAL_ERROR);

            approvalTaskService.refresh(TASK_ID, TICKET_ID, caller());

            assertThat(counterCount(ApprovalMetrics.NAME_DISPATCHED_WITHOUT_CONTENT_FETCH)).isEqualTo(1.0);
            assertThat(counterCount(ApprovalMetrics.NAME_FAST_APPROVED)).isEqualTo(1.0);
        }

        /**
         * 造一个能一路走到 markDispatched 的任务：approvedAt / ticketFetchedAt 由用例指定
         */
        private void givenDispatchedTask(long createTime, Long approvedAt, Long ticketFetchedAt) {
            ApprovalTaskDTO task = buildPendingTask();
            task.setCreateTime(createTime);
            task.setExpireAt(createTime + TimeUnit.HOURS.toMillis(8));
            task.setApprovedAt(approvedAt);
            task.setTicketFetchedAt(ticketFetchedAt);
            when(approvalTaskDAO.getByApprovalTaskId(TASK_ID)).thenReturn(task);
            givenChannelApproved(TASK_ID, CREATOR);
            when(approvalTaskDAO.bindTicketIdIfAbsent(TASK_ID, TICKET_ID)).thenReturn(1);
            when(approvalTaskDAO.casConsumeToExecuting(eq(TASK_ID), eq(CREATOR), anyLong(), anyLong(), anyLong()))
                .thenReturn(1);
            if (executor.exceptionToThrow == null) {
                executor.result = DryRunResult.valid(null, "job-instance-1");
            }
        }

        private double counterCount(String name) {
            Counter counter = meterRegistry.find(name).counter();
            return counter == null ? 0.0 : counter.count();
        }
    }

    private void givenPendingTask() {
        when(approvalTaskDAO.getByApprovalTaskId(TASK_ID)).thenReturn(buildPendingTask());
    }

    private void givenChannelApproved(String returnedApprovalTaskId, String approver) {
        ApprovalResult result = new ApprovalResult();
        result.setStatus(ApprovalResultStatusEnum.APPROVED);
        result.setApprovalTaskId(returnedApprovalTaskId);
        result.setApprover(approver);
        result.setApprovedAt(System.currentTimeMillis());
        when(approvalChannel.queryResult(any(), anyString())).thenReturn(result);
    }

    /**
     * CAS 之前的拒绝分支一律不得写终态，也不得下发下游
     */
    private void assertNoExecuteAndNoStatusWrite() {
        assertThat(executor.dryRunFlags).isEmpty();
        verify(approvalTaskDAO, never()).casConsumeToExecuting(anyString(), anyString(),
            anyLong(), anyLong(), anyLong());
        verify(approvalTaskDAO, never()).markDispatched(anyString(), anyLong());
        assertNoFinalStatusWrite();
        verify(approvalTaskDAO, never()).markRejected(anyString(), anyString(), any(), any());
    }

    private void assertNoFinalStatusWrite() {
        verify(approvalTaskDAO, never()).markExecuted(anyString(), any());
        verify(approvalTaskDAO, never()).markFailed(anyString(), anyString());
        verify(approvalTaskDAO, never()).markCanceled(anyString());
    }

    private ApprovalTaskDTO buildPendingTask() {
        ApprovalTaskDTO task = new ApprovalTaskDTO();
        task.setId(1L);
        task.setApprovalTaskId(TASK_ID);
        task.setTenantId(TENANT_ID);
        task.setAppId(APP_ID);
        task.setOperationType(ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT.name());
        task.setOperationParams(ApprovalParamsCryptoServiceStub.PREFIX + JsonUtils.toJson(buildParams()));
        task.setCreator(CREATOR);
        task.setAppCode(APP_CODE);
        task.setApprovalChannel(ApprovalChannelEnum.IMATE.name());
        task.setStatus(ApprovalStatusEnum.PENDING.name());
        task.setCreateTime(System.currentTimeMillis());
        task.setExpireAt(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(8));
        return task;
    }

    private static V4FastExecuteScriptRequest buildParams() {
        V4FastExecuteScriptRequest request = new V4FastExecuteScriptRequest();
        request.setName("test-script-task");
        request.setContent("echo secret");
        request.setScopeType("biz");
        request.setScopeId("2");
        request.setAppId(APP_ID);
        return request;
    }

    private static ApprovalCallerContext caller() {
        return ApprovalCallerContext.builder()
            .tenantId(TENANT_ID)
            .appId(APP_ID)
            .username(CREATOR)
            .appCode(APP_CODE)
            .build();
    }

    /**
     * 取内容接口的调用上下文：appCode 是渠道自身的，username 由用例指定
     */
    private static ApprovalCallerContext channelCaller(String username) {
        return ApprovalCallerContext.builder()
            .tenantId(TENANT_ID)
            .username(username)
            .appCode(CHANNEL_APP_CODE)
            .build();
    }

    /**
     * 记录调用参数的执行器，用于断言 dryRun 取值与 operator 来源
     */
    private static class RecordingOperationExecutor implements OperationExecutor<V4FastExecuteScriptRequest> {

        private final List<Boolean> dryRunFlags = new ArrayList<>();
        private final List<String> operators = new ArrayList<>();
        private final List<V4FastExecuteScriptRequest> paramsSeen = new ArrayList<>();
        private DryRunResult<?> result = DryRunResult.valid(new ResolvedSummary(), null);
        private RuntimeException exceptionToThrow;

        @Override
        public ApprovalOperationTypeEnum getOperationType() {
            return ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT;
        }

        @Override
        public Class<V4FastExecuteScriptRequest> getParamsClass() {
            return V4FastExecuteScriptRequest.class;
        }

        @Override
        public DryRunResult<?> invoke(V4FastExecuteScriptRequest params, ApprovalTaskDTO task, boolean dryRun) {
            dryRunFlags.add(dryRun);
            operators.add(task.getCreator());
            paramsSeen.add(params);
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            return result;
        }
    }

    /**
     * 加解密替身：加密加前缀、解密去前缀，用于断言"落库的是密文、执行用的是明文"
     */
    private static class ApprovalParamsCryptoServiceStub
        implements com.tencent.bk.job.analysis.approval.ApprovalParamsCryptoService {

        private static final String PREFIX = "ENC:";

        @Override
        public String encryptSensitiveFields(ApprovalOperationTypeEnum operationType, String paramsJson) {
            return PREFIX + paramsJson;
        }

        @Override
        public String decryptSensitiveFields(ApprovalOperationTypeEnum operationType, String paramsJson) {
            return paramsJson.startsWith(PREFIX) ? paramsJson.substring(PREFIX.length()) : paramsJson;
        }
    }
}
