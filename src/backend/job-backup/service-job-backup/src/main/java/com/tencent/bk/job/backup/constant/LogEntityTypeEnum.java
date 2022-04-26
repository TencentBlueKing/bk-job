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

package com.tencent.bk.job.backup.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @since 23/7/2020 21:43
 */
@Getter
@AllArgsConstructor
public enum LogEntityTypeEnum {

    /**
     * 普通文字日志
     */
    NORMAL(1),

    /**
     * 错误日志
     */
    ERROR(2),

    /**
     * 普通链接日志
     */
    LINK(3),

    /**
     * 关联模版日志
     */
    TEMPLATE(4),

    /**
     * 关联执行方案日志
     */
    PLAN(5),

    /**
     * 提示输入密码日志
     */
    REQUEST_PASSWORD(6),

    /**
     * 提示重新输入密码
     */
    RETRY_PASSWORD(7),

    /**
     * 作业执行完成日志
     */
    FINISHED(8);

    @JsonValue
    private final Integer type;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static LogEntityTypeEnum valueOf(Integer type) {
        for (LogEntityTypeEnum value : values()) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        return null;
    }
}
