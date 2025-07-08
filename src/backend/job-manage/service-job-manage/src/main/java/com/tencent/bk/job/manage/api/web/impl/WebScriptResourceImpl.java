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

package com.tencent.bk.job.manage.api.web.impl;

import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.audit.annotations.AuditRequestBody;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.JobResourceTypeEnum;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.job.common.util.file.CharsetDetectHelper;
import com.tencent.bk.job.common.util.file.EncodingUtils;
import com.tencent.bk.job.manage.api.common.ScriptDTOBuilder;
import com.tencent.bk.job.manage.api.common.constants.JobResourceStatusEnum;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.api.web.WebScriptResource;
import com.tencent.bk.job.manage.auth.ScriptAuthService;
import com.tencent.bk.job.manage.model.dto.ScriptCheckResultItemDTO;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.dto.ScriptSyncTemplateStepDTO;
import com.tencent.bk.job.manage.model.dto.SyncScriptResultDTO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.model.dto.TemplateStepIDDTO;
import com.tencent.bk.job.manage.model.dto.converter.ScriptConverter;
import com.tencent.bk.job.manage.model.query.ScriptQuery;
import com.tencent.bk.job.manage.model.web.request.ScriptCheckReq;
import com.tencent.bk.job.manage.model.web.request.ScriptCreateReq;
import com.tencent.bk.job.manage.model.web.request.ScriptInfoUpdateReq;
import com.tencent.bk.job.manage.model.web.request.ScriptSyncReq;
import com.tencent.bk.job.manage.model.web.request.ScriptTagBatchPatchReq;
import com.tencent.bk.job.manage.model.web.request.ScriptVersionCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.BasicScriptVO;
import com.tencent.bk.job.manage.model.web.vo.ScriptCheckResultItemVO;
import com.tencent.bk.job.manage.model.web.vo.ScriptVO;
import com.tencent.bk.job.manage.model.web.vo.TagCountVO;
import com.tencent.bk.job.manage.model.web.vo.script.ScriptCiteCountVO;
import com.tencent.bk.job.manage.model.web.vo.script.ScriptCiteInfoVO;
import com.tencent.bk.job.manage.model.web.vo.script.ScriptRelatedTemplateStepVO;
import com.tencent.bk.job.manage.model.web.vo.script.ScriptSyncResultVO;
import com.tencent.bk.job.manage.service.ScriptCheckService;
import com.tencent.bk.job.manage.service.ScriptManager;
import com.tencent.bk.job.manage.service.ScriptService;
import com.tencent.bk.job.manage.service.TagService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 业务脚本Resource
 */
@RestController
@Slf4j
public class WebScriptResourceImpl extends BaseWebScriptResource implements WebScriptResource {

    private final ScriptService scriptService;

    private final ScriptCheckService scriptCheckService;

    private final ScriptAuthService scriptAuthService;

    @Autowired
    public WebScriptResourceImpl(ScriptService scriptService,
                                 MessageI18nService i18nService,
                                 ScriptCheckService scriptCheckService,
                                 ScriptDTOBuilder scriptDTOBuilder,
                                 ScriptAuthService scriptAuthService,
                                 TagService tagService,
                                 ScriptManager scriptManager) {
        super(i18nService, scriptDTOBuilder, tagService, scriptManager);
        this.scriptService = scriptService;
        this.scriptCheckService = scriptCheckService;
        this.scriptAuthService = scriptAuthService;
    }

    @Override
    @AuditEntry(actionId = ActionId.VIEW_SCRIPT)
    public Response<ScriptVO> getScriptVersionDetail(String username,
                                                     AppResourceScope appResourceScope,
                                                     String scopeType,
                                                     String scopeId,
                                                     Long scriptVersionId) {
        ScriptDTO script = scriptService.getScriptVersion(username, appResourceScope.getAppId(), scriptVersionId);
        ScriptVO scriptVO = ScriptConverter.convertToScriptVO(script);
        return Response.buildSuccessResp(scriptVO);
    }

