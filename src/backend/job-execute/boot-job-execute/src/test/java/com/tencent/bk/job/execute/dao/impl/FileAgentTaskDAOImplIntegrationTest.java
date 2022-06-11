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

import com.tencent.bk.job.execute.dao.FileAgentTaskDAO;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.AgentTaskResultGroupBaseDTO;
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
@Sql({"/init_file_agent_task_data.sql"})
public class FileAgentTaskDAOImplIntegrationTest {
    @Autowired
    private FileAgentTaskDAO fileAgentTaskDAO;

    @Test
    @DisplayName("根据主机ID获取Agent任务")
    public void testGetAgentTaskByHostId() {
        long hostId = 101L;
        long stepInstanceId = 1L;
        int executeCount = 0;
        int batch = 1;
        FileTaskModeEnum mode = FileTaskModeEnum.UPLOAD;
        AgentTaskDTO agentTask = fileAgentTaskDAO.getAgentTaskByHostId(stepInstanceId, executeCount, batch, mode, hostId);

        assertThat(agentTask.getStepInstanceId()).isEqualTo(stepInstanceId);
        assertThat(agentTask.getExecuteCount()).isEqualTo(executeCount);
        assertThat(agentTask.getBatch()).isEqualTo(batch);
        assertThat(agentTask.getFileTaskMode()).isEqualTo(FileTaskModeEnum.UPLOAD);
        assertThat(agentTask.getHostId()).isEqualTo(hostId);
        assertThat(agentTask.getAgentId()).isEqualTo("0:127.0.0.1");
        assertThat(agentTask.getGseTaskId()).isEqualTo(1L);
        assertThat(agentTask.getStatus()).isEqualTo(9);
        assertThat(agentTask.getStartTime()).isEqualTo(1565767148000L);
        assertThat(agentTask.getEndTime()).isEqualTo(1565767149000L);
        assertThat(agentTask.getTotalTime()).isEqualTo(1000L);
        assertThat(agentTask.getErrorCode()).isEqualTo(0);
    }

