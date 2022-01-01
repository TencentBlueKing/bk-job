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
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 驱逐策略：将多个驱逐策略组合为一个驱逐策略
 */
@Data
@NoArgsConstructor
public class ComposedTaskEvictPolicy implements ITaskEvictPolicy {

    public static final String classType = "ComposedTaskEvictPolicy";

    public enum ComposeOperator {
        AND, OR
    }

    private ComposeOperator operator = null;
    private List<ITaskEvictPolicy> policyList = new ArrayList<>();

    public ComposedTaskEvictPolicy(ComposeOperator operator, ITaskEvictPolicy... policyList) {
        this.operator = operator;
        this.policyList.addAll(Arrays.asList(policyList));
    }

    public ComposedTaskEvictPolicy(ComposeOperator operator, List<ITaskEvictPolicy> policyList) {
        this.operator = operator;
        this.policyList.addAll(policyList);
    }

    @Override
    public boolean needToEvict(TaskInstanceDTO taskInstance) {
        if (operator == ComposeOperator.AND) {
            // 只要有一条策略不驱逐就保留
            for (ITaskEvictPolicy policy : policyList) {
                if (!policy.needToEvict(taskInstance)) {
                    return false;
                }
            }
            return true;
        } else if (operator == ComposeOperator.OR) {
            // 只要有一条策略驱逐就驱逐
            for (ITaskEvictPolicy policy : policyList) {
                if (policy.needToEvict(taskInstance)) {
                    return true;
                }
            }
        }
        return false;
    }
}
