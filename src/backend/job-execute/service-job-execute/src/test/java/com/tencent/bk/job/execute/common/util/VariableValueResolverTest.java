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

package com.tencent.bk.job.execute.common.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class VariableValueResolverTest {
    @Test
    void testResolve() {
        Map<String, String> vars = new HashMap<>();
        vars.put("var1", "var1");
        vars.put("var2", "var2");
        String param = "${var1} ${var2}";

        String resolvedParam = VariableValueResolver.resolve(param, vars);
        assertThat(resolvedParam).isEqualTo("var1 var2");
    }

    @Test
    void whenVarIsNotExistThenRemainOrigin() {
        Map<String, String> vars = new HashMap<>();
        vars.put("var1", "var1");
        String param = "${var1} ${var2}";

        String resolvedParam = VariableValueResolver.resolve(param, vars);
        assertThat(resolvedParam).isEqualTo("var1 ${var2}");
    }

    @Test
    void testResolveNoVarParamThenReturnOrigin() {
        Map<String, String> vars = new HashMap<>();
        vars.put("var1", "var1");
        vars.put("var2", "var2");
        String param = "/tmp/";

        String resolvedParam = VariableValueResolver.resolve(param, vars);
        assertThat(resolvedParam).isEqualTo("/tmp/");
    }

    @Test
    void whenVarValueNullThenResolvedAsEmptyStr() {
        Map<String, String> vars = new HashMap<>();
        vars.put("var1", "var1");
        vars.put("var2", null);
        String param = "--test=${var1} --name=${var2}";

        String resolvedParam = VariableValueResolver.resolve(param, vars);
        assertThat(resolvedParam).isEqualTo("--test=var1 --name=");
    }

    @Test
    void testResolveVarValueContainsVarPattern() {
        Map<String, String> vars = new HashMap<>();
        String key = "build_data";
        String value = "-t go -d '{\"repo_workspace\": \"/data/qk_build_workspace/\", \"repo_name\": " +
            "\"AccountServer\", \"repo_url\": \"AccountServer.git\", \"operator\": \"bayizhang\", \"tag\": \"\", " +
            "\"build_cmd\": \"export GOPATH=/root/go:${gopath}/src/AccountServer\\ncd ${gopath}/src\\n[[ -d " +
            "going_proj ]] || { git clone j.git;}\\ncd going_proj\\ngit pull\\nexport " +
            "GOPATH=${GOPATH}:${gopath}/src/going_proj/trunk\\ncd ${gopath}/src/AccountServer/src\\ngo build -o " +
            "${gopath}/src/go_build_target/AccountServer/bin/AccountServer\\nif [ \\\"$?\\\" -ne \\\"0\\\" " +
            "]\\nthen\\n      echo \\\"Compile Failed!\\\"\\n      exit 1\\nfi\\ncd " +
            "${gopath}/src/go_build_target/AccountServer\\ntar -zcf ../AccountServer.tar.gz bin\\nexport " +
            "GOPATH=/root/go\", \"svn_repo\": false}'";
        vars.put(key, value);
        assertThat(VariableValueResolver.resolve("${build_data}", vars)).isEqualTo(value);
        assertThat(VariableValueResolver.resolve("A${build_data}B", vars)).isEqualTo("A" + value + "B");
    }
}
