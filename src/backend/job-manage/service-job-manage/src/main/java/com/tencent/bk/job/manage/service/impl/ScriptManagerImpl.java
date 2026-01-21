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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.JobResourceTypeEnum;
import com.tencent.bk.job.common.exception.AlreadyExistsException;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.mysql.JobTransactional;
import com.tencent.bk.job.common.util.JobUUID;
import com.tencent.bk.job.manage.api.common.constants.JobResourceStatusEnum;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.auth.TemplateAuthService;
import com.tencent.bk.job.manage.dao.ScriptCitedTaskTemplateDAO;
import com.tencent.bk.job.manage.dao.ScriptDAO;
import com.tencent.bk.job.manage.dao.ScriptRelateJobTemplateDAO;
import com.tencent.bk.job.manage.dao.ScriptRelateTaskPlanDAO;
import com.tencent.bk.job.manage.dao.TaskScriptStepDAO;
import com.tencent.bk.job.manage.dao.template.TaskTemplateDAO;
import com.tencent.bk.job.manage.model.dto.ResourceTagDTO;
import com.tencent.bk.job.manage.model.dto.ScriptBasicDTO;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.dto.ScriptRelatedTaskPlanDTO;
import com.tencent.bk.job.manage.model.dto.ScriptSyncTemplateStepDTO;
import com.tencent.bk.job.manage.model.dto.SyncScriptResultDTO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.model.dto.TemplateStepIDDTO;
import com.tencent.bk.job.manage.model.dto.script.ScriptCitedTaskPlanDTO;
import com.tencent.bk.job.manage.model.dto.script.ScriptCitedTaskTemplateDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.query.ScriptQuery;
import com.tencent.bk.job.manage.model.web.vo.TagCountVO;
import com.tencent.bk.job.manage.service.ScriptManager;
import com.tencent.bk.job.manage.service.TagService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import com.tencent.bk.job.manage.service.template.impl.TemplateScriptStatusUpdateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.tencent.bk.job.common.constant.JobConstants.PUBLIC_APP_ID;

/**
 * 脚本管理通用实现
 */
@Slf4j
@Service
public class ScriptManagerImpl implements ScriptManager {

    private final ScriptDAO scriptDAO;
    private final TagService tagService;
    private final ScriptRelateTaskPlanDAO scriptRelateTaskPlanDAO;
    private final ScriptCitedTaskTemplateDAO scriptCitedTaskTemplateDAO;
    private final ScriptRelateJobTemplateDAO scriptRelateJobTemplateDAO;
    private final TaskScriptStepDAO taskScriptStepDAO;
    private final TaskTemplateDAO taskTemplateDAO;
    private final TemplateScriptStatusUpdateService templateScriptStatusUpdateService;
    private final TemplateAuthService templateAuthService;
    private final MessageI18nService i18nService;
    private TaskTemplateService taskTemplateService;

    @Autowired
    @Lazy
    public void setTaskTemplateService(TaskTemplateService taskTemplateService) {
        this.taskTemplateService = taskTemplateService;
    }

    @Autowired
    public ScriptManagerImpl(
        ScriptDAO scriptDAO,
        TagService tagService,
        ScriptRelateTaskPlanDAO scriptRelateTaskPlanDAO,
        ScriptCitedTaskTemplateDAO scriptCitedTaskTemplateDAO,
        ScriptRelateJobTemplateDAO scriptRelateJobTemplateDAO,
        @Qualifier("TaskTemplateScriptStepDAOImpl") TaskScriptStepDAO taskScriptStepDAO,
        TaskTemplateDAO taskTemplateDAO,
        TemplateScriptStatusUpdateService templateScriptStatusUpdateService,
        TemplateAuthService templateAuthService,
        MessageI18nService i18nService) {
        this.scriptDAO = scriptDAO;
        this.tagService = tagService;
        this.scriptRelateTaskPlanDAO = scriptRelateTaskPlanDAO;
        this.scriptCitedTaskTemplateDAO = scriptCitedTaskTemplateDAO;
        this.scriptRelateJobTemplateDAO = scriptRelateJobTemplateDAO;
        this.taskScriptStepDAO = taskScriptStepDAO;
        this.taskTemplateDAO = taskTemplateDAO;
        this.templateScriptStatusUpdateService = templateScriptStatusUpdateService;
        this.templateAuthService = templateAuthService;
        this.i18nService = i18nService;
    }

    @Override
    public ScriptDTO getScriptVersion(Long appId, Long scriptVersionId) {
        ScriptDTO scriptDTO = scriptDAO.getScriptVersionById(scriptVersionId);
        checkScriptInApp(appId, scriptDTO);
        return scriptDTO;
    }

    @Override
    public ScriptDTO getByScriptIdAndVersion(Long appId, String scriptId, String version) {
        ScriptDTO scriptDTO = scriptDAO.getByScriptIdAndVersion(appId, scriptId, version);
        checkScriptInApp(appId, scriptDTO);
        return scriptDTO;
    }

    @Override
    public ScriptDTO getScriptVersion(Long scriptVersionId) {
        return scriptDAO.getScriptVersionById(scriptVersionId);
    }

