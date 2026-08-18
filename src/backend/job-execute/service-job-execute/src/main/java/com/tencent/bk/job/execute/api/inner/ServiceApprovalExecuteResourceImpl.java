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

package com.tencent.bk.job.execute.api.inner;

import com.tencent.bk.job.common.api.model.DryRunResult;
import com.tencent.bk.job.common.model.ResolvedSummary;
import com.tencent.bk.job.common.api.util.DryRunResultUtil;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.error.ErrorType;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.tenant.TenantService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.execute.common.constants.FileTransferModeEnum;
import com.tencent.bk.job.execute.model.FastTaskDTO;
import com.tencent.bk.job.execute.model.TaskExecuteParam;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4ExecuteJobPlanRequest;
import com.tencent.bk.job.execute.model.esb.v4.req.V4FastTransferFileRequest;
import com.tencent.bk.job.execute.model.esb.v4.resp.V4JobExecuteDTO;
import com.tencent.bk.job.execute.model.inner.request.ServiceApprovalExecuteJobPlanRequest;
import com.tencent.bk.job.execute.model.inner.request.ServiceApprovalFastTransferFileRequest;
import com.tencent.bk.job.execute.service.ResolvedSummaryBuilder;
import com.tencent.bk.job.execute.service.TaskExecuteService;
import com.tencent.bk.job.execute.service.V4ExecuteJobPlanRequestConverter;
import com.tencent.bk.job.execute.service.V4FastTransferFileRequestConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Validator;

@RestController
@Slf4j
public class ServiceApprovalExecuteResourceImpl implements ServiceApprovalExecuteResource {

    private final TaskExecuteService taskExecuteService;

    private final V4FastTransferFileRequestConverter fastTransferFileRequestConverter;

    private final AppScopeMappingService appScopeMappingService;

    private final TenantService tenantService;

    /**
     * inner 路径不经过网关，Spring MVC 的自动校验也不会作用于 @RequestBody 内嵌的 v4 请求体，
     * 必须显式持有 Validator 手动跑一次，否则 v4 请求体上的注解约束在审批预检时全部失效
     */
    private final Validator validator;

    @Autowired
    public ServiceApprovalExecuteResourceImpl(TaskExecuteService taskExecuteService,
                                              V4FastTransferFileRequestConverter fastTransferFileRequestConverter,
                                              AppScopeMappingService appScopeMappingService,
                                              TenantService tenantService,
                                              Validator validator) {
        this.taskExecuteService = taskExecuteService;
        this.fastTransferFileRequestConverter = fastTransferFileRequestConverter;
        this.appScopeMappingService = appScopeMappingService;
        this.tenantService = tenantService;
        this.validator = validator;
    }

    @Override
    public InternalResponse<DryRunResult<V4JobExecuteDTO>> fastTransferFile(
        ServiceApprovalFastTransferFileRequest request) {

        log.info("Approval fast transfer file, operator: {}, dryRun: {}", request.getOperator(), request.isDryRun());
        V4FastTransferFileRequest v4Request = request.getRequest();
        DryRunResult<V4JobExecuteDTO> paramCheckResult = checkWrapper(v4Request, request.getOperator());
        if (paramCheckResult != null) {
            return InternalResponse.buildSuccessResp(paramCheckResult);
        }
        return InternalResponse.buildSuccessResp(DryRunResultUtil.call(v4Request, validator, () -> {
            User operator = prepareOperator(v4Request, request.getOperator());
            FastTaskDTO fastTask = fastTransferFileRequestConverter.convert(
                v4Request, operator, request.getAppCode(), request.isDryRun());
            TaskInstanceDTO taskInstance = taskExecuteService.executeFastTask(fastTask);
            if (!request.isDryRun()) {
                return DryRunResult.valid(null, buildFastTaskResult(fastTask));
            }
            ResolvedSummary summary = ResolvedSummaryBuilder.build(taskInstance);
            fillTimeoutDefault(summary, v4Request.getTimeout());
            fillTransferModeDefault(summary, v4Request.getTransferMode());
            return DryRunResult.valid(summary, null);
        }));
    }

