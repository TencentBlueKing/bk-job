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

import com.tencent.bk.job.execute.dao.GseTaskIpLogDAO;
import com.tencent.bk.job.execute.engine.consts.AgentTaskStatusEnum;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
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
public class GseTaskIpLogDAOImplIntegrationTest {
    @Autowired
    private GseTaskIpLogDAO gseTaskIpLogDAO;

    @Test
    public void testGetAgentTaskByIp() {
        String ip = "0:127.0.0.1";
        long stepInstanceId = 1L;
        int executeCount = 0;
        AgentTaskDTO agentTask = gseTaskIpLogDAO.getAgentTaskByIp(stepInstanceId, executeCount, ip);

        assertThat(agentTask.getStepInstanceId()).isEqualTo(stepInstanceId);
        assertThat(agentTask.getExecuteCount()).isEqualTo(executeCount);
        assertThat(agentTask.getCloudIp()).isEqualTo(ip);
        assertThat(agentTask.getStatus()).isEqualTo(AgentTaskStatusEnum.SUCCESS);
        Long expectStartTime = 1565767148000L;
        Long expectEndTime = 1565767149000L;
        assertThat(agentTask.getStartTime()).isEqualTo(expectStartTime);
        assertThat(agentTask.getEndTime()).isEqualTo(expectEndTime);
        assertThat(agentTask.getTotalTime()).isEqualTo(1316L);
        assertThat(agentTask.getErrorCode()).isEqualTo(0);
        assertThat(agentTask.getExitCode()).isEqualTo(0);
        assertThat(agentTask.getTag()).isEqualTo("succ");
        assertThat(agentTask.getScriptLogOffset()).isEqualTo(0);
    }

    @Test
    public void testBatchSaveAgentTasks() {
        List<AgentTaskDTO> agentTasks = new ArrayList<>();
        AgentTaskDTO agentTask1 = new AgentTaskDTO();
        agentTask1.setStepInstanceId(1L);
        agentTask1.setExecuteCount(0);
        agentTask1.setCloudIp("0:127.0.0.1");
        agentTask1.setErrorCode(99);
        agentTask1.setStatus(AgentTaskStatusEnum.AGENT_ERROR);
        agentTask1.setExitCode(1);
        agentTasks.add(agentTask1);

        AgentTaskDTO agentTask2 = new AgentTaskDTO();
        agentTask2.setStepInstanceId(3L);
        agentTask2.setExecuteCount(0);
        agentTask2.setCloudIp("0:127.0.0.1");
        agentTask2.setErrorCode(88);
        agentTask2.setExitCode(1);
        long startTime = 1572858330000L;
        agentTask2.setStartTime(startTime);
        long endTime = 1572858331000L;
        agentTask2.setEndTime(endTime);
        agentTask2.setStatus(AgentTaskStatusEnum.HOST_NOT_EXIST);
        agentTasks.add(agentTask2);

        gseTaskIpLogDAO.batchSaveAgentTasks(agentTasks);

        AgentTaskDTO resultAgentTask1 = gseTaskIpLogDAO.getAgentTaskByIp(1L, 0, "0:127.0.0.1");
        assertThat(resultAgentTask1.getStepInstanceId()).isEqualTo(1L);
        assertThat(resultAgentTask1.getExecuteCount()).isEqualTo(0L);
        assertThat(resultAgentTask1.getCloudIp()).isEqualTo("0:127.0.0.1");
        assertThat(resultAgentTask1.getErrorCode()).isEqualTo(99);
        assertThat(resultAgentTask1.getStatus()).isEqualTo(AgentTaskStatusEnum.AGENT_ERROR);
        assertThat(resultAgentTask1.getExitCode()).isEqualTo(1);


        AgentTaskDTO resultAgentTask2 = gseTaskIpLogDAO.getAgentTaskByIp(3L, 0, "0:127.0.0.1");
        assertThat(resultAgentTask2.getStepInstanceId()).isEqualTo(3L);
        assertThat(resultAgentTask2.getExecuteCount()).isEqualTo(0L);
        assertThat(resultAgentTask2.getCloudIp()).isEqualTo("0:127.0.0.1");
        assertThat(resultAgentTask2.getStartTime()).isEqualTo(startTime);
        assertThat(resultAgentTask2.getEndTime()).isEqualTo(endTime);
        assertThat(resultAgentTask2.getErrorCode()).isEqualTo(88);
        assertThat(resultAgentTask2.getStatus()).isEqualTo(AgentTaskStatusEnum.HOST_NOT_EXIST);
        assertThat(resultAgentTask2.getExitCode()).isEqualTo(1);
    }

    @Test
    public void testGetSuccessAgentTaskCount() {
        Integer count = gseTaskIpLogDAO.getSuccessAgentTaskCount(1L, 0);
        assertThat(count).isEqualTo(2);
    }

    @Test
    public void testListAgentTaskByResultGroup() {
        List<AgentTaskDTO> agentTasks = gseTaskIpLogDAO.listAgentTaskByResultGroup(1L, 0, 9, "succ");
        assertThat(agentTasks.size()).isEqualTo(2);
        assertThat(agentTasks).extracting("tag").containsOnly("succ", "succ");
        assertThat(agentTasks).extracting("stepInstanceId").containsOnly(1L, 1L);
    }

}
