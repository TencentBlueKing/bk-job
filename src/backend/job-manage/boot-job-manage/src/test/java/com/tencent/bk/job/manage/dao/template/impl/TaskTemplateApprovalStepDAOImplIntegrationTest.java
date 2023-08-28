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

import com.tencent.bk.job.common.model.dto.UserRoleInfoDTO;
import com.tencent.bk.job.manage.common.consts.task.TaskApprovalTypeEnum;
import com.tencent.bk.job.manage.dao.TaskApprovalStepDAO;
import com.tencent.bk.job.manage.model.dto.task.TaskApprovalStepDTO;
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
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 6/10/2019 09:11
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:test.properties")
@Sql(value = {"/template/init_template_approval_step_data.sql"})
@SqlConfig(encoding = "utf-8")
class TaskTemplateApprovalStepDAOImplIntegrationTest {

    private static final Random random = new Random();
    private static final TaskApprovalStepDTO APPROVAL_STEP_1 = new TaskApprovalStepDTO();
    private static final TaskApprovalStepDTO APPROVAL_STEP_2 = new TaskApprovalStepDTO();

    @Autowired
    @Qualifier("TaskTemplateApprovalStepDAOImpl")
    private TaskApprovalStepDAO taskApprovalStepDAO;

    @BeforeEach
    void initTest() {
        UserRoleInfoDTO approvalUser1 = new UserRoleInfoDTO();
        approvalUser1.setUserList(Arrays.asList("userC", "userT"));
        APPROVAL_STEP_1.setId(1L);
        APPROVAL_STEP_1.setStepId(2000L);
        APPROVAL_STEP_1.setApprovalType(TaskApprovalTypeEnum.ANYONE);
        APPROVAL_STEP_1.setApprovalUser(approvalUser1);
        APPROVAL_STEP_1.setApprovalMessage("这是一个测试步骤1");
        APPROVAL_STEP_1.setNotifyChannel(Arrays.asList("a", "b"));

        UserRoleInfoDTO approvalUser2 = new UserRoleInfoDTO();
        approvalUser2.setUserList(Collections.singletonList("userC"));
        APPROVAL_STEP_2.setId(2L);
        APPROVAL_STEP_2.setStepId(3000L);
        APPROVAL_STEP_2.setApprovalType(TaskApprovalTypeEnum.ALL);
        APPROVAL_STEP_2.setApprovalUser(approvalUser2);
        APPROVAL_STEP_2.setApprovalMessage("这是一个测试步骤2");
        APPROVAL_STEP_2.setNotifyChannel(Arrays.asList("b", "c"));
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
    void givenNormalApprovalIdAndStepIdReturnApprovalInfo() {
        assertThat(taskApprovalStepDAO.getApprovalById(APPROVAL_STEP_1.getStepId())).isEqualTo(APPROVAL_STEP_1);
        assertThat(taskApprovalStepDAO.getApprovalById(APPROVAL_STEP_2.getStepId())).isEqualTo(APPROVAL_STEP_2);
    }

    @Test
    void givenNotExistApprovalIdOrNotExistStepIdReturnNull() {
        assertThat(taskApprovalStepDAO.getApprovalById(9999999L)).isNull();
    }

    @Test
    void givenApprovalInfoReturnNewApprovalId() {
        UserRoleInfoDTO approvalUser = new UserRoleInfoDTO();
        approvalUser.setUserList(Collections.singletonList(UUID.randomUUID().toString()));
        TaskApprovalStepDTO approvalStep = new TaskApprovalStepDTO();
        approvalStep.setStepId(getRandomPositiveLong());
        approvalStep.setApprovalType(TaskApprovalTypeEnum.ALL);
        approvalStep.setApprovalUser(approvalUser);
        approvalStep.setApprovalMessage(UUID.randomUUID().toString());
        approvalStep.setNotifyChannel(Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        Long approvalId = taskApprovalStepDAO.insertApproval(approvalStep);
        approvalStep.setId(approvalId);
        assertThat(taskApprovalStepDAO.getApprovalById(approvalStep.getStepId())).isEqualTo(approvalStep);
    }

    @Test
    void givenApprovalIdAndStepIdReturnDeleteSuccess() {
        assertThat(taskApprovalStepDAO.deleteApprovalById(APPROVAL_STEP_1.getStepId())).isTrue();
        assertThat(taskApprovalStepDAO.getApprovalById(APPROVAL_STEP_1.getStepId())).isNull();
        assertThat(taskApprovalStepDAO.deleteApprovalById(APPROVAL_STEP_1.getStepId())).isFalse();
    }

    @Test
    void givenWrongApprovalIdOrStepIdReturnDeleteFailed() {
        assertThat(taskApprovalStepDAO.deleteApprovalById(999999999999L)).isFalse();

        assertThat(taskApprovalStepDAO.getApprovalById(APPROVAL_STEP_1.getStepId())).isEqualTo(APPROVAL_STEP_1);
    }

    @Test
    void givenNewApprovalInfoReturnUpdateSuccess() {
        assertThat(taskApprovalStepDAO.updateApprovalById(APPROVAL_STEP_1)).isTrue();
        UserRoleInfoDTO approvalUser = new UserRoleInfoDTO();
        approvalUser.setUserList(Collections.singletonList(UUID.randomUUID().toString()));
        APPROVAL_STEP_1.setApprovalType(TaskApprovalTypeEnum.ALL);
        APPROVAL_STEP_1.setApprovalUser(approvalUser);
        APPROVAL_STEP_1.setApprovalMessage(UUID.randomUUID().toString());
        APPROVAL_STEP_1.setNotifyChannel(Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        assertThat(taskApprovalStepDAO.updateApprovalById(APPROVAL_STEP_1)).isTrue();

        assertThat(taskApprovalStepDAO.getApprovalById(APPROVAL_STEP_1.getStepId())).isEqualTo(APPROVAL_STEP_1);
    }

    @Test
    void givenWrongApprovalInfoReturnUpdateFailed() {
        APPROVAL_STEP_1.setId(getRandomPositiveLong());
        assertThat(taskApprovalStepDAO.updateApprovalById(APPROVAL_STEP_1)).isFalse();

        APPROVAL_STEP_2.setStepId(getRandomPositiveLong());
        assertThat(taskApprovalStepDAO.updateApprovalById(APPROVAL_STEP_2)).isFalse();

        APPROVAL_STEP_1.setId(getRandomPositiveLong());
        APPROVAL_STEP_1.setStepId(getRandomPositiveLong());
        assertThat(taskApprovalStepDAO.updateApprovalById(APPROVAL_STEP_1)).isFalse();
    }

}
