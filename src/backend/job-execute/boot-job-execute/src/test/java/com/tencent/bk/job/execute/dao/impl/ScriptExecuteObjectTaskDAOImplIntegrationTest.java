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

import com.tencent.bk.job.common.constant.ExecuteObjectTypeEnum;
import com.tencent.bk.job.execute.dao.ScriptExecuteObjectTaskDAO;
import com.tencent.bk.job.execute.engine.consts.ExecuteObjectTaskStatusEnum;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.ResultGroupBaseDTO;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:test.properties")
@SqlConfig(encoding = "utf-8")
@Sql({"/init_gse_script_execute_obj_task_data.sql"})
public class ScriptExecuteObjectTaskDAOImplIntegrationTest {
    @Autowired
    private ScriptExecuteObjectTaskDAO scriptExecuteObjectTaskDAO;

    @Test
    @DisplayName("根据执行对象 ID 获取任务")
    public void testGetTaskByExecuteObjectId() {
        String executeObjectId = "1:101";
        long stepInstanceId = 1L;
        long taskInstanceId = 1L;
        int executeCount = 0;
        int batch = 1;
        ExecuteObjectTask executeObjectTask = scriptExecuteObjectTaskDAO.getTaskByExecuteObjectId(
            taskInstanceId, stepInstanceId, executeCount, batch, executeObjectId);

        assertThat(executeObjectTask.getStepInstanceId()).isEqualTo(stepInstanceId);
        assertThat(executeObjectTask.getTaskInstanceId()).isEqualTo(taskInstanceId);
        assertThat(executeObjectTask.getExecuteCount()).isEqualTo(executeCount);
        assertThat(executeObjectTask.getBatch()).isEqualTo(batch);
        assertThat(executeObjectTask.getExecuteObjectType()).isEqualTo(ExecuteObjectTypeEnum.HOST);
        assertThat(executeObjectTask.getExecuteObjectId()).isEqualTo(executeObjectId);
        assertThat(executeObjectTask.getGseTaskId()).isEqualTo(1L);
        assertThat(executeObjectTask.getStatus()).isEqualTo(ExecuteObjectTaskStatusEnum.SUCCESS);
        Long expectStartTime = 1565767148000L;
        Long expectEndTime = 1565767149000L;
        assertThat(executeObjectTask.getStartTime()).isEqualTo(expectStartTime);
        assertThat(executeObjectTask.getEndTime()).isEqualTo(expectEndTime);
        assertThat(executeObjectTask.getTotalTime()).isEqualTo(1316L);
        assertThat(executeObjectTask.getErrorCode()).isEqualTo(0);
        assertThat(executeObjectTask.getExitCode()).isEqualTo(0);
        assertThat(executeObjectTask.getTag()).isEqualTo("succ");
        assertThat(executeObjectTask.getScriptLogOffset()).isEqualTo(0);
    }