    @Override
    public ScriptDTO getScript(Long appId, String scriptId) {
        ScriptDTO script = scriptDAO.getScriptByScriptId(scriptId);
        checkScriptInApp(appId, script);
        setTags(script);
        return script;
    }

    @Override
    public ScriptDTO getScriptByScriptId(String scriptId) {
        ScriptDTO script = scriptDAO.getScriptByScriptId(scriptId);
        setTags(script);
        return script;
    }

    @Override
    public List<ScriptBasicDTO> listScriptBasicInfoByScriptIds(Collection<String> scriptIds) {
        return scriptDAO.listScriptBasicInfoByScriptIds(scriptIds);
    }

    @Override
    public List<ScriptDTO> listScriptVersion(Long appId, String scriptId) {
        List<ScriptDTO> scripts = scriptDAO.listScriptVersionsByScriptId(scriptId);
        if (scripts == null || scripts.isEmpty()) {
            return Collections.emptyList();
        }
        checkScriptInApp(appId, scripts.get(0));
        return scripts;
    }

    private void setTags(ScriptDTO script) {
        if (script == null) {
            return;
        }
        setTags(Collections.singletonList(script));
    }

    private void setTags(Collection<ScriptDTO> scripts) {
        if (CollectionUtils.isEmpty(scripts)) {
            return;
        }
        ScriptDTO someScript = scripts.stream().findAny().orElse(null);
        if (someScript == null) {
            return;
        }
        long appId = someScript.getAppId();
        boolean isPublicScript = someScript.isPublicScript();

        Integer resourceType = isPublicScript ? JobResourceTypeEnum.PUBLIC_SCRIPT.getValue() :
            JobResourceTypeEnum.APP_SCRIPT.getValue();

        List<String> scriptIds = scripts.stream().map(ScriptDTO::getId).collect(Collectors.toList());
        List<ResourceTagDTO> resourceTags = tagService.listResourceTagsByResourceTypeAndResourceIds(appId,
            resourceType, scriptIds);

        Map<String, List<ResourceTagDTO>> scriptTags = new HashMap<>();
        resourceTags.forEach(resourceTag -> scriptTags.compute(resourceTag.getResourceId(), (scriptId, tags) -> {
            if (tags == null) {
                tags = new ArrayList<>();
            }
            tags.add(resourceTag);
            return tags;
        }));

        scripts.forEach(script -> {
            List<ResourceTagDTO> tags = scriptTags.get(script.getId());
            if (CollectionUtils.isNotEmpty(tags)) {
                script.setTags(buildTags(tags));
            }
        });
    }

    private List<TagDTO> buildTags(List<ResourceTagDTO> resourceTags) {
        return resourceTags.stream().map(ResourceTagDTO::getTag).filter(Objects::nonNull)
            .sorted(Comparator.comparing(TagDTO::getName)).collect(Collectors.toList());
    }

    @Override
    public PageData<ScriptDTO> listPageScript(ScriptQuery scriptQuery) {
        if (scriptQuery.isExistTagCondition()) {
            List<String> tagMatchedScriptIds = queryScriptIdsByTags(scriptQuery);
            if (CollectionUtils.isEmpty(tagMatchedScriptIds)) {
                // none match, return empty page data
                return PageData.emptyPageData(scriptQuery.getBaseSearchCondition().getStart(),
                    scriptQuery.getBaseSearchCondition().getLength());
            } else {
                scriptQuery.setIds(tagMatchedScriptIds);
            }
        }
        PageData<ScriptDTO> pageData = scriptDAO.listPageScript(scriptQuery, scriptQuery.getBaseSearchCondition());
        //设置标签
        setTags(pageData.getData());
        return pageData;
    }

    private List<String> queryScriptIdsByTags(ScriptQuery query) {
        List<String> matchScriptIds = new ArrayList<>();
        Integer resourceType = query.isPublicScript() ? JobResourceTypeEnum.PUBLIC_SCRIPT.getValue() :
            JobResourceTypeEnum.APP_SCRIPT.getValue();
        if (query.isUntaggedScript()) {
            // untagged script
            List<String> taggedScriptIds = new ArrayList<>(tagService.listAppTaggedResourceIds(query.getAppId(),
                resourceType));
            matchScriptIds.addAll(scriptDAO.listAppScriptIds(query.getAppId()));
            matchScriptIds.removeAll(taggedScriptIds);
        } else if (CollectionUtils.isNotEmpty(query.getTagIds())) {
            matchScriptIds = tagService.listResourceIdsWithAllTagIds(resourceType, query.getTagIds());
        }
        return matchScriptIds;
    }

    @Override
    public List<ScriptDTO> listScripts(ScriptQuery scriptQuery) {
        List<ScriptDTO> scripts = scriptDAO.listScripts(scriptQuery);
        //设置标签
        setTags(scripts);
        return scripts;
    }

