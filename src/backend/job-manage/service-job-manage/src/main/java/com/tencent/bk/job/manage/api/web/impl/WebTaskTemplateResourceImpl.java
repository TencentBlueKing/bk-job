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
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.model.permission.AuthResultVO;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.common.web.controller.AbstractJobController;
import com.tencent.bk.job.manage.api.web.WebTaskTemplateResource;
import com.tencent.bk.job.manage.common.consts.TemplateTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskTemplateStatusEnum;
import com.tencent.bk.job.manage.common.util.IamPathUtil;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.web.request.TaskTemplateCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.request.TemplateBasicInfoUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.TagCountVO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskTemplateVO;
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

import java.util.*;
import java.util.stream.Collectors;

/**
 * @since 16/10/2019 16:16
 */
@Slf4j
@RestController
public class WebTaskTemplateResourceImpl extends AbstractJobController implements WebTaskTemplateResource {

    private final TaskTemplateService templateService;
    private final TaskFavoriteService taskFavoriteService;
    private final WebAuthService authService;
    private final TaskTemplateAuthService taskTemplateAuthService;

    @Autowired
    public WebTaskTemplateResourceImpl(
        TaskTemplateService templateService,
        @Qualifier("TaskTemplateFavoriteServiceImpl") TaskFavoriteService taskFavoriteService,
        WebAuthService authService,
        TaskTemplateAuthService taskTemplateAuthService
    ) {
        this.templateService = templateService;
        this.taskFavoriteService = taskFavoriteService;
        this.authService = authService;
        this.taskTemplateAuthService = taskTemplateAuthService;
    }

    @Override
    public ServiceResponse<PageData<TaskTemplateVO>> listTemplates(
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

        TaskTemplateInfoDTO taskTemplateCondition = new TaskTemplateInfoDTO();
        taskTemplateCondition.setAppId(appId);
        taskTemplateCondition.setName(name);
        if (templateId != null && templateId > 0) {
            taskTemplateCondition.setId(templateId);
            tags = null;
            panelTag = null;
            type = null;
        }
        if (status != null) {
            taskTemplateCondition.setStatus(TaskTemplateStatusEnum.valueOf(status));
        }
        if (StringUtils.isNotBlank(tags)) {
            taskTemplateCondition.setTags(Arrays.stream(tags.split(",")).map(tag -> {
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
        if (panelTag != null && panelTag > 0) {
            // Frontend need additional param to tell where the tag from
            TagDTO tagInfo = new TagDTO();
            tagInfo.setId(panelTag);
            if (CollectionUtils.isEmpty(taskTemplateCondition.getTags())) {
                taskTemplateCondition.setTags(Collections.singletonList(tagInfo));
            } else {
                taskTemplateCondition.getTags().add(tagInfo);
            }
        }

        // Process type
        taskTemplateCondition.setScriptStatus(null);
        if (type != null) {
            if (TemplateTypeEnum.ALL_TEMPLATE.getValue() == type) {
                // Delete by request
                // taskTemplateCondition.setTags(null);
            } else if (TemplateTypeEnum.UNCLASSIFIED.getValue() == type) {
                TagDTO tagDTO = new TagDTO();
                tagDTO.setId(0L);
                if (CollectionUtils.isNotEmpty(taskTemplateCondition.getTags())) {
                    PageData<TaskTemplateVO> emptyResult = new PageData<>();
                    emptyResult.setStart(start);
                    emptyResult.setPageSize(pageSize);
                    emptyResult.setTotal(0L);
                    emptyResult.setData(Collections.emptyList());
                    return ServiceResponse.buildSuccessResp(emptyResult);
                } else {
                    taskTemplateCondition.setTags(Collections.singletonList(tagDTO));
                }
            } else if (TemplateTypeEnum.NEED_UPDATE.getValue() == type) {
                taskTemplateCondition.setScriptStatus(0b11);
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
        PageData<TaskTemplateInfoDTO> templateInfoPageData =
            templateService.listPageTaskTemplatesBasicInfo(taskTemplateCondition, baseSearchCondition, favoriteList);
        List<TaskTemplateVO> resultTemplates = new ArrayList<>();
        if (templateInfoPageData != null) {
            templateInfoPageData.getData().forEach(templateInfo ->
                resultTemplates.add(TaskTemplateInfoDTO.toVO(templateInfo)));
        } else {
            return ServiceResponse.buildCommonFailResp("No template info found!");
        }

        resultTemplates.forEach(taskTemplate -> {
            taskTemplate.setFavored(favoriteList.contains(taskTemplate.getId()) ? 1 : 0);
        });

        PageData<TaskTemplateVO> resultPageData = new PageData<>();
        resultPageData.setStart(templateInfoPageData.getStart());
        resultPageData.setPageSize(templateInfoPageData.getPageSize());
        resultPageData.setTotal(templateInfoPageData.getTotal());
        resultPageData.setData(resultTemplates);
        resultPageData.setExistAny(templateService.isExistAnyAppTemplate(appId));

        taskTemplateAuthService.processTemplatePermission(username, appId, resultPageData);

        ServiceResponse<PageData<TaskTemplateVO>> resp = ServiceResponse.buildSuccessResp(resultPageData);

        return resp;
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

        ServiceResponse<TaskTemplateVO> resp = ServiceResponse.buildSuccessResp(taskTemplateVO);
        return resp;
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

}