    @Test
    @DisplayName("批量新增Agent任务")
    public void testBatchSaveAgentTasks() {
        List<AgentTaskDTO> agentTaskList = new ArrayList<>();
        AgentTaskDTO agentTask1 = new AgentTaskDTO();
        agentTask1.setStepInstanceId(100L);
        agentTask1.setExecuteCount(0);
        agentTask1.setBatch(1);
        agentTask1.setFileTaskMode(FileTaskModeEnum.UPLOAD);
        agentTask1.setHostId(101L);
        agentTask1.setAgentId("0:127.0.0.1");
        agentTask1.setGseTaskId(1000L);
        agentTask1.setStartTime(1572858334000L);
        agentTask1.setEndTime(1572858335000L);
        agentTask1.setTotalTime(1000L);
        agentTask1.setErrorCode(99);
        agentTask1.setStatus(1);
        agentTaskList.add(agentTask1);

        AgentTaskDTO agentTask2 = new AgentTaskDTO();
        agentTask2.setStepInstanceId(100L);
        agentTask2.setExecuteCount(0);
        agentTask2.setBatch(1);
        agentTask2.setFileTaskMode(FileTaskModeEnum.DOWNLOAD);
        agentTask2.setHostId(102L);
        agentTask2.setAgentId("0:127.0.0.2");
        agentTask2.setGseTaskId(1001L);
        agentTask2.setErrorCode(88);
        agentTask2.setStartTime(1572858330000L);
        agentTask2.setEndTime(1572858331000L);
        agentTask2.setTotalTime(1000L);
        agentTask2.setErrorCode(88);
        agentTask2.setStatus(2);
        agentTaskList.add(agentTask2);

        fileAgentTaskDAO.batchSaveAgentTasks(agentTaskList);

        AgentTaskDTO agentTask1Return = fileAgentTaskDAO.getAgentTaskByHostId(100L, 0, 1,
            FileTaskModeEnum.UPLOAD, 101L);
        assertThat(agentTask1Return.getStepInstanceId()).isEqualTo(100L);
        assertThat(agentTask1Return.getExecuteCount()).isEqualTo(0L);
        assertThat(agentTask1Return.getBatch()).isEqualTo(1);
        assertThat(agentTask1Return.getFileTaskMode()).isEqualTo(FileTaskModeEnum.UPLOAD);
        assertThat(agentTask1Return.getHostId()).isEqualTo(101L);
        assertThat(agentTask1Return.getAgentId()).isEqualTo("0:127.0.0.1");
        assertThat(agentTask1Return.getGseTaskId()).isEqualTo(1000L);
        assertThat(agentTask1Return.getStartTime()).isEqualTo(1572858334000L);
        assertThat(agentTask1Return.getEndTime()).isEqualTo(1572858335000L);
        assertThat(agentTask1Return.getTotalTime()).isEqualTo(1000L);
        assertThat(agentTask1Return.getErrorCode()).isEqualTo(99);
        assertThat(agentTask1Return.getStatus()).isEqualTo(1);


        AgentTaskDTO agentTask2Return = fileAgentTaskDAO.getAgentTaskByHostId(100L, 0, 1,
            FileTaskModeEnum.DOWNLOAD, 102L);
        assertThat(agentTask2Return.getStepInstanceId()).isEqualTo(100L);
        assertThat(agentTask2Return.getExecuteCount()).isEqualTo(0L);
        assertThat(agentTask2Return.getBatch()).isEqualTo(1);
        assertThat(agentTask2Return.getFileTaskMode()).isEqualTo(FileTaskModeEnum.DOWNLOAD);
        assertThat(agentTask2Return.getHostId()).isEqualTo(102L);
        assertThat(agentTask2Return.getAgentId()).isEqualTo("0:127.0.0.2");
        assertThat(agentTask2Return.getGseTaskId()).isEqualTo(1001L);
        assertThat(agentTask2Return.getStartTime()).isEqualTo(1572858330000L);
        assertThat(agentTask2Return.getEndTime()).isEqualTo(1572858331000L);
        assertThat(agentTask2Return.getTotalTime()).isEqualTo(1000L);
        assertThat(agentTask2Return.getErrorCode()).isEqualTo(88);
        assertThat(agentTask2Return.getStatus()).isEqualTo(2);
    }

