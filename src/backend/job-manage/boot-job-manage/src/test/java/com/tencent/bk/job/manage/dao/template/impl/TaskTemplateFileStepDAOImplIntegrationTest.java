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

package com.tencent.bk.job.manage.dao.template.impl;

import com.tencent.bk.job.common.constant.DuplicateHandlerEnum;
import com.tencent.bk.job.common.constant.NotExistPathHandlerEnum;
import com.tencent.bk.job.common.util.RandomUtil;
import com.tencent.bk.job.manage.dao.TaskFileStepDAO;
import com.tencent.bk.job.manage.model.dto.task.TaskFileStepDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 6/10/2019 09:11
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:test.properties")
@Sql(value = {"/template/init_template_file_step_data.sql"})
@SqlConfig(encoding = "utf-8")
class TaskTemplateFileStepDAOImplIntegrationTest {

    private static final TaskFileStepDTO FILE_STEP_1 = new TaskFileStepDTO();
    private static final TaskFileStepDTO FILE_STEP_2 = new TaskFileStepDTO();
    private static final TaskFileStepDTO FILE_STEP_3 = new TaskFileStepDTO();

    @Autowired
    @Qualifier("TaskTemplateFileStepDAOImpl")
    private TaskFileStepDAO taskFileStepDAO;

    @BeforeEach
    void initTest() {
        FILE_STEP_1.setId(1L);
        FILE_STEP_1.setStepId(1000L);
        FILE_STEP_1.setOriginFileList(Collections.emptyList());
        FILE_STEP_1.setDestinationFileLocation("/root/tmp");
        FILE_STEP_1.setExecuteAccount(1L);
        FILE_STEP_1.setDestinationHostList(null);
        FILE_STEP_1.setTimeout(6000L);
        FILE_STEP_1.setOriginSpeedLimit(100L);
        FILE_STEP_1.setTargetSpeedLimit(200L);
        FILE_STEP_1.setIgnoreError(false);
        FILE_STEP_1.setDuplicateHandler(DuplicateHandlerEnum.OVERWRITE);
        FILE_STEP_1.setNotExistPathHandler(NotExistPathHandlerEnum.CREATE_DIR);

        FILE_STEP_2.setId(2L);
        FILE_STEP_2.setStepId(2000L);
        FILE_STEP_2.setOriginFileList(Collections.emptyList());
        FILE_STEP_2.setDestinationFileLocation("/home/user1/tmp");
        FILE_STEP_2.setExecuteAccount(2L);
        FILE_STEP_2.setDestinationHostList(null);
        FILE_STEP_2.setTimeout(6000L);
        FILE_STEP_2.setOriginSpeedLimit(null);
        FILE_STEP_2.setTargetSpeedLimit(null);
        FILE_STEP_2.setIgnoreError(true);
        FILE_STEP_2.setDuplicateHandler(DuplicateHandlerEnum.GROUP_BY_IP);
        FILE_STEP_2.setNotExistPathHandler(NotExistPathHandlerEnum.CREATE_DIR);

        FILE_STEP_3.setId(3L);
        FILE_STEP_3.setStepId(3000L);
        FILE_STEP_3.setOriginFileList(Collections.emptyList());
        FILE_STEP_3.setDestinationFileLocation("/home/userC/tmp");
        FILE_STEP_3.setExecuteAccount(3L);
        FILE_STEP_3.setDestinationHostList(null);
        FILE_STEP_3.setTimeout(6000L);
        FILE_STEP_3.setOriginSpeedLimit(100L);
        FILE_STEP_3.setTargetSpeedLimit(200L);
        FILE_STEP_3.setIgnoreError(false);
        FILE_STEP_3.setDuplicateHandler(DuplicateHandlerEnum.OVERWRITE);
        FILE_STEP_3.setNotExistPathHandler(NotExistPathHandlerEnum.CREATE_DIR);
    }

    @Test
    void givenNormalStepIdReturnFileStepInfo() {
        assertThat(taskFileStepDAO.getFileStepById(FILE_STEP_1.getStepId())).isEqualTo(FILE_STEP_1);
        assertThat(taskFileStepDAO.getFileStepById(FILE_STEP_2.getStepId())).isEqualTo(FILE_STEP_2);
        assertThat(taskFileStepDAO.getFileStepById(FILE_STEP_3.getStepId())).isEqualTo(FILE_STEP_3);
    }

    @Test
    void givenNotExistStepIdReturnNull() {
        assertThat(taskFileStepDAO.getFileStepById(9999999L)).isNull();
    }

