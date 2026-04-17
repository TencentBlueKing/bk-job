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

package com.tencent.bk.job.analysis.validation;

import com.tencent.bk.job.analysis.model.web.req.SaveAIChatSessionReq;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;

/**
 * 当 sceneType 属于需要资源标识的场景（任务报错分析=1, 脚本管理=2）时，
 * sceneResourceId 不允许为空白。
 */
public class CheckSceneResourceIdValidator
    implements ConstraintValidator<CheckSceneResourceId, SaveAIChatSessionReq> {

    /**
     * 需要 sceneResourceId 的场景类型集合
     */
    private static final Set<Integer> SCENE_TYPES_REQUIRING_RESOURCE_ID = Set.of(1, 2);

    @Override
    public boolean isValid(SaveAIChatSessionReq req, ConstraintValidatorContext context) {
        if (req == null || req.getSceneType() == null) {
            return true;
        }
        if (SCENE_TYPES_REQUIRING_RESOURCE_ID.contains(req.getSceneType())) {
            return StringUtils.isNotBlank(req.getSceneResourceId());
        }
        return true;
    }
}
