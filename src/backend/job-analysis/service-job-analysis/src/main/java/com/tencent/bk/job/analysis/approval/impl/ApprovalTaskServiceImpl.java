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
import com.tencent.bk.job.analysis.approval.ApprovalParamsCryptoService;
import com.tencent.bk.job.analysis.approval.ApprovalTaskService;
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
import com.tencent.bk.job.common.model.ResolvedSummary;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.exception.OpenApiPropagatedException;
import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import com.tencent.bk.job.common.esb.model.v4.EsbV4RespError;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.model.error.ErrorType;
import com.tencent.bk.job.common.util.JobUUID;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 审批任务核心编排。
 * <p>
 * <b>{@link #refresh} 里那条校验链的顺序、以及"CAS 之前不写终态"这条不变式，删改任何一处都会让整套
 * 审批机制失效</b>，每一步的意图见各步注释。
 */
@Slf4j
@Service
public class ApprovalTaskServiceImpl implements ApprovalTaskService {

    /**
     * 表示"下游已明确拒绝、且拒绝发生在产生任何副作用之前"的错误语义。
     * <p>
     * 有意不含 ABORTED / TIMEOUT / UNAVAILABLE / INTERNAL：这几类都无法判断操作是否已经生效
     */
    private static final Set<ErrorType> REJECTED_BEFORE_EXECUTE_ERROR_TYPES = EnumSet.of(
        ErrorType.INVALID_PARAM,
        ErrorType.FAILED_PRECONDITION,
        ErrorType.UNAUTHENTICATED,
        ErrorType.PERMISSION_DENIED,
        ErrorType.NOT_FOUND,
        ErrorType.ALREADY_EXISTS,
        ErrorType.RESOURCE_EXHAUSTED,
        ErrorType.UNIMPLEMENTED
    );

    private final ApprovalTaskDAO approvalTaskDAO;
    private final ApprovalChannelRegistry channelRegistry;
    private final OperationExecutorRegistry executorRegistry;
    private final ApprovalParamsCryptoService paramsCryptoService;
    private final ApprovalProperties approvalProperties;
    private final ApprovalContentRenderer contentRenderer;
    private final ApprovalMetrics approvalMetrics;
    private final ApprovalAuditor approvalAuditor;

    public ApprovalTaskServiceImpl(ApprovalTaskDAO approvalTaskDAO,
                                   ApprovalChannelRegistry channelRegistry,
                                   OperationExecutorRegistry executorRegistry,
                                   ApprovalParamsCryptoService paramsCryptoService,
                                   ApprovalProperties approvalProperties,
                                   ApprovalContentRenderer contentRenderer,
                                   ApprovalMetrics approvalMetrics,
                                   ApprovalAuditor approvalAuditor) {
        this.approvalTaskDAO = approvalTaskDAO;
        this.channelRegistry = channelRegistry;
        this.executorRegistry = executorRegistry;
        this.paramsCryptoService = paramsCryptoService;
        this.approvalProperties = approvalProperties;
        this.contentRenderer = contentRenderer;
        this.approvalMetrics = approvalMetrics;
        this.approvalAuditor = approvalAuditor;
    }

    @Override
    public ApprovalTaskDTO create(ApprovalOperationTypeEnum operationType,
                                  Object params,
                                  ApprovalChannelEnum channel,
                                  ApprovalCallerContext caller) {

        ApprovalChannelEnum targetChannel = channel == null ? approvalProperties.getDefaultChannel() : channel;
        // 渠道不可用时直接失败，绝不"先建单再说" —— 建了也回查不了，只会白批
        channelRegistry.getChannel(targetChannel);
        OperationExecutor<?> executor = executorRegistry.getExecutor(operationType);

        long now = System.currentTimeMillis();
        ApprovalTaskDTO task = new ApprovalTaskDTO();
        task.setApprovalTaskId(JobUUID.getUUID());
        task.setTenantId(caller.getTenantId());
        task.setAppId(caller.getAppId());
        task.setOperationType(operationType.name());
        task.setCreator(caller.getUsername());
        task.setAppCode(caller.getAppCode() == null ? StringUtils.EMPTY : caller.getAppCode());
        task.setApprovalChannel(targetChannel.name());
        task.setStatus(ApprovalStatusEnum.PENDING.name());
        task.setCreateTime(now);
        task.setExpireAt(now + TimeUnit.HOURS.toMillis(resolveTtlHours()));

        // 预检与放行执行走同一个 invoke，只有 dryRun 取值不同；下游拒绝会以异常直接向上抛出
        EsbV4Response<?> dryRunResponse = invokeExecutor(executor, params, task, true);

        ResolvedSummary summary = dryRunResponse.getDryRunSummary();
        if (summary == null) {
            summary = new ResolvedSummary();
        }
        // 操作类型由 job-analysis 侧填充：下游不复制审批域的操作类型枚举
        summary.setOperationType(operationType.name());
        task.setResolvedSummary(JsonUtils.toJson(summary));
        // 加密失败让异常向上传播，绝不降级为明文落库
        task.setOperationParams(paramsCryptoService.encryptToSnapshot(operationType, params));

        approvalTaskDAO.insertApprovalTask(task);
        log.info("Approval task created, approvalTaskId: {}, operationType: {}, creator: {}, channel: {}, "
                + "expireAt: {}",
            task.getApprovalTaskId(), operationType, task.getCreator(), targetChannel, task.getExpireAt());
        return task;
    }

    @Override
    public ApprovalTaskDTO get(String approvalTaskId, ApprovalCallerContext caller) {
        ApprovalTaskDTO task = loadOwnTask(approvalTaskId, caller);
        return withPresentationStatus(task, System.currentTimeMillis());
    }

    @Override
    public ApprovalTaskDTO refresh(String approvalTaskId, String approvalTicketId, ApprovalCallerContext caller) {
        // 【0】任务存在且归属一致；不存在与无权访问返回同一个结果，避免用 ID 探测他人任务
        ApprovalTaskDTO task = loadOwnTask(approvalTaskId, caller);

        // 【1】已是终态：幂等短路，不回查、不执行、不改状态
        ApprovalStatusEnum status = task.getStatusEnum();
        if (status != null && status.isFinalStatus()) {
            return task;
        }

        long now = System.currentTimeMillis();
        // 【2】未过期。EXPIRED 只是惰性判断出的呈现值，不落库
        if (task.isExpired(now)) {
            log.info("Approval task {} expired at {}, reject release", approvalTaskId, task.getExpireAt());
            return withPresentationStatus(task, now);
        }

        // 【3】未被消费。非 PENDING 的非终态只有 EXECUTING，说明已被消费，返回当前状态即可
        if (status != ApprovalStatusEnum.PENDING) {
            log.info("Approval task {} is in status {}, reject release", approvalTaskId, status);
            return task;
        }
        if (task.getConsumedAt() != null) {
            // 状态与消费标记不一致属数据异常，保守拒绝而不是继续放行
            log.warn("Approval task {} is PENDING but consumedAt is {}, reject release",
                approvalTaskId, task.getConsumedAt());
            throw new FailedPreconditionException(ErrorCode.APPROVAL_TASK_ALREADY_CONSUMED);
        }

        // 【4】ticket 绑定固化后不可更换
        checkTicketIdMatch(task, approvalTicketId);

        // 【5】在该任务指派的渠道内回查；异常一律 fail-closed
        ApprovalResult approvalResult = queryApprovalResult(task, approvalTicketId);
        if (approvalResult.getStatus() == ApprovalResultStatusEnum.REJECTED) {
            // CAS 之前唯一允许的状态变更：PENDING → REJECTED
            approvalTaskDAO.markRejected(approvalTaskId, approvalResult.getApprover(),
                approvalResult.getApprovedAt(), approvalResult.getComment());
            return loadOwnTask(approvalTaskId, caller);
        }
        if (approvalResult.getStatus() != ApprovalResultStatusEnum.APPROVED) {
            log.info("Approval task {} is still pending in channel {}", approvalTaskId, task.getApprovalChannel());
            return task;
        }

        // 【6】审批人必须是发起人本人
        if (StringUtils.isBlank(approvalResult.getApprover())
            || !approvalResult.getApprover().equals(task.getCreator())) {
            log.warn("Approval task {} rejected: approver {} is not the creator {}",
                approvalTaskId, approvalResult.getApprover(), task.getCreator());
            throw new FailedPreconditionException(ErrorCode.APPROVAL_APPROVER_NOT_CREATOR);
        }

        // 【7】绑定证明：回查响应回带的 approval_task_id 必须与请求一致，
        // 否则拿另一个任务真实批过的单据就能放行本任务
        if (!approvalTaskId.equals(approvalResult.getApprovalTaskId())) {
            log.warn("Approval task {} rejected: channel returned approvalTaskId {}",
                approvalTaskId, approvalResult.getApprovalTaskId());
            throw new FailedPreconditionException(ErrorCode.APPROVAL_TASK_ID_BINDING_MISMATCH);
        }

        // 【8】固化 ticketId，只写 approval_ticket_id，不触碰 status
        bindTicketId(task, approvalTicketId, caller);

        // 【9】参数一律从 DB 原样读出，不接受任何外部覆盖
        Object params = resolveParamsFromSnapshot(task);

        // 【10】CAS 消费：只有一个请求能把 PENDING 变成 EXECUTING
        int consumed = approvalTaskDAO.casConsumeToExecuting(approvalTaskId, approvalResult.getApprover(),
            approvalResult.getApprovedAt() == null ? now : approvalResult.getApprovedAt(), now, now);
        if (consumed != 1) {
            log.info("Approval task {} has been consumed concurrently", approvalTaskId);
            return loadOwnTask(approvalTaskId, caller);
        }

        // 【11】~【12】下发执行
        ApprovalTaskDTO consumedTask = loadOwnTask(approvalTaskId, caller);
        return executeAfterConsume(consumedTask, params, caller);
    }

    @Override
    public ApprovalTaskDTO cancel(String approvalTaskId, ApprovalCallerContext caller) {
        ApprovalTaskDTO task = loadOwnTask(approvalTaskId, caller);
        ApprovalStatusEnum status = task.getStatusEnum();
        if (status != ApprovalStatusEnum.PENDING) {
            throw new FailedPreconditionException(ErrorCode.APPROVAL_TASK_STATUS_NOT_ALLOWED,
                new Object[]{task.getStatus()});
        }
        // 只作废本地任务，不反向通知渠道：渠道侧单据由审批人自行处理
        approvalTaskDAO.markCanceled(approvalTaskId);
        return loadOwnTask(approvalTaskId, caller);
    }

    @Override
    public ApprovalContent getApprovalContent(String approvalTaskId, ApprovalCallerContext caller) {
        ApprovalTaskDTO task = approvalTaskDAO.getByApprovalTaskId(approvalTaskId);
        if (task == null || !isAssignedChannelCaller(task, caller)) {
            throw new NotFoundException(ErrorCode.APPROVAL_TASK_NOT_EXIST, new Object[]{approvalTaskId});
        }
        ApprovalContent content = contentRenderer.render(task);
        try {
            // 仅观测：写失败不影响本次取内容，也不影响放行（放行不校验 ticket_fetched_at）
            approvalTaskDAO.updateTicketFetchedAt(approvalTaskId, System.currentTimeMillis());
        } catch (Exception e) {
            log.warn("Update ticketFetchedAt failed, approvalTaskId: {}", approvalTaskId, e);
        }
        return content;
    }

    /**
     * 取内容方必须同时是该任务的租户、发起人本人、以及任务指派渠道对应的应用之一。
     * <p>
     * 内容里有脚本明文，任一不符都按"任务不存在"处理，不区分"不存在"与"无权访问"。
     * <p>
     * <b>本接口在网关上登记为应用态，网关不认证用户身份</b>：这里比对的 username 是调用方自行填写的
     * 值，其可信度来自"调用方 appCode 必须命中该渠道配置的白名单、且该资源不可自助申请权限"，
     * 即<b>信任被授权的渠道会如实传入当前审批人</b>，而不是来自网关的用户认证。因此 appCode 这条
     * 校验是整条链的信任根，未配置一律不匹配（见 {@link ApprovalChannelRegistry#getChannelAppCodes}）。
     */
    private boolean isAssignedChannelCaller(ApprovalTaskDTO task, ApprovalCallerContext caller) {
        if (!Objects.equals(task.getTenantId(), caller.getTenantId())) {
            log.warn("Get approval content rejected: tenant mismatch, approvalTaskId: {}", task.getApprovalTaskId());
            return false;
        }
        if (!Objects.equals(task.getCreator(), caller.getUsername())) {
            log.warn("Get approval content rejected: username is not the creator of task {}",
                task.getApprovalTaskId());
            return false;
        }
        Set<String> channelAppCodes = channelRegistry.getChannelAppCodes(task.getApprovalChannel());
        if (!channelAppCodes.contains(caller.getAppCode())) {
            log.warn("Get approval content rejected: appCode {} is not one of the assigned channel {} apps "
                    + "of task {}",
                caller.getAppCode(), task.getApprovalChannel(), task.getApprovalTaskId());
            return false;
        }
        return true;
    }

    /**
     * 【11】打点后下发，【12】按下游结果落态。
     * <p>
     * 明确的业务失败落 FAILED；结果未知（超时、连接中断、5xx）一律停在 EXECUTING 且
     * execute_result 为空，<b>绝不重试</b> —— 无法区分"执行成功但响应丢了"与"确实没执行"。
     * 将来若放开重试，必须同时在下游引入以 approval_task_id 为幂等键的去重。
     */
    private ApprovalTaskDTO executeAfterConsume(ApprovalTaskDTO task, Object params, ApprovalCallerContext caller) {
        String approvalTaskId = task.getApprovalTaskId();
        ApprovalOperationTypeEnum operationType = ApprovalOperationTypeEnum.valOf(task.getOperationType());
        OperationExecutor<?> executor = executorRegistry.getExecutor(operationType);
        approvalTaskDAO.markDispatched(approvalTaskId, System.currentTimeMillis());
        // 必须埋在下发之前：下发结果如何都不影响"这一单已被放行"这个事实
        approvalMetrics.recordDispatched(task);

        EsbV4Response<?> response;
        try {
            // dryRun=false 正式执行；operator 取 DB 中的 creator，且不得 skipAuth
            response = invokeExecutor(executor, params, task, false);
        } catch (Exception e) {
            if (isRejectedBeforeExecute(e)) {
                // 下游明确的业务失败：确定未执行，落 FAILED 终态，用户需重新发起审批
                log.warn("Approval task {} rejected by downstream, mark FAILED", approvalTaskId, e);
                approvalTaskDAO.markFailed(approvalTaskId, buildErrorJson(e));
                throw e;
            }
            // 结果未知：保持 EXECUTING、execute_result 为空，转人工排查
            log.error("Approval task {} dispatched but result is unknown, keep EXECUTING and DO NOT retry",
                approvalTaskId, e);
            return loadOwnTask(approvalTaskId, caller);
        }

        Object executeResult = response.getData();
        approvalTaskDAO.markExecuted(approvalTaskId, executeResult == null ? null : JsonUtils.toJson(executeResult));
        log.info("Approval task {} executed", approvalTaskId);
        ApprovalTaskDTO executedTask = loadOwnTask(approvalTaskId, caller);
        // 放行审计带上审批人与审批时间：审计侧只有这一条事件能回答"这次执行是谁批的"
        approvalAuditor.auditRelease(executedTask);
        return executedTask;
    }

    /**
     * 【0】读取并校验归属。任一不符都返回"任务不存在"，
     * <b>不区分"不存在"与"无权访问"</b>，否则 approval_task_id 就成了探测他人任务的工具。
     */
    private ApprovalTaskDTO loadOwnTask(String approvalTaskId, ApprovalCallerContext caller) {
        ApprovalTaskDTO task = approvalTaskDAO.getByApprovalTaskId(approvalTaskId);
        if (task == null || !isSameOwner(task, caller)) {
            if (task != null) {
                log.warn("Approval task {} owner mismatch, return not found", approvalTaskId);
            }
            throw new NotFoundException(ErrorCode.APPROVAL_TASK_NOT_EXIST, new Object[]{approvalTaskId});
        }
        return task;
    }

    private boolean isSameOwner(ApprovalTaskDTO task, ApprovalCallerContext caller) {
        // appId 只在调用上下文里有值时才比对：流转接口的请求体不带资源范围，
        // 无条件比对会让所有流转请求都被判成"任务不存在"
        boolean appIdMatched = caller.getAppId() == null
            || Objects.equals(task.getAppId(), caller.getAppId());
        return Objects.equals(task.getTenantId(), caller.getTenantId())
            && appIdMatched
            && Objects.equals(task.getCreator(), caller.getUsername())
            && Objects.equals(StringUtils.defaultString(task.getAppCode()),
            StringUtils.defaultString(caller.getAppCode()));
    }

    /**
     * 【4】DB 中已有 ticketId 时，请求值必须严格相等
     */
    private void checkTicketIdMatch(ApprovalTaskDTO task, String approvalTicketId) {
        if (StringUtils.isBlank(approvalTicketId)) {
            throw new InvalidParamException(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME,
                new Object[]{"approval_ticket_id"});
        }
        String boundTicketId = task.getApprovalTicketId();
        if (StringUtils.isNotBlank(boundTicketId) && !boundTicketId.equals(approvalTicketId)) {
            log.warn("Approval task {} rejected: ticketId {} does not match the bound one",
                task.getApprovalTaskId(), approvalTicketId);
            throw new FailedPreconditionException(ErrorCode.APPROVAL_TICKET_ID_MISMATCH);
        }
    }

    /**
     * 【5】回查审批结论。<b>任何异常都 fail-closed</b>：保持 PENDING、拒绝放行，
     * 绝不能有"查不到就当通过"的兜底。
     */
    private ApprovalResult queryApprovalResult(ApprovalTaskDTO task, String approvalTicketId) {
        ApprovalChannel channel = channelRegistry.getChannelByName(task.getApprovalChannel());
        ApprovalResult result;
        try {
            result = channel.queryResult(task, approvalTicketId);
        } catch (Exception e) {
            log.error("Query approval result failed, approvalTaskId: {}, channel: {}, fail closed",
                task.getApprovalTaskId(), task.getApprovalChannel(), e);
            throw new FailedPreconditionException(ErrorCode.APPROVAL_CHANNEL_QUERY_FAIL);
        }
        if (result == null || result.getStatus() == null) {
            log.error("Query approval result returned empty result, approvalTaskId: {}, channel: {}, fail closed",
                task.getApprovalTaskId(), task.getApprovalChannel());
            throw new FailedPreconditionException(ErrorCode.APPROVAL_CHANNEL_QUERY_FAIL);
        }
        return result;
    }

    /**
     * 【8】首次通过校验后固化 ticketId。绑定冲突说明有并发请求先绑了别的单号，
     * 重新读取后回到【4】比对，不匹配即拒绝
     */
    private void bindTicketId(ApprovalTaskDTO task, String approvalTicketId, ApprovalCallerContext caller) {
        if (StringUtils.isNotBlank(task.getApprovalTicketId())) {
            return;
        }
        int bound = approvalTaskDAO.bindTicketIdIfAbsent(task.getApprovalTaskId(), approvalTicketId);
        if (bound != 1) {
            ApprovalTaskDTO latest = loadOwnTask(task.getApprovalTaskId(), caller);
            checkTicketIdMatch(latest, approvalTicketId);
        }
    }

    /**
     * 【9】从 DB 参数快照还原请求体，<b>参数只有这一个来源</b>。
     * <p>
     * appId 必须用任务的 app_id 覆盖：它是 {@code @JsonIgnore} 字段、快照里没有，
     * 而 scope 到 appId 的映射在任务有效期内可能变化。
     */
    private Object resolveParamsFromSnapshot(ApprovalTaskDTO task) {
        ApprovalOperationTypeEnum operationType = ApprovalOperationTypeEnum.valOf(task.getOperationType());
        Object params = paramsCryptoService.decryptFromSnapshot(operationType, task.getOperationParams());
        if (params instanceof EsbAppScopeReq) {
            ((EsbAppScopeReq) params).setAppId(task.getAppId());
        }
        return params;
    }

    /**
     * 用 {@link OperationExecutor#getParamsClass()} 完成类型收窄，类型不符属编码错误，直接失败
     */
    private <T> EsbV4Response<?> invokeExecutor(OperationExecutor<T> executor,
                                                Object params,
                                                ApprovalTaskDTO task,
                                                boolean dryRun) {
        Class<T> paramsClass = executor.getParamsClass();
        if (!paramsClass.isInstance(params)) {
            throw new IllegalArgumentException("Params type mismatch for operationType "
                + executor.getOperationType() + ", expect " + paramsClass.getName()
                + " but got " + (params == null ? "null" : params.getClass().getName()));
        }
        EsbV4Response<?> response = executor.invoke(paramsClass.cast(params), task, dryRun);
        if (response == null) {
            // 空响应属于结果未知，不能当成执行失败：调用方会据此决定能否落 FAILED 终态
            throw new InternalException("Empty response from downstream service", ErrorCode.INTERNAL_ERROR);
        }
        return response;
    }

    /**
     * 把过期这一惰性判断反映到对外呈现的状态上。<b>只改内存对象，不写库</b>
     */
    private ApprovalTaskDTO withPresentationStatus(ApprovalTaskDTO task, long now) {
        ApprovalStatusEnum status = task.getStatusEnum();
        boolean pendingButExpired = status == ApprovalStatusEnum.PENDING && task.isExpired(now);
        if (pendingButExpired) {
            task.setStatus(ApprovalStatusEnum.EXPIRED.name());
        }
        return task;
    }

    /**
     * 下游是否在产生任何副作用之前就明确拒绝了本次执行。
     * <p>
     * <b>只有"确定未执行"才允许落 FAILED 终态</b>：落了终态用户就会据此重新发起，
     * 把"结果未知"误判成"确定未执行"，同一个操作就可能被执行两次。因此这里只认下游给出的明确拒绝语义，
     * 其余一律当作未知。微服务下拒绝以 4xx 形态到达，轻量化部署下是被调服务原样抛出的异常
     */
    private boolean isRejectedBeforeExecute(Exception e) {
        if (e instanceof OpenApiPropagatedException) {
            return ((OpenApiPropagatedException) e).isRejectedByDownstream();
        }
        if (e instanceof PermissionDeniedException) {
            return true;
        }
        if (e instanceof ServiceException) {
            return REJECTED_BEFORE_EXECUTE_ERROR_TYPES.contains(((ServiceException) e).getErrorType());
        }
        return false;
    }

    private String buildErrorJson(Exception e) {
        Map<String, Object> error = new HashMap<>();
        if (e instanceof OpenApiPropagatedException) {
            EsbV4RespError respError = ((OpenApiPropagatedException) e).getError();
            if (respError != null) {
                error.put("errorCode", respError.getCode());
                error.put("errorMsg", respError.getMessage());
            }
        } else if (e instanceof ServiceException) {
            ServiceException serviceException = (ServiceException) e;
            error.put("errorCode", serviceException.getErrorCode());
            error.put("errorParams", serviceException.getErrorParams());
        }
        error.putIfAbsent("errorMsg", e.getMessage());
        return JsonUtils.toJson(error);
    }

    private int resolveTtlHours() {
        Integer ttlHours = approvalProperties.getTtlHours();
        return ttlHours == null || ttlHours <= 0 ? 8 : ttlHours;
    }
}
