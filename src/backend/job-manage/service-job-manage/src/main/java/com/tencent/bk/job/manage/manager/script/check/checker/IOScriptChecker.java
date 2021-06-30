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

import static com.tencent.bk.job.manage.manager.script.check.consts.ScriptCheckItemCode.*;

/**
 * 脚本对IO操作的高危检查
 */
@Slf4j
public class IOScriptChecker extends DefaultChecker {

    private static final Pattern noParamToFindBase = Pattern.compile("\\s*find.+/\\s*\\|\\s*xargs\\s+\\S+");
    private static final Pattern paramFindBase = Pattern.compile("\\s*find.+/\\s+.+\\|\\s*xargs\\s+\\S+");
    private static final Pattern noParamToFind = Pattern.compile("\\s*find.+/\\S+\\s*\\|\\s*xargs\\s+\\S+");

    public IOScriptChecker(ScriptCheckParam param) {
        super(param);
    }

    @Override
    public ScriptCheckErrorLevelEnum level() {
        return ScriptCheckErrorLevelEnum.ERROR;
    }

    @Override
    public List<ScriptCheckResultItemDTO> call() {
        StopWatch watch = new StopWatch();
        ArrayList<ScriptCheckResultItemDTO> checkResults = Lists.newArrayList();
        String[] lines = param.getLines();
        watch.start("noParamToFindBase");
        checkScriptLines(checkResults, lines, noParamToFindBase, DANGER_FOUND_ALL);
        watch.stop();
        watch.start("paramFindBase");
        checkScriptLines(checkResults, lines, paramFindBase, DANGER_FOUND_BASE);
        watch.stop();
        watch.start("noParamToFind");
        checkScriptLines(checkResults, lines, noParamToFind, DANGER_FOUND_WITH_NO_CONDITION);
        watch.stop();
        log.debug("watch={}", watch);
        return checkResults;
    }

}
