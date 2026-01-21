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

import com.tencent.bk.job.common.constant.JobResourceTypeEnum;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.manage.api.common.ScriptDTOBuilder;
import com.tencent.bk.job.manage.api.common.constants.JobResourceStatusEnum;
import com.tencent.bk.job.manage.model.dto.ResourceTagDTO;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.dto.ScriptSyncTemplateStepDTO;
import com.tencent.bk.job.manage.model.dto.SyncScriptResultDTO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.model.dto.converter.ScriptConverter;
import com.tencent.bk.job.manage.model.dto.converter.ScriptRelatedTemplateStepConverter;
import com.tencent.bk.job.manage.model.dto.script.ScriptCitedTaskPlanDTO;
import com.tencent.bk.job.manage.model.dto.script.ScriptCitedTaskTemplateDTO;
import com.tencent.bk.job.manage.model.query.ScriptQuery;
import com.tencent.bk.job.manage.model.web.request.ScriptInfoUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.BasicScriptVO;
import com.tencent.bk.job.manage.model.web.vo.ScriptCitedTaskPlanVO;
import com.tencent.bk.job.manage.model.web.vo.ScriptCitedTemplateVO;
import com.tencent.bk.job.manage.model.web.vo.ScriptVO;
import com.tencent.bk.job.manage.model.web.vo.TagVO;
import com.tencent.bk.job.manage.model.web.vo.script.ScriptCiteCountVO;
import com.tencent.bk.job.manage.model.web.vo.script.ScriptCiteInfoVO;
import com.tencent.bk.job.manage.model.web.vo.script.ScriptRelatedTemplateStepVO;
import com.tencent.bk.job.manage.model.web.vo.script.ScriptSyncResultVO;
import com.tencent.bk.job.manage.service.ScriptManager;
import com.tencent.bk.job.manage.service.TagService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.tencent.bk.job.common.constant.JobConstants.PUBLIC_APP_ID;

/**
 * 脚本Resource通用实现
 */
public class BaseWebScriptResource {

    protected final MessageI18nService i18nService;

    protected final ScriptDTOBuilder scriptDTOBuilder;

    protected final TagService tagService;

    private final ScriptManager scriptManager;

    public BaseWebScriptResource(MessageI18nService i18nService,
                                 ScriptDTOBuilder scriptDTOBuilder,
                                 TagService tagService,
                                 ScriptManager scriptManager) {
        this.i18nService = i18nService;
        this.scriptDTOBuilder = scriptDTOBuilder;
        this.tagService = tagService;
        this.scriptManager = scriptManager;
    }

