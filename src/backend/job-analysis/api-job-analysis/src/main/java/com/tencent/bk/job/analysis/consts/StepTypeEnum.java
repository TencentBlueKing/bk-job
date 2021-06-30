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

package com.tencent.bk.job.analysis.consts;

/**
 * 作业模板步骤类型
 */
public enum StepTypeEnum {
    FILE_LOCAL(1, "文件分发_本地文件源"),
    FILE_SERVER(2, "文件分发_服务器文件源"),
    SCRIPT_MANUAL(3, "脚本执行_手工录入"),
    SCRIPT_REF(4, "脚本执行_脚本引用"),
    CONFIRM(5, "人工确认");

    private final int value;
    private final String name;

    StepTypeEnum(int val, String name) {
        this.value = val;
        this.name = name;
    }

    public static StepTypeEnum getStepType(int value) {
        for (StepTypeEnum stepTypeEnum : StepTypeEnum.values()) {
            if (stepTypeEnum.getValue() == value) {
                return stepTypeEnum;
            }
        }
        throw new RuntimeException("Unknown StepType value " + value);
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public String getI18nKey() {
        if (this == FILE_LOCAL) {
            return "step.type.file.local";
        } else if (this == FILE_SERVER) {
            return "step.type.file.server";
        } else if (this == SCRIPT_MANUAL) {
            return "step.type.script.manual";
        } else if (this == SCRIPT_REF) {
            return "step.type.script.ref";
        } else if (this == CONFIRM) {
            return "step.type.confirm";
        } else {
            return "";
        }
    }
}
