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
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.api.web.WebTaskTemplateResource;
import com.tencent.bk.job.manage.common.consts.TemplateTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskTemplateStatusEnum;
import com.tencent.bk.job.manage.common.util.IamPathUtil;
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
import com.tencent.bk.sdk.iam.util.PathBuilder;
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
    private final WebAuthService authService;
    private final TaskTemplateAuthService taskTemplateAuthService;
    private final TagService tagService;
    private final MessageI18nService i18nService;

    @Autowired
    public WebTaskTemplateResourceImpl(
        TaskTemplateService templateService,
        @Qualifier("TaskTemplateFavoriteServiceImpl") TaskFavoriteService taskFavoriteService,
        WebAuthService webAuthService,
        TaskTemplateAuthService taskTemplateAuthService,
        TagService tagService,
        MessageI18nService i18nService) {
        this.templateService = templateService;
        this.taskFavoriteService = taskFavoriteService;
        this.authService = webAuthService;
        this.taskTemplateAuthService = taskTemplateAuthService;
        this.tagService = tagService;
        this.i18nService = i18nService;
    }

    @Override
    public ServiceResponse<PageData<TaskTemplateVO>> listPageTemplates(
        String username,
        Long appId,
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
        Integer order
    ) {
        AuthResultVO authResultVO = authService.auth(true, username, ActionId.LIST_BUSINESS,
            ResourceTypeEnum.BUSINESS, appId.toString(), null);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }

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
            return ServiceResponse.buildCommonFailResp("No template info found!");
        }

        resultTemplates.forEach(taskTemplate ->
            taskTemplate.setFavored(favoriteList.contains(taskTemplate.getId()) ? 1 : 0));

        PageData<TaskTemplateVO> resultPageData = new PageData<>();
        resultPageData.setStart(templateInfoPageData.getStart());
        resultPageData.setPageSize(templateInfoPageData.getPageSize());
        resultPageData.setTotal(templateInfoPageData.getTotal());
        resultPageData.setData(resultTemplates);
        resultPageData.setExistAny(templateService.isExistAnyAppTemplate(appId));

        taskTemplateAuthService.processTemplatePermission(username, appId, resultPageData);

        return ServiceResponse.buildSuccessResp(resultPageData);
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
    public ServiceResponse<TaskTemplateVO> getTemplateById(String username, Long appId, Long templateId) {
        AuthResultVO authResultVO = authService.auth(true, username, ActionId.VIEW_JOB_TEMPLATE,
            ResourceTypeEnum.TEMPLATE, templateId.toString(), IamPathUtil.buildTemplatePathInfo(appId));
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        TaskTemplateInfoDTO templateInfo = templateService.getTaskTemplateById(appId, templateId);
        if (templateInfo == null) {
            return ServiceResponse.buildCommonFailResp(1, "Not found");
        }

        TaskTemplateVO taskTemplateVO = TaskTemplateInfoDTO.toVO(templateInfo);
        taskTemplateVO.setCanView(true);
        taskTemplateVO.setCanEdit(authService.auth(false, username, ActionId.EDIT_JOB_TEMPLATE,
            ResourceTypeEnum.TEMPLATE, templateId.toString(), IamPathUtil.buildTemplatePathInfo(appId)).isPass());
        taskTemplateVO.setCanDelete(authService.auth(false, username, ActionId.DELETE_JOB_TEMPLATE,
            ResourceTypeEnum.TEMPLATE, templateId.toString(), IamPathUtil.buildTemplatePathInfo(appId)).isPass());
        taskTemplateVO.setCanDebug(true);
        taskTemplateVO.setCanClone(taskTemplateVO.getCanView() && authService
            .auth(false, username, ActionId.CREATE_JOB_TEMPLATE,
                ResourceTypeEnum.BUSINESS, appId.toString(), null)
            .isPass());

        return ServiceResponse.buildSuccessResp(taskTemplateVO);
    }

    @Override
    public ServiceResponse<Long> saveTemplate(String username, Long appId, Long templateId,
                                              TaskTemplateCreateUpdateReq taskTemplateCreateUpdateReq) {
        AuthResultVO authResultVO;
        if (templateId > 0) {
            taskTemplateCreateUpdateReq.setId(templateId);
            authResultVO = authService.auth(true, username, ActionId.EDIT_JOB_TEMPLATE,
                ResourceTypeEnum.TEMPLATE, templateId.toString(), IamPathUtil.buildTemplatePathInfo(appId));
        } else {
            authResultVO = authService.auth(true, username,
                ActionId.CREATE_JOB_TEMPLATE, ResourceTypeEnum.BUSINESS,
                appId.toString(), null);
        }
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        if (taskTemplateCreateUpdateReq.validate()) {
            Long finalTemplateId = templateService
                .saveTaskTemplate(TaskTemplateInfoDTO.fromReq(username, appId, taskTemplateCreateUpdateReq));
            authService.registerResource(finalTemplateId.toString(), taskTemplateCreateUpdateReq.getName(),
                ResourceId.TEMPLATE, username, null);
            return ServiceResponse.buildSuccessResp(finalTemplateId);
        } else {
            return ServiceResponse
                .buildCommonFailResp("Valid param failed!" + JsonUtils.toJson(JobContextUtil.getDebugMessage()));
        }
    }

    @Override
    @Transactional(rollbackFor = {Exception.class, Error.class})
    public ServiceResponse<Boolean> deleteTemplate(String username, Long appId, Long templateId) {
        AuthResultVO authResultVO = authService.auth(true, username, ActionId.DELETE_JOB_TEMPLATE,
            ResourceTypeEnum.TEMPLATE, templateId.toString(), IamPathUtil.buildTemplatePathInfo(appId));
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        if (templateService.deleteTaskTemplate(appId, templateId)) {
            taskFavoriteService.deleteFavorite(appId, username, templateId);
            return ServiceResponse.buildSuccessResp(true);
        }
        return ServiceResponse.buildSuccessResp(false);
    }

    @Override
    public ServiceResponse<TagCountVO> getTagTemplateCount(String username, Long appId) {
        AuthResultVO authResultVO = authService.auth(true, username, ActionId.LIST_BUSINESS,
            ResourceTypeEnum.BUSINESS, appId.toString(), null);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        return ServiceResponse.buildSuccessResp(templateService.getTagTemplateCount(appId));
    }

    @Override
    public ServiceResponse<Boolean> updateTemplateBasicInfo(String username, Long appId, Long templateId,
                                                            TemplateBasicInfoUpdateReq templateBasicInfoUpdateReq) {
        if (templateId > 0) {
            templateBasicInfoUpdateReq.setId(templateId);
        } else {
            return ServiceResponse.buildCommonFailResp("Missing template id!");
        }
        AuthResultVO authResultVO = authService.auth(true, username, ActionId.EDIT_JOB_TEMPLATE,
            ResourceTypeEnum.TEMPLATE, templateId.toString(), IamPathUtil.buildTemplatePathInfo(appId));
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        return ServiceResponse.buildSuccessResp(templateService
            .saveTaskTemplateBasicInfo(TaskTemplateInfoDTO.fromBasicReq(username, appId, templateBasicInfoUpdateReq)));
    }

    @Override
    public ServiceResponse<Boolean> addFavorite(String username, Long appId, Long templateId) {
        AuthResultVO authResultVO = authService.auth(true, username, ActionId.LIST_BUSINESS,
            ResourceTypeEnum.BUSINESS, appId.toString(), null);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        return ServiceResponse.buildSuccessResp(taskFavoriteService.addFavorite(appId, username, templateId));
    }

    @Override
    public ServiceResponse<Boolean> removeFavorite(String username, Long appId, Long templateId) {
        AuthResultVO authResultVO = authService.auth(true, username, ActionId.LIST_BUSINESS,
            ResourceTypeEnum.BUSINESS, appId.toString(), null);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        return ServiceResponse.buildSuccessResp(taskFavoriteService.deleteFavorite(appId, username, templateId));
    }

    @Override
    public ServiceResponse<Boolean> checkTemplateName(String username, Long appId, Long templateId, String name) {
        AuthResultVO authResultVO = authService.auth(true, username, ActionId.LIST_BUSINESS,
            ResourceTypeEnum.BUSINESS, appId.toString(), null);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        return ServiceResponse.buildSuccessResp(templateService.checkTemplateName(appId, templateId, name));
    }

    @Override
    public ServiceResponse<List<TaskTemplateVO>> listTemplateBasicInfoByIds(String username, Long appId,
                                                                            List<Long> templateIds) {
        AuthResultVO authResultVO = authService.auth(true, username, ActionId.LIST_BUSINESS,
            ResourceTypeEnum.BUSINESS, appId.toString(), null);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        List<TaskTemplateInfoDTO> taskTemplateBasicInfo =
            templateService.listTaskTemplateBasicInfoByIds(appId, templateIds);
        List<TaskTemplateVO> templateBasicInfoVOList = new ArrayList<>();
        for (TaskTemplateInfoDTO templateInfo : taskTemplateBasicInfo) {
            if (templateInfo != null) {
                TaskTemplateVO taskTemplateVO = new TaskTemplateVO();
                taskTemplateVO.setId(templateInfo.getId());
                taskTemplateVO.setName(templateInfo.getName());

                templateBasicInfoVOList.add(taskTemplateVO);
            }
        }
        return ServiceResponse.buildSuccessResp(templateBasicInfoVOList);
    }

    @Override
    public ServiceResponse<Boolean> batchPatchTemplateTags(String username, Long appId,
                                                           TemplateTagBatchPatchReq req) {
        ValidateResult validateResult = checkTemplateTagBatchPatchReq(req);
        if (!validateResult.isPass()) {
            return ServiceResponse.buildValidateFailResp(i18nService, validateResult);
        }

        AuthResultVO authResultVO = batchAuthTemplate(username, ActionId.EDIT_JOB_TEMPLATE, appId,
            req.getIdList().stream().map(String::valueOf).collect(Collectors.toList()));
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
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

        return ServiceResponse.buildSuccessResp(null);
    }

    private AuthResultVO batchAuthTemplate(String username, String actionId, Long appId, List<String> templateIdList) {
        List<PermissionResource> resources = templateIdList.stream().map(templateId -> {
            PermissionResource resource = new PermissionResource();
            resource.setResourceId(templateId);
            resource.setResourceType(ResourceTypeEnum.TEMPLATE);
            resource.setPathInfo(PathBuilder.newBuilder(
                ResourceTypeEnum.BUSINESS.getId(),
                appId.toString()
            ).build());
            return resource;
        }).collect(Collectors.toList());
        return authService.batchAuthResources(username, actionId, appId, resources);
    }

    private ValidateResult checkTemplateTagBatchPatchReq(TemplateTagBatchPatchReq req) {
        if (CollectionUtils.isEmpty(req.getIdList())) {
            log.warn("TemplateTagBatchPatchReq->idList is empty");
            return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "idList");
        }

        if (CollectionUtils.isEmpty(req.getAddTagIdList()) && CollectionUtils.isEmpty(req.getDeleteTagIdList())) {
            log.warn("TemplateTagBatchPatchReq->No template tags changed!");
            return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME,
                "addTagIdList|deleteTagIdList");
        }
        return ValidateResult.pass();
    }
}
