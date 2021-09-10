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
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.util.TagUtils;
import com.tencent.bk.job.common.util.check.*;
import com.tencent.bk.job.common.util.check.exception.StringCheckException;
import com.tencent.bk.job.manage.dao.TagDAO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.service.TagService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @since 30/9/2019 16:48
 */
@Slf4j
@Service
public class TagServiceImpl implements TagService {
    private TagDAO tagDAO;

    @Autowired
    public TagServiceImpl(TagDAO tagDAO) {
        this.tagDAO = tagDAO;
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
    public Long insertNewTag(Long appId, String tagName, String username) {
        TagDTO tagDTO = new TagDTO();
        tagDTO.setAppId(appId);
        tagDTO.setName(tagName);
        tagDTO.setCreator(username);
        tagDTO.setLastModifyUser(username);
        checkRequiredParam(tagDTO);
        return tagDAO.insertTag(tagDTO);
    }

    @Override
    public Boolean updateTagById(Long appId, Long tagId, String tagName, String username) {
        if (tagId == null || tagId <= 0) {
            throw new ParamErrorException(ErrorCode.ILLEGAL_PARAM);
        }
        TagDTO tagDTO = new TagDTO();
        tagDTO.setAppId(appId);
        tagDTO.setId(tagId);
        tagDTO.setName(tagName);
        tagDTO.setLastModifyUser(username);
        checkRequiredParam(tagDTO);
        return tagDAO.updateTagById(tagDTO);
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
    public String getFormattedTagNames(Long appId, String tagIdsStr) {
        if (StringUtils.isBlank(tagIdsStr)) {
            return "";
        }
        List<Long> tagIds = TagUtils.decodeDbTag(tagIdsStr);
        List<TagDTO> tagDTOS = tagDAO.listTagsByIds(appId, tagIds);
        List<String> tagNameList = new ArrayList<>();
        for (Long tagId : tagIds) {
            for (TagDTO tagDTO : tagDTOS) {
                if (tagDTO.getId().equals(tagId)) {
                    tagNameList.add(tagDTO.getName());
                }
            }
        }
        String tagNames = String.join(",", tagNameList);
        return tagNames;
    }

    @Override
    public Map<String, String> batchGetFormattedTagNames(Long appId, List<String> tagIdsStrList) {
        if (tagIdsStrList == null || tagIdsStrList.isEmpty()) {
            return Collections.EMPTY_MAP;
        }
        List<TagDTO> tagsInApp = tagDAO.listTagsByAppId(appId);

        Map<Long, String> tagIdNameMap = new HashMap<>();
        for (TagDTO tag : tagsInApp) {
            tagIdNameMap.put(tag.getId(), tag.getName());
        }

        Map<String, String> tagsAndNamesMap = new HashMap();
        for (String tagIdsStr : tagIdsStrList) {
            List<String> tagNameList = new ArrayList();
            List<Long> tagIdList = TagUtils.decodeDbTag(tagIdsStr);
            for (Long tagId : tagIdList) {
                tagNameList.add(tagIdNameMap.get(tagId));
            }
            String tagNamesStr = String.join(",", tagNameList);
            tagsAndNamesMap.put(tagIdsStr, tagNamesStr);
        }
        return tagsAndNamesMap;
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
    public PageData<TagDTO> listTags(TagDTO tagCondition, BaseSearchCondition baseSearchCondition){
        return tagDAO.listTags(tagCondition,baseSearchCondition);
    }
}