    @Override
    public ScriptDTO createScript(ScriptDTO script) {
        log.info("Begin to  create script: {}", script);
        long appId = script.getAppId();

        boolean isNameDuplicate = scriptDAO.isExistDuplicateName(script.getTenantId(), appId, script.getName());
        if (isNameDuplicate) {
            log.warn("The script name:{} is exist for app:{}", script.getName(), appId);
            throw new AlreadyExistsException(ErrorCode.SCRIPT_NAME_DUPLICATE);
        }

        script.setId(JobUUID.getUUID());
        // 默认为未上线状态
        script.setStatus(JobResourceStatusEnum.DRAFT.getValue());

        Long scriptVersionId;
        scriptDAO.saveScript(script);
        scriptVersionId = scriptDAO.saveScriptVersion(script);
        script.setScriptVersionId(scriptVersionId);

        saveScriptTags(appId, script);

        return getScriptVersion(scriptVersionId);
    }

    private void saveScriptTags(Long appId, ScriptDTO script) {
        saveScriptTags(script.getLastModifyUser(), appId, script.getId(), script.getTags());
    }

    private void saveScriptTags(String operator, Long appId, String scriptId, List<TagDTO> tags) {
        List<TagDTO> newTags = tags;
        if (tags != null && !tags.isEmpty()) {
            newTags = tagService.createNewTagIfNotExist(tags, appId, operator);
        }

        Integer resourceType = appId == (PUBLIC_APP_ID) ?
            JobResourceTypeEnum.PUBLIC_SCRIPT.getValue() :
            JobResourceTypeEnum.APP_SCRIPT.getValue();

        tagService.patchResourceTags(resourceType, scriptId, CollectionUtils.isEmpty(newTags) ?
            Collections.emptyList() : newTags.stream().map(TagDTO::getId).collect(Collectors.toList()));
    }

    @Override
    public ScriptDTO createScriptVersion(ScriptDTO scriptVersion) {
        log.info("Begin to save scriptVersion: {}", scriptVersion);

        if (scriptDAO.isExistDuplicateVersion(scriptVersion.getId(), scriptVersion.getVersion())) {
            log.warn("Script version:{} is exist, scriptId:{}", scriptVersion.getVersion(), scriptVersion.getId());
            throw new AlreadyExistsException(ErrorCode.SCRIPT_VERSION_NAME_EXIST);
        }

        // 默认为未上线状态
        scriptVersion.setStatus(JobResourceStatusEnum.DRAFT.getValue());

        Long scriptVersionId = scriptDAO.saveScriptVersion(scriptVersion);
        scriptVersion.setScriptVersionId(scriptVersionId);
        scriptDAO.updateScriptLastModify(scriptVersion.getId(), scriptVersion.getCreator(), System.currentTimeMillis());

        return getScriptVersion(scriptVersionId);
    }

    @Override
    public ScriptDTO updateScriptVersion(ScriptDTO scriptVersion) {
        log.info("Begin to update scriptVersion: {}", scriptVersion);

        ScriptDTO scriptVersionToBeUpdate = scriptDAO.getScriptVersionById(scriptVersion.getScriptVersionId());
        if (scriptVersionToBeUpdate == null) {
            throw new NotFoundException(ErrorCode.SCRIPT_NOT_EXIST);
        }
        if (!scriptVersionToBeUpdate.getStatus().equals(JobResourceStatusEnum.DRAFT.getValue())) {
            log.warn("Script status is not draft, can not update.scriptVersionId={}",
                scriptVersion.getScriptVersionId());
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }

        scriptDAO.updateScriptVersion(scriptVersion.getLastModifyUser(), scriptVersion.getScriptVersionId(),
            scriptVersion);

        return scriptDAO.getScriptVersionById(scriptVersion.getScriptVersionId());
    }

    @Override
    @JobTransactional(transactionManager = "jobManageTransactionManager")
    public void deleteScriptVersion(Long appId, Long scriptVersionId) {
        ScriptDTO existScript = scriptDAO.getScriptVersionById(scriptVersionId);
        checkDeleteScriptPermission(appId, existScript);
        checkScriptReferenced(existScript);
        List<ScriptDTO> scriptVersions = scriptDAO.listScriptVersionsByScriptId(existScript.getId());
        if (scriptVersions.size() == 1) {
            scriptDAO.deleteScript(existScript.getId());
            scriptDAO.deleteScriptVersion(scriptVersionId);
            deleteScriptRelatedTags(appId, existScript.getId());
        } else {
            scriptDAO.deleteScriptVersion(scriptVersionId);
        }
    }

    private void checkScriptReferenced(ScriptDTO script) {
        if (isScriptReferenced(script.getId(), script.getScriptVersionId())) {
            throw new FailedPreconditionException(ErrorCode.DELETE_REF_SCRIPT_FAIL);
        }
    }

    @Override
    @JobTransactional(transactionManager = "jobManageTransactionManager")
    public void deleteScript(Long appId, String scriptId) {
        ScriptDTO existScript = getScript(appId, scriptId);
        checkDeleteScriptPermission(appId, existScript);
        checkScriptReferenced(existScript);
        scriptDAO.deleteScript(scriptId);
        scriptDAO.deleteScriptVersionByScriptId(scriptId);
        deleteScriptRelatedTags(appId, scriptId);
    }

    private void deleteScriptRelatedTags(Long appId, String scriptId) {
        Integer resourceType = (appId == PUBLIC_APP_ID) ?
            JobResourceTypeEnum.PUBLIC_SCRIPT.getValue() :
            JobResourceTypeEnum.APP_SCRIPT.getValue();
        tagService.batchDeleteResourceTags(appId, resourceType, scriptId);
    }

