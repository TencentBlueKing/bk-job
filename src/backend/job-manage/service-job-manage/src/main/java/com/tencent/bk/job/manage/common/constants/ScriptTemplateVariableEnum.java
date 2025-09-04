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

package com.tencent.bk.job.manage.common.constants;

public enum ScriptTemplateVariableEnum {
    DATETIME("datetime", "2020-01-01 20:00:00"),
    USERNAME("username", "admin"),
    DISPLAY_NAME("display_name", "admin"),
    BIZ_ID("biz_id", "10000"),
    BIZ_NAME("biz_name", "Demo Business");

    private final String name;
    private final String descI18nKey;
    private final String demo;

    ScriptTemplateVariableEnum(String name, String demo) {
        this.name = name;
        this.descI18nKey = "job.custom.settings.script.template.variable." + name + ".desc";
        this.demo = demo;
    }

    public String getName() {
        return name;
    }

    public String getDescI18nKey() {
        return descI18nKey;
    }

    public String getDemo() {
        return demo;
    }
}
