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

package com.tencent.bk.job.manage.dao.template.impl;

import com.tencent.bk.job.common.model.dto.KubeClusterObjectDTO;
import com.tencent.bk.job.common.model.dto.KubeContainerFilter;
import com.tencent.bk.job.common.model.dto.KubeNamespaceObjectDTO;
import com.tencent.bk.job.common.model.dto.KubePropCondition;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskScriptSourceEnum;
import com.tencent.bk.job.manage.dao.TaskScriptStepDAO;
import com.tencent.bk.job.manage.model.dto.task.TaskScriptStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 6/10/2019 09:11
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:test.properties")
@Sql(value = {"/template/init_template_script_step_data.sql"})
@SqlConfig(encoding = "utf-8")
class TaskTemplateScriptStepDAOImplIntegrationTest {

    private static final Random random = new Random();
    private static final TaskScriptStepDTO SCRIPT_STEP_1 = new TaskScriptStepDTO();
    private static final TaskScriptStepDTO SCRIPT_STEP_2 = new TaskScriptStepDTO();
    private static final TaskScriptStepDTO SCRIPT_STEP_3 = new TaskScriptStepDTO();
    private static final List<TaskScriptStepDTO> SCRIPT_STEP_LIST =
        Arrays.asList(SCRIPT_STEP_1, SCRIPT_STEP_2, SCRIPT_STEP_3);

    @Autowired
    @Qualifier("TaskTemplateScriptStepDAOImpl")
    private TaskScriptStepDAO taskScriptStepDAO;

    @BeforeEach
    void initTest() {
        SCRIPT_STEP_1.setId(1L);
        SCRIPT_STEP_1.setTemplateId(100000L);
        SCRIPT_STEP_1.setStepId(1000L);
        SCRIPT_STEP_1.setScriptSource(TaskScriptSourceEnum.LOCAL);
        SCRIPT_STEP_1.setScriptId("1000");
        SCRIPT_STEP_1.setScriptVersionId(1000L);
        SCRIPT_STEP_1.setContent(null);
        SCRIPT_STEP_1.setLanguage(ScriptTypeEnum.SHELL);
        SCRIPT_STEP_1.setScriptParam("a=a");
        SCRIPT_STEP_1.setTimeout(600L);
        SCRIPT_STEP_1.setAccount(1L);
        SCRIPT_STEP_1.setSecureParam(false);
        SCRIPT_STEP_1.setStatus(1);
        SCRIPT_STEP_1.setIgnoreError(false);

        SCRIPT_STEP_2.setId(2L);
        SCRIPT_STEP_2.setTemplateId(100000L);
        SCRIPT_STEP_2.setStepId(2000L);
        SCRIPT_STEP_2.setScriptSource(TaskScriptSourceEnum.CITING);
        SCRIPT_STEP_2.setScriptId("2000");
        SCRIPT_STEP_2.setScriptVersionId(2000L);
        SCRIPT_STEP_2.setContent("this is a sample content");
        SCRIPT_STEP_2.setLanguage(ScriptTypeEnum.SHELL);
        SCRIPT_STEP_2.setScriptParam(null);
        SCRIPT_STEP_2.setTimeout(600L);
        SCRIPT_STEP_2.setAccount(2L);
        SCRIPT_STEP_2.setSecureParam(true);
        SCRIPT_STEP_2.setStatus(0);
        SCRIPT_STEP_2.setIgnoreError(true);

        SCRIPT_STEP_3.setId(3L);
        SCRIPT_STEP_3.setTemplateId(100000L);
        SCRIPT_STEP_3.setStepId(3000L);
        SCRIPT_STEP_3.setScriptSource(TaskScriptSourceEnum.LOCAL);
        SCRIPT_STEP_3.setScriptId("3000");
        SCRIPT_STEP_3.setScriptVersionId(3000L);
        SCRIPT_STEP_3.setContent(null);
        SCRIPT_STEP_3.setLanguage(ScriptTypeEnum.SHELL);
        SCRIPT_STEP_3.setScriptParam("c=c");
        SCRIPT_STEP_3.setTimeout(600L);
        SCRIPT_STEP_3.setAccount(3L);
        SCRIPT_STEP_3.setSecureParam(true);
        SCRIPT_STEP_3.setStatus(1);
        SCRIPT_STEP_3.setIgnoreError(false);
    }

    @Test
    void givenNormalTemplateIdReturnScriptStepInfoList() {
        List<TaskScriptStepDTO> result = taskScriptStepDAO.listScriptStepByParentId(100000L);
        assertThat(result).isEqualTo(SCRIPT_STEP_LIST);
    }

    @Test
    void givenNotExistTemplateIdReturnEmptyList() {
        assertThat(taskScriptStepDAO.listScriptStepByParentId(9999999999L)).isEmpty();
    }

