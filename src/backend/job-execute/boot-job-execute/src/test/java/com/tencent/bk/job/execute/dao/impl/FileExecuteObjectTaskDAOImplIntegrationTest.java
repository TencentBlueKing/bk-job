/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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
import com.tencent.bk.job.execute.dao.FileExecuteObjectTaskDAO;
import com.tencent.bk.job.execute.engine.consts.ExecuteObjectTaskStatusEnum;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.ResultGroupBaseDTO;
import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;
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
@Sql({"/init_gse_file_execute_obj_task_data.sql"})
public class FileExecuteObjectTaskDAOImplIntegrationTest {
    @Autowired
    private FileExecuteObjectTaskDAO fileExecuteObjectTaskDAO;

    @Test
    @DisplayName("根据执行对象ID获取任务")
    public void testGetTaskByExecuteObjectId() {
        String executeObjectId = "1:101";
        long stepInstanceId = 1L;
        int executeCount = 0;
        int batch = 1;
        FileTaskModeEnum mode = FileTaskModeEnum.UPLOAD;
        ExecuteObjectTask executeObjectTask = fileExecuteObjectTaskDAO.getTaskByExecuteObjectId(stepInstanceId,
            executeCount, batch, mode, executeObjectId);

        assertThat(executeObjectTask.getStepInstanceId()).isEqualTo(stepInstanceId);
        assertThat(executeObjectTask.getExecuteCount()).isEqualTo(executeCount);
        assertThat(executeObjectTask.getBatch()).isEqualTo(batch);
        assertThat(executeObjectTask.getFileTaskMode()).isEqualTo(FileTaskModeEnum.UPLOAD);
        assertThat(executeObjectTask.getExecuteObjectId()).isEqualTo(executeObjectId);
        assertThat(executeObjectTask.getExecuteObjectType()).isEqualTo(ExecuteObjectTypeEnum.HOST);
        assertThat(executeObjectTask.getGseTaskId()).isEqualTo(1L);
        assertThat(executeObjectTask.getStatus()).isEqualTo(ExecuteObjectTaskStatusEnum.SUCCESS);
        assertThat(executeObjectTask.getStartTime()).isEqualTo(1565767148000L);
        assertThat(executeObjectTask.getEndTime()).isEqualTo(1565767149000L);
        assertThat(executeObjectTask.getTotalTime()).isEqualTo(1000L);
        assertThat(executeObjectTask.getErrorCode()).isEqualTo(0);
    }

