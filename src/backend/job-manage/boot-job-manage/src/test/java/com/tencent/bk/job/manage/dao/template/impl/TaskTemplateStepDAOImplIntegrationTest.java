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

import com.tencent.bk.job.common.util.RandomUtil;
import com.tencent.bk.job.manage.common.consts.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.dao.TaskStepDAO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 6/10/2019 09:11
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:test.properties")
@Sql(value = {"/template/init_template_step_data.sql"})
@SqlConfig(encoding = "utf-8")
class TaskTemplateStepDAOImplIntegrationTest {

    private static final TaskStepDTO STEP_1 = new TaskStepDTO();
    private static final TaskStepDTO STEP_2 = new TaskStepDTO();
    private static final TaskStepDTO STEP_3 = new TaskStepDTO();
    private static final List<TaskStepDTO> STEP_LIST = Arrays.asList(STEP_1, STEP_2, STEP_3);

    @Autowired
    @Qualifier("TaskTemplateStepDAOImpl")
    private TaskStepDAO taskStepDAO;

    @BeforeEach
    void initTest() {
        STEP_1.setId(1L);
        STEP_1.setTemplateId(2000L);
        STEP_1.setType(TaskStepTypeEnum.SCRIPT);
        STEP_1.setName("测试用脚本步骤");
        STEP_1.setPreviousStepId(0L);
        STEP_1.setNextStepId(2L);
        STEP_1.setScriptStepId(1L);
        STEP_1.setDelete(0);

        STEP_2.setId(2L);
        STEP_2.setTemplateId(2000L);
        STEP_2.setType(TaskStepTypeEnum.FILE);
        STEP_2.setName("测试用文件步骤");
        STEP_2.setPreviousStepId(1L);
        STEP_2.setNextStepId(3L);
        STEP_2.setFileStepId(1L);
        STEP_2.setDelete(0);

        STEP_3.setId(3L);
        STEP_3.setTemplateId(2000L);
        STEP_3.setType(TaskStepTypeEnum.APPROVAL);
        STEP_3.setName("测试用审批步骤");
        STEP_3.setPreviousStepId(2L);
        STEP_3.setNextStepId(0L);
        STEP_3.setApprovalStepId(1L);
        STEP_3.setDelete(0);
    }

    @Test
    void givenNormalStepIdReturnApprovalList() {
        List<TaskStepDTO> result = taskStepDAO.listStepsByParentId(STEP_1.getTemplateId());
        assertThat(result).isEqualTo(STEP_LIST);
    }

    @Test
    void givenNotExistStepIdReturnEmptyList() {
        assertThat(taskStepDAO.listStepsByParentId(9999999L)).isEqualTo(Collections.emptyList());
    }

    @Test
    void givenNormalApprovalIdAndStepIdReturnApprovalInfo() {
        assertThat(taskStepDAO.getStepById(STEP_1.getTemplateId(), STEP_1.getId())).isEqualTo(STEP_1);
        assertThat(taskStepDAO.getStepById(STEP_2.getTemplateId(), STEP_2.getId())).isEqualTo(STEP_2);
        assertThat(taskStepDAO.getStepById(STEP_3.getTemplateId(), STEP_3.getId())).isEqualTo(STEP_3);
    }

    @Test
    void givenNotExistApprovalIdOrNotExistStepIdReturnNull() {
        assertThat(taskStepDAO.getStepById(STEP_1.getTemplateId(), 9999999L)).isNull();
        assertThat(taskStepDAO.getStepById(9999999L, STEP_1.getId())).isNull();
        assertThat(taskStepDAO.getStepById(9999999L, 9999999L)).isNull();
    }

    @Test
    void givenApprovalInfoReturnNewApprovalId() {
        TaskStepDTO step = new TaskStepDTO();
        step.setTemplateId(RandomUtil.getRandomPositiveLong());
        step.setType(TaskStepTypeEnum.SCRIPT);
        step.setName(UUID.randomUUID().toString());
        step.setPreviousStepId(0L);
        step.setNextStepId(0L);
        step.setScriptStepId(RandomUtil.getRandomPositiveLong());
        Long stepId = taskStepDAO.insertStep(step);
        step.setId(stepId);
        step.setDelete(0);
        taskStepDAO.updateStepById(step);
        assertThat(taskStepDAO.getStepById(step.getTemplateId(), stepId)).isEqualTo(step);
    }

    @Test
    void givenApprovalIdAndStepIdReturnDeleteSuccess() {
        assertThat(taskStepDAO.deleteStepById(STEP_1.getTemplateId(), STEP_1.getId())).isTrue();
        assertThat(taskStepDAO.getStepById(STEP_1.getTemplateId(), STEP_1.getId())).isNull();
        assertThat(taskStepDAO.deleteStepById(STEP_1.getTemplateId(), STEP_1.getId())).isFalse();
    }

    @Test
    void givenWrongApprovalIdOrStepIdReturnDeleteFailed() {
        assertThat(taskStepDAO.deleteStepById(999999999999L, STEP_1.getId())).isFalse();
        assertThat(taskStepDAO.deleteStepById(STEP_1.getTemplateId(), 999999999999L)).isFalse();
        assertThat(taskStepDAO.deleteStepById(999999999999L, 999999999999L)).isFalse();

        assertThat(taskStepDAO.getStepById(STEP_1.getTemplateId(), STEP_1.getId())).isEqualTo(STEP_1);
    }

    @Test
    void givenNewApprovalInfoReturnUpdateSuccess() {
        assertThat(taskStepDAO.updateStepById(STEP_1)).isTrue();
        STEP_1.setType(TaskStepTypeEnum.APPROVAL);
        STEP_1.setName(UUID.randomUUID().toString());
        STEP_1.setPreviousStepId(RandomUtil.getRandomPositiveLong());
        STEP_1.setNextStepId(RandomUtil.getRandomPositiveLong());
        STEP_1.setScriptStepId(RandomUtil.getRandomPositiveLong());
        STEP_1.setFileStepId(RandomUtil.getRandomPositiveLong());
        STEP_1.setApprovalStepId(RandomUtil.getRandomPositiveLong());
        assertThat(taskStepDAO.updateStepById(STEP_1)).isFalse();
        assertThat(taskStepDAO.getStepById(STEP_1.getTemplateId(), STEP_1.getId())).isNotEqualTo(STEP_1);

        STEP_1.setType(TaskStepTypeEnum.SCRIPT);
        assertThat(taskStepDAO.updateStepById(STEP_1)).isTrue();

        STEP_1.setFileStepId(null);
        STEP_1.setApprovalStepId(null);
        assertThat(taskStepDAO.getStepById(STEP_1.getTemplateId(), STEP_1.getId())).isEqualTo(STEP_1);
    }

    @Test
    void givenWrongApprovalInfoReturnUpdateFailed() {
        STEP_1.setId(RandomUtil.getRandomPositiveLong());
        assertThat(taskStepDAO.updateStepById(STEP_1)).isFalse();

        STEP_2.setTemplateId(RandomUtil.getRandomPositiveLong());
        assertThat(taskStepDAO.updateStepById(STEP_2)).isFalse();

        STEP_1.setId(RandomUtil.getRandomPositiveLong());
        STEP_1.setTemplateId(RandomUtil.getRandomPositiveLong());
        assertThat(taskStepDAO.updateStepById(STEP_1)).isFalse();
    }

}
