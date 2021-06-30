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

import com.tencent.bk.job.common.model.dto.ApplicationHostInfoDTO;
import com.tencent.bk.job.manage.common.consts.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.dao.TaskFileInfoDAO;
import com.tencent.bk.job.manage.model.dto.task.TaskFileInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskHostNodeDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetDTO;
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

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 6/10/2019 09:11
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:test.properties")
@Sql(value = {"/template/init_template_file_info_data.sql"})
@SqlConfig(encoding = "utf-8")
class TaskTemplateFileInfoDAOImplIntegrationTest {

    private static final Random random = new Random();
    private static final TaskFileInfoDTO FILE_INFO_1 = new TaskFileInfoDTO();
    private static final TaskFileInfoDTO FILE_INFO_2 = new TaskFileInfoDTO();
    private static final TaskFileInfoDTO FILE_INFO_3 = new TaskFileInfoDTO();
    private static final List<TaskFileInfoDTO> FILE_INFO_LIST = Arrays.asList(FILE_INFO_1, FILE_INFO_2);

    @Autowired
    @Qualifier("TaskTemplateFileInfoDAOImpl")
    private TaskFileInfoDAO taskFileInfoDAO;

    @BeforeEach
    void initTest() {
        TaskTargetDTO target1 = new TaskTargetDTO();
        target1.setHostNodeList(new TaskHostNodeDTO());
        target1.getHostNodeList().setHostList(Collections.singletonList(new ApplicationHostInfoDTO()));
        target1.getHostNodeList().getHostList().get(0).setIp("1.1.1.1");

        FILE_INFO_1.setId(1L);
        FILE_INFO_1.setStepId(1000L);
        FILE_INFO_1.setFileType(TaskFileTypeEnum.SERVER);
        FILE_INFO_1.setFileLocation(Collections.singletonList("/tmp/files/file_one"));
        FILE_INFO_1.setFileSize(null);
        FILE_INFO_1.setFileHash(null);
        FILE_INFO_1.setHost(target1);
        FILE_INFO_1.setHostAccount(1L);

        FILE_INFO_2.setId(2L);
        FILE_INFO_2.setStepId(1000L);
        FILE_INFO_2.setFileType(TaskFileTypeEnum.LOCAL);
        FILE_INFO_2.setFileLocation(Collections.singletonList("/data/storage/upload/file_two"));
        FILE_INFO_2.setFileSize(1000L);
        FILE_INFO_2.setFileHash("bb13fc8fc2a72856189b0f4eb7882248a0d5e05b53ded0fb2a34d41d81dbe85b");
        FILE_INFO_2.setHost(null);
        FILE_INFO_2.setHostAccount(null);

        TaskTargetDTO target2 = new TaskTargetDTO();
        target2.setHostNodeList(new TaskHostNodeDTO());
        target2.getHostNodeList().setHostList(Collections.singletonList(new ApplicationHostInfoDTO()));
        target2.getHostNodeList().getHostList().get(0).setIp("2.2.2.2");
        FILE_INFO_3.setId(3L);
        FILE_INFO_3.setStepId(2000L);
        FILE_INFO_3.setFileType(TaskFileTypeEnum.LOCAL);
        FILE_INFO_3.setFileLocation(Collections.singletonList("/home/userC/file/file_three"));
        FILE_INFO_3.setFileSize(1000L);
        FILE_INFO_3.setFileHash(null);
        FILE_INFO_3.setHost(target2);
        FILE_INFO_3.setHostAccount(2L);
    }

    @Test
    void givenNormalStepIdReturnFileInfoList() {
        assertThat(taskFileInfoDAO.listFileInfoByStepId(FILE_INFO_1.getStepId())).isEqualTo(FILE_INFO_LIST);
    }

    @Test
    void givenNotExistStepIdReturnEmptyList() {
        assertThat(taskFileInfoDAO.listFileInfoByStepId(9999999L)).isEmpty();
    }

    @Test
    void givenNormalStepIdReturnFileInfo() {
        assertThat(taskFileInfoDAO.getFileInfoById(FILE_INFO_1.getStepId(), FILE_INFO_1.getId()))
            .isEqualTo(FILE_INFO_1);
        assertThat(taskFileInfoDAO.getFileInfoById(FILE_INFO_2.getStepId(), FILE_INFO_2.getId()))
            .isEqualTo(FILE_INFO_2);
        assertThat(taskFileInfoDAO.getFileInfoById(FILE_INFO_3.getStepId(), FILE_INFO_3.getId()))
            .isEqualTo(FILE_INFO_3);
    }

