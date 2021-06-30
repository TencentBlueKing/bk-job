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

import com.tencent.bk.job.manage.dao.TagDAO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.service.TagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

/**
 * @since 30/9/2019 18:05
 */
public class TagServiceImplTest {

    private static final TagDTO appOneNoOneTag = new TagDTO(1L, 1L, "测试1", "userC", "userT");
    private static final TagDTO appOneNoTwoTag = new TagDTO(2L, 1L, "测试2", "userC", "userT");
    private TagDAO tagDAO;
    private TagService tagService;

    @BeforeEach
    public void initMock() {
        this.tagDAO = mock(TagDAO.class);
        this.tagService = new TagServiceImpl(tagDAO);

        when(tagDAO.getTagById(1L, 1L)).thenReturn(appOneNoOneTag);
        when(tagDAO.getTagById(1L, 2L)).thenReturn(appOneNoTwoTag);

        when(tagDAO.listTagsByAppId(1L)).thenReturn(new ArrayList<>(Arrays.asList(appOneNoOneTag, appOneNoTwoTag)));
    }

    @Test
    public void getTagInfoById() {
        TagDTO tagDTO = tagService.getTagInfoById(1L, 1L);
        assertThat(tagDTO).isEqualTo(appOneNoOneTag);
        verify(tagDAO).getTagById(1L, 1L);

        tagDTO = tagService.getTagInfoById(2L, 1L);
        assertThat(tagDTO).isNull();
        verify(tagDAO).getTagById(2L, 1L);
    }

    @Test
    public void listTagsByAppId() {
        List<TagDTO> tagList = tagService.listTagsByAppId(1L);
        assertThat(tagList).isEqualTo(Arrays.asList(appOneNoOneTag, appOneNoTwoTag));
        verify(tagDAO).listTagsByAppId(1L);

        tagList = tagService.listTagsByAppId(2L);
        assertThat(tagList).isEqualTo(Collections.emptyList());
        verify(tagDAO).listTagsByAppId(2L);
    }

    @Test
    public void whenListTagsByAppIdAndTagIdListThenReturn() {
        this.tagDAO = mock(TagDAO.class);
        this.tagService = new TagServiceImpl(tagDAO);

        List<Long> tagList = new ArrayList();
        tagList.add(1L);
        tagList.add(2L);
        Long appId = 1L;
        tagService.listTagsByAppIdAndTagIdList(appId, tagList);

        verify(tagDAO).listTagsByIds(appId, tagList);
    }

    @Test
    public void insertNewTag() {
        tagService.insertNewTag(1L, "测试3", "userC");
        TagDTO insertTag = new TagDTO();
        insertTag.setAppId(1L);
        insertTag.setName("测试3");
        insertTag.setCreator("userC");
        insertTag.setLastModifyUser("userC");
        verify(tagDAO).insertTag(insertTag);
    }

    @Test
    public void updateTagById() {
        tagService.updateTagById(1L, 1L, "NewName", "userC");
        TagDTO insertTag = new TagDTO();
        insertTag.setAppId(1L);
        insertTag.setId(1L);
        insertTag.setName("NewName");
        insertTag.setLastModifyUser("userC");
        verify(tagDAO).updateTagById(insertTag);
    }

    @Test
    public void givenEmptyTagIdsStrWhenGetFormattedTagNamesThenReturnEmptyStr() {
        String formattedTagNames = tagService.getFormattedTagNames(2L, null);
        assertThat(formattedTagNames).as("check formatted tag names").isEmpty();
        formattedTagNames = tagService.getFormattedTagNames(2L, "");
        assertThat(formattedTagNames).as("check formatted tag names").isEmpty();
    }

    @Test
    public void whenGetFormattedTagNamesThenReturn() {
        List<Long> tagIdList = new ArrayList();
        tagIdList.add(1L);
        tagIdList.add(2L);
        List<TagDTO> returnTagsByDAO = new ArrayList();
        TagDTO tag1 = new TagDTO();
        tag1.setAppId(2L);
        tag1.setId(1L);
        tag1.setName("tag1");
        TagDTO tag2 = new TagDTO();
        tag2.setAppId(2L);
        tag2.setId(2L);
        tag2.setName("tag2");
        returnTagsByDAO.add(tag1);
        returnTagsByDAO.add(tag2);
        when(tagDAO.listTagsByIds(2L, tagIdList)).thenReturn(returnTagsByDAO);

        String formattedTagNames = tagService.getFormattedTagNames(2L, "<1>,<2>");
        assertThat(formattedTagNames).as("check formatted tag names").isEqualTo("tag1,tag2");
    }

