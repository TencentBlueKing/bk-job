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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.exception.InvalidParamException;
import org.apache.commons.lang3.StringUtils;

/**
 * 资源范围校验工具类
 */
public class ScopeValidator {

    /**
     * 校验资源范围
     *
     * @param bizId     兼容字段：业务ID
     * @param scopeType 资源范围类型
     * @param scopeId   资源范围ID
     */
    public static void validate(Long bizId, String scopeType, String scopeId) {
        if (bizId != null) {
            return;
        }
        if (StringUtils.isBlank(scopeType)) {
            throw new InvalidParamException(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, "bk_scope_type");
        }
        if (!ResourceScopeTypeEnum.isValid(scopeType)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "bk_scope_type");
        }
        if (StringUtils.isBlank(scopeId)) {
            throw new InvalidParamException(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, "bk_scope_id");
        }
    }
}
