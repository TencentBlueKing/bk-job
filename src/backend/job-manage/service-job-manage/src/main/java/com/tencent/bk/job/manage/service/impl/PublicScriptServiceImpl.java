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
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.manage.auth.NoResourceScopeAuthService;
import com.tencent.bk.job.manage.model.dto.ScriptBasicDTO;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.dto.SyncScriptResultDTO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.model.dto.TemplateStepIDDTO;
import com.tencent.bk.job.manage.model.query.ScriptQuery;
import com.tencent.bk.job.manage.model.web.vo.TagCountVO;
import com.tencent.bk.job.manage.service.PublicScriptService;
import com.tencent.bk.job.manage.service.ScriptManager;
import com.tencent.bk.job.manage.util.AssertUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.tencent.bk.job.common.constant.JobConstants.PUBLIC_APP_ID;

/**
 * 公共脚本Service
 */
@Slf4j
@Service
public class PublicScriptServiceImpl implements PublicScriptService {
    private final ScriptManager scriptManager;
    private final NoResourceScopeAuthService noResourceScopeAuthService;

    @Autowired
    public PublicScriptServiceImpl(ScriptManager scriptManager,
                                   NoResourceScopeAuthService noResourceScopeAuthService) {
        this.scriptManager = scriptManager;
        this.noResourceScopeAuthService = noResourceScopeAuthService;
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
    public ScriptDTO getScript(String scriptId) {
        return scriptManager.getScript(PUBLIC_APP_ID, scriptId);
    }

    @Override
    public ScriptDTO getScript(String tenantId, String scriptId) {
        ScriptDTO script = scriptManager.getScript(PUBLIC_APP_ID, scriptId);
        if (!checkScriptTenantMatch(script, tenantId)) {
            return null;
        }
        return script;
    }

    private boolean checkScriptTenantMatch(ScriptDTO script, String expectedTenantId) {
        if (script != null && !script.getTenantId().equals(expectedTenantId)) {
            log.info("Script is not belong to current tenant, currentUserTenantId: {}, scriptTenantId: {}",
                expectedTenantId, script.getTenantId());
            return false;
        }
        return true;
    }

    @Override
    public List<ScriptBasicDTO> listScriptBasicInfoByScriptIds(Collection<String> scriptIds) {
        return scriptManager.listScriptBasicInfoByScriptIds(scriptIds);
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.CREATE_PUBLIC_SCRIPT,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.PUBLIC_SCRIPT,
            instanceIds = "#$?.id",
            instanceNames = "#$?.name"
        ),
        content = EventContentConstants.CREATE_PUBLIC_SCRIPT
    )
    public ScriptDTO saveScript(User user, ScriptDTO script) {
        authCreatePublicScript(user);
        ScriptDTO savedScript = scriptManager.createScript(script);
        noResourceScopeAuthService.registerPublicScript(user, savedScript.getId(), savedScript.getName());
        return savedScript;
    }

    private void authCreatePublicScript(User user) {
        noResourceScopeAuthService.authCreatePublicScript(user).denyIfNoPermission();
    }

