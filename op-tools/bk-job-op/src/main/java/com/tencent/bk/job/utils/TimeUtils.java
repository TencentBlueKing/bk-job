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

package com.tencent.bk.job.utils;

import org.apache.commons.lang3.tuple.Pair;

/**
 * 时间工具类
 */
public class TimeUtils {

    /**
     * 通过相对时间计算起始时间
     * @param relativeTime 相对时间，格式为：1d/1h/1m
     * @return 起始时间，时间戳，单位毫秒
     */
    public static Long getTimesByRelativeTime(String relativeTime, long endTime) {
        if (relativeTime == null || relativeTime.isEmpty()) {
            throw new IllegalArgumentException("relativeTime不能为空");
        }
        String unit = relativeTime.substring(relativeTime.length() - 1).toLowerCase();
        long value;
        try {
            value = Long.parseLong(relativeTime.substring(0, relativeTime.length() - 1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("relativeTime格式不正确: " + relativeTime);
        }
        long durationMillis = switch (unit) {
            case "d" -> value * 24 * 60 * 60 * 1000L;
            case "h" -> value * 60 * 60 * 1000L;
            case "m" -> value * 60 * 1000L;
            default -> throw new IllegalArgumentException("不支持的时间单位: " + unit + "，仅支持d/h/m");
        };
        return endTime - durationMillis;
    }

    /**
     * 计算从当前时间往前算relativeTime的起始和结束时间
     * @param relativeTime 相对时间，格式为：1d/1h/1m
     * @return 起始时间+结束时间，时间戳，单位毫秒
     */
    public static Pair<Long, Long> getTimesByRelativeTimeFromNow(String relativeTime) {
        long endTime = System.currentTimeMillis();
        Long startTime = getTimesByRelativeTime(relativeTime, endTime);
        return Pair.of(startTime, endTime);
    }
}