    private void checkDeleteScriptPermission(
        Long appId,
        ScriptDTO existScript
    ) {
        if (existScript == null) {
            throw new NotFoundException(ErrorCode.SCRIPT_NOT_EXIST);
        }
        long targetAppId = appId;
        if (existScript.isPublicScript()) {
            targetAppId = PUBLIC_APP_ID;
        }
        if (!existScript.getAppId().equals(targetAppId)) {
            throw new NotFoundException(ErrorCode.SCRIPT_NOT_IN_APP);
        }
    }

    @Override
    public void publishScript(Long appId,
                              String scriptId,
                              Long scriptVersionId) {
        log.info("Begin to publish script, appId={}, scriptId={}, scriptVersionId={}", appId, scriptId,
            scriptVersionId);
        List<ScriptDTO> scriptVersions = scriptDAO.listScriptVersionsByScriptId(scriptId);
        if (scriptVersions == null || scriptVersions.isEmpty()) {
            log.warn("Publish script, script:{} is not exist", scriptId);
            throw new NotFoundException(ErrorCode.SCRIPT_NOT_EXIST);
        }

        boolean isPublicScript = scriptVersions.get(0).isPublicScript();
        long targetAppId = appId;
        if (isPublicScript) {
            targetAppId = PUBLIC_APP_ID;
        }

        boolean isScriptVersionInCurrentScript = false;
        ScriptDTO scriptVersionToBePublished = null;
        for (ScriptDTO scriptVersion : scriptVersions) {
            if (!isPublicScript && !scriptVersion.getAppId().equals(targetAppId)) {
                log.warn("Publish script, script:{} is not in current app:{}", scriptId, targetAppId);
                throw new NotFoundException(ErrorCode.SCRIPT_NOT_IN_APP);
            }
            if (scriptVersion.getScriptVersionId().equals(scriptVersionId)) {
                isScriptVersionInCurrentScript = true;
                scriptVersionToBePublished = scriptVersion;
            }
        }
        if (!isScriptVersionInCurrentScript) {
            log.warn("Public script, scriptVersion:{} is not in script:{}", scriptVersionId, scriptId);
            throw new NotFoundException(ErrorCode.SCRIPT_NOT_EXIST);
        }

        if (scriptVersionToBePublished.getStatus().equals(JobResourceStatusEnum.DISABLED.getValue())) {
            log.warn("Publish script, scriptVersion:{}, status:{} could not publish", scriptVersionId,
                scriptVersionToBePublished.getStatus());
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }

        ScriptDTO publishedScriptVersion = null;
        for (ScriptDTO scriptVersion : scriptVersions) {
            if (scriptVersion.getStatus().equals(JobResourceStatusEnum.ONLINE.getValue())) {
                publishedScriptVersion = scriptVersion;
                break;
            }
        }
        if (publishedScriptVersion != null) {
            log.info("Publish script, set scriptVersion:{} offline", publishedScriptVersion.getScriptVersionId());
            scriptDAO.updateScriptVersionStatus(publishedScriptVersion.getScriptVersionId(),
                JobResourceStatusEnum.OFFLINE.getValue());
            templateScriptStatusUpdateService.refreshTemplateScriptStatusByScript(scriptId,
                publishedScriptVersion.getScriptVersionId());
        }
        scriptDAO.updateScriptVersionStatus(scriptVersionId, JobResourceStatusEnum.ONLINE.getValue());
        templateScriptStatusUpdateService.refreshTemplateScriptStatusByScript(scriptId, scriptVersionId);
        log.info("Publish script successfully, scriptId={}, scriptVersionId={}", scriptId, scriptVersionId);
    }

    @Override
    public void disableScript(Long appId,
                              String scriptId,
                              Long scriptVersionId) {
        log.info("Begin to disable script, appId={}, scriptId={}, scriptVersionId={}", appId,
            scriptId, scriptVersionId);
        List<ScriptDTO> scriptVersions = scriptDAO.listScriptVersionsByScriptId(scriptId);
        if (scriptVersions == null || scriptVersions.isEmpty()) {
            log.warn("Disable script, script:{} is not exist", scriptId);
            throw new NotFoundException(ErrorCode.SCRIPT_NOT_EXIST);
        }

        boolean isPublicScript = scriptVersions.get(0).isPublicScript();
        long targetAppId = appId;
        if (isPublicScript) {
            targetAppId = PUBLIC_APP_ID;
        }

        boolean isScriptVersionInCurrentScript = false;
        ScriptDTO scriptVersionToBeDisabled = null;
        for (ScriptDTO scriptVersion : scriptVersions) {
            if (!scriptVersion.getAppId().equals(targetAppId)) {
                log.warn("Disable script, script:{} is not in current app:{}", scriptId, targetAppId);
                throw new NotFoundException(ErrorCode.SCRIPT_NOT_IN_APP);
            }
            if (scriptVersion.getScriptVersionId().equals(scriptVersionId)) {
                isScriptVersionInCurrentScript = true;
                scriptVersionToBeDisabled = scriptVersion;
            }
        }
        if (!isScriptVersionInCurrentScript) {
            log.warn("Disable script, scriptVersion:{} is not in script:{}", scriptVersionId, scriptId);
            throw new NotFoundException(ErrorCode.SCRIPT_NOT_EXIST);
        }

        if (!scriptVersionToBeDisabled.getStatus().equals(JobResourceStatusEnum.ONLINE.getValue())
            && !scriptVersionToBeDisabled.getStatus().equals(JobResourceStatusEnum.OFFLINE.getValue())) {
            log.warn("Disable script, scriptVersion:{}, status:{} could not disable", scriptVersionId,
                scriptVersionToBeDisabled.getStatus());
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }

        scriptDAO.updateScriptVersionStatus(scriptVersionToBeDisabled.getScriptVersionId(),
            JobResourceStatusEnum.DISABLED.getValue());
        templateScriptStatusUpdateService.refreshTemplateScriptStatusByScript(scriptId,
            scriptVersionToBeDisabled.getScriptVersionId());
        log.info("Disable script successfully, scriptId={}, scriptVersionId={}", scriptId, scriptVersionId);
    }