    @Test
    @DisplayName("批量新增任务")
    public void testBatchSaveTasks() {
        List<ExecuteObjectTask> executeObjectTaskList = new ArrayList<>();
        ExecuteObjectTask executeObjectTask1 = new ExecuteObjectTask();
        executeObjectTask1.setTaskInstanceId(100L);
        executeObjectTask1.setStepInstanceId(100L);
        executeObjectTask1.setExecuteCount(1);
        executeObjectTask1.setActualExecuteCount(1);
        executeObjectTask1.setBatch(1);
        executeObjectTask1.setExecuteObjectType(ExecuteObjectTypeEnum.HOST);
        executeObjectTask1.setExecuteObjectId("1:101");
        executeObjectTask1.setGseTaskId(1000L);
        executeObjectTask1.setStartTime(1572858334000L);
        executeObjectTask1.setEndTime(1572858335000L);
        executeObjectTask1.setTotalTime(1000L);
        executeObjectTask1.setErrorCode(99);
        executeObjectTask1.setStatus(ExecuteObjectTaskStatusEnum.AGENT_ERROR);
        executeObjectTask1.setTag("aa");
        executeObjectTask1.setExitCode(1);
        executeObjectTaskList.add(executeObjectTask1);

        ExecuteObjectTask executeObjectTask2 = new ExecuteObjectTask();
        executeObjectTask2.setTaskInstanceId(100L);
        executeObjectTask2.setStepInstanceId(100L);
        executeObjectTask2.setExecuteCount(1);
        executeObjectTask2.setActualExecuteCount(1);
        executeObjectTask2.setBatch(1);
        executeObjectTask2.setExecuteObjectType(ExecuteObjectTypeEnum.HOST);
        executeObjectTask2.setExecuteObjectId("1:102");
        executeObjectTask2.setGseTaskId(1001L);
        executeObjectTask2.setErrorCode(88);
        executeObjectTask2.setExitCode(1);
        executeObjectTask2.setStartTime(1572858330000L);
        executeObjectTask2.setEndTime(1572858331000L);
        executeObjectTask2.setTotalTime(1000L);
        executeObjectTask2.setErrorCode(88);
        executeObjectTask2.setStatus(ExecuteObjectTaskStatusEnum.INVALID_EXECUTE_OBJECT);
        executeObjectTask2.setTag("bb");
        executeObjectTask2.setExitCode(2);
        executeObjectTaskList.add(executeObjectTask2);

        scriptExecuteObjectTaskDAO.batchSaveTasks(executeObjectTaskList);

        ExecuteObjectTask executeObjectTask1Return = scriptExecuteObjectTaskDAO
            .getTaskByExecuteObjectId(100L, 100L, 1, 1, "1:101");
        assertThat(executeObjectTask1Return.getTaskInstanceId()).isEqualTo(100L);
        assertThat(executeObjectTask1Return.getStepInstanceId()).isEqualTo(100L);
        assertThat(executeObjectTask1Return.getExecuteCount()).isEqualTo(1L);
        assertThat(executeObjectTask1Return.getActualExecuteCount()).isEqualTo(1L);
        assertThat(executeObjectTask1Return.getBatch()).isEqualTo(1);
        assertThat(executeObjectTask1Return.getExecuteObjectType()).isEqualTo(ExecuteObjectTypeEnum.HOST);
        assertThat(executeObjectTask1Return.getExecuteObjectId()).isEqualTo("1:101");
        assertThat(executeObjectTask1Return.getGseTaskId()).isEqualTo(1000L);
        assertThat(executeObjectTask1Return.getStartTime()).isEqualTo(1572858334000L);
        assertThat(executeObjectTask1Return.getEndTime()).isEqualTo(1572858335000L);
        assertThat(executeObjectTask1Return.getTotalTime()).isEqualTo(1000L);
        assertThat(executeObjectTask1Return.getErrorCode()).isEqualTo(99);
        assertThat(executeObjectTask1Return.getStatus()).isEqualTo(ExecuteObjectTaskStatusEnum.AGENT_ERROR);
        assertThat(executeObjectTask1Return.getTag()).isEqualTo("aa");
        assertThat(executeObjectTask1Return.getExitCode()).isEqualTo(1);


        ExecuteObjectTask executeObjectTask2Return = scriptExecuteObjectTaskDAO
            .getTaskByExecuteObjectId(100L, 100L, 1, 1, "1:102");
        assertThat(executeObjectTask2Return.getStepInstanceId()).isEqualTo(100L);
        assertThat(executeObjectTask2Return.getTaskInstanceId()).isEqualTo(100L);
        assertThat(executeObjectTask2Return.getExecuteCount()).isEqualTo(1L);
        assertThat(executeObjectTask2Return.getActualExecuteCount()).isEqualTo(1L);
        assertThat(executeObjectTask2Return.getBatch()).isEqualTo(1);
        assertThat(executeObjectTask2Return.getGseTaskId()).isEqualTo(1001L);
        assertThat(executeObjectTask2Return.getExecuteObjectType()).isEqualTo(ExecuteObjectTypeEnum.HOST);
        assertThat(executeObjectTask2Return.getExecuteObjectId()).isEqualTo("1:102");
        assertThat(executeObjectTask2Return.getStartTime()).isEqualTo(1572858330000L);
        assertThat(executeObjectTask2Return.getEndTime()).isEqualTo(1572858331000L);
        assertThat(executeObjectTask2Return.getTotalTime()).isEqualTo(1000L);
        assertThat(executeObjectTask2Return.getErrorCode()).isEqualTo(88);
        assertThat(executeObjectTask2Return.getStatus()).isEqualTo(ExecuteObjectTaskStatusEnum.INVALID_EXECUTE_OBJECT);
        assertThat(executeObjectTask2Return.getTag()).isEqualTo("bb");
        assertThat(executeObjectTask2Return.getExitCode()).isEqualTo(2);
    }

