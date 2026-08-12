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

package com.tencent.bk.job.analysis.approval;

import com.tencent.bk.audit.context.ActionAuditContext;
import com.tencent.bk.audit.context.AuditContext;
import com.tencent.bk.job.analysis.approval.consts.ApprovalOperationTypeEnum;
import com.tencent.bk.job.analysis.model.dto.ApprovalTaskDTO;
import com.tencent.bk.job.common.api.model.ResolvedSummary;
import com.tencent.bk.job.common.audit.JobAuditAttributeNames;
import com.tencent.bk.job.common.audit.JobAuditExtendDataKeys;
import com.tencent.bk.job.common.audit.constants.EventContentConstants;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.model.BasicApp;
import com.tencent.bk.job.common.service.CommonAppService;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 审批链路的审计事件产出。
 * <p>
 * <b>只在审批通过并放行时产出一条事件</b>：这条事件同时回答了"谁发起、谁审批、批的是什么操作、
 * 放行结果如何"，是审计侧唯一需要的答案。发起、驳回、作废都不曾真正改变系统，为它们各记一条
 * 只会把真正重要的放行事件淹没在噪声里。
 * <p>
 * <b>scope 必须显式设置</b>：流转接口（get / refresh / cancel）的请求体不继承 EsbAppScopeReq，
 * {@code JobContextUtil.getApp()} 为空，{@code AddResourceScopeAuditPostFilter} 补 scope 会失效。
 * 因此这里一律用审批任务 DB 里的 app_id 反查 scope 并直接写进事件。
 * <p>
 * <b>沿用各操作原有的 actionId</b>（不新增独立 ActionId），审批本身不构成新的权限点，
 * 其安全性依赖网关侧的授权收口。actionId 由任务的 operation_type 在运行时决定，
 * 因此接入的 v4 Resource 上 {@code @AuditEntry} <b>不要写死 actionId</b> ——
 * 一个流转接口要服务 6 种操作类型，写死会让其余 5 种的事件被 SDK 按"不可记录"丢弃。
 */
@Slf4j
@Component
public class ApprovalAuditor {

    /**
     * 概要中标识"保存定时任务"是新建还是更新的字段名，与 job-crontab 侧 dryRun 填充的 label 一致
     */
    private static final String SUMMARY_FIELD_OPERATION = "operation";

    private static final String OPERATION_UPDATE = "UPDATE";

    private final CommonAppService appService;

    public ApprovalAuditor(CommonAppService appService) {
        this.appService = appService;
    }

    /**
     * 放行：把"谁发起、谁审批、何时审批、放行结果"记在同一条事件上。
     * <p>
     * 事件在操作完成之后补记，而不是包裹操作本身：审批任务的状态已经落库，
     * 失败路径由请求层的 {@code @AuditEntry} 记录错误结果。<b>审计失败绝不影响业务结果</b>。
     */
    public void auditRelease(ApprovalTaskDTO task) {
        try {
            ResolvedSummary summary = parseSummary(task);
            String actionId = resolveActionId(task, summary);
            if (actionId == null) {
                log.warn("No audit actionId for operationType {}, skip audit", task.getOperationType());
                return;
            }
            ApprovalOperationTypeEnum operationType = ApprovalOperationTypeEnum.valOf(task.getOperationType());
            ActionAuditContext auditContext = ActionAuditContext.builder(actionId)
                .setContent(EventContentConstants.RELEASE_APPROVAL)
                .setInstanceId(task.getApprovalTaskId())
                .setInstanceName(instanceName(summary))
                .addAttribute(JobAuditAttributeNames.APPROVAL_TASK_ID, task.getApprovalTaskId())
                .addAttribute(JobAuditAttributeNames.APPROVAL_OPERATION_TYPE,
                    operationType == null ? StringUtils.EMPTY : operationType.name())
                .addAttribute(JobAuditAttributeNames.APPROVAL_CHANNEL,
                    StringUtils.defaultString(task.getApprovalChannel()))
                .build();
            fillScope(auditContext, task.getAppId());
            auditContext.addExtendData(JobAuditExtendDataKeys.APPROVAL_TASK_ID, task.getApprovalTaskId());
            AuditContext.current().updateActionId(actionId);
            auditContext.wrapActionRunnable(() -> {
                auditContext.addAttribute(JobAuditAttributeNames.APPROVER,
                    StringUtils.defaultString(task.getApprover()));
                auditContext.addAttribute(JobAuditAttributeNames.APPROVAL_RESULT,
                    StringUtils.defaultString(task.getStatus()));
                auditContext.addExtendData(JobAuditExtendDataKeys.APPROVAL_APPROVED_AT, task.getApprovedAt());
                auditContext.addExtendData(JobAuditExtendDataKeys.APPROVAL_EXECUTE_RESULT, task.getExecuteResult());
            }).run();
        } catch (Exception e) {
            log.warn("Record approval audit event failed, approvalTaskId: {}", task.getApprovalTaskId(), e);
        }
    }

    /**
     * 用任务的 app_id 反查 scope 并显式写入事件
     */
    private void fillScope(ActionAuditContext auditContext, Long appId) {
        if (appId == null) {
            return;
        }
        try {
            BasicApp app = appService.getApp(appId);
            if (app != null && app.getScope() != null) {
                auditContext.setScopeType(app.getScope().getType().getValue());
                auditContext.setScopeId(app.getScope().getId());
            }
        } catch (Exception e) {
            log.warn("Get scope for approval audit event failed, appId: {}", appId, e);
        }
    }

    /**
     * 沿用各操作原有的 actionId。保存定时任务按新建/更新分别对应 CREATE_CRON / MANAGE_CRON，
     * 与 job-crontab 侧既有写法一致
     */
    private String resolveActionId(ApprovalTaskDTO task, ResolvedSummary summary) {
        ApprovalOperationTypeEnum operationType = ApprovalOperationTypeEnum.valOf(task.getOperationType());
        if (operationType == null) {
            return null;
        }
        switch (operationType) {
            case FAST_EXECUTE_SCRIPT:
                return ActionId.QUICK_EXECUTE_SCRIPT;
            case FAST_TRANSFER_FILE:
                return ActionId.QUICK_TRANSFER_FILE;
            case EXECUTE_JOB_PLAN:
                return ActionId.LAUNCH_JOB_PLAN;
            case CREATE_JOB_PLAN:
                return ActionId.CREATE_JOB_PLAN;
            case SAVE_CRON:
                return isUpdateCron(summary) ? ActionId.MANAGE_CRON : ActionId.CREATE_CRON;
            case UPDATE_CRON_STATUS:
                return ActionId.MANAGE_CRON;
            default:
                return null;
        }
    }

    private boolean isUpdateCron(ResolvedSummary summary) {
        if (summary == null || summary.getFields() == null) {
            return false;
        }
        for (ResolvedSummary.ResolvedField field : summary.getFields()) {
            if (SUMMARY_FIELD_OPERATION.equals(field.getLabel())) {
                return OPERATION_UPDATE.equals(field.getValue());
            }
        }
        return false;
    }

    private String instanceName(ResolvedSummary summary) {
        return summary == null ? StringUtils.EMPTY : StringUtils.defaultString(summary.getName());
    }

    private ResolvedSummary parseSummary(ApprovalTaskDTO task) {
        if (StringUtils.isBlank(task.getResolvedSummary())) {
            return null;
        }
        try {
            return JsonUtils.fromJson(task.getResolvedSummary(), ResolvedSummary.class);
        } catch (Exception e) {
            log.warn("Parse resolved summary for audit failed, approvalTaskId: {}", task.getApprovalTaskId(), e);
            return null;
        }
    }
}