    @Override
    public Map<String, ScriptDTO> batchGetOnlineScriptVersionByScriptIds(List<String> scriptIdList) {
        Map<String, ScriptDTO> scripts = scriptDAO.batchGetOnlineByScriptIds(scriptIdList);
        setTags(scripts.values());
        return scripts;
    }

    @Override
    public ScriptDTO updateScriptDesc(String operator, Long appId, String scriptId, String desc) {
        log.info("Begin to update script desc,appId={},operator={},scriptId={},desc={}", appId, operator, scriptId,
            desc);
        ScriptDTO script = scriptDAO.getScriptByScriptId(scriptId);
        checkScriptInApp(appId, script);
        scriptDAO.updateScriptDesc(operator, scriptId, desc);
        script.setDescription(desc);

        return script;
    }

    private void checkScriptInApp(long appId, ScriptDTO script) {
        if (script == null) {
            throw new NotFoundException(ErrorCode.SCRIPT_NOT_EXIST);
        }
        if (!script.isPublicScript()) {
            if (!script.getAppId().equals(appId)) {
                log.warn("Script:{} is not in current app:{}", script.getId(), appId);
                throw new NotFoundException(ErrorCode.SCRIPT_NOT_IN_APP);
            }
        }
    }

    @Override
    public ScriptDTO updateScriptName(User operator, Long appId, String scriptId, String newName) {
        log.info("Begin to update script name,appId={},operator={},scriptId={},desc={}", appId, operator, scriptId,
            newName);
        ScriptDTO script = scriptDAO.getScriptByScriptId(scriptId);
        checkScriptInApp(appId, script);
        if (script.getName().equals(newName)) {
            return script;
        }
        long targetAppId = appId;
        if (script.isPublicScript()) {
            targetAppId = PUBLIC_APP_ID;
        }
        boolean isNameExist = scriptDAO.isExistDuplicateName(operator.getTenantId(), targetAppId, newName);
        if (isNameExist) {
            log.warn("Update script name, script:{} new name {} is duplicate", scriptId, newName);
            throw new AlreadyExistsException(ErrorCode.SCRIPT_NAME_DUPLICATE);
        }

        scriptDAO.updateScriptName(operator.getUsername(), scriptId, newName);
        script.setName(newName);

        return script;
    }

    @Override
    public ScriptDTO updateScriptTags(String operator, Long appId, String scriptId, List<TagDTO> tags) {
        log.info("Begin to update script tags,appId={},operator={},scriptId={},tags={}", appId, operator, scriptId,
            tags);
        ScriptDTO script = scriptDAO.getScriptByScriptId(scriptId);
        checkScriptInApp(appId, script);
        long targetAppId = appId;
        if (script.isPublicScript()) {
            targetAppId = PUBLIC_APP_ID;
        }
        saveScriptTags(operator, targetAppId, scriptId, tags);

        script.setTags(tags);
        return script;
    }

    @Override
    public List<String> listScriptNames(Long appId, String keyword) {
        return scriptDAO.listScriptNames(appId, keyword);
    }

    @Override
    public List<String> listPublicScriptNames(String tenantId, String keyword) {
        return scriptDAO.listPublicScriptNames(tenantId, keyword);
    }

    @Override
    public List<ScriptDTO> listOnlineScriptForApp(long appId) {
        List<ScriptDTO> scripts = scriptDAO.listOnlineScriptForApp(appId);
        setTags(scripts);
        return scripts;
    }

    @Override
    public List<ScriptDTO> listOnlinePublicScript(String tenantId) {
        List<ScriptDTO> scripts = scriptDAO.listOnlinePublicScript(tenantId);
        setTags(scripts);
        return scripts;
    }

    @Override
    public ScriptDTO getOnlineScriptVersionByScriptId(long appId, String scriptId) {
        return scriptDAO.getOnlineScriptVersionByScriptId(appId, scriptId);
    }

    @Override
    public ScriptDTO getOnlineScriptVersionByScriptId(String scriptId) {
        return scriptDAO.getOnlineScriptVersionByScriptId(scriptId);
    }

