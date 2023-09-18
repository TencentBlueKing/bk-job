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

package com.tencent.bk.job.manage.dao;

import com.tencent.bk.job.common.constant.Order;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 29/9/2019 16:32
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:test.properties")
@Sql(value = {"/init_tag_data.sql"})
@SqlConfig(encoding = "utf-8")
public class TagDAOIntegrationTest {

    @Autowired
    private TagDAO tagDAO;

    @Test
    public void getTagById() {
        TagDTO tag = tagDAO.getTagById(1L, 1L);
        assertThat(tag).isNotNull();
        assertThat(tag.getAppId()).isEqualTo(1L);
        assertThat(tag.getId()).isEqualTo(1L);
        assertThat(tag.getName()).isEqualTo("test1");
        assertThat(tag.getCreator()).isEqualTo("userC");
        assertThat(tag.getLastModifyUser()).isEqualTo("userT");
        assertThat(tag.getDescription()).isEqualTo("test1-desc");
        assertThat(tag.getCreateTime()).isEqualTo(1630648088L);
        assertThat(tag.getLastModifyTime()).isEqualTo(1630648088L);

    }

    @Test
    public void whenTagNotExistThenReturnNull() {
        // id not exist
        assertThat(tagDAO.getTagById(1L, 99999L)).isNull();
        // app not exist
        assertThat(tagDAO.getTagById(13L, 1L)).isNull();
    }

    @Test
    public void listTagsByIds() {
        List<Long> tagIdList = Arrays.asList(1L, 2L, 3L, 5L, 29L, -1L);
        List<TagDTO> result = tagDAO.listTagsByIds(1L, tagIdList);
        assertThat(result).hasSize(2);
        assertThat(result).extracting("id").containsOnly(1L, 2L);
    }

    @Test
    public void listTagsByTagIds() {
        List<Long> tagIdList = Arrays.asList(1L, 2L, 3L);
        List<TagDTO> result = tagDAO.listTagsByIds(tagIdList);
        assertThat(result).hasSize(3);
        assertThat(result).extracting("id").containsOnly(1L, 2L, 3L);
    }

    @Test
    public void listTagsByAppId() {
        List<TagDTO> appTags = tagDAO.listTagsByAppId(1L);
        assertThat(appTags).extracting("appId").containsOnly(1L);

        List<TagDTO> appTagsNull = tagDAO.listTagsByAppId(99999L);
        assertThat(appTagsNull).isEmpty();
    }

    @Test
    public void insertTag() {
        TagDTO newTag = new TagDTO();
        newTag.setAppId(3L);
        newTag.setName("test8");
        newTag.setCreator("userC");
        newTag.setLastModifyUser("userT");
        newTag.setDescription("test8-desc");
        newTag.setCreateTime(DateUtils.currentTimeSeconds());
        newTag.setLastModifyTime(DateUtils.currentTimeSeconds());
        newTag.setId(tagDAO.insertTag(newTag));

        TagDTO savedTag = tagDAO.getTagById(3L, newTag.getId());
        assertTag(savedTag, newTag);
    }

    private void assertTag(TagDTO tag, TagDTO expected) {
        assertThat(tag).isNotNull();
        assertThat(tag.getAppId()).isEqualTo(expected.getAppId());
        assertThat(tag.getId()).isEqualTo(expected.getId());
        assertThat(tag.getName()).isEqualTo(expected.getName());
        assertThat(tag.getCreator()).isEqualTo(expected.getCreator());
        assertThat(tag.getLastModifyUser()).isEqualTo(expected.getLastModifyUser());
        assertThat(tag.getDescription()).isEqualTo(expected.getDescription());
        assertThat(tag.getCreateTime()).isEqualTo(expected.getCreateTime());
        assertThat(tag.getLastModifyTime()).isEqualTo(expected.getLastModifyTime());
    }

