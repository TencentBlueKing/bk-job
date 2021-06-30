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

package com.tencent.bk.job.execute.engine.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ScriptVariableResolverTest {

    @Test
    void testResolvedVariablesFromScript() {
        String scriptContent = "abcd";
        List<String> variables = ScriptVariableResolver.resolvedVariablesFromScript(scriptContent);
        assertThat(variables).isEmpty();

        scriptContent = "#!/bin/bash\necho '123'\n";
        variables = ScriptVariableResolver.resolvedVariablesFromScript(scriptContent);
        assertThat(variables).isEmpty();

        scriptContent = "#!/bin/bash\necho '123'\n\n \n";
        variables = ScriptVariableResolver.resolvedVariablesFromScript(scriptContent);
        assertThat(variables).isEmpty();

        scriptContent = "# job_import {{LAST_SUCCESS_IP}} \n";
        variables = ScriptVariableResolver.resolvedVariablesFromScript(scriptContent);
        assertThat(variables).containsOnly("LAST_SUCCESS_IP");

        scriptContent = "#  job_import  {{LAST_SUCCESS_IP}} \n";
        variables = ScriptVariableResolver.resolvedVariablesFromScript(scriptContent);
        assertThat(variables).containsOnly("LAST_SUCCESS_IP");

        scriptContent = "#  job_import  {{ LAST_SUCCESS_IP }} \n";
        variables = ScriptVariableResolver.resolvedVariablesFromScript(scriptContent);
        assertThat(variables).isEmpty();

        scriptContent = "#  job_import  {{LAST-SUCCESS-IP}} \n";
        variables = ScriptVariableResolver.resolvedVariablesFromScript(scriptContent);
        assertThat(variables).isEmpty();

        scriptContent = "#  job_import X {{LAST-SUCCESS-IP}} \n";
        variables = ScriptVariableResolver.resolvedVariablesFromScript(scriptContent);
        assertThat(variables).isEmpty();

        scriptContent = "#  job_import X {{}} \n";
        variables = ScriptVariableResolver.resolvedVariablesFromScript(scriptContent);
        assertThat(variables).isEmpty();

        scriptContent = "#!/bin/bash\n" +
            "\n" +
            "anynowtime=\"date +'%Y-%m-%d %H:%M:%S'\"\n" +
            "NOW=\"echo [\\`$anynowtime\\`][PID:$$]\"\n" +
            "\n" +
            "##### 可在脚本开始运行时调用，打印当时的时间戳及PID。\n" +
            "function job_start\n" +
            "{\n" +
            "    echo \"`eval $NOW` job_start\"\n" +
            "}\n" +
            "\n" +
            "##### 可在脚本执行成功的逻辑分支处调用，打印当时的时间戳及PID。 \n" +
            "function job_success\n" +
            "{\n" +
            "    MSG=\"$*\"\n" +
            "    echo \"`eval $NOW` job_success:[$MSG]\"\n" +
            "    exit 0\n" +
            "}\n" +
            "\n" +
            "##### 可在脚本执行失败的逻辑分支处调用，打印当时的时间戳及PID。\n" +
            "function job_fail\n" +
            "{\n" +
            "    MSG=\"$*\"\n" +
            "    echo \"`eval $NOW` job_fail:[$MSG]\"\n" +
            "    exit 1\n" +
            "}\n" +
            "\n" +
            "job_start\n" +
            "\n" +
            "###### 可在此处开始编写您的脚本逻辑代码\n" +
            "###### 作业平台中执行脚本成功和失败的标准只取决于脚本最后一条执行语句的返回值\n" +
            "###### 如果返回值为0，则认为此脚本执行成功，如果非0，则认为脚本执行失败\n" +
            "#  job_import  {{LAST_SUCCESS_IP}} \n";
        variables = ScriptVariableResolver.resolvedVariablesFromScript(scriptContent);
        assertThat(variables).containsOnly("LAST_SUCCESS_IP");
    }
}
