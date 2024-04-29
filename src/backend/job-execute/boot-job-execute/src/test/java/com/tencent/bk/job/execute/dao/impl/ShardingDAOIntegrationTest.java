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

package com.tencent.bk.job.execute.dao.impl;

import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.dao.TaskInstanceDAO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:test.properties")
@SqlConfig(encoding = "utf-8")
class ShardingDAOIntegrationTest {

    @Autowired
    private TaskInstanceDAO taskInstanceDAO;

    @Test
    void testAddAndGetTaskInstance() {
        TaskInstanceDTO taskInstance1 = new TaskInstanceDTO();
        taskInstance1.setId(1000001L);
        taskInstance1.setPlanId(3L);
        taskInstance1.setTaskTemplateId(3L);
        taskInstance1.setAppId(2L);
        taskInstance1.setName("task3");
        taskInstance1.setOperator("user1");
        taskInstance1.setType(TaskTypeEnum.NORMAL.getValue());
        taskInstance1.setCreateTime(1572955200000L);
        taskInstance1.setStatus(RunStatusEnum.BLANK);
        taskInstance1.setStartupMode(TaskStartupModeEnum.API.getValue());
        taskInstance1.setCallbackUrl("http://bkjob.com");
        taskInstance1.setAppCode("bk_monitor");
        Long taskInstanceId1 = taskInstanceDAO.addTaskInstance(taskInstance1);

        TaskInstanceDTO taskInstance2 = new TaskInstanceDTO();
        taskInstance2.setId(1000002L);
        taskInstance2.setPlanId(3L);
        taskInstance2.setTaskTemplateId(3L);
        taskInstance2.setAppId(2L);
        taskInstance2.setName("task3");
        taskInstance2.setOperator("user1");
        taskInstance2.setType(TaskTypeEnum.NORMAL.getValue());
        taskInstance2.setCreateTime(1572955200000L);
        taskInstance2.setStatus(RunStatusEnum.BLANK);
        taskInstance2.setStartupMode(TaskStartupModeEnum.API.getValue());
        taskInstance2.setCallbackUrl("http://bkjob.com");
        taskInstance2.setAppCode("bk_monitor");
        Long taskInstanceId2 = taskInstanceDAO.addTaskInstance(taskInstance2);

        TaskInstanceDTO returnTaskInstance = taskInstanceDAO.getTaskInstance(1000001L);

        assertThat(returnTaskInstance.getId()).isEqualTo(1000001L);

        returnTaskInstance = taskInstanceDAO.getTaskInstance(1000002L);
        assertThat(returnTaskInstance.getId()).isEqualTo(1000002L);
    }

}
