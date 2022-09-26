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
 * 作业执行action
 */
public enum JobActionEnum {
    /**
     * 启动作业
     */
    START(1),
    /**
     * 停止作业
     */
    STOP(2),
    /**
     * 从头开始执行作业（执行引擎暂不支持）
     */
    RESTART(3),
    /**
     * 刷新作业状态
     */
    REFRESH(4),
    /**
     * 进入下一步
     */
    NEXT_STEP(6),
    /**
     * 任务执行完成回调
     */
    CALLBACK(7);

    private final int value;

    JobActionEnum(int val) {
        this.value = val;
    }

    public static JobActionEnum valueOf(int value) {
        for (JobActionEnum jobAction : values()) {
            if (jobAction.getValue() == value) {
                return jobAction;
            }
        }
        throw new IllegalArgumentException("No JobActionEnum constant: " + value);
    }

    public int getValue() {
        return value;
    }
}