    @Test
    public void givenEmptyTagIdsStrListWhenBatchGetFormattedTagNamesThenReturnEmptyMap() {
        Map<String, String> formattedTagNamesMap = tagService.batchGetFormattedTagNames(2L, Collections.emptyList());
        assertThat(formattedTagNamesMap).isEmpty();
        formattedTagNamesMap = tagService.batchGetFormattedTagNames(2L, null);
        assertThat(formattedTagNamesMap).isEmpty();
    }

    @Test
    public void whenBatchGetFormattedTagNamesThenReturn() {
        List<TagDTO> returnTagsByDAO = new ArrayList();
        TagDTO tag1 = new TagDTO();
        tag1.setAppId(2L);
        tag1.setId(1L);
        tag1.setName("tag1");
        TagDTO tag2 = new TagDTO();
        tag2.setAppId(2L);
        tag2.setId(2L);
        tag2.setName("tag2");
        TagDTO tag3 = new TagDTO();
        tag3.setAppId(2L);
        tag3.setId(3L);
        tag3.setName("tag3");
        returnTagsByDAO.add(tag1);
        returnTagsByDAO.add(tag2);
        returnTagsByDAO.add(tag3);
        when(tagDAO.listTagsByAppId(2L)).thenReturn(returnTagsByDAO);

        Long appId = 2L;
        List<String> tagIdsStrList = new ArrayList();
        tagIdsStrList.add("<1>,<2>");
        tagIdsStrList.add("<2>,<3>");

        Map<String, String> formattedTagNamesMap = tagService.batchGetFormattedTagNames(appId, tagIdsStrList);

        Map<String, String> expectedFormattedTagNamesMap = new HashMap<>();
        expectedFormattedTagNamesMap.put("<1>,<2>", "tag1,tag2");
        expectedFormattedTagNamesMap.put("<2>,<3>", "tag2,tag3");
        assertThat(formattedTagNamesMap).as("check formatted tag names map").isEqualTo(expectedFormattedTagNamesMap);
    }

    @Test
    @DisplayName("标签全部存在时，返回标签列表")
    public void givenExistTagsWhenCreateNewTagIfNotExistThenReturnTags() {
        List<TagDTO> tags = new ArrayList();
        TagDTO tag1 = new TagDTO();
        tag1.setAppId(2L);
        tag1.setId(1L);
        tag1.setName("tag1");
        TagDTO tag2 = new TagDTO();
        tag2.setAppId(2L);
        tag2.setId(2L);
        tag2.setName("tag2");
        tags.add(tag1);
        tags.add(tag2);

        List<TagDTO> actualTags = tagService.createNewTagIfNotExist(tags, 2L, "user1");

        assertThat(actualTags).containsSequence(tags);
    }

    @Test
    @DisplayName("标签部分存在时，创建新的标签，返回标签列表")
    public void givenNewTagsWhenCreateNewTagIfNotExistThenCreatedAndReturn() {
        List<TagDTO> tags = new ArrayList();
        TagDTO tag1 = new TagDTO();
        tag1.setId(1L);
        tag1.setName("tag1");
        // tag2，没有id，新增标签
        TagDTO tag2 = new TagDTO();
        tag2.setName("tag2");
        tags.add(tag1);
        tags.add(tag2);

        when(tagDAO.insertTag(any())).thenReturn(2L);

        List<TagDTO> actualTags = tagService.createNewTagIfNotExist(tags, 2L, "user1");

        verify(tagDAO).insertTag(any());
        assertThat(actualTags).extracting("id").containsSequence(1L, 2L);
    }

    @Test
    @DisplayName("测试根据标签名称返回标签列表")
    public void whenListTagsByNameThenReturn() {
        Long appId = 2L;
        String keyword = "test";

        List<TagDTO> tagsReturnByDAO = new ArrayList<>();
        TagDTO tag1 = new TagDTO();
        tag1.setId(appId);
        tag1.setName("test");
        tag1.setId(1L);
        tagsReturnByDAO.add(tag1);

        TagDTO searchCondition = new TagDTO();
        searchCondition.setAppId(appId);
        searchCondition.setName(keyword);
        when(tagDAO.listTags(searchCondition)).thenReturn(tagsReturnByDAO);

        List<TagDTO> tags = tagService.listTags(appId, keyword);
        assertThat(tags).hasSize(1).containsOnly(tag1);
    }
}
