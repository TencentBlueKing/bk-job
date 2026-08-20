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

package com.tencent.bk.job.crontab.model.esb.v4;

import com.tencent.bk.job.crontab.common.constants.CronStatusEnum;

/**
 * v4 协议的定时任务状态：1-已启用，0-已停用。
 * <p>
 * 不复用内部的 {@link CronStatusEnum}：它的 2 表示停用，是 v3 时期遗留的取值，
 * 与"布尔语义的状态用 1/0"这一常规约定相悖。v4 是新协议，在此纠正为 1/0，
 * 内部模型与 v3 协议保持不变，两者的转换只发生在 v4 接口边界上。
 */
public enum V4CronStatusEnum {

    /**
     * 已停用：不再按计划触发
     */
    DISABLED(0),

    /**
     * 已启用：按计划触发
     */
    ENABLED(1);

    private final int status;

    V4CronStatusEnum(int status) {
        this.status = status;
    }

    /**
     * @return 取值非法时返回 null，由调用方决定如何报错
     */
    public static V4CronStatusEnum valOf(Integer status) {
        if (status == null) {
            return null;
        }
        for (V4CronStatusEnum value : values()) {
            if (value.status == status) {
                return value;
            }
        }
        return null;
    }

    public static V4CronStatusEnum of(boolean enabled) {
        return enabled ? ENABLED : DISABLED;
    }

    public int getStatus() {
        return status;
    }

    public boolean isEnabled() {
        return this == ENABLED;
    }
}
