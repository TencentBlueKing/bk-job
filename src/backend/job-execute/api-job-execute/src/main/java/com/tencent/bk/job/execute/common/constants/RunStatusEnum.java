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

import java.util.ArrayList;
import java.util.List;

/**
 * 作业执行状态
 */
public enum RunStatusEnum {
    BLANK(1, "等待执行"), RUNNING(2, "正在执行"), SUCCESS(3, "执行成功"),
    FAIL(4, "执行失败"), SKIPPED(5, "跳过"), IGNORE_ERROR(6, "忽略错误"),
    WAITING(7, "等待用户"), TERMINATED(8, "手动结束"), ABNORMAL_STATE(9, "状态异常"),
    STOPPING(10, "强制终止中"), STOP_SUCCESS(11, "强制终止成功"), CONFIRM_TERMINATED(13, "确认终止");

    private final Integer value;
    private final String name;

    RunStatusEnum(Integer val, String name) {
        this.value = val;
        this.name = name;
    }

    public static RunStatusEnum valueOf(int status) {
        for (RunStatusEnum runStatusEnum : values()) {
            if (runStatusEnum.getValue() == status) {
                return runStatusEnum;
            }
        }
        return null;
    }

    /**
     * 获取终止态的状态列表
     *
     * @return
     */
    public static List<Integer> getFinishedStatusValueList() {
        List<Integer> finishedStatusValueList = new ArrayList<>();
        finishedStatusValueList.add(SUCCESS.value);
        finishedStatusValueList.add(FAIL.value);
        finishedStatusValueList.add(SKIPPED.value);
        finishedStatusValueList.add(IGNORE_ERROR.value);
        finishedStatusValueList.add(TERMINATED.value);
        finishedStatusValueList.add(ABNORMAL_STATE.value);
        finishedStatusValueList.add(STOP_SUCCESS.value);
        finishedStatusValueList.add(CONFIRM_TERMINATED.value);
        return finishedStatusValueList;
    }

    public Integer getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public String getI18nKey() {
        return "task.run.status." + this.name().toLowerCase();
    }
}
