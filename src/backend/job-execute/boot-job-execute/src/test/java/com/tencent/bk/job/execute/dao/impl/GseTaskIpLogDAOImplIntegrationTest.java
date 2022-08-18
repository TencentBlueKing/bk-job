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
