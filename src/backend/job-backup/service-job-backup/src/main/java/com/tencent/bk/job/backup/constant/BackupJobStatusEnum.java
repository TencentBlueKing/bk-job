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
 * @since 23/7/2020 19:18
 */
@Getter
@AllArgsConstructor
public enum BackupJobStatusEnum {
    /**
     * 初始状态
     */
    INIT(0),

    /**
     * 解析成功
     */
    PARSE_SUCCESS(1),

    /**
     * 需要密码
     */
    NEED_PASSWORD(2),

    /**
     * 密码错误
     */
    WRONG_PASSWORD(3),

    /**
     * 已提交
     */
    SUBMIT(4),

    /**
     * 处理中
     */
    PROCESSING(5),

    /**
     * 处理成功
     */
    SUCCESS(6),

    /**
     * 处理失败
     */
    FAILED(7),

    /**
     * 已取消
     */
    CANCEL(8),

    /**
     * 已完成
     */
    FINISHED(9);

    @JsonValue
    private final Integer status;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static BackupJobStatusEnum valueOf(Integer status) {
        for (BackupJobStatusEnum value : values()) {
            if (value.getStatus().equals(status)) {
                return value;
            }
        }
        return null;
    }
}
