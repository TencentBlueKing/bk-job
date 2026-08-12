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
import com.tencent.bk.job.analysis.model.esb.v4.req.V4WithApprovalRequest;
import com.tencent.bk.job.analysis.model.esb.v4.resp.V4ApprovalTaskCreatedDTO;
import com.tencent.bk.job.analysis.model.esb.v4.resp.V4ApprovalTaskDTO;
import com.tencent.bk.job.analysis.model.esb.v4.resp.V4ApprovalTicketDTO;
import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 审批相关 v4 接口的公共装配逻辑。
 * <p>
 * 6 个发起接口 + 3 个流转接口只在"操作类型"和"请求体类型"上不同，调用上下文构造、DTO 转换、
 * 状态文案这些东西没有第二份实现的理由 —— 各 Impl 因此只是薄薄一层。
 */
@Slf4j
@Component
public class ApprovalV4ApiSupport {

    private static final String I18N_MESSAGE_PREFIX = "task.approval.message.";

    private final AppScopeMappingService appScopeMappingService;
    private final ApprovalTaskService approvalTaskService;
    private final MessageI18nService i18nService;

    public ApprovalV4ApiSupport(AppScopeMappingService appScopeMappingService,
                                ApprovalTaskService approvalTaskService,
                                MessageI18nService i18nService) {
        this.appScopeMappingService = appScopeMappingService;
        this.approvalTaskService = approvalTaskService;
        this.i18nService = i18nService;
    }

    /**
     * 发起审批。
     * <p>
     * 显式调用 {@code fillAppResourceScope}，不假定 {@code EsbAppResourceScopeReqAspect} 一定生效；
     * 渠道只接受枚举值，地址与密钥一律来自服务端配置。
     *
     * @param operationType 操作类型
     * @param request       原操作的 v4 请求体（带审批渠道）
     * @param appCode       调用方应用编码
     * @return 审批任务创建结果，<b>不含任何操作执行结果</b>
     */
    public <T extends EsbAppScopeReq & V4WithApprovalRequest> EsbV4Response<V4ApprovalTaskCreatedDTO> initiate(
        ApprovalOperationTypeEnum operationType,
        T request,
        String appCode
    ) {
        request.fillAppResourceScope(appScopeMappingService);
        ApprovalCallerContext caller = initiateCaller(request.getAppId(), appCode);
        ApprovalTaskDTO task = approvalTaskService.create(
            operationType,
            request,
            ApprovalChannelEnum.valOf(request.getApprovalChannel()),
            caller
        );
        return EsbV4Response.success(toCreatedDTO(task));
    }

    /**
     * 发起接口的调用上下文：appId 来自本次请求解析出的资源范围
     */
    private ApprovalCallerContext initiateCaller(Long appId, String appCode) {
        return ApprovalCallerContext.builder()
            .tenantId(JobContextUtil.getTenantId())
            .appId(appId)
            .username(JobContextUtil.getUsername())
            .appCode(appCode)
            .build();
    }

    /**
     * 流转接口的调用上下文。
     * <p>
     * <b>appId 只能为空</b>：流转接口的请求体不继承 {@code EsbAppScopeReq}（审批任务的 app_id 只以 DB
     * 为准、不接受入参覆盖），因此本次请求根本没有资源范围可解析。归属由 tenant_id / creator / app_code
     * 三者比对，其中 creator 已经锚定到具体的人，appId 再比也不会更严格。
     */
    public ApprovalCallerContext workflowCaller(String appCode) {
        return ApprovalCallerContext.builder()
            .tenantId(JobContextUtil.getTenantId())
            .username(JobContextUtil.getUsername())
            .appCode(appCode)
            .build();
    }

    public V4ApprovalTaskCreatedDTO toCreatedDTO(ApprovalTaskDTO task) {
        V4ApprovalTaskCreatedDTO createdDTO = new V4ApprovalTaskCreatedDTO();
        createdDTO.setApprovalTaskId(task.getApprovalTaskId());
        // 渠道取单必须带上租户，否则多租户环境下会在网关层直接 401
        createdDTO.setTenantId(task.getTenantId());
        createdDTO.setStatus(task.getStatus());
        createdDTO.setApprovalChannel(task.getApprovalChannel());
        createdDTO.setExpireAt(task.getExpireAt());
        return createdDTO;
    }

