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

package com.tencent.bk.job.execute.engine.listener;

/**
 * execute模块MQ输入binding名称常量
 */
public class MqBindingNames {
    /**
     * 作业事件输入binding名称
     */
    public static final String HANDLE_JOB_EVENT = "handleJobEvent-in-0";
    /**
     * 步骤事件输入binding名称
     */
    public static final String HANDLE_STEP_EVENT = "handleStepEvent-in-0";
    /**
     * GSE任务事件输入binding名称
     */
    public static final String HANDLE_GSE_TASK_EVENT = "handleGseTaskEvent-in-0";
    /**
     * 执行结果重新调度事件输入binding名称
     */
    public static final String HANDLE_RESULT_HANDLE_RESUME_EVENT = "handleResultHandleResumeEvent-in-0";
    /**
     * 通知消息输入binding名称
     */
    public static final String HANDLE_NOTIFY_MSG = "handleNotifyMsg-in-0";
    /**
     * 回调消息输入binding名称
     */
    public static final String HANDLE_CALLBACK_MSG = "handleCallbackMsg-in-0";

}