    @Override
    @AuditEntry(actionId = ActionId.VIEW_SCRIPT)
    public Response<ScriptVO> getScript(String username,
                                        AppResourceScope appResourceScope,
                                        String scopeType,
                                        String scopeId,
                                        String scriptId) {
        ScriptDTO script = scriptService.getScript(username, appResourceScope.getAppId(), scriptId);
        ScriptVO scriptVO = ScriptConverter.convertToScriptVO(script);

        List<ScriptDTO> scriptVersions = scriptService.listScriptVersion(appResourceScope.getAppId(), scriptId);
        if (CollectionUtils.isNotEmpty(scriptVersions)) {
            List<ScriptVO> scriptVersionVOS = scriptVersions.stream().map(ScriptConverter::convertToScriptVO)
                .collect(Collectors.toList());
            scriptVO.setScriptVersions(scriptVersionVOS);
        }

        return Response.buildSuccessResp(scriptVO);
    }

    @Override
    @AuditEntry(actionId = ActionId.VIEW_SCRIPT)
    public Response<BasicScriptVO> getScriptBasicInfo(String username,
                                                      AppResourceScope appResourceScope,
                                                      String scopeType,
                                                      String scopeId,
                                                      String scriptId) {
        ScriptDTO script = scriptService.getScript(username, appResourceScope.getAppId(), scriptId);
        BasicScriptVO scriptVO = ScriptConverter.convertToBasicScriptVO(script);
        return Response.buildSuccessResp(scriptVO);
    }

    @Override
    @AuditEntry(actionId = ActionId.VIEW_SCRIPT)
    public Response<ScriptVO> getOnlineScriptVersionByScriptId(String username,
                                                               AppResourceScope appResourceScope,
                                                               String scopeType,
                                                               String scopeId,
                                                               String scriptId) {
        ScriptDTO onlineScriptVersion = scriptService.getOnlineScriptVersionByScriptId(username,
            appResourceScope.getAppId(), scriptId);
        if (onlineScriptVersion == null) {
            return Response.buildSuccessResp(null);
        }

        ScriptVO onlineScriptVO = ScriptConverter.convertToScriptVO(onlineScriptVersion);
        return Response.buildSuccessResp(onlineScriptVO);
    }

    @Override
    public Response<PageData<ScriptVO>> listPageScript(String username,
                                                       AppResourceScope appResourceScope,
                                                       String scopeType,
                                                       String scopeId,
                                                       String name,
                                                       Integer type,
                                                       String tags,
                                                       Long panelTag,
                                                       Integer panelType,
                                                       String creator,
                                                       String lastModifyUser,
                                                       String scriptId,
                                                       String content,
                                                       Integer start,
                                                       Integer pageSize,
                                                       String orderField,
                                                       Integer order) {
        ScriptQuery scriptQuery = buildListPageScriptQuery(appResourceScope, name, type, tags, panelTag, panelType,
            creator, lastModifyUser, scriptId, content, start, pageSize, orderField, order);

        PageData<ScriptDTO> pageData = scriptService.listPageScript(scriptQuery);
        PageData<ScriptVO> resultPageData = pageVOs(pageData, start, pageSize);

        //设置脚本引用信息
        setScriptCiteCount(resultPageData.getData());

        // 设置脚本的最新版本
        setOnlineScriptVersionInfo(resultPageData.getData());

        // 设置权限
        processPermissionForList(username, appResourceScope, resultPageData);
        resultPageData.setExistAny(scriptService.isExistAnyAppScript(appResourceScope.getAppId()));

        return Response.buildSuccessResp(resultPageData);
    }

