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

package com.tencent.bk.job.manage.manager.script.check.checker;

import com.tencent.bk.job.manage.api.common.constants.RuleMatchHandleActionEnum;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptCheckErrorLevelEnum;
import com.tencent.bk.job.manage.model.dto.ScriptCheckResultItemDTO;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * 脚本检查器
 */
public interface ScriptChecker extends Callable<List<ScriptCheckResultItemDTO>> {

    ScriptCheckErrorLevelEnum level();

    /**
     * 构造检查结果
     *
     * @param lineNumber    行号
     * @param checkItemCode 检查项
     * @param description   检查项描述
     * @param lineContent   当前行的脚本内容
     * @param matchContent  匹配的内容
     * @return 检查结果
     */
    default ScriptCheckResultItemDTO createResult(int lineNumber, String checkItemCode, String description,
                                                  String lineContent, String matchContent) {
        return createResult(null, null, null, lineNumber,
            checkItemCode, description, lineContent, matchContent);
    }

    /**
     * 构造检查结果
     *
     * @param ruleId         高危规则ID
     * @param ruleExpression 高危规则表达式
     * @param action         高危规则匹配处理动作
     * @param lineNumber     行号
     * @param checkItemCode  检查项
     * @param description    检查项描述
     * @param lineContent    当前行的脚本内容
     * @param matchContent   匹配的内容
     * @return 检查结果
     */
    default ScriptCheckResultItemDTO createResult(Long ruleId, String ruleExpression, RuleMatchHandleActionEnum action,
                                                  int lineNumber, String checkItemCode, String description,
                                                  String lineContent, String matchContent) {
        ScriptCheckResultItemDTO result = new ScriptCheckResultItemDTO();
        result.setLine(lineNumber);
        result.setLevel(level());
        result.setCheckItemCode(checkItemCode);
        result.setDescription(description);
        result.setLineContent(lineContent);
        result.setMatchContent(matchContent);
        result.setRuleId(ruleId);
        result.setRuleExpression(ruleExpression);
        result.setAction(action);
        return result;
    }
}
