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

package com.tencent.bk.job.manage.api.web.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.job.manage.api.web.WebCustomSettingsResource;
import com.tencent.bk.job.manage.common.constants.ScriptTemplateVariableEnum;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.model.dto.customsetting.ScriptTemplateDTO;
import com.tencent.bk.job.manage.model.dto.customsetting.ScriptTemplateVariableRenderDTO;
import com.tencent.bk.job.manage.model.web.request.customsetting.ScriptTemplateCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.request.customsetting.ScriptTemplateRenderReq;
import com.tencent.bk.job.manage.model.web.vo.customsetting.ScriptTemplateVO;
import com.tencent.bk.job.manage.model.web.vo.customsetting.ScriptTemplateVariableVO;
import com.tencent.bk.job.manage.service.CustomScriptTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class WebCustomSettingsResourceImpl implements WebCustomSettingsResource {
    private final CustomScriptTemplateService customScriptTemplateService;
    private final MessageI18nService i18nService;
    private final AppScopeMappingService appScopeMappingService;

    @Autowired
    public WebCustomSettingsResourceImpl(CustomScriptTemplateService customScriptTemplateService,
                                         MessageI18nService i18nService,
                                         AppScopeMappingService appScopeMappingService) {
        this.customScriptTemplateService = customScriptTemplateService;
        this.i18nService = i18nService;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    public Response<List<ScriptTemplateVO>> listUserCustomScriptTemplate(String username,
                                                                         String scriptLanguages) {
        List<ScriptTemplateDTO> scriptTemplates = getUserCustomScriptTemplate(username, scriptLanguages);

        List<ScriptTemplateVO> scriptTemplateVOS = scriptTemplates.stream()
            .map(this::toScriptTemplateVO).collect(Collectors.toList());
        return Response.buildSuccessResp(scriptTemplateVOS);
    }

    private List<ScriptTemplateDTO> getUserCustomScriptTemplate(String username,
                                                                String scriptLanguages) {
        List<ScriptTemplateDTO> scriptTemplates = customScriptTemplateService.listCustomScriptTemplate(username);
        if (CollectionUtils.isEmpty(scriptTemplates)) {
            return Collections.emptyList();
        }

        if (StringUtils.isNotEmpty(scriptLanguages)) {
            Set<Integer> languages = new HashSet<>();
            for (String language : scriptLanguages.split(",")) {
                languages.add(Integer.parseInt(language));
            }
            scriptTemplates = scriptTemplates.stream()
                .filter(scriptTemplate -> languages.contains(scriptTemplate.getScriptLanguage()))
                .collect(Collectors.toList());
        }
        return scriptTemplates;
    }

    private ScriptTemplateVO toScriptTemplateVO(ScriptTemplateDTO scriptTemplate) {
        String scriptContent = scriptTemplate.getScriptContent();
        if (StringUtils.isNotEmpty(scriptContent)) {
            scriptContent = Base64Util.encodeContentToStr(scriptContent);
        }
        return new ScriptTemplateVO(scriptTemplate.getScriptLanguage(), scriptContent);
    }

    @Override
    public Response<List<ScriptTemplateVO>> listRenderedUserCustomScriptTemplate(String username,
                                                                                 String scriptLanguages,
                                                                                 String scopeType,
                                                                                 String scopeId) {
        AppResourceScope appResourceScope = appScopeMappingService.getAppResourceScope(null, scopeType, scopeId);
        List<ScriptTemplateDTO> scriptTemplates = getUserCustomScriptTemplate(username, scriptLanguages);
        // 业务ID内置变量设计上需要修改，暂时先使用appId
        scriptTemplates.forEach(scriptTemplate -> customScriptTemplateService.renderScriptTemplate(
            new ScriptTemplateVariableRenderDTO(appResourceScope.getAppId(), username), scriptTemplate));
        List<ScriptTemplateVO> scriptTemplateVOS = scriptTemplates.stream().map(this::toScriptTemplateVO)
            .collect(Collectors.toList());
        return Response.buildSuccessResp(scriptTemplateVOS);
    }

    @Override
    public Response saveScriptTemplate(String username, ScriptTemplateCreateUpdateReq req) {
        ValidateResult validateResult = checkScriptTemplateCreateUpdateReq(req);
        if (!validateResult.isPass()) {
            return Response.buildCommonFailResp(validateResult.getErrorCode(), validateResult.getErrorParams());
        }
        String scriptContent = req.getScriptContent();
        if (StringUtils.isNotEmpty(scriptContent)) {
            scriptContent = Base64Util.decodeContentToStr(scriptContent);
        }
        ScriptTemplateDTO scriptTemplate = new ScriptTemplateDTO(req.getScriptLanguage(), scriptContent);
        customScriptTemplateService.saveScriptTemplate(username, scriptTemplate);
        return Response.buildSuccessResp(null);
    }

    private ValidateResult checkScriptTemplateCreateUpdateReq(ScriptTemplateCreateUpdateReq req) {
        if (req.getScriptLanguage() == null || ScriptTypeEnum.valueOf(req.getScriptLanguage()) == null) {
            return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM);
        }
        if (req.getScriptContent() == null) {
            req.setScriptContent("");
        }
        return ValidateResult.pass();
    }

    @Override
    public Response<ScriptTemplateVO> renderScriptTemplate(String username, ScriptTemplateRenderReq req) {
        String scriptContent = req.getScriptContent();
        if (StringUtils.isNotEmpty(scriptContent)) {
            scriptContent = Base64Util.decodeContentToStr(scriptContent);
        }

        ScriptTemplateDTO scriptTemplate = new ScriptTemplateDTO(req.getScriptLanguage(), scriptContent);

        Long appId = null;
        if (StringUtils.isNotBlank(req.getScopeType()) && StringUtils.isNotBlank(req.getScopeId())) {
            appId = appScopeMappingService.getAppIdByScope(req.getScopeType(), req.getScopeId());
        }
        ScriptTemplateVariableRenderDTO variableRender = new ScriptTemplateVariableRenderDTO(appId, username);
        if (appId == null) {
            variableRender.addDefaultValue(ScriptTemplateVariableEnum.BIZ_ID.getName(),
                ScriptTemplateVariableEnum.BIZ_ID.getDemo());
            variableRender.addDefaultValue(ScriptTemplateVariableEnum.BIZ_NAME.getName(),
                ScriptTemplateVariableEnum.BIZ_NAME.getDemo());
        }
        customScriptTemplateService.renderScriptTemplate(variableRender, scriptTemplate);
        return Response.buildSuccessResp(toScriptTemplateVO(scriptTemplate));
    }

    @Override
    public Response<List<ScriptTemplateVariableVO>> listScriptTemplateVariables(String username) {
        List<ScriptTemplateVariableVO> variableVOS = new ArrayList<>();
        for (ScriptTemplateVariableEnum variable : ScriptTemplateVariableEnum.values()) {
            ScriptTemplateVariableVO variableVO = new ScriptTemplateVariableVO();
            variableVO.setName(variable.getName());
            variableVO.setDescription(i18nService.getI18n(variable.getDescI18nKey()));
            variableVO.setDemo(variable.getDemo());
            variableVOS.add(variableVO);
        }
        return Response.buildSuccessResp(variableVOS);
    }
}
