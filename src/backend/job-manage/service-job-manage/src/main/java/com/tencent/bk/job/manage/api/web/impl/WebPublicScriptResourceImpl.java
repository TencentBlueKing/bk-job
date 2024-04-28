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

import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.audit.annotations.AuditRequestBody;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.JobResourceTypeEnum;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.manage.api.common.ScriptDTOBuilder;
import com.tencent.bk.job.manage.api.common.constants.JobResourceStatusEnum;
import com.tencent.bk.job.manage.api.web.WebPublicScriptResource;
import com.tencent.bk.job.manage.auth.NoResourceScopeAuthService;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.dto.ScriptSyncTemplateStepDTO;
import com.tencent.bk.job.manage.model.dto.SyncScriptResultDTO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.model.dto.TemplateStepIDDTO;
import com.tencent.bk.job.manage.model.dto.converter.ScriptConverter;
import com.tencent.bk.job.manage.model.query.ScriptQuery;
import com.tencent.bk.job.manage.model.web.request.ScriptCreateReq;
import com.tencent.bk.job.manage.model.web.request.ScriptInfoUpdateReq;
import com.tencent.bk.job.manage.model.web.request.ScriptSyncReq;
import com.tencent.bk.job.manage.model.web.request.ScriptTagBatchPatchReq;
import com.tencent.bk.job.manage.model.web.request.ScriptVersionCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.BasicScriptVO;
import com.tencent.bk.job.manage.model.web.vo.ScriptVO;
import com.tencent.bk.job.manage.model.web.vo.TagCountVO;
import com.tencent.bk.job.manage.model.web.vo.script.ScriptCiteCountVO;
import com.tencent.bk.job.manage.model.web.vo.script.ScriptCiteInfoVO;
import com.tencent.bk.job.manage.model.web.vo.script.ScriptRelatedTemplateStepVO;
import com.tencent.bk.job.manage.model.web.vo.script.ScriptSyncResultVO;
import com.tencent.bk.job.manage.service.PublicScriptService;
import com.tencent.bk.job.manage.service.ScriptManager;
import com.tencent.bk.job.manage.service.TagService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.tencent.bk.job.common.constant.JobConstants.PUBLIC_APP_ID;

/**
 * 公共脚本Resource
 */
@RestController
@Slf4j
public class WebPublicScriptResourceImpl extends BaseWebScriptResource implements WebPublicScriptResource {
    private final PublicScriptService publicScriptService;

    private final NoResourceScopeAuthService noResourceScopeAuthService;

    @Autowired
    public WebPublicScriptResourceImpl(PublicScriptService publicScriptService,
                                       MessageI18nService i18nService,
                                       ScriptDTOBuilder scriptDTOBuilder,
                                       NoResourceScopeAuthService noResourceScopeAuthService,
                                       TagService tagService,
                                       ScriptManager scriptManager) {
        super(i18nService, scriptDTOBuilder, tagService, scriptManager);
        this.publicScriptService = publicScriptService;
        this.noResourceScopeAuthService = noResourceScopeAuthService;
    }

    @Override
    public Response<ScriptVO> getScriptVersionDetail(String username,
                                                     Long scriptVersionId) {
        ScriptDTO script = publicScriptService.getScriptVersion(scriptVersionId);

        ScriptVO scriptVO = ScriptConverter.convertToScriptVO(script);
        return Response.buildSuccessResp(scriptVO);
    }


    @Override
    public Response<ScriptVO> getScript(String username,
                                        String scriptId) {
        ScriptDTO script = publicScriptService.getScript(scriptId);

        List<ScriptDTO> scriptVersions = publicScriptService.listScriptVersion(scriptId);
        if (CollectionUtils.isEmpty(scriptVersions)) {
            throw new NotFoundException(ErrorCode.SCRIPT_NOT_EXIST);
        }

        List<ScriptVO> scriptVersionVOS = convertToScriptVOList(scriptVersions);
        ScriptVO scriptVO = ScriptConverter.convertToScriptVO(script);
        scriptVO.setScriptVersions(scriptVersionVOS);

        return Response.buildSuccessResp(scriptVO);
    }

    @Override
    public Response<ScriptVO> getScriptBasicInfo(String username,
                                                 String scriptId) {
        ScriptDTO script = publicScriptService.getScript(scriptId);
        ScriptVO scriptVO = ScriptConverter.convertToScriptVO(script);
        return Response.buildSuccessResp(scriptVO);
    }