    @Test
    @DisplayName("批量更新任务")
    public void testBatchUpdateTasks() {
        List<ExecuteObjectTask> executeObjectTaskList = new ArrayList<>();
        ExecuteObjectTask executeObjectTask1 = new ExecuteObjectTask();
        executeObjectTask1.setTaskInstanceId(1L);
        executeObjectTask1.setStepInstanceId(1L);
        executeObjectTask1.setExecuteCount(0);
        executeObjectTask1.setBatch(3);
        executeObjectTask1.setExecuteObjectId("1:103");
        executeObjectTask1.setExecuteObjectType(ExecuteObjectTypeEnum.HOST);
        executeObjectTask1.setGseTaskId(1000L);
        executeObjectTask1.setStartTime(1572858334000L);
        executeObjectTask1.setEndTime(1572858335000L);
        executeObjectTask1.setTotalTime(1000L);
        executeObjectTask1.setErrorCode(99);
        executeObjectTask1.setStatus(ExecuteObjectTaskStatusEnum.AGENT_ERROR);
        executeObjectTask1.setTag("aa");
        executeObjectTask1.setExitCode(1);
        executeObjectTaskList.add(executeObjectTask1);

        ExecuteObjectTask executeObjectTask2 = new ExecuteObjectTask();
        executeObjectTask2.setTaskInstanceId(1L);
        executeObjectTask2.setStepInstanceId(1L);
        executeObjectTask2.setExecuteCount(0);
        executeObjectTask2.setBatch(3);
        executeObjectTask2.setExecuteObjectId("1:104");
        executeObjectTask2.setExecuteObjectType(ExecuteObjectTypeEnum.HOST);
        executeObjectTask2.setGseTaskId(1001L);
        executeObjectTask2.setErrorCode(88);
        executeObjectTask2.setExitCode(1);
        executeObjectTask2.setStartTime(1572858330000L);
        executeObjectTask2.setEndTime(1572858331000L);
        executeObjectTask2.setTotalTime(1000L);
        executeObjectTask2.setErrorCode(88);
        executeObjectTask2.setStatus(ExecuteObjectTaskStatusEnum.INVALID_EXECUTE_OBJECT);
        executeObjectTask2.setTag("bb");
        executeObjectTask2.setExitCode(2);
        executeObjectTaskList.add(executeObjectTask2);

        scriptExecuteObjectTaskDAO.batchUpdateTasks(executeObjectTaskList);

        ExecuteObjectTask executeObjectTask1Return =
            scriptExecuteObjectTaskDAO.getTaskByExecuteObjectId(1L, 1L, 0, 3, "1:103");
        assertThat(executeObjectTask1Return.getStepInstanceId()).isEqualTo(1L);
        assertThat(executeObjectTask1Return.getExecuteCount()).isEqualTo(0L);
        assertThat(executeObjectTask1Return.getBatch()).isEqualTo(3);
        assertThat(executeObjectTask1Return.getExecuteObjectId()).isEqualTo("1:103");
        assertThat(executeObjectTask1Return.getExecuteObjectType()).isEqualTo(ExecuteObjectTypeEnum.HOST);
        assertThat(executeObjectTask1Return.getGseTaskId()).isEqualTo(1000L);
        assertThat(executeObjectTask1Return.getStartTime()).isEqualTo(1572858334000L);
        assertThat(executeObjectTask1Return.getEndTime()).isEqualTo(1572858335000L);
        assertThat(executeObjectTask1Return.getTotalTime()).isEqualTo(1000L);
        assertThat(executeObjectTask1Return.getErrorCode()).isEqualTo(99);
        assertThat(executeObjectTask1Return.getStatus()).isEqualTo(ExecuteObjectTaskStatusEnum.AGENT_ERROR);
        assertThat(executeObjectTask1Return.getTag()).isEqualTo("aa");
        assertThat(executeObjectTask1Return.getExitCode()).isEqualTo(1);


        ExecuteObjectTask executeObjectTask2Return =
            scriptExecuteObjectTaskDAO.getTaskByExecuteObjectId(1L, 1L, 0, 3, "1:104");
        assertThat(executeObjectTask2Return.getStepInstanceId()).isEqualTo(1L);
        assertThat(executeObjectTask2Return.getExecuteCount()).isEqualTo(0L);
        assertThat(executeObjectTask2Return.getBatch()).isEqualTo(3);
        assertThat(executeObjectTask2Return.getGseTaskId()).isEqualTo(1001L);
        assertThat(executeObjectTask2Return.getExecuteObjectId()).isEqualTo("1:104");
        assertThat(executeObjectTask2Return.getExecuteObjectType()).isEqualTo(ExecuteObjectTypeEnum.HOST);
        assertThat(executeObjectTask2Return.getStartTime()).isEqualTo(1572858330000L);
        assertThat(executeObjectTask2Return.getEndTime()).isEqualTo(1572858331000L);
        assertThat(executeObjectTask2Return.getTotalTime()).isEqualTo(1000L);
        assertThat(executeObjectTask2Return.getErrorCode()).isEqualTo(88);
        assertThat(executeObjectTask2Return.getStatus()).isEqualTo(ExecuteObjectTaskStatusEnum.INVALID_EXECUTE_OBJECT);
        assertThat(executeObjectTask2Return.getTag()).isEqualTo("bb");
        assertThat(executeObjectTask2Return.getExitCode()).isEqualTo(2);
    }