    public ScriptQuery buildListPageScriptQuery(AppResourceScope appResourceScope,
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
        boolean queryPublic = appResourceScope == null || appResourceScope.getAppId() == null
            || appResourceScope.getAppId().equals(PUBLIC_APP_ID);
        scriptQuery.setPublicScript(queryPublic);
        scriptQuery.setAppId(queryPublic ? PUBLIC_APP_ID : appResourceScope.getAppId());
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

        scriptQuery.setBaseSearchCondition(baseSearchCondition);

        return scriptQuery;
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

    protected PageData<ScriptVO> pageVOs(PageData<ScriptDTO> pageData,
                                         Integer start,
                                         Integer pageSize) {
        List<ScriptVO> resultScripts = new ArrayList<>();
        if (pageData == null) {
            PageData<ScriptVO> resultPageData = new PageData<>();
            resultPageData.setStart(start);
            resultPageData.setPageSize(pageSize);
            resultPageData.setTotal(0L);
            resultPageData.setData(resultScripts);
            return resultPageData;
        }

        for (ScriptDTO scriptDTO : pageData.getData()) {
            ScriptVO scriptVO = ScriptConverter.convertToScriptVO(scriptDTO);
            resultScripts.add(scriptVO);
        }

        PageData<ScriptVO> resultPageData = new PageData<>();
        resultPageData.setStart(pageData.getStart());
        resultPageData.setPageSize(pageData.getPageSize());
        resultPageData.setTotal(pageData.getTotal());
        resultPageData.setData(resultScripts);

        return resultPageData;
    }

    protected void setScriptCiteCount(List<ScriptVO> scriptVOS) {
        for (ScriptVO scriptVO : scriptVOS) {
            String scriptId = scriptVO.getId();
            Long scriptVersionId = scriptVO.getScriptVersionId();
            Integer taskTemplateCiteCount = scriptManager.getScriptTemplateCiteCount(
                scriptId, scriptVersionId);
            scriptVO.setRelatedTaskTemplateNum(taskTemplateCiteCount);
            Integer taskPlanCiteCount = scriptManager.getScriptTaskPlanCiteCount(
                scriptId, scriptVersionId);
            scriptVO.setRelatedTaskPlanNum(taskPlanCiteCount);
        }
    }

    protected void setOnlineScriptVersionInfo(List<ScriptVO> scripts) {
        if (scripts != null && !scripts.isEmpty()) {
            List<String> scriptIdList = new ArrayList<>();
            for (ScriptVO script : scripts) {
                scriptIdList.add(script.getId());
            }
            Map<String, ScriptDTO> onlineScriptMap = scriptManager.batchGetOnlineScriptVersionByScriptIds(scriptIdList);

            for (ScriptVO scriptVO : scripts) {
                ScriptDTO onlineScriptVersion = onlineScriptMap.get(scriptVO.getId());
                if (onlineScriptVersion != null) {
                    scriptVO.setScriptVersionId(onlineScriptVersion.getScriptVersionId());
                    scriptVO.setVersion(onlineScriptVersion.getVersion());
                }
            }
        }
    }

    protected List<TagDTO> extractTags(ScriptInfoUpdateReq scriptInfoUpdateReq) {
        List<TagDTO> tags = new ArrayList<>();
        if (scriptInfoUpdateReq.getScriptTags() != null && !scriptInfoUpdateReq.getScriptTags().isEmpty()) {
            for (TagVO tagVO : scriptInfoUpdateReq.getScriptTags()) {
                TagDTO tagDTO = new TagDTO();
                tagDTO.setId(tagVO.getId());
                tagDTO.setName(tagVO.getName());
                tags.add(tagDTO);
            }
        }
        return tags;
    }

    protected ScriptCiteCountVO getScriptCiteCountOfAllScript(String scriptId, Long scriptVersionId) {
        Integer templateCiteCount = scriptManager.getScriptTemplateCiteCount(scriptId, scriptVersionId);
        Integer taskPlanCiteCount = scriptManager.getScriptTaskPlanCiteCount(scriptId, scriptVersionId);
        return new ScriptCiteCountVO(templateCiteCount, taskPlanCiteCount);
    }

    protected ScriptCiteInfoVO getScriptCiteInfoOfAllScript(String scriptId, Long scriptVersionId) {
        List<ScriptCitedTaskTemplateDTO> citedTemplateList = scriptManager.getScriptCitedTemplates(
            scriptId, scriptVersionId);
        if (citedTemplateList == null) {
            citedTemplateList = Collections.emptyList();
        }
        List<ScriptCitedTemplateVO> citedTemplateVOList =
            citedTemplateList.parallelStream().map(ScriptCitedTaskTemplateDTO::toVO).collect(Collectors.toList());
        List<ScriptCitedTaskPlanDTO> citedTaskPlanList = scriptManager.getScriptCitedTaskPlans(
            scriptId, scriptVersionId);
        if (citedTaskPlanList == null) {
            citedTaskPlanList = Collections.emptyList();
        }
        List<ScriptCitedTaskPlanVO> citedTaskPlanVOList =
            citedTaskPlanList.parallelStream().map(ScriptCitedTaskPlanDTO::toVO).collect(Collectors.toList());
        return new ScriptCiteInfoVO(citedTemplateVOList, citedTaskPlanVOList);
    }

    protected List<ScriptVO> convertToScriptVOList(List<ScriptDTO> scriptList) {
        if (CollectionUtils.isEmpty(scriptList)) {
            return Collections.emptyList();
        }

        return scriptList.stream().map(ScriptConverter::convertToScriptVO)
            .collect(Collectors.toList());
    }

    protected List<BasicScriptVO> convertToBasicScriptVOList(List<ScriptDTO> scriptList) {
        if (CollectionUtils.isEmpty(scriptList)) {
            return Collections.emptyList();
        }

        return scriptList.stream().map(ScriptConverter::convertToBasicScriptVO)
            .collect(Collectors.toList());
    }

    protected void batchPatchResourceTags(JobResourceTypeEnum resourceType,
                                          List<String> scriptIdList,
                                          List<Long> addTagIdList,
                                          List<Long> deleteTagIdList) {
        List<ResourceTagDTO> addResourceTags = null;
        List<ResourceTagDTO> deleteResourceTags = null;
        if (CollectionUtils.isNotEmpty(addTagIdList)) {
            addResourceTags = tagService.buildResourceTags(resourceType.getValue(),
                scriptIdList.stream().map(String::valueOf).collect(Collectors.toList()),
                addTagIdList);
        }
        if (CollectionUtils.isNotEmpty(deleteTagIdList)) {
            deleteResourceTags = tagService.buildResourceTags(resourceType.getValue(),
                scriptIdList.stream().map(String::valueOf).collect(Collectors.toList()),
                deleteTagIdList);
        }
        tagService.batchPatchResourceTags(addResourceTags, deleteResourceTags);
    }

    protected List<ScriptRelatedTemplateStepVO> listScriptSyncTemplateSteps(Long appId,
                                                                            String scriptId,
                                                                            Long scriptVersionId) {
        List<ScriptSyncTemplateStepDTO> steps = getSyncTemplateSteps(appId, scriptId, scriptVersionId);
        if (CollectionUtils.isEmpty(steps)) {
            return Collections.emptyList();
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
        return stepVOS;
    }

    protected List<ScriptSyncTemplateStepDTO> getSyncTemplateSteps(Long appId,
                                                                   String scriptId,
                                                                   Long scriptVersionId) {
        List<ScriptSyncTemplateStepDTO> steps = scriptManager.listScriptSyncTemplateSteps(appId, scriptId);
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

    protected List<ScriptSyncResultVO> convertToSyncResultVOs(List<SyncScriptResultDTO> syncResults,
                                                              AppResourceScope appResourceScope) {
        List<ScriptSyncResultVO> syncResultVOS = new ArrayList<>(syncResults.size());
        for (SyncScriptResultDTO syncResult : syncResults) {
            ScriptSyncResultVO syncVO = new ScriptSyncResultVO();
            ScriptSyncTemplateStepDTO syncStep = syncResult.getTemplateStep();
            if (syncStep.getAppId() != null && !syncStep.getAppId().equals(PUBLIC_APP_ID)) {
                syncVO.setScopeType(appResourceScope.getType().getValue());
                syncVO.setScopeId(appResourceScope.getId());
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

        return syncResultVOS;
    }
}
