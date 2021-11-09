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

import com.tencent.bk.job.common.model.dto.ApplicationInfoDTO;
import com.tencent.bk.job.common.service.VariableResolver;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.manage.dao.customsetting.CustomScriptTemplateDAO;
import com.tencent.bk.job.manage.model.dto.customsetting.ScriptTemplateDTO;
import com.tencent.bk.job.manage.model.dto.customsetting.ScriptTemplateVariableRenderDTO;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.CustomScriptTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static com.tencent.bk.job.manage.common.constants.ScriptTemplateVariableEnum.BIZ_ID;
import static com.tencent.bk.job.manage.common.constants.ScriptTemplateVariableEnum.BIZ_NAME;
import static com.tencent.bk.job.manage.common.constants.ScriptTemplateVariableEnum.DATETIME;
import static com.tencent.bk.job.manage.common.constants.ScriptTemplateVariableEnum.DISPLAY_NAME;
import static com.tencent.bk.job.manage.common.constants.ScriptTemplateVariableEnum.USERNAME;

@Service
@Slf4j
public class CustomScriptTemplateServiceImpl implements CustomScriptTemplateService {

    private CustomScriptTemplateDAO customScriptTemplateDAO;
    private ApplicationService applicationService;

    @Autowired
    public CustomScriptTemplateServiceImpl(CustomScriptTemplateDAO customScriptTemplateDAO,
                                           ApplicationService applicationService) {
        this.customScriptTemplateDAO = customScriptTemplateDAO;
        this.applicationService = applicationService;
    }

    @Override
    public List<ScriptTemplateDTO> listCustomScriptTemplate(String username) {
        return customScriptTemplateDAO.listCustomScriptTemplate(username);
    }

    @Override
    public void saveScriptTemplate(String username, ScriptTemplateDTO scriptTemplate) {
        customScriptTemplateDAO.saveScriptTemplate(username, scriptTemplate);
    }

    @Override
    public List<ScriptTemplateDTO> listRenderedCustomScriptTemplate(String username, long appId) {
        List<ScriptTemplateDTO> scriptTemplates = listCustomScriptTemplate(username);
        if (CollectionUtils.isNotEmpty(scriptTemplates)) {
            scriptTemplates.forEach(scriptTemplate ->
                renderScriptTemplate(new ScriptTemplateVariableRenderDTO(appId, username), scriptTemplate));
        }
        return scriptTemplates;
    }

    @Override
    public void renderScriptTemplate(ScriptTemplateVariableRenderDTO scriptTemplateVariableRender,
                                     ScriptTemplateDTO scriptTemplate) {
        String scriptContent = scriptTemplate.getScriptContent();
        Set<String> variables = VariableResolver.resolveScriptBuildInVariables(scriptContent);
        if (CollectionUtils.isEmpty(variables)) {
            return;
        }
        for (String variable : variables) {
            String variablePattern = "\\{\\{" + variable + "}}";
            if (variable.equals(BIZ_ID.getName())) {
                if (scriptTemplateVariableRender.getAppId() != null) {
                    scriptContent = scriptContent.replaceAll(variablePattern,
                        String.valueOf(scriptTemplateVariableRender.getAppId()));
                } else {
                    scriptContent = scriptContent.replaceAll(variablePattern, scriptTemplateVariableRender
                        .getDefaultVariablesValues().get(variable));
                }
            } else if (variable.equals(BIZ_NAME.getName())) {
                if (scriptTemplateVariableRender.getAppId() != null) {
                    ApplicationInfoDTO app = applicationService.getAppInfoById(scriptTemplateVariableRender.getAppId());
                    String appName = app == null ? "" : app.getName();
                    scriptContent = scriptContent.replaceAll(variablePattern, appName);
                } else {
                    scriptContent = scriptContent.replaceAll(variablePattern, scriptTemplateVariableRender
                        .getDefaultVariablesValues().get(variable));
                }
            } else if (variable.equals(USERNAME.getName())) {
                scriptContent = scriptContent.replaceAll(variablePattern,
                    scriptTemplateVariableRender.getUsername());
            } else if (variable.equals(DISPLAY_NAME.getName())) {
                scriptContent = scriptContent.replaceAll(variablePattern,
                    scriptTemplateVariableRender.getUsername());
            } else if (variable.equals(DATETIME.getName())) {
                String datetime = DateUtils.formatLocalDateTime(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss");
                scriptContent = scriptContent.replaceAll(variablePattern, datetime);
            }
        }
        scriptTemplate.setScriptContent(scriptContent);
    }
}