    @Test
    public void updateTagById() {
        TagDTO updateTag = new TagDTO();
        updateTag.setAppId(2L);
        updateTag.setId(3L);
        updateTag.setName("New Name");
        updateTag.setDescription("New Description");
        updateTag.setLastModifyUser("userC");
        assertThat(tagDAO.updateTagById(updateTag)).isTrue();

        TagDTO result = tagDAO.getTagById(updateTag.getAppId(), updateTag.getId());
        assertThat(result.getName()).isEqualTo(updateTag.getName());
        assertThat(result.getLastModifyUser()).isEqualTo(updateTag.getLastModifyUser());
        assertThat(result.getDescription()).isEqualTo(updateTag.getDescription());


        // Tag not exist in current application
        updateTag.setAppId(1L);
        assertThat(tagDAO.updateTagById(updateTag)).isFalse();
        // Tag Not exist
        updateTag.setId(9999L);
        assertThat(tagDAO.updateTagById(updateTag)).isFalse();
    }

    @Test
    @DisplayName("验证根据标签查询标签列表")
    public void whenListTagsThenReturnTags() {
        Long appId = 2L;
        String keyword = "test";
        TagDTO searchCondition = new TagDTO();
        searchCondition.setAppId(appId);
        searchCondition.setName(keyword);

        List<TagDTO> tags = tagDAO.listTags(searchCondition);

        assertThat(tags).hasSize(2).extracting("id").containsOnly(3L, 4L);
        assertThat(tags).extracting("name").contains("test1", "test2");
    }

    @Test
    @DisplayName("验证分页查询")
    public void listPageTags() {
        TagDTO tagQuery = new TagDTO();
        tagQuery.setAppId(2L);
        tagQuery.setName("test");

        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(0);
        baseSearchCondition.setLength(10);
        baseSearchCondition.setOrder(Order.DESCENDING.getOrder());
        baseSearchCondition.setOrderField("lastModifyTime");

        PageData<TagDTO> pageData = tagDAO.listPageTags(tagQuery, baseSearchCondition);
        assertThat(pageData.getData()).hasSize(2);

        TagDTO tag1 = pageData.getData().get(0);
        assertThat(tag1.getAppId()).isEqualTo(2L);
        assertThat(tag1.getId()).isGreaterThan(0L);
        assertThat(tag1.getName()).isEqualTo("test2");
        assertThat(tag1.getCreator()).isEqualTo("userC");
        assertThat(tag1.getLastModifyUser()).isEqualTo("userT");
        assertThat(tag1.getDescription()).isEqualTo("test2-desc");
        assertThat(tag1.getCreateTime()).isEqualTo(1630648091);
        assertThat(tag1.getLastModifyTime()).isEqualTo(1630648091);

        TagDTO tag2 = pageData.getData().get(1);
        assertThat(tag2.getAppId()).isEqualTo(2L);
        assertThat(tag2.getId()).isGreaterThan(0L);
        assertThat(tag2.getName()).isEqualTo("test1");
        assertThat(tag2.getCreator()).isEqualTo("userC");
        assertThat(tag2.getLastModifyUser()).isEqualTo("userT");
        assertThat(tag2.getDescription()).isEqualTo("test1-desc");
        assertThat(tag2.getCreateTime()).isEqualTo(1630648090);
        assertThat(tag2.getLastModifyTime()).isEqualTo(1630648090);
    }

    @Test
    @DisplayName("删除标签")
    public void deleteTag() {
        assertThat(tagDAO.deleteTagById(1L)).isEqualTo(true);
        assertThat(tagDAO.deleteTagById(999999L)).isEqualTo(false);
    }

    @Test
    @DisplayName("检查标签名称是否重复")
    public void testCheckDuplicateTagName() {
        assertThat(tagDAO.isExistDuplicateName(1L, "test1")).isEqualTo(true);
        assertThat(tagDAO.isExistDuplicateName(1L, "new-tag")).isEqualTo(false);
        assertThat(tagDAO.isExistDuplicateName(1L, "test3")).isEqualTo(false);
    }


}
