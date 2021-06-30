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
 * 作业启动方式
 */
public enum TaskStartupModeEnum {
    NORMAL(1, "页面执行"), API(2, "API调用"), CRON(3, "定时执行");

    private final int value;
    private final String name;

    TaskStartupModeEnum(int val, String name) {
        this.value = val;
        this.name = name;
    }

    public static TaskStartupModeEnum getStartupMode(int value) {
        for (TaskStartupModeEnum taskStartupMode : TaskStartupModeEnum.values()) {
            if (taskStartupMode.getValue() == value) {
                return taskStartupMode;
            }
        }
        throw new RuntimeException("Unknown TaskStartupMode value " + value);
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public String getI18nKey() {
        if (this == NORMAL) {
            return "task.startup.mode.normal";
        } else if (this == API) {
            return "task.startup.mode.api";
        } else if (this == CRON) {
            return "task.startup.mode.cron";
        } else {
            return "";
        }
    }
}