    private void processPermissionForList(String username, AppResourceScope appResourceScope,
                                          PageData<ScriptVO> resultPageData) {
        resultPageData.setCanCreate(
            scriptAuthService.authCreateScript(username, appResourceScope).isPass());

        List<String> scriptIdList = new ArrayList<>();
        resultPageData.getData().forEach(script -> scriptIdList.add(script.getId()));

        List<String> allowedManageScriptIdList =
            scriptAuthService.batchAuthManageScript(username, appResourceScope, scriptIdList);
        List<String> allowedViewScriptIdList =
            scriptAuthService.batchAuthViewScript(username, appResourceScope, scriptIdList);
        resultPageData.getData()
            .forEach(script -> script.setCanManage(allowedManageScriptIdList.contains(script.getId())));
        resultPageData.getData().forEach(script -> {
            boolean canView = allowedViewScriptIdList.contains(script.getId());
            script.setCanView(canView);
            if (!canView) {
                script.setContent("******");
            }
        });
    }

    @Override
    @AuditEntry(actionId = ActionId.MANAGE_SCRIPT)
    public Response<ScriptVO> updateScriptInfo(String username,
                                               AppResourceScope appResourceScope,
                                               String scopeType,
                                               String scopeId,
                                               String scriptId,
                                               @AuditRequestBody ScriptInfoUpdateReq request) {
        long appId = appResourceScope.getAppId();
        String updateField = request.getUpdateField();
        boolean isUpdateDesc = "scriptDesc".equals(updateField);
        boolean isUpdateName = "scriptName".equals(updateField);
        boolean isUpdateTags = "scriptTags".equals(updateField);

        if (StringUtils.isBlank(updateField) || !(isUpdateDesc || isUpdateName || isUpdateTags)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }

        ScriptDTO updateScript;
        if (isUpdateDesc) {
            updateScript = scriptService.updateScriptDesc(appId, username, scriptId,
                request.getScriptDesc());
        } else if (isUpdateName) {
            updateScript = updateScriptName(username, appId, scriptId, request);
        } else {
            updateScript = updateScriptTags(username, appId, scriptId, request);
        }
        return Response.buildSuccessResp(ScriptConverter.convertToScriptVO(updateScript));
    }

    private ScriptDTO updateScriptName(String username, Long appId, String scriptId,
                                       ScriptInfoUpdateReq scriptInfoUpdateReq) {
        scriptInfoUpdateReq.validateScriptName();
        return scriptService.updateScriptName(appId, username, scriptId, scriptInfoUpdateReq.getScriptName());
    }

    private ScriptDTO updateScriptTags(String username, Long appId, String scriptId,
                                       ScriptInfoUpdateReq scriptInfoUpdateReq) {
        List<TagDTO> tags = extractTags(scriptInfoUpdateReq);
        return scriptService.updateScriptTags(appId, username, scriptId, tags);
    }

    @Override
    public Response<List<ScriptVO>> listScriptBasicInfo(String username,
                                                        AppResourceScope appResourceScope,
                                                        String scopeType,
                                                        String scopeId,
                                                        List<String> scriptIds) {
        Long appId = appResourceScope.getAppId();
        ScriptQuery scriptQuery = new ScriptQuery();
        scriptQuery.setAppId(appId);
        scriptQuery.setIds(scriptIds);
        scriptQuery.setPublicScript(false);
        List<ScriptDTO> scripts = scriptService.listScripts(scriptQuery);
        if (CollectionUtils.isNotEmpty(scripts)) {
            scripts = scripts.stream().filter(script -> script.getAppId().equals(appId))
                .collect(Collectors.toList());
        }

        List<ScriptVO> scriptVOS = scripts.stream().map(ScriptConverter::convertToScriptVO)
            .collect(Collectors.toList());
        return Response.buildSuccessResp(scriptVOS);
    }

