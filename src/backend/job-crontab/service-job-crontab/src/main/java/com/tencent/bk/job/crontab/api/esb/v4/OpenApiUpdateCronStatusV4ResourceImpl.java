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

package com.tencent.bk.job.crontab.api.esb.v4;

import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.audit.annotations.AuditRequestBody;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.ResolvedSummary;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.model.esb.v4.V4CronStatusEnum;
import com.tencent.bk.job.crontab.model.esb.v4.req.V4UpdateCronStatusRequest;
import com.tencent.bk.job.crontab.model.esb.v4.resp.V4CronJobDTO;
import com.tencent.bk.job.crontab.service.CronJobService;
import com.tencent.bk.job.crontab.service.V4UpdateCronStatusRequestConverter;
import com.tencent.bk.job.crontab.util.CronExpressionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class OpenApiUpdateCronStatusV4ResourceImpl implements OpenApiUpdateCronStatusV4Resource {

    private final CronJobService cronJobService;

    @Autowired
    public OpenApiUpdateCronStatusV4ResourceImpl(CronJobService cronJobService) {
        this.cronJobService = cronJobService;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v4_update_cron_status"})
    @AuditEntry(actionId = ActionId.MANAGE_CRON)
    public EsbV4Response<V4CronJobDTO> updateCronStatus(String username,
                                                        String appCode,
                                                        Boolean dryRun,
                                                        @AuditRequestBody V4UpdateCronStatusRequest request) {

        User operator = JobContextUtil.getUser();
        boolean enable = V4UpdateCronStatusRequestConverter.convertToEnable(request);
        Long appId = request.getAppId();

        if (Boolean.TRUE.equals(dryRun)) {
            cronJobService.dryRunChangeCronJobEnableStatus(operator, appId, request.getId(), enable);
            return EsbV4Response.dryRunSuccess(buildSummary(appId, request.getId(), enable));
        }

        cronJobService.changeCronJobEnableStatus(operator, appId, request.getId(), enable);
        V4CronJobDTO result = new V4CronJobDTO();
        result.setId(request.getId());
        result.setStatus(request.getStatus());
        return EsbV4Response.success(result);
    }

    /**
     * 启停操作的入参只有 id 与目标状态，必须补出定时任务名与执行方案，
     * 否则审批人看到的只是一个数字 ID，无法判断启的是哪个任务。
     * <p>
     * 定时任务不存在时<b>直接拒绝，不允许产出"只剩 id 与目标状态"的概要</b>：
     * 那样的单据无法判断影响面，等于让审批人盲签。
     * <p>
     * 定时规则按<b>用户提交的 UNIX 形态</b>展示：库里存的是转换后的 Quartz 表达式，
     * 审批人核对的应当是创建时传进来的那一串
     */
    private ResolvedSummary buildSummary(Long appId, Long cronJobId, boolean enable) {
        CronJobInfoDTO cronJobInfo = cronJobService.getCronJobInfoById(appId, cronJobId);
        if (cronJobInfo == null) {
            throw new NotFoundException(ErrorCode.CRON_JOB_NOT_EXIST);
        }
        ResolvedSummary summary = new ResolvedSummary();
        summary.setName(cronJobInfo.getName());
        summary.addField("cron_id", String.valueOf(cronJobId));
        summary.addField("target_status", V4CronStatusEnum.of(enable).name());
        summary.addField("job_plan_id", String.valueOf(cronJobInfo.getTaskPlanId()));
        summary.addField("cron_expression",
            CronExpressionUtil.fixExpressionForUserSafely(cronJobInfo.getCronExpression()));
        return summary;
    }
}
