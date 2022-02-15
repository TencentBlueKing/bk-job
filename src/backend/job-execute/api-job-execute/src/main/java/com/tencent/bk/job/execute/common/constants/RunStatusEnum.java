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
import java.util.Collections;
import java.util.List;

/**
 * 作业执行状态
 */
public enum RunStatusEnum {
    /**
     * 等待执行
     */
    BLANK(1),
    /**
     * 正在执行
     */
    RUNNING(2),
    /**
     * 执行成功
     */
    SUCCESS(3),
    /**
     * 执行失败
     */
    FAIL(4),
    /**
     * 跳过
     */
    SKIPPED(5),
    /**
     * 忽略错误
     */
    IGNORE_ERROR(6),
    /**
     * 等待用户
     */
    WAITING(7),
    /**
     * 手动结束
     */
    TERMINATED(8),
    /**
     * 状态异常
     */
    ABNORMAL_STATE(9),
    /**
     * 强制终止中
     */
    STOPPING(10),
    /**
     * 强制终止成功
     */
    STOP_SUCCESS(11),
    /**
     * 确认终止
     */
    CONFIRM_TERMINATED(13),
    /**
     * 被丢弃
     */
    ABANDONED(14),
    /**
     * 滚动等待
     */
    ROLLING_WAITING(15);

    private final Integer value;

    private static final List<Integer> FINISHED_STATUS_LIST = new ArrayList<>();

    static {
        FINISHED_STATUS_LIST.add(SUCCESS.value);
        FINISHED_STATUS_LIST.add(FAIL.value);
        FINISHED_STATUS_LIST.add(SKIPPED.value);
        FINISHED_STATUS_LIST.add(IGNORE_ERROR.value);
        FINISHED_STATUS_LIST.add(TERMINATED.value);
        FINISHED_STATUS_LIST.add(ABNORMAL_STATE.value);
        FINISHED_STATUS_LIST.add(STOP_SUCCESS.value);
        FINISHED_STATUS_LIST.add(CONFIRM_TERMINATED.value);
        FINISHED_STATUS_LIST.add(ABANDONED.value);
    }

    RunStatusEnum(Integer val) {
        this.value = val;
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
     */
    public static List<Integer> getFinishedStatusValueList() {
        return Collections.unmodifiableList(FINISHED_STATUS_LIST);
    }

    /**
     * 是否终止态
     *
     * @param status 状态
     */
    public static boolean isFinishedStatus(RunStatusEnum status) {
        if (status == null) {
            return false;
        }
        return FINISHED_STATUS_LIST.contains(status.value);
    }

    public Integer getValue() {
        return value;
    }

    /**
     * 获取国际化Key
     **/
    public String getI18nKey() {
        return "task.run.status." + this.name().toLowerCase();
    }
}
