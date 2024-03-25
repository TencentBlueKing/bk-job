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

package com.tencent.bk.job.common.util;

import lombok.extern.slf4j.Slf4j;

/**
 * 线程操作工具类
 */
@Slf4j
public class ThreadUtils {
    /**
     * 线程sleep
     *
     * @param sleepInMills sleep时间
     */
    public static void sleep(long sleepInMills) {
        try {
            Thread.sleep(sleepInMills);
        } catch (InterruptedException e) {
            log.warn("Caught InterruptedException", e);
        }
    }


    /**
     * 线程sleep
     *
     * @param sleepInMills   sleep时间
     * @param logInterrupted 是否输出线程打断的错误日志
     */
    public static void sleep(long sleepInMills, boolean logInterrupted) {
        try {
            Thread.sleep(sleepInMills);
        } catch (InterruptedException e) {
            if (logInterrupted) {
                log.warn("Caught InterruptedException", e);
            }
        }
    }
}
