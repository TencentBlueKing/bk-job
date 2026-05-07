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

package com.tencent.bk.job.analysis.model.esb.v4.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.function.Function;

/**
 * 任务上下文字段
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class TaskContextField {

    private static final String I18N_KEY_PREFIX = "analysis.taskContext.fieldDescription.";

    /**
     * 字段名称
     */
    private String name;
    /**
     * 字段值
     */
    private String value;
    /**
     * 字段描述
     */
    private String description;

    /**
     * 根据 name 自动生成 i18n key 并解析 description。
     * i18n key 规则：analysis.taskContext.fieldDescription.{name}
     *
     * @param name         字段名称
     * @param value        字段值
     * @param i18nResolver i18n 解析函数，例如 messageI18nService::getI18n
     */
    public TaskContextField(String name, String value, Function<String, String> i18nResolver) {
        this.name = name;
        this.value = value;
        this.description = i18nResolver.apply(I18N_KEY_PREFIX + name);
    }
}