    @Override
    public Response<ScriptVO> getOnlineScriptVersionByScriptId(String username,
                                                               String scriptId) {
        ScriptDTO onlineScriptVersion =
            publicScriptService.getOnlineScriptVersionByScriptId(scriptId);
        if (onlineScriptVersion == null) {
            return Response.buildSuccessResp(null);
        }

        ScriptVO onlineScriptVO = ScriptConverter.convertToScriptVO(onlineScriptVersion);
        return Response.buildSuccessResp(onlineScriptVO);
    }

    @Override
    public Response<PageData<ScriptVO>> listPageScript(String username,
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
        ScriptQuery scriptQuery = buildListPageScriptQuery(null, name, type, tags, panelTag,
            panelType, creator, lastModifyUser, scriptId, content, start, pageSize, orderField, order);

        PageData<ScriptDTO> pageData = publicScriptService.listPageScript(scriptQuery);
        PageData<ScriptVO> resultPageData = pageVOs(pageData, start, pageSize);

        //设置脚本引用信息
        setScriptCiteCount(resultPageData.getData());

        // 设置脚本的最新版本
        setOnlineScriptVersionInfo(resultPageData.getData());

        // 设置权限
        processPermissionForList(username, resultPageData);
        resultPageData.setExistAny(publicScriptService.isExistAnyPublicScript());

        return Response.buildSuccessResp(resultPageData);
    }

    private void processPermissionForList(String username,
                                          PageData<ScriptVO> resultPageData) {
        resultPageData.setCanCreate(
            noResourceScopeAuthService.authCreatePublicScript(username).isPass()
        );

        resultPageData.getData()
            .forEach(script -> script.setCanManage(
                noResourceScopeAuthService.authManagePublicScript(username, script.getId()).isPass()));
        resultPageData.getData().forEach(script -> script.setCanView(true));
    }


    @Override
    @AuditEntry(actionId = ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE)
    public Response<ScriptVO> updateScriptInfo(String username,
                                               String scriptId,
                                               @AuditRequestBody ScriptInfoUpdateReq request) {
        String updateField = request.getUpdateField();
        boolean isUpdateDesc = "scriptDesc".equals(updateField);
        boolean isUpdateName = "scriptName".equals(updateField);
        boolean isUpdateTags = "scriptTags".equals(updateField);

        if (StringUtils.isBlank(updateField) || !(isUpdateDesc || isUpdateName || isUpdateTags)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }

        ScriptDTO updateScript;
        if (isUpdateDesc) {
            updateScript = publicScriptService.updateScriptDesc(username, scriptId, request.getScriptDesc());
        } else if (isUpdateName) {
            updateScript = updateScriptName(username, scriptId, request);
        } else {
            updateScript = updateScriptTags(username, scriptId, request);
        }
        return Response.buildSuccessResp(ScriptConverter.convertToScriptVO(updateScript));
    }

    private ScriptDTO updateScriptName(String operator, String scriptId, ScriptInfoUpdateReq scriptInfoUpdateReq) {
        scriptInfoUpdateReq.validateScriptName();
        return publicScriptService.updateScriptName(operator, scriptId, scriptInfoUpdateReq.getScriptName());
    }

    private ScriptDTO updateScriptTags(String operator, String scriptId, ScriptInfoUpdateReq scriptInfoUpdateReq) {
        List<TagDTO> tags = extractTags(scriptInfoUpdateReq);
        return publicScriptService.updateScriptTags(operator, scriptId, tags);
    }

    @Override
    public Response<List<ScriptVO>> listScriptBasicInfo(String username,
                                                        List<String> scriptIds) {
        ScriptQuery scriptQuery = new ScriptQuery();
        scriptQuery.setAppId(PUBLIC_APP_ID);
        scriptQuery.setIds(scriptIds);
        scriptQuery.setPublicScript(true);
        List<ScriptDTO> scripts = publicScriptService.listScripts(scriptQuery);
        if (CollectionUtils.isNotEmpty(scripts)) {
            scripts = scripts.stream().filter(script -> script.getAppId().equals(PUBLIC_APP_ID))
                .collect(Collectors.toList());
        }

        List<ScriptVO> scriptVOS = scripts.stream().map(ScriptConverter::convertToScriptVO)
            .collect(Collectors.toList());
        return Response.buildSuccessResp(scriptVOS);
    }

