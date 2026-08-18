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

package com.tencent.bk.job.execute.api.esb.v4;

import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.audit.annotations.AuditRequestBody;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.ResolvedSummary;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.web.metrics.CustomTimed;
import com.tencent.bk.job.execute.metrics.ExecuteMetricsConstants;
import com.tencent.bk.job.execute.model.FastTaskDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4FastExecuteScriptRequest;
import com.tencent.bk.job.execute.model.esb.v4.resp.V4JobExecuteDTO;
import com.tencent.bk.job.execute.service.ResolvedSummaryBuilder;
import com.tencent.bk.job.execute.service.TaskExecuteService;
import com.tencent.bk.job.execute.service.V4FastExecuteScriptRequestConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class OpenApiFastExecuteScriptV4ResourceImpl implements OpenApiFastExecuteScriptV4Resource {

    private final TaskExecuteService taskExecuteService;

    @Autowired
    public OpenApiFastExecuteScriptV4ResourceImpl(TaskExecuteService taskExecuteService) {
        this.taskExecuteService = taskExecuteService;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v4_fast_execute_script"})
    @CustomTimed(
        metricName = ExecuteMetricsConstants.NAME_JOB_TASK_START,
        extraTags = {
            ExecuteMetricsConstants.TAG_KEY_START_MODE, ExecuteMetricsConstants.TAG_VALUE_START_MODE_API,
            ExecuteMetricsConstants.TAG_KEY_TASK_TYPE, ExecuteMetricsConstants.TAG_VALUE_TASK_TYPE_FAST_SCRIPT
        })
    @AuditEntry
    public EsbV4Response<V4JobExecuteDTO> fastExecuteScript(String username,
                                                            String appCode,
                                                            Boolean dryRun,
                                                            @AuditRequestBody V4FastExecuteScriptRequest request) {

        boolean isDryRun = Boolean.TRUE.equals(dryRun);
        FastTaskDTO fastTask = V4FastExecuteScriptRequestConverter.convert(
            request, JobContextUtil.getUser(), appCode, isDryRun);
        TaskInstanceDTO taskInstance = taskExecuteService.executeFastTask(fastTask);

        if (isDryRun) {
            ResolvedSummary summary = ResolvedSummaryBuilder.build(taskInstance);
            if (request.getTimeout() == null) {
                summary.addDefaultApplied("timeout", JobConstants.DEFAULT_JOB_TIMEOUT_SECONDS + "s");
            }
            return EsbV4Response.dryRunSuccess(summary);
        }

        V4JobExecuteDTO jobExecuteDTO = new V4JobExecuteDTO();
        jobExecuteDTO.setTaskInstanceId(fastTask.getTaskInstance().getId());
        jobExecuteDTO.setStepInstanceId(fastTask.getStepInstance().getId());
        jobExecuteDTO.setTaskName(fastTask.getTaskInstance().getName());
        return EsbV4Response.success(jobExecuteDTO);
    }
}
