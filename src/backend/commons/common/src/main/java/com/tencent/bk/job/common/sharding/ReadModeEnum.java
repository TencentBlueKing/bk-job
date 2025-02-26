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
 * DB 读模式
 */
public enum ReadModeEnum {
    /**
     * 读模式-读单库
     */
    READ_ORIGIN(Constants.READ_ORIGIN_DB),
    /**
     * 读模式-读分库
     */
    READ_SHARDING(Constants.READ_SHARDING_DB);

    public static class Constants {
        public static final int READ_ORIGIN_DB = 1;
        public static final int READ_SHARDING_DB = 2;
    }


    ReadModeEnum(int mode) {
        this.mode = mode;
    }

    private final int mode;

    public int getValue() {
        return mode;
    }

    public static ReadModeEnum valOf(int mode) {
        for (ReadModeEnum modeEnum : values()) {
            if (modeEnum.getValue() == mode) {
                return modeEnum;
            }
        }
        throw new IllegalArgumentException("No ReadModeEnum constant: " + mode);
    }
}
