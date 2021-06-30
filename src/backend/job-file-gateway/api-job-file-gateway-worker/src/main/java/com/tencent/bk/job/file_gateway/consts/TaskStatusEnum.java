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

package com.tencent.bk.job.file_gateway.consts;

import java.util.HashSet;
import java.util.Set;

public enum TaskStatusEnum {
    INIT((byte) 1, "init"),
    DISPATCH_FAILED((byte) 2, "dispatch failed"),
    RUNNING((byte) 3, "running"),
    FAILED((byte) 4, "failed"),
    SUCCESS((byte) 5, "success"),
    STOPPED((byte) 6, "stopped");

    private final Byte status;
    private final String description;

    TaskStatusEnum(Byte status, String description) {
        this.status = status;
        this.description = description;
    }

    public static TaskStatusEnum valueOf(Byte status) {
        TaskStatusEnum[] values = TaskStatusEnum.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].getStatus().equals(status)) return values[i];
        }
        return null;
    }

    /**
     * 任务处于终止态
     *
     * @return
     */
    public static boolean isDone(Byte status) {
        return status != null && (status.equals(DISPATCH_FAILED.status)
            || status.equals(TaskStatusEnum.SUCCESS.status)
            || status.equals(TaskStatusEnum.FAILED.status)
            || status.equals(TaskStatusEnum.STOPPED.status)
        );
    }

    public static Set<Byte> getFinishedStatusSet() {
        Set<Byte> runningStatusSet = new HashSet<>();
        runningStatusSet.add(DISPATCH_FAILED.status);
        runningStatusSet.add(FAILED.status);
        runningStatusSet.add(SUCCESS.status);
        runningStatusSet.add(STOPPED.status);
        return runningStatusSet;
    }

    public static Set<Byte> getRunningStatusSet() {
        Set<Byte> runningStatusSet = new HashSet<>();
        runningStatusSet.add(INIT.status);
        runningStatusSet.add(RUNNING.status);
        return runningStatusSet;
    }

    public Byte getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }
}
