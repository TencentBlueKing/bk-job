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

package com.tencent.bk.job.common.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 滚动批次执行模式。独立于滚动机制({@link RollingModeEnum})，用于控制滚动批次间是串行还是并行执行。
 * <p>
 * 串行(SERIAL)：批次间串行，上一批完成后再推进下一批，{@link RollingModeEnum} 生效。
 * 并行(PARALLEL)：批次间按错峰延迟并行下发，允许多批重叠，不读取 {@link RollingModeEnum}。
 */
public enum RollingExecutionModeEnum {
    /**
     * 串行执行（默认）
     */
    SERIAL(1),
    /**
     * 并行执行（错峰）
     */
    PARALLEL(2);

    /**
     * 执行模式
     */
    @JsonValue
    private final int mode;

    RollingExecutionModeEnum(int mode) {
        this.mode = mode;
    }

    @JsonCreator
    public static RollingExecutionModeEnum valOf(int mode) {
        for (RollingExecutionModeEnum modeEnum : values()) {
            if (modeEnum.mode == mode) {
                return modeEnum;
            }
        }
        throw new IllegalArgumentException("No RollingExecutionModeEnum constant: " + mode);
    }

    public int getValue() {
        return mode;
    }

    public static boolean isValid(Integer mode) {
        if (mode == null) {
            return false;
        }
        try {
            valOf(mode);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 是否为并行模式
     *
     * @param mode 执行模式值
     * @return 是否并行
     */
    public static boolean isParallel(Integer mode) {
        return mode != null && PARALLEL.mode == mode;
    }
}
