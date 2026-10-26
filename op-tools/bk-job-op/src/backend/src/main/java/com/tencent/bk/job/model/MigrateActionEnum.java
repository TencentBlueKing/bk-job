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

package com.tencent.bk.job.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 按业务分批迁移环境时的操作类型枚举
 */
public enum MigrateActionEnum {

    /** 新增迁移标记 */
    ADD("add"),

    /** 删除迁移标记 */
    DELETE("delete");

    /** 存入 DB / JSON 序列化时使用的小写字符串 */
    private final String type;

    MigrateActionEnum(String type) {
        this.type = type;
    }

    /**
     * 序列化：JSON 输出 type 字段值
     */
    @JsonValue
    public String getType() {
        return type;
    }

    /**
     * 反序列化：根据 type 字符串还原枚举
     */
    @JsonCreator
    public static MigrateActionEnum fromType(String type) {
        if (type == null) {
            return null;
        }
        for (MigrateActionEnum value : values()) {
            if (value.type.equalsIgnoreCase(type)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown MigrateActionEnum type: " + type);
    }
}
