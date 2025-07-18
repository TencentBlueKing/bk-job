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

package com.tencent.bk.job.execute.model;

import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 脚本日志内容
 */
@Data
@NoArgsConstructor
public class ScriptExecuteObjectLogContent {
    /**
     * 步骤实例ID
     */
    private long stepInstanceId;
    /**
     * 执行次数
     */
    private int executeCount;
    /**
     * 执行对象
     */
    private ExecuteObject executeObject;
    /**
     * 日志内容
     */
    private String content;
    /**
     * 日志是否拉取完成
     */
    private boolean finished;

    public ScriptExecuteObjectLogContent(long stepInstanceId,
                                         int executeCount,
                                         ExecuteObject executeObject,
                                         String content,
                                         boolean finished) {
        this.stepInstanceId = stepInstanceId;
        this.executeCount = executeCount;
        this.executeObject = executeObject;
        this.content = content;
        this.finished = finished;
    }
}
