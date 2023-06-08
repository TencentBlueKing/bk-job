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
import com.tencent.bk.job.common.constant.JobResourceTypeEnum;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.util.ArrayUtil;
import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.job.common.util.check.IlegalCharChecker;
import com.tencent.bk.job.common.util.check.MaxLengthChecker;
import com.tencent.bk.job.common.util.check.NotEmptyChecker;
import com.tencent.bk.job.common.util.check.StringCheckHelper;
import com.tencent.bk.job.common.util.check.TrimChecker;
import com.tencent.bk.job.common.util.check.WhiteCharChecker;
import com.tencent.bk.job.common.util.check.exception.StringCheckException;
import com.tencent.bk.job.common.util.file.CharsetDetectHelper;
import com.tencent.bk.job.common.util.file.EncodingUtils;
import com.tencent.bk.job.manage.api.common.ScriptDTOBuilder;
import com.tencent.bk.job.manage.api.web.WebScriptResource;
import com.tencent.bk.job.manage.auth.NoResourceScopeAuthService;
import com.tencent.bk.job.manage.auth.ScriptAuthService;
import com.tencent.bk.job.manage.common.consts.JobResourceStatusEnum;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.model.dto.ResourceTagDTO;
import com.tencent.bk.job.manage.model.dto.ScriptCheckResultItemDTO;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.dto.ScriptSyncTemplateStepDTO;
import com.tencent.bk.job.manage.model.dto.SyncScriptResultDTO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.model.dto.TemplateStepIDDTO;
import com.tencent.bk.job.manage.model.dto.converter.ScriptConverter;
import com.tencent.bk.job.manage.model.dto.converter.ScriptRelatedTemplateStepConverter;
import com.tencent.bk.job.manage.model.dto.script.ScriptCitedTaskPlanDTO;
import com.tencent.bk.job.manage.model.dto.script.ScriptCitedTaskTemplateDTO;
import com.tencent.bk.job.manage.model.query.ScriptQuery;
import com.tencent.bk.job.manage.model.web.request.ScriptCheckReq;
import com.tencent.bk.job.manage.model.web.request.ScriptCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.request.ScriptInfoUpdateReq;
import com.tencent.bk.job.manage.model.web.request.ScriptSyncReq;
import com.tencent.bk.job.manage.model.web.request.ScriptTagBatchPatchReq;
import com.tencent.bk.job.manage.model.web.vo.BasicScriptVO;
import com.tencent.bk.job.manage.model.web.vo.ScriptCheckResultItemVO;
import com.tencent.bk.job.manage.model.web.vo.ScriptCitedTaskPlanVO;
import com.tencent.bk.job.manage.model.web.vo.ScriptCitedTemplateVO;
import com.tencent.bk.job.manage.model.web.vo.ScriptVO;
import com.tencent.bk.job.manage.model.web.vo.TagCountVO;
import com.tencent.bk.job.manage.model.web.vo.TagVO;
import com.tencent.bk.job.manage.model.web.vo.script.ScriptCiteCountVO;
import com.tencent.bk.job.manage.model.web.vo.script.ScriptCiteInfoVO;
import com.tencent.bk.job.manage.model.web.vo.script.ScriptRelatedTemplateStepVO;
import com.tencent.bk.job.manage.model.web.vo.script.ScriptSyncResultVO;
import com.tencent.bk.job.manage.service.ScriptCheckService;
import com.tencent.bk.job.manage.service.ScriptService;
import com.tencent.bk.job.manage.service.TagService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.tencent.bk.job.common.constant.JobConstants.PUBLIC_APP_ID;

@RestController
@Slf4j
public class WebScriptResourceImpl implements WebScriptResource {

    private final ScriptService scriptService;

    private final MessageI18nService i18nService;

    private final ScriptCheckService scriptCheckService;

    private final ScriptDTOBuilder scriptDTOBuilder;

    private final ScriptAuthService scriptAuthService;

    private final NoResourceScopeAuthService noResourceScopeAuthService;

    private final TagService tagService;

    @Autowired
    public WebScriptResourceImpl(ScriptService scriptService,
                                 MessageI18nService i18nService,
                                 ScriptCheckService scriptCheckService,
                                 ScriptDTOBuilder scriptDTOBuilder,
                                 ScriptAuthService scriptAuthService,
                                 NoResourceScopeAuthService noResourceScopeAuthService,
                                 TagService tagService) {
        this.scriptService = scriptService;
        this.i18nService = i18nService;
        this.scriptCheckService = scriptCheckService;
        this.scriptDTOBuilder = scriptDTOBuilder;
        this.scriptAuthService = scriptAuthService;
        this.noResourceScopeAuthService = noResourceScopeAuthService;
        this.tagService = tagService;
    }

    @Override
    public Response<ScriptVO> getScriptVersionDetail(String username,
                                                     AppResourceScope appResourceScope,
                                                     String scopeType,
                                                     String scopeId,
                                                     Long scriptVersionId) {
        Long appId = appResourceScope == null ? PUBLIC_APP_ID : appResourceScope.getAppId();
        if (scriptVersionId == null || scriptVersionId <= 0) {
            log.warn("Get script version by id, param scriptVersionId is empty");
            throw new InvalidParamException(ErrorCode.MISSING_PARAM);
        }
        ScriptDTO script = scriptService.getScriptVersion(username, appId, scriptVersionId);
        if (script == null) {
            log.warn("Get script version by id, appId={},id={}, the script is not exist", appId,
                scriptVersionId);
            throw new NotFoundException(ErrorCode.SCRIPT_NOT_EXIST, ArrayUtil.toArray(scriptVersionId));
        }

        // 鉴权
        AuthResult authResult = checkScriptViewPermission(username, appResourceScope, script);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        ScriptVO scriptVO = ScriptConverter.convertToScriptVO(script);

        if (!checkPublicScriptVersionViewPermission(username, scriptVO)) {
            return Response.buildSuccessResp(null);
        }

        scriptVO.setTypeName(ScriptTypeEnum.getName(scriptVO.getType()));
        // 给前端的脚本内容需要base64编码
        scriptVO.setContent(Base64Util.encodeContentToStr(script.getContent()));
        return Response.buildSuccessResp(scriptVO);
    }

