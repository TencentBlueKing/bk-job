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

package com.tencent.bk.job.manage.service.impl;

import com.tencent.bk.audit.annotations.ActionAuditRecord;
import com.tencent.bk.audit.annotations.AuditAttribute;
import com.tencent.bk.audit.annotations.AuditInstanceRecord;
import com.tencent.bk.audit.context.ActionAuditContext;
import com.tencent.bk.job.common.audit.constants.EventContentConstants;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.manage.api.common.constants.JobResourceStatusEnum;
import com.tencent.bk.job.manage.auth.ScriptAuthService;
import com.tencent.bk.job.manage.model.dto.ScriptBasicDTO;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.dto.SyncScriptResultDTO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.model.dto.TemplateStepIDDTO;
import com.tencent.bk.job.manage.model.query.ScriptQuery;
import com.tencent.bk.job.manage.model.web.vo.TagCountVO;
import com.tencent.bk.job.manage.service.ScriptManager;
import com.tencent.bk.job.manage.service.ScriptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 业务脚本 Service
 */
@Slf4j
@Service
public class ScriptServiceImpl implements ScriptService {
    private final ScriptManager scriptManager;
    private final ScriptAuthService scriptAuthService;

    @Autowired
    public ScriptServiceImpl(ScriptManager scriptManager,
                             ScriptAuthService scriptAuthService) {
        this.scriptManager = scriptManager;
        this.scriptAuthService = scriptAuthService;
    }

    @Override
    public PageData<ScriptDTO> listPageScript(ScriptQuery scriptQuery) {
        return scriptManager.listPageScript(scriptQuery);
    }

    @Override
    public List<ScriptDTO> listScripts(ScriptQuery scriptQuery) {
        return scriptManager.listScripts(scriptQuery);
    }