    @Test
    @DisplayName("批量新增任务")
    public void testBatchSaveTasks() {
        List<ExecuteObjectTask> executeObjectTaskList = new ArrayList<>();
        ExecuteObjectTask executeObjectTask1 = new ExecuteObjectTask();
        executeObjectTask1.setStepInstanceId(100L);
        executeObjectTask1.setExecuteCount(1);
        executeObjectTask1.setActualExecuteCount(1);
        executeObjectTask1.setBatch(1);
        executeObjectTask1.setFileTaskMode(FileTaskModeEnum.UPLOAD);
        executeObjectTask1.setExecuteObjectId("1:101");
        executeObjectTask1.setExecuteObjectType(ExecuteObjectTypeEnum.HOST);
        executeObjectTask1.setGseTaskId(1000L);
        executeObjectTask1.setStartTime(1572858334000L);
        executeObjectTask1.setEndTime(1572858335000L);
        executeObjectTask1.setTotalTime(1000L);
        executeObjectTask1.setErrorCode(99);
        executeObjectTask1.setStatus(ExecuteObjectTaskStatusEnum.AGENT_ERROR);
        executeObjectTaskList.add(executeObjectTask1);

        ExecuteObjectTask executeObjectTask2 = new ExecuteObjectTask();
        executeObjectTask2.setStepInstanceId(100L);
        executeObjectTask2.setExecuteCount(1);
        executeObjectTask2.setActualExecuteCount(1);
        executeObjectTask2.setBatch(1);
        executeObjectTask2.setFileTaskMode(FileTaskModeEnum.DOWNLOAD);
        executeObjectTask2.setExecuteObjectId("1:102");
        executeObjectTask2.setExecuteObjectType(ExecuteObjectTypeEnum.HOST);
        executeObjectTask2.setGseTaskId(1001L);
        executeObjectTask2.setErrorCode(88);
        executeObjectTask2.setStartTime(1572858330000L);
        executeObjectTask2.setEndTime(1572858331000L);
        executeObjectTask2.setTotalTime(1000L);
        executeObjectTask2.setErrorCode(88);
        executeObjectTask2.setStatus(ExecuteObjectTaskStatusEnum.HOST_NOT_EXIST);
        executeObjectTaskList.add(executeObjectTask2);

        fileExecuteObjectTaskDAO.batchSaveTasks(executeObjectTaskList);

        ExecuteObjectTask executeObjectTask1Return = fileExecuteObjectTaskDAO.getTaskByExecuteObjectId(100L, 1, 1,
            FileTaskModeEnum.UPLOAD, "1:101");
        assertThat(executeObjectTask1Return.getStepInstanceId()).isEqualTo(100L);
        assertThat(executeObjectTask1Return.getExecuteCount()).isEqualTo(1L);
        assertThat(executeObjectTask1Return.getActualExecuteCount()).isEqualTo(1L);
        assertThat(executeObjectTask1Return.getBatch()).isEqualTo(1);
        assertThat(executeObjectTask1Return.getFileTaskMode()).isEqualTo(FileTaskModeEnum.UPLOAD);
        assertThat(executeObjectTask1Return.getExecuteObjectId()).isEqualTo("1:101");
        assertThat(executeObjectTask1Return.getExecuteObjectType()).isEqualTo(ExecuteObjectTypeEnum.HOST);
        assertThat(executeObjectTask1Return.getGseTaskId()).isEqualTo(1000L);
        assertThat(executeObjectTask1Return.getStartTime()).isEqualTo(1572858334000L);
        assertThat(executeObjectTask1Return.getEndTime()).isEqualTo(1572858335000L);
        assertThat(executeObjectTask1Return.getTotalTime()).isEqualTo(1000L);
        assertThat(executeObjectTask1Return.getErrorCode()).isEqualTo(99);
        assertThat(executeObjectTask1Return.getStatus()).isEqualTo(ExecuteObjectTaskStatusEnum.AGENT_ERROR);


        ExecuteObjectTask executeObjectTask2Return = fileExecuteObjectTaskDAO.getTaskByExecuteObjectId(100L, 1, 1,
            FileTaskModeEnum.DOWNLOAD, "1:102");
        assertThat(executeObjectTask2Return.getStepInstanceId()).isEqualTo(100L);
        assertThat(executeObjectTask2Return.getExecuteCount()).isEqualTo(1L);
        assertThat(executeObjectTask2Return.getActualExecuteCount()).isEqualTo(1L);
        assertThat(executeObjectTask2Return.getBatch()).isEqualTo(1);
        assertThat(executeObjectTask2Return.getFileTaskMode()).isEqualTo(FileTaskModeEnum.DOWNLOAD);
        assertThat(executeObjectTask2Return.getExecuteObjectId()).isEqualTo("1:102");
        assertThat(executeObjectTask2Return.getExecuteObjectType()).isEqualTo(ExecuteObjectTypeEnum.HOST);
        assertThat(executeObjectTask2Return.getGseTaskId()).isEqualTo(1001L);
        assertThat(executeObjectTask2Return.getStartTime()).isEqualTo(1572858330000L);
        assertThat(executeObjectTask2Return.getEndTime()).isEqualTo(1572858331000L);
        assertThat(executeObjectTask2Return.getTotalTime()).isEqualTo(1000L);
        assertThat(executeObjectTask2Return.getErrorCode()).isEqualTo(88);
        assertThat(executeObjectTask2Return.getStatus()).isEqualTo(ExecuteObjectTaskStatusEnum.HOST_NOT_EXIST);
    }

