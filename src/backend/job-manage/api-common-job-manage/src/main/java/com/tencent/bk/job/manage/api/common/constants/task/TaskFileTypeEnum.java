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

package com.tencent.bk.job.manage.api.common.constants.task;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaskFileTypeEnum {
    /**
     * 服务器文件
     */
    SERVER(1),

    /**
     * Job本地文件
     */
    LOCAL(2),

    /**
     * 文件源文件
     */
    FILE_SOURCE(3),

    /**
     * Base64编码的文件
     */
    BASE64_FILE(4);

    private final int type;

    public static TaskFileTypeEnum valueOf(int type) {
        for (TaskFileTypeEnum fileType : values()) {
            if (fileType.type == type) {
                return fileType;
            }
        }
        throw new IllegalArgumentException("No TaskFileTypeEnum constant: " + type);
    }

    public static boolean isValid(Integer type) {
        if (type == null) {
            return false;
        }
        for (TaskFileTypeEnum fileType : values()) {
            if (fileType.type == type) {
                return true;
            }
        }
        return false;
    }
}
