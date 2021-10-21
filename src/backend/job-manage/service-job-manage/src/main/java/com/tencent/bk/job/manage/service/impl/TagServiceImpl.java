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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.ParamErrorException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.util.check.MaxLengthChecker;
import com.tencent.bk.job.common.util.check.NotEmptyChecker;
import com.tencent.bk.job.common.util.check.StringCheckHelper;
import com.tencent.bk.job.common.util.check.TrimChecker;
import com.tencent.bk.job.common.util.check.WhiteCharChecker;
import com.tencent.bk.job.common.util.check.exception.StringCheckException;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
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

    @Autowired
    public TagServiceImpl(TagDAO tagDAO, ResourceTagDAO resourceTagDAO) {
        this.tagDAO = tagDAO;
        this.resourceTagDAO = resourceTagDAO;
    }

    @Override
    public TagDTO getTagInfoById(Long tagId) {
        if (tagId == null || tagId <= 0) {
            throw new ParamErrorException(ErrorCode.ILLEGAL_PARAM);
        }
        return tagDAO.getTagById(tagId);
    }

    @Override
    public TagDTO getTagInfoById(Long appId, Long tagId) {
        if (appId == null || appId < 0 || tagId == null || tagId <= 0) {
            throw new ParamErrorException(ErrorCode.ILLEGAL_PARAM);
        }
        return tagDAO.getTagById(appId, tagId);
    }

    @Override
    public List<TagDTO> listTagsByAppId(Long appId) {
        if (appId == null || appId < 0) {
            throw new ParamErrorException(ErrorCode.WRONG_APP_ID);
        }
        return tagDAO.listTagsByAppId(appId);
    }

    @Override
    public List<TagDTO> listTagsByAppIdAndTagIdList(Long appId, List<Long> tagIdList) {
        return tagDAO.listTagsByIds(appId, tagIdList);
    }

    @Override
    public Long insertNewTag(String username, TagDTO tag) {
        tag.setCreator(username);
        tag.setLastModifyUser(username);
        checkRequiredParam(tag);

        boolean isTagExist = tagDAO.isExistDuplicateName(tag.getAppId(), tag.getName());
        if (isTagExist) {
            throw new ServiceException(ErrorCode.TAG_ALREADY_EXIST);
        }
        return tagDAO.insertTag(tag);
    }

    @Override
    public boolean updateTagById(String username, TagDTO tag) {
        if (tag.getId() == null || tag.getId() <= 0) {
            throw new ParamErrorException(ErrorCode.ILLEGAL_PARAM);
        }
        tag.setAppId(tag.getAppId());
        tag.setId(tag.getId());
        tag.setLastModifyUser(username);
        checkRequiredParam(tag);

        boolean isTagNameValid = checkTagName(tag.getAppId(), tag.getId(), tag.getName());
        if (!isTagNameValid) {
            throw new ServiceException(ErrorCode.TAG_ALREADY_EXIST);
        }
        return tagDAO.updateTagById(tag);
    }

    private void checkRequiredParam(TagDTO tag) {
        if (tag.getAppId() == null || tag.getAppId() <= 0) {
            throw new ParamErrorException(ErrorCode.WRONG_APP_ID);
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
            throw new ParamErrorException(ErrorCode.ILLEGAL_PARAM);
        }
        if (StringUtils.isAllBlank(tag.getCreator(), tag.getLastModifyUser())) {
            throw new ParamErrorException(ErrorCode.MISSING_PARAM);
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
    @Transactional
    public void deleteTag(Long tagId) {
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
        tags.forEach(tag -> {
            if (!tag.getAppId().equals(appId)) {
                throw new ServiceException("Tag is not exist");
            }
        });
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
    @Transactional(rollbackFor = {Exception.class, Error.class})
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
                v+= 1L;
            }
            return v;
        }));
        return tagAndResourceCountMap;
    }

    @Override
    public List<String> listAppTaggedResourceIds(Long appId, Integer resourceType) {
        List<Long> allAppTagIds = tagDAO.listTagsByAppId(appId).stream().map(TagDTO::getId).collect(Collectors.toList());
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
            return !tagDAO.isExistDuplicateName(appId, name);
        } else {
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