    @Override
    public Response<List<ScriptVO>> listScriptVersion(String username,
                                                      AppResourceScope appResourceScope,
                                                      String scopeType,
                                                      String scopeId,
                                                      String scriptId) {
        long appId = appResourceScope.getAppId();
        ScriptDTO script = scriptService.getScriptByScriptId(scriptId);
        if (script == null) {
            throw new NotFoundException(ErrorCode.SCRIPT_NOT_EXIST);
        }

        // 鉴权
        AuthResult viewAuthResult = scriptAuthService.authViewScript(username, appResourceScope, scriptId, null);
        AuthResult manageAuthResult = scriptAuthService.authManageScript(username, appResourceScope, scriptId, null);

        List<ScriptDTO> scripts = scriptService.listScriptVersion(appId, scriptId);
        List<ScriptVO> resultVOS = new ArrayList<>();
        if (scripts != null && !scripts.isEmpty()) {
            for (ScriptDTO scriptDTO : scripts) {
                ScriptVO scriptVO = ScriptConverter.convertToScriptVO(scriptDTO);
                scriptVO.setCanView(viewAuthResult.isPass());
                scriptVO.setCanManage(manageAuthResult.isPass());
                // 克隆需要管理权限
                scriptVO.setCanClone(manageAuthResult.isPass());

                // 是否支持同步操作
                if (scriptDTO.getStatus().equals(JobResourceStatusEnum.ONLINE.getValue())) {
                    List<ScriptSyncTemplateStepDTO> syncSteps = getSyncTemplateSteps(appId, scriptId,
                        scriptDTO.getScriptVersionId());
                    scriptVO.setSyncEnabled(!syncSteps.isEmpty());
                } else {
                    scriptVO.setSyncEnabled(false);
                }
                resultVOS.add(scriptVO);

            }
        }

        // 统计被引用次数
        setScriptCiteCount(resultVOS);

        return Response.buildSuccessResp(resultVOS);
    }

    @Override
    @AuditEntry(actionId = ActionId.CREATE_SCRIPT)
    public Response<ScriptVO> saveScript(String username,
                                         AppResourceScope appResourceScope,
                                         String scopeType,
                                         String scopeId,
                                         @AuditRequestBody @Validated ScriptCreateReq request) {

        ScriptDTO script = buildCreateScriptDTO(request, appResourceScope, username);
        ScriptDTO savedScript = scriptService.createScript(username, script);

        ScriptVO scriptVO = ScriptConverter.convertToScriptVO(savedScript);
        return Response.buildSuccessResp(scriptVO);
    }


    private ScriptDTO buildCreateScriptDTO(ScriptCreateReq request,
                                           AppResourceScope appResourceScope,
                                           String username) {
        ScriptDTO script = scriptDTOBuilder.buildFromScriptCreateReq(request);
        script.setAppId(appResourceScope.getAppId());
        script.setPublicScript(false);
        script.setCreator(username);
        script.setLastModifyUser(username);
        return script;
    }

    @Override
    @AuditEntry(actionId = ActionId.MANAGE_SCRIPT)
    public Response<ScriptVO> saveScriptVersion(String username,
                                                AppResourceScope appResourceScope,
                                                String scopeType,
                                                String scopeId,
                                                String scriptId,
                                                @AuditRequestBody ScriptVersionCreateUpdateReq request) {

        ScriptDTO script = buildCreateOrUpdateScriptVersion(true, request, scriptId, null,
            appResourceScope, username);
        ScriptDTO savedScript = scriptService.createScriptVersion(username, script);

        ScriptVO scriptVO = ScriptConverter.convertToScriptVO(savedScript);
        return Response.buildSuccessResp(scriptVO);
    }

    private ScriptDTO buildCreateOrUpdateScriptVersion(boolean isCreate,
                                                       ScriptVersionCreateUpdateReq request,
                                                       String scriptId,
                                                       Long scriptVersionId,
                                                       AppResourceScope appResourceScope,
                                                       String username) {
        ScriptDTO script = scriptDTOBuilder.buildFromScriptVersionCreateUpdateReq(request);
        script.setId(scriptId);
        script.setAppId(appResourceScope.getAppId());
        script.setPublicScript(false);
        script.setLastModifyUser(username);
        if (isCreate) {
            script.setCreator(username);
        } else {
            script.setScriptVersionId(scriptVersionId);
        }
        return script;
    }