    @Test
    void givenNormalStepIdReturnScriptStepInfo() {
        assertThat(taskScriptStepDAO.getScriptStepById(SCRIPT_STEP_1.getStepId())).isEqualTo(SCRIPT_STEP_1);
        assertThat(taskScriptStepDAO.getScriptStepById(SCRIPT_STEP_2.getStepId())).isEqualTo(SCRIPT_STEP_2);
        assertThat(taskScriptStepDAO.getScriptStepById(SCRIPT_STEP_3.getStepId())).isEqualTo(SCRIPT_STEP_3);
    }

    @Test
    void givenNotExistStepIdReturnNull() {
        assertThat(taskScriptStepDAO.getScriptStepById(9999999L)).isNull();
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
    void givenScriptStepInfoReturnNewScriptStepId() {
        TaskScriptStepDTO scriptStep = new TaskScriptStepDTO();

        scriptStep.setTemplateId(getRandomPositiveLong());
        scriptStep.setStepId(getRandomPositiveLong());
        scriptStep.setScriptSource(TaskScriptSourceEnum.CITING);
        scriptStep.setScriptId(UUID.randomUUID().toString().replace("-", ""));
        scriptStep.setScriptVersionId(getRandomPositiveLong());
        scriptStep.setContent(UUID.randomUUID().toString());
        scriptStep.setLanguage(ScriptTypeEnum.PERL);
        scriptStep.setScriptParam(UUID.randomUUID().toString());
        scriptStep.setTimeout(getRandomPositiveLong());
        scriptStep.setAccount(getRandomPositiveLong());
        scriptStep.setSecureParam(true);
        scriptStep.setStatus(0);
        scriptStep.setIgnoreError(true);

        System.out.println(scriptStep);
        Long approvalId = taskScriptStepDAO.insertScriptStep(scriptStep);
        scriptStep.setId(approvalId);
        assertThat(taskScriptStepDAO.getScriptStepById(scriptStep.getStepId())).isEqualTo(scriptStep);
    }

    @Test
    void givenStepIdReturnDeleteSuccess() {
        assertThat(taskScriptStepDAO.deleteScriptStepById(SCRIPT_STEP_1.getStepId())).isTrue();
        assertThat(taskScriptStepDAO.getScriptStepById(SCRIPT_STEP_1.getStepId())).isNull();
        assertThat(taskScriptStepDAO.deleteScriptStepById(SCRIPT_STEP_1.getStepId())).isFalse();
    }

    @Test
    void givenWrongStepIdReturnDeleteFailed() {
        assertThat(taskScriptStepDAO.deleteScriptStepById(999999999999L)).isFalse();

        assertThat(taskScriptStepDAO.getScriptStepById(SCRIPT_STEP_1.getStepId())).isEqualTo(SCRIPT_STEP_1);
    }

    @Test
    void givenNewScriptStepInfoReturnUpdateSuccess() {
        assertThat(taskScriptStepDAO.updateScriptStepById(SCRIPT_STEP_1)).isTrue();

        SCRIPT_STEP_1.setId(getRandomPositiveLong());
        SCRIPT_STEP_1.setScriptId(UUID.randomUUID().toString().replace("-", ""));
        SCRIPT_STEP_1.setScriptVersionId(getRandomPositiveLong());
        SCRIPT_STEP_1.setScriptSource(TaskScriptSourceEnum.CITING);
        SCRIPT_STEP_1.setContent(UUID.randomUUID().toString());
        SCRIPT_STEP_1.setLanguage(ScriptTypeEnum.PYTHON);
        SCRIPT_STEP_1.setScriptParam(UUID.randomUUID().toString());
        SCRIPT_STEP_1.setTimeout(getRandomPositiveLong());
        SCRIPT_STEP_1.setSecureParam(false);
        SCRIPT_STEP_1.setStatus(1);
        SCRIPT_STEP_1.setIgnoreError(true);

        assertThat(taskScriptStepDAO.updateScriptStepById(SCRIPT_STEP_1)).isTrue();

        assertThat(taskScriptStepDAO.getScriptStepById(SCRIPT_STEP_1.getStepId())).isNotEqualTo(SCRIPT_STEP_1);
        SCRIPT_STEP_1.setId(1L);
        assertThat(taskScriptStepDAO.getScriptStepById(SCRIPT_STEP_1.getStepId())).isEqualTo(SCRIPT_STEP_1);
    }

    @Test
    void givenWrongScriptStepInfoReturnUpdateFailed() {
        SCRIPT_STEP_1.setStepId(getRandomPositiveLong());
        assertThat(taskScriptStepDAO.updateScriptStepById(SCRIPT_STEP_1)).isFalse();
    }

    @Test
    void whenUpdateStepScriptVersionIdThenSuccess() {
        assertThat(taskScriptStepDAO.updateScriptStepRefScriptVersionId(100000L, 1000L, 9999L)).isEqualTo(true);
        TaskScriptStepDTO scriptStep = taskScriptStepDAO.getScriptStepById(1000L);
        assertThat(scriptStep).isNotNull();
        assertThat(scriptStep.getTemplateId()).isEqualTo(100000L);
        assertThat(scriptStep.getStepId()).isEqualTo(1000L);
        assertThat(scriptStep.getScriptVersionId()).isEqualTo(9999L);

    }

    /**
     * 容器动态条件「新字段端到端写入读取」回归：插入含 containerFilters + propConditions 的 executeTarget，
     * 经 JsonMapper 序列化到 destination_host_list LONGTEXT 列，再从 H2 读回并经
     * {@code TaskTargetDTO.fromJsonString} 反序列化，所有字段需保真。
     */
    @Test
    @DisplayName("destination_host_list 持久化 containerFilters + propConditions 完整保真")
    void givenScriptStepWithContainerFiltersReturnRoundTripPreserved() {
        TaskScriptStepDTO scriptStep = newScriptStepWithRandomBasics();
        scriptStep.setExecuteTarget(buildTargetWithContainerFilters());

        Long newId = taskScriptStepDAO.insertScriptStep(scriptStep);
        scriptStep.setId(newId);

        TaskScriptStepDTO reloaded = taskScriptStepDAO.getScriptStepById(scriptStep.getStepId());
        TaskTargetDTO target = reloaded.getExecuteTarget();
        assertThat(target).isNotNull();
        assertThat(target.getContainerFilters()).hasSize(1);

        KubeContainerFilter filter = target.getContainerFilters().get(0);
        assertThat(filter.getClusterNodes()).hasSize(1);
        assertThat(filter.getClusterNodes().get(0).getId()).isEqualTo(1000L);
        assertThat(filter.getClusterNodes().get(0).getId()).isEqualTo(1000L);
        assertThat(filter.getNamespaceNodes()).hasSize(1);
        assertThat(filter.getNamespaceNodes().get(0).getId()).isEqualTo(10000L);
        assertThat(filter.getPropConditions()).hasSize(2);
        assertThat(filter.getPropConditions().get(0).getField()).isEqualTo("container_container_uid");
        assertThat(filter.getPropConditions().get(0).getOperator()).isEqualTo("contains");
        assertThat(filter.getPropConditions().get(0).getValue()).isEqualTo("docker://abcdefg");
        assertThat(filter.getPropConditions().get(1).getField()).isEqualTo("pod_name");
        assertThat(filter.getPropConditions().get(1).getValue()).isEqualTo("pod-a");
    }

    /**
     * 容器动态条件「老数据兼容」回归：现有用例的 SCRIPT_STEP_* 的 executeTarget 一直为 null（destination_host_list = null），
     * 这里显式断言出库后该字段保持 null、TaskTargetDTO 反序列化为 null，老逻辑分支不受 containerFilters 字段引入影响。
     */
    @Test
    @DisplayName("destination_host_list = null 时反序列化为 null，老数据行为不变")
    void givenNullDestinationHostListReturnNullExecuteTarget() {
        TaskScriptStepDTO reloaded = taskScriptStepDAO.getScriptStepById(SCRIPT_STEP_1.getStepId());
        assertThat(reloaded.getExecuteTarget()).isNull();
    }

    private TaskScriptStepDTO newScriptStepWithRandomBasics() {
        TaskScriptStepDTO scriptStep = new TaskScriptStepDTO();
        scriptStep.setTemplateId(getRandomPositiveLong());
        scriptStep.setStepId(getRandomPositiveLong());
        scriptStep.setScriptSource(TaskScriptSourceEnum.CITING);
        scriptStep.setScriptId(UUID.randomUUID().toString().replace("-", ""));
        scriptStep.setScriptVersionId(getRandomPositiveLong());
        scriptStep.setContent(null);
        scriptStep.setLanguage(ScriptTypeEnum.SHELL);
        scriptStep.setScriptParam("a=a");
        scriptStep.setTimeout(600L);
        scriptStep.setAccount(1L);
        scriptStep.setSecureParam(false);
        scriptStep.setStatus(1);
        scriptStep.setIgnoreError(false);
        return scriptStep;
    }

    private TaskTargetDTO buildTargetWithContainerFilters() {
        KubeContainerFilter filter = new KubeContainerFilter();

        filter.setClusterNodes(Collections.singletonList(new KubeClusterObjectDTO(1000L)));
        filter.setNamespaceNodes(Collections.singletonList(new KubeNamespaceObjectDTO(10000L)));

        filter.setPropConditions(Arrays.asList(
            new KubePropCondition("container_container_uid", "contains", "docker://abcdefg"),
            new KubePropCondition("pod_name", "equal", "pod-a")
        ));

        TaskTargetDTO target = new TaskTargetDTO();
        target.setContainerFilters(Collections.singletonList(filter));
        return target;
    }
}