    /**
     * 流转接口的返回体装配。
     * <p>
     * {@code result_unknown} 与 {@code message} 是这里唯一的"计算"：状态本身已由 Service 给出呈现值
     */
    public V4ApprovalTaskDTO toTaskDTO(ApprovalTaskDTO task) {
        V4ApprovalTaskDTO taskDTO = new V4ApprovalTaskDTO();
        taskDTO.setApprovalTaskId(task.getApprovalTaskId());
        taskDTO.setStatus(task.getStatus());
        taskDTO.setOperationType(task.getOperationType());
        taskDTO.setApprovalChannel(task.getApprovalChannel());
        taskDTO.setApprovalTicketId(task.getApprovalTicketId());
        taskDTO.setCreator(task.getCreator());
        taskDTO.setCreateTime(task.getCreateTime());
        taskDTO.setExpireAt(task.getExpireAt());
        taskDTO.setApprover(task.getApprover());
        taskDTO.setApprovedAt(task.getApprovedAt());
        taskDTO.setExecuteResult(parseExecuteResult(task));
        boolean resultUnknown = isResultUnknown(task);
        taskDTO.setResultUnknown(resultUnknown);
        taskDTO.setMessage(resolveMessage(task, resultUnknown));
        return taskDTO;
    }

    /**
     * 已下发但没拿到下游响应。
     * <p>
     * 必须同时满足"已下发"（dispatched_at 不为空）：CAS 成功后崩在下发之前的任务同样停在 EXECUTING，
     * 但那种情况下作业确定没产生，让用户去翻执行历史是白费功夫
     */
    private boolean isResultUnknown(ApprovalTaskDTO task) {
        return ApprovalStatusEnum.EXECUTING == ApprovalStatusEnum.valOf(task.getStatus())
            && StringUtils.isBlank(task.getExecuteResult())
            && task.getDispatchedAt() != null;
    }

    private Object parseExecuteResult(ApprovalTaskDTO task) {
        if (StringUtils.isBlank(task.getExecuteResult())) {
            return null;
        }
        try {
            return JsonUtils.fromJson(task.getExecuteResult(), Object.class);
        } catch (Exception e) {
            log.warn("Parse execute result failed, approvalTaskId: {}", task.getApprovalTaskId(), e);
            return null;
        }
    }

    /**
     * 面向用户的可读说明。EXECUTING 分两种说法：已下发结果未知让用户去执行历史确认，
     * 未下发则只让用户联系管理员排查
     */
    private String resolveMessage(ApprovalTaskDTO task, boolean resultUnknown) {
        ApprovalStatusEnum status = ApprovalStatusEnum.valOf(task.getStatus());
        if (status == null) {
            return StringUtils.EMPTY;
        }
        String key;
        switch (status) {
            case PENDING:
                key = "pending";
                break;
            case EXPIRED:
                key = "expired";
                break;
            case REJECTED:
                key = "rejected";
                break;
            case CANCELED:
                key = "canceled";
                break;
            case EXECUTED:
                key = "executed";
                break;
            case FAILED:
                key = "failed";
                break;
            case EXECUTING:
                key = resultUnknown ? "resultUnknown" : "notDispatched";
                break;
            default:
                return StringUtils.EMPTY;
        }
        return i18nService.getI18n(I18N_MESSAGE_PREFIX + key);
    }

    public V4ApprovalTicketDTO toTicketDTO(ApprovalTicket ticket) {
        V4ApprovalTicketDTO ticketDTO = new V4ApprovalTicketDTO();
        ticketDTO.setApprovalTaskId(ticket.getApprovalTaskId());
        ticketDTO.setTitle(ticket.getTitle());
        ticketDTO.setRiskLevel(ticket.getRiskLevel());
        ticketDTO.setOperationType(ticket.getOperationType());
        ResourceScope scope = ticket.getScope();
        if (scope != null) {
            ticketDTO.setScopeType(scope.getType() == null ? null : scope.getType().getValue());
            ticketDTO.setScopeId(scope.getId());
        }
        ticketDTO.setCreator(ticket.getCreator());
        ticketDTO.setExpireAt(ticket.getExpireAt());
        // 单据正文在渲染阶段就已完成脱敏，这里原样透传，不做任何还原
        ticketDTO.setApprovalContent(ticket.getApprovalContent());
        return ticketDTO;
    }
}
