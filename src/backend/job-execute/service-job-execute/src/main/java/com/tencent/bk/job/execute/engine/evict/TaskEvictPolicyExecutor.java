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

package com.tencent.bk.job.execute.engine.evict;

import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 任务驱逐策略执行器，用于判定某个任务在当前驱逐策略下是否应当被驱逐
 */
@Slf4j
@Component
public class TaskEvictPolicyExecutor {

    private final TaskEvictPolicyManager taskEvictPolicyManager;

    @Autowired
    public TaskEvictPolicyExecutor(TaskEvictPolicyManager taskEvictPolicyManager) {
        this.taskEvictPolicyManager = taskEvictPolicyManager;
    }

    /**
     * 判断一个任务实例是否应当被驱逐
     *
     * @param taskInstance 任务实例
     * @return 是否应当被驱逐
     */
    public boolean shouldEvictTask(TaskInstanceDTO taskInstance) {
        ComposedTaskEvictPolicy policy = taskEvictPolicyManager.getPolicy();
        if (policy == null) {
            return false;
        }
        return policy.needToEvict(taskInstance);
    }
}
