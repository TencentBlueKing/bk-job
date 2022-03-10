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

import com.google.common.base.CaseFormat;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.JobResourceTypeEnum;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.manage.api.web.WebTaskTemplateResource;
import com.tencent.bk.job.manage.auth.TemplateAuthService;
import com.tencent.bk.job.manage.common.consts.TemplateTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskTemplateStatusEnum;
import com.tencent.bk.job.manage.manager.variable.StepRefVariableParser;
import com.tencent.bk.job.manage.model.dto.ResourceTagDTO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.query.TaskTemplateQuery;
import com.tencent.bk.job.manage.model.web.request.TaskTemplateCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.request.TemplateBasicInfoUpdateReq;
import com.tencent.bk.job.manage.model.web.request.TemplateTagBatchPatchReq;
import com.tencent.bk.job.manage.model.web.vo.TagCountVO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskTemplateVO;
import com.tencent.bk.job.manage.service.TagService;
import com.tencent.bk.job.manage.service.TaskFavoriteService;
import com.tencent.bk.job.manage.service.auth.TaskTemplateAuthService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @since 16/10/2019 16:16
 */
@Slf4j
@RestController
public class WebTaskTemplateResourceImpl implements WebTaskTemplateResource {

    private final TaskTemplateService templateService;
    private final TaskFavoriteService taskFavoriteService;
    private final TaskTemplateAuthService taskTemplateAuthService;
    private final TagService tagService;
    private final TemplateAuthService templateAuthService;
    private final AppScopeMappingService appScopeMappingService;

    @Autowired
    public WebTaskTemplateResourceImpl(
        TaskTemplateService templateService,
        @Qualifier("TaskTemplateFavoriteServiceImpl") TaskFavoriteService taskFavoriteService,
        TaskTemplateAuthService taskTemplateAuthService,
        TagService tagService,
        TemplateAuthService templateAuthService,
        AppScopeMappingService appScopeMappingService) {
        this.templateService = templateService;
        this.taskFavoriteService = taskFavoriteService;
        this.templateAuthService = templateAuthService;
        this.taskTemplateAuthService = taskTemplateAuthService;
        this.tagService = tagService;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    public Response<PageData<TaskTemplateVO>> listPageTemplates(String username,
                                                                String scopeType,
                                                                String scopeId,
                                                                Long templateId,
                                                                String name,
                                                                Integer status,
                                                                String tags,
                                                                Long panelTag,
                                                                Integer type,
                                                                String creator,
                                                                String lastModifyUser,
                                                                Integer start,
                                                                Integer pageSize,
                                                                String orderField,
                                                                Integer order) {
        AppResourceScope appResourceScope = appScopeMappingService.getAppResourceScope(null, scopeType, scopeId);
        Long appId = appResourceScope.getAppId();
        List<Long> favoriteList = taskFavoriteService.listFavorites(appId, username);

        TaskTemplateQuery query = buildTaskTemplateQuery(appId, name, templateId, status, tags, panelTag, type,
            start, pageSize, creator, lastModifyUser, orderField, order);

        PageData<TaskTemplateInfoDTO> templateInfoPageData =
            templateService.listPageTaskTemplatesBasicInfo(query, favoriteList);
        List<TaskTemplateVO> resultTemplates = new ArrayList<>();
        if (templateInfoPageData != null) {
            templateInfoPageData.getData().forEach(templateInfo ->
                resultTemplates.add(TaskTemplateInfoDTO.toVO(templateInfo)));
        } else {
            throw new NotFoundException(ErrorCode.TEMPLATE_NOT_EXIST);
        }

        resultTemplates.forEach(taskTemplate ->
            taskTemplate.setFavored(favoriteList.contains(taskTemplate.getId()) ? 1 : 0));

        PageData<TaskTemplateVO> resultPageData = new PageData<>();
        resultPageData.setStart(templateInfoPageData.getStart());
        resultPageData.setPageSize(templateInfoPageData.getPageSize());
        resultPageData.setTotal(templateInfoPageData.getTotal());
        resultPageData.setData(resultTemplates);
        resultPageData.setExistAny(templateService.isExistAnyAppTemplate(appId));

        taskTemplateAuthService.processTemplatePermission(username, appResourceScope, resultPageData);

        return Response.buildSuccessResp(resultPageData);
    }

    private TaskTemplateQuery buildTaskTemplateQuery(Long appId, String name, Long templateId, Integer status,
                                                     String tags,
                                                     Long panelTag, Integer type, Integer start, Integer pageSize,
                                                     String creator,
                                                     String lastModifyUser, String orderField, Integer order) {
        TaskTemplateQuery query = TaskTemplateQuery.builder().appId(appId).name(name).id(templateId).build();

        if (status != null) {
            query.setStatus(TaskTemplateStatusEnum.valueOf(status));
        }
        addTagCondition(query, tags, panelTag);

        // Process type
        query.setScriptStatus(null);
        if (type != null) {
            if (TemplateTypeEnum.UNCLASSIFIED.getValue() == type) {
                query.setUntaggedTemplate(true);
            } else if (TemplateTypeEnum.NEED_UPDATE.getValue() == type) {
                query.setScriptStatus(0b11);
            }
        }

        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(start);
        baseSearchCondition.setLength(pageSize);
        if (StringUtils.isNoneBlank(creator)) {
            baseSearchCondition.setCreator(creator);
        }
        if (StringUtils.isNotBlank(lastModifyUser)) {
            baseSearchCondition.setLastModifyUser(lastModifyUser);
        }
        if (StringUtils.isNotBlank(orderField)) {
            baseSearchCondition.setOrderField(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, orderField));
        }
        baseSearchCondition.setOrder(order);
        query.setBaseSearchCondition(baseSearchCondition);
        return query;
    }

