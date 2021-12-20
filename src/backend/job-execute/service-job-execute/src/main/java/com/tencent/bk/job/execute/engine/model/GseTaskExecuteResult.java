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

package com.tencent.bk.job.execute.engine.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

/**
 * GSE 任务执行结果
 */
@Getter
@Setter
@ToString
public class GseTaskExecuteResult {
    public static final int RESULT_CODE_SUCCESS = 1;
    public static final int RESULT_CODE_FAILED = 2;
    public static final int RESULT_CODE_STOP_SUCCESS = 3;
    public static final int RESULT_CODE_STOP_FAILED = 4;
    /**
     * 任务被打断
     */
    public static final int RESULT_CODE_INTERRUPTED = 5;
    public static final int RESULT_CODE_RUNNING = 6;
    public static final int RESULT_CODE_SKIPPED = 7;
    /**
     * 任务被丢弃
     */
    public static final int RESULT_CODE_DISCARDED = 8;
    /**
     * 任务异常
     */
    public static final int RESULT_CODE_EXCEPTION = 9;

    public static final GseTaskExecuteResult SUCCESS = new GseTaskExecuteResult(RESULT_CODE_SUCCESS);
    public static final GseTaskExecuteResult FAILED = new GseTaskExecuteResult(RESULT_CODE_FAILED);
    public static final GseTaskExecuteResult STOP_SUCCESS = new GseTaskExecuteResult(RESULT_CODE_STOP_SUCCESS);
    public static final GseTaskExecuteResult STOP_FAILED = new GseTaskExecuteResult(RESULT_CODE_STOP_FAILED);
    public static final GseTaskExecuteResult INTERRUPTED = new GseTaskExecuteResult(RESULT_CODE_INTERRUPTED);
    public static final GseTaskExecuteResult RUNNING = new GseTaskExecuteResult(RESULT_CODE_RUNNING);
    public static final GseTaskExecuteResult SKIPPED = new GseTaskExecuteResult(RESULT_CODE_SKIPPED);
    public static final GseTaskExecuteResult DISCARDED = new GseTaskExecuteResult(RESULT_CODE_DISCARDED);
    public static final GseTaskExecuteResult EXCEPTION = new GseTaskExecuteResult(RESULT_CODE_EXCEPTION);

    private Integer resultCode;

    private String msg;


    public GseTaskExecuteResult(Integer resultCode, String msg) {
        this.resultCode = resultCode;
        this.msg = msg;
    }

    public GseTaskExecuteResult(Integer resultCode) {
        this.resultCode = resultCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GseTaskExecuteResult that = (GseTaskExecuteResult) o;
        return Objects.equals(resultCode, that.resultCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resultCode);
    }
}
