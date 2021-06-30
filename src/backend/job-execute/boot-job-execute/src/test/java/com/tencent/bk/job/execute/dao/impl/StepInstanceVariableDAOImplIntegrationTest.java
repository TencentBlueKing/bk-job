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

import com.tencent.bk.job.execute.constants.VariableValueTypeEnum;
import com.tencent.bk.job.execute.dao.StepInstanceVariableDAO;
import com.tencent.bk.job.execute.model.HostVariableValuesDTO;
import com.tencent.bk.job.execute.model.StepInstanceVariableValuesDTO;
import com.tencent.bk.job.execute.model.VariableValueDTO;
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
@Sql({"/init_step_instance_variable_data.sql"})
class StepInstanceVariableDAOImplIntegrationTest {
    @Autowired
    private StepInstanceVariableDAO stepInstanceVariableDAO;

    @Test
    void testSaveVariableValues() {
        StepInstanceVariableValuesDTO variableValues = new StepInstanceVariableValuesDTO();
        variableValues.setTaskInstanceId(100L);
        variableValues.setStepInstanceId(200L);
        variableValues.setExecuteCount(1);
        variableValues.setType(VariableValueTypeEnum.OUTPUT.getValue());
        List<VariableValueDTO> globalParams = new ArrayList<>();
        VariableValueDTO value1 = new VariableValueDTO("param11", 1, "value11");
        VariableValueDTO value2 = new VariableValueDTO("param12", 1, "value12");
        globalParams.add(value1);
        globalParams.add(value2);
        variableValues.setGlobalParams(globalParams);
        List<HostVariableValuesDTO> hostParamValuesList = new ArrayList<>();
        HostVariableValuesDTO hostParamValues = new HostVariableValuesDTO();
        hostParamValues.setIp("1.1.1.1");
        List<VariableValueDTO> namespaceParamValues = new ArrayList<>();
        namespaceParamValues.add(new VariableValueDTO("param11", 2, "value11"));
        namespaceParamValues.add(new VariableValueDTO("param12", 2, "value12"));
        hostParamValues.setValues(namespaceParamValues);
        hostParamValuesList.add(hostParamValues);
        variableValues.setNamespaceParams(hostParamValuesList);

        stepInstanceVariableDAO.saveVariableValues(variableValues);

        StepInstanceVariableValuesDTO actual = stepInstanceVariableDAO
            .getStepVariableValues(200L, 1, VariableValueTypeEnum.OUTPUT);

        assertThat(actual.getStepInstanceId()).isEqualTo(200L);
        assertThat(actual.getExecuteCount()).isEqualTo(1);
        assertThat(actual.getType()).isEqualTo(VariableValueTypeEnum.OUTPUT.getValue());

        assertThat(actual.getGlobalParams()).hasSize(2);
        assertThat(actual.getGlobalParams()).extracting("name").containsOnly("param11", "param12");
        assertThat(actual.getGlobalParams()).extracting("value").containsOnly("value11", "value12");

        assertThat(actual.getNamespaceParams()).hasSize(1);
        assertThat(actual.getNamespaceParams().get(0).getIp()).isEqualTo("1.1.1.1");
        assertThat(actual.getNamespaceParams().get(0).getValues()).hasSize(2);
        assertThat(actual.getNamespaceParams().get(0).getValues()).extracting("name")
            .containsOnly("param11", "param12");
        assertThat(actual.getNamespaceParams().get(0).getValues()).extracting("value")
            .containsOnly("value11", "value12");
    }

    @Test
    void testListSortedPreStepOutputVariableValues() {
        List<StepInstanceVariableValuesDTO> stepInstanceVariableValuesList =
            stepInstanceVariableDAO.listSortedPreStepOutputVariableValues(1L, 3L);
        assertThat(stepInstanceVariableValuesList).hasSize(3);
        assertThat(stepInstanceVariableValuesList.get(0).getTaskInstanceId()).isEqualTo(1L);
        assertThat(stepInstanceVariableValuesList.get(0).getStepInstanceId()).isEqualTo(1L);
        assertThat(stepInstanceVariableValuesList.get(0).getExecuteCount()).isEqualTo(0);
        assertThat(stepInstanceVariableValuesList.get(1).getTaskInstanceId()).isEqualTo(1L);
        assertThat(stepInstanceVariableValuesList.get(1).getStepInstanceId()).isEqualTo(2L);
        assertThat(stepInstanceVariableValuesList.get(1).getExecuteCount()).isEqualTo(0);
        assertThat(stepInstanceVariableValuesList.get(2).getTaskInstanceId()).isEqualTo(1L);
        assertThat(stepInstanceVariableValuesList.get(2).getStepInstanceId()).isEqualTo(2L);
        assertThat(stepInstanceVariableValuesList.get(2).getExecuteCount()).isEqualTo(1);
    }
}
