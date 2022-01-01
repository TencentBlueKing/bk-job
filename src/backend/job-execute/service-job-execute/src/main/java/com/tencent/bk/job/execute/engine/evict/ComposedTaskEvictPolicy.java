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

package com.tencent.bk.job.execute.engine.evict;

import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.inner.ComposedTaskEvictPolicyDTO;
import com.tencent.bk.job.execute.model.inner.TaskEvictPolicyDTO;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 驱逐策略：将多个驱逐策略组合为一个驱逐策略
 */
@Data
@NoArgsConstructor
public class ComposedTaskEvictPolicy extends ComposedTaskEvictPolicyDTO implements ITaskEvictPolicy {

    public ComposedTaskEvictPolicy(ComposedTaskEvictPolicyDTO policyDTO) {
        super(policyDTO.getOperator(), policyDTO.getPolicyList());
    }

    public ComposedTaskEvictPolicy(ComposeOperator operator, TaskEvictPolicyDTO... policyDTOList) {
        super(operator, policyDTOList);
    }

    public ComposedTaskEvictPolicy(ComposeOperator operator, List<TaskEvictPolicyDTO> policyDTOList) {
        super(operator, policyDTOList);
    }

    @Override
    public boolean needToEvict(TaskInstanceDTO taskInstance) {
        if (operator == ComposeOperator.AND) {
            // 只要有一条策略不驱逐就保留
            for (TaskEvictPolicyDTO policyDTO : policyList) {
                ITaskEvictPolicy policy = PolicyFactory.createPolicyByDTO(policyDTO);
                if (!policy.needToEvict(taskInstance)) {
                    return false;
                }
            }
            return true;
        } else if (operator == ComposeOperator.OR) {
            // 只要有一条策略驱逐就驱逐
            for (TaskEvictPolicyDTO policyDTO : policyList) {
                ITaskEvictPolicy policy = PolicyFactory.createPolicyByDTO(policyDTO);
                if (policy.needToEvict(taskInstance)) {
                    return true;
                }
            }
        }
        return false;
    }
}