    private void authManagePublicScript(User user, String scriptId) {
        noResourceScopeAuthService.authManagePublicScript(user, scriptId).denyIfNoPermission();
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.PUBLIC_SCRIPT,
            instanceIds = "#scriptId"
        ),
        content = EventContentConstants.DELETE_PUBLIC_SCRIPT
    )
    public void deleteScript(User user, String scriptId) {
        log.info("Delete script[{}], operator={}", scriptId, user.getUsername());
        ScriptDTO script = getAndCheckScriptAvailable(user.getTenantId(), scriptId);

        authManagePublicScript(user, scriptId);

        ActionAuditContext.current().setInstanceName(script.getName());

        scriptManager.deleteScript(PUBLIC_APP_ID, scriptId);
    }

    private ScriptDTO getAndCheckScriptAvailable(String tenantId, String scriptId) throws NotFoundException {
        ScriptDTO script = getScript(tenantId, scriptId);
        AssertUtil.scriptAvailable(() -> script != null);
        return script;
    }

    private ScriptDTO getAndCheckScriptVersionAvailable(String tenantId,
                                                        Long scriptVersionId) throws NotFoundException {
        ScriptDTO script = getScriptVersion(tenantId, scriptVersionId);
        AssertUtil.scriptAvailable(() -> script != null);
        return script;
    }

    @Override
    public ScriptDTO getScriptVersion(String tenantId, Long scriptVersionId) {
        ScriptDTO scriptVersion = scriptManager.getScriptVersion(PUBLIC_APP_ID, scriptVersionId);
        if (!checkScriptTenantMatch(scriptVersion, tenantId)) {
            return null;
        }
        return scriptVersion;
    }

    @Override
    public ScriptDTO getScriptVersion(Long scriptVersionId) {
        return scriptManager.getScriptVersion(PUBLIC_APP_ID, scriptVersionId);
    }

    @Override
    public List<ScriptDTO> listScriptVersion(String scriptId) {
        return scriptManager.listScriptVersion(PUBLIC_APP_ID, scriptId);
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.PUBLIC_SCRIPT,
            instanceIds = "#scriptVersion?.id",
            instanceNames = "#$?.name"
        ),
        attributes = @AuditAttribute(
            name = "@VERSION", value = "#scriptVersion?.version"
        ),
        content = EventContentConstants.CREATE_PUBLIC_SCRIPT_VERSION
    )
    public ScriptDTO saveScriptVersion(User user, ScriptDTO scriptVersion) {
        getAndCheckScriptAvailable(user.getTenantId(), scriptVersion.getId());
        authManagePublicScript(user, scriptVersion.getId());
        return scriptManager.createScriptVersion(scriptVersion);
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.PUBLIC_SCRIPT
        ),
        content = EventContentConstants.EDIT_PUBLIC_SCRIPT_VERSION
    )
    public ScriptDTO updateScriptVersion(User user, ScriptDTO scriptVersion) {
        ScriptDTO originScript = getAndCheckScriptAvailable(user.getTenantId(), scriptVersion.getId());
        authManagePublicScript(user, scriptVersion.getId());
        ScriptDTO updateScript = scriptManager.updateScriptVersion(scriptVersion);

        // 审计
        addModifyPublicScriptVersionAuditInfo(originScript, updateScript);

        return updateScript;
    }

    private void addModifyPublicScriptVersionAuditInfo(ScriptDTO originScriptVersion, ScriptDTO updateScriptVersion) {
        // 审计
        ActionAuditContext.current()
            .setInstanceId(originScriptVersion.getId())
            .setInstanceName(originScriptVersion.getName())
            .setOriginInstance(originScriptVersion.toEsbScriptV3DTO())
            .setInstance(updateScriptVersion.toEsbScriptV3DTO())
            .addAttribute("@VERSION", originScriptVersion.getVersion());
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.PUBLIC_SCRIPT
        ),
        content = EventContentConstants.DELETE_PUBLIC_SCRIPT_VERSION
    )
    public void deleteScriptVersion(User user, Long scriptVersionId) {
        log.info("Delete scriptVersion[{}], operator={}", scriptVersionId, user);
        preProcessManagePublicScriptVersion(user, scriptVersionId);
        scriptManager.deleteScriptVersion(PUBLIC_APP_ID, scriptVersionId);
    }

    private void preProcessManagePublicScriptVersion(User user, Long scriptVersionId) {
        ScriptDTO script = getAndCheckScriptVersionAvailable(user.getTenantId(), scriptVersionId);

        authManagePublicScript(user, script.getId());

        ActionAuditContext.current()
            .setInstanceId(script.getId())
            .setInstanceName(script.getName())
            .addAttribute("@VERSION", script.getVersion());
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.PUBLIC_SCRIPT
        ),
        content = EventContentConstants.ONLINE_PUBLIC_SCRIPT_VERSION
    )
    public void publishScript(User user, String scriptId, Long scriptVersionId) {
        log.info("Publish script version, scriptId={}, scriptVersionId={}, user={}",
            scriptId, scriptVersionId, user);
        preProcessManagePublicScriptVersion(user, scriptVersionId);
        scriptManager.publishScript(PUBLIC_APP_ID, scriptId, scriptVersionId);
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.PUBLIC_SCRIPT
        ),
        content = EventContentConstants.FORBIDDEN_PUBLIC_SCRIPT_VERSION
    )
    public void disableScript(User user, String scriptId, Long scriptVersionId) {
        log.info("Disable script version, scriptId={}, scriptVersionId={}, user={}",
            scriptId, scriptVersionId, user);
        preProcessManagePublicScriptVersion(user, scriptVersionId);
        scriptManager.disableScript(PUBLIC_APP_ID, scriptId, scriptVersionId);
    }

    @Override
    public Map<String, ScriptDTO> batchGetOnlineScriptVersionByScriptIds(List<String> scriptIdList) {
        return scriptManager.batchGetOnlineScriptVersionByScriptIds(scriptIdList);
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.PUBLIC_SCRIPT
        ),
        content = EventContentConstants.EDIT_PUBLIC_SCRIPT
    )
    public ScriptDTO updateScriptDesc(User user, String scriptId, String desc) {
        ScriptDTO originScript = getAndCheckScriptAvailable(user.getTenantId(), scriptId);

        authManagePublicScript(user, scriptId);

        ScriptDTO updateScript = scriptManager.updateScriptDesc(user.getUsername(), PUBLIC_APP_ID, scriptId, desc);

        addModifyPublicScriptAuditInfo(originScript, updateScript);

        return updateScript;
    }

    private void addModifyPublicScriptAuditInfo(ScriptDTO originScript, ScriptDTO updateScript) {
        // 审计
        ActionAuditContext.current()
            .setInstanceId(originScript.getId())
            .setInstanceName(originScript.getName())
            .setOriginInstance(originScript.toEsbScriptV3DTO())
            .setInstance(updateScript.toEsbScriptV3DTO());
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.PUBLIC_SCRIPT
        ),
        content = EventContentConstants.EDIT_PUBLIC_SCRIPT
    )
    public ScriptDTO updateScriptName(User user, String scriptId, String newName) {
        ScriptDTO originScript = getAndCheckScriptAvailable(user.getTenantId(), scriptId);

        authManagePublicScript(user, scriptId);

        ScriptDTO updateScript = scriptManager.updateScriptName(user, PUBLIC_APP_ID, scriptId, newName);

        addModifyPublicScriptAuditInfo(originScript, updateScript);

        return updateScript;
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.PUBLIC_SCRIPT
        ),
        content = EventContentConstants.EDIT_PUBLIC_SCRIPT
    )
    public ScriptDTO updateScriptTags(User user,
                                      String scriptId,
                                      List<TagDTO> tags) {
        ScriptDTO originScript = getAndCheckScriptAvailable(user.getTenantId(), scriptId);

        authManagePublicScript(user, scriptId);

        ScriptDTO updateScript = scriptManager.updateScriptTags(user.getUsername(), PUBLIC_APP_ID, scriptId, tags);

        addModifyPublicScriptAuditInfo(originScript, updateScript);

        return updateScript;
    }

    @Override
    public List<String> listScriptNames(String tenantId, String keyword) {
        return scriptManager.listPublicScriptNames(tenantId, keyword);
    }

    @Override
    public List<ScriptDTO> listOnlineScript(String tenantId) {
        return scriptManager.listOnlinePublicScript(tenantId);
    }

    @Override
    public ScriptDTO getOnlineScriptVersionByScriptId(String scriptId) {
        return scriptManager.getOnlineScriptVersionByScriptId(PUBLIC_APP_ID, scriptId);
    }

    @Override
    public PageData<ScriptDTO> listPageScriptVersion(ScriptQuery scriptQuery) {
        return scriptManager.listPageScriptVersion(scriptQuery);
    }

    @Override
    public List<SyncScriptResultDTO> syncScriptToTaskTemplate(User user,
                                                              String scriptId,
                                                              Long syncScriptVersionId,
                                                              List<TemplateStepIDDTO> templateStepIDs) {
        getAndCheckScriptVersionAvailable(user.getTenantId(), syncScriptVersionId);
        return scriptManager.syncScriptToTaskTemplate(user, PUBLIC_APP_ID, scriptId, syncScriptVersionId,
            templateStepIDs);
    }

    @Override
    public List<String> listScriptIds(String tenantId) {
        return scriptManager.listPublicScriptIds(tenantId);
    }

    @Override
    public TagCountVO getTagScriptCount(String tenantId) {
        return scriptManager.getTagPublicScriptCount(tenantId);
    }

    @Override
    public boolean isExistAnyPublicScript(String tenantId) {
        return scriptManager.isExistAnyPublicScript(tenantId);
    }

    @Override
    public ScriptDTO getByScriptIdAndVersion(String tenantId, String scriptId, String version) {
        ScriptDTO script = scriptManager.getByScriptIdAndVersion(PUBLIC_APP_ID, scriptId, version);
        if (!checkScriptTenantMatch(script, tenantId)) {
            return null;
        }
        return script;
    }
}
