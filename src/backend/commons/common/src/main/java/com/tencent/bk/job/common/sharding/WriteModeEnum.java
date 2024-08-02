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

package com.tencent.bk.job.common.sharding;

/**
 * DB 写模式
 */
public enum WriteModeEnum {
    /**
     * DB 写模式 - 写单库
     */
    WRITE_ORIGIN(Constants.WRITE_ORIGIN),
    /**
     * DB 写模式 - 写分库
     */
    WRITE_SHARDING(Constants.WRITE_SHARDING),
    /**
     * DB 写模式 - 双写
     */
    WRITE_BOTH(Constants.WRITE_BOTH);

    public static class Constants {
        public static final int WRITE_ORIGIN = 1;
        public static final int WRITE_SHARDING = 2;
        public static final int WRITE_BOTH = 3;
    }


    WriteModeEnum(int mode) {
        this.mode = mode;
    }

    private final int mode;

    public int getValue() {
        return mode;
    }

    public static WriteModeEnum valOf(int mode) {
        for (WriteModeEnum modeEnum : values()) {
            if (modeEnum.getValue() == mode) {
                return modeEnum;
            }
        }
        throw new IllegalArgumentException("No WriteModeEnum constant: " + mode);
    }
}
