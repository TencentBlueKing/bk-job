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
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.model.permission.AuthResultVO;
import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.job.common.util.JobContextUtil;
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
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.util.PathBuilder;
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

    private final WebAuthService authService;

    private final TagService tagService;

    @Autowired
    public WebScriptResourceImpl(
        ScriptService scriptService,
        MessageI18nService i18nService,
        ScriptCheckService scriptCheckService,
        ScriptDTOBuilder scriptDTOBuilder,
        WebAuthService webAuthService,
        TagService tagService) {
        this.scriptService = scriptService;
        this.i18nService = i18nService;
        this.scriptCheckService = scriptCheckService;
        this.scriptDTOBuilder = scriptDTOBuilder;
        this.authService = webAuthService;
        this.tagService = tagService;
    }

    @Override
    public ServiceResponse<ScriptVO> getScriptVersionDetail(String username, Long appId, Long scriptVersionId) {
        if (appId == null || appId < 0) {
            log.warn("Get script version by id, param appId is empty");
            return ServiceResponse.buildCommonFailResp(ErrorCode.MISSING_PARAM,
                i18nService.getI18n(String.valueOf(ErrorCode.MISSING_PARAM)));
        }
        if (scriptVersionId == null || scriptVersionId <= 0) {
            log.warn("Get script version by id, param scriptVersionId is empty");
            return ServiceResponse.buildCommonFailResp(ErrorCode.MISSING_PARAM,
                i18nService.getI18n(String.valueOf(ErrorCode.MISSING_PARAM)));
        }
        ScriptDTO script = scriptService.getScriptVersion(username, appId, scriptVersionId);
        if (script == null) {
            log.warn("Get script version by id, appId={},id={}, the script is not exist", appId, scriptVersionId);
            return ServiceResponse.buildCommonFailResp(ErrorCode.SCRIPT_NOT_EXIST,
                i18nService.getI18nWithArgs(String.valueOf(ErrorCode.SCRIPT_NOT_EXIST), scriptVersionId));
        }

        // 鉴权
        AuthResultVO authResultVO = checkScriptViewPermission(username, appId, script);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }

        ScriptVO scriptVO = ScriptConverter.convertToScriptVO(script);

        if (!checkPublicScriptVersionViewPermission(username, scriptVO)) {
            return ServiceResponse.buildSuccessResp(null);
        }

        scriptVO.setTypeName(ScriptTypeEnum.getName(scriptVO.getType()));
        // 给前端的脚本内容需要base64编码
        scriptVO.setContent(Base64Util.encodeContentToStr(script.getContent()));
        return ServiceResponse.buildSuccessResp(scriptVO);
    }

    private boolean checkPublicScriptVersionViewPermission(String username, ScriptVO script) {
        if (script.getPublicScript() != null && !script.getPublicScript()) {
            return true;
        }

        AuthResultVO authResultVO = checkScriptManagePermission(username, script.getAppId(), script.getId());
        if (authResultVO.isPass()) {
            return true;
        } else {
            // if user does not have public script management permission, only return online public script version list
            return script.getStatus() == JobResourceStatusEnum.ONLINE.getValue();
        }
    }

    private List<ScriptVO> excludeNotOnlinePublicScriptVersion(String username,
                                                               Long appId,
                                                               String scriptId,
                                                               List<ScriptVO> scriptVersions) {
        AuthResultVO authResultVO = checkScriptManagePermission(username, appId, scriptId);
        if (authResultVO.isPass()) {
            return scriptVersions;
        } else {
            // if user does not have public script management permission, only return online public script version list
            return scriptVersions.stream().filter(scriptVersion ->
                scriptVersion.getStatus() == JobResourceStatusEnum.ONLINE.getValue())
                .collect(Collectors.toList());
        }
    }

    @Override
    public ServiceResponse<ScriptVO> getScript(String username, Long appId, String scriptId) {
        ScriptDTO script = scriptService.getScript(username, appId, scriptId);
        if (script == null) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.SCRIPT_NOT_EXIST,
                i18nService.getI18n(String.valueOf(ErrorCode.SCRIPT_NOT_EXIST)));
        }

        // 鉴权
        AuthResultVO authResultVO = checkScriptViewPermission(username, appId, script);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }

        List<ScriptDTO> scriptVersions = scriptService.listScriptVersion(username, appId, scriptId);
        if (scriptVersions == null || scriptVersions.isEmpty()) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.SCRIPT_NOT_EXIST,
                i18nService.getI18n(String.valueOf(ErrorCode.SCRIPT_NOT_EXIST)));
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

        return ServiceResponse.buildSuccessResp(scriptVO);
    }

    @Override
    public ServiceResponse<ScriptVO> getScriptBasicInfo(String username, Long appId, String scriptId) {
        ScriptDTO script = scriptService.getScript(username, appId, scriptId);
        if (script == null) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.SCRIPT_NOT_EXIST,
                i18nService.getI18n(String.valueOf(ErrorCode.SCRIPT_NOT_EXIST)));
        }
        ScriptVO scriptVO = ScriptConverter.convertToScriptVO(script);
        return ServiceResponse.buildSuccessResp(scriptVO);
    }

    @Override
    public ServiceResponse<ScriptVO> getOnlineScriptVersionByScriptId(String username, Long appId, String scriptId,
                                                                      Boolean publicScript) {
        long targetAppId = appId;
        if (publicScript != null && publicScript) {
            targetAppId = PUBLIC_APP_ID;
        }
        ScriptDTO onlineScriptVersion = scriptService.getOnlineScriptVersionByScriptId(username, targetAppId, scriptId);
        if (onlineScriptVersion == null) {
            return ServiceResponse.buildSuccessResp(null);
        }

        // 鉴权
        AuthResultVO authResultVO = checkScriptViewPermission(username, appId, onlineScriptVersion);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }

        ScriptVO onlineScriptVO = ScriptConverter.convertToScriptVO(onlineScriptVersion);
        return ServiceResponse.buildSuccessResp(onlineScriptVO);
    }

    @Override
    public ServiceResponse<PageData<ScriptVO>> listPageScript(
        String username,
        Long appId,
        Boolean publicScript,
        String name,
        Integer type,
        String tags,
        Long panelTag,
        Integer panelType,
        String creator,
        String lastModifyUser,
        String scriptId,
        Integer start,
        Integer pageSize,
        String orderField,
        Integer order
    ) {
        JobContextUtil.setAppId(appId);
        ScriptQuery scriptQuery = new ScriptQuery();
        if (publicScript != null && publicScript) {
            scriptQuery.setPublicScript(true);
            scriptQuery.setAppId(PUBLIC_APP_ID);
        } else {
            scriptQuery.setPublicScript(false);
            scriptQuery.setAppId(appId);
        }
        scriptQuery.setId(scriptId);
        scriptQuery.setName(name);
        scriptQuery.setType(type);
        scriptQuery.setPublicScript(publicScript);
        if (panelType != null && panelType == 2) {
            scriptQuery.setUntaggedScript(true);
        } else {
            addTagCondition(scriptQuery, tags, panelTag);
        }

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
            return ServiceResponse.buildSuccessResp(resultPageData);
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

        processPermissionForList(username, appId, resultPageData);
        processAnyScriptExistFlag(appId, publicScript, resultPageData);

        return ServiceResponse.buildSuccessResp(resultPageData);
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

    private void processAnyScriptExistFlag(Long appId, Boolean publicScript, PageData resultPageData) {
        if (publicScript != null && publicScript) {
            resultPageData.setExistAny(scriptService.isExistAnyPublicScript());
        } else {
            resultPageData.setExistAny(scriptService.isExistAnyAppScript(appId));
        }
    }

    private void processPermissionForList(String username, Long appId, PageData<ScriptVO> resultPageData) {
        boolean isQueryPublicScript = (appId == 0);
        if (isQueryPublicScript) {
            resultPageData.setCanCreate(
                authService.auth(false, username, ActionId.CREATE_PUBLIC_SCRIPT).isPass()
            );
        } else {
            resultPageData.setCanCreate(authService.auth(false, username, ActionId.CREATE_SCRIPT,
                ResourceTypeEnum.BUSINESS, appId.toString(), null).isPass());
        }

        List<String> scriptIdList = new ArrayList<>();
        resultPageData.getData().forEach(script -> {
            scriptIdList.add(script.getId());
        });
        if (isQueryPublicScript) {
            resultPageData.getData()
                .forEach(script -> script.setCanManage(authService.auth(false, username,
                    ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE).isPass()));
            resultPageData.getData().forEach(script -> script.setCanView(true));
        } else {
            List<String> allowedManageScriptIdList =
                authService.batchAuth(username, ActionId.MANAGE_SCRIPT, appId, ResourceTypeEnum.SCRIPT, scriptIdList);
            List<String> allowedViewScriptIdList =
                authService.batchAuth(username, ActionId.VIEW_SCRIPT, appId, ResourceTypeEnum.SCRIPT, scriptIdList);
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

    @Override
    public ServiceResponse updateScriptInfo(String username, Long appId, String scriptId,
                                            ScriptInfoUpdateReq scriptInfoUpdateReq) {
        JobContextUtil.setAppId(appId);
        if (StringUtils.isBlank(scriptId) || scriptInfoUpdateReq == null) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM,
                i18nService.getI18n(String.valueOf(ErrorCode.ILLEGAL_PARAM)));
        }
        String updateField = scriptInfoUpdateReq.getUpdateField();
        boolean isUpdateDesc = updateField.equals("scriptDesc");
        boolean isUpdateName = updateField.equals("scriptName");
        boolean isUpdateTags = updateField.equals("scriptTags");

        if (StringUtils.isBlank(updateField) || !(isUpdateDesc || isUpdateName || isUpdateTags)) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM,
                i18nService.getI18n(String.valueOf(ErrorCode.ILLEGAL_PARAM)));
        }

        // 鉴权
        AuthResultVO authResultVO = checkScriptManagePermission(username, appId, scriptId);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }

        try {
            if (isUpdateDesc) {
                scriptService.updateScriptDesc(appId, username, scriptId, scriptInfoUpdateReq.getScriptDesc());
            } else if (isUpdateName) {
                try {
                    StringCheckHelper stringCheckHelper = new StringCheckHelper(new TrimChecker(),
                        new NotEmptyChecker(), new IlegalCharChecker(), new MaxLengthChecker(60));
                    scriptInfoUpdateReq
                        .setScriptName(stringCheckHelper.checkAndGetResult(scriptInfoUpdateReq.getScriptName()));
                } catch (StringCheckException e) {
                    log.warn("scriptName is invalid:", e);
                    return ServiceResponse.buildCommonFailResp(ErrorCode.SCRIPT_NAME_INVALID,
                        i18nService.getI18n(String.valueOf(ErrorCode.SCRIPT_NAME_INVALID)));
                }
                scriptService.updateScriptName(appId, username, scriptId, scriptInfoUpdateReq.getScriptName());
            } else if (isUpdateTags) {
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
        } catch (ServiceException e) {
            log.warn("Update script info fail, scriptId={}, req={}", scriptId, scriptInfoUpdateReq);
            return ServiceResponse.buildCommonFailResp(e.getErrorCode(),
                i18nService.getI18n(String.valueOf(e.getErrorCode())));
        }
        return ServiceResponse.buildSuccessResp(null);
    }

    @Override
    public ServiceResponse<List<ScriptVO>> listScriptVersion(String username, Long appId, String scriptId) {
        JobContextUtil.setAppId(appId);
        // 鉴权
        AuthResultVO viewAuthResultVO = checkScriptViewPermission(username, appId, scriptId);
        if (!viewAuthResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(viewAuthResultVO);
        }
        AuthResultVO manageAuthResultVO = checkScriptManagePermission(username, appId, scriptId);

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

                scriptVO.setCanView(viewAuthResultVO.isPass());
                scriptVO.setCanManage(manageAuthResultVO.isPass());
                // 克隆需要管理权限
                scriptVO.setCanClone(manageAuthResultVO.isPass());

                // 统计被引用次数
                Integer taskTemplateCiteCount = scriptService.getScriptTemplateCiteCount(username, appId,
                    scriptDTO.getId(), scriptDTO.getScriptVersionId());
                scriptVO.setRelatedTaskTemplateNum(taskTemplateCiteCount);
                Integer taskPlanCiteCount = scriptService.getScriptTaskPlanCiteCount(username, appId,
                    scriptDTO.getId(), scriptDTO.getScriptVersionId());
                scriptVO.setRelatedTaskPlanNum(taskPlanCiteCount);

                // 是否支持同步操作
                if (scriptDTO.getStatus().equals(JobResourceStatusEnum.ONLINE.getValue())) {
                    List<ScriptSyncTemplateStepDTO> syncSteps = getSyncTemplateSteps(username, appId, scriptId,
                        scriptDTO.getScriptVersionId());
                    scriptVO.setSyncEnabled(!syncSteps.isEmpty());
                } else {
                    scriptVO.setSyncEnabled(false);
                }
                resultVOS.add(scriptVO);

            }
        }

        resultVOS = excludeNotOnlinePublicScriptVersion(username, appId, scriptId, resultVOS);
        return ServiceResponse.buildSuccessResp(resultVOS);
    }

    @Override
    public ServiceResponse<ScriptVO> saveScript(String username, Long appId,
                                                ScriptCreateUpdateReq scriptCreateUpdateReq) {
        scriptCreateUpdateReq.setAppId(appId);
        log.info("Save script,operator={},appId={},script={}", username, appId, scriptCreateUpdateReq.toString());

        AuthResultVO authResultVO = checkSaveScript(username, appId, scriptCreateUpdateReq);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }

        try {
            StringCheckHelper scriptNameCheckHelper = new StringCheckHelper(new TrimChecker(), new NotEmptyChecker(),
                new IlegalCharChecker(), new MaxLengthChecker(60));
            scriptCreateUpdateReq.setName(scriptNameCheckHelper.checkAndGetResult(scriptCreateUpdateReq.getName()));
        } catch (StringCheckException e) {
            log.warn("Script name [{}] is invalid", scriptCreateUpdateReq.getName());
            String errorMsg = i18nService.getI18n(String.valueOf(ErrorCode.SCRIPT_NAME_INVALID));
            return ServiceResponse.buildCommonFailResp(ErrorCode.SCRIPT_NAME_INVALID, errorMsg);
        }
        try {
            StringCheckHelper scriptVersionCheckHelper = new StringCheckHelper(new TrimChecker(), new NotEmptyChecker(),
                new WhiteCharChecker("A-Za-z0-9_\\-#@\\."), new MaxLengthChecker(60));
            scriptCreateUpdateReq
                .setVersion(scriptVersionCheckHelper.checkAndGetResult(scriptCreateUpdateReq.getVersion()));
        } catch (StringCheckException e) {
            log.warn("Script version [{}] is invalid", scriptCreateUpdateReq.getVersion());
            String errorMsg = i18nService.getI18n(String.valueOf(ErrorCode.SCRIPT_VERSION_ILLEGAL));
            return ServiceResponse.buildCommonFailResp(ErrorCode.SCRIPT_VERSION_ILLEGAL, errorMsg);
        }
        ScriptDTO script = scriptDTOBuilder.buildFromCreateUpdateReq(scriptCreateUpdateReq);
        script.setAppId(appId);
        script.setCreator(username);
        script.setLastModifyUser(username);
        try {
            ScriptDTO savedScript = scriptService.saveScript(username, appId, script);
            // 只在新建脚本时新建关联权限，编辑/复制并新建版本时不动
            if (StringUtils.isBlank(scriptCreateUpdateReq.getId())) {
                if (script.isPublicScript()) {
                    // 公共脚本
                    authService.registerResource(
                        savedScript.getId(),
                        script.getName(),
                        ResourceId.PUBLIC_SCRIPT,
                        username,
                        null
                    );
                } else {
                    // 业务脚本
                    authService.registerResource(
                        savedScript.getId(),
                        script.getName(),
                        ResourceId.SCRIPT,
                        username,
                        null
                    );
                }
            }
            ScriptVO scriptVO = new ScriptVO();
            scriptVO.setScriptVersionId(savedScript.getScriptVersionId());
            scriptVO.setId(savedScript.getId());
            return ServiceResponse.buildSuccessResp(scriptVO);
        } catch (ServiceException e) {
            String errorMsg = i18nService.getI18n(String.valueOf(e.getErrorCode()));
            log.warn("Fail to save script, {}", errorMsg);
            return ServiceResponse.buildCommonFailResp(e.getErrorCode(), errorMsg);
        }
    }

    private AuthResultVO checkSaveScript(String username, long appId, ScriptCreateUpdateReq scriptCreateUpdateReq) {
        Long scriptVersionId = scriptCreateUpdateReq.getScriptVersionId();
        boolean isCreateNew = scriptVersionId == null || scriptVersionId < 0;
        // 创建脚本版本鉴管理权限
        if (!StringUtils.isBlank(scriptCreateUpdateReq.getId())) {
            isCreateNew = false;
        }
        return isCreateNew ? checkScriptCreatePermission(username, appId)
            : checkScriptManagePermission(username, appId, scriptCreateUpdateReq.getId());
    }

    @Override
    public ServiceResponse publishScriptVersion(String username, Long appId, String scriptId, Long scriptVersionId) {
        JobContextUtil.setAppId(appId);
        log.info("Publish script version, appId={}, scriptId={}, scriptVersionId={}, username={}", appId, scriptId,
            scriptVersionId, username);
        if (appId == null || scriptId == null || scriptVersionId == null) {
            log.warn("Illegal param,appId:{}, scriptId:{}, scriptVersion:{}", appId, scriptId, scriptVersionId);
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM,
                i18nService.getI18n(String.valueOf(ErrorCode.ILLEGAL_PARAM)));
        }

        AuthResultVO authResultVO = checkScriptManagePermission(username, appId, scriptId);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }

        try {
            scriptService.publishScript(appId, username, scriptId, scriptVersionId);
        } catch (ServiceException e) {
            log.warn("Publish script version error, errorCode={}", e.getErrorCode());
            return ServiceResponse.buildCommonFailResp(e.getErrorCode(),
                i18nService.getI18n(String.valueOf(e.getErrorCode())));
        }
        return ServiceResponse.buildSuccessResp(null);
    }

    @Override
    public ServiceResponse disableScriptVersion(String username, Long appId, String scriptId, Long scriptVersionId) {
        JobContextUtil.setAppId(appId);
        log.info("Disable script version, appId={}, scriptId={}, scriptVersionId={}, username={}", appId, scriptId,
            scriptVersionId, username);

        AuthResultVO authResultVO = checkScriptManagePermission(username, appId, scriptId);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }

        try {
            scriptService.disableScript(appId, username, scriptId, scriptVersionId);
        } catch (ServiceException e) {
            log.warn("Disable script version error, errorCode={}", e.getErrorCode());
            return ServiceResponse.buildCommonFailResp(e.getErrorCode(),
                i18nService.getI18n(String.valueOf(e.getErrorCode())));
        }
        return ServiceResponse.buildSuccessResp(null);
    }

    @Override
    public ServiceResponse deleteScriptByScriptId(String username, Long appId, String scriptId) {
        JobContextUtil.setAppId(appId);
        log.info("Delete script[{}], operator={}, appId={}", scriptId, username, appId);

        AuthResultVO authResultVO = checkScriptManagePermission(username, appId, scriptId);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }

        try {
            scriptService.deleteScript(username, appId, scriptId);
        } catch (ServiceException e) {
            String errorMsg = i18nService.getI18n(String.valueOf(e.getErrorCode()));
            log.warn("Fail to delete script, appId={}, scriptId={}, reason={}", appId, scriptId, errorMsg);
            return ServiceResponse.buildCommonFailResp(e.getErrorCode(), errorMsg);
        }
        return ServiceResponse.buildSuccessResp(null);
    }

    @Override
    public ServiceResponse deleteScriptByScriptVersionId(String username, Long appId, Long scriptVersionId) {
        JobContextUtil.setAppId(appId);
        log.info("Delete scriptVersion[{}], operator={}, appId={}", scriptVersionId, username, appId);

        ScriptDTO script = scriptService.getScriptVersion(username, appId, scriptVersionId);
        if (script == null) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.SCRIPT_NOT_EXIST);
        }

        AuthResultVO authResultVO = checkScriptManagePermission(username, appId, script.getId());
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }

        try {
            scriptService.deleteScriptVersion(username, appId, scriptVersionId);
        } catch (ServiceException e) {
            String errorMsg = i18nService.getI18n(String.valueOf(e.getErrorCode()));
            log.warn("Fail to delete script version,appId={}, scriptVersionId={}, reason={}", appId, scriptVersionId,
                errorMsg);
            return ServiceResponse.buildCommonFailResp(e.getErrorCode(), errorMsg);
        }
        return ServiceResponse.buildSuccessResp(null);
    }

    @Override
    public ServiceResponse listAppScriptNames(String username, Long appId, String scriptName) {
        JobContextUtil.setAppId(appId);
        List<String> scriptNames = scriptService.listScriptNames(appId, scriptName);
        return ServiceResponse.buildSuccessResp(scriptNames);
    }

    @Override
    public ServiceResponse<List<BasicScriptVO>> listScriptOnline(String username, Long appId, Boolean publicScript) {
        long queryAppId = appId;
        if (publicScript != null && publicScript) {
            queryAppId = PUBLIC_APP_ID;
        }
        List<ScriptDTO> scriptList = scriptService.listOnlineScriptForApp(username, queryAppId);
        if (scriptList == null || scriptList.isEmpty()) {
            return ServiceResponse.buildSuccessResp(Collections.emptyList());
        }

        List<BasicScriptVO> scriptVOList = new ArrayList<>();
        for (ScriptDTO script : scriptList) {
            BasicScriptVO basicScriptVO = ScriptConverter.convertToBasicScriptVO(script);
            scriptVOList.add(basicScriptVO);
        }
        processScriptPermission(username, queryAppId, scriptVOList);
        return ServiceResponse.buildSuccessResp(scriptVOList);
    }

    private void processScriptPermission(String username, Long appId, List<BasicScriptVO> scriptList) {
        List<String> scriptIdList = new ArrayList<>();
        scriptList.forEach(script -> scriptIdList.add(script.getId()));

        if (PUBLIC_APP_ID == appId) {
            AuthResultVO managePermAuthResult = authService.auth(
                false,
                username,
                ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE
            );
            scriptList.forEach(script -> {
                script.setCanManage(managePermAuthResult.isPass());
                script.setCanView(true);
            });
        } else {
            List<String> allowedManageScriptIdList =
                authService.batchAuth(username, ActionId.MANAGE_SCRIPT, appId, ResourceTypeEnum.SCRIPT, scriptIdList);
            List<String> allowedViewScriptIdList =
                authService.batchAuth(username, ActionId.VIEW_SCRIPT, appId, ResourceTypeEnum.SCRIPT, scriptIdList);
            scriptList
                .forEach(script -> {
                    script.setCanManage(allowedManageScriptIdList.contains(script.getId()));
                    script.setCanView(allowedViewScriptIdList.contains(script.getId()));
                });
        }
    }

    @Override
    public ServiceResponse<List<ScriptCheckResultItemVO>> checkScript(String username, ScriptCheckReq scriptCheckReq) {
        if (scriptCheckReq.getScriptType() == null || StringUtils.isBlank(scriptCheckReq.getContent())) {
            log.warn("Check script, request is illegal! req={}", scriptCheckReq);
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM,
                i18nService.getI18n(String.valueOf(ErrorCode.ILLEGAL_PARAM)));
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
        return ServiceResponse.buildSuccessResp(checkResultItemVOS);
    }

    @Override
    public ServiceResponse<ScriptVO> uploadScript(String username, MultipartFile scriptFile) {
        String uploadFileName = scriptFile.getOriginalFilename();
        if (StringUtils.isBlank(uploadFileName)) {
            log.warn("Script file name is empty!");
            return ServiceResponse.buildCommonFailResp(ErrorCode.UPLOAD_SCRIPT_FILE_NAME_EMPTY,
                i18nService.getI18n(String.valueOf(ErrorCode.UPLOAD_SCRIPT_FILE_NAME_EMPTY)));
        }
        String ext = uploadFileName.substring(uploadFileName.lastIndexOf("."));
        ScriptTypeEnum type = ScriptTypeEnum.getTypeByExt(ext);
        if (type == null) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.UPLOAD_SCRIPT_EXT_TYPE_ILLEGAL,
                i18nService.getI18n(String.valueOf(ErrorCode.UPLOAD_SCRIPT_EXT_TYPE_ILLEGAL)));
        }

        String fileContent;
        try {
            fileContent = fileToString(scriptFile);
            if (StringUtils.isNotEmpty(fileContent) && !EncodingUtils.isMessyCode(fileContent)) {
                ScriptVO script = new ScriptVO();
                script.setContent(Base64Util.encodeContentToStr(fileContent));
                script.setType(type.getValue());
                script.setTypeName(type.getName());
                return ServiceResponse.buildSuccessResp(script);
            } else {
                return ServiceResponse.buildCommonFailResp(ErrorCode.UPLOAD_SCRIPT_CONTENT_ILLEGAL,
                    i18nService.getI18n(String.valueOf(ErrorCode.UPLOAD_SCRIPT_CONTENT_ILLEGAL)));
            }
        } catch (Exception e) {
            log.error("Fail to parse script content", e);
            return ServiceResponse.buildCommonFailResp(ErrorCode.UPLOAD_SCRIPT_CONTENT_ILLEGAL,
                i18nService.getI18n(String.valueOf(ErrorCode.UPLOAD_SCRIPT_CONTENT_ILLEGAL)));
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

    private AuthResultVO checkScriptViewPermission(String username, long appId, String scriptId) {
        boolean isPublicScript = (appId == PUBLIC_APP_ID);
        if (isPublicScript) {
            // 公共脚本默认公开，无需查看权限
            return AuthResultVO.pass();
        }
        return authService.auth(true, username, ActionId.VIEW_SCRIPT, ResourceTypeEnum.SCRIPT, scriptId,
            buildAppScriptPathInfo(appId));
    }

    private AuthResultVO checkScriptViewPermission(String username, long appId, ScriptDTO script) {
        if (script.isPublicScript()) {
            // 公共脚本默认公开，无需查看权限
            return AuthResultVO.pass();
        }
        return authService.auth(true, username, ActionId.VIEW_SCRIPT, ResourceTypeEnum.SCRIPT,
            script.getId(), buildAppScriptPathInfo(appId));
    }


    private AuthResultVO checkScriptManagePermission(String username, long appId, String scriptId) {
        boolean isPublicScript = (appId == PUBLIC_APP_ID);
        if (isPublicScript) {
            return authService.auth(true, username, ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE,
                ResourceTypeEnum.PUBLIC_SCRIPT, scriptId, null);
        } else {
            return authService.auth(true, username, ActionId.MANAGE_SCRIPT, ResourceTypeEnum.SCRIPT,
                scriptId, buildAppScriptPathInfo(appId));
        }
    }

    private AuthResultVO checkScriptCreatePermission(String username, long appId) {
        boolean isPublicScript = (appId == PUBLIC_APP_ID);
        if (isPublicScript) {
            return authService.auth(true, username, ActionId.CREATE_PUBLIC_SCRIPT);
        } else {
            return authService.auth(true, username, ActionId.CREATE_SCRIPT, ResourceTypeEnum.BUSINESS,
                String.valueOf(appId), null);
        }
    }

    private PathInfoDTO buildAppScriptPathInfo(Long appId) {
        return PathBuilder.newBuilder(ResourceTypeEnum.BUSINESS.getId(), appId.toString()).build();
    }

    @Override
    public ServiceResponse<List<ScriptRelatedTemplateStepVO>> listScriptSyncTemplateSteps(
        String username,
        Long appId,
        String scriptId,
        Long scriptVersionId
    ) {
        List<ScriptSyncTemplateStepDTO> steps = getSyncTemplateSteps(username, appId, scriptId, scriptVersionId);
        if (CollectionUtils.isEmpty(steps)) {
            return ServiceResponse.buildSuccessResp(Collections.emptyList());
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
        return ServiceResponse.buildSuccessResp(stepVOS);
    }

    private List<ScriptSyncTemplateStepDTO> getSyncTemplateSteps(
        String username,
        Long appId,
        String scriptId,
        Long scriptVersionId
    ) {
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
    public ServiceResponse<List<ScriptSyncResultVO>> syncScripts(
        String username,
        Long appId,
        String scriptId,
        Long scriptVersionId,
        ScriptSyncReq scriptSyncReq
    ) {
        if (scriptSyncReq == null || CollectionUtils.isEmpty(scriptSyncReq.getSteps())) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM, i18nService);
        }
        List<TemplateStepIDDTO> templateStepIDs = new ArrayList<>(scriptSyncReq.getSteps().size());
        scriptSyncReq.getSteps().forEach(step -> {
            templateStepIDs.add(new TemplateStepIDDTO(step.getTemplateId(), step.getStepId()));
        });

        List<SyncScriptResultDTO> syncResults = scriptService.syncScriptToTaskTemplate(username, appId, scriptId,
            scriptVersionId, templateStepIDs);
        List<ScriptSyncResultVO> syncResultVOS = new ArrayList<>(syncResults.size());
        for (SyncScriptResultDTO syncResult : syncResults) {
            ScriptSyncResultVO syncVO = new ScriptSyncResultVO();
            ScriptSyncTemplateStepDTO syncStep = syncResult.getTemplateStep();
            syncVO.setAppId(syncStep.getAppId());
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
        return ServiceResponse.buildSuccessResp(syncResultVOS);
    }

    private ScriptCiteCountVO getScriptCiteCountOfAllScript(
        String username,
        Long appId,
        String scriptId,
        Long scriptVersionId
    ) {
        Integer templateCiteCount = scriptService.getScriptTemplateCiteCount(username, appId, scriptId,
            scriptVersionId);
        Integer taskPlanCiteCount = scriptService.getScriptTaskPlanCiteCount(username, appId, scriptId,
            scriptVersionId);
        return new ScriptCiteCountVO(templateCiteCount, taskPlanCiteCount);
    }

    private ScriptCiteInfoVO getScriptCiteInfoOfAllScript(
        String username,
        Long appId,
        String scriptId,
        Long scriptVersionId
    ) {
        List<ScriptCitedTaskTemplateDTO> citedTemplateList = scriptService.getScriptCitedTemplates(username, appId,
            scriptId, scriptVersionId);
        if (citedTemplateList == null) {
            citedTemplateList = Collections.emptyList();
        }
        List<ScriptCitedTemplateVO> citedTemplateVOList =
            citedTemplateList.parallelStream().map(ScriptCitedTaskTemplateDTO::toVO).collect(Collectors.toList());
        List<ScriptCitedTaskPlanDTO> citedTaskPlanList = scriptService.getScriptCitedTaskPlans(username, appId,
            scriptId, scriptVersionId);
        if (citedTaskPlanList == null) {
            citedTaskPlanList = Collections.emptyList();
        }
        List<ScriptCitedTaskPlanVO> citedTaskPlanVOList =
            citedTaskPlanList.parallelStream().map(ScriptCitedTaskPlanDTO::toVO).collect(Collectors.toList());
        return new ScriptCiteInfoVO(citedTemplateVOList, citedTaskPlanVOList);
    }

    @Override
    public ServiceResponse<ScriptCiteInfoVO> getScriptCiteInfo(
        String username,
        Long appId,
        String scriptId,
        Long scriptVersionId
    ) {
        ScriptCiteInfoVO scriptCiteInfoVO = getScriptCiteInfoOfAllScript(username, appId, scriptId, scriptVersionId);
        return ServiceResponse.buildSuccessResp(scriptCiteInfoVO);
    }

    @Override
    public ServiceResponse<ScriptCiteCountVO> getScriptCiteCount(
        String username,
        Long appId,
        String scriptId,
        Long scriptVersionId
    ) {
        ScriptCiteCountVO scriptCiteCountVO = getScriptCiteCountOfAllScript(username, appId, scriptId, scriptVersionId);
        return ServiceResponse.buildSuccessResp(scriptCiteCountVO);
    }

    @Override
    public ServiceResponse<?> batchUpdateScriptTags(String username, Long appId, ScriptTagBatchPatchReq req) {
        ValidateResult validateResult = checkScriptTagBatchPatchReq(req);
        if (!validateResult.isPass()) {
            return ServiceResponse.buildValidateFailResp(i18nService, validateResult);
        }

        boolean isPublicScript = appId == PUBLIC_APP_ID;
        List<String> scriptIdList = req.getIdList();

        ResourceTypeEnum iamResourceType = isPublicScript ? ResourceTypeEnum.PUBLIC_SCRIPT : ResourceTypeEnum.SCRIPT;
        String actionId = isPublicScript ? ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE : ActionId.MANAGE_SCRIPT;
        AuthResultVO authResultVO = batchAuthScript(username, actionId, appId, iamResourceType, scriptIdList);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }

        List<ResourceTagDTO> addResourceTags = null;
        List<ResourceTagDTO> deleteResourceTags = null;
        Integer resourceType = appId == PUBLIC_APP_ID ? JobResourceTypeEnum.PUBLIC_SCRIPT.getValue() :
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

        return ServiceResponse.buildSuccessResp(true);
    }

    private AuthResultVO batchAuthScript(String username, String actionId, Long appId, ResourceTypeEnum resourceType,
                                         List<String> scriptIdList) {
        List<PermissionResource> resources = scriptIdList.stream().map(scriptId -> {
            PermissionResource resource = new PermissionResource();
            resource.setResourceId(scriptId);
            resource.setResourceType(resourceType);
            if (resourceType == ResourceTypeEnum.SCRIPT) {
                resource.setPathInfo(PathBuilder.newBuilder(
                    ResourceTypeEnum.BUSINESS.getId(),
                    appId.toString()
                ).build());
            }
            return resource;
        }).collect(Collectors.toList());
        return authService.batchAuthResources(username, actionId, appId, resources);
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
    public ServiceResponse<TagCountVO> getTagScriptCount(String username, Long appId) {

        return ServiceResponse.buildSuccessResp(scriptService.getTagScriptCount(appId));
    }
}