    @Override
    public PageData<ScriptDTO> listPageScriptVersion(ScriptQuery scriptQuery) {
        return scriptDAO.listPageScriptVersion(scriptQuery);
    }

    @Override
    public List<ScriptSyncTemplateStepDTO> listScriptSyncTemplateSteps(Long appId, String scriptId) {
        // 检查脚本
        ScriptDTO syncScript = scriptDAO.getScriptByScriptId(scriptId);
        checkScriptInApp(appId, syncScript);

        List<ScriptSyncTemplateStepDTO> templateSteps =
            scriptRelateJobTemplateDAO.listScriptRelatedJobTemplateSteps(scriptId);
        if (CollectionUtils.isEmpty(templateSteps)) {
            return templateSteps;
        }
        // 按业务过滤
        if (appId > 0) {
            templateSteps =
                templateSteps.stream().filter(step -> step.getAppId().equals(appId)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(templateSteps)) {
                return templateSteps;
            }
        }

        Set<Long> scriptVersionIds =
            templateSteps.stream().map(ScriptSyncTemplateStepDTO::getScriptVersionId).collect(Collectors.toSet());
        List<ScriptDTO> scriptVersions = scriptDAO.batchGetScriptVersionsByIds(scriptVersionIds);
        if (CollectionUtils.isEmpty(scriptVersions)) {
            log.warn("Scripts are not exist, scriptVersionIds: {}", scriptVersionIds);
            return Collections.emptyList();
        }

        Map<Long, ScriptDTO> scripts = new HashMap<>();
        scriptVersions.forEach(scriptVersion -> scripts.put(scriptVersion.getScriptVersionId(), scriptVersion));
        templateSteps.forEach(step -> fillScriptInfo(step, scripts));
        return templateSteps;
    }

    private void fillScriptInfo(ScriptSyncTemplateStepDTO templateStep, Map<Long, ScriptDTO> scripts) {
        ScriptDTO script = scripts.get(templateStep.getScriptVersionId());
        if (script != null) {
            templateStep.setScriptName(script.getName());
            templateStep.setScriptVersion(script.getVersion());
            templateStep.setScriptStatus(script.getStatus());
        }
    }

    @Override
    public List<SyncScriptResultDTO> syncScriptToTaskTemplate(User user,
                                                              Long appId,
                                                              String scriptId,
                                                              Long syncScriptVersionId,
                                                              List<TemplateStepIDDTO> templateStepIDs)
        throws PermissionDeniedException {
        if (CollectionUtils.isEmpty(templateStepIDs)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }

        ScriptDTO syncScript = scriptDAO.getScriptVersionById(syncScriptVersionId);
        checkScriptInApp(appId, syncScript);
        if (!scriptId.equals(syncScript.getId())) {
            log.warn("Script ID and scriptVersionId not match!");
            throw new NotFoundException(ErrorCode.SCRIPT_NOT_EXIST);
        }
        // 鉴权
        List<Long> authTemplateIds = new ArrayList<>();
        templateStepIDs.forEach(step -> {
            TaskTemplateInfoDTO taskTemplate = taskTemplateService.getTaskTemplateBasicInfoById(step.getTemplateId());
            if (appId > 0 && taskTemplate != null && !taskTemplate.getAppId().equals(appId)) {
                throw new NotFoundException(ErrorCode.TEMPLATE_NOT_EXIST);
            }
            if (taskTemplate != null) {
                step.setAppId(taskTemplate.getAppId());
                if (!authTemplateIds.contains(step.getTemplateId())) {
                    authTemplateIds.add(step.getTemplateId());
                }
            }
        });
        AuthResult authResult = templateAuthService.batchAuthResultEditJobTemplate(
            user,
            new AppResourceScope(appId),
            authTemplateIds
        );
        if (!authResult.isPass()) {
            log.info("Sync script to template auth fail, scriptId: {}, syncScriptVersionId: {}, template steps: {}",
                scriptId, syncScriptVersionId, templateStepIDs);
            throw new PermissionDeniedException(authResult);
        }

        List<SyncScriptResultDTO> syncResults = new ArrayList<>();
        templateStepIDs.forEach(templateStep -> {
            Long templateId = templateStep.getTemplateId();
            Long stepId = templateStep.getStepId();
            ScriptSyncTemplateStepDTO updateStep = new ScriptSyncTemplateStepDTO();
            updateStep.setTemplateId(templateId);
            updateStep.setStepId(stepId);
            try {
                boolean success = updateTemplateRefScript(appId, templateId, stepId, syncScriptVersionId);
                if (success) {
                    syncResults.add(SyncScriptResultDTO.buildSuccessSyncResult(updateStep));
                    templateScriptStatusUpdateService.refreshTemplateScriptStatusByTemplate(templateId);
                } else {
                    syncResults.add(SyncScriptResultDTO.buildFailSyncResult(updateStep, ErrorCode.TEMPLATE_NOT_EXIST));
                }
            } catch (Throwable e) {
                log.warn("Update script step ref script fail, templateId: {}, stepId: {}, scriptVersionId: {}",
                    templateId, stepId, syncScriptVersionId);
                syncResults.add(SyncScriptResultDTO.buildFailSyncResult(updateStep, ErrorCode.INTERNAL_ERROR));
            }
        });

        List<ScriptSyncTemplateStepDTO> scriptRelatedTemplateSteps = listScriptSyncTemplateSteps(appId, scriptId);
        Map<Long, ScriptSyncTemplateStepDTO> stepMap = new HashMap<>();
        scriptRelatedTemplateSteps.forEach(step -> stepMap.put(step.getStepId(), step));
        fillSyncResultDetail(syncScriptVersionId, syncResults, stepMap);

        return syncResults;
    }

