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

package com.tencent.bk.job.execute.common.context;

import io.micrometer.context.ThreadLocalAccessor;

/**
 * 让 {@link JobExecuteContext} 接入 Micrometer Context Propagation 体系，
 * 通过 {@link io.micrometer.context.ContextSnapshotFactory#captureAll()} 自动随 trace 上下文
 * 一起在线程池间传播。
 */
public class JobExecuteContextThreadLocalAccessor implements ThreadLocalAccessor<JobExecuteContext> {

    public static final String KEY = JobExecuteContext.KEY;

    @Override
    public Object key() {
        return KEY;
    }

    @Override
    public JobExecuteContext getValue() {
        return JobExecuteContextThreadLocalRepo.get();
    }

    @Override
    public void setValue(JobExecuteContext value) {
        JobExecuteContextThreadLocalRepo.set(value);
    }

    @Override
    public void setValue() {
        JobExecuteContextThreadLocalRepo.unset();
    }
}
