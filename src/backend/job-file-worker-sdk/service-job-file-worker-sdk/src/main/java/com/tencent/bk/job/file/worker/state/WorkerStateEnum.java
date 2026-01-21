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

package com.tencent.bk.job.file.worker.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * File-Worker状态枚举值
 */
public enum WorkerStateEnum {
    STARTING(1, "启动中"),
    WAIT_ACCESS_READY(2, "等待自身可被外界访问"),
    HEART_BEATING(3, "心跳中"),
    HEART_BEAT_WAIT(4, "等待下一次心跳中"),
    RUNNING(5, "运行中"),
    OFFLINE_ING(6, "下线中"),
    OFFLINE_FAILED(7, "下线失败"),
    OFFLINE(8, "已下线");

    /**
     * 状态值
     */
    @JsonValue
    private final int state;
    /**
     * 状态描述
     */
    private final String description;

    WorkerStateEnum(int state, String description) {
        this.state = state;
        this.description = description;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static WorkerStateEnum valOf(int state) {
        for (WorkerStateEnum workerState : values()) {
            if (workerState.state == state) {
                return workerState;
            }
        }
        return null;
    }

    public int getValue() {
        return state;
    }
}
