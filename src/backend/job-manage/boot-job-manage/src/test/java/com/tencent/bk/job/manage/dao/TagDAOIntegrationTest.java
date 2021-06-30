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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
        assertThat(tagDAO.getTagById(1L, 1L)).isEqualTo(new TagDTO(1L, 1L, "测试1", "userC", "userT"));
        assertThat(tagDAO.getTagById(1L, 99999L)).isNull();
        assertThat(tagDAO.getTagById(13L, 1L)).isNull();
    }

    @Test
    public void listTagsByIds() {
        List<Long> tagIdList = Arrays.asList(1L, 2L, 3L, 5L, 29L, -1L);
        List<TagDTO> result = tagDAO.listTagsByIds(1L, tagIdList);
        List<TagDTO> expect = new ArrayList<>();
        expect.add(new TagDTO(1L, 1L, "测试1", "userC", "userT"));
        expect.add(new TagDTO(2L, 1L, "测试2", "userC", "userT"));
        assertThat(result).isEqualTo(expect);
    }

    @Test
    public void testListTagsByIds() {
        List<TagDTO> result = tagDAO.listTagsByIds(1L, 1L, 2L, 3L, 5L, 29L, -1L);
        List<TagDTO> expect = new ArrayList<>();
        expect.add(new TagDTO(1L, 1L, "测试1", "userC", "userT"));
        expect.add(new TagDTO(2L, 1L, "测试2", "userC", "userT"));
        assertThat(result).isEqualTo(expect);
    }

    @Test
    public void listTagsByAppId() {
        List<TagDTO> appTags = tagDAO.listTagsByAppId(1L);
        List<TagDTO> expect = new ArrayList<>();
        expect.add(new TagDTO(1L, 1L, "测试1", "userC", "userT"));
        expect.add(new TagDTO(2L, 1L, "测试2", "userC", "userT"));
        assertThat(appTags).isEqualTo(expect);

        List<TagDTO> appTags2 = tagDAO.listTagsByAppId(2L);
        List<TagDTO> expect2 = new ArrayList<>();
        expect2.add(new TagDTO(5L, 2L, "持续集成", "userC", "userT"));
        expect2.add(new TagDTO(3L, 2L, "测试1", "userC", "userT"));
        expect2.add(new TagDTO(4L, 2L, "测试2", "userC", "userT"));
        assertThat(appTags2).isEqualTo(expect2);

        List<TagDTO> appTagsNull = tagDAO.listTagsByAppId(99999L);
        assertThat(appTagsNull).isEqualTo(Collections.emptyList());
    }

    @Test
    public void insertTag() {
        TagDTO newTag = new TagDTO();
        newTag.setAppId(3L);
        newTag.setName("测试");
        newTag.setCreator("userC");
        newTag.setLastModifyUser("userT");
        newTag.setId(tagDAO.insertTag(newTag));
        assertThat(tagDAO.getTagById(3L, newTag.getId())).isEqualTo(newTag);

        TagDTO newTag2 = new TagDTO();
        newTag2.setAppId(3L);
        newTag2.setName("测试2");
        newTag2.setCreator("userC");
        newTag2.setId(tagDAO.insertTag(newTag2));

        newTag2.setLastModifyUser(newTag2.getCreator());
        assertThat(tagDAO.getTagById(3L, newTag2.getId())).isEqualTo(newTag2);
    }

    @Test
    public void updateTagById() {
        TagDTO updateTag = new TagDTO();
        updateTag.setAppId(2L);
        updateTag.setId(3L);
        updateTag.setName("New Name");
        updateTag.setLastModifyUser("userC");
        assertThat(tagDAO.updateTagById(updateTag)).isTrue();
        TagDTO result = tagDAO.getTagById(updateTag.getAppId(), updateTag.getId());
        assertThat(result.getName()).isEqualTo(updateTag.getName());
        assertThat(result.getLastModifyUser()).isEqualTo(updateTag.getLastModifyUser());
        updateTag.setAppId(1L);
        assertThat(tagDAO.updateTagById(updateTag)).isFalse();
        updateTag.setId(9999L);
        assertThat(tagDAO.updateTagById(updateTag)).isFalse();
    }

    @Test
    @DisplayName("验证根据标签查询标签列表")
    public void whenListTagsThenReturnTags() {
        Long appId = 2L;
        String keyword = "测试";
        TagDTO searchConditon = new TagDTO();
        searchConditon.setAppId(appId);
        searchConditon.setName(keyword);

        List<TagDTO> tags = tagDAO.listTags(searchConditon);

        assertThat(tags).hasSize(2).extracting("id").containsOnly(3L, 4L);
        assertThat(tags).extracting("name").contains("测试1", "测试2");
    }
}