    @Override
    @AuditEntry(actionId = ActionId.MANAGE_SCRIPT)
    public Response<ScriptVO> updateScriptVersion(String username,
                                                  AppResourceScope appResourceScope,
                                                  String scopeType, String scopeId,
                                                  String scriptId,
                                                  Long scriptVersionId,
                                                  @AuditRequestBody ScriptVersionCreateUpdateReq request) {

        ScriptDTO script = buildCreateOrUpdateScriptVersion(false, request, scriptId, scriptVersionId,
            appResourceScope, username);
        ScriptDTO savedScriptVersion = scriptService.updateScriptVersion(username, script);

        ScriptVO scriptVO = ScriptConverter.convertToScriptVO(savedScriptVersion);
        return Response.buildSuccessResp(scriptVO);
    }

    @Override
    @AuditEntry(actionId = ActionId.MANAGE_SCRIPT)
    public Response publishScriptVersion(String username,
                                         AppResourceScope appResourceScope,
                                         String scopeType,
                                         String scopeId,
                                         String scriptId,
                                         Long scriptVersionId) {
        scriptService.publishScript(appResourceScope.getAppId(), username, scriptId, scriptVersionId);
        return Response.buildSuccessResp(null);
    }

    @Override
    @AuditEntry(actionId = ActionId.MANAGE_SCRIPT)
    public Response disableScriptVersion(String username,
                                         AppResourceScope appResourceScope,
                                         String scopeType,
                                         String scopeId,
                                         String scriptId,
                                         Long scriptVersionId) {
        scriptService.disableScript(appResourceScope.getAppId(), username, scriptId, scriptVersionId);
        return Response.buildSuccessResp(null);
    }

    @Override
    @AuditEntry(actionId = ActionId.MANAGE_SCRIPT)
    public Response deleteScriptByScriptId(String username,
                                           AppResourceScope appResourceScope,
                                           String scopeType,
                                           String scopeId,
                                           String scriptId) {
        scriptService.deleteScript(username, appResourceScope.getAppId(), scriptId);
        return Response.buildSuccessResp(null);
    }

    @Override
    @AuditEntry(actionId = ActionId.MANAGE_SCRIPT)
    public Response deleteScriptByScriptVersionId(String username,
                                                  AppResourceScope appResourceScope,
                                                  String scopeType,
                                                  String scopeId,
                                                  Long scriptVersionId) {
        log.info("Delete scriptVersion[{}], operator={}, scope={}", scriptVersionId, username, appResourceScope);
        long appId = appResourceScope.getAppId();
        ScriptDTO script = scriptService.getScriptVersion(appId, scriptVersionId);

        scriptService.deleteScriptVersion(username, appId, scriptVersionId);
        return Response.buildSuccessResp(null);
    }

    @Override
    public Response<List<String>> listAppScriptNames(String username,
                                                     AppResourceScope appResourceScope,
                                                     String scopeType,
                                                     String scopeId,
                                                     String scriptName) {
        List<String> scriptNames = scriptService.listScriptNames(appResourceScope.getAppId(), scriptName);
        return Response.buildSuccessResp(scriptNames);
    }

    @Override
    public Response<List<BasicScriptVO>> listScriptOnline(String username,
                                                          AppResourceScope appResourceScope,
                                                          String scopeType,
                                                          String scopeId) {
        List<ScriptDTO> scriptList = scriptService.listOnlineScript(username, appResourceScope.getAppId());
        List<BasicScriptVO> scriptVOList = convertToBasicScriptVOList(scriptList);
        processScriptPermission(username, appResourceScope, scriptVOList);
        return Response.buildSuccessResp(scriptVOList);
    }

    private void processScriptPermission(String username, AppResourceScope appResourceScope,
                                         List<BasicScriptVO> scriptList) {
        List<String> scriptIdList = new ArrayList<>();
        scriptList.forEach(script -> scriptIdList.add(script.getId()));
        List<String> allowedManageScriptIdList =
            scriptAuthService.batchAuthManageScript(username, appResourceScope, scriptIdList);
        List<String> allowedViewScriptIdList =
            scriptAuthService.batchAuthViewScript(username, appResourceScope, scriptIdList);
        scriptList
            .forEach(script -> {
                script.setCanManage(allowedManageScriptIdList.contains(script.getId()));
                script.setCanView(allowedViewScriptIdList.contains(script.getId()));
            });
    }

