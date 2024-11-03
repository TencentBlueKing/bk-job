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

package com.tencent.bk.job.common.mysql.dynamic.ds;

import lombok.Getter;

/**
 * 数据源模式
 */
@Getter
public enum DataSourceMode {
    /**
     * 单点 DB
     */
    STANDALONE(Constants.STANDALONE),
    /**
     * 垂直分库
     */
    VERTICAL_SHARDING(Constants.VERTICAL_SHARDING),
    /**
     * 水平分库
     */
    HORIZONTAL_SHARDING(Constants.HORIZONTAL_SHARDING);

    public static class Constants {
        public final static String STANDALONE = "standalone";
        public final static String VERTICAL_SHARDING = "vertical_sharding";
        public final static String HORIZONTAL_SHARDING = "horizontal_sharding";
    }

    private final String mode;

    DataSourceMode(String mode) {
        this.mode = mode;
    }

    public static DataSourceMode valOf(String mode) {
        if (mode == null) {
            return null;
        }
        for (DataSourceMode value : values()) {
            if (value.getMode().equals(mode)) {
                return value;
            }
        }
        throw new IllegalArgumentException("No DataSourceMode constant: " + mode);
    }

    public static boolean checkValid(String mode) {
        if (mode == null) {
            return false;
        }
        for (DataSourceMode value : values()) {
            if (value.getMode().equals(mode)) {
                return true;
            }
        }
        return false;
    }
}
