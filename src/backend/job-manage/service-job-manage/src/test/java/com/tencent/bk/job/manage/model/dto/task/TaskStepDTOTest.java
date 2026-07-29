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

package com.tencent.bk.job.manage.model.dto.task;

import com.tencent.bk.job.common.esb.model.job.v3.resp.EsbStepV3DTO;
import com.tencent.bk.job.manage.api.common.constants.task.TaskStepTypeEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskStepDTOTest {

    private TaskStepDTO buildScriptStep(Integer enable) {
        TaskStepDTO step = new TaskStepDTO();
        step.setId(1L);
        step.setName("step");
        step.setType(TaskStepTypeEnum.SCRIPT);
        step.setEnable(enable);
        return step;
    }

    @Test
    @DisplayName("步骤启用状态映射：enable=1 时 EsbStepV3DTO.enabled 为 1")
    void toEsbStepV3WhenEnabled() {
        EsbStepV3DTO esbStep = TaskStepDTO.toEsbStepV3(buildScriptStep(1));
        assertThat(esbStep.getEnabled()).isEqualTo(1);
    }

    @Test
    @DisplayName("步骤启用状态映射：enable=0 时 EsbStepV3DTO.enabled 为 0")
    void toEsbStepV3WhenDisabled() {
        EsbStepV3DTO esbStep = TaskStepDTO.toEsbStepV3(buildScriptStep(0));
        assertThat(esbStep.getEnabled()).isEqualTo(0);
    }

    @Test
    @DisplayName("步骤启用状态映射：enable 为 null 时 EsbStepV3DTO.enabled 为 null")
    void toEsbStepV3WhenEnableNull() {
        EsbStepV3DTO esbStep = TaskStepDTO.toEsbStepV3(buildScriptStep(null));
        assertThat(esbStep.getEnabled()).isNull();
    }
}
