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

package com.tencent.bk.job.common.util;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 用于统一输出兼容性代码被调用时的日志，用于后续删除兼容代码时的检查
 */
@Slf4j
public class CompatibleLogUtil {
    /**
     * 标明是兼容代码被调用时产生的日志前缀
     */
    public static final String COMPATIBLE_LOG_PREFIX = "[COMPATIBLE]";

    // ======================= mongodb 文件任务日志相关 ======================

    public static final AtomicLong FILE_TASK_LOG_OLD_INVOKE_COUNT = new AtomicLong(0);
    /**
     * 日志采样率，用于控制兼容性日志的输出频率（每100次记录一次）
     */
    public static final Integer FILE_TASK_LOG_OLD_INVOKE_SAMPLE_RATE = 100;

    public static void logOldLogicLogFileTaskLogInvoke(String infoContent) {
        long currentCount = FILE_TASK_LOG_OLD_INVOKE_COUNT.getAndIncrement();
        if (currentCount % FILE_TASK_LOG_OLD_INVOKE_SAMPLE_RATE == 0) {
            log.info(COMPATIBLE_LOG_PREFIX + infoContent);
            if (currentCount >= Integer.MAX_VALUE) {
                FILE_TASK_LOG_OLD_INVOKE_COUNT.set(0);
            }
        }
    }

}