    @Test
    @DisplayName("批量更新Agent任务")
    public void testBatchUpdateAgentTasks() {
        List<AgentTaskDTO> agentTaskList = new ArrayList<>();
        AgentTaskDTO agentTask1 = new AgentTaskDTO();
        agentTask1.setStepInstanceId(1L);
        agentTask1.setExecuteCount(0);
        agentTask1.setBatch(2);
        agentTask1.setFileTaskMode(FileTaskModeEnum.UPLOAD);
        agentTask1.setHostId(101L);
        agentTask1.setAgentId("0:127.0.0.1");
        agentTask1.setGseTaskId(1000L);
        agentTask1.setStartTime(1572858334000L);
        agentTask1.setEndTime(1572858335000L);
        agentTask1.setTotalTime(1000L);
        agentTask1.setErrorCode(99);
        agentTask1.setStatus(1);
        agentTaskList.add(agentTask1);

        AgentTaskDTO agentTask2 = new AgentTaskDTO();
        agentTask2.setStepInstanceId(1L);
        agentTask2.setExecuteCount(0);
        agentTask2.setBatch(2);
        agentTask2.setFileTaskMode(FileTaskModeEnum.DOWNLOAD);
        agentTask2.setHostId(103L);
        agentTask2.setAgentId("0:127.0.0.3");
        agentTask2.setGseTaskId(1001L);
        agentTask2.setErrorCode(88);
        agentTask2.setStartTime(1572858330000L);
        agentTask2.setEndTime(1572858331000L);
        agentTask2.setTotalTime(1000L);
        agentTask2.setErrorCode(88);
        agentTask2.setStatus(2);
        agentTaskList.add(agentTask2);

        fileAgentTaskDAO.batchUpdateAgentTasks(agentTaskList);

        AgentTaskDTO agentTask1Return = fileAgentTaskDAO.getAgentTaskByHostId(1L, 0, 2,
            FileTaskModeEnum.UPLOAD, 101L);
        assertThat(agentTask1Return.getStepInstanceId()).isEqualTo(1L);
        assertThat(agentTask1Return.getExecuteCount()).isEqualTo(0L);
        assertThat(agentTask1Return.getBatch()).isEqualTo(2);
        assertThat(agentTask1Return.getAgentId()).isEqualTo("0:127.0.0.1");
        assertThat(agentTask1Return.getHostId()).isEqualTo(101L);
        assertThat(agentTask1Return.getGseTaskId()).isEqualTo(1000L);
        assertThat(agentTask1Return.getStartTime()).isEqualTo(1572858334000L);
        assertThat(agentTask1Return.getEndTime()).isEqualTo(1572858335000L);
        assertThat(agentTask1Return.getTotalTime()).isEqualTo(1000L);
        assertThat(agentTask1Return.getErrorCode()).isEqualTo(99);
        assertThat(agentTask1Return.getStatus()).isEqualTo(1);


        AgentTaskDTO agentTask2Return = fileAgentTaskDAO.getAgentTaskByHostId(1L, 0, 2,
            FileTaskModeEnum.DOWNLOAD, 103L);
        assertThat(agentTask2Return.getStepInstanceId()).isEqualTo(1L);
        assertThat(agentTask2Return.getExecuteCount()).isEqualTo(0L);
        assertThat(agentTask2Return.getBatch()).isEqualTo(2);
        assertThat(agentTask2Return.getGseTaskId()).isEqualTo(1001L);
        assertThat(agentTask2Return.getHostId()).isEqualTo(103L);
        assertThat(agentTask2Return.getAgentId()).isEqualTo("0:127.0.0.3");
        assertThat(agentTask2Return.getStartTime()).isEqualTo(1572858330000L);
        assertThat(agentTask2Return.getEndTime()).isEqualTo(1572858331000L);
        assertThat(agentTask2Return.getTotalTime()).isEqualTo(1000L);
        assertThat(agentTask2Return.getErrorCode()).isEqualTo(88);
        assertThat(agentTask2Return.getStatus()).isEqualTo(2);
    }

    @Test
    @DisplayName("Agent任务结果分组")
    public void listResultGroups() {
        List<AgentTaskResultGroupBaseDTO> resultGroups = fileAgentTaskDAO.listResultGroups(1L, 0, null);

        assertThat(resultGroups.size()).isEqualTo(2);
        assertThat(resultGroups).extracting("status").containsOnly(9, 11);
        AgentTaskResultGroupBaseDTO resultGroup = resultGroups.get(0);
        if (resultGroup.getStatus().equals(9)) {
            assertThat(resultGroup.getTotalAgentTasks()).isEqualTo(2);
        }
        if (resultGroup.getStatus().equals(11)) {
            assertThat(resultGroup.getTotalAgentTasks()).isEqualTo(1);
        }

        // 根据滚动执行批次查询
        resultGroups = fileAgentTaskDAO.listResultGroups(1L, 0, 2);

        assertThat(resultGroups.size()).isEqualTo(2);
        assertThat(resultGroups).extracting("status").containsOnly(9, 11);
        resultGroup = resultGroups.get(0);
        if (resultGroup.getStatus().equals(9)) {
            assertThat(resultGroup.getTotalAgentTasks()).isEqualTo(1);
        }
        if (resultGroup.getStatus().equals(11)) {
            assertThat(resultGroup.getTotalAgentTasks()).isEqualTo(1);
        }
    }

    @Test
    public void testListAgentTaskByResultGroup() {
        List<AgentTaskDTO> agentTasks = fileAgentTaskDAO.listAgentTaskByResultGroup(1L, 0, 2, 9);
        assertThat(agentTasks.size()).isEqualTo(1);
        assertThat(agentTasks.get(0).getStepInstanceId()).isEqualTo(1L);
        assertThat(agentTasks.get(0).getExecuteCount()).isEqualTo(0);
        assertThat(agentTasks.get(0).getBatch()).isEqualTo(2);
        assertThat(agentTasks.get(0).getStatus()).isEqualTo(9);
        assertThat(agentTasks).extracting("hostId").containsOnly(103L);
    }

}