    @Override
    public InternalResponse<DryRunResult<V4JobExecuteDTO>> executeJobPlan(
        ServiceApprovalExecuteJobPlanRequest request) {

        log.info("Approval execute job plan, operator: {}, dryRun: {}", request.getOperator(), request.isDryRun());
        V4ExecuteJobPlanRequest v4Request = request.getRequest();
        DryRunResult<V4JobExecuteDTO> paramCheckResult = checkWrapper(v4Request, request.getOperator());
        if (paramCheckResult != null) {
            return InternalResponse.buildSuccessResp(paramCheckResult);
        }
        return InternalResponse.buildSuccessResp(DryRunResultUtil.call(v4Request, validator, () -> {
            User operator = prepareOperator(v4Request, request.getOperator());
            TaskExecuteParam executeParam = V4ExecuteJobPlanRequestConverter.convert(
                v4Request, operator, request.getAppCode(), request.isDryRun());
            TaskInstanceDTO taskInstance = taskExecuteService.executeJobPlan(executeParam);
            if (!request.isDryRun()) {
                return DryRunResult.valid(null, buildJobPlanResult(taskInstance));
            }
            return DryRunResult.valid(ResolvedSummaryBuilder.build(taskInstance), null);
        }));
    }

    /**
     * 校验包装体自身的必填项。包装体字段不参与 v4 请求体的注解校验，只能在这里显式检查
     *
     * @return 校验通过返回 null
     */
    private <T> DryRunResult<T> checkWrapper(EsbAppScopeReq v4Request, String operator) {
        if (v4Request == null) {
            return invalidParam("request");
        }
        if (StringUtils.isBlank(operator)) {
            // 操作人缺失绝不能兜底成当前调用方：下游会以该身份鉴权并执行
            return invalidParam("operator");
        }
        return null;
    }

    private <T> DryRunResult<T> invalidParam(String paramName) {
        return DryRunResult.invalid(
            ErrorCode.MISSING_PARAM_WITH_PARAM_NAME,
            new Object[]{paramName},
            ErrorType.INVALID_PARAM.getType()
        );
    }

    /**
     * 补全资源范围并构造操作人。
     * <p>
     * ESB 路径下 appId 由 EsbAppResourceScopeReqAspect 按请求路径切面补全、操作人由网关鉴权后注入，
     * 两者在 inner 路径都不生效，必须在此显式完成，否则转换器拿到的 appId 为 null。
     * 租户由 appId 反查而非取调用方传值，避免上游传错租户导致越权。
     */
    private User prepareOperator(EsbAppScopeReq v4Request, String operator) {
        v4Request.fillAppResourceScope(appScopeMappingService);
        if (v4Request.getAppId() == null) {
            throw new InvalidParamException(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME,
                new Object[]{"bk_scope_type|bk_scope_id"});
        }
        String tenantId = tenantService.getTenantIdByAppId(v4Request.getAppId());
        User user = new User(tenantId, operator, operator);
        JobContextUtil.setUser(user);
        return user;
    }

    private V4JobExecuteDTO buildFastTaskResult(FastTaskDTO fastTask) {
        V4JobExecuteDTO result = new V4JobExecuteDTO();
        result.setTaskInstanceId(fastTask.getTaskInstance().getId());
        result.setStepInstanceId(fastTask.getStepInstance().getId());
        result.setTaskName(fastTask.getTaskInstance().getName());
        return result;
    }

    private V4JobExecuteDTO buildJobPlanResult(TaskInstanceDTO taskInstance) {
        V4JobExecuteDTO result = new V4JobExecuteDTO();
        result.setTaskInstanceId(taskInstance.getId());
        result.setTaskName(taskInstance.getName());
        return result;
    }

    private void fillTimeoutDefault(ResolvedSummary summary, Integer timeout) {
        if (timeout == null) {
            summary.addDefaultApplied("timeout", JobConstants.DEFAULT_JOB_TIMEOUT_SECONDS + "s");
        }
    }

    /**
     * 不传或传非法值都会落到强制模式：目标路径不存在时自动建目录、同名文件直接覆盖。
     * 后果远大于严格模式，必须在单据里标成"按默认生效"，不能让审批人以为用户显式选过
     */
    private void fillTransferModeDefault(ResolvedSummary summary, Integer transferMode) {
        if (FileTransferModeEnum.getFileTransferModeEnum(transferMode) != FileTransferModeEnum.STRICT) {
            summary.addDefaultApplied("transfer_mode", FileTransferModeEnum.FORCE.name());
        }
    }
}
