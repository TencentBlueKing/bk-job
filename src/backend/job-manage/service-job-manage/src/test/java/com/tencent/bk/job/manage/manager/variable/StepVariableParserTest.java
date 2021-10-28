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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StepVariableParserTest {

    @Test
    void parse() {
    }

    @Test
    @DisplayName("Test parse variable from shell script - Basic")
    void testParseShellScriptVarBasic() {
        String scriptContent = "#!/bin/bash\necho \"${var1}\"";
        List<String> varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("var1");

        scriptContent = "#!/bin/bash\necho \"${var}\"";
        varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("var");

        scriptContent = "#!/bin/bash\necho \"${_var}\"";
        varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("_var");

        scriptContent = "#!/bin/bash\necho \"${Var}\"";
        varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("Var");

        scriptContent = "#!/bin/bash\necho \"${var1}\" & echo \"${_var2}\"";
        varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("var1", "_var2");
    }

    @Test
    @DisplayName("Test parse variable from shell script - Expression")
    void testParseShellScriptVarForStringExpression() {
        String scriptContent = "#!/bin/bash\necho ${var-DEFAULT}";
        List<String> varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("var");

        scriptContent = "#!/bin/bash\necho ${var:-DEFAULT}";
        varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("var");

        scriptContent = "#!/bin/bash\necho ${var=DEFAULT}";
        varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("var");

        scriptContent = "#!/bin/bash\necho ${var=DEFAULT}";
        varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("var");

        scriptContent = "#!/bin/bash\necho ${var+OTHER}";
        varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("var");

        scriptContent = "#!/bin/bash\necho ${var:+OTHER}";
        varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("var");

        scriptContent = "#!/bin/bash\necho ${var?ERR_MSG}";
        varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("var");

        scriptContent = "#!/bin/bash\necho ${var:?ERR_MSG}";
        varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("var");
    }

    @Test
    @DisplayName("Test parse variable from shell script - String manipulation")
    void testParseShellScriptVarForStringManipulation() {
        String scriptContent = "#!/bin/bash\necho ${#var}";
        List<String> varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("var");

        scriptContent = "#!/bin/bash\necho ${var:1}";
        varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("var");

        scriptContent = "#!/bin/bash\necho ${var:1:2}";
        varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("var");

        scriptContent = "#!/bin/bash\necho ${var#substring}";
        varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("var");

        scriptContent = "#!/bin/bash\necho ${var##substring}";
        varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("var");

        scriptContent = "#!/bin/bash\necho ${var%substring}";
        varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("var");

        scriptContent = "#!/bin/bash\necho ${var%%substring}";
        varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("var");

        scriptContent = "#!/bin/bash\necho ${var/substring/replacement}";
        varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("var");

        scriptContent = "#!/bin/bash\necho ${var/#substring/replacement}";
        varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("var");

        scriptContent = "#!/bin/bash\necho ${var/%substring/replacement}";
        varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("var");
    }

    @Test
    @DisplayName("Test parse variable from shell script - Array variable")
    void testParseShellScriptVarForArrayVar() {
        String scriptContent = "#!/bin/bash\necho ${array_name[0]}";
        List<String> varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("array_name");

        scriptContent = "#!/bin/bash\necho ${array_name['test']}";
        varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("array_name");

        scriptContent = "#!/bin/bash\necho ${array_name[*]}";
        varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("array_name");

        scriptContent = "#!/bin/bash\necho ${array_name[@]}";
        varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("array_name");

        scriptContent = "#!/bin/bash\necho ${#array_name[@]}";
        varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("array_name");

        scriptContent = "#!/bin/bash\necho ${#array_name[*]}";
        varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("array_name");
    }

    @Test
    @DisplayName("Test parse variable from shell script - Multi line and vars")
    void testParseShellScriptVarForMultiLineAndVars() {
        String scriptContent = "#!/bin/bash\necho ${my_array[0]}\n echo ${var1} \n job_fail ${var2} ${_var3} \n";
        List<String> varNames = StepVariableParser.parseShellScriptVar(scriptContent);
        assertThat(varNames).containsOnly("my_array", "var1", "var2", "_var3");
    }
}
