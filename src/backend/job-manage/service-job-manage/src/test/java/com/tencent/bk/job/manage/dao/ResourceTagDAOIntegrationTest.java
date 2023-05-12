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

import com.tencent.bk.job.common.constant.JobResourceTypeEnum;
import com.tencent.bk.job.manage.model.dto.ResourceTagDTO;
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

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:test.properties")
@Sql(value = {"/init_resource_tag_data.sql"})
@SqlConfig(encoding = "utf-8")
public class ResourceTagDAOIntegrationTest {

    @Autowired
    private ResourceTagDAO resourceTagDAO;

    @Test
    public void listResourceTagsByResourceIds() {
        List<String> scriptResourceIdList = new ArrayList<>();
        scriptResourceIdList.add("1");
        List<ResourceTagDTO> scriptResourceTags =
            resourceTagDAO.listResourceTags(JobResourceTypeEnum.APP_SCRIPT.getValue(), scriptResourceIdList);
        assertThat(scriptResourceTags).hasSize(2);
        assertThat(scriptResourceTags).extracting("resourceType").containsOnly(JobResourceTypeEnum.APP_SCRIPT.getValue());
        assertThat(scriptResourceTags).extracting("resourceId").containsOnly("1");
        assertThat(scriptResourceTags).extracting("tagId").containsOnly(1L, 2L);

        List<String> templateResourceIdList = new ArrayList<>();
        templateResourceIdList.add("1");
        templateResourceIdList.add("2");
        List<ResourceTagDTO> templateResourceTags =
            resourceTagDAO.listResourceTags(JobResourceTypeEnum.TEMPLATE.getValue(), templateResourceIdList);
        assertThat(templateResourceTags).hasSize(3);
        assertThat(templateResourceTags).extracting("resourceType").containsOnly(JobResourceTypeEnum.TEMPLATE.getValue());
        assertThat(templateResourceTags).extracting("resourceId").containsOnly("1", "2");
        assertThat(templateResourceTags).extracting("tagId").containsOnly(1L, 2L);
    }

    @Test
    public void listResourceTagsByResourceId() {
        List<ResourceTagDTO> scriptResourceTags =
            resourceTagDAO.listResourceTags(JobResourceTypeEnum.APP_SCRIPT.getValue(), String.valueOf(1));
        assertThat(scriptResourceTags).hasSize(2);
        assertThat(scriptResourceTags).extracting("resourceType").containsOnly(JobResourceTypeEnum.APP_SCRIPT.getValue());
        assertThat(scriptResourceTags).extracting("resourceId").containsOnly("1");
        assertThat(scriptResourceTags).extracting("tagId").containsOnly(1L, 2L);
    }

    @Test
    public void listResourceTagsByTagId() {
        List<ResourceTagDTO> resourceTags = resourceTagDAO.listResourceTags(1L);
        assertThat(resourceTags).hasSize(4);
        assertThat(resourceTags).extracting("resourceType").containsOnly(JobResourceTypeEnum.APP_SCRIPT.getValue(), JobResourceTypeEnum.TEMPLATE.getValue());
        assertThat(resourceTags).extracting("resourceId").containsOnly("1", "2");
        assertThat(resourceTags).extracting("tagId").containsOnly(1L);
    }

    @Test
    public void listResourceTagsByTagIdAndResourceType() {
        List<ResourceTagDTO> resourceTags = resourceTagDAO.listResourceTags(1L, JobResourceTypeEnum.APP_SCRIPT.getValue());
        assertThat(resourceTags).hasSize(2);
        assertThat(resourceTags).extracting("resourceType").containsOnly(JobResourceTypeEnum.APP_SCRIPT.getValue());
        assertThat(resourceTags).extracting("resourceId").containsOnly("1", "2");
        assertThat(resourceTags).extracting("tagId").containsOnly(1L);
    }

    @Test
    public void listResourceTagsByTagIdS() {
        List<ResourceTagDTO> resourceTags = resourceTagDAO.listResourceTags(Arrays.asList(1L, 2L));
        assertThat(resourceTags).hasSize(6);
        assertThat(resourceTags).extracting("resourceType").containsOnly(JobResourceTypeEnum.APP_SCRIPT.getValue(), JobResourceTypeEnum.TEMPLATE.getValue());
        assertThat(resourceTags).extracting("resourceId").containsOnly("1", "2");
        assertThat(resourceTags).extracting("tagId").containsOnly(1L, 2L);
    }

