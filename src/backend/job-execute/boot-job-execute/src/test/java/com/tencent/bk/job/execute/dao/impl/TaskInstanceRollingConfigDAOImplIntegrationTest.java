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

import com.tencent.bk.job.common.constant.RollingModeEnum;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.execute.dao.TaskInstanceRollingConfigDAO;
import com.tencent.bk.job.execute.model.TaskInstanceRollingConfigDTO;
import com.tencent.bk.job.execute.model.db.RollingConfigDO;
import com.tencent.bk.job.execute.model.db.RollingServerBatchDO;
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
@Sql({"/init_task_instance_rolling_config_data.sql"})
public class TaskInstanceRollingConfigDAOImplIntegrationTest {

    @Autowired
    private TaskInstanceRollingConfigDAO taskInstanceRollingConfigDAO;

    @Test
    @DisplayName("根据ID获取滚动配置")
    void queryRollingConfig() {
        TaskInstanceRollingConfigDTO savedTaskInstanceRollingConfig =
            taskInstanceRollingConfigDAO.queryRollingConfigById(1L);

        assertThat(savedTaskInstanceRollingConfig.getId()).isEqualTo(1L);
        assertThat(savedTaskInstanceRollingConfig.getConfigName()).isEqualTo("config1");
        assertThat(savedTaskInstanceRollingConfig.getTaskInstanceId()).isEqualTo(1L);
        assertThat(savedTaskInstanceRollingConfig.getConfig()).isNotNull();
        assertThat(savedTaskInstanceRollingConfig.getConfig().getExpr()).isEqualTo("1 10% 100%");
        assertThat(savedTaskInstanceRollingConfig.getConfig().getMode()).isEqualTo(RollingModeEnum.PAUSE_IF_FAIL.getValue());
        assertThat(savedTaskInstanceRollingConfig.getConfig().getName()).isEqualTo("config1");
        assertThat(savedTaskInstanceRollingConfig.getConfig().getIncludeStepInstanceIdList()).containsSequence(100L,
            101L, 102L, 103L);
        assertThat(savedTaskInstanceRollingConfig.getConfig().getBatchRollingStepInstanceIdList()).containsSequence(100L,
            102L, 103L);
        assertThat(savedTaskInstanceRollingConfig.getConfig().getAllRollingStepInstanceIdList()).containsSequence(101L);
        assertThat(savedTaskInstanceRollingConfig.getConfig().getServerBatchList()).hasSize(3);
        assertThat(savedTaskInstanceRollingConfig.getConfig().getServerBatchList().get(0).getBatch()).isEqualTo(1);
        assertThat(savedTaskInstanceRollingConfig.getConfig().getServerBatchList().get(0).getServers()).hasSize(1);
        assertThat(savedTaskInstanceRollingConfig.getConfig().getServerBatchList().get(0).getServers().get(0).getCloudAreaId()).isEqualTo(0L);
        assertThat(savedTaskInstanceRollingConfig.getConfig().getServerBatchList().get(0).getServers().get(0).getIp()).isEqualTo("127.0.0.1");
        assertThat(savedTaskInstanceRollingConfig.getConfig().getServerBatchList().get(1).getBatch()).isEqualTo(2);
        assertThat(savedTaskInstanceRollingConfig.getConfig().getServerBatchList().get(1).getServers()).hasSize(1);
        assertThat(savedTaskInstanceRollingConfig.getConfig().getServerBatchList().get(1).getServers().get(0).getCloudAreaId()).isEqualTo(0L);
        assertThat(savedTaskInstanceRollingConfig.getConfig().getServerBatchList().get(1).getServers().get(0).getIp()).isEqualTo("127.0.0.2");
        assertThat(savedTaskInstanceRollingConfig.getConfig().getServerBatchList().get(2).getBatch()).isEqualTo(3);
        assertThat(savedTaskInstanceRollingConfig.getConfig().getServerBatchList().get(2).getServers()).hasSize(2);
        assertThat(savedTaskInstanceRollingConfig.getConfig().getServerBatchList().get(2).getServers().get(0).getCloudAreaId()).isEqualTo(0L);
        assertThat(savedTaskInstanceRollingConfig.getConfig().getServerBatchList().get(2).getServers().get(0).getIp()).isEqualTo("127.0.0.3");
        assertThat(savedTaskInstanceRollingConfig.getConfig().getServerBatchList().get(2).getServers().get(1).getCloudAreaId()).isEqualTo(0L);
        assertThat(savedTaskInstanceRollingConfig.getConfig().getServerBatchList().get(2).getServers().get(1).getIp()).isEqualTo("127.0.0.4");
    }

