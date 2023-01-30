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

package com.tencent.bk.job.manage.service.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.manager.cache.DangerousRuleCache;
import com.tencent.bk.job.manage.manager.script.check.ScriptCheckParam;
import com.tencent.bk.job.manage.manager.script.check.checker.BuildInDangerousScriptChecker;
import com.tencent.bk.job.manage.manager.script.check.checker.DangerousRuleScriptChecker;
import com.tencent.bk.job.manage.manager.script.check.checker.DeviceCrashScriptChecker;
import com.tencent.bk.job.manage.manager.script.check.checker.IOScriptChecker;
import com.tencent.bk.job.manage.manager.script.check.checker.ScriptGrammarChecker;
import com.tencent.bk.job.manage.manager.script.check.checker.ScriptLogicChecker;
import com.tencent.bk.job.manage.model.dto.ScriptCheckResultItemDTO;
import com.tencent.bk.job.manage.model.dto.globalsetting.DangerousRuleDTO;
import com.tencent.bk.job.manage.service.ScriptCheckService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ScriptCheckServiceImpl implements ScriptCheckService {

    private final DangerousRuleCache dangerousRuleCache;
    private final ExecutorService executor = new ThreadPoolExecutor(
        10, 50, 60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>());

    @Autowired
    public ScriptCheckServiceImpl(DangerousRuleCache dangerousRuleCache) {
        this.dangerousRuleCache = dangerousRuleCache;
    }

    @Override
    public List<ScriptCheckResultItemDTO> check(ScriptTypeEnum scriptType, String content) {
        List<ScriptCheckResultItemDTO> checkResultList = new ArrayList<>();
        if (StringUtils.isBlank(content)) {
            return checkResultList;
        }

        try {
            List<DangerousRuleDTO> dangerousRules =
                dangerousRuleCache.listDangerousRuleFromCache(scriptType.getValue());
            int timeout = 5;
            ScriptCheckParam scriptCheckParam = new ScriptCheckParam(scriptType, content);
            Future<List<ScriptCheckResultItemDTO>> dangerousRuleCheckResultItems =
                executor.submit(new DangerousRuleScriptChecker(scriptCheckParam, dangerousRules));
            if (ScriptTypeEnum.SHELL.equals(scriptType)) {
                Future<List<ScriptCheckResultItemDTO>> grammar = executor.submit(new ScriptGrammarChecker(scriptType,
                    content));

                Future<List<ScriptCheckResultItemDTO>> danger =
                    executor.submit(new BuildInDangerousScriptChecker(scriptCheckParam));

                Future<List<ScriptCheckResultItemDTO>> logic =
                    executor.submit(new ScriptLogicChecker(scriptCheckParam));

                Future<List<ScriptCheckResultItemDTO>> io = executor.submit(new IOScriptChecker(scriptCheckParam));

                Future<List<ScriptCheckResultItemDTO>> device =
                    executor.submit(new DeviceCrashScriptChecker(scriptCheckParam));
                checkResultList.addAll(grammar.get(timeout, TimeUnit.SECONDS));
                checkResultList.addAll(logic.get(timeout, TimeUnit.SECONDS));
                checkResultList.addAll(danger.get(timeout, TimeUnit.SECONDS));
                checkResultList.addAll(io.get(timeout, TimeUnit.SECONDS));
                checkResultList.addAll(device.get(timeout, TimeUnit.SECONDS));
            }
            checkResultList.addAll(dangerousRuleCheckResultItems.get(timeout, TimeUnit.SECONDS));
            checkResultList.sort(Comparator.comparingInt(ScriptCheckResultItemDTO::getLine));

        } catch (Exception e) {
            String errorMsg = MessageFormatter.format(
                "Check script caught exception! scriptType: {}, content: {}",
                scriptType, content).getMessage();
            log.error(errorMsg, e);
            throw new InternalException(e, ErrorCode.INTERNAL_ERROR);
        }
        return checkResultList;
    }

    @Override
    public List<ScriptCheckResultItemDTO> checkScriptWithDangerousRule(ScriptTypeEnum scriptType, String content) {
        List<ScriptCheckResultItemDTO> checkResultList = new ArrayList<>();

        if (StringUtils.isBlank(content)) {
            return Collections.emptyList();
        }

        try {
            int timeout = 5;
            ScriptCheckParam scriptCheckParam = new ScriptCheckParam(scriptType, content);
            List<DangerousRuleDTO> dangerousRules =
                dangerousRuleCache.listDangerousRuleFromCache(scriptType.getValue());
            if (CollectionUtils.isEmpty(dangerousRules)) {
                return Collections.emptyList();
            }
            Future<List<ScriptCheckResultItemDTO>> dangerousRuleCheckResultItems = executor.submit(
                new DangerousRuleScriptChecker(scriptCheckParam, dangerousRules));
            checkResultList.addAll(dangerousRuleCheckResultItems.get(timeout, TimeUnit.SECONDS));
            checkResultList.sort(Comparator.comparingInt(ScriptCheckResultItemDTO::getLine));
        } catch (Exception e) {
            String errorMsg = MessageFormatter.format(
                "Check script caught exception! scriptType: {}, content: {}",
                scriptType, content).getMessage();
            log.error(errorMsg, e);
            throw new InternalException(e, ErrorCode.INTERNAL_ERROR);
        }
        return checkResultList;
    }
}
