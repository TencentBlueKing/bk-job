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

/**
 * 资源范围类型
 */
public enum ResourceScopeTypeEnum {
    /**
     * CMDB业务
     */
    BIZ("biz"),
    /**
     * CMDB业务集
     */
    BIZ_SET("biz_set");

    /**
     * 资源范围类型
     */
    @JsonValue
    private final String value;

    ResourceScopeTypeEnum(String value) {
        this.value = value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ResourceScopeTypeEnum from(String type) {
        for (ResourceScopeTypeEnum scopeType : values()) {
            if (scopeType.value.equals(type)) {
                return scopeType;
            }
        }
        throw new IllegalArgumentException("No ResourceScopeTypeEnum constant: " + type);
    }

    public String getValue() {
        return value;
    }

    public static boolean isValid(String type) {
        try {
            from(type);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
