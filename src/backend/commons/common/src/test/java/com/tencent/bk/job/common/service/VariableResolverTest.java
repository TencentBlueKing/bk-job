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

package com.tencent.bk.job.common.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class VariableResolverTest {

    @Test
    void testResolvedJobImportVariables() {
        String scriptContent = "abcd";
        List<String> variables = VariableResolver.resolveJobImportVariables(scriptContent);
        assertThat(variables).isEmpty();

        scriptContent = "#!/bin/bash\necho '123'\n";
        variables = VariableResolver.resolveJobImportVariables(scriptContent);
        assertThat(variables).isEmpty();

        scriptContent = "#!/bin/bash\necho '123'\n\n \n";
        variables = VariableResolver.resolveJobImportVariables(scriptContent);
        assertThat(variables).isEmpty();

        scriptContent = "# job_import {{LAST_SUCCESS_IP}} \n";
        variables = VariableResolver.resolveJobImportVariables(scriptContent);
        assertThat(variables).containsOnly("LAST_SUCCESS_IP");

        scriptContent = "#  job_import  {{LAST_SUCCESS_IP}} \n";
        variables = VariableResolver.resolveJobImportVariables(scriptContent);
        assertThat(variables).containsOnly("LAST_SUCCESS_IP");

        scriptContent = "#  job_import  {{ LAST_SUCCESS_IP }} \n";
        variables = VariableResolver.resolveJobImportVariables(scriptContent);
        assertThat(variables).isEmpty();

        scriptContent = "#  job_import  {{LAST-SUCCESS-IP}} \n";
        variables = VariableResolver.resolveJobImportVariables(scriptContent);
        assertThat(variables).isEmpty();

        scriptContent = "#  job_import X {{LAST-SUCCESS-IP}} \n";
        variables = VariableResolver.resolveJobImportVariables(scriptContent);
        assertThat(variables).isEmpty();

        scriptContent = "#  job_import X {{}} \n";
        variables = VariableResolver.resolveJobImportVariables(scriptContent);
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
        variables = VariableResolver.resolveJobImportVariables(scriptContent);
        assertThat(variables).containsOnly("LAST_SUCCESS_IP");
    }

    @Test
    void testResolvedScriptBuildInVariables() {
        String content = "abc{{username}}def";
        Set<String> variables = VariableResolver.resolveScriptBuildInVariables(content);
        assertThat(variables).containsOnly("username");

        content = "abc{{username}}def\n#d{{biz_id}}\n";
        variables = VariableResolver.resolveScriptBuildInVariables(content);
        assertThat(variables).containsOnly("username", "biz_id");

        content = "{{username}}{{biz_id}}";
        variables = VariableResolver.resolveScriptBuildInVariables(content);
        assertThat(variables).containsOnly("username", "biz_id");
    }

    @Nested
    @DisplayName("TestParseShellScriptVar")
    class TestParseShellScriptVar {
        @Test
        @DisplayName("Test parse variable from shell script - Basic")
        void testParseShellScriptVarBasic() {
            String scriptContent = "#!/bin/bash\necho \"${var1}\"";
            List<String> varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("var1");

            scriptContent = "#!/bin/bash\necho \"${var}\"";
            varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("var");

            scriptContent = "#!/bin/bash\necho \"${_var}\"";
            varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("_var");

            scriptContent = "#!/bin/bash\necho \"${Var}\"";
            varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("Var");

            scriptContent = "#!/bin/bash\necho \"${var1}\" & echo \"${_var2}\"";
            varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("var1", "_var2");

            scriptContent = "#!/bin/bash\necho \"$var1\"";
            varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("var1");

            scriptContent = "#!/bin/bash\necho \"${var1}\" & echo \"$_var2\"";
            varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("var1", "_var2");
        }

        @Test
        @DisplayName("Test parse variable from shell script - Ignore comment line")
        void testParseShellScriptVarIgnoreCommentLine() {
            String scriptContent = "#!/bin/bash\necho \"${var1}\"\n# ${var2}";
            List<String> varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("var1");
        }

        @Test
        @DisplayName("Test parse variable from shell script - Expression")
        void testParseShellScriptVarForStringExpression() {
            String scriptContent = "#!/bin/bash\necho ${var-DEFAULT}";
            List<String> varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("var");

            scriptContent = "#!/bin/bash\necho ${var:-DEFAULT}";
            varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("var");

            scriptContent = "#!/bin/bash\necho ${var=DEFAULT}";
            varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("var");

            scriptContent = "#!/bin/bash\necho ${var=DEFAULT}";
            varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("var");

            scriptContent = "#!/bin/bash\necho ${var+OTHER}";
            varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("var");

            scriptContent = "#!/bin/bash\necho ${var:+OTHER}";
            varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("var");

            scriptContent = "#!/bin/bash\necho ${var?ERR_MSG}";
            varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("var");

            scriptContent = "#!/bin/bash\necho ${var:?ERR_MSG}";
            varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("var");
        }

        @Test
        @DisplayName("Test parse variable from shell script - String manipulation")
        void testParseShellScriptVarForStringManipulation() {
            String scriptContent = "#!/bin/bash\necho ${#var}";
            List<String> varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("var");

            scriptContent = "#!/bin/bash\necho ${var:1}";
            varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("var");

            scriptContent = "#!/bin/bash\necho ${var:1:2}";
            varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("var");

            scriptContent = "#!/bin/bash\necho ${var#substring}";
            varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("var");

            scriptContent = "#!/bin/bash\necho ${var##substring}";
            varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("var");

            scriptContent = "#!/bin/bash\necho ${var%substring}";
            varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("var");

            scriptContent = "#!/bin/bash\necho ${var%%substring}";
            varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("var");

            scriptContent = "#!/bin/bash\necho ${var/substring/replacement}";
            varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("var");

            scriptContent = "#!/bin/bash\necho ${var/#substring/replacement}";
            varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("var");

            scriptContent = "#!/bin/bash\necho ${var/%substring/replacement}";
            varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("var");
        }

        @Test
        @DisplayName("Test parse variable from shell script - Array variable")
        void testParseShellScriptVarForArrayVar() {
            String scriptContent = "#!/bin/bash\necho ${array_name[0]}";
            List<String> varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("array_name");

            scriptContent = "#!/bin/bash\necho ${array_name['test']}";
            varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("array_name");

            scriptContent = "#!/bin/bash\necho ${array_name[*]}";
            varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("array_name");

            scriptContent = "#!/bin/bash\necho ${array_name[@]}";
            varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("array_name");

            scriptContent = "#!/bin/bash\necho ${#array_name[@]}";
            varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("array_name");

            scriptContent = "#!/bin/bash\necho ${#array_name[*]}";
            varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("array_name");
        }

        @Test
        @DisplayName("Test parse variable from shell script - Multi line and vars")
        void testParseShellScriptVarForMultiLineAndVars() {
            String scriptContent = "#!/bin/bash\necho ${my_array[0]}\n echo ${var1} \n job_fail ${var2} ${_var3} \n";
            List<String> varNames = VariableResolver.resolveShellScriptVar(scriptContent);
            assertThat(varNames).containsOnly("my_array", "var1", "var2", "_var3");
        }
    }


    @Test
    @DisplayName("Test parse variable using job variable standard format")
    void testParseJobStandardVar() {
        String content = "/data/${log_dir}/${log_module}/job.log";
        List<String> varNames = VariableResolver.resolveJobStandardVar(content);
        assertThat(varNames).containsOnly("log_dir", "log_module");

        content = "/data/${log_dir}/job_${log_module}/job.log";
        varNames = VariableResolver.resolveJobStandardVar(content);
        assertThat(varNames).containsOnly("log_dir", "log_module");
    }
}
