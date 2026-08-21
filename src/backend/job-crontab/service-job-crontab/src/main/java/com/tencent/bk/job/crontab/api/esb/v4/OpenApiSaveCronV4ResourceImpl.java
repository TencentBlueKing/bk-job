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
import com.tencent.bk.audit.context.AuditContext;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.ResolvedSummary;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.model.esb.v4.V4CronStatusEnum;
import com.tencent.bk.job.crontab.model.esb.v4.req.V4SaveCronRequest;
import com.tencent.bk.job.crontab.model.esb.v4.resp.V4CronJobDTO;
import com.tencent.bk.job.crontab.service.CronJobService;
import com.tencent.bk.job.crontab.service.V4SaveCronRequestConverter;
import com.tencent.bk.job.crontab.util.CronExpressionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class OpenApiSaveCronV4ResourceImpl implements OpenApiSaveCronV4Resource {

    private final CronJobService cronJobService;
    private final V4SaveCronRequestConverter requestConverter;

    @Autowired
    public OpenApiSaveCronV4ResourceImpl(CronJobService cronJobService,
                                         V4SaveCronRequestConverter requestConverter) {
        this.cronJobService = cronJobService;
        this.requestConverter = requestConverter;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v4_save_cron"})
    @AuditEntry
    public EsbV4Response<V4CronJobDTO> saveCron(String username,
                                                String appCode,
                                                Boolean dryRun,
                                                @AuditRequestBody V4SaveCronRequest request) {

        boolean update = V4SaveCronRequestConverter.isUpdate(request);
        // 新增与修改是两个不同的操作，审计动作只能在解析出意图之后才能确定
        AuditContext.current().updateActionId(update ? ActionId.MANAGE_CRON : ActionId.CREATE_CRON);

        User operator = JobContextUtil.getUser();
        CronJobInfoDTO cronJobInfo = requestConverter.convert(request, operator);

        if (Boolean.TRUE.equals(dryRun)) {
            CronJobInfoDTO checked = update
                ? cronJobService.dryRunUpdateCronJobInfo(operator, cronJobInfo)
                : cronJobService.dryRunCreateCronJobInfo(operator, cronJobInfo);
            return EsbV4Response.dryRunSuccess(buildSummary(checked, update));
        }

        CronJobInfoDTO saved = update
            ? cronJobService.updateCronJobInfo(operator, cronJobInfo)
            : cronJobService.createCronJobInfo(operator, cronJobInfo);
        return EsbV4Response.success(buildResult(saved));
    }

    /**
     * 定时任务没有执行目标，概要区展示的是"将按什么周期、用哪个执行方案跑什么任务"。
     * <p>
     * 定时规则按<b>用户提交的 UNIX 形态</b>展示：{@link CronJobInfoDTO#getCronExpression()} 里存的是
     * 转换后的 Quartz 表达式，审批人核对的应当是自己传进来的那一串
     */
    private ResolvedSummary buildSummary(CronJobInfoDTO cronJobInfo, boolean update) {
        ResolvedSummary summary = new ResolvedSummary();
        summary.setName(cronJobInfo.getName());
        summary.addField("operation", update ? "UPDATE" : "CREATE");
        if (update) {
            summary.addField("cron_id", String.valueOf(cronJobInfo.getId()));
        }
        summary.addField("job_plan_id", String.valueOf(cronJobInfo.getTaskPlanId()));
        summary.addField("cron_expression",
            CronExpressionUtil.fixExpressionForUserSafely(cronJobInfo.getCronExpression()));
        summary.addField("execute_time",
            cronJobInfo.getExecuteTime() == null ? null : String.valueOf(cronJobInfo.getExecuteTime()));
        summary.addField("execute_time_zone", cronJobInfo.getExecuteTimeZone());
        return summary;
    }

    private V4CronJobDTO buildResult(CronJobInfoDTO cronJobInfo) {
        V4CronJobDTO result = new V4CronJobDTO();
        result.setId(cronJobInfo.getId());
        result.setName(cronJobInfo.getName());
        result.setStatus(V4CronStatusEnum.of(Boolean.TRUE.equals(cronJobInfo.getEnable())).getStatus());
        return result;
    }
}
