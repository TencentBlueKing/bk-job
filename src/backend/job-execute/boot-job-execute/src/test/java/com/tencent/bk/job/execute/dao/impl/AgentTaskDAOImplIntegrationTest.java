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

import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.AgentTaskResultGroupDTO;
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
@Sql({"/init_gse_task_ip_log_data.sql"})
public class AgentTaskDAOImplIntegrationTest {
    @Autowired
    private AgentTaskDAO agentTaskDAO;

    @Test
    @DisplayName("根据IP获取Agent任务")
    public void testGetAgentTaskByIp() {
        String ip = "0:127.0.0.1";
        long stepInstanceId = 1L;
        int executeCount = 0;
        AgentTaskDTO agentTask = agentTaskDAO.getAgentTaskByIp(stepInstanceId, executeCount, ip);

        assertThat(agentTask.getStepInstanceId()).isEqualTo(stepInstanceId);
        assertThat(agentTask.getExecuteCount()).isEqualTo(executeCount);
        assertThat(agentTask.getBatch()).isEqualTo(1);
        assertThat(agentTask.getCloudIp()).isEqualTo(ip);
        assertThat(agentTask.getBatch()).isEqualTo(1);
        assertThat(agentTask.getGseTaskId()).isEqualTo(1L);
        assertThat(agentTask.getStatus()).isEqualTo(9);
        Long expectStartTime = 1565767148000L;
        Long expectEndTime = 1565767149000L;
        assertThat(agentTask.getStartTime()).isEqualTo(expectStartTime);
        assertThat(agentTask.getEndTime()).isEqualTo(expectEndTime);
        assertThat(agentTask.getTotalTime()).isEqualTo(1316L);
        assertThat(agentTask.getErrorCode()).isEqualTo(0);
        assertThat(agentTask.getExitCode()).isEqualTo(0);
        assertThat(agentTask.getTag()).isEqualTo("succ");
        assertThat(agentTask.getScriptLogOffset()).isEqualTo(0);
        assertThat(agentTask.getCloudId()).isEqualTo(0);
        assertThat(agentTask.getDisplayIp()).isEqualTo("127.0.0.1");
        assertThat(agentTask.isTargetServer()).isEqualTo(true);
        assertThat(agentTask.isSourceServer()).isEqualTo(false);
    }

    @Test
    @DisplayName("批量新增/更新Agent任务")
    public void testBatchSaveAgentTasks() {
        // UPDATE
        List<AgentTaskDTO> agentTaskList = new ArrayList<>();
        AgentTaskDTO agentTask1 = new AgentTaskDTO();
        agentTask1.setStepInstanceId(1L);
        agentTask1.setExecuteCount(0);
        agentTask1.setBatch(1);
        agentTask1.setGseTaskId(1L);
        agentTask1.setCloudIp("0:127.0.0.1");
        agentTask1.setDisplayIp("127.0.0.1");
        agentTask1.setErrorCode(99);
        agentTask1.setStatus(1);
        agentTask1.setExitCode(1);
        agentTaskList.add(agentTask1);

        // INSERT
        AgentTaskDTO agentTask2 = new AgentTaskDTO();
        agentTask2.setStepInstanceId(100L);
        agentTask2.setExecuteCount(0);
        agentTask2.setBatch(1);
        agentTask2.setGseTaskId(1000L);
        agentTask2.setCloudIp("0:127.0.0.1");
        agentTask2.setErrorCode(88);
        agentTask2.setExitCode(1);
        agentTask2.setDisplayIp("127.0.0.1");
        long startTime = 1572858330000L;
        agentTask2.setStartTime(startTime);
        long endTime = 1572858331000L;
        agentTask2.setEndTime(endTime);
        agentTask2.setStatus(2);
        agentTaskList.add(agentTask2);

        agentTaskDAO.batchSaveAgentTasks(agentTaskList);

        AgentTaskDTO agentTask1Return = agentTaskDAO.getAgentTaskByIp(1L, 0, "0:127.0.0.1");
        assertThat(agentTask1Return.getStepInstanceId()).isEqualTo(1L);
        assertThat(agentTask1Return.getExecuteCount()).isEqualTo(0L);
        assertThat(agentTask1Return.getBatch()).isEqualTo(1);
        assertThat(agentTask1Return.getCloudIp()).isEqualTo("0:127.0.0.1");
        assertThat(agentTask1Return.getErrorCode()).isEqualTo(99);
        assertThat(agentTask1Return.getStatus()).isEqualTo(1);
        assertThat(agentTask1Return.getExitCode()).isEqualTo(1);


        AgentTaskDTO agentTask2Return = agentTaskDAO.getAgentTaskByIp(100L, 0, "0:127.0.0.1");
        assertThat(agentTask2Return.getStepInstanceId()).isEqualTo(100L);
        assertThat(agentTask2Return.getExecuteCount()).isEqualTo(0L);
        assertThat(agentTask2Return.getBatch()).isEqualTo(1);
        assertThat(agentTask2Return.getGseTaskId()).isEqualTo(1000L);
        assertThat(agentTask2Return.getCloudIp()).isEqualTo("0:127.0.0.1");
        assertThat(agentTask2Return.getStartTime()).isEqualTo(startTime);
        assertThat(agentTask2Return.getEndTime()).isEqualTo(endTime);
        assertThat(agentTask2Return.getErrorCode()).isEqualTo(88);
        assertThat(agentTask2Return.getStatus()).isEqualTo(2);
        assertThat(agentTask2Return.getExitCode()).isEqualTo(1);
    }