    private boolean checkPublicScriptVersionViewPermission(String username, ScriptVO script) {
        if (script.getPublicScript() != null && !script.getPublicScript()) {
            return true;
        }

        AuthResult authResult = checkScriptManagePermission(username, null, script.getId());
        if (authResult.isPass()) {
            return true;
        } else {
            // if user does not have public script management permission, only return online public script version list
            return script.getStatus() == JobResourceStatusEnum.ONLINE.getValue();
        }
    }

    private List<ScriptVO> excludeNotOnlinePublicScriptVersion(String username,
                                                               AppResourceScope appResourceScope,
                                                               String scriptId,
                                                               List<ScriptVO> scriptVersions) {
        AuthResult authResult = checkScriptManagePermission(username, appResourceScope, scriptId);
        if (authResult.isPass()) {
            return scriptVersions;
        } else {
            // if user does not have public script management permission, only return online public script version list
            return scriptVersions.stream().filter(scriptVersion ->
                scriptVersion.getStatus() == JobResourceStatusEnum.ONLINE.getValue())
                .collect(Collectors.toList());
        }
    }

    @Override
    public Response<ScriptVO> getScript(String username,
                                        AppResourceScope appResourceScope,
                                        String scopeType,
                                        String scopeId,
                                        String scriptId) {
        Long appId = appResourceScope == null ? PUBLIC_APP_ID : appResourceScope.getAppId();
        ScriptDTO script = scriptService.getScript(username, appId, scriptId);
        if (script == null) {
            throw new NotFoundException(ErrorCode.SCRIPT_NOT_EXIST);
        }

        // 鉴权
        AuthResult authResult = checkScriptViewPermission(username, appResourceScope, script);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        List<ScriptDTO> scriptVersions = scriptService.listScriptVersion(username, appId, scriptId);
        if (scriptVersions == null || scriptVersions.isEmpty()) {
            throw new NotFoundException(ErrorCode.SCRIPT_NOT_EXIST);
        }

        List<ScriptVO> scriptVersionVOS = new ArrayList<>();
        for (ScriptDTO scriptVersion : scriptVersions) {
            ScriptVO scriptVO = ScriptConverter.convertToScriptVO(scriptVersion);
            JobResourceStatusEnum status = JobResourceStatusEnum.getJobResourceStatus(scriptVO.getStatus());
            scriptVO.setStatusDesc(i18nService.getI18n(status != null ? status.getStatusI18nKey() : null));
            scriptVO.setTypeName(ScriptTypeEnum.getName(scriptVO.getType()));
            // 给前端的脚本内容需要base64编码
            scriptVO.setContent(Base64Util.encodeContentToStr(scriptVersion.getContent()));
            scriptVersionVOS.add(scriptVO);
        }
        ScriptVO scriptVO = ScriptConverter.convertToScriptVO(script);
        scriptVO.setScriptVersions(scriptVersionVOS);

        return Response.buildSuccessResp(scriptVO);
    }

    @Override
    public Response<ScriptVO> getScriptBasicInfo(String username,
                                                 AppResourceScope appResourceScope,
                                                 String scopeType,
                                                 String scopeId,
                                                 String scriptId) {
        Long appId = appResourceScope == null ? PUBLIC_APP_ID : appResourceScope.getAppId();
        ScriptDTO script = scriptService.getScript(username, appId, scriptId);
        if (script == null) {
            throw new NotFoundException(ErrorCode.SCRIPT_NOT_EXIST);
        }
        ScriptVO scriptVO = ScriptConverter.convertToScriptVO(script);
        return Response.buildSuccessResp(scriptVO);
    }

    @Override
    public Response<ScriptVO> getOnlineScriptVersionByScriptId(String username,
                                                               AppResourceScope appResourceScope,
                                                               String scopeType,
                                                               String scopeId,
                                                               String scriptId,
                                                               Boolean publicScript) {
        long appId = appResourceScope == null ? PUBLIC_APP_ID : appResourceScope.getAppId();
        ScriptDTO onlineScriptVersion = scriptService.getOnlineScriptVersionByScriptId(username,
            appId, scriptId);
        if (onlineScriptVersion == null) {
            return Response.buildSuccessResp(null);
        }

        // 鉴权
        AuthResult authResult = checkScriptViewPermission(username, appResourceScope, onlineScriptVersion);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        ScriptVO onlineScriptVO = ScriptConverter.convertToScriptVO(onlineScriptVersion);
        return Response.buildSuccessResp(onlineScriptVO);
    }