    @Override
    public Response<List<ScriptCheckResultItemVO>> checkScript(String username, ScriptCheckReq scriptCheckReq) {
        if (scriptCheckReq.getScriptType() == null || StringUtils.isBlank(scriptCheckReq.getContent())) {
            log.warn("Check script, request is illegal! req={}", scriptCheckReq);
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }

        String content = new String(Base64.decodeBase64(scriptCheckReq.getContent()), StandardCharsets.UTF_8);
        List<ScriptCheckResultItemDTO> checkResultItems =
            scriptCheckService.check(ScriptTypeEnum.valOf(scriptCheckReq.getScriptType()), content);

        List<ScriptCheckResultItemVO> checkResultItemVOS = new ArrayList<>();
        if (checkResultItems != null) {
            for (ScriptCheckResultItemDTO checkResultItem : checkResultItems) {
                ScriptCheckResultItemVO checkResultVO = new ScriptCheckResultItemVO();
                checkResultVO.setCode(checkResultItem.getCheckItemCode());
                checkResultVO.setLevel(checkResultItem.getLevel().getValue());
                checkResultVO.setAction(checkResultItem.getAction() == null ? null :
                    checkResultItem.getAction().getValue());
                checkResultVO.setLine(checkResultItem.getLine());
                checkResultVO.setLineContent(checkResultItem.getLineContent());
                checkResultVO.setMatchContent(checkResultItem.getMatchContent());

                if (StringUtils.isNotBlank(checkResultItem.getCheckItemCode())) {
                    String desc = i18nService.getI18n(checkResultItem.getCheckItemCode());
                    if (StringUtils.isNotBlank(desc) && !checkResultItem.getCheckItemCode().equals(desc)) {
                        checkResultVO.setDescription(desc);
                    }
                } else {
                    checkResultVO.setDescription(checkResultItem.getDescription());
                }
                checkResultItemVOS.add(checkResultVO);
            }
        }
        return Response.buildSuccessResp(checkResultItemVOS);
    }

    @Override
    public Response<ScriptVO> uploadScript(String username, MultipartFile scriptFile) {
        String uploadFileName = scriptFile.getOriginalFilename();
        if (StringUtils.isBlank(uploadFileName)) {
            log.warn("Script file name is empty!");
            throw new InvalidParamException(ErrorCode.UPLOAD_SCRIPT_FILE_NAME_EMPTY);
        }
        String ext = uploadFileName.substring(uploadFileName.lastIndexOf("."));
        ScriptTypeEnum type = ScriptTypeEnum.getTypeByExt(ext);
        if (type == null) {
            throw new InvalidParamException(ErrorCode.UPLOAD_SCRIPT_EXT_TYPE_ILLEGAL);
        }

        String fileContent;
        try {
            fileContent = fileToString(scriptFile);
            if (StringUtils.isNotEmpty(fileContent) && !EncodingUtils.isMessyCode(fileContent)) {
                ScriptVO script = new ScriptVO();
                script.setContent(Base64Util.encodeContentToStr(fileContent));
                script.setType(type.getValue());
                script.setTypeName(type.getName());
                return Response.buildSuccessResp(script);
            } else {
                throw new FailedPreconditionException(ErrorCode.UPLOAD_SCRIPT_CONTENT_ILLEGAL);
            }
        } catch (Exception e) {
            log.error("Fail to parse script content", e);
            throw new InternalException(ErrorCode.UPLOAD_SCRIPT_CONTENT_ILLEGAL);
        }
    }

    private String fileToString(MultipartFile is) {
        String fileContent;
        try {

            byte[] bytes = is.getBytes();
            String[] charset = (new CharsetDetectHelper()).detectCharset(bytes);
            if (charset.length == 0) {
                return null;
            }
            // Windows 上的 Unicode编码，采用的UCS2-2 Little-Endian
            if (charset[0].startsWith("windows-")) {
                // 如果是 UCS-2 Big-Endian, 则会识别为UTF-16BE,不需要做强转
                charset[0] = "UTF16";
            }
            fileContent = new String(bytes, charset[0]);
        } catch (IOException e) {
            log.error("fileToString:", e);
            return null;
        }
        return fileContent;
    }

