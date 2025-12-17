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

package com.tencent.bk.job.execute.engine.util;

import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.execute.config.ResourceScopeTaskTimeoutParser;

import static com.tencent.bk.job.common.constant.JobConstants.DEFAULT_JOB_TIMEOUT_SECONDS;
import static com.tencent.bk.job.common.constant.JobConstants.MAX_JOB_TIMEOUT_SECONDS;

public class TimeoutUtils {

    /**
     * 调整任务超时时间
     *
     * @param timeout 超时时间
     * @param appId   业务ID
     * @return 超时时间
     */
    public static Integer adjustTaskTimeout(Long appId, Integer timeout) {
        if (timeout == null) {
            return DEFAULT_JOB_TIMEOUT_SECONDS;
        }

        ResourceScopeTaskTimeoutParser resourceScopeTaskTimeoutParser = ApplicationContextRegister.getBean(
            ResourceScopeTaskTimeoutParser.class);
        int maxTimeout = resourceScopeTaskTimeoutParser.getMaxTimeoutOrDefault(appId, MAX_JOB_TIMEOUT_SECONDS);

        Integer finalTimeout = timeout;
        if (timeout <= 0 || timeout > maxTimeout) {
            finalTimeout = maxTimeout;
        }
        return finalTimeout;
    }
}
