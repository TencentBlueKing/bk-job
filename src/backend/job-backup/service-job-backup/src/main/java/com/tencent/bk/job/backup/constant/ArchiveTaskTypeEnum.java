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

package com.tencent.bk.job.backup.constant;

import lombok.Getter;

@Getter
public enum ArchiveTaskTypeEnum {
    /**
     * 作业实例数据归档
     */
    JOB_INSTANCE(1),
    /**
     * 作业实例按业务冗余数据归档
     */
    JOB_INSTANCE_APP(2),
    /**
     * 作业执行日志归档
     */
    JOB_EXECUTE_LOG(3);

    private final int type;

    ArchiveTaskTypeEnum(int type) {
        this.type = type;
    }

    public static ArchiveTaskTypeEnum valOf(int type) {
        for (ArchiveTaskTypeEnum taskType : values()) {
            if (taskType.getType() == type) {
                return taskType;
            }
        }
        throw new IllegalArgumentException("No ArchiveTaskTypeEnum constant: " + type);
    }
}
