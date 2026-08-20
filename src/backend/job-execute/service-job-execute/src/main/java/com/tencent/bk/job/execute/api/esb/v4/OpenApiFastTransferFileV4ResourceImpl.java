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
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.ResolvedSummary;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.web.metrics.CustomTimed;
import com.tencent.bk.job.execute.common.constants.FileTransferModeEnum;
import com.tencent.bk.job.execute.metrics.ExecuteMetricsConstants;
import com.tencent.bk.job.execute.model.FastTaskDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4FastTransferFileRequest;
import com.tencent.bk.job.execute.model.esb.v4.resp.V4JobExecuteDTO;
import com.tencent.bk.job.execute.service.ResolvedSummaryBuilder;
import com.tencent.bk.job.execute.service.TaskExecuteService;
import com.tencent.bk.job.execute.service.V4FastTransferFileRequestConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class OpenApiFastTransferFileV4ResourceImpl implements OpenApiFastTransferFileV4Resource {

    private final TaskExecuteService taskExecuteService;
    private final V4FastTransferFileRequestConverter requestConverter;

    @Autowired
    public OpenApiFastTransferFileV4ResourceImpl(TaskExecuteService taskExecuteService,
                                                 V4FastTransferFileRequestConverter requestConverter) {
        this.taskExecuteService = taskExecuteService;
        this.requestConverter = requestConverter;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v4_fast_transfer_file"})
    @CustomTimed(
        metricName = ExecuteMetricsConstants.NAME_JOB_TASK_START,
        extraTags = {
            ExecuteMetricsConstants.TAG_KEY_START_MODE, ExecuteMetricsConstants.TAG_VALUE_START_MODE_API,
            ExecuteMetricsConstants.TAG_KEY_TASK_TYPE, ExecuteMetricsConstants.TAG_VALUE_TASK_TYPE_FAST_FILE
        })
    @AuditEntry(actionId = ActionId.QUICK_TRANSFER_FILE)
    public EsbV4Response<V4JobExecuteDTO> fastTransferFile(String username,
                                                           String appCode,
                                                           Boolean dryRun,
                                                           @AuditRequestBody V4FastTransferFileRequest request) {

        boolean isDryRun = Boolean.TRUE.equals(dryRun);
        FastTaskDTO fastTask = requestConverter.convert(
            request, JobContextUtil.getUser(), appCode, isDryRun);
        TaskInstanceDTO taskInstance = taskExecuteService.executeFastTask(fastTask);

        if (isDryRun) {
            ResolvedSummary summary = ResolvedSummaryBuilder.build(taskInstance);
            if (request.getTimeout() == null) {
                summary.addDefaultApplied("timeout", JobConstants.DEFAULT_JOB_TIMEOUT_SECONDS + "s");
            }
            fillTransferModeDefault(summary, request.getTransferMode());
            return EsbV4Response.dryRunSuccess(summary);
        }

        V4JobExecuteDTO jobExecuteDTO = new V4JobExecuteDTO();
        jobExecuteDTO.setTaskInstanceId(fastTask.getTaskInstance().getId());
        jobExecuteDTO.setStepInstanceId(fastTask.getStepInstance().getId());
        jobExecuteDTO.setTaskName(fastTask.getTaskInstance().getName());
        return EsbV4Response.success(jobExecuteDTO);
    }

    /**
     * 不传分发模式会落到强制模式：目标路径不存在时自动建目录、同名文件直接覆盖。后果远大于严格模式，
     * 必须在概要里标成"按默认生效"，不能让审批人以为用户显式选过。
     * <p>
     * 只在<b>未传</b>时标注：显式传入的模式由概要里的分发模式行按预检解析结果如实展示，
     * 再标一次"默认"反而是误导（取值合法性已由请求转换器校验，非法值走不到这里）
     */
    private void fillTransferModeDefault(ResolvedSummary summary, Integer transferMode) {
        if (transferMode == null) {
            summary.addDefaultApplied("transfer_mode", FileTransferModeEnum.FORCE.name());
        }
    }
}
