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

package com.tencent.bk.job.manage.service.impl;

import com.tencent.bk.audit.annotations.ActionAuditRecord;
import com.tencent.bk.audit.annotations.AuditInstanceRecord;
import com.tencent.bk.audit.context.ActionAuditContext;
import com.tencent.bk.job.common.audit.constants.EventContentConstants;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.exception.AlreadyExistsException;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.mysql.JobTransactional;
import com.tencent.bk.job.common.util.check.MaxLengthChecker;
import com.tencent.bk.job.common.util.check.NotEmptyChecker;
import com.tencent.bk.job.common.util.check.StringCheckHelper;
import com.tencent.bk.job.common.util.check.TrimChecker;
import com.tencent.bk.job.common.util.check.WhiteCharChecker;
import com.tencent.bk.job.common.util.check.exception.StringCheckException;
import com.tencent.bk.job.manage.auth.TagAuthService;
import com.tencent.bk.job.manage.dao.ResourceTagDAO;
import com.tencent.bk.job.manage.dao.TagDAO;
import com.tencent.bk.job.manage.model.dto.ResourceTagDTO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.service.TagService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TagServiceImpl implements TagService {
    private final TagDAO tagDAO;
    private final ResourceTagDAO resourceTagDAO;
    private final TagAuthService tagAuthService;

    @Autowired
    public TagServiceImpl(TagDAO tagDAO,
                          ResourceTagDAO resourceTagDAO,
                          TagAuthService tagAuthService) {
        this.tagDAO = tagDAO;
        this.resourceTagDAO = resourceTagDAO;
        this.tagAuthService = tagAuthService;
    }

    @Override
    public TagDTO getTagInfoById(Long tagId) {
        if (tagId == null || tagId <= 0) {
            throw new InternalException(ErrorCode.ILLEGAL_PARAM);
        }
        return tagDAO.getTagById(tagId);
    }

    @Override
    public List<TagDTO> listTagInfoByIds(Collection<Long> tagIds) {
        return tagDAO.listTagInfoByIds(tagIds);
    }

    @Override
    public TagDTO getTagInfoById(Long appId, Long tagId) {
        if (appId == null || appId < 0 || tagId == null || tagId <= 0) {
            throw new InternalException(ErrorCode.ILLEGAL_PARAM);
        }
        return tagDAO.getTagById(appId, tagId);
    }

    @Override
    public List<TagDTO> listTagsByAppId(Long appId) {
        if (appId == null || appId < 0) {
            throw new InternalException(ErrorCode.WRONG_APP_ID);
        }
        return tagDAO.listTagsByAppId(appId);
    }

    @Override
    public List<TagDTO> listTagsByAppIdAndTagIdList(Long appId, List<Long> tagIdList) {
        return tagDAO.listTagsByIds(appId, tagIdList);
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.CREATE_TAG,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.TAG,
            instanceIds = "#$?.id",
            instanceNames = "#tag?.name"
        ),
        content = EventContentConstants.CREATE_TAG
    )
    public TagDTO createTag(String username, TagDTO tag) {
        checkCreateTagPermission(username, tag.getAppId());

        tag.setCreator(username);
        tag.setLastModifyUser(username);
        checkRequiredParam(tag);

        boolean isTagExist = tagDAO.isExistDuplicateName(tag.getAppId(), tag.getName());
        if (isTagExist) {
            throw new AlreadyExistsException(ErrorCode.TAG_ALREADY_EXIST);
        }
        tag.setId(tagDAO.insertTag(tag));

        tagAuthService.registerTag(tag.getId(), tag.getName(), username);

        return tag;
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_TAG,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.TAG,
            instanceIds = "#tag?.id",
            instanceNames = "#$?.name"
        ),
        content = EventContentConstants.EDIT_TAG
    )
    public boolean updateTagById(String username, TagDTO tag) {
        checkManageTagPermission(username, tag.getAppId(), tag.getId());

        tag.setLastModifyUser(username);
        checkRequiredParam(tag);

        TagDTO originTag = getTagInfoById(tag.getId());

        boolean isTagNameValid = checkTagName(tag.getAppId(), tag.getId(), tag.getName());
        if (!isTagNameValid) {
            throw new AlreadyExistsException(ErrorCode.TAG_ALREADY_EXIST);
        }

        boolean result = tagDAO.updateTagById(tag);

        // 审计 - 当前数据
        ActionAuditContext.current()
            .setInstanceId(String.valueOf(tag.getId()))
            .setInstanceName(originTag.getName())
            .setOriginInstance(TagDTO.toEsbTagV3DTO(originTag))
            .setInstance(TagDTO.toEsbTagV3DTO(tag));
        return result;
    }

    private void checkManageTagPermission(String username, long appId, Long tagId) {
        tagAuthService.authManageTag(username, new AppResourceScope(appId), tagId, null).denyIfNoPermission();
    }

    private void checkCreateTagPermission(String username, long appId) {
        tagAuthService.authCreateTag(username, new AppResourceScope(appId)).denyIfNoPermission();
    }

    private void checkRequiredParam(TagDTO tag) {
        if (tag.getAppId() == null || tag.getAppId() <= 0) {
            throw new InvalidParamException(ErrorCode.WRONG_APP_ID);
        }
        try {
            StringCheckHelper stringCheckHelper = new StringCheckHelper(
                new TrimChecker(),
                new NotEmptyChecker(),
                new WhiteCharChecker("\\u4e00-\\u9fa5A-Za-z0-9_\\-!#@$&%^~=\\+\\."),
                new MaxLengthChecker(20)
            );
            tag.setName(stringCheckHelper.checkAndGetResult(tag.getName()));
        } catch (StringCheckException e) {
            log.warn("Tag name is invalid:", e);
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        if (StringUtils.isAllBlank(tag.getCreator(), tag.getLastModifyUser())) {
            throw new InvalidParamException(ErrorCode.MISSING_PARAM);
        }
    }

    @Override
    public List<TagDTO> createNewTagIfNotExist(List<TagDTO> tags, Long appId, String username) {
        List<Long> tagIdList = new ArrayList<>();
        Iterator<TagDTO> tagIterator = tags.iterator();
        while (tagIterator.hasNext()) {
            TagDTO tag = tagIterator.next();
            if (tag.getId() == null || tag.getId() == 0) {
                String tagName = tag.getName();
                TagDTO tagDTO = new TagDTO();
                tagDTO.setAppId(appId);
                tagDTO.setName(tagName);
                tagDTO.setCreator(username);
                tagDTO.setLastModifyUser(username);
                Long tagId = tagDAO.insertTag(tagDTO);

                tag.setId(tagId);
            }
            if (tagIdList.contains(tag.getId())) {
                tagIterator.remove();
            } else {
                TagDTO existTag = getTagInfoById(appId, tag.getId());
                if (existTag == null) {
                    throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                        new String[]{"tagId", String.format("tag (id=%s, app_id=%s) not exist", tag.getId(), appId)});
                }
                tagIdList.add(tag.getId());
            }
        }
        return tags;
    }

    @Override
    public List<TagDTO> listTags(Long appId, String tagNameKeyword) {
        TagDTO searchCondition = new TagDTO();
        searchCondition.setAppId(appId);
        searchCondition.setName(tagNameKeyword);
        return tagDAO.listTags(searchCondition);
    }

    @Override
    public PageData<TagDTO> listPageTags(TagDTO tagQuery, BaseSearchCondition baseSearchCondition) {
        return tagDAO.listPageTags(tagQuery, baseSearchCondition);
    }

    @Override
    @JobTransactional(transactionManager = "jobManageTransactionManager")
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_TAG,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.TAG,
            instanceIds = "#tagId"
        ),
        content = EventContentConstants.DELETE_TAG
    )
    public void deleteTag(String username, long appId, Long tagId) {
        checkManageTagPermission(username, appId, tagId);

        TagDTO tag = getTagInfoById(tagId);
        ActionAuditContext.current().setInstanceName(tag.getName());

        tagDAO.deleteTagById(tagId);
        resourceTagDAO.deleteResourceTags(tagId);
    }

    @Override
    public List<ResourceTagDTO> listResourceTagsByTagId(Long appId, Long tagId) {
        TagDTO tag = tagDAO.getTagById(tagId);
        checkTags(appId, Collections.singletonList(tag));

        List<ResourceTagDTO> resourceTags = resourceTagDAO.listResourceTags(Collections.singletonList(tagId));
        resourceTags.forEach(resourceTag -> resourceTag.setTag(tag));
        return resourceTags;
    }

    @Override
    public List<ResourceTagDTO> listResourceTagsByTagIds(Long appId, List<Long> tagIds) {
        List<TagDTO> tags = tagDAO.listTagsByIds(tagIds);
        checkTags(appId, tags);

        Map<Long, TagDTO> tagMap = new HashMap<>();
        tags.forEach(tag -> tagMap.put(tag.getId(), tag));

        List<ResourceTagDTO> resourceTags = resourceTagDAO.listResourceTags(tagIds);
        resourceTags.forEach(resourceTag -> resourceTag.setTag(tagMap.get(resourceTag.getTagId())));

        return resourceTags;
    }

    private void checkTags(Long appId, List<TagDTO> tags) {
        Iterator<TagDTO> iterator = tags.iterator();
        while (iterator.hasNext()) {
            TagDTO tag = iterator.next();
            if (!tag.getAppId().equals(appId) && !tag.getAppId().equals(JobConstants.PUBLIC_APP_ID)) {
                log.info("Tag is not exist, appId={}, tagId={}", appId, tag.getId());
                iterator.remove();
            }
        }
    }

    @Override
    public List<ResourceTagDTO> listResourcesTagsByTagIdAndResourceType(Long appId, Long tagId, Integer resourceType) {
        TagDTO tag = tagDAO.getTagById(tagId);
        checkTags(appId, Collections.singletonList(tag));

        List<ResourceTagDTO> resourceTags = resourceTagDAO.listResourceTags(tagId, resourceType);
        resourceTags.forEach(resourceTag -> resourceTag.setTag(tag));
        return resourceTags;
    }

    @Override
    public List<ResourceTagDTO> listResourceTagsByResourceTypeAndResourceIds(Long appId,
                                                                             Integer resourceType,
                                                                             List<String> resourceIds) {
        List<ResourceTagDTO> resourceTags = resourceTagDAO.listResourceTags(resourceType, resourceIds);
        setTags(appId, resourceTags);
        return resourceTags;
    }

    private void setTags(Long appId, List<ResourceTagDTO> resourceTags) {
        List<Long> tagIds = resourceTags.stream().map(ResourceTagDTO::getTagId).distinct().collect(Collectors.toList());
        List<TagDTO> tags = tagDAO.listTagsByIds(tagIds);

        if (tags.size() != tagIds.size()) {
            log.warn("Inconsistent tag data. tadIds: {}, tags: {}", tagIds, tags);
        }

        checkTags(appId, tags);

        Map<Long, TagDTO> tagMap = new HashMap<>();
        tags.forEach(tag -> tagMap.put(tag.getId(), tag));
        resourceTags.forEach(resourceTag -> resourceTag.setTag(tagMap.get(resourceTag.getTagId())));
    }

    @Override
    public boolean batchDeleteResourceTags(Long appId,
                                           Integer resourceType,
                                           String resourceId,
                                           List<Long> tagIds) {
        return resourceTagDAO.deleteResourceTags(resourceType, resourceId, tagIds);
    }

    @Override
    public boolean batchDeleteResourceTags(Long appId, Integer resourceType, String resourceId) {
        return resourceTagDAO.deleteResourceTags(resourceType, resourceId);
    }

    @Override
    public boolean batchSaveResourceTags(List<ResourceTagDTO> resourceTags) {
        return resourceTagDAO.batchSaveResourceTags(resourceTags);
    }

    @Override
    @JobTransactional(transactionManager = "jobManageTransactionManager")
    public void batchPatchResourceTags(List<ResourceTagDTO> addResourceTags,
                                       List<ResourceTagDTO> deleteResourceTags) {
        StopWatch watch = new StopWatch("batchPatchResourceTags");
        if (CollectionUtils.isNotEmpty(addResourceTags)) {
            watch.start("batchSaveResourceTags");
            resourceTagDAO.batchSaveResourceTags(addResourceTags);
            watch.stop();
        }
        if (CollectionUtils.isNotEmpty(deleteResourceTags)) {
            watch.start("deleteResourceTags");
            resourceTagDAO.deleteResourceTags(deleteResourceTags);
            watch.stop();
        }
        if (watch.getTotalTimeMillis() > 1000L) {
            log.warn("BatchPatchResourceTags cost too much time, cost: {} ms, statistics: {}",
                watch.getTotalTimeMillis(), watch.prettyPrint());
        }
    }

    @Override
    public void patchResourceTags(Integer resourceType, String resourceId, List<Long> latestTagIds) {
        List<ResourceTagDTO> tags = resourceTagDAO.listResourceTags(resourceType, resourceId);
        if (CollectionUtils.isEmpty(tags)) {
            List<ResourceTagDTO> resourceTags = latestTagIds.stream()
                .map(tagId -> new ResourceTagDTO(resourceType, resourceId, tagId)).collect(Collectors.toList());
            resourceTagDAO.batchSaveResourceTags(resourceTags);
        } else {
            List<ResourceTagDTO> addTags = new ArrayList<>();
            List<ResourceTagDTO> deleteTags = new ArrayList<>();
            List<Long> currentTagIds = tags.stream().map(ResourceTagDTO::getTagId).distinct()
                .collect(Collectors.toList());
            latestTagIds.forEach(tagId -> {
                if (!currentTagIds.contains(tagId)) {
                    addTags.add(new ResourceTagDTO(resourceType, resourceId, tagId));
                }
            });
            currentTagIds.forEach(tagId -> {
                if (!latestTagIds.contains(tagId)) {
                    deleteTags.add(new ResourceTagDTO(resourceType, resourceId, tagId));
                }
            });
            batchPatchResourceTags(addTags, deleteTags);
        }
    }

    public List<ResourceTagDTO> buildResourceTags(Integer resourceType, List<String> resourceIds, List<Long> tagIds) {
        List<ResourceTagDTO> resourceTags = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(tagIds) && CollectionUtils.isNotEmpty(resourceIds)) {
            resourceIds.forEach(resourceId -> tagIds.forEach(tagId -> resourceTags.add(
                new ResourceTagDTO(resourceType, resourceId, tagId))));
        }
        return resourceTags;
    }

    @Override
    public Map<Long, Long> countResourcesByTag(List<ResourceTagDTO> tags) {
        Map<Long, Long> tagAndResourceCountMap = new HashMap<>();
        tags.forEach(tag -> tagAndResourceCountMap.compute(tag.getTagId(), (k, v) -> {
            if (v == null) {
                v = 1L;
            } else {
                v += 1L;
            }
            return v;
        }));
        return tagAndResourceCountMap;
    }

    @Override
    public List<String> listAppTaggedResourceIds(Long appId, Integer resourceType) {
        List<Long> allAppTagIds =
            tagDAO.listTagsByAppId(appId).stream().map(TagDTO::getId).collect(Collectors.toList());
        List<ResourceTagDTO> resourceTags = resourceTagDAO.listResourceTags(allAppTagIds, resourceType);
        return resourceTags.stream().map(ResourceTagDTO::getResourceId).distinct().collect(Collectors.toList());
    }

    @Override
    public List<String> listResourceIdsWithAllTagIds(Integer resourceType, List<Long> tagIds) {
        List<Long> distinctTagIds = tagIds.stream().distinct().collect(Collectors.toList());
        List<ResourceTagDTO> resourceTags = resourceTagDAO.listResourceTags(distinctTagIds, resourceType);
        if (CollectionUtils.isEmpty(resourceTags)) {
            return Collections.emptyList();
        }

        Map<String, Integer> resourceTagCountMap = new HashMap<>();
        int matchCount = distinctTagIds.size();
        resourceTags.forEach(resourceTag -> resourceTagCountMap.compute(resourceTag.getResourceId(), (k, v) -> {
            if (v == null) {
                v = 1;
            } else {
                v += 1;
            }
            return v;
        }));

        List<String> resourceIds = new ArrayList<>();
        resourceTagCountMap.forEach((resourceId, count) -> {
            if (count == matchCount) {
                resourceIds.add(resourceId);
            }
        });
        return resourceIds;
    }

    public boolean checkTagName(Long appId, Long tagId, String name) {
        if (tagId == null || tagId == 0) {
            // create tag
            return !tagDAO.isExistDuplicateName(appId, name);
        } else {
            // update tag name
            TagDTO tag = tagDAO.getTagById(tagId);
            if (tag != null && tag.getName().equals(name)) {
                return true;
            } else {
                return !tagDAO.isExistDuplicateName(appId, name);
            }
        }
    }

    @Override
    public List<TagDTO> listAllTags() {
        return tagDAO.listAllTags();
    }
}
