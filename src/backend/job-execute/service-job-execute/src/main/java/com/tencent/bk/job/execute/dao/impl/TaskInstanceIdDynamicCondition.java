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
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.feature.FeatureExecutionContext;
import com.tencent.bk.job.common.util.feature.FeatureIdConstants;
import com.tencent.bk.job.common.util.feature.FeatureToggle;
import com.tencent.bk.job.common.util.feature.ToggleStrategyContextParams;
import com.tencent.bk.job.execute.colddata.JobExecuteContextThreadLocalRepo;
import com.tencent.bk.job.execute.common.context.PropagatedJobExecuteContext;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.impl.DSL;

import java.util.function.Function;

@Slf4j
public class TaskInstanceIdDynamicCondition {

    public static Condition build(Long taskInstanceId,
                                  Function<Long, Condition> taskInstanceIdConditionBuilder) {
        PropagatedJobExecuteContext jobExecuteContext = JobExecuteContextThreadLocalRepo.get();
        if (jobExecuteContext == null) {
            log.warn("TaskInstanceIdDynamicCondition : Empty JobExecuteContext!");
            // 为了不影响兼容性，忽略错误
            return DSL.trueCondition();
        }
        ResourceScope resourceScope = jobExecuteContext.getResourceScope();
        if (resourceScope == null) {
            log.warn("TaskInstanceIdDynamicCondition : Empty resource scope!");
            // 为了不影响兼容性，忽略错误
            return DSL.trueCondition();
        }
        if (FeatureToggle.checkFeature(
            FeatureIdConstants.DAO_ADD_TASK_INSTANCE_ID,
            FeatureExecutionContext.builder()
                .addContextParam(ToggleStrategyContextParams.CTX_PARAM_RESOURCE_SCOPE,
                    JobContextUtil.getAppResourceScope()))) {
            if (taskInstanceId == null || taskInstanceId <= 0L) {
                log.warn("TaskInstanceIdDynamicCondition : Invalid taskInstanceId for building query condition. " +
                        "taskInstanceId : {}",
                    taskInstanceId);
                // 为了不影响兼容性，忽略错误
                return DSL.trueCondition();
            } else {
                return taskInstanceIdConditionBuilder.apply(taskInstanceId);
            }
        } else {
            return DSL.trueCondition();
        }
    }
}
