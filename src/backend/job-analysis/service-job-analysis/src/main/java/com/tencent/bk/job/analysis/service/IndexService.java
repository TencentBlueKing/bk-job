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

package com.tencent.bk.job.analysis.service;

import com.tencent.bk.job.analysis.client.GlobalSettingsResourceClient;
import com.tencent.bk.job.analysis.dao.AnalysisTaskInstanceDAO;
import com.tencent.bk.job.analysis.dao.AnalysisTaskStaticInstanceDAO;
import com.tencent.bk.job.analysis.model.inner.AnalysisTaskResultItemLocation;
import com.tencent.bk.job.analysis.model.web.AnalysisResultItemVO;
import com.tencent.bk.job.analysis.model.web.AnalysisResultVO;
import com.tencent.bk.job.analysis.task.analysis.AnalysisTaskScheduler;
import com.tencent.bk.job.analysis.task.analysis.anotation.AnalysisTask;
import com.tencent.bk.job.analysis.task.analysis.task.IAnalysisTask;
import com.tencent.bk.job.analysis.task.analysis.task.impl.DefaultTipsProvider;
import com.tencent.bk.job.analysis.task.analysis.task.pojo.AnalysisTaskResultVO;
import com.tencent.bk.job.common.i18n.locale.LocaleUtils;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class IndexService {

    private final DSLContext dslContext;
    private final AnalysisTaskInstanceDAO analysisTaskInstanceDAO;
    private final AnalysisTaskStaticInstanceDAO analysisTaskStaticInstanceDAO;
    private final GlobalSettingsResourceClient globalSettingsService;
    private final MessageI18nService i18nService;

    @Autowired
    public IndexService(DSLContext dslContext, AnalysisTaskInstanceDAO analysisTaskInstanceDAO,
                        AnalysisTaskStaticInstanceDAO analysisTaskStaticInstanceDAO,
                        GlobalSettingsResourceClient globalSettingsService, MessageI18nService i18nService) {
        this.dslContext = dslContext;
        this.analysisTaskInstanceDAO = analysisTaskInstanceDAO;
        this.analysisTaskStaticInstanceDAO = analysisTaskStaticInstanceDAO;
        this.globalSettingsService = globalSettingsService;
        this.i18nService = i18nService;
    }

    private Map<String, String> getVariablesMap() {
        Map<String, String> variablesMap = new HashMap<>();
        variablesMap.put("BK_DOCS_CENTER", globalSettingsService.getDocCenterBaseUrl().getData());
        return variablesMap;
    }

    private String parseVariables(String rawTemplate) {
        Map<String, String> variablesMap = getVariablesMap();
        String pattern = "(\\$\\{(.*?)\\})";
        return StringUtil.replaceByRegex(rawTemplate, pattern, variablesMap);
    }

    public List<AnalysisResultVO> listAnalysisResult(String username, Long appId, Long limit) {
        String normalLang = LocaleUtils.getNormalLang(JobContextUtil.getUserLang());
        //业务专属分析结果
        List<AnalysisResultVO> resultList = analysisTaskInstanceDAO.listNewestActiveInstance(dslContext,
            appId, limit).stream().map(it -> {
            //根据任务代码找到对应的任务类组装结果
            IAnalysisTask analysisTask = AnalysisTaskScheduler.analysisTaskMap.get(it.getTaskCode());
            String descriptionTemplate;
            String itemTemplate;
            String resultData = it.getResultData();
            if (normalLang.equals(LocaleUtils.LANG_EN) || normalLang.equals(LocaleUtils.LANG_EN_US)) {
                descriptionTemplate = it.getResultDescriptionTemplateEn();
                itemTemplate = it.getResultItemTemplateEn();
                if (StringUtils.isBlank(itemTemplate)) {
                    log.warn("{} i18n resource of {} is null or blank, use default", normalLang,
                        it.getResultItemTemplate());
                    itemTemplate = it.getResultItemTemplate();
                }
            } else {
                descriptionTemplate = it.getResultDescriptionTemplate();
                itemTemplate = it.getResultItemTemplate();
            }
            // ResultData中进行正则匹配替换实现国际化
            List<String> i18nKeys = StringUtil.findOneRegexPatterns(resultData, "\\$\\{(.*?)\\}");
            for (String key : i18nKeys) {
                resultData = resultData.replace("${" + key + "}", i18nService.getI18n(key));
            }
            if (descriptionTemplate == null) {
                log.warn("normalLang={}, taskCode={}, descriptionTemplate is null, plz config one", normalLang,
                    it.getTaskCode());
                descriptionTemplate = "";
            }
            if (itemTemplate == null) {
                log.warn("normalLang={}, taskCode={}, itemTemplate is null, plz config one", normalLang,
                    it.getTaskCode());
                itemTemplate = "";
            }
            descriptionTemplate = parseVariables(descriptionTemplate);
            itemTemplate = parseVariables(itemTemplate);
            AnalysisTaskResultVO taskResultVO = analysisTask.generateResultVO(descriptionTemplate, itemTemplate,
                resultData);
            if (taskResultVO == null) {
                return null;
            } else {
                return new AnalysisResultVO(
                    it.getId(),
                    analysisTask.getTaskCode(),
                    it.getPriority(),
                    taskResultVO.getDescription(),
                    taskResultVO.getContents().stream().map(item -> new AnalysisResultItemVO(item.getType(),
                        item.getLocation(), item.getContent())).collect(Collectors.toList())
                );
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
        // 静态文案类分析结果
        List<AnalysisResultVO> tipsResultList =
            analysisTaskStaticInstanceDAO.listActiveInstance(dslContext, 0L, limit).stream().map(it -> {
                String resultData;
                // 国际化处理
                if (normalLang.equals(LocaleUtils.LANG_EN) || normalLang.equals(LocaleUtils.LANG_EN_US)) {
                    resultData = it.getResultDataEn();
                    if (StringUtils.isBlank(resultData)) {
                        log.warn("{} i18n resource of {} is null or blank, use default", normalLang,
                            it.getResultData());
                        resultData = it.getResultData();
                    }
                } else {
                    resultData = it.getResultData();
                }
                // 解析变量
                resultData = parseVariables(resultData);
                return new AnalysisResultVO(
                    it.getId(),
                    DefaultTipsProvider.class.getAnnotation(AnalysisTask.class).value(),
                    it.getPriority(),
                    resultData,
                    Stream.of(it.getResultData()).map(item -> new AnalysisResultItemVO("-1",
                        new AnalysisTaskResultItemLocation("", "-1"), item)).collect(Collectors.toList())
                );
            }).collect(Collectors.toList());
        resultList.addAll(tipsResultList);
        return resultList;
    }
}
