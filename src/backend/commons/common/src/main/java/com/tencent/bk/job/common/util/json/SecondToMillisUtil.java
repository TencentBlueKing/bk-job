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

package com.tencent.bk.job.common.util.json;

/**
 * 秒级/毫秒级 Unix 时间戳统一转换为毫秒级时间戳的工具类。
 */
public final class SecondToMillisUtil {

    private static final long MAX_SECOND_TIMESTAMP = 9_999_999_999L;

    private SecondToMillisUtil() {
    }

    /**
     * 将时间戳转换为毫秒级时间戳。
     * 若入参已是毫秒级（大于 9999999999），则原样返回。
     *
     * @param timestamp 时间戳（秒级或毫秒级）
     * @return 毫秒级时间戳
     */
    public static Long toMillis(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        if (timestamp > MAX_SECOND_TIMESTAMP) {
            return timestamp;
        }
        return timestamp * 1000L;
    }
}