    @Test
    void givenNotExistStepIdReturnNull() {
        assertThat(taskFileInfoDAO.getFileInfoById(FILE_INFO_1.getStepId(), 9999999L)).isNull();
        assertThat(taskFileInfoDAO.getFileInfoById(9999999L, FILE_INFO_1.getId())).isNull();
        assertThat(taskFileInfoDAO.getFileInfoById(9999999L, 9999999L)).isNull();
    }

    private long getRandomPositiveLong() {
        long value = random.nextLong();
        if (value == Long.MIN_VALUE || value == 1L) {
            return 1;
        } else if (value < 0) {
            return -value;
        }
        return value;
    }

    @Test
    void givenFileInfoReturnNewFileInfoId() {
        TaskFileInfoDTO fileInfo = new TaskFileInfoDTO();

        fileInfo.setStepId(getRandomPositiveLong());
        fileInfo.setFileType(TaskFileTypeEnum.SERVER);
        fileInfo.setFileLocation(
            Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        fileInfo.setFileSize(getRandomPositiveLong());
        fileInfo.setFileHash(UUID.randomUUID().toString());
        fileInfo.setHost(null);
        fileInfo.setHostAccount(getRandomPositiveLong());

        Long fileInfoId = taskFileInfoDAO.insertFileInfo(fileInfo);
        fileInfo.setId(fileInfoId);
        assertThat(taskFileInfoDAO.getFileInfoById(fileInfo.getStepId(), fileInfoId)).isEqualTo(fileInfo);
    }

    @Test
    void givenStepIdReturnDeleteSuccess() {
        assertThat(taskFileInfoDAO.deleteFileInfoById(FILE_INFO_1.getStepId(), FILE_INFO_1.getId())).isTrue();
        assertThat(taskFileInfoDAO.getFileInfoById(FILE_INFO_1.getStepId(), FILE_INFO_1.getId())).isNull();
        assertThat(taskFileInfoDAO.deleteFileInfoById(FILE_INFO_1.getStepId(), FILE_INFO_1.getId())).isFalse();
    }

    @Test
    void givenWrongStepIdReturnDeleteFailed() {
        assertThat(taskFileInfoDAO.deleteFileInfoById(FILE_INFO_1.getStepId(), 999999999999L)).isFalse();
        assertThat(taskFileInfoDAO.deleteFileInfoById(999999999999L, FILE_INFO_1.getId())).isFalse();
        assertThat(taskFileInfoDAO.deleteFileInfoById(999999999999L, 999999999999L)).isFalse();

        assertThat(taskFileInfoDAO.getFileInfoById(FILE_INFO_1.getStepId(), FILE_INFO_1.getId()))
            .isEqualTo(FILE_INFO_1);
    }

    @Test
    void givenNewFileInfoReturnUpdateSuccess() {
        assertThat(taskFileInfoDAO.updateFileInfoById(FILE_INFO_1)).isTrue();

        FILE_INFO_1.setFileType(TaskFileTypeEnum.LOCAL);
        FILE_INFO_1.setFileLocation(
            Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        FILE_INFO_1.setFileSize(getRandomPositiveLong());
        FILE_INFO_1.setFileHash(UUID.randomUUID().toString());
        FILE_INFO_1.setHost(null);
        FILE_INFO_1.setHostAccount(getRandomPositiveLong());

        assertThat(taskFileInfoDAO.updateFileInfoById(FILE_INFO_1)).isTrue();

        assertThat(taskFileInfoDAO.getFileInfoById(FILE_INFO_1.getStepId(), FILE_INFO_1.getId()))
            .isEqualTo(FILE_INFO_1);
    }

    @Test
    void givenWrongFileInfoReturnUpdateFailed() {
        FILE_INFO_1.setStepId(getRandomPositiveLong());
        assertThat(taskFileInfoDAO.updateFileInfoById(FILE_INFO_1)).isFalse();
    }

    @Test
    void batchInsertFileInfo() {
        System.out.println(taskFileInfoDAO.batchInsertFileInfo(FILE_INFO_LIST));
    }
}
