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

package com.tencent.bk.job.common.validation;

import com.tencent.bk.job.common.model.openapi.v4.OpenApiExecuteTargetDTO;
import com.tencent.bk.job.common.model.vo.TaskExecuteObjectsInfoVO;
import com.tencent.bk.job.common.model.vo.TaskHostNodeVO;
import org.apache.commons.collections4.CollectionUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ExecuteTargetNotEmptyValidator
    implements ConstraintValidator<ExecuteTargetNotEmpty, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value instanceof OpenApiExecuteTargetDTO) {
            return isValid((OpenApiExecuteTargetDTO) value);
        } else if (value instanceof TaskHostNodeVO) {
            return isValid((TaskHostNodeVO) value);
        } else if (value instanceof TaskExecuteObjectsInfoVO) {
            return isValid((TaskExecuteObjectsInfoVO) value);
        }
        return true;
    }

    private boolean isValid(OpenApiExecuteTargetDTO dto) {
        return CollectionUtils.isNotEmpty(dto.getHosts())
            || CollectionUtils.isNotEmpty(dto.getHostDynamicGroups())
            || CollectionUtils.isNotEmpty(dto.getHostTopoNodes())
            || CollectionUtils.isNotEmpty(dto.getKubeContainerFilters());
    }

    private boolean isValid(TaskHostNodeVO vo) {
        return CollectionUtils.isNotEmpty(vo.getHostList())
            || CollectionUtils.isNotEmpty(vo.getNodeList())
            || CollectionUtils.isNotEmpty(vo.getDynamicGroupList());
    }

    private boolean isValid(TaskExecuteObjectsInfoVO vo) {
        return CollectionUtils.isNotEmpty(vo.getHostList())
            || CollectionUtils.isNotEmpty(vo.getNodeList())
            || CollectionUtils.isNotEmpty(vo.getDynamicGroupList())
            || CollectionUtils.isNotEmpty(vo.getContainerList());
    }

    @Override
    public void initialize(ExecuteTargetNotEmpty constraintAnnotation) {

    }
}
