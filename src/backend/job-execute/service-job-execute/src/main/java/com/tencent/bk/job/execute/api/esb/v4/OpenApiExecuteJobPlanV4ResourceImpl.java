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
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.web.metrics.CustomTimed;
import com.tencent.bk.job.execute.metrics.ExecuteMetricsConstants;
import com.tencent.bk.job.execute.model.TaskExecuteParam;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4ExecuteJobPlanRequest;
import com.tencent.bk.job.execute.model.esb.v4.req.V4GlobalVarDTO;
import com.tencent.bk.job.execute.model.esb.v4.resp.V4JobExecuteDTO;
import com.tencent.bk.job.execute.service.ResolvedSummaryBuilder;
import com.tencent.bk.job.execute.service.TaskExecuteService;
import com.tencent.bk.job.execute.service.V4ExecuteJobPlanRequestConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@Slf4j
public class OpenApiExecuteJobPlanV4ResourceImpl implements OpenApiExecuteJobPlanV4Resource {

    private final TaskExecuteService taskExecuteService;

    @Autowired
    public OpenApiExecuteJobPlanV4ResourceImpl(TaskExecuteService taskExecuteService) {
        this.taskExecuteService = taskExecuteService;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v4_execute_job_plan"})
    @CustomTimed(
        metricName = ExecuteMetricsConstants.NAME_JOB_TASK_START,
        extraTags = {
            ExecuteMetricsConstants.TAG_KEY_START_MODE, ExecuteMetricsConstants.TAG_VALUE_START_MODE_API,
            ExecuteMetricsConstants.TAG_KEY_TASK_TYPE, ExecuteMetricsConstants.TAG_VALUE_TASK_TYPE_EXECUTE_PLAN
        })
    @AuditEntry(actionId = ActionId.LAUNCH_JOB_PLAN)
    public EsbV4Response<V4JobExecuteDTO> executeJobPlan(String username,
                                                         String appCode,
                                                         Boolean dryRun,
                                                         @AuditRequestBody V4ExecuteJobPlanRequest request) {

        boolean isDryRun = Boolean.TRUE.equals(dryRun);
        TaskExecuteParam executeParam = V4ExecuteJobPlanRequestConverter.convert(
            request, JobContextUtil.getUser(), appCode, isDryRun);
        TaskInstanceDTO taskInstance = taskExecuteService.executeJobPlan(executeParam);

        if (isDryRun) {
            return EsbV4Response.dryRunSuccess(
                ResolvedSummaryBuilder.build(taskInstance, assignedVarNames(request, taskInstance)));
        }

        V4JobExecuteDTO jobExecuteDTO = new V4JobExecuteDTO();
        jobExecuteDTO.setTaskInstanceId(taskInstance.getId());
        jobExecuteDTO.setTaskName(taskInstance.getName());
        return EsbV4Response.success(jobExecuteDTO);
    }

    /**
     * 本次请求显式指定了取值的变量名。概要要列出执行方案的全部变量，靠这个集合区分
     * "本次改成这样"与"一直就是这样"——同一个取值，这两种情况的审批结论可能完全不同。
     * <p>
     * 调用方可以只传变量 ID，此时用预检解析出的变量反查变量名：概要按变量名标注，两种传法都要认得
     */
    private Set<String> assignedVarNames(V4ExecuteJobPlanRequest request, TaskInstanceDTO taskInstance) {
        if (CollectionUtils.isEmpty(request.getGlobalVars())) {
            return Collections.emptySet();
        }
        Map<Long, String> varNameById = new HashMap<>();
        if (CollectionUtils.isNotEmpty(taskInstance.getVariables())) {
            taskInstance.getVariables().stream()
                .filter(variable -> variable.getId() != null)
                .forEach(variable -> varNameById.put(variable.getId(), variable.getName()));
        }
        Set<String> assignedVarNames = new HashSet<>();
        for (V4GlobalVarDTO globalVar : request.getGlobalVars()) {
            String name = StringUtils.isNotBlank(globalVar.getName())
                ? globalVar.getName() : varNameById.get(globalVar.getId());
            if (StringUtils.isNotBlank(name)) {
                assignedVarNames.add(name);
            }
        }
        return assignedVarNames;
    }
}
