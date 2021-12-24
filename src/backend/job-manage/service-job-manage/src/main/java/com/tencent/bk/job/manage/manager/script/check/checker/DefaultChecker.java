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

package com.tencent.bk.job.manage.manager.script.check.checker;

import com.tencent.bk.job.manage.common.consts.RuleMatchHandleActionEnum;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.manager.script.check.ScriptCheckParam;
import com.tencent.bk.job.manage.model.dto.ScriptCheckResultItemDTO;
import com.tencent.bk.job.manage.model.dto.globalsetting.DangerousRuleDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 默认脚本检查
 */
@Slf4j
public abstract class DefaultChecker implements ScriptChecker {

    protected ScriptCheckParam param;

    DefaultChecker(ScriptCheckParam param) {
        this.param = param;
    }

    protected void checkScriptLines(List<ScriptCheckResultItemDTO> results, String[] lines, Pattern pattern,
                                    String checkItemCode) {
        checkScriptLines(null, null, null, results, lines, pattern, checkItemCode, null);
    }

    protected void checkScriptLines(Long ruleId, String ruleExpression, RuleMatchHandleActionEnum action,
                                    List<ScriptCheckResultItemDTO> results, String[] lines,
                                    Pattern pattern, String checkItemCode, String description) {
        if (lines == null || lines.length == 0) {
            return;
        }

        Matcher matcher;
        int lineNumber = 1;
        while (lineNumber <= lines.length) {
            String lineContent = lines[lineNumber - 1];
            if (isComment(param.getScriptType(), lineContent) || StringUtils.isBlank(lineContent)) {
                lineNumber++;
                continue;
            }
            matcher = pattern.matcher(lineContent);
            if (matcher.find()) {
                results.add(createResult(ruleId, ruleExpression, action, lineNumber, checkItemCode,
                    description, lineContent, matcher.group()));
            }
            lineNumber++;
        }
    }

    protected boolean isComment(ScriptTypeEnum scriptType, String line) {
        switch (scriptType) {
            case SHELL:
                return isShellComment(line);
            case BAT:
                return isBatComment(line);
            case PERL:
                return isPerlComment(line);
            case PYTHON:
                return isPythonComment(line);
            case POWERSHELL:
                return isPowerShellComment(line);
            default:
                return false;
        }
    }

    private boolean isShellComment(String line) {
        return line.trim().startsWith("#");
    }

    private boolean isBatComment(String line) {
        return line.trim().startsWith("REM");
    }

    private boolean isPerlComment(String line) {
        return line.trim().startsWith("#");
    }

    private boolean isPythonComment(String line) {
        return line.trim().startsWith("#");
    }

    private boolean isPowerShellComment(String line) {
        return line.trim().startsWith("#");
    }

    protected void checkScriptLines(List<ScriptCheckResultItemDTO> results, String[] lines,
                                    DangerousRuleDTO dangerousRule) {
        Pattern pattern = Pattern.compile(dangerousRule.getExpression());
        checkScriptLines(dangerousRule.getId(), dangerousRule.getExpression(),
            RuleMatchHandleActionEnum.getRuleMatchHandleModeEnum(dangerousRule.getAction()), results, lines,
            pattern, null, dangerousRule.getDescription());
    }
}