    @Override
    public Response<List<ScriptRelatedTemplateStepVO>> listScriptSyncTemplateSteps(String username,
                                                                                   AppResourceScope appResourceScope,
                                                                                   String scopeType,
                                                                                   String scopeId,
                                                                                   String scriptId,
                                                                                   Long scriptVersionId) {
        List<ScriptRelatedTemplateStepVO> stepVOS = listScriptSyncTemplateSteps(appResourceScope.getAppId(),
            scriptId, scriptVersionId);
        return Response.buildSuccessResp(stepVOS);
    }

    @Override
    @AuditEntry(actionId = ActionId.EDIT_JOB_TEMPLATE)
    public Response<List<ScriptSyncResultVO>> syncScripts(String username,
                                                          AppResourceScope appResourceScope,
                                                          String scopeType,
                                                          String scopeId,
                                                          String scriptId,
                                                          Long scriptVersionId,
                                                          ScriptSyncReq scriptSyncReq) {
        long appId = appResourceScope.getAppId();
        List<TemplateStepIDDTO> templateStepIDs = new ArrayList<>(scriptSyncReq.getSteps().size());
        scriptSyncReq.getSteps().forEach(step ->
            templateStepIDs.add(new TemplateStepIDDTO(step.getTemplateId(), step.getStepId())));

        List<SyncScriptResultDTO> syncResults = scriptService.syncScriptToTaskTemplate(username,
            appId, scriptId, scriptVersionId, templateStepIDs);
        List<ScriptSyncResultVO> syncResultVOS = convertToSyncResultVOs(syncResults, appResourceScope);
        return Response.buildSuccessResp(syncResultVOS);
    }


    @Override
    public Response<ScriptCiteInfoVO> getScriptCiteInfo(String username,
                                                        AppResourceScope appResourceScope,
                                                        String scopeType,
                                                        String scopeId,
                                                        String scriptId,
                                                        Long scriptVersionId) {
        ScriptCiteInfoVO scriptCiteInfoVO = getScriptCiteInfoOfAllScript(scriptId, scriptVersionId);
        return Response.buildSuccessResp(scriptCiteInfoVO);
    }

    @Override
    public Response<ScriptCiteCountVO> getScriptCiteCount(String username,
                                                          AppResourceScope appResourceScope,
                                                          String scopeType,
                                                          String scopeId,
                                                          String scriptId,
                                                          Long scriptVersionId) {
        ScriptCiteCountVO scriptCiteCountVO = getScriptCiteCountOfAllScript(scriptId, scriptVersionId);
        return Response.buildSuccessResp(scriptCiteCountVO);
    }

    @Override
    @AuditEntry(actionId = ActionId.MANAGE_SCRIPT)
    public Response<?> batchUpdateScriptTags(String username,
                                             AppResourceScope appResourceScope,
                                             String scopeType,
                                             String scopeId,
                                             ScriptTagBatchPatchReq req) {

        req.validate();
        if (CollectionUtils.isEmpty(req.getAddTagIdList()) && CollectionUtils.isEmpty(req.getDeleteTagIdList())) {
            return Response.buildSuccessResp(true);
        }

        // 鉴权
        List<String> scriptIdList = req.getIdList();
        scriptAuthService.batchAuthResultManageScript(username, appResourceScope, scriptIdList).denyIfNoPermission();

        batchPatchResourceTags(JobResourceTypeEnum.APP_SCRIPT, req.getIdList(), req.getAddTagIdList(),
            req.getDeleteTagIdList());

        return Response.buildSuccessResp(true);
    }

    @Override
    public Response<TagCountVO> getTagScriptCount(String username,
                                                  AppResourceScope appResourceScope,
                                                  String scopeType,
                                                  String scopeId) {
        return Response.buildSuccessResp(scriptService.getTagScriptCount(appResourceScope.getAppId()));
    }
}