    @Override
    public Integer getScriptTemplateCiteCount(String scriptId, Long scriptVersionId) {
        if (scriptVersionId == null) {
            return scriptCitedTaskTemplateDAO.countScriptCitedTaskTemplate(scriptId);
        } else {
            return scriptCitedTaskTemplateDAO.countScriptVersionCitedTaskTemplate(scriptId, scriptVersionId);
        }
    }

    @Override
    public Integer getScriptTaskPlanCiteCount(String scriptId, Long scriptVersionId) {
        if (scriptVersionId == null) {
            return scriptRelateTaskPlanDAO.countScriptRelatedTaskPlan(scriptId);
        } else {
            return scriptRelateTaskPlanDAO.countScriptVersionRelatedTaskPlan(scriptId, scriptVersionId);
        }
    }

    @Override
    public List<ScriptCitedTaskTemplateDTO> getScriptCitedTemplates(String scriptId,
                                                                    Long scriptVersionId) {
        List<ScriptCitedTaskTemplateDTO> scriptCitedTaskTemplateDTOList;
        if (scriptVersionId == null) {
            scriptCitedTaskTemplateDTOList = scriptCitedTaskTemplateDAO.listScriptCitedTaskTemplate(scriptId);
        } else {
            scriptCitedTaskTemplateDTOList = scriptCitedTaskTemplateDAO.listScriptVersionCitedTaskTemplate(scriptId,
                scriptVersionId);
        }
        //填充scriptStatusDesc
        scriptCitedTaskTemplateDTOList.forEach(scriptCitedTaskTemplateDTO ->
            scriptCitedTaskTemplateDTO.setScriptStatusDesc(i18nService.getI18n(
                    scriptCitedTaskTemplateDTO.getScriptStatus().getStatusI18nKey()
                )
            ));
        return scriptCitedTaskTemplateDTOList;
    }

    @Override
    public List<ScriptCitedTaskPlanDTO> getScriptCitedTaskPlans(String scriptId,
                                                                Long scriptVersionId) {
        List<ScriptRelatedTaskPlanDTO> scriptRelatedTaskPlanDTOList;
        if (scriptVersionId == null) {
            scriptRelatedTaskPlanDTOList = scriptRelateTaskPlanDAO.listScriptRelatedTaskPlan(scriptId);
        } else {
            scriptRelatedTaskPlanDTOList = scriptRelateTaskPlanDAO.listScriptVersionRelatedTaskPlan(scriptId,
                scriptVersionId);
        }
        List<ScriptCitedTaskPlanDTO> scriptCitedTaskPlanDTOList = new ArrayList<>();
        if (scriptRelatedTaskPlanDTOList != null) {
            scriptRelatedTaskPlanDTOList.forEach(scriptRelatedTaskPlanDTO -> {
                ScriptCitedTaskPlanDTO scriptCitedTaskPlanDTO = new ScriptCitedTaskPlanDTO();
                scriptCitedTaskPlanDTO.setAppId(scriptRelatedTaskPlanDTO.getAppId());
                scriptCitedTaskPlanDTO.setScriptStatus(scriptRelatedTaskPlanDTO.getScriptStatus().getValue());
                scriptCitedTaskPlanDTO.setScriptVersion(scriptRelatedTaskPlanDTO.getScriptVersion());
                scriptCitedTaskPlanDTO.setScriptStatusDesc(
                    i18nService.getI18n(
                        scriptRelatedTaskPlanDTO.getScriptStatus().getStatusI18nKey()
                    )
                );
                scriptCitedTaskPlanDTO.setTaskTemplateId(scriptRelatedTaskPlanDTO.getTemplateId());
                scriptCitedTaskPlanDTO.setTaskPlanId(scriptRelatedTaskPlanDTO.getTaskId());
                scriptCitedTaskPlanDTO.setTaskPlanName(scriptRelatedTaskPlanDTO.getTaskName());
                scriptCitedTaskPlanDTOList.add(scriptCitedTaskPlanDTO);
            });
        }
        return scriptCitedTaskPlanDTOList;
    }

    private void fillSyncResultDetail(Long syncScriptVersionId, List<SyncScriptResultDTO> syncResults,
                                      Map<Long, ScriptSyncTemplateStepDTO> stepMap) {
        for (SyncScriptResultDTO syncResult : syncResults) {
            ScriptSyncTemplateStepDTO step = syncResult.getTemplateStep();
            ScriptSyncTemplateStepDTO updatedStep = stepMap.get(step.getStepId());
            if (updatedStep == null) {
                log.warn("Step is not exist, stepId: {}", step.getStepId());
                syncResult.setErrorCode(ErrorCode.TEMPLATE_NOT_EXIST);
                continue;
            }
            if (!updatedStep.getScriptVersionId().equals(syncScriptVersionId)
                && syncResult.getErrorCode() == null) {
                syncResult.setErrorCode(ErrorCode.SYNC_SCRIPT_UNKNOWN_ERROR);
            }
            step.setTemplateName(updatedStep.getTemplateName());
            step.setStepName(updatedStep.getStepName());
            step.setScriptStatus(updatedStep.getScriptStatus());
            step.setScriptVersion(updatedStep.getScriptVersion());
            step.setScriptVersionId(updatedStep.getScriptVersionId());
            step.setScriptId(updatedStep.getScriptId());
            step.setScriptName(updatedStep.getScriptName());
        }
    }