    @Override
    public Response<List<ScriptVO>> listScriptVersion(String username,
                                                      String scriptId) {
        // 鉴权
        AuthResult manageAuthResult = noResourceScopeAuthService.authManagePublicScript(username, scriptId);

        List<ScriptDTO> scripts = publicScriptService.listScriptVersion(scriptId);
        List<ScriptVO> resultVOS = new ArrayList<>();
        if (scripts != null && !scripts.isEmpty()) {
            for (ScriptDTO scriptDTO : scripts) {
                ScriptVO scriptVO = ScriptConverter.convertToScriptVO(scriptDTO);
                scriptVO.setCanView(true);
                scriptVO.setCanManage(manageAuthResult.isPass());
                // 克隆需要管理权限
                scriptVO.setCanClone(true);

                // 是否支持同步操作
                if (scriptDTO.getStatus().equals(JobResourceStatusEnum.ONLINE.getValue())) {
                    List<ScriptSyncTemplateStepDTO> syncSteps = getSyncTemplateSteps(PUBLIC_APP_ID, scriptId,
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
    @AuditEntry(actionId = ActionId.CREATE_PUBLIC_SCRIPT)
    public Response<ScriptVO> saveScript(String username,
                                         @AuditRequestBody ScriptCreateReq request) {

        ScriptDTO script = buildCreateScriptDTO(request, username);
        ScriptDTO savedScript = publicScriptService.saveScript(username, script);
        ScriptVO scriptVO = ScriptConverter.convertToScriptVO(savedScript);
        return Response.buildSuccessResp(scriptVO);
    }

    private ScriptDTO buildCreateScriptDTO(ScriptCreateReq request,
                                           String username) {
        ScriptDTO script = scriptDTOBuilder.buildFromScriptCreateReq(request);
        script.setAppId(PUBLIC_APP_ID);
        script.setPublicScript(true);
        script.setCreator(username);
        script.setLastModifyUser(username);
        return script;
    }

    @Override
    @AuditEntry(actionId = ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE)
    public Response<ScriptVO> saveScriptVersion(String username,
                                                String scriptId,
                                                @AuditRequestBody ScriptVersionCreateUpdateReq request) {
        ScriptDTO script = buildCreateOrUpdateScriptVersion(true, request, scriptId,
            null, username);
        ScriptDTO savedScript = publicScriptService.saveScriptVersion(username, script);

        ScriptVO scriptVO = ScriptConverter.convertToScriptVO(savedScript);
        return Response.buildSuccessResp(scriptVO);
    }

    private ScriptDTO buildCreateOrUpdateScriptVersion(boolean isCreate,
                                                       ScriptVersionCreateUpdateReq request,
                                                       String scriptId,
                                                       Long scriptVersionId,
                                                       String username) {
        ScriptDTO script = scriptDTOBuilder.buildFromScriptVersionCreateUpdateReq(request);
        script.setId(scriptId);
        script.setAppId(PUBLIC_APP_ID);
        script.setPublicScript(true);
        script.setLastModifyUser(username);
        if (isCreate) {
            script.setCreator(username);
        } else {
            script.setScriptVersionId(scriptVersionId);
        }
        return script;
    }

    @Override
    @AuditEntry(actionId = ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE)
    public Response<ScriptVO> updateScriptVersion(String username,
                                                  String scriptId,
                                                  Long scriptVersionId,
                                                  @AuditRequestBody ScriptVersionCreateUpdateReq request) {
        ScriptDTO script = buildCreateOrUpdateScriptVersion(false, request, scriptId,
            scriptVersionId, username);
        ScriptDTO savedScriptVersion = publicScriptService.updateScriptVersion(username, script);

        ScriptVO scriptVO = ScriptConverter.convertToScriptVO(savedScriptVersion);
        return Response.buildSuccessResp(scriptVO);
    }

    @Override
    @AuditEntry(actionId = ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE)
    public Response publishScriptVersion(String username,
                                         String scriptId,
                                         Long scriptVersionId) {
        publicScriptService.publishScript(username, scriptId, scriptVersionId);
        return Response.buildSuccessResp(null);
    }

    @Override
    @AuditEntry(actionId = ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE)
    public Response disableScriptVersion(String username,
                                         String scriptId,
                                         Long scriptVersionId) {
        publicScriptService.disableScript(username, scriptId, scriptVersionId);
        return Response.buildSuccessResp(null);
    }

    @Override
    @AuditEntry(actionId = ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE)
    public Response deleteScriptByScriptId(String username,
                                           String scriptId) {
        publicScriptService.deleteScript(username, scriptId);
        return Response.buildSuccessResp(null);
    }

    @Override
    @AuditEntry(actionId = ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE)
    public Response deleteScriptByScriptVersionId(String username,
                                                  Long scriptVersionId) {
        publicScriptService.deleteScriptVersion(username, scriptVersionId);
        return Response.buildSuccessResp(null);
    }

    @Override
    public Response<List<String>> listPublicScriptNames(String username,
                                                        String scriptName) {
        List<String> scriptNames = publicScriptService.listScriptNames(scriptName);
        return Response.buildSuccessResp(scriptNames);
    }

    @Override
    public Response<List<BasicScriptVO>> listScriptOnline(String username) {
        List<ScriptDTO> scriptList = publicScriptService.listOnlineScript();
        List<BasicScriptVO> scriptVOList = convertToBasicScriptVOList(scriptList);
        processScriptPermission(username, scriptVOList);
        return Response.buildSuccessResp(scriptVOList);
    }

    private void processScriptPermission(String username,
                                         List<BasicScriptVO> scriptList) {
        scriptList.forEach(script -> {
            AuthResult managePermAuthResult = noResourceScopeAuthService.authManagePublicScript(username,
                script.getId());
            script.setCanManage(managePermAuthResult.isPass());
            script.setCanView(true);
        });
    }

    @Override
    public Response<List<ScriptRelatedTemplateStepVO>> listScriptSyncTemplateSteps(String username,
                                                                                   String scriptId,
                                                                                   Long scriptVersionId) {
        List<ScriptRelatedTemplateStepVO> stepVOS = listScriptSyncTemplateSteps(PUBLIC_APP_ID,
            scriptId, scriptVersionId);
        return Response.buildSuccessResp(stepVOS);
    }

    @Override
    @AuditEntry(actionId = ActionId.EDIT_JOB_TEMPLATE)
    public Response<List<ScriptSyncResultVO>> syncScripts(String username,
                                                          String scriptId,
                                                          Long scriptVersionId,
                                                          @AuditRequestBody ScriptSyncReq scriptSyncReq) {
        List<TemplateStepIDDTO> templateStepIDs = new ArrayList<>(scriptSyncReq.getSteps().size());
        scriptSyncReq.getSteps().forEach(step ->
            templateStepIDs.add(new TemplateStepIDDTO(step.getTemplateId(), step.getStepId())));

        List<SyncScriptResultDTO> syncResults = publicScriptService.syncScriptToTaskTemplate(username, scriptId,
            scriptVersionId, templateStepIDs);
        List<ScriptSyncResultVO> syncResultVOS = convertToSyncResultVOs(syncResults, null);
        return Response.buildSuccessResp(syncResultVOS);
    }

    @Override
    public Response<ScriptCiteInfoVO> getPublicScriptCiteInfo(String username,
                                                              String scriptId,
                                                              Long scriptVersionId) {
        ScriptCiteInfoVO scriptCiteInfoVO = getScriptCiteInfoOfAllScript(scriptId, scriptVersionId);
        return Response.buildSuccessResp(scriptCiteInfoVO);
    }

    @Override
    public Response<ScriptCiteCountVO> getPublicScriptCiteCount(String username,
                                                                String scriptId,
                                                                Long scriptVersionId) {
        ScriptCiteCountVO scriptCiteCountVO = getScriptCiteCountOfAllScript(
            scriptId, scriptVersionId);
        return Response.buildSuccessResp(scriptCiteCountVO);
    }

    @Override
    @AuditEntry(actionId = ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE)
    public Response<?> batchUpdatePublicScriptTags(String username,
                                                   @AuditRequestBody ScriptTagBatchPatchReq req) {
        // 校验
        req.validate();

        // 鉴权
        List<String> scriptIdList = req.getIdList();
        noResourceScopeAuthService.batchAuthResultManagePublicScript(username, scriptIdList).denyIfNoPermission();

        batchPatchResourceTags(JobResourceTypeEnum.PUBLIC_SCRIPT, scriptIdList, req.getAddTagIdList(),
            req.getDeleteTagIdList());

        return Response.buildSuccessResp(true);
    }


    @Override
    public Response<TagCountVO> getTagPublicScriptCount(String username) {
        return Response.buildSuccessResp(publicScriptService.getTagScriptCount());
    }

}
