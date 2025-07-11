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

import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.model.inner.ServiceScriptCheckResultItemDTO;

import java.util.List;

/**
 * 高危脚本检查
 */
public interface DangerousScriptCheckService {
    /**
     * 检查脚本
     *
     * @param scriptType 脚本类型
     * @param content    脚本内容
     * @return 检查结果
     */
    List<ServiceScriptCheckResultItemDTO> check(ScriptTypeEnum scriptType, String content);

    /**
     * 是否需要拦截
     *
     * @param checkResultItems 脚本检查结果
     * @return 是否拦截
     */
    boolean shouldIntercept(List<ServiceScriptCheckResultItemDTO> checkResultItems);

    /**
     * 输出检查结果
     *
     * @param scriptName   脚本执行步骤名称
     * @param checkResults 检查结果
     * @return 检查结果
     */
    String summaryDangerousScriptCheckResult(String scriptName, List<ServiceScriptCheckResultItemDTO> checkResults);

    /**
     * 保存记录
     *
     * @param taskInstance     任务实例
     * @param stepInstance     步骤实例
     * @param checkResultItems 脚本检查结果
     */
    void saveDangerousRecord(TaskInstanceDTO taskInstance, StepInstanceDTO stepInstance,
                             List<ServiceScriptCheckResultItemDTO> checkResultItems);
}
