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

package com.tencent.bk.job.common.util.toggle.prop;

import com.tencent.bk.job.common.util.toggle.ToggleEvaluateContext;
import com.tencent.bk.job.common.util.toggle.ToggleStrategy;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

@Data
public class PropToggle {
    private String defaultValue;

    private List<PropValueCondition> conditions;

    @Data
    @NoArgsConstructor
    public static class PropValueCondition {
        private String value;
        private ToggleStrategy strategy;
    }

    /**
     * 根据上下文计算属性值
     *
     * @param propName 属性名称
     * @param ctx      开关评估上下文
     * @return 属性值
     */
    public String evaluateValue(String propName, ToggleEvaluateContext ctx) {
        if (CollectionUtils.isEmpty(conditions)) {
            return defaultValue;
        }

        String propValue = null;
        for (PropToggle.PropValueCondition condition : conditions) {
            ToggleStrategy toggleStrategy = condition.getStrategy();
            if (toggleStrategy != null && toggleStrategy.evaluate(propName, ctx)) {
                propValue = condition.getValue();
                break;
            }
        }
        if (propValue == null) {
            propValue = defaultValue;
        }
        return propValue;
    }

}
