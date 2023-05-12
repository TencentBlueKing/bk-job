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

package com.tencent.bk.job.manage.dao.template.impl;

import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.manage.common.consts.task.TaskTemplateStatusEnum;
import com.tencent.bk.job.manage.dao.template.TaskTemplateDAO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 11/10/2019 21:37
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:test.properties")
@Sql(value = {"/template/init_template_data.sql"})
@SqlConfig(encoding = "utf-8")
class TaskTemplateDAOImplIntegrationTest {

    private static final Random random = new Random();
    private static final TaskTemplateInfoDTO TEMPLATE_INFO_1 = new TaskTemplateInfoDTO();
    private static final TaskTemplateInfoDTO TEMPLATE_INFO_2 = new TaskTemplateInfoDTO();
    private static final TaskTemplateInfoDTO TEMPLATE_INFO_3 = new TaskTemplateInfoDTO();
    private static final TaskTemplateInfoDTO TEMPLATE_INFO_4 = new TaskTemplateInfoDTO();
    private static final TaskTemplateInfoDTO TEMPLATE_INFO_5 = new TaskTemplateInfoDTO();
    private static final TaskTemplateInfoDTO TEMPLATE_INFO_6 = new TaskTemplateInfoDTO();
    private static final TaskTemplateInfoDTO TEMPLATE_INFO_7 = new TaskTemplateInfoDTO();
    private static final TaskTemplateInfoDTO TEMPLATE_INFO_8 = new TaskTemplateInfoDTO();

    @Autowired
    private TaskTemplateDAO taskTemplateDAO;

