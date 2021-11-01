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

package com.tencent.bk.job.manage.manager.variable;

import com.tencent.bk.job.manage.common.consts.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.model.dto.task.TaskFileInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskFileStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskScriptStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StepVariableParserTest {
    @Test
    @DisplayName("ParseScriptStepRefVars")
    void parseScriptStepRefVars() {
        List<TaskStepDTO> steps = new ArrayList<>();
        TaskScriptStepDTO scriptStep = new TaskScriptStepDTO();
        scriptStep.setId(1L);
        scriptStep.setContent("#!/bin/bash\n# job_import {{var1}}\necho ${var2}");
        scriptStep.setScriptParam("${var3} ${var4}");
        TaskStepDTO step1 = new TaskStepDTO();
        step1.setId(scriptStep.getId());
        step1.setType(TaskStepTypeEnum.SCRIPT);
        step1.setScriptStepInfo(scriptStep);
        steps.add(step1);

        List<TaskVariableDTO> variables = new ArrayList<>();
        TaskVariableDTO var1 = new TaskVariableDTO();
        var1.setName("var1");
        variables.add(var1);
        TaskVariableDTO var2 = new TaskVariableDTO();
        var2.setName("var2");
        variables.add(var2);
        TaskVariableDTO var4 = new TaskVariableDTO();
        var4.setName("var4");
        variables.add(var4);
        TaskVariableDTO var5 = new TaskVariableDTO();
        var5.setName("var5");
        variables.add(var5);

        StepVariableParser.parseStepRefVars(steps, variables);
        assertThat(step1.getRefVariables()).extracting("name")
            .containsOnly("var1", "var2", "var4");
    }

    @Test
    @DisplayName("ParseFileStepRefVars")
    void parseFileStepRefVars() {
        List<TaskStepDTO> steps = new ArrayList<>();
        TaskFileStepDTO fileStep = new TaskFileStepDTO();
        fileStep.setId(1L);
        List<TaskFileInfoDTO> originFiles = new ArrayList<>();
        TaskFileInfoDTO file1 = new TaskFileInfoDTO();
        file1.setFileLocation(null);
        originFiles.add(file1);
        TaskFileInfoDTO file2 = new TaskFileInfoDTO();
        file2.setFileLocation(Arrays.asList("/tmp/${var1}.log", "/tmp/${var2}.log"));
        originFiles.add(file2);
        fileStep.setOriginFileList(originFiles);

        fileStep.setDestinationFileLocation("/tmp/${var3}/");
        TaskStepDTO step1 = new TaskStepDTO();
        step1.setId(fileStep.getId());
        step1.setType(TaskStepTypeEnum.FILE);
        step1.setFileStepInfo(fileStep);
        steps.add(step1);

        List<TaskVariableDTO> variables = new ArrayList<>();
        TaskVariableDTO var1 = new TaskVariableDTO();
        var1.setName("var1");
        variables.add(var1);
        TaskVariableDTO var2 = new TaskVariableDTO();
        var2.setName("var2");
        variables.add(var2);
        TaskVariableDTO var4 = new TaskVariableDTO();
        var4.setName("var3");
        variables.add(var4);
        TaskVariableDTO var5 = new TaskVariableDTO();
        var5.setName("var4");
        variables.add(var5);

        StepVariableParser.parseStepRefVars(steps, variables);
        assertThat(step1.getRefVariables()).extracting("name")
            .containsOnly("var1", "var2", "var3");
    }
}
