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
import com.tencent.bk.job.analysis.approval.ApprovalMetrics;
import com.tencent.bk.job.analysis.approval.ApprovalParamsCryptoService;
import com.tencent.bk.job.analysis.approval.ApprovalTaskService;
import com.tencent.bk.job.analysis.approval.ApprovalTicketRenderer;
import com.tencent.bk.job.analysis.approval.channel.ApprovalChannel;
import com.tencent.bk.job.analysis.approval.channel.ApprovalChannelRegistry;
import com.tencent.bk.job.analysis.approval.channel.model.ApprovalResult;
import com.tencent.bk.job.analysis.approval.channel.model.ApprovalTicket;
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
import com.tencent.bk.job.common.api.model.ResolvedSummary;
import com.tencent.bk.job.common.api.util.DryRunResultUtil;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.util.JobUUID;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 审批任务核心编排。
 * <p>
 * <b>本类里最重要的东西是 {@link #refresh} 里那条校验链的顺序，以及"CAS 之前不写终态"这条不变式。</b>
 * 每一步都对应一种已识别的绕过手法，删掉任何一步都能让整套机制失效：
 * <ul>
 *     <li>不回查渠道 → 调用方自己说批了就算批了；</li>
 *     <li>不校验 approver == creator → 找个同事随手批一下即可；</li>
 *     <li>不校验回查响应回带的 approval_task_id → 拿另一个任务真实批过的单据即可放行任意任务；</li>
 *     <li>ticket 绑定后允许更换 → pending 期间可用不同单号反复试探；</li>
 *     <li>参数接受外部覆盖 → 批的是 A，执行的是 B；</li>
 *     <li>回查异常时兜底放行 → 把渠道抖动变成免审通道。</li>
 * </ul>
 * 修改本类前请先读 {@code develop_plan} §5 与 §5.2 的落态自查表。
 */
@Slf4j
@Service
public class ApprovalTaskServiceImpl implements ApprovalTaskService {

    private final ApprovalTaskDAO approvalTaskDAO;
    private final ApprovalChannelRegistry channelRegistry;
    private final OperationExecutorRegistry executorRegistry;
    private final ApprovalParamsCryptoService paramsCryptoService;
    private final ApprovalProperties approvalProperties;
    private final ApprovalTicketRenderer ticketRenderer;
    private final ApprovalMetrics approvalMetrics;
    private final ApprovalAuditor approvalAuditor;

    public ApprovalTaskServiceImpl(ApprovalTaskDAO approvalTaskDAO,
                                   ApprovalChannelRegistry channelRegistry,
                                   OperationExecutorRegistry executorRegistry,
                                   ApprovalParamsCryptoService paramsCryptoService,
                                   ApprovalProperties approvalProperties,
                                   ApprovalTicketRenderer ticketRenderer,
                                   ApprovalMetrics approvalMetrics,
                                   ApprovalAuditor approvalAuditor) {
        this.approvalTaskDAO = approvalTaskDAO;
        this.channelRegistry = channelRegistry;
        this.executorRegistry = executorRegistry;
        this.paramsCryptoService = paramsCryptoService;
        this.approvalProperties = approvalProperties;
        this.ticketRenderer = ticketRenderer;
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

        // 预检与放行执行走同一个 invoke，只有 dryRun 取值不同
        DryRunResult<?> dryRunResult = invokeExecutor(executor, params, task, true);
        if (!dryRunResult.isValid()) {
            log.info("Create approval task rejected by dry run, operationType: {}, errorCode: {}",
                operationType, dryRunResult.getErrorCode());
            throw DryRunResultUtil.toException(dryRunResult);
        }

        ResolvedSummary summary = dryRunResult.getResolvedSummary();
        if (summary == null) {
            summary = new ResolvedSummary();
        }
        // 操作类型由 job-analysis 侧填充：下游不复制审批域的操作类型枚举
        summary.setOperationType(operationType.name());
        task.setResolvedSummary(JsonUtils.toJson(summary));
        // 加密失败让异常向上传播，绝不降级为明文落库
        task.setOperationParams(
            paramsCryptoService.encryptSensitiveFields(operationType, JsonUtils.toJson(params)));

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

        // 【7】绑定证明：回查响应回带的 approval_task_id 必须与请求一致。
        // 缺了这一步，拿另一个任务真实批过的单据就能放行本任务
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
    public ApprovalTicket getTicket(String approvalTaskId, String tenantId, String appCode) {
        ApprovalTaskDTO task = approvalTaskDAO.getByApprovalTaskId(approvalTaskId);
        if (task == null || !isAssignedChannelCaller(task, tenantId, appCode)) {
            throw new NotFoundException(ErrorCode.APPROVAL_TASK_NOT_EXIST, new Object[]{approvalTaskId});
        }
        ApprovalTicket ticket = ticketRenderer.render(task);
        try {
            // 仅观测：写失败不影响取单，也不影响放行（放行不校验 ticket_fetched_at，
            // 其观测价值由 ApprovalMetrics 的两个代理指标兑现）
            approvalTaskDAO.updateTicketFetchedAt(approvalTaskId, System.currentTimeMillis());
        } catch (Exception e) {
            log.warn("Update ticketFetchedAt failed, approvalTaskId: {}", approvalTaskId, e);
        }
        return ticket;
    }

    /**
     * 取单方必须是该任务指派的渠道本身。
     * <p>
     * 租户与渠道 appCode 都必须严格相等，任一不符按"任务不存在"处理：
     * 单据里有脚本明文，若只校验网关权限，任何被授予取单接口权限的应用都能枚举他人的单据内容。
     */
    private boolean isAssignedChannelCaller(ApprovalTaskDTO task, String tenantId, String appCode) {
        if (!Objects.equals(task.getTenantId(), tenantId)) {
            log.warn("Get approval ticket rejected: tenant mismatch, approvalTaskId: {}", task.getApprovalTaskId());
            return false;
        }
        String channelAppCode = channelRegistry.getChannelAppCode(task.getApprovalChannel());
        if (StringUtils.isBlank(channelAppCode) || !channelAppCode.equals(appCode)) {
            log.warn("Get approval ticket rejected: appCode {} is not the assigned channel {} of task {}",
                appCode, task.getApprovalChannel(), task.getApprovalTaskId());
            return false;
        }
        return true;
    }

    /**
     * 【11】打点后下发，【12】按下游结果落态。
     * <p>
     * <b>三种落法只有两种会写状态</b>：明确的业务失败落 FAILED；结果未知（超时、连接中断、5xx）
     * 一律停在 EXECUTING 且 execute_result 为空，<b>不重试</b>。因为 CAS 之后
     * "下游其实执行成功了但响应丢了"与"下游确实没执行"在这里无法区分，重试就等于赌一次重复执行。
     * <p>
     * 正因为不重试，下游<b>不需要</b>以 approval_task_id 做幂等去重。
     * <b>将来若放开重试，必须同时在下游引入以 approval_task_id 为幂等键的去重</b>，
     * 否则重复执行的口子立刻就开了。
     */
    private ApprovalTaskDTO executeAfterConsume(ApprovalTaskDTO task, Object params, ApprovalCallerContext caller) {
        String approvalTaskId = task.getApprovalTaskId();
        ApprovalOperationTypeEnum operationType = ApprovalOperationTypeEnum.valOf(task.getOperationType());
        OperationExecutor<?> executor = executorRegistry.getExecutor(operationType);
        approvalTaskDAO.markDispatched(approvalTaskId, System.currentTimeMillis());
        // 埋在放行成功路径上、且在下发之前：下发结果如何都不影响"这一单已被放行"这个事实，
        // 漏计会让"从未取单就放行""秒批"两类异常彻底不可见
        approvalMetrics.recordDispatched(task);

        DryRunResult<?> result;
        try {
            // dryRun=false 正式执行；operator 取 DB 中的 creator，且不得 skipAuth
            result = invokeExecutor(executor, params, task, false);
        } catch (Exception e) {
            // 结果未知：保持 EXECUTING、execute_result 为空，转人工排查
            log.error("Approval task {} dispatched but result is unknown, keep EXECUTING and DO NOT retry",
                approvalTaskId, e);
            return loadOwnTask(approvalTaskId, caller);
        }

        if (!result.isValid()) {
            // 下游明确的业务失败：确定未执行，落 FAILED 终态，用户需重新发起审批
            log.warn("Approval task {} execute failed with errorCode {}", approvalTaskId, result.getErrorCode());
            approvalTaskDAO.markFailed(approvalTaskId, buildErrorJson(result));
            throw DryRunResultUtil.toException(result);
        }
        Object executeResult = result.getExecuteResult();
        approvalTaskDAO.markExecuted(approvalTaskId, executeResult == null ? null : JsonUtils.toJson(executeResult));
        log.info("Approval task {} executed", approvalTaskId);
        ApprovalTaskDTO executedTask = loadOwnTask(approvalTaskId, caller);
        // 放行审计带上审批人与审批时间：审计侧只有这一条事件能回答"这次执行是谁批的"
        approvalAuditor.auditRelease(executedTask);
        return executedTask;
    }

    /**
     * 【0】读取并校验归属。
     * <p>
     * tenantId / appId / creator / appCode 四者都必须与调用上下文一致，任一不符都返回"任务不存在"
     * —— <b>不区分"不存在"与"无权访问"</b>，否则 approval_task_id 就成了探测他人任务的工具。
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
        // appId 只在调用上下文里有值时才比对：流转接口的请求体不带资源范围（app_id 只以 DB 为准），
        // 这时无条件比对 appId 会让所有流转请求都被判成"任务不存在"。
        // 归属仍由 tenant_id / creator / app_code 三者共同保证，creator 已经锚定到具体的人
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
     * 【5】回查审批结论。<b>任何异常都 fail-closed</b>：保持 PENDING、拒绝放行。
     * <p>
     * 这里绝不能有"查不到就当通过"或"渠道抖动就放行"的兜底 —— 那等于把渠道故障变成免审通道。
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
     * 【9】从 DB 参数快照还原请求体。
     * <p>
     * <b>参数只有这一个来源</b>：解密、反序列化，然后用任务的 app_id 覆盖资源范围。覆盖是必需的 ——
     * appId 是 {@code @JsonIgnore} 字段、快照里根本没有，而 scope 到 appId 的映射在 8 小时内可能变化，
     * 以 DB 的 app_id 为准才能保证不会落到别的业务上。
     */
    private Object resolveParamsFromSnapshot(ApprovalTaskDTO task) {
        ApprovalOperationTypeEnum operationType = ApprovalOperationTypeEnum.valOf(task.getOperationType());
        OperationExecutor<?> executor = executorRegistry.getExecutor(operationType);
        String paramsJson = paramsCryptoService.decryptSensitiveFields(operationType, task.getOperationParams());
        Object params = JsonUtils.fromJson(paramsJson, executor.getParamsClass());
        if (params instanceof EsbAppScopeReq) {
            ((EsbAppScopeReq) params).setAppId(task.getAppId());
        }
        return params;
    }

    /**
     * 用 {@link OperationExecutor#getParamsClass()} 完成类型收窄：类型不符说明分发表与快照对不上，
     * 属于编码错误，直接失败而不是强转出 ClassCastException
     */
    private <T> DryRunResult<?> invokeExecutor(OperationExecutor<T> executor,
                                               Object params,
                                               ApprovalTaskDTO task,
                                               boolean dryRun) {
        Class<T> paramsClass = executor.getParamsClass();
        if (!paramsClass.isInstance(params)) {
            throw new IllegalArgumentException("Params type mismatch for operationType "
                + executor.getOperationType() + ", expect " + paramsClass.getName()
                + " but got " + (params == null ? "null" : params.getClass().getName()));
        }
        return executor.invoke(paramsClass.cast(params), task, dryRun);
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

    private String buildErrorJson(DryRunResult<?> result) {
        Map<String, Object> error = new HashMap<>();
        error.put("errorCode", result.getErrorCode());
        error.put("errorParams", result.getErrorParams());
        return JsonUtils.toJson(error);
    }

    private int resolveTtlHours() {
        Integer ttlHours = approvalProperties.getTtlHours();
        return ttlHours == null || ttlHours <= 0 ? 8 : ttlHours;
    }
}
