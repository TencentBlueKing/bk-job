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

package com.tencent.bk.job.manage.api.esb.impl.v3;

import com.tencent.bk.audit.annotations.ActionAuditRecord;
import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.audit.annotations.AuditInstanceRecord;
import com.tencent.bk.audit.annotations.AuditRequestBody;
import com.tencent.bk.job.common.audit.constants.EventContentConstants;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.job.v3.EsbPageDataV3;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.manage.api.common.ScriptDTOBuilder;
import com.tencent.bk.job.manage.api.common.constants.JobResourceStatusEnum;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.api.esb.v3.EsbScriptV3Resource;
import com.tencent.bk.job.manage.auth.ScriptAuthService;
import com.tencent.bk.job.manage.model.dto.ScriptCheckResultItemDTO;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbCheckScriptV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbCreateScriptV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbCreateScriptVersionV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbDeleteScriptV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbDeleteScriptVersionV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetScriptListV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetScriptVersionDetailV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetScriptVersionListV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbManageScriptVersionV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbUpdateScriptBasicV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbUpdateScriptVersionV3Req;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbCheckScriptV3DTO;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbScriptV3DTO;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbScriptVersionDetailV3DTO;
import com.tencent.bk.job.manage.model.query.ScriptQuery;
import com.tencent.bk.job.manage.service.ScriptCheckService;
import com.tencent.bk.job.manage.service.ScriptService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
public class EsbScriptResourceV3Impl implements EsbScriptV3Resource {
    private final ScriptService scriptService;
    private final ScriptAuthService scriptAuthService;
    private final ScriptDTOBuilder scriptDTOBuilder;
    private final ScriptCheckService scriptCheckService;
    protected final MessageI18nService i18nService;
    private final AppScopeMappingService appScopeMappingService;

