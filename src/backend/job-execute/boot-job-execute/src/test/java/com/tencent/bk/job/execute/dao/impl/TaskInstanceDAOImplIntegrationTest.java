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

import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.dao.TaskInstanceDAO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceQuery;
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

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:test.properties")
@SqlConfig(encoding = "utf-8")
@Sql({"/init_task_instance_data.sql"})
class TaskInstanceDAOImplIntegrationTest {

    @Autowired
    private TaskInstanceDAO taskInstanceDAO;

    @Test
    void testGetTaskInstance() {
        TaskInstanceDTO taskInstance = taskInstanceDAO.getTaskInstance(1L);

        assertThat(taskInstance.getId()).isEqualTo(1L);
        assertThat(taskInstance.getAppId()).isEqualTo(2L);
        assertThat(taskInstance.getTaskId()).isEqualTo(1L);
        assertThat(taskInstance.getTaskTemplateId()).isEqualTo(1L);
        assertThat(taskInstance.getName()).isEqualTo("task1");
        assertThat(taskInstance.getType()).isEqualTo(TaskTypeEnum.SCRIPT.getValue());
        assertThat(taskInstance.getOperator()).isEqualTo("admin");
        assertThat(taskInstance.getCreateTime()).isEqualTo(1572868800000L);
        assertThat(taskInstance.getStartTime()).isEqualTo(1572868800000L);
        assertThat(taskInstance.getEndTime()).isEqualTo(1572868801000L);
        assertThat(taskInstance.getStatus()).isEqualTo(RunStatusEnum.SUCCESS);
        assertThat(taskInstance.getTotalTime()).isEqualTo(1111L);
        assertThat(taskInstance.getStartupMode()).isEqualTo(1);
        assertThat(taskInstance.getCallbackUrl()).isEqualTo("http://bkjob.com");
        assertThat(taskInstance.getAppCode()).isEqualTo("bk_monitor");
    }

    @Test
    void testAddTaskInstance() {
        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setTaskId(3L);
        taskInstance.setTaskTemplateId(3L);
        taskInstance.setAppId(2L);
        taskInstance.setName("task3");
        taskInstance.setOperator("user1");
        taskInstance.setType(TaskTypeEnum.NORMAL.getValue());
        taskInstance.setCreateTime(1572955200000L);
        taskInstance.setStatus(RunStatusEnum.BLANK);
        taskInstance.setStartupMode(TaskStartupModeEnum.API.getValue());
        taskInstance.setCallbackUrl("http://bkjob.com");
        taskInstance.setAppCode("bk_monitor");

        Long taskInstanceId = taskInstanceDAO.addTaskInstance(taskInstance);

        TaskInstanceDTO returnTaskInstance = taskInstanceDAO.getTaskInstance(taskInstanceId);

        assertThat(returnTaskInstance.getId()).isEqualTo(4L);
        assertThat(returnTaskInstance.getAppId()).isEqualTo(2L);
        assertThat(returnTaskInstance.getTaskId()).isEqualTo(3L);
        assertThat(returnTaskInstance.getTaskTemplateId()).isEqualTo(3L);
        assertThat(returnTaskInstance.getName()).isEqualTo("task3");
        assertThat(taskInstance.getType()).isEqualTo(TaskTypeEnum.NORMAL.getValue());
        assertThat(returnTaskInstance.getOperator()).isEqualTo("user1");
        assertThat(returnTaskInstance.getCreateTime()).isEqualTo(1572955200000L);
        assertThat(returnTaskInstance.getStartTime()).isNull();
        assertThat(returnTaskInstance.getEndTime()).isNull();
        assertThat(returnTaskInstance.getStatus()).isEqualTo(RunStatusEnum.BLANK);
        assertThat(returnTaskInstance.getStartupMode()).isEqualTo(TaskStartupModeEnum.API.getValue());
        assertThat(returnTaskInstance.getCallbackUrl()).isEqualTo("http://bkjob.com");
        assertThat(taskInstance.getAppCode()).isEqualTo("bk_monitor");
    }

    @Test
    void testGetTaskInstanceByTaskId() {
        long taskId = 1L;

        List<TaskInstanceDTO> taskInstances = taskInstanceDAO.getTaskInstanceByTaskId(taskId);

        assertThat(taskInstances).hasSize(2).extracting("id").containsOnly(1L, 2L);

    }

