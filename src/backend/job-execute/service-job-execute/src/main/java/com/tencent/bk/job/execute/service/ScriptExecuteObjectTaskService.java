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

package com.tencent.bk.job.execute.service;

import com.tencent.bk.job.execute.model.ExecuteObjectCompositeKey;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;

import java.util.Collection;
import java.util.List;

public interface ScriptExecuteObjectTaskService extends ExecuteObjectTaskService {
    /**
     * 根据执行对象复合 KEY 获取执行对象任务
     *
     * @param stepInstance              步骤实例
     * @param executeCount              执行次数
     * @param batch                     滚动执行批次；传入null或者0将忽略该参数
     * @param executeObjectCompositeKey 执行对象复合 KEY
     * @return 执行对象任务
     */
    ExecuteObjectTask getTaskByExecuteObjectCompositeKey(StepInstanceBaseDTO stepInstance,
                                                         Integer executeCount,
                                                         Integer batch,
                                                         ExecuteObjectCompositeKey executeObjectCompositeKey);

    /**
     * 根据执行对象ID批量获取执行对象任务
     *
     * @param stepInstance     步骤实例
     * @param executeCount     执行次数
     * @param batch            滚动执行批次；传入null或者0将忽略该参数
     * @param executeObjectIds 执行对象ID列表
     * @return 执行对象任务列表
     */
    List<ExecuteObjectTask> getTaskByExecuteObjectIds(StepInstanceBaseDTO stepInstance,
                                                      Integer executeCount,
                                                      Integer batch,
                                                      Collection<String> executeObjectIds);
}
