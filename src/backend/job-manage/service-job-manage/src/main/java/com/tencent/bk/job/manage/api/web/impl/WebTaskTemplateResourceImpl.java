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

import com.google.common.base.CaseFormat;
import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.audit.annotations.AuditRequestBody;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.JobResourceTypeEnum;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.mysql.JobTransactional;
import com.tencent.bk.job.manage.api.common.constants.TemplateTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskTemplateStatusEnum;
import com.tencent.bk.job.manage.api.web.WebTaskTemplateResource;
import com.tencent.bk.job.manage.auth.TemplateAuthService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 作业模板Resource
 */
@Slf4j
@RestController
public class WebTaskTemplateResourceImpl implements WebTaskTemplateResource {

    private final TaskTemplateService templateService;
    private final TaskFavoriteService taskFavoriteService;
    private final TaskTemplateAuthService taskTemplateAuthService;
    private final TagService tagService;
    private final TemplateAuthService templateAuthService;

    @Autowired
    public WebTaskTemplateResourceImpl(
        TaskTemplateService templateService,
        @Qualifier("TaskTemplateFavoriteServiceImpl") TaskFavoriteService taskFavoriteService,
        TaskTemplateAuthService taskTemplateAuthService,
        TagService tagService,
        TemplateAuthService templateAuthService) {
        this.templateService = templateService;
        this.taskFavoriteService = taskFavoriteService;
        this.templateAuthService = templateAuthService;
        this.taskTemplateAuthService = taskTemplateAuthService;
        this.tagService = tagService;
    }

    @Override
    public Response<PageData<TaskTemplateVO>> listPageTemplates(String username,
                                                                AppResourceScope appResourceScope,
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
    @AuditEntry(actionId = ActionId.VIEW_JOB_TEMPLATE)
    public Response<TaskTemplateVO> getTemplateById(String username,
                                                    AppResourceScope appResourceScope,
                                                    String scopeType,
                                                    String scopeId,
                                                    Long templateId) {
        TaskTemplateInfoDTO templateInfo = templateService.getTaskTemplate(username,
            appResourceScope.getAppId(), templateId);

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
    @AuditEntry(
        actionId = ActionId.CREATE_JOB_TEMPLATE
    )
    public Response<TaskTemplateVO> createTemplate(String username,
                                                   AppResourceScope appResourceScope,
                                                   String scopeType,
                                                   String scopeId,
                                                   @AuditRequestBody TaskTemplateCreateUpdateReq request) {

        request.validate();

        TaskTemplateInfoDTO createdTemplate = templateService.saveTaskTemplate(username,
            TaskTemplateInfoDTO.fromReq(username, appResourceScope.getAppId(), request));
        return Response.buildSuccessResp(TaskTemplateInfoDTO.toVO(createdTemplate));
    }

    @Override
    @JobTransactional(transactionManager = "jobManageTransactionManager")
    @AuditEntry(
        actionId = ActionId.DELETE_JOB_TEMPLATE,
        subActionIds = {ActionId.DELETE_JOB_PLAN}
    )
    public Response<Boolean> deleteTemplate(String username,
                                            AppResourceScope appResourceScope,
                                            String scopeType,
                                            String scopeId,
                                            Long templateId) {

        Long appId = appResourceScope.getAppId();
        templateService.deleteTaskTemplate(username, appId, templateId);
        taskFavoriteService.deleteFavorite(appId, username, templateId);

        return Response.buildSuccessResp(true);
    }

    @Override
    @AuditEntry(
        actionId = ActionId.EDIT_JOB_TEMPLATE
    )
    public Response<TaskTemplateVO> updateTemplate(String username,
                                                   AppResourceScope appResourceScope,
                                                   String scopeType,
                                                   String scopeId,
                                                   Long templateId,
                                                   @AuditRequestBody TaskTemplateCreateUpdateReq request) {
        request.setId(templateId);
        request.validate();

        TaskTemplateInfoDTO updatedTemplate = templateService.updateTaskTemplate(
            username, TaskTemplateInfoDTO.fromReq(username, appResourceScope.getAppId(), request));

        return Response.buildSuccessResp(TaskTemplateInfoDTO.toVO(updatedTemplate));
    }

    @Override
    public Response<TagCountVO> getTagTemplateCount(String username,
                                                    AppResourceScope appResourceScope,
                                                    String scopeType,
                                                    String scopeId) {

        return Response.buildSuccessResp(templateService.getTagTemplateCount(appResourceScope.getAppId()));
    }

    @Override
    @AuditEntry(
        actionId = ActionId.EDIT_JOB_TEMPLATE
    )
    public Response<Boolean> updateTemplateBasicInfo(String username,
                                                     AppResourceScope appResourceScope,
                                                     String scopeType,
                                                     String scopeId,
                                                     Long templateId,
                                                     @AuditRequestBody TemplateBasicInfoUpdateReq request) {
        templateService.saveTaskTemplateBasicInfo(username,
            TaskTemplateInfoDTO.fromBasicReq(username, appResourceScope.getAppId(), request));
        return Response.buildSuccessResp(true);
    }

    @Override
    public Response<Boolean> addFavorite(String username,
                                         AppResourceScope appResourceScope,
                                         String scopeType,
                                         String scopeId,
                                         Long templateId) {

        return Response.buildSuccessResp(taskFavoriteService.addFavorite(appResourceScope.getAppId(), username,
            templateId));
    }

    @Override
    public Response<Boolean> removeFavorite(String username,
                                            AppResourceScope appResourceScope,
                                            String scopeType,
                                            String scopeId,
                                            Long templateId) {

        return Response.buildSuccessResp(taskFavoriteService.deleteFavorite(appResourceScope.getAppId(), username,
            templateId));
    }

    @Override
    public Response<Boolean> checkTemplateName(String username,
                                               AppResourceScope appResourceScope,
                                               String scopeType,
                                               String scopeId,
                                               Long templateId,
                                               String name) {

        return Response.buildSuccessResp(templateService.checkTemplateName(appResourceScope.getAppId(), templateId,
            name));
    }

    @Override
    public Response<List<TaskTemplateVO>> listTemplateBasicInfoByIds(String username,
                                                                     AppResourceScope appResourceScope,
                                                                     String scopeType,
                                                                     String scopeId,
                                                                     List<Long> templateIds) {

        List<TaskTemplateInfoDTO> taskTemplateBasicInfo =
            templateService.listTaskTemplateBasicInfoByIds(appResourceScope.getAppId(), templateIds);
        List<TaskTemplateVO> templateBasicInfoVOList = taskTemplateBasicInfo.stream()
            .map(TaskTemplateInfoDTO::toVO).collect(Collectors.toList());
        return Response.buildSuccessResp(templateBasicInfoVOList);
    }

    @Override
    public Response<Boolean> batchPatchTemplateTags(String username,
                                                    AppResourceScope appResourceScope,
                                                    String scopeType,
                                                    String scopeId,
                                                    TemplateTagBatchPatchReq req) {

        ValidateResult validateResult = checkTemplateTagBatchPatchReq(req);
        if (!validateResult.isPass()) {
            throw new InvalidParamException(validateResult);
        }

        if (CollectionUtils.isEmpty(req.getAddTagIdList()) && CollectionUtils.isEmpty(req.getDeleteTagIdList())) {
            // do nothing
            return Response.buildSuccessResp(true);
        }

        AuthResult authResult = templateAuthService.batchAuthResultEditJobTemplate(username,
            appResourceScope, req.getIdList());
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