    @Test
    public void batchSaveResourceTags() {
        List<ResourceTagDTO> resourceTags = new ArrayList<>();
        ResourceTagDTO resourceTag1 = new ResourceTagDTO(JobResourceTypeEnum.APP_SCRIPT.getValue(), "1000", 1L);
        ResourceTagDTO resourceTag2 = new ResourceTagDTO(JobResourceTypeEnum.APP_SCRIPT.getValue(), "1000", 2L);
        ResourceTagDTO resourceTag3 = new ResourceTagDTO(JobResourceTypeEnum.APP_SCRIPT.getValue(), "1001", 1L);
        ResourceTagDTO resourceTag4 = new ResourceTagDTO(JobResourceTypeEnum.TEMPLATE.getValue(), "1000", 1L);
        ResourceTagDTO resourceTag5 = new ResourceTagDTO(JobResourceTypeEnum.TEMPLATE.getValue(), "1000", 2L);
        ResourceTagDTO resourceTag6 = new ResourceTagDTO(JobResourceTypeEnum.TEMPLATE.getValue(), "1001", 1L);
        resourceTags.add(resourceTag1);
        resourceTags.add(resourceTag2);
        resourceTags.add(resourceTag3);
        resourceTags.add(resourceTag4);
        resourceTags.add(resourceTag5);
        resourceTags.add(resourceTag6);

        resourceTagDAO.batchSaveResourceTags(resourceTags);

        List<String> scriptResourceIdList = new ArrayList<>();
        scriptResourceIdList.add("1000");
        scriptResourceIdList.add("1001");
        List<ResourceTagDTO> scriptResourceTags =
            resourceTagDAO.listResourceTags(JobResourceTypeEnum.APP_SCRIPT.getValue(), scriptResourceIdList);
        assertThat(scriptResourceTags).hasSize(3);
        assertThat(scriptResourceTags).extracting("resourceType").containsOnly(JobResourceTypeEnum.APP_SCRIPT.getValue());
        assertThat(scriptResourceTags).extracting("resourceId").containsOnly("1000", "1001");
        assertThat(scriptResourceTags).extracting("tagId").containsOnly(1L, 2L);

        List<String> templateResourceIdList = new ArrayList<>();
        templateResourceIdList.add("1000");
        templateResourceIdList.add("1001");
        List<ResourceTagDTO> templateResourceTags =
            resourceTagDAO.listResourceTags(JobResourceTypeEnum.TEMPLATE.getValue(), templateResourceIdList);
        assertThat(templateResourceTags).hasSize(3);
        assertThat(templateResourceTags).extracting("resourceType").containsOnly(JobResourceTypeEnum.TEMPLATE.getValue());
        assertThat(templateResourceTags).extracting("resourceId").containsOnly("1000", "1001");
        assertThat(templateResourceTags).extracting("tagId").containsOnly(1L, 2L);

    }

    @Test
    public void givenDuplicateKeyWhenBatchSaveResourceTagsThenSuccess() {
        List<ResourceTagDTO> resourceTags = new ArrayList<>();
        ResourceTagDTO resourceTag1 = new ResourceTagDTO(JobResourceTypeEnum.APP_SCRIPT.getValue(), "1000", 1L);
        ResourceTagDTO resourceTag2 = new ResourceTagDTO(JobResourceTypeEnum.APP_SCRIPT.getValue(), "1000", 1L);
        resourceTags.add(resourceTag1);
        resourceTags.add(resourceTag2);

        resourceTagDAO.batchSaveResourceTags(resourceTags);

        List<String> scriptResourceIdList = new ArrayList<>();
        scriptResourceIdList.add("1000");
        List<ResourceTagDTO> scriptResourceTags =
            resourceTagDAO.listResourceTags(JobResourceTypeEnum.APP_SCRIPT.getValue(), scriptResourceIdList);
        assertThat(scriptResourceTags).hasSize(1);
        assertThat(scriptResourceTags).extracting("resourceType").containsOnly(JobResourceTypeEnum.APP_SCRIPT.getValue());
        assertThat(scriptResourceTags).extracting("resourceId").containsOnly("1000");
        assertThat(scriptResourceTags).extracting("tagId").containsOnly(1L);
    }

    @Test
    public void deleteResourceTagsByResourceId() {
        boolean result = resourceTagDAO.deleteResourceTags(JobResourceTypeEnum.APP_SCRIPT.getValue(), "1");
        assertThat(result).isEqualTo(true);

        List<ResourceTagDTO> tags = resourceTagDAO.listResourceTags(JobResourceTypeEnum.APP_SCRIPT.getValue(), Collections.singletonList("1"));
        assertThat(tags).hasSize(0);
    }

    @Test
    public void deleteResourceTagByResourceAndTagId() {
        boolean result = resourceTagDAO.deleteResourceTag(JobResourceTypeEnum.APP_SCRIPT.getValue(), "1", 1L);
        assertThat(result).isEqualTo(true);
    }

    @Test
    public void deleteResourceTagsByResourceAndTagIds() {
        boolean result = resourceTagDAO.deleteResourceTags(JobResourceTypeEnum.APP_SCRIPT.getValue(), "1", Collections.singletonList(1L));
        assertThat(result).isEqualTo(true);

        List<ResourceTagDTO> tags = resourceTagDAO.listResourceTags(JobResourceTypeEnum.APP_SCRIPT.getValue(), Collections.singletonList("1"));
        assertThat(tags).hasSize(1);
        assertThat(tags.get(0).getTagId()).isEqualTo(2L);
    }


}
