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

package com.tencent.bk.job.manage.model.dto;

import com.tencent.bk.job.manage.api.common.constants.RuleMatchHandleActionEnum;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptCheckErrorLevelEnum;
import com.tencent.bk.job.manage.model.inner.ServiceScriptCheckResultItemDTO;
import lombok.Data;

/**
 * 脚本内容检查结果
 */
@Data
public class ScriptCheckResultItemDTO {
    /**
     * 错误所在行数
     */
    private int line;
    /**
     * 错误级别
     */
    private ScriptCheckErrorLevelEnum level;
    /**
     * 脚本检查项
     */
    private String checkItemCode;
    /**
     * 脚本检查项描述
     */
    private String description;
    /**
     * 脚本所在行的内容
     */
    private String lineContent;
    /**
     * 匹配的内容
     */
    private String matchContent;

    /**
     * 规则ID
     */
    private Long ruleId;
    /**
     * 规则表达式
     */
    private String ruleExpression;
    /**
     * 匹配规则处理动作
     */
    private RuleMatchHandleActionEnum action;

    public ServiceScriptCheckResultItemDTO toServiceScriptCheckResultDTO() {
        return new ServiceScriptCheckResultItemDTO(line, level, description, lineContent, matchContent, ruleId,
            ruleExpression, action);
    }
}
