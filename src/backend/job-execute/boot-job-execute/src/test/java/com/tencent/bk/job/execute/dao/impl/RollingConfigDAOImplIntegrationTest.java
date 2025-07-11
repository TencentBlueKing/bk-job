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

import com.tencent.bk.job.common.constant.RollingModeEnum;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.dao.RollingConfigDAO;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.model.RollingConfigDTO;
import com.tencent.bk.job.execute.model.db.RollingConfigDetailDO;
import com.tencent.bk.job.execute.model.db.RollingExecuteObjectsBatchDO;
import com.tencent.bk.job.execute.model.db.StepRollingConfigDO;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:test.properties")
@SqlConfig(encoding = "utf-8")
@Sql({"/init_rolling_config_data.sql"})
public class RollingConfigDAOImplIntegrationTest {

    @Autowired
    private RollingConfigDAO rollingConfigDAO;

    @Test
    @DisplayName("根据ID获取滚动配置")
    void queryRollingConfig() {
        RollingConfigDTO savedTaskInstanceRollingConfig =
            rollingConfigDAO.queryRollingConfigById(1L);

        assertThat(savedTaskInstanceRollingConfig.getId()).isEqualTo(1L);
        assertThat(savedTaskInstanceRollingConfig.getConfigName()).isEqualTo("config1");
        assertThat(savedTaskInstanceRollingConfig.getTaskInstanceId()).isEqualTo(1L);
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail()).isNotNull();
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail().getExpr()).isEqualTo("1 10% 100%");
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail().getMode())
            .isEqualTo(RollingModeEnum.PAUSE_IF_FAIL.getValue());
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail().getName()).isEqualTo("config1");
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail().getStepRollingConfigs()).hasSize(4);
        Map<Long, StepRollingConfigDO> stepRollingConfigs =
            savedTaskInstanceRollingConfig.getConfigDetail().getStepRollingConfigs();
        assertThat(stepRollingConfigs.get(100L).isBatch()).isEqualTo(true);
        assertThat(stepRollingConfigs.get(101L).isBatch()).isEqualTo(false);
        assertThat(stepRollingConfigs.get(102L).isBatch()).isEqualTo(true);
        assertThat(stepRollingConfigs.get(103L).isBatch()).isEqualTo(true);
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail().getExecuteObjectsBatchListCompatibly()).hasSize(3);
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail().getExecuteObjectsBatchListCompatibly().get(0)
            .getBatch()).isEqualTo(1);
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail().getExecuteObjectsBatchListCompatibly().get(0)
            .getExecuteObjectsCompatibly()).hasSize(1);
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail().getExecuteObjectsBatchListCompatibly().get(0)
            .getExecuteObjectsCompatibly().get(0).getHost().getBkCloudId()).isEqualTo(0L);
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail().getExecuteObjectsBatchListCompatibly().get(0)
            .getExecuteObjectsCompatibly().get(0).getHost().getIp()).isEqualTo("127.0.0.1");
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail().getExecuteObjectsBatchListCompatibly().get(1)
            .getBatch()).isEqualTo(2);
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail().getExecuteObjectsBatchListCompatibly().get(1)
            .getExecuteObjectsCompatibly()).hasSize(1);
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail().getExecuteObjectsBatchListCompatibly().get(1)
            .getExecuteObjectsCompatibly().get(0).getHost().getBkCloudId()).isEqualTo(0L);
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail().getExecuteObjectsBatchListCompatibly().get(1)
            .getExecuteObjectsCompatibly().get(0).getHost().getIp()).isEqualTo("127.0.0.2");
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail().getExecuteObjectsBatchListCompatibly().get(2)
            .getBatch()).isEqualTo(3);
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail().getExecuteObjectsBatchListCompatibly().get(2)
            .getExecuteObjectsCompatibly()).hasSize(2);
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail().getExecuteObjectsBatchListCompatibly().get(2)
            .getExecuteObjectsCompatibly().get(0).getHost().getBkCloudId()).isEqualTo(0L);
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail().getExecuteObjectsBatchListCompatibly().get(2)
            .getExecuteObjectsCompatibly().get(0).getHost().getIp()).isEqualTo("127.0.0.3");
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail().getExecuteObjectsBatchListCompatibly().get(2)
            .getExecuteObjectsCompatibly().get(1).getHost().getBkCloudId()).isEqualTo(0L);
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail().getExecuteObjectsBatchListCompatibly().get(2)
            .getExecuteObjectsCompatibly().get(1).getHost().getIp()).isEqualTo("127.0.0.4");
    }

    @Test
    @DisplayName("保存作业实例滚动配置")
    void saveRollingConfig() {
        RollingConfigDTO taskInstanceRollingConfig = new RollingConfigDTO();
        taskInstanceRollingConfig.setTaskInstanceId(10L);
        taskInstanceRollingConfig.setConfigName("default");
        RollingConfigDetailDO rollingConfig = new RollingConfigDetailDO();
        rollingConfig.setName("default");
        rollingConfig.setExpr("10%");
        rollingConfig.setMode(RollingModeEnum.PAUSE_IF_FAIL.getValue());
        List<Long> includeStepInstanceIdList = new ArrayList<>();
        includeStepInstanceIdList.add(1000L);
        includeStepInstanceIdList.add(1001L);
        includeStepInstanceIdList.add(1002L);
        includeStepInstanceIdList.add(1003L);
        rollingConfig.setIncludeStepInstanceIdList(includeStepInstanceIdList);
        Map<Long, StepRollingConfigDO> stepRollingConfigs = new HashMap<>();
        stepRollingConfigs.put(1000L, new StepRollingConfigDO(true));
        stepRollingConfigs.put(1001L, new StepRollingConfigDO(false));
        stepRollingConfigs.put(1002L, new StepRollingConfigDO(true));
        stepRollingConfigs.put(1003L, new StepRollingConfigDO(true));
        rollingConfig.setStepRollingConfigs(stepRollingConfigs);
        List<RollingExecuteObjectsBatchDO> executeObjectsBatchList = new ArrayList<>();
        List<ExecuteObject> executeObjects = new ArrayList<>();
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject((new HostDTO(1L, 0L, "127.0.0.1"))));
        RollingExecuteObjectsBatchDO hostBatch1 = new RollingExecuteObjectsBatchDO(1, executeObjects);
        executeObjectsBatchList.add(hostBatch1);
        rollingConfig.setExecuteObjectsBatchList(executeObjectsBatchList);
        taskInstanceRollingConfig.setConfigDetail(rollingConfig);

        long rollingConfigId = rollingConfigDAO.saveRollingConfig(taskInstanceRollingConfig);
        assertThat(rollingConfigId).isGreaterThan(0);

        RollingConfigDTO savedTaskInstanceRollingConfig =
            rollingConfigDAO.queryRollingConfigById(rollingConfigId);

        assertThat(savedTaskInstanceRollingConfig.getId()).isEqualTo(rollingConfigId);
        assertThat(savedTaskInstanceRollingConfig.getConfigName()).isEqualTo("default");
        assertThat(savedTaskInstanceRollingConfig.getTaskInstanceId()).isEqualTo(10L);
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail()).isNotNull();
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail().getExpr()).isEqualTo("10%");
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail().getMode())
            .isEqualTo(RollingModeEnum.PAUSE_IF_FAIL.getValue());
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail().getName()).isEqualTo("default");
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail().getStepRollingConfigs()).hasSize(4);
        Map<Long, StepRollingConfigDO> savedStepRollingConfigs =
            savedTaskInstanceRollingConfig.getConfigDetail().getStepRollingConfigs();
        assertThat(savedStepRollingConfigs.get(1000L).isBatch()).isEqualTo(true);
        assertThat(savedStepRollingConfigs.get(1001L).isBatch()).isEqualTo(false);
        assertThat(savedStepRollingConfigs.get(1002L).isBatch()).isEqualTo(true);
        assertThat(savedStepRollingConfigs.get(1003L).isBatch()).isEqualTo(true);
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail().getExecuteObjectsBatchListCompatibly()).hasSize(1);
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail().getExecuteObjectsBatchListCompatibly().get(0)
            .getBatch()).isEqualTo(1);
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail().getExecuteObjectsBatchListCompatibly().get(0)
            .getExecuteObjectsCompatibly().get(0).getHost().getHostId()).isEqualTo(1L);
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail().getExecuteObjectsBatchListCompatibly().get(0)
            .getExecuteObjectsCompatibly().get(0).getHost().getBkCloudId()).isEqualTo(0L);
        assertThat(savedTaskInstanceRollingConfig.getConfigDetail().getExecuteObjectsBatchListCompatibly().get(0)
            .getExecuteObjectsCompatibly().get(0).getHost().getIp()).isEqualTo("127.0.0.1");
    }
}