    @Override
    public Response<PageData<ScriptVO>> listPageScript(String username,
                                                       AppResourceScope appResourceScope,
                                                       String scopeType,
                                                       String scopeId,
                                                       Boolean publicScript,
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
        ScriptQuery scriptQuery = new ScriptQuery();
        if (publicScript != null && publicScript) {
            scriptQuery.setPublicScript(true);
            scriptQuery.setAppId(PUBLIC_APP_ID);
        } else {
            scriptQuery.setPublicScript(false);
            scriptQuery.setAppId(appResourceScope.getAppId());
        }
        scriptQuery.setId(scriptId);
        scriptQuery.setName(name);
        scriptQuery.setType(type);
        if (panelType != null && panelType == 2) {
            scriptQuery.setUntaggedScript(true);
        } else {
            addTagCondition(scriptQuery, tags, panelTag);
        }
        scriptQuery.setContentKeyword(content);

        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setLength(pageSize);
        baseSearchCondition.setStart(start);
        baseSearchCondition.setOrderField(orderField);
        baseSearchCondition.setOrder(order);
        baseSearchCondition.setCreator(creator);
        baseSearchCondition.setLastModifyUser(lastModifyUser);

        PageData<ScriptDTO> pageData = scriptService.listPageScript(scriptQuery, baseSearchCondition);
        List<ScriptVO> resultScripts = new ArrayList<>();
        if (pageData == null) {
            PageData<ScriptVO> resultPageData = new PageData<>();
            resultPageData.setStart(start);
            resultPageData.setPageSize(pageSize);
            resultPageData.setTotal(0L);
            resultPageData.setData(resultScripts);
            return Response.buildSuccessResp(resultPageData);
        }

        for (ScriptDTO scriptDTO : pageData.getData()) {
            ScriptVO scriptVO = ScriptConverter.convertToScriptVO(scriptDTO);
            resultScripts.add(scriptVO);
        }
        for (ScriptVO scriptVO : resultScripts) {
            String resultScriptId = scriptVO.getId();
            Integer taskTemplateCiteCount = scriptService.getScriptTemplateCiteCount(username, scriptQuery.getAppId()
                , resultScriptId, null);
            scriptVO.setRelatedTaskTemplateNum(taskTemplateCiteCount);
            Integer taskPlanCiteCount = scriptService.getScriptTaskPlanCiteCount(username, scriptQuery.getAppId(),
                resultScriptId, null);
            scriptVO.setRelatedTaskPlanNum(taskPlanCiteCount);
        }

        // 脚本类型处理
        for (ScriptVO scriptVO : resultScripts) {
            scriptVO.setTypeName(ScriptTypeEnum.getName(scriptVO.getType()));
        }

        // 设置脚本的最新版本
        setOnlineScriptVersionInfo(resultScripts);

        PageData<ScriptVO> resultPageData = new PageData<>();
        resultPageData.setStart(pageData.getStart());
        resultPageData.setPageSize(pageData.getPageSize());
        resultPageData.setTotal(pageData.getTotal());
        resultPageData.setData(resultScripts);

        processPermissionForList(username, appResourceScope, resultPageData);
        processAnyScriptExistFlag(appResourceScope, publicScript, resultPageData);

        return Response.buildSuccessResp(resultPageData);
    }

