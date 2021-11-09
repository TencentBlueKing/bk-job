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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.dao.globalsetting.DangerousRuleDAO;
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
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ScriptCheckServiceImpl implements ScriptCheckService {

    private final DSLContext dslContext;
    private final DangerousRuleDAO dangerousRuleDAO;
    private final ExecutorService executor = new ThreadPoolExecutor(
        10, 50, 60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<Runnable>());
    private LoadingCache<Integer, List<DangerousRuleDTO>> dangerousRuleCache = CacheBuilder.newBuilder()
        .maximumSize(100).expireAfterWrite(1, TimeUnit.MINUTES).
            build(new CacheLoader<Integer, List<DangerousRuleDTO>>() {
                      @Override
                      public List<DangerousRuleDTO> load(Integer scriptType) {
                          List<DangerousRuleDTO> dangerousRules =
                              dangerousRuleDAO.listDangerousRulesByScriptType(dslContext, scriptType);
                          if (CollectionUtils.isEmpty(dangerousRules)) {
                              return Collections.emptyList();
                          } else {
                              return dangerousRules.stream()
                                  .filter(DangerousRuleDTO::isEnabled)
                                  .collect(Collectors.toList());
                          }
                      }
                  }
            );

    @Autowired
    public ScriptCheckServiceImpl(DSLContext dslContext, DangerousRuleDAO dangerousRuleDAO) {
        this.dslContext = dslContext;
        this.dangerousRuleDAO = dangerousRuleDAO;
    }

    private List<DangerousRuleDTO> listDangerousRuleFromCache(Integer scriptType) throws ExecutionException {
        return dangerousRuleCache.get(scriptType);
    }

    @Override
    public List<ScriptCheckResultItemDTO> check(ScriptTypeEnum scriptType, String content) {
        List<ScriptCheckResultItemDTO> checkResultList = new ArrayList<>();
        try {
            List<DangerousRuleDTO> dangerousRules = listDangerousRuleFromCache(scriptType.getValue());
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
            // 脚本检查非强制，如果检查过程中抛出异常不应该影响业务的使用
            log.warn("Check script caught exception, return empty check result by default!", e);
            return Collections.emptyList();
        }
        return checkResultList;
    }

    @Override
    public List<ScriptCheckResultItemDTO> checkScriptWithDangerousRule(ScriptTypeEnum scriptType, String content) {
        List<ScriptCheckResultItemDTO> checkResultList = new ArrayList<>();

        try {
            int timeout = 5;
            List<DangerousRuleDTO> dangerousRules = listDangerousRuleFromCache(scriptType.getValue());
            if (CollectionUtils.isEmpty(dangerousRules)) {
                return Collections.emptyList();
            }

            ScriptCheckParam scriptCheckParam = new ScriptCheckParam(scriptType, content);
            Future<List<ScriptCheckResultItemDTO>> dangerousRuleCheckResultItems = executor.submit(
                new DangerousRuleScriptChecker(scriptCheckParam, dangerousRules));
            checkResultList.addAll(dangerousRuleCheckResultItems.get(timeout, TimeUnit.SECONDS));
            checkResultList.sort(Comparator.comparingInt(ScriptCheckResultItemDTO::getLine));
        } catch (Exception e) {
            log.warn("Check script caught exception!", e);
            throw new InternalException(ErrorCode.INTERNAL_ERROR);
        }
        return checkResultList;
    }
}
