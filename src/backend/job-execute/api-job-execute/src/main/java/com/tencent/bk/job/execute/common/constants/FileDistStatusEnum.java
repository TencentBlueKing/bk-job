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

package com.tencent.bk.job.execute.common.constants;

/**
 * 单个文件分发对（源执行对象，源文件路径，目标执行对象，目标文件路径）的文件分发状态，体现在详情页文件分发日志中
 */
public enum FileDistStatusEnum {
    PULLING(0, "Pulling from third file source"),
    WAITING(1, "Waiting"),
    UPLOADING(2, "Uploading"),
    DOWNLOADING(3, "Downloading"),
    FINISHED(4, "Finished"),
    FAILED(5, "Failed");

    private final Integer value;
    private final String name;

    FileDistStatusEnum(Integer val, String name) {
        this.value = val;
        this.name = name;
    }

    public static FileDistStatusEnum getFileDistStatus(Integer status) {
        if (status == null) {
            throw new IllegalArgumentException("Null FileDistStatusEnum constant");
        }
        for (FileDistStatusEnum fileDistStatus : values()) {
            if (fileDistStatus.getValue().equals(status)) {
                return fileDistStatus;
            }
        }
        throw new IllegalArgumentException("No FileDistStatusEnum constant: " + status);
    }

    public Integer getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public static boolean isFinishedStatus(FileDistStatusEnum status) {
        return status == FINISHED || status == FAILED;
    }

    public static boolean isFinishedStatus(Integer statusValue) {
        return statusValue.equals(FINISHED.value) || statusValue.equals(FAILED.value);
    }
}
