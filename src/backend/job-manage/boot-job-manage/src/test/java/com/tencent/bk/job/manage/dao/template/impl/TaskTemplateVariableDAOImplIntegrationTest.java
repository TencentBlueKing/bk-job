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

import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.manage.dao.TaskVariableDAO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
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
@Sql(value = {"/template/init_template_variable_data.sql"})
@SqlConfig(encoding = "utf-8")
class TaskTemplateVariableDAOImplIntegrationTest {

    private static final Random random = new Random();
    private static final TaskVariableDTO VARIABLE_1 = new TaskVariableDTO();
    private static final TaskVariableDTO VARIABLE_2 = new TaskVariableDTO();
    private static final TaskVariableDTO VARIABLE_3 = new TaskVariableDTO();
    private static final TaskVariableDTO VARIABLE_4 = new TaskVariableDTO();
    private static final List<TaskVariableDTO> VARIABLE_LIST =
        Arrays.asList(VARIABLE_1, VARIABLE_2, VARIABLE_3, VARIABLE_4);

    @Autowired
    @Qualifier("TaskTemplateVariableDAOImpl")
    private TaskVariableDAO taskVariableDAO;

    @BeforeEach
    void initTest() {
        VARIABLE_1.setId(1L);
        VARIABLE_1.setTemplateId(10000L);
        VARIABLE_1.setName("测试1");
        VARIABLE_1.setType(TaskVariableTypeEnum.STRING);
        VARIABLE_1.setDefaultValue("test1");
        VARIABLE_1.setDescription("这是一个测试变量1");
        VARIABLE_1.setChangeable(true);
        VARIABLE_1.setRequired(true);

        VARIABLE_2.setId(2L);
        VARIABLE_2.setTemplateId(10000L);
        VARIABLE_2.setName("测试2");
        VARIABLE_2.setType(TaskVariableTypeEnum.NAMESPACE);
        VARIABLE_2.setDefaultValue("test2");
        VARIABLE_2.setDescription("这是一个测试变量2");
        VARIABLE_2.setChangeable(false);
        VARIABLE_2.setRequired(false);

        VARIABLE_3.setId(3L);
        VARIABLE_3.setTemplateId(10000L);
        VARIABLE_3.setName("测试3");
        VARIABLE_3.setType(TaskVariableTypeEnum.HOST_LIST);
        VARIABLE_3.setDefaultValue("test3");
        VARIABLE_3.setDescription("这是一个测试变量3");
        VARIABLE_3.setChangeable(true);
        VARIABLE_3.setRequired(false);

        VARIABLE_4.setId(4L);
        VARIABLE_4.setTemplateId(10000L);
        VARIABLE_4.setName("测试4");
        VARIABLE_4.setType(TaskVariableTypeEnum.CIPHER);
        VARIABLE_4.setDefaultValue("test4");
        VARIABLE_4.setDescription("这是一个测试变量4");
        VARIABLE_4.setChangeable(false);
        VARIABLE_4.setRequired(true);
    }

    @Test
    void givenNormalParentIdReturnVariableList() {
        List<TaskVariableDTO> result = taskVariableDAO.listVariablesByParentId(10000L);
        assertThat(result).isEqualTo(VARIABLE_LIST);
    }

    @Test
    void givenNotExistParentIdReturnEmptyList() {
        assertThat(taskVariableDAO.listVariablesByParentId(9999999L)).isEqualTo(Collections.emptyList());
    }

    @Test
    void givenNormalVariableIdAndParentIdReturnVariableInfo() {
        assertThat(taskVariableDAO.getVariableById(VARIABLE_1.getTemplateId(), VARIABLE_1.getId()))
            .isEqualTo(VARIABLE_1);
        assertThat(taskVariableDAO.getVariableById(VARIABLE_2.getTemplateId(), VARIABLE_2.getId()))
            .isEqualTo(VARIABLE_2);
        assertThat(taskVariableDAO.getVariableById(VARIABLE_3.getTemplateId(), VARIABLE_3.getId()))
            .isEqualTo(VARIABLE_3);
        assertThat(taskVariableDAO.getVariableById(VARIABLE_4.getTemplateId(), VARIABLE_4.getId()))
            .isEqualTo(VARIABLE_4);
    }

    @Test
    void givenNotExistVariableIdOrNotExistParentIdReturnNull() {
        assertThat(taskVariableDAO.getVariableById(VARIABLE_1.getTemplateId(), 9999999L)).isNull();
        assertThat(taskVariableDAO.getVariableById(9999999L, VARIABLE_1.getId())).isNull();
        assertThat(taskVariableDAO.getVariableById(9999999L, 9999999L)).isNull();
    }

