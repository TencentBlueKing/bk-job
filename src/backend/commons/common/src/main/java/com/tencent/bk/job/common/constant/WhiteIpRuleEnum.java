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

package com.tencent.bk.job.common.constant;

/**
 * @since 11/11/2019 17:40
 */
public enum WhiteIpRuleEnum {
    /**
     * 白名单可操作的类型
     */
    DENY((byte) 0, "不允许白名单"), FAST_SCRIPT((byte) 1, "快速执行脚本操作"), FAST_PUSH_FILE((byte) 2, "快速分发文件操作"),
    FAST_SQL((byte) 3, "快速执行SQL操作"), ALL((byte) 9, "所有操作");

    private final byte value;
    private final String name;

    WhiteIpRuleEnum(byte value, String name) {
        this.value = value;
        this.name = name;
    }

    public static WhiteIpRuleEnum valueOf(byte value) {
        for (WhiteIpRuleEnum whiteIpRule : WhiteIpRuleEnum.values()) {
            if (whiteIpRule.getValue() == value) {
                return whiteIpRule;
            }
        }
        return null;
    }

    public byte getValue() {
        return value;
    }

    public String getName() {
        return name;
    }
}
