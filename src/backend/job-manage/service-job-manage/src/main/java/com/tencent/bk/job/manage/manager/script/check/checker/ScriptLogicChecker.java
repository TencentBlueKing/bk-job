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

import com.google.common.collect.Lists;
import com.tencent.bk.job.manage.common.consts.script.ScriptCheckErrorLevelEnum;
import com.tencent.bk.job.manage.manager.script.check.ScriptCheckParam;
import com.tencent.bk.job.manage.model.dto.ScriptCheckResultItemDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.tencent.bk.job.manage.manager.script.check.consts.ScriptCheckItemCode.LOGIC_NEED_CD_DIR_CHECK;
import static com.tencent.bk.job.manage.manager.script.check.consts.ScriptCheckItemCode.LOGIC_NEED_SHEBANG;

/**
 * 脚本逻辑检查
 */
@Slf4j
public class ScriptLogicChecker extends DefaultChecker {
    private final Pattern shellDefine = Pattern.compile("^#!/bin/(ksh|bash|tsh|sh)$");
    private final Pattern cdDirCheck = Pattern.compile("^((\\s*?[^#c].*)(;|[|]{1,2}|[&]{1,2}))?(\\s*)cd(\\s+((" +
        "(?<b>[\"'])[^;&|\"']*\\k<b>)+|[^;&|\"']+))+(([;&|]*$)|(([|&]|[;]+)[^|&]+))");

    public ScriptLogicChecker(ScriptCheckParam param) {
        super(param);
    }

    @Override
    public ScriptCheckErrorLevelEnum level() {
        return ScriptCheckErrorLevelEnum.WARNING;
    }

    @Override
    public List<ScriptCheckResultItemDTO> call() {
        StopWatch watch = new StopWatch();
        ArrayList<ScriptCheckResultItemDTO> checkResults = Lists.newArrayList();
        String[] lines = param.getLines();
        try {
            if (!shellDefine.matcher(lines[0]).matches()) {
                checkResults.add(createResult(1, LOGIC_NEED_SHEBANG, null, lines[0], lines[0]));
            }

            watch.start("cd");
            checkScriptLines(checkResults, lines, cdDirCheck, LOGIC_NEED_CD_DIR_CHECK);
            watch.stop();
        } finally {
            log.debug("watch={}", watch);
        }
        return checkResults;
    }

}
