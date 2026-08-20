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

package com.tencent.bk.job.execute.common.constants;

import com.tencent.bk.job.common.constant.DuplicateHandlerEnum;
import com.tencent.bk.job.common.constant.NotExistPathHandlerEnum;

/**
 * 文件分发模式。
 * <p>
 * 分发模式是「同名文件如何处理」与「目标路径不存在如何处理」两个底层选项的组合，
 * 对外只暴露这一个组合枚举，底层两个选项由 {@link #getDuplicateHandler()} 与
 * {@link #getNotExistPathHandler()} 给出。
 */
public enum FileTransferModeEnum {
    STRICT(1, "严谨模式", DuplicateHandlerEnum.OVERWRITE, NotExistPathHandlerEnum.STEP_FAIL),
    FORCE(2, "强制模式", DuplicateHandlerEnum.OVERWRITE, NotExistPathHandlerEnum.CREATE_DIR),
    SAFETY_IP_PREFIX(3, "保险模式(FILE_SRC_IP)", DuplicateHandlerEnum.GROUP_BY_IP, NotExistPathHandlerEnum.CREATE_DIR),
    SAFETY_DATE_PREFIX(4, "保险模式(YYYY-MM-DD)",
        DuplicateHandlerEnum.GROUP_BY_DATE_AND_IP, NotExistPathHandlerEnum.CREATE_DIR);

    private final Integer value;
    private final String name;
    private final DuplicateHandlerEnum duplicateHandler;
    private final NotExistPathHandlerEnum notExistPathHandler;

    FileTransferModeEnum(Integer val,
                         String name,
                         DuplicateHandlerEnum duplicateHandler,
                         NotExistPathHandlerEnum notExistPathHandler) {
        this.value = val;
        this.name = name;
        this.duplicateHandler = duplicateHandler;
        this.notExistPathHandler = notExistPathHandler;
    }

    public static FileTransferModeEnum getFileTransferModeEnum(Integer mode) {
        if (mode == null) {
            return null;
        }
        for (FileTransferModeEnum modeEnum : values()) {
            if (modeEnum.getValue().equals(mode)) {
                return modeEnum;
            }
        }
        return null;
    }

    public Integer getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    /**
     * 同名文件的处理方式
     */
    public DuplicateHandlerEnum getDuplicateHandler() {
        return duplicateHandler;
    }

    /**
     * 目标路径不存在时的处理方式
     */
    public NotExistPathHandlerEnum getNotExistPathHandler() {
        return notExistPathHandler;
    }
}
