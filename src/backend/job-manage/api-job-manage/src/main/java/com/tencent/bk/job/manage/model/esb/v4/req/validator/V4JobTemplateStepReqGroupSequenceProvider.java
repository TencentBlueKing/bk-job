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

package com.tencent.bk.job.manage.model.esb.v4.req.validator;

import com.tencent.bk.job.manage.api.common.constants.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateStepReq;
import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * 按步骤类型启用对应步骤详情的必填校验。
 */
public class V4JobTemplateStepReqGroupSequenceProvider
    implements DefaultGroupSequenceProvider<V4JobTemplateStepReq> {

    @Override
    public List<Class<?>> getValidationGroups(V4JobTemplateStepReq step) {
        List<Class<?>> groups = new ArrayList<>();
        groups.add(V4JobTemplateStepReq.class);
        if (step == null || !TaskStepTypeEnum.isValid(step.getType())) {
            return groups;
        }
        switch (TaskStepTypeEnum.valueOf(step.getType())) {
            case SCRIPT:
                groups.add(V4JobTemplateValidationGroups.StepType.Script.class);
                break;
            case FILE:
                groups.add(V4JobTemplateValidationGroups.StepType.File.class);
                break;
            case APPROVAL:
                groups.add(V4JobTemplateValidationGroups.StepType.Approval.class);
                break;
            default:
                break;
        }
        return groups;
    }
}
