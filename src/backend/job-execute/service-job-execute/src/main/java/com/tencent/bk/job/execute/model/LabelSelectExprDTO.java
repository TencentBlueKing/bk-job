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

package com.tencent.bk.job.execute.model;

import com.tencent.bk.job.common.annotation.PersistenceObject;
import com.tencent.bk.job.common.constant.LabelSelectorOperatorEnum;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Label selector 计算表达式
 */
@Data
@NoArgsConstructor
@PersistenceObject
public class LabelSelectExprDTO implements Cloneable {
    /**
     * Label key
     */
    private String key;

    /**
     * 计算操作符
     **/
    private LabelSelectorOperatorEnum operator;

    /**
     * Label values
     */
    private List<String> values;

    public LabelSelectExprDTO(String key, LabelSelectorOperatorEnum operator, List<String> values) {
        this.key = key;
        this.operator = operator;
        this.values = values;
    }

    @Override
    public LabelSelectExprDTO clone() {
        LabelSelectExprDTO clone = new LabelSelectExprDTO();
        clone.setKey(key);
        clone.setOperator(operator);
        if (CollectionUtils.isNotEmpty(values)) {
            clone.setValues(new ArrayList<>(values));
        }
        return clone;
    }
}
