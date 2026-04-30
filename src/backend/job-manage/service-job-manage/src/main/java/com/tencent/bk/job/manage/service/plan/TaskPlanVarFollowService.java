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

package com.tencent.bk.job.manage.service.plan;

import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;

/**
 * 执行方案变量跟随模板相关规则处理
 */
public interface TaskPlanVarFollowService {

    /**
     * 存在跟随作业模板的变量，需要判断该变量默认值是否跟作业模板的一致，如果不一致修改执行方案的版本产生差异
     */
    void updatePlanVersionIfFollowVarChanged(TaskPlanInfoDTO taskPlanInfo);

    /**
     * 更新执行方案的版本号，
     * 1.在更新作业时，基础信息变动了作业会生成新版本，即changed=true时本方法直接返回，
     * 2.执行方案的变量跟随作业模板变量，且执行方案变量值跟作业的不一样，该执行方案生成新版本号，产生差异，后续可以去同步
     */
    void updatePlanVersionIfVarValueChanged(TaskTemplateInfoDTO taskTemplateInfo, Boolean changed);
}
