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

import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
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
import java.util.*;
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
    private static final PageData<TaskTemplateInfoDTO> TEMPLATE_INFO_FIRST_PAGE_DATA = new PageData<>();
    private static final PageData<TaskTemplateInfoDTO> TEMPLATE_INFO_SECOND_PAGE_DATA = new PageData<>();
    private static final PageData<TaskTemplateInfoDTO> TEMPLATE_INFO_LAST_MODIFY_USER_PAGE_DATA = new PageData<>();

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
        TEMPLATE_INFO_1.setTags(Arrays.asList(1L, 2L, 3L).stream().map(tagId -> {
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
        TEMPLATE_INFO_2.setTags(Arrays.asList(2L, 3L, 4L).stream().map(tagId -> {
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
        TEMPLATE_INFO_3.setTags(Arrays.asList(3L, 4L, 5L).stream().map(tagId -> {
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
        TEMPLATE_INFO_4.setTags(Arrays.asList(1L, 3L, 5L).stream().map(tagId -> {
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
        TEMPLATE_INFO_5.setTags(Arrays.asList(2L, 4L, 6L).stream().map(tagId -> {
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
        TEMPLATE_INFO_6.setTags(Arrays.asList(1L, 4L, 7L).stream().map(tagId -> {
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
        TEMPLATE_INFO_7.setTags(Arrays.asList(2L, 5L, 8L).stream().map(tagId -> {
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
        TEMPLATE_INFO_8.setTags(Arrays.asList(3L, 6L, 9L).stream().map(tagId -> {
            TagDTO tagInfo = new TagDTO();
            tagInfo.setId(tagId);
            return tagInfo;
        }).collect(Collectors.toList()));
        TEMPLATE_INFO_8.setFirstStepId(8000L);
        TEMPLATE_INFO_8.setLastStepId(9000L);
        TEMPLATE_INFO_8.setScriptStatus(0);
        TEMPLATE_INFO_8.setVersion("abcd1234");

        List<TaskTemplateInfoDTO> templateInfoListFirstThree =
            Arrays.asList(TEMPLATE_INFO_3, TEMPLATE_INFO_5, TEMPLATE_INFO_4);
        TEMPLATE_INFO_FIRST_PAGE_DATA.setStart(0);
        TEMPLATE_INFO_FIRST_PAGE_DATA.setPageSize(3);
        TEMPLATE_INFO_FIRST_PAGE_DATA.setTotal(6L);
        TEMPLATE_INFO_FIRST_PAGE_DATA.setData(templateInfoListFirstThree);

        List<TaskTemplateInfoDTO> templateInfoListSecondThree =
            Arrays.asList(TEMPLATE_INFO_6, TEMPLATE_INFO_2, TEMPLATE_INFO_1);
        TEMPLATE_INFO_SECOND_PAGE_DATA.setStart(3);
        TEMPLATE_INFO_SECOND_PAGE_DATA.setPageSize(3);
        TEMPLATE_INFO_SECOND_PAGE_DATA.setTotal(6L);
        TEMPLATE_INFO_SECOND_PAGE_DATA.setData(templateInfoListSecondThree);

        List<TaskTemplateInfoDTO> templateInfoLastModifyUser =
            Arrays.asList(TEMPLATE_INFO_1, TEMPLATE_INFO_3, TEMPLATE_INFO_5);
        templateInfoLastModifyUser.sort(Comparator.comparing(TaskTemplateInfoDTO::getId));
        TEMPLATE_INFO_LAST_MODIFY_USER_PAGE_DATA.setStart(0);
        TEMPLATE_INFO_LAST_MODIFY_USER_PAGE_DATA.setPageSize(3);
        TEMPLATE_INFO_LAST_MODIFY_USER_PAGE_DATA.setTotal(6L);
        TEMPLATE_INFO_LAST_MODIFY_USER_PAGE_DATA.setData(templateInfoLastModifyUser);

    }

    @Test
    void givenSearchConditionReturnPageTemplateData() {
        TaskTemplateInfoDTO templateCondition = new TaskTemplateInfoDTO();
        templateCondition.setAppId(1000L);
        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(0);
        baseSearchCondition.setLength(3);
        assertThat(taskTemplateDAO.listPageTaskTemplates(templateCondition, baseSearchCondition, null))
            .isEqualTo(TEMPLATE_INFO_FIRST_PAGE_DATA);

        baseSearchCondition.setStart(3);
        assertThat(taskTemplateDAO.listPageTaskTemplates(templateCondition, baseSearchCondition, null))
            .isEqualTo(TEMPLATE_INFO_SECOND_PAGE_DATA);

        baseSearchCondition.setStart(0);
        baseSearchCondition.setOrder(1);
        baseSearchCondition.setOrderField("last_modify_user");
        PageData<TaskTemplateInfoDTO> templateInfoLastModifyUserPageData =
            taskTemplateDAO.listPageTaskTemplates(templateCondition, baseSearchCondition, null);
        templateInfoLastModifyUserPageData.getData().sort(Comparator.comparing(TaskTemplateInfoDTO::getId));
        assertThat(templateInfoLastModifyUserPageData).isEqualTo(TEMPLATE_INFO_LAST_MODIFY_USER_PAGE_DATA);

        baseSearchCondition.setStart(0);
        baseSearchCondition.setOrder(null);
        baseSearchCondition.setOrderField(null);

        templateCondition.setName("1");
        assertThat(taskTemplateDAO.listPageTaskTemplates(templateCondition, baseSearchCondition, null).getData().get(0))
            .isEqualTo(TEMPLATE_INFO_1);

        templateCondition.setName(null);
        templateCondition.setCreator("userT");

        PageData<TaskTemplateInfoDTO> templateInfoCreatorPageData =
            taskTemplateDAO.listPageTaskTemplates(templateCondition, baseSearchCondition, null);
        templateInfoCreatorPageData.getData().sort(Comparator.comparing(TaskTemplateInfoDTO::getId));
        assertThat(templateInfoCreatorPageData.getData())
            .isEqualTo(Arrays.asList(TEMPLATE_INFO_4, TEMPLATE_INFO_5, TEMPLATE_INFO_6));
        assertThat(templateInfoCreatorPageData.getTotal()).isEqualTo(3L);

        templateCondition.setCreator(null);
        baseSearchCondition.setCreator("userT");
        templateInfoCreatorPageData =
            taskTemplateDAO.listPageTaskTemplates(templateCondition, baseSearchCondition, null);
        templateInfoCreatorPageData.getData().sort(Comparator.comparing(TaskTemplateInfoDTO::getId));
        assertThat(templateInfoCreatorPageData.getData())
            .isEqualTo(Arrays.asList(TEMPLATE_INFO_4, TEMPLATE_INFO_5, TEMPLATE_INFO_6));
        assertThat(templateInfoCreatorPageData.getTotal()).isEqualTo(3L);

        baseSearchCondition.setCreator(null);
        templateCondition.setTags(Arrays.asList(1L, 3L).stream().map(tagId -> {
            TagDTO tagInfo = new TagDTO();
            tagInfo.setId(tagId);
            return tagInfo;
        }).collect(Collectors.toList()));
        PageData<TaskTemplateInfoDTO> templateInfoTagsPageData =
            taskTemplateDAO.listPageTaskTemplates(templateCondition, baseSearchCondition, null);
        templateInfoTagsPageData.getData().sort(Comparator.comparing(TaskTemplateInfoDTO::getId));
        assertThat(templateInfoTagsPageData.getData()).isEqualTo(Arrays.asList(TEMPLATE_INFO_1, TEMPLATE_INFO_4));
        assertThat(templateInfoTagsPageData.getTotal()).isEqualTo(2L);
    }

    @Test
    void giveNormalTemplateIdReturnTemplateInfo() {
        assertThat(taskTemplateDAO.getTaskTemplateById(TEMPLATE_INFO_1.getAppId(), TEMPLATE_INFO_1.getId()))
            .isEqualTo(TEMPLATE_INFO_1);
        assertThat(taskTemplateDAO.getTaskTemplateById(TEMPLATE_INFO_2.getAppId(), TEMPLATE_INFO_2.getId()))
            .isEqualTo(TEMPLATE_INFO_2);
        assertThat(taskTemplateDAO.getTaskTemplateById(TEMPLATE_INFO_3.getAppId(), TEMPLATE_INFO_3.getId()))
            .isEqualTo(TEMPLATE_INFO_3);
        assertThat(taskTemplateDAO.getTaskTemplateById(TEMPLATE_INFO_4.getAppId(), TEMPLATE_INFO_4.getId()))
            .isEqualTo(TEMPLATE_INFO_4);
        assertThat(taskTemplateDAO.getTaskTemplateById(TEMPLATE_INFO_7.getAppId(), TEMPLATE_INFO_7.getId()))
            .isEqualTo(TEMPLATE_INFO_7);
        assertThat(taskTemplateDAO.getTaskTemplateById(TEMPLATE_INFO_8.getAppId(), TEMPLATE_INFO_8.getId()))
            .isEqualTo(TEMPLATE_INFO_8);
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
        newTaskTemplateInfo.setScriptStatus(0);
        assertThat(taskInfoId).isGreaterThan(0L);
        TaskTemplateInfoDTO taskTemplateById =
            taskTemplateDAO.getTaskTemplateById(newTaskTemplateInfo.getAppId(), taskInfoId);
        assertThat(taskTemplateById).isNotEqualTo(newTaskTemplateInfo);
        newTaskTemplateInfo.setVersion(taskTemplateById.getVersion());
        assertThat(taskTemplateById).isEqualTo(newTaskTemplateInfo);
    }

    @Test
    void giveTemplateInfoReturnUpdateSuccess() {
        assertThat(taskTemplateDAO.updateTaskTemplateById(TEMPLATE_INFO_1, true)).isTrue();
        TEMPLATE_INFO_1.setName(UUID.randomUUID().toString());
        TEMPLATE_INFO_1.setLastModifyUser(UUID.randomUUID().toString());
        TEMPLATE_INFO_1.setLastModifyTime(DateUtils.currentTimeSeconds());
        assertThat(taskTemplateDAO.updateTaskTemplateById(TEMPLATE_INFO_1, true)).isTrue();
        TaskTemplateInfoDTO taskTemplateById =
            taskTemplateDAO.getTaskTemplateById(TEMPLATE_INFO_1.getAppId(), TEMPLATE_INFO_1.getId());
        assertThat(taskTemplateById).isNotEqualTo(TEMPLATE_INFO_1);
        TEMPLATE_INFO_1.setVersion(taskTemplateById.getVersion());
        assertThat(taskTemplateById).isEqualTo(TEMPLATE_INFO_1);
    }

    @Test
    void giveTemplateIdReturnDeleteSuccess() {
        assertThat(taskTemplateDAO.deleteTaskTemplateById(TEMPLATE_INFO_1.getAppId(), TEMPLATE_INFO_1.getId()))
            .isTrue();
        assertThat(taskTemplateDAO.deleteTaskTemplateById(TEMPLATE_INFO_1.getAppId(), TEMPLATE_INFO_1.getId()))
            .isFalse();
        assertThat(taskTemplateDAO.getTaskTemplateById(TEMPLATE_INFO_1.getAppId(), TEMPLATE_INFO_1.getId())).isNull();
    }

    @Test
    void giveAppIdReturnTagCount() {
        Map<Long, Long> tagCount = taskTemplateDAO.getTemplateTagCount(TEMPLATE_INFO_1.getAppId());
        assertThat(tagCount.get(1L)).isEqualTo(3L);
        assertThat(tagCount.get(2L)).isEqualTo(3L);
        assertThat(tagCount.get(3L)).isEqualTo(4L);
        assertThat(tagCount.get(4L)).isEqualTo(4L);
        assertThat(tagCount.get(5L)).isEqualTo(2L);
        assertThat(tagCount.get(6L)).isEqualTo(1L);
        assertThat(tagCount.get(7L)).isEqualTo(1L);
    }

}
