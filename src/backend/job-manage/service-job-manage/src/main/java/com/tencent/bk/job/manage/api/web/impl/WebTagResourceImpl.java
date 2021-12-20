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
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.model.PermissionActionResource;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.manage.api.web.WebTagResource;
import com.tencent.bk.job.manage.model.dto.ResourceTagDTO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.model.web.request.BatchPatchResourceTagReq;
import com.tencent.bk.job.manage.model.web.request.TagCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.TagVO;
import com.tencent.bk.job.manage.service.TagService;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.util.PathBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class WebTagResourceImpl implements WebTagResource {
    private final TagService tagService;
    private final AuthService authService;

    @Autowired
    public WebTagResourceImpl(TagService tagService,
                              AuthService authService) {
        this.tagService = tagService;
        this.authService = authService;
    }

    @Override
    public Response<PageData<TagVO>> listPageTags(String username, Long appId, String name, String creator,
                                                  String lastModifyUser, Integer start, Integer pageSize,
                                                  String orderField, Integer order) {
        TagDTO tagQuery = new TagDTO();
        tagQuery.setAppId(appId);
        tagQuery.setName(name);
        tagQuery.setCreator(creator);
        tagQuery.setLastModifyUser(lastModifyUser);

        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(start);
        baseSearchCondition.setLength(pageSize);
        baseSearchCondition.setOrder(order);
        baseSearchCondition.setOrderField(orderField);

        PageData<TagDTO> pageTags = tagService.listPageTags(tagQuery, baseSearchCondition);
        PageData<TagVO> pageTagVOs = PageData.from(pageTags, TagDTO::toVO);

        List<Long> tagIds = pageTags.getData().stream().map(TagDTO::getId).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(tagIds)) {
            List<ResourceTagDTO> resourceTags = tagService.listResourceTagsByTagIds(appId, tagIds);
            Map<Long, Map<Integer, List<ResourceTagDTO>>> resourcesGroupByTagIdAndResourceType =
                groupByTagIdAndResourceType(resourceTags);
            pageTagVOs.getData().forEach(tag -> {
                Map<Integer, List<ResourceTagDTO>> resourcesGroupByType =
                    resourcesGroupByTagIdAndResourceType.get(tag.getId());
                if (resourcesGroupByType != null) {
                    List<ResourceTagDTO> appScriptResources = resourcesGroupByType
                        .get(JobResourceTypeEnum.APP_SCRIPT.getValue());
                    tag.setRelatedScriptNum(appScriptResources == null ? 0 : appScriptResources.size());
                    List<ResourceTagDTO> templateResources = resourcesGroupByType
                        .get(JobResourceTypeEnum.TEMPLATE.getValue());
                    tag.setRelatedTaskTemplateNum(templateResources == null ? 0 : templateResources.size());
                } else {
                    tag.setRelatedScriptNum(0);
                    tag.setRelatedTaskTemplateNum(0);
                }
            });
        }

        processManagePermission(username, appId, pageTagVOs.getData());

        return Response.buildSuccessResp(pageTagVOs);
    }

    private void processManagePermission(String username, Long appId, List<TagVO> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            return;
        }
        List<String> tagIds =
            tags.stream().map(tag -> String.valueOf(tag.getId())).distinct().collect(Collectors.toList());

        List<String> allowTagIds = authService
            .batchAuth(username, ActionId.MANAGE_TAG, appId, ResourceTypeEnum.TAG, tagIds)
            .parallelStream().collect(Collectors.toList());

        tags.forEach(tagVO -> tagVO.setCanManage(allowTagIds.contains(String.valueOf(tagVO.getId()))));
    }

    private Map<Long, Map<Integer, List<ResourceTagDTO>>> groupByTagIdAndResourceType(
        List<ResourceTagDTO> resourceTags) {

        Map<Long, Map<Integer, List<ResourceTagDTO>>> result = new HashMap<>();
        resourceTags.forEach(resourceTag -> {
            result.computeIfAbsent(resourceTag.getTagId(), k -> new HashMap<>());
            result.get(resourceTag.getTagId()).computeIfAbsent(resourceTag.getResourceType(), k -> new ArrayList<>());
            result.get(resourceTag.getTagId()).get(resourceTag.getResourceType()).add(resourceTag);
        });
        return result;
    }

    @Override
    public Response<List<TagVO>> listTagsBasic(String username, Long appId, String name) {
        List<TagDTO> tags = tagService.listTags(appId, name);
        assert tags != null;
        List<TagVO> tagVOS = new ArrayList<>(tags.size());
        for (TagDTO tag : tags) {
            TagVO tagVO = new TagVO();
            tagVO.setId(tag.getId());
            tagVO.setName(tag.getName());
            tagVOS.add(tagVO);
        }
        return Response.buildSuccessResp(tagVOS);
    }

    @Override
    public Response<Boolean> updateTagInfo(String username, Long appId, Long tagId,
                                           TagCreateUpdateReq tagCreateUpdateReq) {
        AuthResult authResult = checkManageTagPermission(username, appId, tagId);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        TagDTO tag = new TagDTO();
        tag.setId(tagId);
        tag.setAppId(appId);
        tag.setName(tagCreateUpdateReq.getName());
        tag.setDescription(tagCreateUpdateReq.getDescription());
        return Response.buildSuccessResp(tagService.updateTagById(username, tag));
    }

    @Override
    public Response<TagVO> saveTagInfo(String username, Long appId, TagCreateUpdateReq tagCreateUpdateReq) {
        AuthResult authResult = checkCreateTagPermission(username, appId);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        TagDTO tag = new TagDTO();
        tag.setAppId(appId);
        tag.setName(tagCreateUpdateReq.getName());
        tag.setDescription(tagCreateUpdateReq.getDescription());
        Long tagId = tagService.insertNewTag(username, tag);
        authService.registerResource(tagId.toString(), tagCreateUpdateReq.getName(), ResourceId.TAG, username, null);

        TagDTO savedTag = tagService.getTagInfoById(appId, tagId);
        return Response.buildSuccessResp(TagDTO.toVO(savedTag));
    }

    private AuthResult checkManageTagPermission(String username, Long appId, Long tagId) {
        return authService.auth(true, username, ActionId.MANAGE_TAG, ResourceTypeEnum.TAG,
            String.valueOf(tagId), buildTagPathInfo(appId));
    }

    private AuthResult checkCreateTagPermission(String username, Long appId) {
        return authService.auth(true, username, ActionId.CREATE_TAG, ResourceTypeEnum.BUSINESS,
            appId.toString(), null);
    }

    private PathInfoDTO buildTagPathInfo(Long appId) {
        return PathBuilder.newBuilder(ResourceTypeEnum.BUSINESS.getId(), appId.toString()).build();
    }

    @Override
    public Response<Boolean> deleteTag(String username, Long appId, Long tagId) {
        AuthResult authResult = checkManageTagPermission(username, appId, tagId);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        tagService.deleteTag(tagId);
        return Response.buildSuccessResp(true);
    }

    @Override
    public Response<?> patchTagRefResourceTags(String username, Long appId, Long tagId,
                                               BatchPatchResourceTagReq tagBatchUpdateReq) {
        ValidateResult validateResult = checkBatchPatchResourceTagReq(tagId, tagBatchUpdateReq);
        if (!validateResult.isPass()) {
            return Response.buildValidateFailResp(validateResult);
        }

        List<ResourceTagDTO> resourceTags = tagService.listResourceTagsByTagId(appId, tagId);
        Map<JobResourceTypeEnum, Set<String>> resourceGroups = filterAndClassifyResources(
            tagBatchUpdateReq.getResourceTypeList(), resourceTags);
        if (resourceGroups.isEmpty()) {
            return Response.buildSuccessResp(null);
        }

        AuthResult authResult = checkTagRelatedResourcesUpdatePermission(username, appId, resourceGroups);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        List<ResourceTagDTO> addResourceTags = new ArrayList<>();
        List<ResourceTagDTO> deleteResourceTags = new ArrayList<>();
        resourceGroups.forEach((resourceType, resourceIds) -> {
            if (CollectionUtils.isNotEmpty(tagBatchUpdateReq.getAddTagIdList())) {
                resourceIds.forEach(resourceId -> tagBatchUpdateReq.getAddTagIdList()
                    .forEach(addTagId -> addResourceTags.add(
                        new ResourceTagDTO(resourceType.getValue(), resourceId, addTagId))));
            }
            if (CollectionUtils.isNotEmpty(tagBatchUpdateReq.getDeleteTagIdList())) {
                resourceIds.forEach(resourceId -> tagBatchUpdateReq.getDeleteTagIdList()
                    .forEach(deleteTagId -> deleteResourceTags.add(
                        new ResourceTagDTO(resourceType.getValue(), resourceId, deleteTagId))));
            }
            tagService.batchPatchResourceTags(addResourceTags, deleteResourceTags);
        });

        return Response.buildSuccessResp(null);
    }

    private AuthResult checkTagRelatedResourcesUpdatePermission(String username, Long appId,
                                                                  Map<JobResourceTypeEnum, Set<String>> resourceGroup) {
        if (resourceGroup.size() == 0) {
            return AuthResult.pass();
        }

        String appIdStr = String.valueOf(appId);

        List<PermissionActionResource> actionResources = new ArrayList<>(resourceGroup.size());
        resourceGroup.forEach((resourceType, resources) -> {
            switch (resourceType) {
                case APP_SCRIPT:
                    PermissionActionResource manageAppScriptActionResource = new PermissionActionResource();
                    manageAppScriptActionResource.setActionId(ActionId.MANAGE_SCRIPT);
                    resources.forEach(resourceId ->
                        manageAppScriptActionResource.addResource(ResourceTypeEnum.SCRIPT, resourceId,
                            buildAppPathInfo(appIdStr)));
                    actionResources.add(manageAppScriptActionResource);
                    break;
                case PUBLIC_SCRIPT:
                    PermissionActionResource managePublicScriptActionResource = new PermissionActionResource();
                    managePublicScriptActionResource.setActionId(ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE);
                    resources.forEach(resourceId ->
                        managePublicScriptActionResource.addResource(ResourceTypeEnum.PUBLIC_SCRIPT, resourceId, null));
                    actionResources.add(managePublicScriptActionResource);
                    break;
                case TEMPLATE:
                    PermissionActionResource editTemplateActionResource = new PermissionActionResource();
                    editTemplateActionResource.setActionId(ActionId.EDIT_JOB_TEMPLATE);
                    resources.forEach(resourceId ->
                        editTemplateActionResource.addResource(ResourceTypeEnum.TEMPLATE, resourceId,
                            buildAppPathInfo(appIdStr)));
                    actionResources.add(editTemplateActionResource);
                    break;
            }
        });
        return authService.auth(true, username, actionResources);
    }

    private PathInfoDTO buildAppPathInfo(String appId) {
        return PathBuilder.newBuilder(ResourceTypeEnum.BUSINESS.getId(), appId).build();
    }


    private ValidateResult checkBatchPatchResourceTagReq(Long baseTagId, BatchPatchResourceTagReq req) {
        if (CollectionUtils.isEmpty(req.getResourceTypeList())) {
            log.warn("BatchPatchResourceTagReq->resourceTypeList is empty");
            return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "resourceTypeList");
        }
        if (CollectionUtils.isNotEmpty(req.getAddTagIdList())) {
            req.getAddTagIdList().remove(baseTagId);
        }
        for (Integer resourceType : req.getResourceTypeList()) {
            if (!isSupportResourceType(resourceType)) {
                log.warn("BatchPatchResourceTagReq->resourceType is invalid. resourceType: {}", resourceType);
                return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "resourceTypeList");
            }
        }
        if (CollectionUtils.isEmpty(req.getAddTagIdList()) && CollectionUtils.isEmpty(req.getDeleteTagIdList())) {
            log.warn("BatchPatchResourceTagReq->No tags changed!");
            return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME,
                "addTagIdList|deleteTagIdList");
        }
        return ValidateResult.pass();
    }

    private boolean isSupportResourceType(Integer resourceType) {
        JobResourceTypeEnum resourceTypeEnum = JobResourceTypeEnum.valOf(resourceType);
        return (resourceTypeEnum == JobResourceTypeEnum.APP_SCRIPT
            || resourceTypeEnum == JobResourceTypeEnum.TEMPLATE);
    }

    private Map<JobResourceTypeEnum, Set<String>> filterAndClassifyResources(List<Integer> filterResourceTypes,
                                                                             List<ResourceTagDTO> resourceTags) {
        Map<JobResourceTypeEnum, Set<String>> resources = new HashMap<>();
        resourceTags.stream().filter(resourceTag -> filterResourceTypes.contains(resourceTag.getResourceType()))
            .forEach(resourceTag -> {
                JobResourceTypeEnum resourceType = JobResourceTypeEnum.valOf(resourceTag.getResourceType());
                resources.computeIfAbsent(resourceType, k -> new HashSet<>());
                resources.get(resourceType).add(resourceTag.getResourceId());

            });
        return resources;
    }

    @Override
    public Response<Boolean> checkTagName(String username, Long appId, Long tagId, String name) {
        boolean isTagNameValid = tagService.checkTagName(appId, tagId, name);
        return Response.buildSuccessResp(isTagNameValid);
    }
}