    @Test
    void testUpdateTaskStatus() {
        long taskInstanceId = 2L;
        taskInstanceDAO.updateTaskStatus(taskInstanceId, RunStatusEnum.ABNORMAL_STATE.getValue());

        TaskInstanceDTO taskInstanceDTO = taskInstanceDAO.getTaskInstance(taskInstanceId);
        assertThat(taskInstanceDTO.getId()).isEqualTo(taskInstanceId);
        assertThat(taskInstanceDTO.getStatus()).isEqualTo(RunStatusEnum.ABNORMAL_STATE);
    }

    @Test
    void testUpdateTaskStartTime() {
        long taskInstanceId = 2L;
        long startTime = 1572955200000L;

        taskInstanceDAO.updateTaskStartTime(taskInstanceId, startTime);

        TaskInstanceDTO taskInstanceDTO = taskInstanceDAO.getTaskInstance(taskInstanceId);
        assertThat(taskInstanceDTO.getId()).isEqualTo(taskInstanceId);
        assertThat(taskInstanceDTO.getStartTime()).isEqualTo(startTime);
    }

    @Test
    void testUpdateTaskEndTime() {
        long taskInstanceId = 2L;
        Long endTime = 1572955200000L;

        taskInstanceDAO.updateTaskEndTime(taskInstanceId, endTime);

        TaskInstanceDTO taskInstanceDTO = taskInstanceDAO.getTaskInstance(taskInstanceId);
        assertThat(taskInstanceDTO.getId()).isEqualTo(taskInstanceId);
        assertThat(taskInstanceDTO.getEndTime()).isEqualTo(endTime);
    }

    @Test
    void testUpdateTaskCurrentStepId() {
        long taskInstanceId = 2L;
        long stepInstanceId = 100L;
        taskInstanceDAO.updateTaskCurrentStepId(taskInstanceId, stepInstanceId);

        TaskInstanceDTO taskInstanceDTO = taskInstanceDAO.getTaskInstance(taskInstanceId);

        assertThat(taskInstanceDTO.getId()).isEqualTo(taskInstanceId);
        assertThat(taskInstanceDTO.getCurrentStepInstanceId()).isEqualTo(stepInstanceId);


    }

    @Test
    void testResetTaskStatus() {
        long taskInstanceId = 2L;
        taskInstanceDAO.resetTaskStatus(taskInstanceId);

        TaskInstanceDTO taskInstanceDTO = taskInstanceDAO.getTaskInstance(taskInstanceId);
        assertThat(taskInstanceDTO.getId()).isEqualTo(taskInstanceId);
        assertThat(taskInstanceDTO.getStartTime()).isNull();
        assertThat(taskInstanceDTO.getEndTime()).isNull();
        assertThat(taskInstanceDTO.getTotalTime()).isNull();
        assertThat(taskInstanceDTO.getCurrentStepInstanceId()).isEqualTo(0L);
    }

    @Test
    void testCleanTaskEndTime() {
        long taskInstanceId = 2L;
        taskInstanceDAO.cleanTaskEndTime(taskInstanceId);

        TaskInstanceDTO taskInstanceDTO = taskInstanceDAO.getTaskInstance(taskInstanceId);

        assertThat(taskInstanceDTO.getEndTime()).isNull();
        assertThat(taskInstanceDTO.getTotalTime()).isNull();
    }

    @Test
    void testUpdateTaskTotalTime() {
        long taskInstanceId = 2L;
        long totalTime = 1612L;
        taskInstanceDAO.updateTaskTotalTime(taskInstanceId, totalTime);

        TaskInstanceDTO taskInstanceDTO = taskInstanceDAO.getTaskInstance(taskInstanceId);

        assertThat(taskInstanceDTO.getId()).isEqualTo(taskInstanceId);
        assertThat(taskInstanceDTO.getTotalTime()).isEqualTo(totalTime);
    }

    @Test
    void testAddCallbackUrl() {
        String callbackUrl = "http://newbkjob.com";
        long taskInstanceId = 2L;

        taskInstanceDAO.addCallbackUrl(taskInstanceId, callbackUrl);

        TaskInstanceDTO taskInstanceDTO = taskInstanceDAO.getTaskInstance(taskInstanceId);

        assertThat(taskInstanceDTO.getId()).isEqualTo(taskInstanceId);
        assertThat(taskInstanceDTO.getCallbackUrl()).isEqualTo(callbackUrl);

    }

