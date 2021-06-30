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

package com.tencent.bk.job.execute.constants;

/**
 * 用户操作
 */
public enum UserOperationEnum {
    RETRY_STEP_FAIL(1, "失败重试"),
    IGNORE_ERROR(2, "忽略错误"),
    SKIP_STEP(3, "手动跳过"),
    TERMINATE_JOB(4, "强制终止"),
    RETRY_STEP_ALL(5, "全部重试"),
    START(6, "启动作业"),
    CONFIRM_CONTINUE(7, "人工确认-继续"),
    CONFIRM_TERMINATE(8, "人工确认-终止"),
    NEXT_STEP(9, "进入下一步"),
    CONFIRM_RESTART(10, "重新发起确认");

    private final Integer value;
    private final String name;

    UserOperationEnum(Integer val, String name) {
        this.value = val;
        this.name = name;
    }

    public static UserOperationEnum valueOf(int status) {
        for (UserOperationEnum runStatusEnum : values()) {
            if (runStatusEnum.getValue() == status) {
                return runStatusEnum;
            }
        }
        return null;
    }

    public Integer getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public String getI18nKey() {
        return "user.operation." + this.name().toLowerCase();
    }
}
