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

package com.tencent.bk.job.execute.engine.consts;

/**
 * 步骤执行Action
 */
public enum StepActionEnum {
    /**
     * 启动步骤
     */
    START(1),
    /**
     * 跳过步骤-引擎暂不支持
     */
    SKIP(2),
    /**
     * 重试失败IP
     */
    RETRY_FAIL(3),
    /**
     * 停止步骤
     */
    STOP(4),
    /**
     * 忽略错误并进入下一步
     */
    IGNORE_ERROR(5),
    /**
     * 进入下一步
     */
    NEXT_STEP(6),
    /**
     * 全部重试
     */
    RETRY_ALL(7),
    /**
     * 人工确认-终止流程
     */
    CONFIRM_TERMINATE(8),
    /**
     * 人工确认-重新发起确认
     */
    CONFIRM_RESTART(9),
    /**
     * 人工确认-重新发起确认
     */
    CONFIRM_CONTINUE(10),
    /**
     * 第三方文件源文件拉取完成后继续GSE分发
     */
    CONTINUE_FILE_PUSH(11),
    /**
     * 步骤完成后执行清理动作
     */
    CLEAR(12),
    /**
     * 恢复步骤执行
     */
    RESUME(13);

    private final int value;

    StepActionEnum(int val) {
        this.value = val;
    }

    public int getValue() {
        return value;
    }
}
