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

package com.tencent.bk.job.crontab.api.inner.impl;

import com.tencent.bk.job.common.api.model.DryRunResult;
import com.tencent.bk.job.common.model.ResolvedSummary;
import com.tencent.bk.job.common.api.util.DryRunResultUtil;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.error.ErrorType;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.tenant.TenantService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.crontab.api.inner.ServiceApprovalCronResource;
import com.tencent.bk.job.crontab.common.constants.CronStatusEnum;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.model.esb.v4.req.V4SaveCronRequest;
import com.tencent.bk.job.crontab.model.esb.v4.req.V4UpdateCronStatusRequest;
import com.tencent.bk.job.crontab.model.esb.v4.resp.V4CronJobDTO;
import com.tencent.bk.job.crontab.model.inner.request.ServiceApprovalSaveCronRequest;
import com.tencent.bk.job.crontab.model.inner.request.ServiceApprovalUpdateCronStatusRequest;
import com.tencent.bk.job.crontab.service.CronJobService;
import com.tencent.bk.job.crontab.service.V4SaveCronRequestConverter;
import com.tencent.bk.job.crontab.service.V4UpdateCronStatusRequestConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Validator;

@RestController
@Slf4j
public class ServiceApprovalCronResourceImpl implements ServiceApprovalCronResource {

    private final CronJobService cronJobService;

    private final V4SaveCronRequestConverter saveCronRequestConverter;

    private final AppScopeMappingService appScopeMappingService;

    private final TenantService tenantService;

    /**
     * inner 路径不经过网关，Spring MVC 的自动校验也不会作用于 @RequestBody 内嵌的 v4 请求体，
     * 必须显式持有 Validator 手动跑一次，否则 v4 请求体上的注解约束在审批预检时全部失效
     */
    private final Validator validator;

    @Autowired
    public ServiceApprovalCronResourceImpl(CronJobService cronJobService,
                                           V4SaveCronRequestConverter saveCronRequestConverter,
                                           AppScopeMappingService appScopeMappingService,
                                           TenantService tenantService,
                                           Validator validator) {
        this.cronJobService = cronJobService;
        this.saveCronRequestConverter = saveCronRequestConverter;
        this.appScopeMappingService = appScopeMappingService;
        this.tenantService = tenantService;
        this.validator = validator;
    }

    @Override
    public InternalResponse<DryRunResult<V4CronJobDTO>> saveCron(ServiceApprovalSaveCronRequest request) {
        log.info("Approval save cron, operator: {}, dryRun: {}", request.getOperator(), request.isDryRun());
        V4SaveCronRequest v4Request = request.getRequest();
        DryRunResult<V4CronJobDTO> paramCheckResult = checkWrapper(v4Request, request.getOperator());
        if (paramCheckResult != null) {
            return InternalResponse.buildSuccessResp(paramCheckResult);
        }
        return InternalResponse.buildSuccessResp(DryRunResultUtil.call(v4Request, validator, () -> {
            User operator = prepareOperator(v4Request, request.getOperator());
            CronJobInfoDTO cronJobInfo = saveCronRequestConverter.convert(v4Request, operator);
            boolean update = V4SaveCronRequestConverter.isUpdate(v4Request);
            if (request.isDryRun()) {
                CronJobInfoDTO checked = update
                    ? cronJobService.dryRunUpdateCronJobInfo(operator, cronJobInfo)
                    : cronJobService.dryRunCreateCronJobInfo(operator, cronJobInfo);
                return DryRunResult.valid(buildSaveCronSummary(checked, update), null);
            }
            CronJobInfoDTO saved = update
                ? cronJobService.updateCronJobInfo(operator, cronJobInfo)
                : cronJobService.createCronJobInfo(operator, cronJobInfo);
            return DryRunResult.valid(null, buildCronJobResult(saved));
        }));
    }