    private void addTagCondition(ScriptQuery query, String tags, Long panelTagId) {
        if (StringUtils.isNotBlank(tags)) {
            query.setTagIds(Arrays.stream(tags.split(",")).map(tagIdStr -> {
                try {
                    return Long.parseLong(tagIdStr);
                } catch (NumberFormatException e) {
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList()));
        }
        if (panelTagId != null && panelTagId > 0) {
            // Frontend need additional param to tell where the tag from
            TagDTO tagInfo = new TagDTO();
            tagInfo.setId(panelTagId);
            if (CollectionUtils.isEmpty(query.getTagIds())) {
                query.setTagIds(Collections.singletonList(panelTagId));
            } else {
                query.getTagIds().add(panelTagId);
            }
        }
    }

    private void processAnyScriptExistFlag(AppResourceScope appResourceScope, Boolean publicScript,
                                           PageData<ScriptVO> resultPageData) {
        if (publicScript != null && publicScript) {
            resultPageData.setExistAny(scriptService.isExistAnyPublicScript());
        } else {
            resultPageData.setExistAny(scriptService.isExistAnyAppScript(appResourceScope.getAppId()));
        }
    }

    private void processPermissionForList(String username, AppResourceScope appResourceScope,
                                          PageData<ScriptVO> resultPageData) {
        boolean isQueryPublicScript = (appResourceScope == null);
        if (isQueryPublicScript) {
            resultPageData.setCanCreate(
                noResourceScopeAuthService.authCreatePublicScript(username).isPass()
            );
        } else {
            resultPageData.setCanCreate(
                scriptAuthService.authCreateScript(username, appResourceScope).isPass());
        }

        List<String> scriptIdList = new ArrayList<>();
        resultPageData.getData().forEach(script -> {
            scriptIdList.add(script.getId());
        });
        if (isQueryPublicScript) {
            resultPageData.getData()
                .forEach(script -> script.setCanManage(
                    noResourceScopeAuthService.authManagePublicScript(username, script.getId()).isPass()));
            resultPageData.getData().forEach(script -> script.setCanView(true));
        } else {
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
    }

    private void setOnlineScriptVersionInfo(List<ScriptVO> scripts) {
        if (scripts != null && !scripts.isEmpty()) {
            List<String> scriptIdList = new ArrayList<>();
            for (ScriptVO script : scripts) {
                scriptIdList.add(script.getId());
            }
            Map<String, ScriptDTO> onlineScriptMap = scriptService.batchGetOnlineScriptVersionByScriptIds(scriptIdList);

            for (ScriptVO scriptVO : scripts) {
                ScriptDTO onlineScriptVersion = onlineScriptMap.get(scriptVO.getId());
                if (onlineScriptVersion != null) {
                    scriptVO.setScriptVersionId(onlineScriptVersion.getScriptVersionId());
                    scriptVO.setVersion(onlineScriptVersion.getVersion());
                }
            }
        }
    }

    private void updateScriptName(String username, Long appId, String scriptId,
                                  ScriptInfoUpdateReq scriptInfoUpdateReq) {
        try {
            StringCheckHelper stringCheckHelper = new StringCheckHelper(new TrimChecker(),
                new NotEmptyChecker(), new IlegalCharChecker(), new MaxLengthChecker(60));
            scriptInfoUpdateReq
                .setScriptName(stringCheckHelper.checkAndGetResult(scriptInfoUpdateReq.getScriptName()));
        } catch (StringCheckException e) {
            log.warn("scriptName is invalid:", e);
            throw new InvalidParamException(ErrorCode.SCRIPT_NAME_INVALID);
        }
        scriptService.updateScriptName(appId, username, scriptId, scriptInfoUpdateReq.getScriptName());
    }

    private void updateScriptTags(String username, Long appId, String scriptId,
                                  ScriptInfoUpdateReq scriptInfoUpdateReq) {
        List<TagDTO> tags = new ArrayList<>();
        if (scriptInfoUpdateReq.getScriptTags() != null && !scriptInfoUpdateReq.getScriptTags().isEmpty()) {
            for (TagVO tagVO : scriptInfoUpdateReq.getScriptTags()) {
                TagDTO tagDTO = new TagDTO();
                tagDTO.setId(tagVO.getId());
                tagDTO.setName(tagVO.getName());
                tags.add(tagDTO);
            }
        }
        scriptService.updateScriptTags(appId, username, scriptId, tags);
    }

    @Override
    public Response updateScriptInfo(String username,
                                     AppResourceScope appResourceScope,
                                     String scopeType,
                                     String scopeId,
                                     String scriptId,
                                     ScriptInfoUpdateReq scriptInfoUpdateReq) {
        Long appId = appResourceScope == null ? PUBLIC_APP_ID : appResourceScope.getAppId();
        String updateField = scriptInfoUpdateReq.getUpdateField();
        boolean isUpdateDesc = "scriptDesc".equals(updateField);
        boolean isUpdateName = "scriptName".equals(updateField);
        boolean isUpdateTags = "scriptTags".equals(updateField);

        if (StringUtils.isBlank(updateField) || !(isUpdateDesc || isUpdateName || isUpdateTags)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }

        // 鉴权
        AuthResult authResult = checkScriptManagePermission(username, appResourceScope, scriptId);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        if (isUpdateDesc) {
            scriptService.updateScriptDesc(appId, username, scriptId,
                scriptInfoUpdateReq.getScriptDesc());
        } else if (isUpdateName) {
            updateScriptName(username, appId, scriptId, scriptInfoUpdateReq);
        } else {
            updateScriptTags(username, appId, scriptId, scriptInfoUpdateReq);
        }
        return Response.buildSuccessResp(null);
    }

    @Override
    public Response<List<ScriptVO>> listScriptBasicInfo(String username,
                                                        AppResourceScope appResourceScope,
                                                        String scopeType,
                                                        String scopeId,
                                                        List<String> scriptIds) {
        Long appId = appResourceScope == null ? PUBLIC_APP_ID : appResourceScope.getAppId();
        ScriptQuery scriptQuery = new ScriptQuery();
        scriptQuery.setAppId(appId);
        scriptQuery.setIds(scriptIds);
        scriptQuery.setPublicScript(appResourceScope == null);
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
        Long appId = appResourceScope == null ? PUBLIC_APP_ID : appResourceScope.getAppId();
        // 鉴权
        AuthResult viewAuthResult = checkScriptViewPermission(username, appResourceScope, scriptId);
        if (!viewAuthResult.isPass()) {
            throw new PermissionDeniedException(viewAuthResult);
        }
        AuthResult manageAuthResult = checkScriptManagePermission(username, appResourceScope, scriptId);

        List<ScriptDTO> scripts = scriptService.listScriptVersion(username, appId, scriptId);
        List<ScriptVO> resultVOS = new ArrayList<>();
        if (scripts != null && !scripts.isEmpty()) {

            for (ScriptDTO scriptDTO : scripts) {
                ScriptVO scriptVO = ScriptConverter.convertToScriptVO(scriptDTO);
                JobResourceStatusEnum status = JobResourceStatusEnum.getJobResourceStatus(scriptVO.getStatus());
                scriptVO.setStatusDesc(i18nService.getI18n(status != null ? status.getStatusI18nKey() : null));
                // 给前端的脚本内容需要base64编码
                scriptVO.setContent(Base64Util.encodeContentToStr(scriptDTO.getContent()));
                scriptVO.setTypeName(ScriptTypeEnum.getName(scriptVO.getType()));

                scriptVO.setCanView(viewAuthResult.isPass());
                scriptVO.setCanManage(manageAuthResult.isPass());
                // 克隆需要管理权限
                scriptVO.setCanClone(manageAuthResult.isPass());

                // 统计被引用次数
                Integer taskTemplateCiteCount = scriptService.getScriptTemplateCiteCount(username,
                    appId, scriptDTO.getId(), scriptDTO.getScriptVersionId());
                scriptVO.setRelatedTaskTemplateNum(taskTemplateCiteCount);
                Integer taskPlanCiteCount = scriptService.getScriptTaskPlanCiteCount(username,
                    appId, scriptDTO.getId(), scriptDTO.getScriptVersionId());
                scriptVO.setRelatedTaskPlanNum(taskPlanCiteCount);

                // 是否支持同步操作
                if (scriptDTO.getStatus().equals(JobResourceStatusEnum.ONLINE.getValue())) {
                    List<ScriptSyncTemplateStepDTO> syncSteps = getSyncTemplateSteps(username,
                        appId, scriptId, scriptDTO.getScriptVersionId());
                    scriptVO.setSyncEnabled(!syncSteps.isEmpty());
                } else {
                    scriptVO.setSyncEnabled(false);
                }
                resultVOS.add(scriptVO);

            }
        }

        resultVOS = excludeNotOnlinePublicScriptVersion(username, appResourceScope, scriptId, resultVOS);
        return Response.buildSuccessResp(resultVOS);
    }

    @Override
    public Response<ScriptVO> saveScript(String username,
                                         AppResourceScope appResourceScope,
                                         String scopeType,
                                         String scopeId,
                                         ScriptCreateUpdateReq scriptCreateUpdateReq) {
        Long appId = appResourceScope == null ? PUBLIC_APP_ID : appResourceScope.getAppId();
        log.info("Save script,operator={},appId={},script={}", username, appId, scriptCreateUpdateReq.toString());

        AuthResult authResult = checkSaveScript(username, appResourceScope, scriptCreateUpdateReq);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        try {
            StringCheckHelper scriptNameCheckHelper = new StringCheckHelper(new TrimChecker(), new NotEmptyChecker(),
                new IlegalCharChecker(), new MaxLengthChecker(60));
            scriptCreateUpdateReq.setName(scriptNameCheckHelper.checkAndGetResult(scriptCreateUpdateReq.getName()));
        } catch (StringCheckException e) {
            log.warn("Script name [{}] is invalid", scriptCreateUpdateReq.getName());
            throw new InvalidParamException(ErrorCode.SCRIPT_NAME_INVALID);
        }
        try {
            StringCheckHelper scriptVersionCheckHelper = new StringCheckHelper(new TrimChecker(), new NotEmptyChecker(),
                new WhiteCharChecker("A-Za-z0-9_\\-#@\\."), new MaxLengthChecker(60));
            scriptCreateUpdateReq
                .setVersion(scriptVersionCheckHelper.checkAndGetResult(scriptCreateUpdateReq.getVersion()));
        } catch (StringCheckException e) {
            log.warn("Script version [{}] is invalid", scriptCreateUpdateReq.getVersion());
            throw new InvalidParamException(ErrorCode.SCRIPT_VERSION_ILLEGAL);
        }
        ScriptDTO script = scriptDTOBuilder.buildFromCreateUpdateReq(scriptCreateUpdateReq);
        script.setAppId(appId);
        script.setPublicScript(appResourceScope == null);
        script.setCreator(username);
        script.setLastModifyUser(username);
        ScriptDTO savedScript = scriptService.saveScript(username, appId, script);
        // 只在新建脚本时新建关联权限，编辑/复制并新建版本时不动
        if (StringUtils.isBlank(scriptCreateUpdateReq.getId())) {
            if (script.isPublicScript()) {
                // 公共脚本
                noResourceScopeAuthService.registerPublicScript(savedScript.getId(), script.getName(), username);
            } else {
                // 业务脚本
                scriptAuthService.registerScript(savedScript.getId(), script.getName(), username);
            }
        }
        ScriptVO scriptVO = new ScriptVO();
        scriptVO.setScriptVersionId(savedScript.getScriptVersionId());
        scriptVO.setId(savedScript.getId());
        return Response.buildSuccessResp(scriptVO);
    }

    private AuthResult checkSaveScript(String username, AppResourceScope appResourceScope,
                                       ScriptCreateUpdateReq scriptCreateUpdateReq) {
        Long scriptVersionId = scriptCreateUpdateReq.getScriptVersionId();
        boolean isCreateNew = scriptVersionId == null || scriptVersionId < 0;
        // 创建脚本版本鉴管理权限
        if (!StringUtils.isBlank(scriptCreateUpdateReq.getId())) {
            isCreateNew = false;
        }
        return isCreateNew ? checkScriptCreatePermission(username, appResourceScope)
            : checkScriptManagePermission(username, appResourceScope, scriptCreateUpdateReq.getId());
    }

    @Override
    public Response publishScriptVersion(String username,
                                         AppResourceScope appResourceScope,
                                         String scopeType,
                                         String scopeId,
                                         String scriptId,
                                         Long scriptVersionId) {
        Long appId = appResourceScope == null ? PUBLIC_APP_ID : appResourceScope.getAppId();
        log.info("Publish script version, scope={}, scriptId={}, scriptVersionId={}, username={}", appResourceScope,
            scriptId, scriptVersionId, username);

        AuthResult authResult = checkScriptManagePermission(username, appResourceScope, scriptId);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        scriptService.publishScript(appId, username, scriptId, scriptVersionId);
        return Response.buildSuccessResp(null);
    }

    @Override
    public Response disableScriptVersion(String username,
                                         AppResourceScope appResourceScope,
                                         String scopeType,
                                         String scopeId,
                                         String scriptId,
                                         Long scriptVersionId) {
        log.info("Disable script version, scope={}, scriptId={}, scriptVersionId={}, username={}", appResourceScope,
            scriptId, scriptVersionId, username);
        Long appId = appResourceScope == null ? PUBLIC_APP_ID : appResourceScope.getAppId();

        AuthResult authResult = checkScriptManagePermission(username, appResourceScope, scriptId);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        scriptService.disableScript(appId, username, scriptId, scriptVersionId);
        return Response.buildSuccessResp(null);
    }

    @Override
    public Response deleteScriptByScriptId(String username,
                                           AppResourceScope appResourceScope,
                                           String scopeType,
                                           String scopeId,
                                           String scriptId) {
        log.info("Delete script[{}], operator={}, scope={}", scriptId, username, appResourceScope);
        Long appId = appResourceScope == null ? PUBLIC_APP_ID : appResourceScope.getAppId();

        AuthResult authResult = checkScriptManagePermission(username, appResourceScope, scriptId);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        scriptService.deleteScript(username, appId, scriptId);
        return Response.buildSuccessResp(null);
    }

    @Override
    public Response deleteScriptByScriptVersionId(String username,
                                                  AppResourceScope appResourceScope,
                                                  String scopeType,
                                                  String scopeId,
                                                  Long scriptVersionId) {
        log.info("Delete scriptVersion[{}], operator={}, scope={}", scriptVersionId, username, appResourceScope);
        Long appId = appResourceScope == null ? PUBLIC_APP_ID : appResourceScope.getAppId();
        ScriptDTO script = scriptService.getScriptVersion(username, appId, scriptVersionId);
        if (script == null) {
            return Response.buildCommonFailResp(ErrorCode.SCRIPT_NOT_EXIST);
        }

        AuthResult authResult = checkScriptManagePermission(username, appResourceScope, script.getId());
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        scriptService.deleteScriptVersion(username, appId, scriptVersionId);
        return Response.buildSuccessResp(null);
    }

    @Override
    public Response listAppScriptNames(String username,
                                       AppResourceScope appResourceScope,
                                       String scopeType,
                                       String scopeId,
                                       String scriptName) {
        Long appId = appResourceScope == null ? PUBLIC_APP_ID : appResourceScope.getAppId();
        List<String> scriptNames = scriptService.listScriptNames(appId, scriptName);
        return Response.buildSuccessResp(scriptNames);
    }

    @Override
    public Response<List<BasicScriptVO>> listScriptOnline(String username,
                                                          AppResourceScope appResourceScope,
                                                          String scopeType,
                                                          String scopeId) {
        long queryAppId = appResourceScope == null ? PUBLIC_APP_ID : appResourceScope.getAppId();

        List<ScriptDTO> scriptList = scriptService.listOnlineScriptForApp(username, queryAppId);
        if (scriptList == null || scriptList.isEmpty()) {
            return Response.buildSuccessResp(Collections.emptyList());
        }

        List<BasicScriptVO> scriptVOList = new ArrayList<>();
        for (ScriptDTO script : scriptList) {
            BasicScriptVO basicScriptVO = ScriptConverter.convertToBasicScriptVO(script);
            scriptVOList.add(basicScriptVO);
        }
        processScriptPermission(username, appResourceScope, scriptVOList);
        return Response.buildSuccessResp(scriptVOList);
    }

    private void processScriptPermission(String username, AppResourceScope appResourceScope,
                                         List<BasicScriptVO> scriptList) {
        List<String> scriptIdList = new ArrayList<>();
        scriptList.forEach(script -> scriptIdList.add(script.getId()));
        boolean isPublicScript = appResourceScope == null;
        if (isPublicScript) {
            scriptList.forEach(script -> {
                // TODO:batchAuth
                AuthResult managePermAuthResult = noResourceScopeAuthService.authManagePublicScript(username,
                    script.getId());
                script.setCanManage(managePermAuthResult.isPass());
                script.setCanView(true);
            });
        } else {

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
    }

    @Override
    public Response<List<ScriptCheckResultItemVO>> checkScript(String username, ScriptCheckReq scriptCheckReq) {
        if (scriptCheckReq.getScriptType() == null || StringUtils.isBlank(scriptCheckReq.getContent())) {
            log.warn("Check script, request is illegal! req={}", scriptCheckReq);
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }

        String content = new String(Base64.decodeBase64(scriptCheckReq.getContent()), StandardCharsets.UTF_8);
        List<ScriptCheckResultItemDTO> checkResultItems =
            scriptCheckService.check(ScriptTypeEnum.valueOf(scriptCheckReq.getScriptType()), content);

        List<ScriptCheckResultItemVO> checkResultItemVOS = new ArrayList<>();
        if (checkResultItems != null) {
            for (ScriptCheckResultItemDTO checkResultItem : checkResultItems) {
                ScriptCheckResultItemVO checkResultVO = new ScriptCheckResultItemVO();
                checkResultVO.setCode(checkResultItem.getCheckItemCode());
                checkResultVO.setLevel(checkResultItem.getLevel().getValue());
                checkResultVO.setAction(checkResultItem.getAction() == null ? null : checkResultItem.getAction().getValue());
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

    private AuthResult checkScriptViewPermission(String username, AppResourceScope appResourceScope, String scriptId) {
        boolean isPublicScript = (appResourceScope == null);
        if (isPublicScript) {
            // 公共脚本默认公开，无需查看权限
            return AuthResult.pass();
        }
        return scriptAuthService.authViewScript(username, appResourceScope, scriptId, null);
    }

    private AuthResult checkScriptViewPermission(String username, AppResourceScope appResourceScope, ScriptDTO script) {
        if (script.isPublicScript()) {
            // 公共脚本默认公开，无需查看权限
            return AuthResult.pass();
        }
        return scriptAuthService.authViewScript(username, appResourceScope, script.getId(), null);
    }


    private AuthResult checkScriptManagePermission(String username, AppResourceScope appResourceScope,
                                                   String scriptId) {
        boolean isPublicScript = appResourceScope == null;
        if (isPublicScript) {
            return noResourceScopeAuthService.authManagePublicScript(username, scriptId);
        } else {
            return scriptAuthService.authManageScript(username, appResourceScope, scriptId, null);
        }
    }

    private AuthResult checkScriptCreatePermission(String username, AppResourceScope appResourceScope) {
        boolean isPublicScript = appResourceScope == null;
        if (isPublicScript) {
            return noResourceScopeAuthService.authCreatePublicScript(username);
        } else {
            return scriptAuthService.authCreateScript(username, appResourceScope);
        }
    }

    @Override
    public Response<List<ScriptRelatedTemplateStepVO>> listScriptSyncTemplateSteps(String username,
                                                                                   AppResourceScope appResourceScope,
                                                                                   String scopeType,
                                                                                   String scopeId,
                                                                                   String scriptId,
                                                                                   Long scriptVersionId) {
        Long appId = appResourceScope == null ? PUBLIC_APP_ID : appResourceScope.getAppId();
        List<ScriptSyncTemplateStepDTO> steps = getSyncTemplateSteps(username, appId, scriptId, scriptVersionId);
        if (CollectionUtils.isEmpty(steps)) {
            return Response.buildSuccessResp(Collections.emptyList());
        }

        List<ScriptRelatedTemplateStepVO> stepVOS =
            steps.stream()
                .map(ScriptRelatedTemplateStepConverter::convertToScriptRelatedTemplateStepVO)
                .collect(Collectors.toList());
        stepVOS.forEach(stepVO -> {
            JobResourceStatusEnum scriptStatus = JobResourceStatusEnum.getJobResourceStatus(stepVO.getScriptStatus());
            if (scriptStatus != null) {
                stepVO.setScriptStatusDesc(i18nService.getI18n(scriptStatus.getStatusI18nKey()));
            }
            stepVO.setCanEdit(true);
        });
        return Response.buildSuccessResp(stepVOS);
    }

    private List<ScriptSyncTemplateStepDTO> getSyncTemplateSteps(String username,
                                                                 Long appId,
                                                                 String scriptId,
                                                                 Long scriptVersionId) {
        List<ScriptSyncTemplateStepDTO> steps = scriptService.listScriptSyncTemplateSteps(username, appId, scriptId);
        if (CollectionUtils.isEmpty(steps)) {
            return Collections.emptyList();
        }
        // 过滤掉已经是最新的模板步骤
        steps =
            steps.stream().filter(step ->
                !scriptVersionId.equals(step.getScriptVersionId()))
                .collect(Collectors.toList());
        return steps;
    }


    @Override
    public Response<List<ScriptSyncResultVO>> syncScripts(String username,
                                                          AppResourceScope appResourceScope,
                                                          String scopeType,
                                                          String scopeId,
                                                          String scriptId,
                                                          Long scriptVersionId,
                                                          ScriptSyncReq scriptSyncReq) {
        Long appId = appResourceScope == null ? PUBLIC_APP_ID : appResourceScope.getAppId();
        List<TemplateStepIDDTO> templateStepIDs = new ArrayList<>(scriptSyncReq.getSteps().size());
        scriptSyncReq.getSteps().forEach(step -> {
            templateStepIDs.add(new TemplateStepIDDTO(step.getTemplateId(), step.getStepId()));
        });

        List<SyncScriptResultDTO> syncResults = scriptService.syncScriptToTaskTemplate(username,
            appId, scriptId, scriptVersionId, templateStepIDs);
        List<ScriptSyncResultVO> syncResultVOS = new ArrayList<>(syncResults.size());
        for (SyncScriptResultDTO syncResult : syncResults) {
            ScriptSyncResultVO syncVO = new ScriptSyncResultVO();
            ScriptSyncTemplateStepDTO syncStep = syncResult.getTemplateStep();
            if (syncStep.getAppId() != null && !syncStep.getAppId().equals(PUBLIC_APP_ID)) {
                syncVO.setScopeType(scopeType);
                syncVO.setScopeId(scopeId);
            }
            syncVO.setScriptId(syncStep.getScriptId());
            syncVO.setScriptVersionId(syncStep.getScriptVersionId());
            syncVO.setScriptVersion(syncStep.getScriptVersion());
            syncVO.setScriptName(syncStep.getScriptName());
            syncVO.setScriptStatus(syncStep.getScriptStatus());
            JobResourceStatusEnum scriptStatus = JobResourceStatusEnum.getJobResourceStatus(syncStep.getScriptStatus());
            if (scriptStatus != null) {
                syncVO.setScriptStatusDesc(i18nService.getI18n(scriptStatus.getStatusI18nKey()));
            }
            syncVO.setStepId(syncStep.getStepId());
            syncVO.setTemplateId(syncStep.getTemplateId());
            syncVO.setStepName(syncStep.getStepName());
            syncVO.setTemplateName(syncStep.getTemplateName());
            if (syncResult.isSuccess()) {
                syncVO.setSyncStatus(ScriptSyncResultVO.SYNC_SUCCESS);
            } else {
                syncVO.setSyncStatus(ScriptSyncResultVO.SYNC_FAIL);
                syncVO.setFailMsg(i18nService.getI18n(String.valueOf(syncResult.getErrorCode())));
            }
            syncResultVOS.add(syncVO);
        }
        return Response.buildSuccessResp(syncResultVOS);
    }

    private ScriptCiteCountVO getScriptCiteCountOfAllScript(String username,
                                                            Long appId,
                                                            String scriptId,
                                                            Long scriptVersionId) {
        Integer templateCiteCount = scriptService.getScriptTemplateCiteCount(username, appId, scriptId,
            scriptVersionId);
        Integer taskPlanCiteCount = scriptService.getScriptTaskPlanCiteCount(username, appId, scriptId,
            scriptVersionId);
        return new ScriptCiteCountVO(templateCiteCount, taskPlanCiteCount);
    }

    private ScriptCiteInfoVO getScriptCiteInfoOfAllScript(String username,
                                                          Long appId,
                                                          String scriptId,
                                                          Long scriptVersionId) {
        List<ScriptCitedTaskTemplateDTO> citedTemplateList = scriptService.getScriptCitedTemplates(username, appId,
            scriptId, scriptVersionId);
        if (citedTemplateList == null) {
            citedTemplateList = Collections.emptyList();
        }
        List<ScriptCitedTemplateVO> citedTemplateVOList =
            citedTemplateList.stream().map(ScriptCitedTaskTemplateDTO::toVO).collect(Collectors.toList());
        List<ScriptCitedTaskPlanDTO> citedTaskPlanList = scriptService.getScriptCitedTaskPlans(username, appId,
            scriptId, scriptVersionId);
        if (citedTaskPlanList == null) {
            citedTaskPlanList = Collections.emptyList();
        }
        List<ScriptCitedTaskPlanVO> citedTaskPlanVOList =
            citedTaskPlanList.stream().map(ScriptCitedTaskPlanDTO::toVO).collect(Collectors.toList());
        return new ScriptCiteInfoVO(citedTemplateVOList, citedTaskPlanVOList);
    }

    @Override
    public Response<ScriptCiteInfoVO> getScriptCiteInfo(String username,
                                                        AppResourceScope appResourceScope,
                                                        String scopeType,
                                                        String scopeId,
                                                        String scriptId,
                                                        Long scriptVersionId) {
        Long appId = appResourceScope == null ? PUBLIC_APP_ID : appResourceScope.getAppId();
        ScriptCiteInfoVO scriptCiteInfoVO = getScriptCiteInfoOfAllScript(username, appId,
            scriptId, scriptVersionId);
        return Response.buildSuccessResp(scriptCiteInfoVO);
    }

    @Override
    public Response<ScriptCiteCountVO> getScriptCiteCount(String username,
                                                          AppResourceScope appResourceScope,
                                                          String scopeType,
                                                          String scopeId,
                                                          String scriptId,
                                                          Long scriptVersionId) {
        Long appId = appResourceScope == null ? PUBLIC_APP_ID : appResourceScope.getAppId();
        ScriptCiteCountVO scriptCiteCountVO = getScriptCiteCountOfAllScript(username, appId,
            scriptId, scriptVersionId);
        return Response.buildSuccessResp(scriptCiteCountVO);
    }

    @Override
    public Response<?> batchUpdateScriptTags(String username,
                                             AppResourceScope appResourceScope,
                                             String scopeType,
                                             String scopeId,
                                             ScriptTagBatchPatchReq req) {

        ValidateResult validateResult = checkScriptTagBatchPatchReq(req);
        if (!validateResult.isPass()) {
            return Response.buildValidateFailResp(validateResult);
        }

        boolean isPublicScript = appResourceScope == null;
        List<String> scriptIdList = req.getIdList();
        AuthResult authResult;
        if (isPublicScript) {
            authResult = noResourceScopeAuthService.batchAuthResultManagePublicScript(username, scriptIdList);
        } else {
            authResult = scriptAuthService.batchAuthResultManageScript(username, appResourceScope, scriptIdList);
        }

        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        List<ResourceTagDTO> addResourceTags = null;
        List<ResourceTagDTO> deleteResourceTags = null;
        Integer resourceType = appResourceScope == null ?
            JobResourceTypeEnum.PUBLIC_SCRIPT.getValue() :
            JobResourceTypeEnum.APP_SCRIPT.getValue();
        if (CollectionUtils.isNotEmpty(req.getAddTagIdList())) {
            addResourceTags = tagService.buildResourceTags(resourceType,
                scriptIdList.stream().map(String::valueOf).collect(Collectors.toList()),
                req.getAddTagIdList());
        }
        if (CollectionUtils.isNotEmpty(req.getDeleteTagIdList())) {
            deleteResourceTags = tagService.buildResourceTags(resourceType,
                scriptIdList.stream().map(String::valueOf).collect(Collectors.toList()),
                req.getDeleteTagIdList());
        }
        tagService.batchPatchResourceTags(addResourceTags, deleteResourceTags);

        return Response.buildSuccessResp(true);
    }

    private ValidateResult checkScriptTagBatchPatchReq(ScriptTagBatchPatchReq req) {
        if (CollectionUtils.isEmpty(req.getIdList())) {
            log.warn("ScriptTagBatchUpdateReq->idList is empty");
            return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "idList");
        }

        if (CollectionUtils.isEmpty(req.getAddTagIdList()) && CollectionUtils.isEmpty(req.getDeleteTagIdList())) {
            log.warn("ScriptTagBatchUpdateReq->No script tags changed!");
            return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME,
                "addTagIdList|deleteTagIdList");
        }
        return ValidateResult.pass();
    }

    @Override
    public Response<TagCountVO> getTagScriptCount(String username,
                                                  AppResourceScope appResourceScope,
                                                  String scopeType,
                                                  String scopeId) {
        Long appId = appResourceScope != null ? appResourceScope.getAppId() : PUBLIC_APP_ID;
        return Response.buildSuccessResp(scriptService.getTagScriptCount(appId));
    }
}