    @Test
    public void testGetSuccessIpCount() {
        Integer count = scriptExecuteObjectTaskDAO.getSuccessTaskCount(1L, 1L, 0);
        assertThat(count).isEqualTo(4);
    }

    @Test
    @DisplayName("查询任务结果分组")
    public void listResultGroups() {
        List<ResultGroupBaseDTO> resultGroups = scriptExecuteObjectTaskDAO.listResultGroups(1L, 1L, 0, null);

        assertThat(resultGroups.size()).isEqualTo(2);
        assertThat(resultGroups).extracting("status").containsOnly(9, 11);
        assertThat(resultGroups).extracting("tag").containsOnly("succ", "fail");
        ResultGroupBaseDTO resultGroup = resultGroups.get(0);
        if (resultGroup.getStatus().equals(9)) {
            assertThat(resultGroup.getTotal()).isEqualTo(4);
        }
        if (resultGroup.getStatus().equals(11)) {
            assertThat(resultGroup.getTotal()).isEqualTo(1);
        }

        // 根据滚动执行批次查询
        resultGroups = scriptExecuteObjectTaskDAO.listResultGroups(1L, 1L, 0, 3);

        assertThat(resultGroups.size()).isEqualTo(2);
        assertThat(resultGroups).extracting("status").containsOnly(9, 11);
        assertThat(resultGroups).extracting("tag").containsOnly("succ", "fail");
        resultGroup = resultGroups.get(0);
        if (resultGroup.getStatus().equals(9)) {
            assertThat(resultGroup.getTotal()).isEqualTo(2);
        }
        if (resultGroup.getStatus().equals(11)) {
            assertThat(resultGroup.getTotal()).isEqualTo(1);
        }
    }

    @Test
    public void testListAgentTaskByResultGroup() {
        List<ExecuteObjectTask> executeObjectTasks = scriptExecuteObjectTaskDAO.listTasksByResultGroup(1L, 1L, 0, 1, 9,
            "succ");
        assertThat(executeObjectTasks.size()).isEqualTo(1);
        assertThat(executeObjectTasks.get(0).getStepInstanceId()).isEqualTo(1L);
        assertThat(executeObjectTasks.get(0).getExecuteCount()).isEqualTo(0);
        assertThat(executeObjectTasks.get(0).getBatch()).isEqualTo(1);
        assertThat(executeObjectTasks.get(0).getStatus()).isEqualTo(ExecuteObjectTaskStatusEnum.SUCCESS);
        assertThat(executeObjectTasks.get(0).getTag()).isEqualTo("succ");
    }

}