    private void addTagCondition(TaskTemplateQuery query, String tags, Long panelTagId) {
        if (StringUtils.isNotBlank(tags)) {
            query.setTags(Arrays.stream(tags.split(",")).map(tag -> {
                TagDTO tagInfo = new TagDTO();
                long tagId;
                try {
                    tagId = Long.parseLong(tag);
                } catch (NumberFormatException e) {
                    return null;
                }
                tagInfo.setId(tagId);
                return tagInfo;
            }).filter(Objects::nonNull).collect(Collectors.toList()));
        }
        if (panelTagId != null && panelTagId > 0) {
            // Frontend need additional param to tell where the tag from
            TagDTO tagInfo = new TagDTO();
            tagInfo.setId(panelTagId);
            if (CollectionUtils.isEmpty(query.getTags())) {
                query.setTags(Collections.singletonList(tagInfo));
            } else {
                query.getTags().add(tagInfo);
            }
        }
    }

    @Override
    public Response<TaskTemplateVO> getTemplateById(String username, String scopeType, String scopeId,
                                                    Long templateId) {
        AppResourceScope appResourceScope = appScopeMappingService.getAppResourceScope(null, scopeType, scopeId);
        AuthResult authResult = templateAuthService.authViewJobTemplate(username, appResourceScope,
            templateId);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        TaskTemplateInfoDTO templateInfo = templateService.getTaskTemplateById(appResourceScope.getAppId(), templateId);
        if (templateInfo == null) {
            throw new NotFoundException(ErrorCode.TEMPLATE_NOT_EXIST);
        }
        StepRefVariableParser.parseStepRefVars(templateInfo.getStepList(), templateInfo.getVariableList());

        TaskTemplateVO taskTemplateVO = TaskTemplateInfoDTO.toVO(templateInfo);
        taskTemplateVO.setCanView(true);
        taskTemplateVO.setCanEdit(templateAuthService.authEditJobTemplate(username, appResourceScope,
            templateId).isPass());
        taskTemplateVO.setCanDelete(templateAuthService.authDeleteJobTemplate(username, appResourceScope,
            templateId).isPass());
        taskTemplateVO.setCanDebug(true);
        taskTemplateVO.setCanClone(taskTemplateVO.getCanView()
            && templateAuthService.authCreateJobTemplate(username, appResourceScope).isPass());

        return Response.buildSuccessResp(taskTemplateVO);
    }

