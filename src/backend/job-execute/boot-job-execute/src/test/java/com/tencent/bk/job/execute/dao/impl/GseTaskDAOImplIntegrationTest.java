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
import com.tencent.bk.job.execute.dao.GseTaskDAO;
import com.tencent.bk.job.execute.model.GseTaskDTO;
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

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:test.properties")
@SqlConfig(encoding = "utf-8")
@Sql({"/init_gse_task_data.sql"})
public class GseTaskDAOImplIntegrationTest {

    @Autowired
    private GseTaskDAO gseTaskDAO;

    @Test
    @DisplayName("保存GseTask")
    void saveGseTask() {
        GseTaskDTO gseTask = new GseTaskDTO();
        gseTask.setStepInstanceId(10L);
        gseTask.setExecuteCount(0);
        gseTask.setBatch(1);
        gseTask.setStatus(RunStatusEnum.SUCCESS.getValue());
        gseTask.setStartTime(1660639907757L);
        gseTask.setEndTime(1660639914013L);
        gseTask.setTotalTime(6256L);
        gseTask.setGseTaskId("GSE_TASK_10");

        long id = gseTaskDAO.saveGseTask(gseTask);
        assertThat(id).isGreaterThan(0);

        GseTaskDTO savedGseTask = gseTaskDAO.getGseTask(id);

        assertThat(savedGseTask.getStepInstanceId()).isEqualTo(10L);
        assertThat(savedGseTask.getExecuteCount()).isEqualTo(0);
        assertThat(savedGseTask.getBatch()).isEqualTo(1);
        assertThat(savedGseTask.getStatus()).isEqualTo(RunStatusEnum.SUCCESS.getValue());
        assertThat(savedGseTask.getStartTime()).isEqualTo(1660639907757L);
        assertThat(savedGseTask.getEndTime()).isEqualTo(1660639914013L);
        assertThat(savedGseTask.getTotalTime()).isEqualTo(6256L);
        assertThat(savedGseTask.getGseTaskId()).isEqualTo("GSE_TASK_10");
    }

    @Test
    @DisplayName("更新GseTask")
    void updateGseTask() {
        GseTaskDTO gseTask = new GseTaskDTO();
        gseTask.setId(1L);
        gseTask.setStepInstanceId(1L);
        gseTask.setExecuteCount(0);
        gseTask.setBatch(0);
        gseTask.setStatus(RunStatusEnum.SUCCESS.getValue());
        gseTask.setStartTime(1660639907757L);
        gseTask.setEndTime(1660639914013L);
        gseTask.setTotalTime(6256L);
        gseTask.setGseTaskId("GSE_TASK_2");

        boolean result = gseTaskDAO.updateGseTask(gseTask);
        assertThat(result).isTrue();

        GseTaskDTO savedGseTask = gseTaskDAO.getGseTask(1L);

        assertThat(savedGseTask.getStepInstanceId()).isEqualTo(1L);
        assertThat(savedGseTask.getExecuteCount()).isEqualTo(0);
        assertThat(savedGseTask.getBatch()).isEqualTo(0);
        assertThat(savedGseTask.getStatus()).isEqualTo(RunStatusEnum.SUCCESS.getValue());
        assertThat(savedGseTask.getStartTime()).isEqualTo(1660639907757L);
        assertThat(savedGseTask.getEndTime()).isEqualTo(1660639914013L);
        assertThat(savedGseTask.getTotalTime()).isEqualTo(6256L);
        assertThat(savedGseTask.getGseTaskId()).isEqualTo("GSE_TASK_2");
    }
}