    @Test
    @DisplayName("批量更新任务")
    public void testBatchUpdateAgentTasks() {
        List<ExecuteObjectTask> executeObjectTaskList = new ArrayList<>();
        ExecuteObjectTask executeObjectTask1 = new ExecuteObjectTask();
        executeObjectTask1.setTaskInstanceId(1L);
        executeObjectTask1.setStepInstanceId(1L);
        executeObjectTask1.setExecuteCount(0);
        executeObjectTask1.setBatch(2);
        executeObjectTask1.setFileTaskMode(FileTaskModeEnum.UPLOAD);
        executeObjectTask1.setExecuteObjectId("1:101");
        executeObjectTask1.setExecuteObjectType(ExecuteObjectTypeEnum.HOST);
        executeObjectTask1.setGseTaskId(1000L);
        executeObjectTask1.setStartTime(1572858334000L);
        executeObjectTask1.setEndTime(1572858335000L);
        executeObjectTask1.setTotalTime(1000L);
        executeObjectTask1.setErrorCode(99);
        executeObjectTask1.setStatus(ExecuteObjectTaskStatusEnum.AGENT_ERROR);
        executeObjectTaskList.add(executeObjectTask1);

        ExecuteObjectTask executeObjectTask2 = new ExecuteObjectTask();
        executeObjectTask2.setTaskInstanceId(1L);
        executeObjectTask2.setStepInstanceId(1L);
        executeObjectTask2.setExecuteCount(0);
        executeObjectTask2.setBatch(2);
        executeObjectTask2.setFileTaskMode(FileTaskModeEnum.DOWNLOAD);
        executeObjectTask2.setExecuteObjectId("1:103");
        executeObjectTask2.setExecuteObjectType(ExecuteObjectTypeEnum.HOST);
        executeObjectTask2.setGseTaskId(1001L);
        executeObjectTask2.setErrorCode(88);
        executeObjectTask2.setStartTime(1572858330000L);
        executeObjectTask2.setEndTime(1572858331000L);
        executeObjectTask2.setTotalTime(1000L);
        executeObjectTask2.setErrorCode(88);
        executeObjectTask2.setStatus(ExecuteObjectTaskStatusEnum.HOST_NOT_EXIST);
        executeObjectTaskList.add(executeObjectTask2);

        fileExecuteObjectTaskDAO.batchUpdateTasks(executeObjectTaskList);

        ExecuteObjectTask executeObjectTask1Return = fileExecuteObjectTaskDAO.getTaskByExecuteObjectId(1L, 0, 2,
            FileTaskModeEnum.UPLOAD, "1:101");
        assertThat(executeObjectTask1Return.getStepInstanceId()).isEqualTo(1L);
        assertThat(executeObjectTask1Return.getExecuteCount()).isEqualTo(0L);
        assertThat(executeObjectTask1Return.getBatch()).isEqualTo(2);
        assertThat(executeObjectTask1Return.getExecuteObjectType()).isEqualTo(ExecuteObjectTypeEnum.HOST);
        assertThat(executeObjectTask1Return.getExecuteObjectId()).isEqualTo("1:101");
        assertThat(executeObjectTask1Return.getGseTaskId()).isEqualTo(1000L);
        assertThat(executeObjectTask1Return.getStartTime()).isEqualTo(1572858334000L);
        assertThat(executeObjectTask1Return.getEndTime()).isEqualTo(1572858335000L);
        assertThat(executeObjectTask1Return.getTotalTime()).isEqualTo(1000L);
        assertThat(executeObjectTask1Return.getErrorCode()).isEqualTo(99);
        assertThat(executeObjectTask1Return.getStatus()).isEqualTo(ExecuteObjectTaskStatusEnum.AGENT_ERROR);


        ExecuteObjectTask executeObjectTask2Return = fileExecuteObjectTaskDAO.getTaskByExecuteObjectId(1L, 0, 2,
            FileTaskModeEnum.DOWNLOAD, "1:103");
        assertThat(executeObjectTask2Return.getStepInstanceId()).isEqualTo(1L);
        assertThat(executeObjectTask2Return.getExecuteCount()).isEqualTo(0L);
        assertThat(executeObjectTask2Return.getBatch()).isEqualTo(2);
        assertThat(executeObjectTask2Return.getGseTaskId()).isEqualTo(1001L);
        assertThat(executeObjectTask2Return.getExecuteObjectId()).isEqualTo("1:103");
        assertThat(executeObjectTask2Return.getExecuteObjectType()).isEqualTo(ExecuteObjectTypeEnum.HOST);
        assertThat(executeObjectTask2Return.getStartTime()).isEqualTo(1572858330000L);
        assertThat(executeObjectTask2Return.getEndTime()).isEqualTo(1572858331000L);
        assertThat(executeObjectTask2Return.getTotalTime()).isEqualTo(1000L);
        assertThat(executeObjectTask2Return.getErrorCode()).isEqualTo(88);
        assertThat(executeObjectTask2Return.getStatus()).isEqualTo(ExecuteObjectTaskStatusEnum.HOST_NOT_EXIST);
    }

    @Test
    @DisplayName("任务结果分组")
    public void listResultGroups() {
        List<ResultGroupBaseDTO> resultGroups = fileExecuteObjectTaskDAO.listResultGroups(1L, 0, null);

        assertThat(resultGroups.size()).isEqualTo(2);
        assertThat(resultGroups).extracting("status").containsOnly(9, 11);
        ResultGroupBaseDTO resultGroup = resultGroups.get(0);
        if (resultGroup.getStatus().equals(9)) {
            assertThat(resultGroup.getTotal()).isEqualTo(2);
        }
        if (resultGroup.getStatus().equals(11)) {
            assertThat(resultGroup.getTotal()).isEqualTo(1);
        }

        // 根据滚动执行批次查询
        resultGroups = fileExecuteObjectTaskDAO.listResultGroups(1L, 0, 2);

        assertThat(resultGroups.size()).isEqualTo(2);
        assertThat(resultGroups).extracting("status").containsOnly(9, 11);
        resultGroup = resultGroups.get(0);
        if (resultGroup.getStatus().equals(9)) {
            assertThat(resultGroup.getTotal()).isEqualTo(1);
        }
        if (resultGroup.getStatus().equals(11)) {
            assertThat(resultGroup.getTotal()).isEqualTo(1);
        }
    }

    @Test
    public void testListAgentTaskByResultGroup() {
        List<ExecuteObjectTask> executeObjectTasks = fileExecuteObjectTaskDAO.listTaskByResultGroup(1L, 0, 2, 9);
        assertThat(executeObjectTasks.size()).isEqualTo(1);
        assertThat(executeObjectTasks.get(0).getStepInstanceId()).isEqualTo(1L);
        assertThat(executeObjectTasks.get(0).getExecuteCount()).isEqualTo(0);
        assertThat(executeObjectTasks.get(0).getBatch()).isEqualTo(2);
        assertThat(executeObjectTasks.get(0).getStatus()).isEqualTo(ExecuteObjectTaskStatusEnum.SUCCESS);
        assertThat(executeObjectTasks).extracting("executeObjectId").containsOnly("1:103");
    }

}
