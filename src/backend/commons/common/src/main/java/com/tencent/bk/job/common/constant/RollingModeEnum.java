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
 * 滚动机制
 */
public enum RollingModeEnum {
    /**
     * 执行失败则暂停
     */
    PAUSE_IF_FAIL(1),
    /**
     * 执行成功，自动滚动下一批
     */
    CONTINUE_WHEN_SUCCESS(2),
    /**
     * 忽略失败，自动滚动下一批
     */
    IGNORE_ERROR(3),
    /**
     * 不自动，每批次都人工确认
     */
    MANUAL(4);

    /*
     * 滚动模式
     */
    @JsonValue
    private final int mode;

    RollingModeEnum(int mode) {
        this.mode = mode;
    }

    @JsonCreator
    public static RollingModeEnum valOf(int mode) {
        for (RollingModeEnum modeEnum : values()) {
            if (modeEnum.mode == mode) {
                return modeEnum;
            }
        }
        return null;
    }

    public int getValue() {
        return mode;
    }
}