    @Test
    public void testGetSuccessIpCount() {
        Integer count = agentTaskDAO.getSuccessIpCount(1L, 0);
        assertThat(count).isEqualTo(4);
    }

    @Test
    @DisplayName("Agent任务结果分组")
    public void listResultGroups() {
        List<AgentTaskResultGroupDTO> resultGroups = agentTaskDAO.listResultGroups(1L, 0, null);

        assertThat(resultGroups.size()).isEqualTo(2);
        assertThat(resultGroups).extracting("status").containsOnly(9, 11);
        assertThat(resultGroups).extracting("tag").containsOnly("succ", "fail");
        AgentTaskResultGroupDTO resultGroup = resultGroups.get(0);
        if (resultGroup.getStatus().equals(9)) {
            assertThat(resultGroup.getTotalAgentTasks()).isEqualTo(4);
        }
        if (resultGroup.getStatus().equals(11)) {
            assertThat(resultGroup.getTotalAgentTasks()).isEqualTo(1);
        }

        // 根据滚动执行批次查询
        resultGroups = agentTaskDAO.listResultGroups(1L, 0, 3);

        assertThat(resultGroups.size()).isEqualTo(2);
        assertThat(resultGroups).extracting("status").containsOnly(9, 11);
        assertThat(resultGroups).extracting("tag").containsOnly("succ", "fail");
        resultGroup = resultGroups.get(0);
        if (resultGroup.getStatus().equals(9)) {
            assertThat(resultGroup.getTotalAgentTasks()).isEqualTo(2);
        }
        if (resultGroup.getStatus().equals(11)) {
            assertThat(resultGroup.getTotalAgentTasks()).isEqualTo(1);
        }
    }

    @Test
    public void testGetAgentTaskByResultGroup() {
        List<AgentTaskDTO> agentTasks = agentTaskDAO.listAgentTaskByResultGroup(1L, 0, 1, 9, "succ");
        assertThat(agentTasks.size()).isEqualTo(1);
        assertThat(agentTasks.get(0).getStepInstanceId()).isEqualTo(1L);
        assertThat(agentTasks.get(0).getExecuteCount()).isEqualTo(0);
        assertThat(agentTasks.get(0).getBatch()).isEqualTo(1);
        assertThat(agentTasks.get(0).getStatus()).isEqualTo(9);
        assertThat(agentTasks.get(0).getTag()).isEqualTo("succ");
    }

    @Test
    public void testGetAgentTaskByIps() {
        String[] ipArray = {"0:127.0.0.1", "0:127.0.0.2"};
        List<AgentTaskDTO> agentTasks = agentTaskDAO.listAgentTasksByIps(1L, 0, ipArray);

        assertThat(agentTasks.size()).isEqualTo(2);
        assertThat(agentTasks).extracting("cloudIp").containsOnly("0:127.0.0.1", "0:127.0.0.2");
        assertThat(agentTasks).extracting("stepInstanceId").containsOnly(1L, 1L);
        assertThat(agentTasks).extracting("executeCount").containsOnly(0, 0);
    }

    @Test
    public void testGetTaskFileSourceIps() {
        List<String> fileSourceIps = agentTaskDAO.getTaskFileSourceIps(3L, 0);
        assertThat(fileSourceIps.size()).isEqualTo(1);
        assertThat(fileSourceIps.get(0)).isEqualTo("0:127.0.0.2");
    }

    @Test
    public void testFuzzySearchTargetIpsByIp() {
        List<String> matchIps = agentTaskDAO.fuzzySearchTargetIpsByIp(1L, 0, "0.0.2");
        assertThat(matchIps.size()).isEqualTo(1);
        assertThat(matchIps.get(0)).isEqualTo("0:127.0.0.2");
    }


}
