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

package com.tencent.bk.job.k8s;

import com.beust.jcommander.ParameterException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StartupControllerTest {

    @Test
    void testParseDependModelFromArgsOrEnv() {
        // 测试异常参数解析
        assertThrows(ParameterException.class, () -> StartupController.parseDependModelFromArgsOrEnv(
            new String[]{"-nn", "ns1", "-ss", " job-execute"}
        ));
        // 测试简写参数解析
        String[] args = new String[]{"-n", "ns1", "-s", " job-execute", "-d", "(job-execute:job-manage,job-logsvr)"};
        ServiceDependModel serviceDependModel = StartupController.parseDependModelFromArgsOrEnv(args);
        assertNotNull(serviceDependModel);
        assertEquals("ns1", serviceDependModel.getNamespace());
        assertEquals("job-execute", serviceDependModel.getServiceName());
        assertEquals("(job-execute:job-manage,job-logsvr)", serviceDependModel.getDependenciesStr());
        // 测试全写参数解析
        args = new String[]{
            "--namespace", "ns1",
            "--service", " job-execute",
            "--dependencies", "(job-execute:job-manage,job-logsvr)"
        };
        serviceDependModel = StartupController.parseDependModelFromArgsOrEnv(args);
        assertNotNull(serviceDependModel);
        assertEquals("ns1", serviceDependModel.getNamespace());
        assertEquals("job-execute", serviceDependModel.getServiceName());
        assertEquals("(job-execute:job-manage,job-logsvr)", serviceDependModel.getDependenciesStr());
    }

    @Test
    void testParseDependencyMap() {
        // 测试结构化依赖关系映射表解析
        String dependenciesStr = "(job-execute:job-manage,job-logsvr)," +
            "(job-crontab:job-execute),(job-analysis:job-crontab),(job-file-worker:job-file-gateway)";
        Map<String, List<String>> dependencyMap = StartupController.parseDependencyMap(dependenciesStr);
        assertEquals(4, dependencyMap.size());
        assertTrue(dependencyMap.containsKey("job-execute"));
        assertTrue(dependencyMap.containsKey("job-crontab"));
        assertTrue(dependencyMap.containsKey("job-analysis"));
        assertTrue(dependencyMap.containsKey("job-file-worker"));
        assertEquals(2, dependencyMap.get("job-execute").size());
    }
}
