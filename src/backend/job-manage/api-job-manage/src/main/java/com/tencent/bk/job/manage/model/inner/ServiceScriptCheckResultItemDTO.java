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

package com.tencent.bk.job.manage.model.inner;

import com.tencent.bk.job.manage.common.consts.RuleMatchHandleActionEnum;
import com.tencent.bk.job.manage.common.consts.script.ScriptCheckErrorLevelEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 脚本检查结果项
 */
@Data
@NoArgsConstructor
public class ServiceScriptCheckResultItemDTO {
    /**
     * 错误所在行数
     */
    private int line;
    /**
     * 错误级别
     *
     * @see ScriptCheckErrorLevelEnum
     */
    private Integer level;
    /**
     * 错误描述
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
     * 高危脚本处理动作
     */
    private Integer action;

    public ServiceScriptCheckResultItemDTO(int line, Integer level, String description,
                                           String lineContent, String matchContent, Long ruleId, String ruleExpression,
                                           Integer action) {
        this.line = line;
        this.level = level;
        this.description = description;
        this.lineContent = lineContent;
        this.matchContent = matchContent;
        this.ruleId = ruleId;
        this.ruleExpression = ruleExpression;
        this.action = action;
    }

    public ServiceScriptCheckResultItemDTO(int line, ScriptCheckErrorLevelEnum level, String description,
                                           String lineContent, String matchContent, Long ruleId, String ruleExpression,
                                           RuleMatchHandleActionEnum action) {
        this(line, level.getValue(), description, lineContent, matchContent, ruleId, ruleExpression, action.getValue());
    }
}