    public EsbScriptResourceV3Impl(ScriptService scriptService,
                                   ScriptAuthService scriptAuthService,
                                   ScriptDTOBuilder scriptDTOBuilder,
                                   ScriptCheckService scriptCheckService,
                                   MessageI18nService i18nService,
                                   AppScopeMappingService appScopeMappingService) {
        this.scriptService = scriptService;
        this.scriptAuthService = scriptAuthService;
        this.scriptDTOBuilder = scriptDTOBuilder;
        this.scriptCheckService = scriptCheckService;
        this.i18nService = i18nService;
        this.appScopeMappingService = appScopeMappingService;
    }


    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_get_script_list"})
    public EsbResp<EsbPageDataV3<EsbScriptV3DTO>> getScriptList(String username,
                                                                String appCode,
                                                                Long bizId,
                                                                String scopeType,
                                                                String scopeId,
                                                                String name,
                                                                Integer scriptLanguage,
                                                                Integer start,
                                                                Integer length) {
        EsbGetScriptListV3Req request = new EsbGetScriptListV3Req();
        request.setBizId(bizId);
        request.setScopeType(scopeType);
        request.setScopeId(scopeId);
        request.setName(name);
        request.setScriptLanguage(scriptLanguage);
        request.setStart(start);
        request.setLength(length);
        request.fillAppResourceScope(appScopeMappingService);
        return getScriptListUsingPost(username, appCode, request);
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_get_script_version_list"})
    @AuditEntry(actionId = ActionId.VIEW_SCRIPT)
    public EsbResp<EsbPageDataV3<EsbScriptVersionDetailV3DTO>> getScriptVersionList(String username,
                                                                                    String appCode,
                                                                                    Long bizId,
                                                                                    String scopeType,
                                                                                    String scopeId,
                                                                                    String scriptId,
                                                                                    boolean returnScriptContent,
                                                                                    Integer start,
                                                                                    Integer length) {
        EsbGetScriptVersionListV3Req request = new EsbGetScriptVersionListV3Req();
        request.setBizId(bizId);
        request.setScopeType(scopeType);
        request.setScopeId(scopeId);
        request.setScriptId(scriptId);
        request.setReturnScriptContent(returnScriptContent);
        request.setStart(start);
        request.setLength(length);
        request.fillAppResourceScope(appScopeMappingService);
        return getScriptVersionListUsingPost(username, appCode, request);
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_get_script_version_detail"})
    @AuditEntry(actionId = ActionId.VIEW_SCRIPT)
    public EsbResp<EsbScriptVersionDetailV3DTO> getScriptVersionDetail(String username,
                                                                       String appCode,
                                                                       Long bizId,
                                                                       String scopeType,
                                                                       String scopeId,
                                                                       Long scriptVersionId,
                                                                       String scriptId,
                                                                       String version) {
        EsbGetScriptVersionDetailV3Req request = new EsbGetScriptVersionDetailV3Req();
        request.setBizId(bizId);
        request.setScopeType(scopeType);
        request.setScopeId(scopeId);
        request.setId(scriptVersionId);
        request.setScriptId(scriptId);
        request.setVersion(version);
        request.fillAppResourceScope(appScopeMappingService);
        return getScriptVersionDetailUsingPost(username, appCode, request);
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_get_script_list"})
    public EsbResp<EsbPageDataV3<EsbScriptV3DTO>> getScriptListUsingPost(String username,
                                                                         String appCode,
                                                                         EsbGetScriptListV3Req request) {
        checkEsbGetScriptListV3Req(request);

        ScriptQuery scriptQuery = buildListPageScriptQuery(request);

        PageData<ScriptDTO> pageScripts = scriptService.listPageScript(scriptQuery);
        setOnlineScriptVersionInfo(pageScripts.getData());

        EsbPageDataV3<EsbScriptV3DTO> result = EsbPageDataV3.from(pageScripts, ScriptDTO::toEsbScriptV3DTO);
        return EsbResp.buildSuccessResp(result);
    }

    private ScriptQuery buildListPageScriptQuery(EsbGetScriptListV3Req request) {
        long appId = request.getAppId();
        ScriptQuery scriptQuery = new ScriptQuery();
        scriptQuery.setAppId(appId);
        scriptQuery.setPublicScript(false);
        scriptQuery.setName(request.getName());
        // 如果script_type=0,表示查询所有类型,不需要传查询条件
        if (request.getScriptLanguage() != null && request.getScriptLanguage() > 0) {
            scriptQuery.setType(request.getScriptLanguage());
        }
        scriptQuery.setStatus(JobResourceStatusEnum.ONLINE.getValue());

        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(request.getStart());
        baseSearchCondition.setLength(request.getLength());
        scriptQuery.setBaseSearchCondition(baseSearchCondition);

        return scriptQuery;
    }

    private void checkEsbGetScriptListV3Req(EsbGetScriptListV3Req request) {
        request.adjustPageParam();
        // 如果script_type=0,表示查询所有类型
        if (request.getScriptLanguage() != null
            && request.getScriptLanguage() > 0
            && ScriptTypeEnum.valOf(request.getScriptLanguage()) == null) {
            log.warn("Param [type]:[{}] is illegal!", request.getScriptLanguage());
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "type");
        }
    }

    private void setOnlineScriptVersionInfo(List<ScriptDTO> scripts) {
        if (scripts != null && !scripts.isEmpty()) {
            List<String> scriptIdList = new ArrayList<>();
            for (ScriptDTO script : scripts) {
                scriptIdList.add(script.getId());
            }
            Map<String, ScriptDTO> onlineScriptMap = scriptService.batchGetOnlineScriptVersionByScriptIds(scriptIdList);

            for (ScriptDTO script : scripts) {
                ScriptDTO onlineScriptVersion = onlineScriptMap.get(script.getId());
                if (onlineScriptVersion != null) {
                    script.setScriptVersionId(onlineScriptVersion.getScriptVersionId());
                    script.setVersion(onlineScriptVersion.getVersion());
                }
            }
        }
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_get_script_version_list"})
    @AuditEntry(actionId = ActionId.VIEW_SCRIPT)
    @ActionAuditRecord(
        actionId = ActionId.VIEW_SCRIPT,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.SCRIPT
        ),
        content = EventContentConstants.VIEW_SCRIPT
    )
    public EsbResp<EsbPageDataV3<EsbScriptVersionDetailV3DTO>> getScriptVersionListUsingPost(
        String username,
        String appCode,
        @AuditRequestBody EsbGetScriptVersionListV3Req request) {
        checkEsbGetScriptVersionListV3Req(request);

        scriptAuthService.authViewScript(username, request.getAppResourceScope(), request.getScriptId(),
            null).denyIfNoPermission();

        ScriptQuery scriptQuery = buildListScriptVersionQuery(request);

        PageData<ScriptDTO> pageScriptVersions = scriptService.listPageScriptVersion(scriptQuery);

        EsbPageDataV3<EsbScriptVersionDetailV3DTO> result = EsbPageDataV3.from(pageScriptVersions,
            ScriptDTO::toEsbScriptVersionDetailV3DTO);
        if (request.getReturnScriptContent() == null || !request.getReturnScriptContent()) {
            if (CollectionUtils.isNotEmpty(result.getData())) {
                result.getData().forEach(scriptVersion -> scriptVersion.setContent(null));
            }
        }
        return EsbResp.buildSuccessResp(result);
    }

    private ScriptQuery buildListScriptVersionQuery(EsbGetScriptVersionListV3Req request) {
        long appId = request.getAppId();
        ScriptQuery scriptQuery = new ScriptQuery();
        scriptQuery.setAppId(appId);
        scriptQuery.setPublicScript(false);
        scriptQuery.setId(request.getScriptId());

        BaseSearchCondition baseSearchCondition = BaseSearchCondition.pageCondition(request.getStart(),
            request.getLength());
        scriptQuery.setBaseSearchCondition(baseSearchCondition);

        return scriptQuery;
    }

    private void checkEsbGetScriptVersionListV3Req(EsbGetScriptVersionListV3Req request) {
        if (StringUtils.isBlank(request.getScriptId())) {
            log.warn("Param [script_id] is empty!");
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "script_id");
        }
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_get_script_version_detail"})
    @AuditEntry(actionId = ActionId.VIEW_SCRIPT)
    public EsbResp<EsbScriptVersionDetailV3DTO> getScriptVersionDetailUsingPost(
        String username,
        String appCode,
        @AuditRequestBody EsbGetScriptVersionDetailV3Req request) {
        checkEsbGetScriptVersionDetailV3Req(request);

        long appId = request.getAppId();
        String scriptId = request.getScriptId();
        String version = request.getVersion();
        Long id = request.getId();
        ScriptDTO scriptVersion;
        if (id != null && id > 0) {
            scriptVersion = scriptService.getScriptVersion(username, appId, id);
        } else {
            scriptVersion = scriptService.getByScriptIdAndVersion(username, appId, scriptId, version);
        }

        EsbScriptVersionDetailV3DTO result = null;
        if (scriptVersion != null) {
            result = scriptVersion.toEsbScriptVersionDetailV3DTO();
        }

        return EsbResp.buildSuccessResp(result);
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_create_script"})
    @AuditEntry(actionId = ActionId.CREATE_SCRIPT)
    public EsbResp<EsbScriptVersionDetailV3DTO> createScript(
        String username,
        String appCode,
        @AuditRequestBody EsbCreateScriptV3Req request) {
        AppResourceScope appResourceScope = request.getAppResourceScope();

        ScriptDTO script = scriptDTOBuilder.buildFromEsbCreateReq(request);
        script.setAppId(appResourceScope.getAppId());
        script.setPublicScript(false);
        script.setCreator(username);
        script.setLastModifyUser(username);
        ScriptDTO savedScript = scriptService.createScript(username, script);

        EsbScriptVersionDetailV3DTO result = savedScript.toEsbCreateScriptV3DTO();
        return EsbResp.buildSuccessResp(result);
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_create_script_version"})
    @AuditEntry(actionId = ActionId.CREATE_SCRIPT)
    public EsbResp<EsbScriptVersionDetailV3DTO> createScriptVersion(
        String username,
        String appCode,
        @AuditRequestBody EsbCreateScriptVersionV3Req request) {
        AppResourceScope appResourceScope = request.getAppResourceScope();

        ScriptDTO script = scriptDTOBuilder.buildFromEsbCreateReq(request);
        script.setAppId(appResourceScope.getAppId());
        script.setPublicScript(false);
        script.setCreator(username);
        script.setLastModifyUser(username);
        ScriptDTO savedScript = scriptService.createScriptVersion(username, script);

        EsbScriptVersionDetailV3DTO result = null;
        if (savedScript != null) {
            result = savedScript.toEsbCreateScriptV3DTO();
        }
        return EsbResp.buildSuccessResp(result);
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_delete_script"})
    @AuditEntry(actionId = ActionId.MANAGE_SCRIPT)
    public EsbResp deleteScript(String username,
                                String appCode,
                                @AuditRequestBody EsbDeleteScriptV3Req request) {
        scriptService.deleteScript(username, request.getAppId(), request.getScriptId());
        return EsbResp.buildSuccessResp(null);
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_delete_script_version"})
    @AuditEntry(actionId = ActionId.MANAGE_SCRIPT)
    public EsbResp deleteScriptVersion(String username,
                                       String appCode,
                                       @AuditRequestBody EsbDeleteScriptVersionV3Req request) {
        scriptService.deleteScriptVersion(username, request.getAppResourceScope().getAppId(),
            request.getScriptVersionId());
        return EsbResp.buildSuccessResp(null);
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_disable_script_version"})
    @AuditEntry(actionId = ActionId.MANAGE_SCRIPT)
    public EsbResp<EsbScriptVersionDetailV3DTO> disableScriptVersion(
        String username,
        String appCode,
        @AuditRequestBody EsbManageScriptVersionV3Req request) {
        scriptService.disableScript(request.getAppResourceScope().getAppId(), username,
            request.getScriptId(), request.getScriptVersionId());
        ScriptDTO scriptVersion = scriptService.getScriptVersion(request.getScriptVersionId());
        return EsbResp.buildSuccessResp(scriptVersion.toEsbManageScriptV3DTO());
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_publish_script_version"})
    @AuditEntry(actionId = ActionId.MANAGE_SCRIPT)
    public EsbResp<EsbScriptVersionDetailV3DTO> publishScriptVersion(
        String username,
        String appCode,
        @AuditRequestBody EsbManageScriptVersionV3Req request) {
        scriptService.publishScript(request.getAppResourceScope().getAppId(), username,
            request.getScriptId(), request.getScriptVersionId());
        ScriptDTO scriptVersion = scriptService.getScriptVersion(request.getScriptVersionId());
        return EsbResp.buildSuccessResp(scriptVersion.toEsbManageScriptV3DTO());
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_update_script_basic"})
    @AuditEntry(actionId = ActionId.MANAGE_SCRIPT)
    public EsbResp<EsbScriptV3DTO> updateScriptBasic(
        String username,
        String appCode,
        @AuditRequestBody EsbUpdateScriptBasicV3Req request) {
        String scriptId = request.getScriptId();
        AppResourceScope appResourceScope = request.getAppResourceScope();
        scriptService.updateScriptName(appResourceScope.getAppId(), username, scriptId, request.getName());
        if (StringUtils.isNotEmpty(request.getDescription())) {
            scriptService.updateScriptDesc(appResourceScope.getAppId(), username, scriptId, request.getDescription());
        }

        ScriptDTO scriptDTO = scriptService.getScript(appResourceScope.getAppId(), scriptId);
        EsbScriptV3DTO updateScriptV3DTO = scriptDTO.toEsbScriptV3DTO();
        return EsbResp.buildSuccessResp(updateScriptV3DTO);
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_update_script_version"})
    @AuditEntry(actionId = ActionId.MANAGE_SCRIPT)
    public EsbResp<EsbScriptVersionDetailV3DTO> updateScriptVersion(
        String username,
        String appCode,
        @AuditRequestBody EsbUpdateScriptVersionV3Req request) {
        ScriptDTO scriptVersionDTO = scriptDTOBuilder.buildFromCreateUpdateReq(request);
        scriptVersionDTO.setAppId(request.getAppResourceScope().getAppId());
        scriptVersionDTO.setPublicScript(false);
        scriptVersionDTO.setCreator(username);
        scriptVersionDTO.setLastModifyUser(username);
        scriptService.updateScriptVersion(username, scriptVersionDTO);
        ScriptDTO scriptDTO = scriptService.getScriptVersion(request.getScriptVersionId());
        return EsbResp.buildSuccessResp(scriptDTO.toEsbCreateScriptV3DTO());
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_check_script"})
    public EsbResp<List<EsbCheckScriptV3DTO>> checkScript(EsbCheckScriptV3Req request) {
        String content = new String(Base64.decodeBase64(request.getContent()), StandardCharsets.UTF_8);
        List<ScriptCheckResultItemDTO> checkResultItems =
            scriptCheckService.check(ScriptTypeEnum.valOf(request.getType()), content);
        List<EsbCheckScriptV3DTO> checkScriptDTOS = new ArrayList<>();
        if (checkResultItems != null) {
            for (ScriptCheckResultItemDTO checkResultItem : checkResultItems) {
                EsbCheckScriptV3DTO checkScriptDTO = new EsbCheckScriptV3DTO();
                checkScriptDTO.setLine(checkResultItem.getLine());
                checkScriptDTO.setLineContent(checkResultItem.getLineContent());
                checkScriptDTO.setMatchContent(checkResultItem.getMatchContent());
                checkScriptDTO.setLevel(checkResultItem.getLevel().getValue());
                if (StringUtils.isNotBlank(checkResultItem.getCheckItemCode())) {
                    String desc = i18nService.getI18n(checkResultItem.getCheckItemCode());
                    if (StringUtils.isNotBlank(desc) && !checkResultItem.getCheckItemCode().equals(desc)) {
                        checkScriptDTO.setDescription(desc);
                    }
                } else {
                    checkScriptDTO.setDescription(checkResultItem.getDescription());
                }
                checkScriptDTOS.add(checkScriptDTO);
            }
        }
        return EsbResp.buildSuccessResp(checkScriptDTOS);
    }

    private void checkEsbGetScriptVersionDetailV3Req(EsbGetScriptVersionDetailV3Req request) {
        if (request.getId() != null && request.getId() > 0) {
            // 如果ID合法，那么忽略其他参数
            return;
        }

        if (StringUtils.isBlank(request.getScriptId())) {
            log.warn("Param [script_id] is empty!");
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "script_id");
        }

        if (StringUtils.isBlank(request.getVersion())) {
            log.warn("Param [version] is empty!");
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "version");
        }
    }
}