    @Override
    public ScriptDTO getScript(Long appId, String scriptId) {
        return scriptManager.getScript(appId, scriptId);
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.VIEW_SCRIPT,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.SCRIPT,
            instanceIds = "#scriptId",
            instanceNames = "#$?.name"
        ),
        content = EventContentConstants.VIEW_SCRIPT
    )
    public ScriptDTO getScript(User user, Long appId, String scriptId) {
        authViewScript(user, appId, scriptId);
        return getScript(appId, scriptId);
    }

    @Override
    public List<ScriptBasicDTO> listScriptBasicInfoByScriptIds(Collection<String> scriptIds) {
        return scriptManager.listScriptBasicInfoByScriptIds(scriptIds);
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.CREATE_SCRIPT,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.SCRIPT,
            instanceIds = "#$?.id",
            instanceNames = "#$?.name"
        ),
        content = EventContentConstants.CREATE_SCRIPT
    )
    public ScriptDTO createScript(User user, ScriptDTO script) {
        authCreateScript(user, script.getAppId());
        ScriptDTO newScript = scriptManager.createScript(script);
        scriptAuthService.registerScript(user, newScript.getId(), newScript.getName());
        return newScript;
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_SCRIPT,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.SCRIPT,
            instanceIds = "#scriptId"
        ),
        content = EventContentConstants.DELETE_SCRIPT
    )
    public void deleteScript(User user, Long appId, String scriptId) {
        log.info("Delete script[{}], operator={}, appId={}, scriptId", scriptId, user, scriptId);
        authManageScript(user, appId, scriptId);

        ScriptDTO script = getScript(appId, scriptId);
        if (script == null) {
            throw new NotFoundException(ErrorCode.SCRIPT_NOT_EXIST);
        }

        ActionAuditContext.current().setInstanceName(script.getName());

        scriptManager.deleteScript(appId, scriptId);
    }

    @Override
    public ScriptDTO getScriptVersion(long appId, Long scriptVersionId) {
        return scriptManager.getScriptVersion(appId, scriptVersionId);
    }

    @Override
    public ScriptDTO getScriptVersion(Long scriptVersionId) {
        return scriptManager.getScriptVersion(scriptVersionId);
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.VIEW_SCRIPT,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.SCRIPT
        ),
        content = EventContentConstants.VIEW_SCRIPT
    )
    public ScriptDTO getScriptVersion(User user, long appId, Long scriptVersionId) {
        ScriptDTO scriptVersion = getScriptVersion(appId, scriptVersionId);
        authViewScript(user, appId, scriptVersion.getId());

        ActionAuditContext.current()
            .setInstanceId(scriptVersion.getId())
            .setInstanceName(scriptVersion.getName());

        return scriptVersion;
    }

    @Override
    public List<ScriptDTO> listScriptVersion(long appId, String scriptId) {
        ScriptDTO script = getScript(appId, scriptId);
        if (script == null) {
            throw new NotFoundException(ErrorCode.SCRIPT_NOT_EXIST);
        }
        ActionAuditContext.current().setInstanceName(script.getName());

        return scriptManager.listScriptVersion(appId, scriptId);
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_SCRIPT,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.SCRIPT,
            instanceIds = "#scriptVersion?.id",
            instanceNames = "#$?.name"
        ),
        attributes = @AuditAttribute(
            name = "@VERSION", value = "#scriptVersion?.version"
        ),
        content = EventContentConstants.CREATE_SCRIPT_VERSION
    )
    public ScriptDTO createScriptVersion(User user, ScriptDTO scriptVersion) {
        authCreateScript(user, scriptVersion.getAppId());
        return scriptManager.createScriptVersion(scriptVersion);
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_SCRIPT,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.SCRIPT
        ),
        content = EventContentConstants.EDIT_SCRIPT_VERSION
    )
    public ScriptDTO updateScriptVersion(User user, ScriptDTO scriptVersion) {
        ScriptDTO originScriptVersion = getScriptVersion(scriptVersion.getScriptVersionId());
        if (originScriptVersion == null) {
            throw new NotFoundException(ErrorCode.SCRIPT_NOT_EXIST);
        }

        authManageScript(user, scriptVersion.getAppId(), originScriptVersion.getId());

        ScriptDTO updateScriptVersion = scriptManager.updateScriptVersion(scriptVersion);

        // 审计
        ActionAuditContext.current()
            .setInstanceId(scriptVersion.getId())
            .setInstanceName(originScriptVersion.getName())
            .setOriginInstance(originScriptVersion.toEsbScriptV3DTO())
            .addAttribute("@VERSION", originScriptVersion.toEsbScriptVersionDetailV3DTO())
            .setInstance(updateScriptVersion.toEsbScriptVersionDetailV3DTO());

        return updateScriptVersion;
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_SCRIPT,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.SCRIPT
        ),
        content = EventContentConstants.DELETE_SCRIPT_VERSION
    )
    public void deleteScriptVersion(User user, Long appId, Long scriptVersionId) {
        preProcessManageScriptVersion(user, appId, scriptVersionId);
        scriptManager.deleteScriptVersion(appId, scriptVersionId);
    }

    private void preProcessManageScriptVersion(User user, Long appId, Long scriptVersionId) {
        ScriptDTO scriptVersion = getScriptVersion(appId, scriptVersionId);
        authManageScript(user, appId, scriptVersion.getId());
        addScriptVersionAuditInfo(scriptVersion);
    }

    private void addScriptVersionAuditInfo(ScriptDTO scriptVersion) {
        ActionAuditContext.current()
            .setInstanceId(scriptVersion.getId())
            .setInstanceName(scriptVersion.getName())
            .addAttribute("@VERSION", scriptVersion.getVersion());
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_SCRIPT,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.SCRIPT
        ),
        content = EventContentConstants.ONLINE_SCRIPT_VERSION
    )
    public void publishScript(Long appId, User user, String scriptId, Long scriptVersionId) {
        log.info("Publish script version, appId={}, scriptId={}, scriptVersionId={}, username={}", appId,
            scriptId, scriptVersionId, user);
        preProcessManageScriptVersion(user, appId, scriptVersionId);

        scriptManager.publishScript(appId, scriptId, scriptVersionId);
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_SCRIPT,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.SCRIPT
        ),
        content = EventContentConstants.FORBIDDEN_SCRIPT_VERSION
    )
    public void disableScript(Long appId, User user, String scriptId, Long scriptVersionId) {
        log.info("Disable script version, appId={}, scriptId={}, scriptVersionId={}, username={}", appId,
            scriptId, scriptVersionId, user);
        preProcessManageScriptVersion(user, appId, scriptVersionId);
        scriptManager.disableScript(appId, scriptId, scriptVersionId);
    }

    @Override
    public Map<String, ScriptDTO> batchGetOnlineScriptVersionByScriptIds(List<String> scriptIdList) {
        return scriptManager.batchGetOnlineScriptVersionByScriptIds(scriptIdList);
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_SCRIPT,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.SCRIPT
        ),
        content = EventContentConstants.EDIT_SCRIPT
    )
    public ScriptDTO updateScriptDesc(Long appId, User user, String scriptId, String desc) {
        authManageScript(user, appId, scriptId);

        ScriptDTO originScript = getScript(appId, scriptId);
        if (originScript == null) {
            throw new NotFoundException(ErrorCode.SCRIPT_NOT_EXIST);
        }

        ScriptDTO updateScript = scriptManager.updateScriptDesc(user.getUsername(), appId, scriptId, desc);

        addModifyScriptAuditInfo(originScript, updateScript);

        return updateScript;
    }

    private void addModifyScriptAuditInfo(ScriptDTO originScript, ScriptDTO updateScript) {
        // 审计
        ActionAuditContext.current()
            .setInstanceId(originScript.getId())
            .setInstanceName(originScript.getName())
            .setOriginInstance(originScript.toEsbScriptV3DTO())
            .setInstance(updateScript.toEsbScriptV3DTO());
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_SCRIPT,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.SCRIPT
        ),
        content = EventContentConstants.EDIT_SCRIPT
    )
    public ScriptDTO updateScriptName(Long appId, User user, String scriptId, String newName) {
        authManageScript(user, appId, scriptId);

        ScriptDTO originScript = getScript(appId, scriptId);
        if (originScript == null) {
            throw new NotFoundException(ErrorCode.SCRIPT_NOT_EXIST);
        }

        ScriptDTO updateScript = scriptManager.updateScriptName(user, appId, scriptId, newName);

        addModifyScriptAuditInfo(originScript, updateScript);

        return updateScript;
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_SCRIPT,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.SCRIPT
        ),
        content = EventContentConstants.EDIT_SCRIPT
    )
    public ScriptDTO updateScriptTags(Long appId,
                                      User user,
                                      String scriptId,
                                      List<TagDTO> tags) {
        authManageScript(user, appId, scriptId);

        ScriptDTO originScript = getScript(appId, scriptId);
        if (originScript == null) {
            throw new NotFoundException(ErrorCode.SCRIPT_NOT_EXIST);
        }

        ScriptDTO updateScript = scriptManager.updateScriptTags(user.getUsername(), appId, scriptId, tags);

        addModifyScriptAuditInfo(originScript, updateScript);

        return updateScript;
    }

    @Override
    public List<String> listScriptNames(Long appId, String keyword) {
        return scriptManager.listScriptNames(appId, keyword);
    }

    @Override
    public List<ScriptDTO> listOnlineScript(String operator, long appId) {
        return scriptManager.listOnlineScriptForApp(appId);
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.VIEW_SCRIPT,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.SCRIPT,
            instanceIds = "#scriptId",
            instanceNames = "#$?.name"
        ),
        content = EventContentConstants.VIEW_SCRIPT
    )
    public ScriptDTO getOnlineScriptVersionByScriptId(User user, long appId, String scriptId) {
        authViewScript(user, appId, scriptId);
        return scriptManager.getOnlineScriptVersionByScriptId(appId, scriptId);
    }

    @Override
    public PageData<ScriptDTO> listPageScriptVersion(ScriptQuery scriptQuery) {
        return scriptManager.listPageScriptVersion(scriptQuery);
    }

    @Override
    public List<SyncScriptResultDTO> syncScriptToTaskTemplate(User user,
                                                              Long appId,
                                                              String scriptId,
                                                              Long syncScriptVersionId,
                                                              List<TemplateStepIDDTO> templateStepIDs) {
        return scriptManager.syncScriptToTaskTemplate(user,
            appId, scriptId, syncScriptVersionId, templateStepIDs);
    }

    @Override
    public boolean isExistAnyAppScript(long appId) {
        return scriptManager.isExistAnyScript(appId);
    }

    @Override
    public TagCountVO getTagScriptCount(Long appId) {
        return scriptManager.getTagScriptCount(appId);
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.VIEW_SCRIPT,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.SCRIPT,
            instanceIds = "#scriptId",
            instanceNames = "#$?.name"
        ),
        content = EventContentConstants.VIEW_SCRIPT
    )
    public ScriptDTO getByScriptIdAndVersion(User user, Long appId, String scriptId, String version) {
        return scriptManager.getByScriptIdAndVersion(appId, scriptId, version);
    }

    @Override
    public ScriptDTO getScriptByScriptId(String scriptId) {
        return scriptManager.getScriptByScriptId(scriptId);
    }

    private void authViewScript(User user, long appId, String scriptId) {
        scriptAuthService.authViewScript(user, new AppResourceScope(appId), scriptId, null)
            .denyIfNoPermission();
    }

    private void authCreateScript(User user, long appId) {
        scriptAuthService.authCreateScript(user, new AppResourceScope(appId)).denyIfNoPermission();
    }

    private void authManageScript(User user, long appId, String scriptId) {
        scriptAuthService.authManageScript(user, new AppResourceScope(appId), scriptId, null)
            .denyIfNoPermission();
    }

    @Override
    public Map<Long, JobResourceStatusEnum> batchGetScriptVersionStatus(Collection<Long> scriptVersionId) {
        return null;
    }
}
