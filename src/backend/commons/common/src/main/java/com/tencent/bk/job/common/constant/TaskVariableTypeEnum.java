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

package com.tencent.bk.job.common.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @since 3/10/2019 17:17
 */
@Getter
@AllArgsConstructor
public enum TaskVariableTypeEnum {
    /**
     * 字符串
     */
    STRING(1, true, false, null),

    /**
     * 命名空间
     */
    NAMESPACE(2, true, false, null),

    /**
     * 主机列表
     */
    HOST_LIST(3, false, false, null),

    /**
     * 密文
     */

    CIPHER(4, false, true, "******"),

    /**
     * 关联数组
     */
    ASSOCIATIVE_ARRAY(5, true, false, null),

    /**
     * 索引数组
     */
    INDEX_ARRAY(6, true, false, null);

    @JsonValue
    private int type;

    private boolean changeable;

    private boolean needMask;

    private String mask;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static TaskVariableTypeEnum valOf(int type) {
        for (TaskVariableTypeEnum value : values()) {
            if (value.type == type) {
                return value;
            }
        }
        return null;
    }

    public boolean isChangeable() {
        return changeable;
    }

    public boolean needMask() {
        return needMask;
    }

    public String getI18nKey() {
        return "task.variable.type.name." + this.name().toLowerCase();
    }
}