    @Test
    void givenFileStepInfoReturnNewFileStepId() {
        TaskFileStepDTO scriptStep = new TaskFileStepDTO();

        scriptStep.setStepId(RandomUtil.getRandomPositiveLong());
        scriptStep.setOriginFileList(Collections.emptyList());
        scriptStep.setDestinationFileLocation(UUID.randomUUID().toString());
        scriptStep.setExecuteAccount(RandomUtil.getRandomPositiveLong());
        scriptStep.setDestinationHostList(null);
        scriptStep.setTimeout(RandomUtil.getRandomPositiveLong());
        scriptStep.setDuplicateHandler(DuplicateHandlerEnum.OVERWRITE);
        scriptStep.setNotExistPathHandler(NotExistPathHandlerEnum.CREATE_DIR);
        scriptStep.setIgnoreError(false);

        Long fileStepId = taskFileStepDAO.insertFileStep(scriptStep);
        scriptStep.setId(fileStepId);
        assertThat(taskFileStepDAO.getFileStepById(scriptStep.getStepId())).isEqualTo(scriptStep);

        scriptStep.setId(null);
        scriptStep.setStepId(RandomUtil.getRandomPositiveLong());
        scriptStep.setOriginFileList(Collections.emptyList());
        scriptStep.setDestinationFileLocation(UUID.randomUUID().toString());
        scriptStep.setExecuteAccount(RandomUtil.getRandomPositiveLong());
        scriptStep.setDestinationHostList(null);
        scriptStep.setTimeout(RandomUtil.getRandomPositiveLong());
        scriptStep.setOriginSpeedLimit(RandomUtil.getRandomPositiveLong());
        scriptStep.setTargetSpeedLimit(RandomUtil.getRandomPositiveLong());
        scriptStep.setDuplicateHandler(DuplicateHandlerEnum.GROUP_BY_IP);
        scriptStep.setIgnoreError(true);
        fileStepId = taskFileStepDAO.insertFileStep(scriptStep);
        scriptStep.setId(fileStepId);
        assertThat(taskFileStepDAO.getFileStepById(scriptStep.getStepId())).isEqualTo(scriptStep);
    }

    @Test
    void givenStepIdReturnDeleteSuccess() {
        assertThat(taskFileStepDAO.deleteFileStepById(FILE_STEP_1.getStepId())).isTrue();
        assertThat(taskFileStepDAO.getFileStepById(FILE_STEP_1.getStepId())).isNull();
        assertThat(taskFileStepDAO.deleteFileStepById(FILE_STEP_1.getStepId())).isFalse();
    }

    @Test
    void givenWrongStepIdReturnDeleteFailed() {
        assertThat(taskFileStepDAO.deleteFileStepById(999999999999L)).isFalse();

        assertThat(taskFileStepDAO.getFileStepById(FILE_STEP_1.getStepId())).isEqualTo(FILE_STEP_1);
    }

    @Test
    void givenNewFileStepInfoReturnUpdateSuccess() {
        assertThat(taskFileStepDAO.updateFileStepById(FILE_STEP_1)).isTrue();

        FILE_STEP_1.setId(RandomUtil.getRandomPositiveLong());
        FILE_STEP_1.setOriginFileList(Collections.emptyList());
        FILE_STEP_1.setDestinationFileLocation(UUID.randomUUID().toString());
        FILE_STEP_1.setExecuteAccount(RandomUtil.getRandomPositiveLong());
        FILE_STEP_1.setDestinationHostList(null);
        FILE_STEP_1.setTimeout(RandomUtil.getRandomPositiveLong());
        FILE_STEP_1.setOriginSpeedLimit(RandomUtil.getRandomPositiveLong());
        FILE_STEP_1.setTargetSpeedLimit(RandomUtil.getRandomPositiveLong());
        FILE_STEP_1.setIgnoreError(true);
        FILE_STEP_1.setDuplicateHandler(DuplicateHandlerEnum.GROUP_BY_IP);
        FILE_STEP_1.setNotExistPathHandler(NotExistPathHandlerEnum.CREATE_DIR);

        assertThat(taskFileStepDAO.updateFileStepById(FILE_STEP_1)).isTrue();

        assertThat(taskFileStepDAO.getFileStepById(FILE_STEP_1.getStepId())).isNotEqualTo(FILE_STEP_1);
        FILE_STEP_1.setId(1L);
        assertThat(taskFileStepDAO.getFileStepById(FILE_STEP_1.getStepId())).isEqualTo(FILE_STEP_1);

        assertThat(taskFileStepDAO.updateFileStepById(FILE_STEP_2)).isTrue();

        FILE_STEP_2.setId(RandomUtil.getRandomPositiveLong());
        FILE_STEP_2.setOriginFileList(Collections.emptyList());
        FILE_STEP_2.setDestinationFileLocation(UUID.randomUUID().toString());
        FILE_STEP_2.setExecuteAccount(RandomUtil.getRandomPositiveLong());
        FILE_STEP_2.setDestinationHostList(null);
        FILE_STEP_2.setTimeout(RandomUtil.getRandomPositiveLong());
        FILE_STEP_2.setOriginSpeedLimit(null);
        FILE_STEP_2.setTargetSpeedLimit(null);
        FILE_STEP_2.setIgnoreError(false);
        FILE_STEP_2.setDuplicateHandler(DuplicateHandlerEnum.OVERWRITE);

        assertThat(taskFileStepDAO.updateFileStepById(FILE_STEP_2)).isTrue();

        assertThat(taskFileStepDAO.getFileStepById(FILE_STEP_2.getStepId())).isNotEqualTo(FILE_STEP_2);
        FILE_STEP_2.setId(2L);
        assertThat(taskFileStepDAO.getFileStepById(FILE_STEP_2.getStepId())).isEqualTo(FILE_STEP_2);
    }

    @Test
    void givenWrongFileStepInfoReturnUpdateFailed() {
        FILE_STEP_1.setStepId(RandomUtil.getRandomPositiveLong());
        assertThat(taskFileStepDAO.updateFileStepById(FILE_STEP_1)).isFalse();
    }
}
