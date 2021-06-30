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
 * 作业类型
 */
public enum TaskTypeEnum {
    NORMAL(0, "普通作业"), SCRIPT(1, "快速脚本作业"), FILE(2, "文件分发作业");


    private final Integer value;
    private final String name;
    TaskTypeEnum(Integer value, String name) {
        this.value = value;
        this.name = name;
    }

    public static TaskTypeEnum valueOf(int type) {
        for (TaskTypeEnum taskType : values()) {
            if (taskType.getValue() == type) {
                return taskType;
            }
        }
        return null;
    }

    public Integer getValue() {
        return value;
    }

    public String getI18nKey() {
        if (this == NORMAL) {
            return "task.instance.type.normal";
        } else if (this == SCRIPT) {
            return "task.instance.type.script";
        } else if (this == FILE) {
            return "task.instance.type.file";
        } else {
            return "";
        }
    }
}
