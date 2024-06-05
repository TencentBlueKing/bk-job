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

/**
 * 归档模式
 */
public enum ArchiveModeEnum {
    /**
     * 仅删除原始数据
     */
    DELETE_ONLY(Constants.DELETE_ONLY),
    /**
     * 备份数据后再删除
     */
    BACKUP_THEN_DELETE(Constants.BACKUP_THEN_DELETE),
    /**
     * 仅备份数据,不删除原始数据。该模式为开发/测试场景下使用，暂不对外开放
     */
    BACKUP_ONLY(Constants.BACKUP_ONLY);

    ArchiveModeEnum(String mode) {
        this.mode = mode;
    }

    public interface Constants {
        String DELETE_ONLY = "deleteOnly";
        String BACKUP_THEN_DELETE = "backupThenDelete";
        String BACKUP_ONLY = "backupOnly";
    }

    private final String mode;

    public static ArchiveModeEnum valOf(String mode) {
        for (ArchiveModeEnum value : values()) {
            if (value.mode.equalsIgnoreCase(mode)) {
                return value;
            }
        }
        throw new IllegalArgumentException("No ArchiveModeEnum constant: " + mode);
    }

    public String getMode() {
        return mode;
    }
}
