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

package com.tencent.bk.job.manage.dao.plan.impl;

import com.tencent.bk.job.common.model.dto.KubeClusterObjectDTO;
import com.tencent.bk.job.common.model.dto.KubeContainerFilter;
import com.tencent.bk.job.common.model.dto.KubeNamespaceObjectDTO;
import com.tencent.bk.job.common.model.dto.KubePropCondition;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskScriptSourceEnum;
import com.tencent.bk.job.manage.dao.TaskScriptStepDAO;
import com.tencent.bk.job.manage.model.dto.task.TaskScriptStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetDTO;
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
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 执行方案脚本步骤 DAO 的容器动态条件持久化集成测试。
 * <p>
 * 方案 DAO 与模板 DAO 共享 {@link TaskTargetDTO#fromJsonString} / {@code toJsonString} 路径，
 * 但 jOOQ bean / 表结构 / 注入资格符独立；本测试单独验证方案侧 {@code task_plan_step_script.destination_host_list}
 * 列对 containerFilters 的写入读取链路同样通畅。
 * 复用 {@code init_script_relate_task_plan_data.sql} 用于建表与基线 fixture，不引入新 SQL 文件。
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:test.properties")
@Sql(value = {"/init_script_relate_task_plan_data.sql"})
@SqlConfig(encoding = "utf-8")
class TaskPlanScriptStepDAOContainerFilterIntegrationTest {

    private static final Random RANDOM = new Random();

    @Autowired
    @Qualifier("TaskPlanScriptStepDAOImpl")
    private TaskScriptStepDAO planScriptStepDAO;

    @Test
    @DisplayName("destination_host_list 持久化 containerFilters + propConditions 在方案侧完整保真")
    void givenPlanScriptStepWithContainerFiltersReturnRoundTripPreserved() {
        TaskScriptStepDTO scriptStep = newPlanScriptStepWithRandomBasics();
        scriptStep.setExecuteTarget(buildTargetWithContainerFilters());

        Long newId = planScriptStepDAO.insertScriptStep(scriptStep);
        scriptStep.setId(newId);

        TaskScriptStepDTO reloaded = planScriptStepDAO.getScriptStepById(scriptStep.getStepId());
        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getExecuteTarget()).isNotNull();
        assertThat(reloaded.getExecuteTarget().getContainerFilters()).hasSize(1);

        KubeContainerFilter filter = reloaded.getExecuteTarget().getContainerFilters().get(0);
        assertThat(filter.getClusterNodes()).hasSize(1);
        assertThat(filter.getClusterNodes().get(0).getId()).isEqualTo(1001L);
        assertThat(filter.getNamespaceNodes()).hasSize(2);
        assertThat(filter.getNamespaceNodes().get(0).getId()).isEqualTo(1L);
        assertThat(filter.getNamespaceNodes().get(1).getId()).isEqualTo(2L);
        assertThat(filter.getPropConditions()).hasSize(2);
        assertThat(filter.getPropConditions().get(0).getField()).isEqualTo("container_container_uid");
        assertThat(filter.getPropConditions().get(0).getValue()).isEqualTo("docker://abcdefg");
        assertThat(filter.getPropConditions().get(1).getField()).isEqualTo("pod_name");
        assertThat(filter.getPropConditions().get(1).getValue()).isEqualTo("pod-a");
    }

    private TaskScriptStepDTO newPlanScriptStepWithRandomBasics() {
        TaskScriptStepDTO scriptStep = new TaskScriptStepDTO();
        scriptStep.setPlanId(randomPositiveLong());
        scriptStep.setStepId(randomPositiveLong());
        scriptStep.setScriptSource(TaskScriptSourceEnum.CITING);
        scriptStep.setScriptId(UUID.randomUUID().toString().replace("-", ""));
        scriptStep.setScriptVersionId(randomPositiveLong());
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
        filter.setClusterNodes(Collections.singletonList(new KubeClusterObjectDTO(1001L)));
        filter.setNamespaceNodes(Arrays.asList(
            new KubeNamespaceObjectDTO(1L),
            new KubeNamespaceObjectDTO(2L)
        ));
        filter.setPropConditions(Arrays.asList(
            new KubePropCondition("container_container_uid", "contains", "docker://abcdefg"),
            new KubePropCondition("pod_name", "equal", "pod-a")
        ));

        TaskTargetDTO target = new TaskTargetDTO();
        target.setContainerFilters(Collections.singletonList(filter));
        return target;
    }

    private static long randomPositiveLong() {
        long value = RANDOM.nextLong();
        if (value == Long.MIN_VALUE || value == 1L) {
            return 1;
        }
        return value < 0 ? -value : value;
    }
}