    @Override
    public InternalResponse<DryRunResult<V4CronJobDTO>> updateCronStatus(
        ServiceApprovalUpdateCronStatusRequest request) {

        log.info("Approval update cron status, operator: {}, dryRun: {}", request.getOperator(), request.isDryRun());
        V4UpdateCronStatusRequest v4Request = request.getRequest();
        DryRunResult<V4CronJobDTO> paramCheckResult = checkWrapper(v4Request, request.getOperator());
        if (paramCheckResult != null) {
            return InternalResponse.buildSuccessResp(paramCheckResult);
        }
        return InternalResponse.buildSuccessResp(DryRunResultUtil.call(v4Request, validator, () -> {
            User operator = prepareOperator(v4Request, request.getOperator());
            boolean enable = V4UpdateCronStatusRequestConverter.convertToEnable(v4Request);
            Long appId = v4Request.getAppId();
            if (request.isDryRun()) {
                cronJobService.dryRunChangeCronJobEnableStatus(operator, appId, v4Request.getId(), enable);
                return DryRunResult.valid(buildUpdateStatusSummary(appId, v4Request.getId(), enable), null);
            }
            cronJobService.changeCronJobEnableStatus(operator, appId, v4Request.getId(), enable);
            V4CronJobDTO result = new V4CronJobDTO();
            result.setId(v4Request.getId());
            result.setStatus(v4Request.getStatus());
            return DryRunResult.valid(null, result);
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
            // 操作人缺失绝不能兜底成当前调用方：下游会以该身份鉴权并操作
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
     * 两者在 inner 路径都不生效，必须在此显式完成。租户由 appId 反查而非取调用方传值，避免上游传错租户导致越权。
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

    /**
     * 定时任务没有执行目标，概要区展示的是"将按什么周期、用哪个执行方案跑什么任务"
     */
    private ResolvedSummary buildSaveCronSummary(CronJobInfoDTO cronJobInfo, boolean update) {
        ResolvedSummary summary = new ResolvedSummary();
        summary.setName(cronJobInfo.getName());
        summary.addField("operation", update ? "UPDATE" : "CREATE");
        if (update) {
            summary.addField("cron_id", String.valueOf(cronJobInfo.getId()));
        }
        summary.addField("job_plan_id", String.valueOf(cronJobInfo.getTaskPlanId()));
        summary.addField("cron_expression", cronJobInfo.getCronExpression());
        summary.addField("execute_time",
            cronJobInfo.getExecuteTime() == null ? null : String.valueOf(cronJobInfo.getExecuteTime()));
        summary.addField("execute_time_zone", cronJobInfo.getExecuteTimeZone());
        return summary;
    }

    /**
     * 启停操作的入参只有 id 与目标状态，必须补出定时任务名与执行方案，
     * 否则审批人看到的只是一个数字 ID，无法判断启的是哪个任务。
     * <p>
     * 定时任务不存在时<b>直接拒绝，不允许产出"只剩 id 与目标状态"的概要</b>：
     * 那样的单据无法判断影响面，等于让审批人盲签。
     */
    private ResolvedSummary buildUpdateStatusSummary(Long appId, Long cronJobId, boolean enable) {
        CronJobInfoDTO cronJobInfo = cronJobService.getCronJobInfoById(appId, cronJobId);
        if (cronJobInfo == null) {
            throw new NotFoundException(ErrorCode.CRON_JOB_NOT_EXIST);
        }
        ResolvedSummary summary = new ResolvedSummary();
        summary.setName(cronJobInfo.getName());
        summary.addField("cron_id", String.valueOf(cronJobId));
        summary.addField("target_status",
            enable ? CronStatusEnum.RUNNING.name() : CronStatusEnum.STOPPING.name());
        summary.addField("job_plan_id", String.valueOf(cronJobInfo.getTaskPlanId()));
        summary.addField("cron_expression", cronJobInfo.getCronExpression());
        return summary;
    }

    private V4CronJobDTO buildCronJobResult(CronJobInfoDTO cronJobInfo) {
        V4CronJobDTO result = new V4CronJobDTO();
        result.setId(cronJobInfo.getId());
        result.setName(cronJobInfo.getName());
        result.setStatus(Boolean.TRUE.equals(cronJobInfo.getEnable())
            ? CronStatusEnum.RUNNING.getStatus() : CronStatusEnum.STOPPING.getStatus());
        return result;
    }
}
