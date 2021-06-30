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

package com.tencent.bk.job.manage.service.template.impl;

import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.manage.common.consts.task.TaskTemplateStatusEnum;
import com.tencent.bk.job.manage.dao.template.TaskTemplateDAO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.service.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;

/**
 * @since 18/10/2019 14:41
 */
class TaskTemplateServiceImplTest {

    private static final TaskTemplateInfoDTO TEMPLATE_INFO_1 = new TaskTemplateInfoDTO();
    private static final TaskTemplateInfoDTO TEMPLATE_INFO_2 = new TaskTemplateInfoDTO();
    private static final TaskTemplateInfoDTO TEMPLATE_INFO_3 = new TaskTemplateInfoDTO();
    private static final PageData<TaskTemplateInfoDTO> TEMPLATE_INFO_FIRST_PAGE_DATA = new PageData<>();

    private static AbstractTaskStepService taskStepService;
    private static AbstractTaskVariableService taskVariableService;
    private static TaskTemplateDAO taskTemplateDAO;
    private static TagService tagService;

    private static TemplateStatusUpdateService templateStatusUpdateService;
    private static TaskTemplateServiceImpl taskTemplateService;
    private static ScriptService scriptService;
    private static TaskFavoriteService taskFavoriteService;

    private static CronJobService cronJobService;
    private static MeterRegistry meterRegistry;

    @BeforeAll
    static void init() {
        taskStepService = mock(AbstractTaskStepService.class);
        taskVariableService = mock(AbstractTaskVariableService.class);
        taskTemplateDAO = mock(TaskTemplateDAO.class);
        tagService = mock(TagService.class);

        templateStatusUpdateService = mock(TemplateStatusUpdateService.class);
        scriptService = mock(ScriptService.class);
        taskFavoriteService = mock(TaskFavoriteService.class);
        cronJobService = mock(CronJobService.class);
        meterRegistry = mock(MeterRegistry.class);

        taskTemplateService =
            new TaskTemplateServiceImpl(taskStepService, taskVariableService, taskTemplateDAO, tagService,
                templateStatusUpdateService, taskFavoriteService, cronJobService);
        taskTemplateService.setScriptService(scriptService);

    }

    @BeforeEach
    void setUp() {

        TEMPLATE_INFO_1.setId(1L);
        TEMPLATE_INFO_1.setAppId(1000L);
        TEMPLATE_INFO_1.setName("测试模版1");
        TEMPLATE_INFO_1.setDescription("这是一个测试模版。这段描述是随便写的，没什么意义。1");
        TEMPLATE_INFO_1.setCreator("userC");
        TEMPLATE_INFO_1.setStatus(TaskTemplateStatusEnum.NEW);
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
        TEMPLATE_INFO_1.setScriptStatus(0);

        TEMPLATE_INFO_2.setId(2L);
        TEMPLATE_INFO_2.setAppId(1000L);
        TEMPLATE_INFO_2.setName("测试模版2");
        TEMPLATE_INFO_2.setDescription("这是一个测试模版。这段描述是随便写的，没什么意义。2");
        TEMPLATE_INFO_2.setCreator("userC");
        TEMPLATE_INFO_2.setStatus(TaskTemplateStatusEnum.IN_REVIEW);
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
        TEMPLATE_INFO_2.setScriptStatus(1);

        TEMPLATE_INFO_3.setId(3L);
        TEMPLATE_INFO_3.setAppId(1000L);
        TEMPLATE_INFO_3.setName("测试模版3");
        TEMPLATE_INFO_3.setDescription("这是一个测试模版。这段描述是随便写的，没什么意义。3");
        TEMPLATE_INFO_3.setCreator("userC");
        TEMPLATE_INFO_3.setStatus(TaskTemplateStatusEnum.PUBLISHED);
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
        TEMPLATE_INFO_3.setScriptStatus(0);

        List<TaskTemplateInfoDTO> templateInfoListFirstThree =
            Arrays.asList(TEMPLATE_INFO_1, TEMPLATE_INFO_2, TEMPLATE_INFO_3);
        TEMPLATE_INFO_FIRST_PAGE_DATA.setStart(0);
        TEMPLATE_INFO_FIRST_PAGE_DATA.setPageSize(3);
        TEMPLATE_INFO_FIRST_PAGE_DATA.setTotal(6L);
        TEMPLATE_INFO_FIRST_PAGE_DATA.setData(templateInfoListFirstThree);
    }
}
