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
import com.tencent.bk.job.manage.model.dto.globalsetting.DangerousRuleDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义高危脚本规则检查器
 */
@Slf4j
public class DangerousRuleScriptChecker extends DefaultChecker {
    private final List<DangerousRuleDTO> dangerousRules;

    public DangerousRuleScriptChecker(ScriptCheckParam param, List<DangerousRuleDTO> dangerousRules) {
        super(param);
        this.dangerousRules = dangerousRules;
    }

    @Override
    public List<ScriptCheckResultItemDTO> call() {
        StopWatch watch = new StopWatch("scriptDangerousRuleCheck");
        ArrayList<ScriptCheckResultItemDTO> resultItems = Lists.newArrayList();
        try {
            for (DangerousRuleDTO dangerousRule : dangerousRules) {
                watch.start(dangerousRule.getId().toString());
                String[] lines = param.getLines();
                checkScriptLines(resultItems, lines, dangerousRule);
                watch.stop();
            }
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
            if (watch.getTotalTimeMillis() > 10) {
                log.info("Check dangerous script is slow, watch={}", watch.prettyPrint());
            }
        }
        return resultItems;
    }


    @Override
    public ScriptCheckErrorLevelEnum level() {
        return ScriptCheckErrorLevelEnum.FATAL;
    }
}