    @Test
    @DisplayName("保存作业实例滚动配置")
    void saveRollingConfig() {
        TaskInstanceRollingConfigDTO taskInstanceRollingConfig = new TaskInstanceRollingConfigDTO();
        taskInstanceRollingConfig.setTaskInstanceId(10L);
        taskInstanceRollingConfig.setConfigName("default");
        RollingConfigDO rollingConfig = new RollingConfigDO();
        rollingConfig.setName("default");
        rollingConfig.setExpr("10%");
        rollingConfig.setMode(RollingModeEnum.PAUSE_IF_FAIL.getValue());
        List<Long> includeStepInstanceIdList = new ArrayList<>();
        includeStepInstanceIdList.add(1000L);
        includeStepInstanceIdList.add(1001L);
        includeStepInstanceIdList.add(1002L);
        includeStepInstanceIdList.add(1003L);
        rollingConfig.setIncludeStepInstanceIdList(includeStepInstanceIdList);
        List<Long> rollingBatchStepInstanceIdList = new ArrayList<>();
        rollingBatchStepInstanceIdList.add(1000L);
        rollingBatchStepInstanceIdList.add(1002L);
        rollingBatchStepInstanceIdList.add(1003L);
        rollingConfig.setBatchRollingStepInstanceIdList(rollingBatchStepInstanceIdList);
        List<Long> rollingAllStepInstanceIdList = new ArrayList<>();
        rollingAllStepInstanceIdList.add(1001L);
        rollingConfig.setAllRollingStepInstanceIdList(rollingAllStepInstanceIdList);
        List<RollingServerBatchDO> serverBatchList = new ArrayList<>();
        List<IpDTO> servers = new ArrayList<>();
        servers.add(new IpDTO(0L, "127.0.0.1"));
        RollingServerBatchDO serverBatch1 = new RollingServerBatchDO(1, servers);
        serverBatchList.add(serverBatch1);
        rollingConfig.setServerBatchList(serverBatchList);
        taskInstanceRollingConfig.setConfig(rollingConfig);

        long rollingConfigId = taskInstanceRollingConfigDAO.saveRollingConfig(taskInstanceRollingConfig);
        assertThat(rollingConfigId).isGreaterThan(0);

        TaskInstanceRollingConfigDTO savedTaskInstanceRollingConfig =
            taskInstanceRollingConfigDAO.queryRollingConfigById(rollingConfigId);

        assertThat(savedTaskInstanceRollingConfig.getId()).isEqualTo(rollingConfigId);
        assertThat(savedTaskInstanceRollingConfig.getConfigName()).isEqualTo("default");
        assertThat(savedTaskInstanceRollingConfig.getTaskInstanceId()).isEqualTo(10L);
        assertThat(savedTaskInstanceRollingConfig.getConfig()).isNotNull();
        assertThat(savedTaskInstanceRollingConfig.getConfig().getExpr()).isEqualTo("10%");
        assertThat(savedTaskInstanceRollingConfig.getConfig().getMode()).isEqualTo(RollingModeEnum.PAUSE_IF_FAIL.getValue());
        assertThat(savedTaskInstanceRollingConfig.getConfig().getName()).isEqualTo("default");
        assertThat(savedTaskInstanceRollingConfig.getConfig().getIncludeStepInstanceIdList()).containsSequence(1000L,
            1001L, 1002L, 1003L);
        assertThat(savedTaskInstanceRollingConfig.getConfig().getBatchRollingStepInstanceIdList()).containsSequence(1000L,
            1002L, 1003L);
        assertThat(savedTaskInstanceRollingConfig.getConfig().getAllRollingStepInstanceIdList()).containsSequence(1001L);
        assertThat(savedTaskInstanceRollingConfig.getConfig().getServerBatchList()).hasSize(1);
        assertThat(savedTaskInstanceRollingConfig.getConfig().getServerBatchList().get(0).getBatch()).isEqualTo(1);
        assertThat(savedTaskInstanceRollingConfig.getConfig().getServerBatchList().get(0).getServers().get(0).getCloudAreaId()).isEqualTo(0L);
        assertThat(savedTaskInstanceRollingConfig.getConfig().getServerBatchList().get(0).getServers().get(0).getIp()).isEqualTo("127.0.0.1");
    }
}