    @Test
    void givenVariableInfoReturnNewVariableId() {
        TaskVariableDTO variable = new TaskVariableDTO();
        variable.setTemplateId(getRandomPositiveLong());
        variable.setName(UUID.randomUUID().toString());
        variable.setType(TaskVariableTypeEnum.STRING);
        variable.setDefaultValue(UUID.randomUUID().toString());
        variable.setDescription(UUID.randomUUID().toString());
        variable.setChangeable(true);
        variable.setRequired(false);
        System.out.println(variable);
        Long variableId = taskVariableDAO.insertVariable(variable);
        variable.setId(variableId);
        assertThat(taskVariableDAO.getVariableById(variable.getTemplateId(), variableId)).isEqualTo(variable);
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
    void givenVariableInfoListReturnNewVariableIdList() {
        TaskVariableDTO variable1 = new TaskVariableDTO();
        variable1.setTemplateId(getRandomPositiveLong());
        variable1.setName(UUID.randomUUID().toString());
        variable1.setType(TaskVariableTypeEnum.STRING);
        variable1.setDefaultValue(UUID.randomUUID().toString());
        variable1.setDescription(UUID.randomUUID().toString());
        variable1.setChangeable(true);
        variable1.setRequired(false);
        TaskVariableDTO variable2 = new TaskVariableDTO();
        variable2.setTemplateId(getRandomPositiveLong());
        variable2.setName(UUID.randomUUID().toString());
        variable2.setType(TaskVariableTypeEnum.STRING);
        variable2.setDefaultValue(UUID.randomUUID().toString());
        variable2.setDescription(UUID.randomUUID().toString());
        variable2.setChangeable(true);
        variable2.setRequired(false);
        TaskVariableDTO variable3 = new TaskVariableDTO();
        variable3.setTemplateId(getRandomPositiveLong());
        variable3.setName(UUID.randomUUID().toString());
        variable3.setType(TaskVariableTypeEnum.STRING);
        variable3.setDefaultValue(UUID.randomUUID().toString());
        variable3.setDescription(UUID.randomUUID().toString());
        variable3.setChangeable(true);
        variable3.setRequired(false);
        List<Long> variableIdList =
            taskVariableDAO.batchInsertVariables(Arrays.asList(variable1, variable2, variable3));
        System.out.println(variableIdList);
        variable1.setId(variableIdList.get(0));
        variable2.setId(variableIdList.get(1));
        variable3.setId(variableIdList.get(2));
        assertThat(taskVariableDAO.getVariableById(variable1.getTemplateId(), variable1.getId())).isEqualTo(variable1);
        assertThat(taskVariableDAO.getVariableById(variable2.getTemplateId(), variable2.getId())).isEqualTo(variable2);
        assertThat(taskVariableDAO.getVariableById(variable3.getTemplateId(), variable3.getId())).isEqualTo(variable3);
    }

    @Test
    void givenVariableIdAndTemplateIdReturnDeleteSuccess() {
        assertThat(taskVariableDAO.deleteVariableById(VARIABLE_1.getTemplateId(), VARIABLE_1.getId())).isTrue();
        assertThat(taskVariableDAO.getVariableById(VARIABLE_1.getTemplateId(), VARIABLE_1.getId())).isNull();
        assertThat(taskVariableDAO.deleteVariableById(VARIABLE_1.getTemplateId(), VARIABLE_1.getId())).isFalse();
    }

    @Test
    void givenWrongVariableIdOrTemplateIdReturnDeleteFailed() {
        assertThat(taskVariableDAO.deleteVariableById(999999999999L, VARIABLE_1.getId())).isFalse();
        assertThat(taskVariableDAO.deleteVariableById(VARIABLE_1.getTemplateId(), 999999999999L)).isFalse();
        assertThat(taskVariableDAO.deleteVariableById(999999999999L, 999999999999L)).isFalse();

        assertThat(taskVariableDAO.getVariableById(VARIABLE_1.getTemplateId(), VARIABLE_1.getId()))
            .isEqualTo(VARIABLE_1);
    }

    @Test
    void givenNewVariableInfoReturnUpdateSuccess() {
        assertThat(taskVariableDAO.updateVariableById(VARIABLE_1)).isTrue();
        VARIABLE_1.setName(UUID.randomUUID().toString());
        VARIABLE_1.setType(TaskVariableTypeEnum.CIPHER);
        VARIABLE_1.setDescription(UUID.randomUUID().toString());
        VARIABLE_1.setDefaultValue(UUID.randomUUID().toString());
        VARIABLE_1.setChangeable(!VARIABLE_1.getChangeable());
        VARIABLE_1.setRequired(!VARIABLE_1.getRequired());
        assertThat(taskVariableDAO.updateVariableById(VARIABLE_1)).isTrue();

        assertThat(taskVariableDAO.getVariableById(VARIABLE_1.getTemplateId(), VARIABLE_1.getId()))
            .isNotEqualTo(VARIABLE_1);
        VARIABLE_1.setType(TaskVariableTypeEnum.STRING);
        assertThat(taskVariableDAO.getVariableById(VARIABLE_1.getTemplateId(), VARIABLE_1.getId()))
            .isEqualTo(VARIABLE_1);
    }

    @Test
    void givenWrongVariableInfoReturnUpdateFailed() {
        VARIABLE_1.setId(getRandomPositiveLong());
        VARIABLE_1.setTemplateId(getRandomPositiveLong());
        assertThat(taskVariableDAO.updateVariableById(VARIABLE_1)).isFalse();

        VARIABLE_2.setTemplateId(getRandomPositiveLong());
        assertThat(taskVariableDAO.updateVariableById(VARIABLE_2)).isFalse();

        VARIABLE_3.setId(getRandomPositiveLong());
        assertThat(taskVariableDAO.updateVariableById(VARIABLE_3)).isFalse();
    }

}
