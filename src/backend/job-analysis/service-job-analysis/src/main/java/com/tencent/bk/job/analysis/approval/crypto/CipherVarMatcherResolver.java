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

package com.tencent.bk.job.analysis.approval.crypto;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.manage.api.inner.ServiceTaskPlanResource;
import com.tencent.bk.job.manage.api.inner.ServiceTaskTemplateResource;
import com.tencent.bk.job.manage.model.inner.ServiceTaskVariableTypeDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 向 job-manage 查询执行方案 / 作业模板的全局变量类型。
 * <p>
 * 查询失败一律 fail-closed：加密发生在预检之后，此时执行方案与模板必然已确认存在，查不到只可能是下游异常，
 * 继续往下走就会把密码类变量明文写进库里。
 */
@Slf4j
@Service
public class CipherVarMatcherResolver {

    private final ServiceTaskPlanResource taskPlanResource;
    private final ServiceTaskTemplateResource taskTemplateResource;

    public CipherVarMatcherResolver(ServiceTaskPlanResource taskPlanResource,
                                    ServiceTaskTemplateResource taskTemplateResource) {
        this.taskPlanResource = taskPlanResource;
        this.taskTemplateResource = taskTemplateResource;
    }

    public CipherVarMatcher ofPlan(Long planId) {
        if (planId == null || planId <= 0) {
            throw new InternalException(
                "Job plan id is required to resolve global var types", ErrorCode.INTERNAL_ERROR);
        }
        return new CipherVarMatcher(unwrap(taskPlanResource.listPlanGlobalVarTypes(planId),
            "job plan " + planId));
    }

    public CipherVarMatcher ofTemplate(Long templateId) {
        if (templateId == null || templateId <= 0) {
            throw new InternalException(
                "Job template id is required to resolve global var types", ErrorCode.INTERNAL_ERROR);
        }
        return new CipherVarMatcher(unwrap(taskTemplateResource.listTemplateGlobalVarTypes(templateId),
            "job template " + templateId));
    }

    private List<ServiceTaskVariableTypeDTO> unwrap(InternalResponse<List<ServiceTaskVariableTypeDTO>> response,
                                                    String source) {
        if (response == null || !response.isSuccess()) {
            throw new InternalException("Query global var types of " + source + " failed",
                response == null ? ErrorCode.INTERNAL_ERROR : response.getCode());
        }
        return response.getData();
    }
}