    @Test
    @DisplayName("测试分页查询执行实例-使用ID")
    void testQueryPageListById() {
        TaskInstanceQuery taskQuery = new TaskInstanceQuery();
        taskQuery.setTaskInstanceId(1L);
        taskQuery.setAppId(2L);

        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(0);
        baseSearchCondition.setLength(10);

        PageData<TaskInstanceDTO> taskInstanceDTOS = taskInstanceDAO.listPageTaskInstance(taskQuery,
            baseSearchCondition);

        assertThat(taskInstanceDTOS.getTotal()).isEqualTo(1);
        assertThat(taskInstanceDTOS.getData().size()).isEqualTo(1);
        assertThat(taskInstanceDTOS.getData().get(0).getId()).isEqualTo(1L);

    }

    @Test
    @DisplayName("测试分页查询执行实例-使用其他搜索条件")
    void testQueryPageListWithSearchCondition() {
        TaskInstanceQuery taskQuery = new TaskInstanceQuery();
        taskQuery.setAppId(2L);
        taskQuery.setOperator("admin");
        taskQuery.setStartupModes(Collections.singletonList(TaskStartupModeEnum.NORMAL));
        taskQuery.setTaskType(TaskTypeEnum.SCRIPT);
        taskQuery.setStatus(RunStatusEnum.SUCCESS);
        taskQuery.setStartTime(1572865200000L);
        taskQuery.setEndTime(1572868802000L);
        taskQuery.setTaskName("task1");

        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(0);
        baseSearchCondition.setLength(10);

        PageData<TaskInstanceDTO> taskInstanceDTOS = taskInstanceDAO.listPageTaskInstance(taskQuery,
            baseSearchCondition);

        assertThat(taskInstanceDTOS.getTotal()).isEqualTo(1);
        assertThat(taskInstanceDTOS.getData().size()).isEqualTo(1);
        assertThat(taskInstanceDTOS.getData().get(0).getId()).isEqualTo(1L);

    }

    @Test
    @DisplayName("测试分页查询执行实例-使用IP搜索条件")
    void testQueryPageListWithIp() {
        TaskInstanceQuery taskQuery = new TaskInstanceQuery();
        taskQuery.setAppId(2L);
        taskQuery.setOperator("admin");
        taskQuery.setIp("127.0.0.2");

        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(0);
        baseSearchCondition.setLength(10);

        PageData<TaskInstanceDTO> taskInstanceDTOS = taskInstanceDAO.listPageTaskInstance(taskQuery,
            baseSearchCondition);

        assertThat(taskInstanceDTOS.getTotal()).isEqualTo(2);
        assertThat(taskInstanceDTOS.getData().size()).isEqualTo(2);
        assertThat(taskInstanceDTOS.getData()).extracting("id").containsOnly(1L, 2L);
    }

    @Test
    void testUpdateTaskExecutionInfo() {
        long taskInstanceId = 2L;
        Long startTime = 1577808000000L;
        Long endTime = 1577808001000L;
        long totalTime = 1000L;

        taskInstanceDAO.updateTaskExecutionInfo(taskInstanceId, RunStatusEnum.SUCCESS, null, startTime, endTime,
            totalTime);

        TaskInstanceDTO taskInstanceDTO = taskInstanceDAO.getTaskInstance(taskInstanceId);
        assertThat(taskInstanceDTO.getId()).isEqualTo(taskInstanceId);
        assertThat(taskInstanceDTO.getStartTime()).isEqualTo(startTime);
        assertThat(taskInstanceDTO.getEndTime()).isEqualTo(endTime);
        assertThat(taskInstanceDTO.getTotalTime()).isEqualTo(totalTime);
        assertThat(taskInstanceDTO.getStatus()).isEqualTo(RunStatusEnum.SUCCESS);
    }

    @Test
    void testResetTaskExecuteInfoForResume() {
        long taskInstanceId = 2L;

        taskInstanceDAO.resetTaskExecuteInfoForRetry(taskInstanceId);

        TaskInstanceDTO taskInstanceDTO = taskInstanceDAO.getTaskInstance(taskInstanceId);
        assertThat(taskInstanceDTO.getId()).isEqualTo(taskInstanceId);
        assertThat(taskInstanceDTO.getEndTime()).isNull();
        assertThat(taskInstanceDTO.getTotalTime()).isNull();
        assertThat(taskInstanceDTO.getStatus()).isEqualTo(RunStatusEnum.RUNNING);
    }

}
