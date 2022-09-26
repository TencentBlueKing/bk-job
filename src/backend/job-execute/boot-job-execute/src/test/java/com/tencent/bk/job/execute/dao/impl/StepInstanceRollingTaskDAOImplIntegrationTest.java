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
import com.tencent.bk.job.execute.dao.StepInstanceRollingTaskDAO;
import com.tencent.bk.job.execute.model.StepInstanceRollingTaskDTO;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:test.properties")
@SqlConfig(encoding = "utf-8")
@Sql({"/init_step_instance_rolling_task_data.sql"})
public class StepInstanceRollingTaskDAOImplIntegrationTest {

    @Autowired
    private StepInstanceRollingTaskDAO stepInstanceRollingTaskDAO;

    @Test
    @DisplayName("根据步骤实例ID、执行次数、滚动批次查询步骤滚动任务")
    void queryRollingTask() {
        StepInstanceRollingTaskDTO rollingTask = stepInstanceRollingTaskDAO.queryRollingTask(1, 0, 1);
        assertThat(rollingTask).isNotNull();
        assertThat(rollingTask.getStepInstanceId()).isEqualTo(1L);
        assertThat(rollingTask.getExecuteCount()).isEqualTo(0);
        assertThat(rollingTask.getBatch()).isEqualTo(1);
        assertThat(rollingTask.getStatus()).isEqualTo(RunStatusEnum.BLANK);
        assertThat(rollingTask.getStartTime()).isEqualTo(1642247802000L);
        assertThat(rollingTask.getEndTime()).isEqualTo(1642247803000L);
        assertThat(rollingTask.getTotalTime()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("根据步骤实例ID查询步骤滚动任务")
    void listRollingTasks() {
        List<StepInstanceRollingTaskDTO> rollingTasks = stepInstanceRollingTaskDAO.listRollingTasks(1L, null, null);
        assertThat(rollingTasks).hasSize(2);

        StepInstanceRollingTaskDTO rollingTask1 = rollingTasks.get(0);
        assertThat(rollingTask1).isNotNull();
        assertThat(rollingTask1.getStepInstanceId()).isEqualTo(1L);
        assertThat(rollingTask1.getExecuteCount()).isEqualTo(0);
        assertThat(rollingTask1.getBatch()).isEqualTo(1);
        assertThat(rollingTask1.getStatus()).isEqualTo(RunStatusEnum.BLANK);
        assertThat(rollingTask1.getStartTime()).isEqualTo(1642247802000L);
        assertThat(rollingTask1.getEndTime()).isEqualTo(1642247803000L);
        assertThat(rollingTask1.getTotalTime()).isEqualTo(1000L);

        StepInstanceRollingTaskDTO rollingTask2 = rollingTasks.get(1);
        assertThat(rollingTask2).isNotNull();
        assertThat(rollingTask2.getStepInstanceId()).isEqualTo(1L);
        assertThat(rollingTask2.getExecuteCount()).isEqualTo(0);
        assertThat(rollingTask2.getBatch()).isEqualTo(2);
        assertThat(rollingTask2.getStatus()).isEqualTo(RunStatusEnum.BLANK);
        assertThat(rollingTask2.getStartTime()).isEqualTo(1642247804000L);
        assertThat(rollingTask2.getEndTime()).isEqualTo(1642247805000L);
        assertThat(rollingTask2.getTotalTime()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("保存步骤滚动任务")
    void saveRollingTask() {
        StepInstanceRollingTaskDTO rollingTask = new StepInstanceRollingTaskDTO();
        rollingTask.setStepInstanceId(100L);
        rollingTask.setExecuteCount(0);
        rollingTask.setBatch(1);
        rollingTask.setStatus(RunStatusEnum.ROLLING_WAITING);
        rollingTask.setStartTime(1642247802000L);
        stepInstanceRollingTaskDAO.saveRollingTask(rollingTask);

        StepInstanceRollingTaskDTO savedRollingTask = stepInstanceRollingTaskDAO.queryRollingTask(100L, 0, 1);
        assertThat(savedRollingTask).isNotNull();
        assertThat(savedRollingTask.getStepInstanceId()).isEqualTo(100L);
        assertThat(savedRollingTask.getExecuteCount()).isEqualTo(0);
        assertThat(savedRollingTask.getBatch()).isEqualTo(1);
        assertThat(savedRollingTask.getStatus()).isEqualTo(RunStatusEnum.ROLLING_WAITING);
        assertThat(savedRollingTask.getStartTime()).isEqualTo(1642247802000L);
        assertThat(savedRollingTask.getEndTime()).isNull();
        assertThat(savedRollingTask.getTotalTime()).isNull();
    }

    @Test
    void updateRollingTask() {
        long startTime = System.currentTimeMillis();
        Long endTime = startTime + 1000L;
        Long totalTime = 1000L;
        stepInstanceRollingTaskDAO.updateRollingTask(1L, 0, 1, RunStatusEnum.SUCCESS, startTime, endTime, totalTime);

        StepInstanceRollingTaskDTO savedRollingTask = stepInstanceRollingTaskDAO.queryRollingTask(1L, 0, 1);

        assertThat(savedRollingTask).isNotNull();
        assertThat(savedRollingTask.getStepInstanceId()).isEqualTo(1L);
        assertThat(savedRollingTask.getExecuteCount()).isEqualTo(0);
        assertThat(savedRollingTask.getBatch()).isEqualTo(1);
        assertThat(savedRollingTask.getStatus()).isEqualTo(RunStatusEnum.SUCCESS);
        assertThat(savedRollingTask.getStartTime()).isEqualTo(startTime);
        assertThat(savedRollingTask.getEndTime()).isEqualTo(endTime);
        assertThat(savedRollingTask.getTotalTime()).isEqualTo(totalTime);

    }
}