    @JobTransactional(transactionManager = "jobManageTransactionManager")
    public boolean updateTemplateRefScript(long appId, long templateId, long stepId, long syncScriptVersionId) {
        boolean success = taskScriptStepDAO.updateScriptStepRefScriptVersionId(
            templateId,
            stepId,
            syncScriptVersionId
        );
        taskTemplateDAO.updateTaskTemplateVersion(appId, templateId, UUID.randomUUID().toString());
        return success;
    }

    @Override
    public Integer countScripts(Long appId, ScriptTypeEnum scriptTypeEnum,
                                JobResourceStatusEnum jobResourceStatusEnum) {
        return scriptDAO.countScripts(appId, scriptTypeEnum, jobResourceStatusEnum);
    }

    @Override
    public Integer countScriptVersions(Long appId, ScriptTypeEnum scriptTypeEnum,
                                       JobResourceStatusEnum jobResourceStatusEnum) {
        return scriptDAO.countScriptVersions(appId, scriptTypeEnum, jobResourceStatusEnum);
    }

    @Override
    public List<String> listScriptIds(Long appId) {
        return scriptDAO.listAppScriptIds(appId);
    }

    @Override
    public List<String> listPublicScriptIds(String tenantId) {
        return scriptDAO.listPublicScriptIds(tenantId);
    }

    @Override
    public Integer countCiteScripts(Long appId) {
        // 1.查询业务下所有脚本
        List<String> scriptIdList = scriptDAO.listAppScriptIds(appId);
        // 2.查询被引用的脚本数量
        return taskScriptStepDAO.countScriptCitedByStepsByScriptIds(appId, scriptIdList);
    }

    @Override
    public TagCountVO getTagScriptCount(Long appId) {
        TagCountVO tagCount = new TagCountVO();

        List<String> appScriptIds = scriptDAO.listAppScriptIds(appId)
            .stream().map(String::valueOf).collect(Collectors.toList());
        tagCount.setTotal((long) appScriptIds.size());

        Integer resourceType = PUBLIC_APP_ID == appId ? JobResourceTypeEnum.PUBLIC_SCRIPT.getValue() :
            JobResourceTypeEnum.APP_SCRIPT.getValue();
        List<ResourceTagDTO> tags = tagService.listResourceTagsByResourceTypeAndResourceIds(appId,
            resourceType, appScriptIds);
        Map<Long, Long> scriptTagCount = tagService.countResourcesByTag(tags);
        tagCount.setTagCount(scriptTagCount);

        long taggedScriptCount = tags.stream()
            .map(ResourceTagDTO::getResourceId).distinct().count();
        tagCount.setUnclassified(appScriptIds.size() - taggedScriptCount);
        return tagCount;
    }

    @Override
    public TagCountVO getTagPublicScriptCount(String tenantId) {
        TagCountVO tagCount = new TagCountVO();

        List<String> scriptIds = scriptDAO.listPublicScriptIds(tenantId)
            .stream().map(String::valueOf).collect(Collectors.toList());
        tagCount.setTotal((long) scriptIds.size());

        List<ResourceTagDTO> tags = tagService.listResourceTagsByResourceTypeAndResourceIds(PUBLIC_APP_ID,
            JobResourceTypeEnum.PUBLIC_SCRIPT.getValue(), scriptIds);
        Map<Long, Long> scriptTagCount = tagService.countResourcesByTag(tags);
        tagCount.setTagCount(scriptTagCount);

        long taggedScriptCount = tags.stream()
            .map(ResourceTagDTO::getResourceId).distinct().count();
        tagCount.setUnclassified(scriptIds.size() - taggedScriptCount);
        return tagCount;
    }

    @Override
    public boolean isExistAnyScript(Long appId) {
        return scriptDAO.isExistAnyScript(appId);
    }

    @Override
    public boolean isExistAnyPublicScript(String tenantId) {
        return scriptDAO.isExistAnyPublicScript(tenantId);
    }

    @Override
    public boolean isScriptReferenced(String scriptId, Long scriptVersionId) {
        int citeCount = getScriptTemplateCiteCount(scriptId, scriptVersionId);
        if (citeCount == 0) {
            citeCount = getScriptTaskPlanCiteCount(scriptId, scriptVersionId);
        }
        if (citeCount > 0 && scriptVersionId != null) {
            ScriptDTO scriptVersion = getScriptVersion(scriptVersionId);
            return scriptVersion != null && scriptVersion.getStatus().equals(JobResourceStatusEnum.ONLINE.getValue());
        }
        return citeCount > 0;
    }
}
