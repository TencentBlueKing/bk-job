/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.job.execute.dao.impl;

import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.util.toggle.ToggleEvaluateContext;
import com.tencent.bk.job.common.util.toggle.ToggleStrategyContextParams;
import com.tencent.bk.job.common.util.toggle.feature.FeatureIdConstants;
import com.tencent.bk.job.common.util.toggle.feature.FeatureToggle;
import com.tencent.bk.job.execute.common.context.JobExecuteContext;
import com.tencent.bk.job.execute.common.context.JobExecuteContextThreadLocalRepo;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.impl.DSL;

import java.util.function.Function;

/**
 * task_instance_id DAO 层查询条件动态构造。
 * 背景：由于分库分表改造，原有的一些表(比如step_instance_script)中需要加入 task_instance_id 字段作为分片键。从数据的唯一性来说，
 * 查询的时候传入 task_instance_id 条件是多余的，涉及的这些表的唯一业务主键都是(step_instance_id+execute_count+其它字段)。
 * <p>
 * 考虑到 db 变更过程中表中 task_instance_id 可能为 0 的情况（历史数据），
 * 所以，需要通过业务 ID 、task_instance_id 的值动态去构造查询条件，兼容历史数据的查询
 */
@Slf4j
public class TaskInstanceIdDynamicCondition {

    public static Condition build(Long taskInstanceId,
                                  Function<Long, Condition> taskInstanceIdConditionBuilder) {
        ToggleEvaluateContext toggleEvaluateContext;
        JobExecuteContext jobExecuteContext = JobExecuteContextThreadLocalRepo.get();
        if (jobExecuteContext == null) {
            // JobExecuteContext 正常应该不会为 null 。为了不影响请求正常处理，忽略错误,直接返回 TRUE Condition
            // (不会影响 DAO 查询，task_instance_id 仅作为分片功能实用，实际业务数据关系并不强依赖 task_instance_id)
            toggleEvaluateContext = ToggleEvaluateContext.EMPTY;
        } else {
            ResourceScope resourceScope = jobExecuteContext.getResourceScope();
            if (resourceScope != null) {
                toggleEvaluateContext = ToggleEvaluateContext.builder()
                    .addContextParam(ToggleStrategyContextParams.CTX_PARAM_RESOURCE_SCOPE, resourceScope);
            } else {
                toggleEvaluateContext = ToggleEvaluateContext.EMPTY;
            }
        }

        if (FeatureToggle.checkFeature(FeatureIdConstants.DAO_ADD_TASK_INSTANCE_ID, toggleEvaluateContext)) {
            if (taskInstanceId == null || taskInstanceId <= 0L) {
                log.info("TaskInstanceIdDynamicCondition : InvalidTaskInstanceId : {}", taskInstanceId);
                // 输出堆栈信息，便于发现问题；后续功能稳定之后需要删除
                safePrintStackTrace();
                // 为了不影响兼容性，忽略错误
                return DSL.trueCondition();
            } else {
                return taskInstanceIdConditionBuilder.apply(taskInstanceId);
            }
        } else {
            return DSL.trueCondition();
        }
    }

    private static void safePrintStackTrace() {
        try {
            StringBuilder message = new StringBuilder();
            for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
                message.append(System.lineSeparator()).append(stackTraceElement.toString());
            }
            log.info("InvalidTaskInstanceIdConditionStackTrace: {}", message);
        } catch (Throwable e) {
            // ignore
        }
    }
}
