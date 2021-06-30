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

import com.tencent.bk.job.execute.constants.UserOperationEnum;
import com.tencent.bk.job.execute.dao.OperationLogDAO;
import com.tencent.bk.job.execute.model.OperationLogDTO;
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
@Sql({"/init_operation_log_data.sql"})
public class OperationLogDAOImplIntegrationTest {
    @Autowired
    private OperationLogDAO operationLogDAO;

    @Test
    public void testListOperationLog() {
        List<OperationLogDTO> opLogs = operationLogDAO.listOperationLog(1L);
        assertThat(opLogs).hasSize(2);

        OperationLogDTO opLog = opLogs.get(0);
        assertThat(opLog.getId()).isEqualTo(2L);
        assertThat(opLog.getTaskInstanceId()).isEqualTo(1L);
        assertThat(opLog.getOperationEnum()).isEqualTo(UserOperationEnum.IGNORE_ERROR);
        assertThat(opLog.getOperator()).isEqualTo("admin");
        assertThat(opLog.getDetail()).isNotNull();
        assertThat(opLog.getDetail().getStepName()).isEqualTo("deploy2");
        assertThat(opLog.getCreateTime()).isEqualTo(1573441872000L);
    }

    @Test
    public void testSaveOperationLog() {
        OperationLogDTO savedOpLog = new OperationLogDTO();
        savedOpLog.setTaskInstanceId(2L);
        savedOpLog.setOperationEnum(UserOperationEnum.RETRY_STEP_FAIL);
        long createTime = 1573441872000L;
        savedOpLog.setCreateTime(createTime);
        savedOpLog.setOperator("admin");
        OperationLogDTO.OperationDetail detail = new OperationLogDTO.OperationDetail();
        detail.setStepName("xx");
        detail.setStepInstanceId(2L);
        savedOpLog.setDetail(detail);

        operationLogDAO.saveOperationLog(savedOpLog);

        List<OperationLogDTO> opLogs = operationLogDAO.listOperationLog(2L);
        assertThat(opLogs).hasSize(1);

        OperationLogDTO opLog = opLogs.get(0);
        assertThat(opLog.getTaskInstanceId()).isEqualTo(2L);
        assertThat(opLog.getOperator()).isEqualTo("admin");
        assertThat(opLog.getDetail()).isNotNull();
        assertThat(opLog.getDetail().getStepInstanceId()).isEqualTo(2L);
        assertThat(opLog.getDetail().getStepName()).isEqualTo("xx");
        assertThat(opLog.getCreateTime()).isEqualTo(createTime);
        assertThat(opLog.getOperationEnum()).isEqualTo(UserOperationEnum.RETRY_STEP_FAIL);
    }
}
