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

import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.execute.dao.TaskInstanceVariableDAO;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:test.properties")
@SqlConfig(encoding = "utf-8")
@Sql({"/init_task_instance_variable_data.sql"})
public class TaskInstanceVariableDAOImplIntegrationTest {
    @Autowired
    private TaskInstanceVariableDAO taskInstanceVariableDAO;

    @Test
    public void testGetByTaskInstanceId() {
        long taskInstanceId = 1L;
        List<TaskVariableDTO> taskVars = taskInstanceVariableDAO.getByTaskInstanceId(taskInstanceId);

        assertThat(taskVars).hasSize(2);
        assertThat(taskVars).extracting("name").containsOnly("param1", "param2");
        assertThat(taskVars).extracting("type").containsOnly(1, 2);
        assertThat(taskVars).extracting("value").containsOnly("param1_value", "param2_value");
    }

    @Test
    public void testSaveTaskInstanceVariables() {
        List<TaskVariableDTO> taskVars = new ArrayList<>();
        TaskVariableDTO taskVar1 = new TaskVariableDTO();
        taskVar1.setTaskInstanceId(100L);
        taskVar1.setName("param1");
        taskVar1.setType(TaskVariableTypeEnum.STRING.getType());
        taskVar1.setValue("param1-value");
        taskVar1.setChangeable(true);
        TaskVariableDTO taskVar2 = new TaskVariableDTO();
        taskVar2.setTaskInstanceId(100L);
        taskVar2.setName("param2");
        taskVar2.setType(TaskVariableTypeEnum.CIPHER.getType());
        taskVar2.setValue("password");
        taskVar2.setChangeable(false);
        taskVars.add(taskVar1);
        taskVars.add(taskVar2);

        taskInstanceVariableDAO.saveTaskInstanceVariables(taskVars);

        List<TaskVariableDTO> savedTaskVars = taskInstanceVariableDAO.getByTaskInstanceId(100L);

        assertThat(savedTaskVars).hasSize(2);
        assertThat(savedTaskVars).extracting("name").containsOnly("param1", "param2");

        for (TaskVariableDTO taskVar : savedTaskVars) {
            if (taskVar.getName().equals("param1")) {
                assertThat(taskVar.getType()).isEqualTo(TaskVariableTypeEnum.STRING.getType());
                assertThat(taskVar.getValue()).isEqualTo("param1-value");
                assertThat(taskVar.isChangeable()).isEqualTo(true);
            } else if (taskVar.getName().equals("param2")) {
                assertThat(taskVar.getType()).isEqualTo(TaskVariableTypeEnum.CIPHER.getType());
                assertThat(taskVar.getValue()).isEqualTo("password");
                assertThat(taskVar.isChangeable()).isEqualTo(false);
            }
        }

    }
}