    @BeforeEach
    void initTest() {
        TEMPLATE_INFO_1.setId(1L);
        TEMPLATE_INFO_1.setAppId(1000L);
        TEMPLATE_INFO_1.setName("测试模版1");
        TEMPLATE_INFO_1.setDescription("这是一个测试模版。这段描述是随便写的，没什么意义。1");
        TEMPLATE_INFO_1.setCreator("userC");
        TEMPLATE_INFO_1.setStatus(TaskTemplateStatusEnum.NEW);
        TEMPLATE_INFO_1.setCreateTime(LocalDateTime.of(2019, 10, 1, 0, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        TEMPLATE_INFO_1.setLastModifyUser("userC");
        TEMPLATE_INFO_1
            .setLastModifyTime(LocalDateTime.of(2019, 10, 1, 0, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        TEMPLATE_INFO_1.setTags(Stream.of(1L, 2L, 3L).map(tagId -> {
            TagDTO tagInfo = new TagDTO();
            tagInfo.setId(tagId);
            return tagInfo;
        }).collect(Collectors.toList()));
        TEMPLATE_INFO_1.setFirstStepId(1000L);
        TEMPLATE_INFO_1.setLastStepId(2000L);
        TEMPLATE_INFO_1.setScriptStatus(1);
        TEMPLATE_INFO_1.setVersion("abcd1234");

        TEMPLATE_INFO_2.setId(2L);
        TEMPLATE_INFO_2.setAppId(1000L);
        TEMPLATE_INFO_2.setName("测试模版2");
        TEMPLATE_INFO_2.setDescription("这是一个测试模版。这段描述是随便写的，没什么意义。2");
        TEMPLATE_INFO_2.setCreator("userC");
        TEMPLATE_INFO_2.setStatus(TaskTemplateStatusEnum.IN_REVIEW);
        TEMPLATE_INFO_2.setCreateTime(LocalDateTime.of(2019, 10, 1, 0, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        TEMPLATE_INFO_2.setLastModifyUser("userT");
        TEMPLATE_INFO_2
            .setLastModifyTime(LocalDateTime.of(2019, 10, 2, 0, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        TEMPLATE_INFO_2.setTags(Stream.of(2L, 3L, 4L).map(tagId -> {
            TagDTO tagInfo = new TagDTO();
            tagInfo.setId(tagId);
            return tagInfo;
        }).collect(Collectors.toList()));
        TEMPLATE_INFO_2.setFirstStepId(2000L);
        TEMPLATE_INFO_2.setLastStepId(3000L);
        TEMPLATE_INFO_2.setScriptStatus(0);
        TEMPLATE_INFO_2.setVersion("abcd1234");

        TEMPLATE_INFO_3.setId(3L);
        TEMPLATE_INFO_3.setAppId(1000L);
        TEMPLATE_INFO_3.setName("测试模版3");
        TEMPLATE_INFO_3.setDescription("这是一个测试模版。这段描述是随便写的，没什么意义。3");
        TEMPLATE_INFO_3.setCreator("userC");
        TEMPLATE_INFO_3.setStatus(TaskTemplateStatusEnum.PUBLISHED);
        TEMPLATE_INFO_3.setCreateTime(LocalDateTime.of(2019, 10, 1, 0, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        TEMPLATE_INFO_3.setLastModifyUser("userC");
        TEMPLATE_INFO_3
            .setLastModifyTime(LocalDateTime.of(2019, 10, 8, 0, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        TEMPLATE_INFO_3.setTags(Stream.of(3L, 4L, 5L).map(tagId -> {
            TagDTO tagInfo = new TagDTO();
            tagInfo.setId(tagId);
            return tagInfo;
        }).collect(Collectors.toList()));
        TEMPLATE_INFO_3.setFirstStepId(3000L);
        TEMPLATE_INFO_3.setLastStepId(4000L);
        TEMPLATE_INFO_3.setScriptStatus(1);
        TEMPLATE_INFO_3.setVersion("abcd1234");

        TEMPLATE_INFO_4.setId(4L);
        TEMPLATE_INFO_4.setAppId(1000L);
        TEMPLATE_INFO_4.setName("测试模版4");
        TEMPLATE_INFO_4.setDescription("这是一个测试模版。这段描述是随便写的，没什么意义。4");
        TEMPLATE_INFO_4.setCreator("userT");
        TEMPLATE_INFO_4.setStatus(TaskTemplateStatusEnum.REJECTED);
        TEMPLATE_INFO_4.setCreateTime(LocalDateTime.of(2019, 10, 1, 0, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        TEMPLATE_INFO_4.setLastModifyUser("userT");
        TEMPLATE_INFO_4
            .setLastModifyTime(LocalDateTime.of(2019, 10, 4, 0, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        TEMPLATE_INFO_4.setTags(Stream.of(1L, 3L, 5L).map(tagId -> {
            TagDTO tagInfo = new TagDTO();
            tagInfo.setId(tagId);
            return tagInfo;
        }).collect(Collectors.toList()));
        TEMPLATE_INFO_4.setFirstStepId(4000L);
        TEMPLATE_INFO_4.setLastStepId(5000L);
        TEMPLATE_INFO_4.setScriptStatus(0);
        TEMPLATE_INFO_4.setVersion("abcd1234");

        TEMPLATE_INFO_5.setId(5L);
        TEMPLATE_INFO_5.setAppId(1000L);
        TEMPLATE_INFO_5.setName("测试模版5");
        TEMPLATE_INFO_5.setDescription("这是一个测试模版。这段描述是随便写的，没什么意义。5");
        TEMPLATE_INFO_5.setCreator("userT");
        TEMPLATE_INFO_5.setStatus(TaskTemplateStatusEnum.PUBLISHED);
        TEMPLATE_INFO_5.setCreateTime(LocalDateTime.of(2019, 10, 1, 0, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        TEMPLATE_INFO_5.setLastModifyUser("userC");
        TEMPLATE_INFO_5
            .setLastModifyTime(LocalDateTime.of(2019, 10, 5, 0, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        TEMPLATE_INFO_5.setTags(Stream.of(2L, 4L, 6L).map(tagId -> {
            TagDTO tagInfo = new TagDTO();
            tagInfo.setId(tagId);
            return tagInfo;
        }).collect(Collectors.toList()));
        TEMPLATE_INFO_5.setFirstStepId(5000L);
        TEMPLATE_INFO_5.setLastStepId(6000L);
        TEMPLATE_INFO_5.setScriptStatus(1);
        TEMPLATE_INFO_5.setVersion("abcd1234");

        TEMPLATE_INFO_6.setId(6L);
        TEMPLATE_INFO_6.setAppId(1000L);
        TEMPLATE_INFO_6.setName("测试模版6");
        TEMPLATE_INFO_6.setDescription("这是一个测试模版。这段描述是随便写的，没什么意义。6");
        TEMPLATE_INFO_6.setCreator("userT");
        TEMPLATE_INFO_6.setStatus(TaskTemplateStatusEnum.IN_REVIEW);
        TEMPLATE_INFO_6.setCreateTime(LocalDateTime.of(2019, 10, 1, 0, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        TEMPLATE_INFO_6.setLastModifyUser("userT");
        TEMPLATE_INFO_6
            .setLastModifyTime(LocalDateTime.of(2019, 10, 3, 1, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        TEMPLATE_INFO_6.setTags(Stream.of(1L, 4L, 7L).map(tagId -> {
            TagDTO tagInfo = new TagDTO();
            tagInfo.setId(tagId);
            return tagInfo;
        }).collect(Collectors.toList()));
        TEMPLATE_INFO_6.setFirstStepId(6000L);
        TEMPLATE_INFO_6.setLastStepId(7000L);
        TEMPLATE_INFO_6.setScriptStatus(0);
        TEMPLATE_INFO_6.setVersion("abcd1234");

        TEMPLATE_INFO_7.setId(7L);
        TEMPLATE_INFO_7.setAppId(2000L);
        TEMPLATE_INFO_7.setName("测试模版7");
        TEMPLATE_INFO_7.setDescription("这是一个测试模版。这段描述是随便写的，没什么意义。7");
        TEMPLATE_INFO_7.setCreator("userT");
        TEMPLATE_INFO_7.setStatus(TaskTemplateStatusEnum.NEW);
        TEMPLATE_INFO_7.setCreateTime(LocalDateTime.of(2019, 10, 1, 0, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        TEMPLATE_INFO_7.setLastModifyUser("userC");
        TEMPLATE_INFO_7
            .setLastModifyTime(LocalDateTime.of(2019, 10, 2, 1, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        TEMPLATE_INFO_7.setTags(Stream.of(2L, 5L, 8L).map(tagId -> {
            TagDTO tagInfo = new TagDTO();
            tagInfo.setId(tagId);
            return tagInfo;
        }).collect(Collectors.toList()));
        TEMPLATE_INFO_7.setFirstStepId(7000L);
        TEMPLATE_INFO_7.setLastStepId(8000L);
        TEMPLATE_INFO_7.setScriptStatus(1);
        TEMPLATE_INFO_7.setVersion("abcd1234");

        TEMPLATE_INFO_8.setId(8L);
        TEMPLATE_INFO_8.setAppId(2000L);
        TEMPLATE_INFO_8.setName("测试模版8");
        TEMPLATE_INFO_8.setDescription("这是一个测试模版。这段描述是随便写的，没什么意义。8");
        TEMPLATE_INFO_8.setCreator("userC");
        TEMPLATE_INFO_8.setStatus(TaskTemplateStatusEnum.PUBLISHED);
        TEMPLATE_INFO_8.setCreateTime(LocalDateTime.of(2019, 10, 1, 0, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        TEMPLATE_INFO_8.setLastModifyUser("userT");
        TEMPLATE_INFO_8
            .setLastModifyTime(LocalDateTime.of(2019, 10, 6, 0, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        TEMPLATE_INFO_8.setTags(Stream.of(3L, 6L, 9L).map(tagId -> {
            TagDTO tagInfo = new TagDTO();
            tagInfo.setId(tagId);
            return tagInfo;
        }).collect(Collectors.toList()));
        TEMPLATE_INFO_8.setFirstStepId(8000L);
        TEMPLATE_INFO_8.setLastStepId(9000L);
        TEMPLATE_INFO_8.setScriptStatus(0);
        TEMPLATE_INFO_8.setVersion("abcd1234");
    }

    @Test
    void giveNormalTemplateIdReturnTemplateInfo() {
        TaskTemplateInfoDTO template = taskTemplateDAO.getTaskTemplateById(TEMPLATE_INFO_1.getAppId(), TEMPLATE_INFO_1.getId());
        assertThat(template).isNotNull();
        assertThat(template.getId()).isEqualTo(TEMPLATE_INFO_1.getId());
        assertThat(template.getAppId()).isEqualTo(TEMPLATE_INFO_1.getAppId());
        assertThat(template.getName()).isEqualTo(TEMPLATE_INFO_1.getName());
        assertThat(template.getDescription()).isEqualTo(TEMPLATE_INFO_1.getDescription());
        assertThat(template.getCreator()).isEqualTo(TEMPLATE_INFO_1.getCreator());
        assertThat(template.getCreateTime()).isEqualTo(TEMPLATE_INFO_1.getCreateTime());
        assertThat(template.getLastModifyUser()).isEqualTo(TEMPLATE_INFO_1.getLastModifyUser());
        assertThat(template.getLastModifyTime()).isEqualTo(TEMPLATE_INFO_1.getLastModifyTime());
    }

    @Test
    void giveNotExistTemplateIdReturnNull() {
        assertThat(taskTemplateDAO.getTaskTemplateById(TEMPLATE_INFO_1.getAppId(), 9999999L)).isNull();
        assertThat(taskTemplateDAO.getTaskTemplateById(9999999L, TEMPLATE_INFO_1.getId())).isNull();
        assertThat(taskTemplateDAO.getTaskTemplateById(9999999L, 9999999L)).isNull();
    }

    private long getRandomPositiveLong() {
        long value = random.nextLong();
        if (value == Long.MIN_VALUE || value == 1L) {
            return 1;
        } else if (value < 0) {
            return -value;
        }
        return value;
    }

    @Test
    void giveTemplateInfoReturnInsertSuccess() {
        TaskTemplateInfoDTO newTaskTemplateInfo = new TaskTemplateInfoDTO();
        newTaskTemplateInfo.setAppId(1000L);
        newTaskTemplateInfo.setName(UUID.randomUUID().toString());
        newTaskTemplateInfo.setDescription(UUID.randomUUID().toString());
        newTaskTemplateInfo.setCreator("userC");
        newTaskTemplateInfo.setStatus(TaskTemplateStatusEnum.PUBLISHED);
        newTaskTemplateInfo.setCreateTime(DateUtils.currentTimeSeconds());
        newTaskTemplateInfo.setLastModifyUser("userT");
        newTaskTemplateInfo.setLastModifyTime(DateUtils.currentTimeSeconds());
        newTaskTemplateInfo.setTags(Stream.of(6L, 10L, 32L).map(tagId -> {
            TagDTO tagInfo = new TagDTO();
            tagInfo.setId(tagId);
            return tagInfo;
        }).collect(Collectors.toList()));
        newTaskTemplateInfo.setFirstStepId(getRandomPositiveLong());
        newTaskTemplateInfo.setLastStepId(getRandomPositiveLong());
        Long taskInfoId = taskTemplateDAO.insertTaskTemplate(newTaskTemplateInfo);
        newTaskTemplateInfo.setId(taskInfoId);
        TaskTemplateInfoDTO savedTaskTemplate =
            taskTemplateDAO.getTaskTemplateById(newTaskTemplateInfo.getAppId(), taskInfoId);

        assertThat(savedTaskTemplate).isNotNull();
        assertThat(savedTaskTemplate.getId()).isGreaterThan(0L);
        assertThat(savedTaskTemplate.getAppId()).isEqualTo(newTaskTemplateInfo.getAppId());
        assertThat(savedTaskTemplate.getName()).isEqualTo(newTaskTemplateInfo.getName());
        assertThat(savedTaskTemplate.getDescription()).isEqualTo(newTaskTemplateInfo.getDescription());
        assertThat(savedTaskTemplate.getCreator()).isEqualTo(newTaskTemplateInfo.getCreator());
        assertThat(savedTaskTemplate.getCreateTime()).isEqualTo(newTaskTemplateInfo.getCreateTime());
        assertThat(savedTaskTemplate.getLastModifyUser()).isEqualTo(newTaskTemplateInfo.getLastModifyUser());
        assertThat(savedTaskTemplate.getLastModifyTime()).isEqualTo(newTaskTemplateInfo.getLastModifyTime());
    }

    @Test
    void giveTemplateInfoReturnUpdateSuccess() {
        TEMPLATE_INFO_1.setName(UUID.randomUUID().toString());
        TEMPLATE_INFO_1.setLastModifyUser(UUID.randomUUID().toString());
        TEMPLATE_INFO_1.setLastModifyTime(DateUtils.currentTimeSeconds());
        assertThat(taskTemplateDAO.updateTaskTemplateById(TEMPLATE_INFO_1, true)).isTrue();
        TaskTemplateInfoDTO updatedTemplate =
            taskTemplateDAO.getTaskTemplateById(TEMPLATE_INFO_1.getAppId(), TEMPLATE_INFO_1.getId());
        assertThat(updatedTemplate).isNotNull();
        assertThat(updatedTemplate.getId()).isGreaterThan(0L);
        assertThat(updatedTemplate.getAppId()).isEqualTo(TEMPLATE_INFO_1.getAppId());
        assertThat(updatedTemplate.getName()).isEqualTo(TEMPLATE_INFO_1.getName());
        assertThat(updatedTemplate.getDescription()).isEqualTo(TEMPLATE_INFO_1.getDescription());
        assertThat(updatedTemplate.getCreator()).isEqualTo(TEMPLATE_INFO_1.getCreator());
        assertThat(updatedTemplate.getCreateTime()).isEqualTo(TEMPLATE_INFO_1.getCreateTime());
        assertThat(updatedTemplate.getLastModifyUser()).isEqualTo(TEMPLATE_INFO_1.getLastModifyUser());
        assertThat(updatedTemplate.getLastModifyTime()).isEqualTo(TEMPLATE_INFO_1.getLastModifyTime());
    }

    @Test
    void giveTemplateIdReturnDeleteSuccess() {
        assertThat(taskTemplateDAO.deleteTaskTemplateById(TEMPLATE_INFO_1.getAppId(), TEMPLATE_INFO_1.getId()))
            .isTrue();
        assertThat(taskTemplateDAO.deleteTaskTemplateById(TEMPLATE_INFO_1.getAppId(), TEMPLATE_INFO_1.getId()))
            .isFalse();
        assertThat(taskTemplateDAO.getTaskTemplateById(TEMPLATE_INFO_1.getAppId(), TEMPLATE_INFO_1.getId())).isNull();
    }

}