    @Override
    public Response<Long> saveTemplate(String username, String scopeType, String scopeId, Long templateId,
                                       TaskTemplateCreateUpdateReq taskTemplateCreateUpdateReq) {
        AppResourceScope appResourceScope = appScopeMappingService.getAppResourceScope(null, scopeType, scopeId);
        AuthResult authResult;
        if (templateId > 0) {
            taskTemplateCreateUpdateReq.setId(templateId);
            authResult = templateAuthService.authEditJobTemplate(username, appResourceScope, templateId);
        } else {
            authResult = templateAuthService.authCreateJobTemplate(username, appResourceScope);
        }
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        if (taskTemplateCreateUpdateReq.validate()) {
            Long finalTemplateId = templateService
                .saveTaskTemplate(TaskTemplateInfoDTO.fromReq(username, appResourceScope.getAppId(),
                    taskTemplateCreateUpdateReq));
            templateAuthService.registerTemplate(finalTemplateId, taskTemplateCreateUpdateReq.getName(), username);
            return Response.buildSuccessResp(finalTemplateId);
        } else {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
    }

    @Override
    @Transactional(rollbackFor = {Exception.class, Error.class})
    public Response<Boolean> deleteTemplate(String username, String scopeType, String scopeId, Long templateId) {
        AppResourceScope appResourceScope = appScopeMappingService.getAppResourceScope(null, scopeType, scopeId);
        AuthResult authResult = templateAuthService.authDeleteJobTemplate(username, appResourceScope,
            templateId);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        Long appId = appResourceScope.getAppId();
        if (templateService.deleteTaskTemplate(appId, templateId)) {
            taskFavoriteService.deleteFavorite(appId, username, templateId);
            return Response.buildSuccessResp(true);
        }
        return Response.buildSuccessResp(false);
    }

    @Override
    public Response<TagCountVO> getTagTemplateCount(String username, String scopeType, String scopeId) {
        AppResourceScope appResourceScope = appScopeMappingService.getAppResourceScope(null, scopeType, scopeId);
        return Response.buildSuccessResp(templateService.getTagTemplateCount(appResourceScope.getAppId()));
    }

    @Override
    public Response<Boolean> updateTemplateBasicInfo(String username, String scopeType, String scopeId, Long templateId,
                                                     TemplateBasicInfoUpdateReq templateBasicInfoUpdateReq) {
        AppResourceScope appResourceScope = appScopeMappingService.getAppResourceScope(null, scopeType, scopeId);
        if (templateId > 0) {
            templateBasicInfoUpdateReq.setId(templateId);
        } else {
            throw new NotFoundException(ErrorCode.TEMPLATE_NOT_EXIST);
        }
        AuthResult authResult = templateAuthService.authEditJobTemplate(username, appResourceScope,
            templateId);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        return Response.buildSuccessResp(
            templateService.saveTaskTemplateBasicInfo(
                TaskTemplateInfoDTO.fromBasicReq(username, appResourceScope.getAppId(), templateBasicInfoUpdateReq)));
    }

    @Override
    public Response<Boolean> addFavorite(String username, String scopeType, String scopeId, Long templateId) {
        AppResourceScope appResourceScope = appScopeMappingService.getAppResourceScope(null, scopeType, scopeId);
        return Response.buildSuccessResp(taskFavoriteService.addFavorite(appResourceScope.getAppId(), username,
            templateId));
    }

    @Override
    public Response<Boolean> removeFavorite(String username, String scopeType, String scopeId, Long templateId) {
        AppResourceScope appResourceScope = appScopeMappingService.getAppResourceScope(null, scopeType, scopeId);
        return Response.buildSuccessResp(taskFavoriteService.deleteFavorite(appResourceScope.getAppId(), username,
            templateId));
    }

    @Override
    public Response<Boolean> checkTemplateName(String username, String scopeType, String scopeId, Long templateId,
                                               String name) {
        AppResourceScope appResourceScope = appScopeMappingService.getAppResourceScope(null, scopeType, scopeId);
        return Response.buildSuccessResp(templateService.checkTemplateName(appResourceScope.getAppId(), templateId,
            name));
    }

    @Override
    public Response<List<TaskTemplateVO>> listTemplateBasicInfoByIds(String username, String scopeType, String scopeId,
                                                                     List<Long> templateIds) {
        AppResourceScope appResourceScope = appScopeMappingService.getAppResourceScope(null, scopeType, scopeId);
        List<TaskTemplateInfoDTO> taskTemplateBasicInfo =
            templateService.listTaskTemplateBasicInfoByIds(appResourceScope.getAppId(), templateIds);
        List<TaskTemplateVO> templateBasicInfoVOList = taskTemplateBasicInfo.stream()
            .map(TaskTemplateInfoDTO::toVO).collect(Collectors.toList());
        return Response.buildSuccessResp(templateBasicInfoVOList);
    }

    @Override
    public Response<Boolean> batchPatchTemplateTags(String username, String scopeType, String scopeId,
                                                    TemplateTagBatchPatchReq req) {
        AppResourceScope appResourceScope = appScopeMappingService.getAppResourceScope(null, scopeType, scopeId);
        ValidateResult validateResult = checkTemplateTagBatchPatchReq(req);
        if (!validateResult.isPass()) {
            throw new InvalidParamException(validateResult);
        }

        if (CollectionUtils.isEmpty(req.getAddTagIdList()) && CollectionUtils.isEmpty(req.getDeleteTagIdList())) {
            // do nothing
            return Response.buildSuccessResp(true);
        }

        AuthResult authResult = templateAuthService.batchAuthResultEditJobTemplate(username,
            appResourceScope,
            req.getIdList());
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        List<Long> templateIdList = req.getIdList();
        List<ResourceTagDTO> addResourceTags = null;
        List<ResourceTagDTO> deleteResourceTags = null;
        if (CollectionUtils.isNotEmpty(req.getAddTagIdList())) {
            addResourceTags = tagService.buildResourceTags(JobResourceTypeEnum.TEMPLATE.getValue(),
                templateIdList.stream().map(String::valueOf).collect(Collectors.toList()),
                req.getAddTagIdList());
        }
        if (CollectionUtils.isNotEmpty(req.getDeleteTagIdList())) {
            deleteResourceTags = tagService.buildResourceTags(JobResourceTypeEnum.TEMPLATE.getValue(),
                templateIdList.stream().map(String::valueOf).collect(Collectors.toList()),
                req.getDeleteTagIdList());
        }
        tagService.batchPatchResourceTags(addResourceTags, deleteResourceTags);

        return Response.buildSuccessResp(null);
    }

    private ValidateResult checkTemplateTagBatchPatchReq(TemplateTagBatchPatchReq req) {
        if (CollectionUtils.isEmpty(req.getIdList())) {
            log.warn("TemplateTagBatchPatchReq->idList is empty");
            return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "idList");
        }

        return ValidateResult.pass();
    }
}
