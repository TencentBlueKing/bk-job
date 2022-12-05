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

import java.util.Arrays;
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
        String[] args = new String[]{
            "-n", "ns1",
            "-s", " job-execute",
            "-d", "(job-execute:job-manage,job-logsvr)",
            "-lc", "bk.job.image/tag=3.6.0-latest",
            "-ls", "(job-manage:label1=value1,label2=value2),(job-execute:label3=value3)"
        };
        ServiceDependModel serviceDependModel = StartupController.parseDependModelFromArgsOrEnv(args);
        assertNotNull(serviceDependModel);
        assertEquals("ns1", serviceDependModel.getNamespace());
        assertEquals("job-execute", serviceDependModel.getServiceName());
        assertEquals("(job-execute:job-manage,job-logsvr)", serviceDependModel.getDependenciesStr());
        assertEquals("bk.job.image/tag=3.6.0-latest", serviceDependModel.getExpectLabelsCommon());
        assertEquals(
            "(job-manage:label1=value1,label2=value2),(job-execute:label3=value3)",
            serviceDependModel.getExpectLabelsService()
        );
        // 测试全写参数解析
        args = new String[]{
            "--namespace", "ns1",
            "--service", " job-execute",
            "--dependencies", "(job-execute:job-manage,job-logsvr)",
            "--expect-pod-labels-common", "bk.job.image/tag=3.6.0-latest",
            "--expect-pod-labels-service", "(job-manage:label1=value1,label2=value2),(job-execute:label3=value3)"
        };
        serviceDependModel = StartupController.parseDependModelFromArgsOrEnv(args);
        assertNotNull(serviceDependModel);
        assertEquals("ns1", serviceDependModel.getNamespace());
        assertEquals("job-execute", serviceDependModel.getServiceName());
        assertEquals("(job-execute:job-manage,job-logsvr)", serviceDependModel.getDependenciesStr());
        assertEquals("bk.job.image/tag=3.6.0-latest", serviceDependModel.getExpectLabelsCommon());
        assertEquals(
            "(job-manage:label1=value1,label2=value2),(job-execute:label3=value3)",
            serviceDependModel.getExpectLabelsService()
        );
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

    @Test
    void testParseExpectPodLabelsForService() {
        String expectPodLabelsCommon = "bk.job.image/tag=3.6.0-latest";
        String expectPodLabelsService = " (job-manage:label1=value1, label2=value2), (job-execute:label3=value3 ) ";
        List<String> dependServiceList = Arrays.asList("job-manage", "job-execute");
        Map<String, Map<String, String>> servicePodLabelsMap = StartupController.parseExpectPodLabelsForService(
            expectPodLabelsCommon,
            expectPodLabelsService,
            dependServiceList
        );
        assertTrue(servicePodLabelsMap.containsKey("job-manage"));
        assertTrue(servicePodLabelsMap.get("job-manage").containsKey("bk.job.image/tag"));
        assertTrue(servicePodLabelsMap.get("job-manage").containsKey("label1"));
        assertTrue(servicePodLabelsMap.get("job-manage").containsKey("label2"));
        assertEquals(3, servicePodLabelsMap.get("job-manage").size());
        assertEquals("3.6.0-latest", servicePodLabelsMap.get("job-manage").get("bk.job.image/tag"));
        assertEquals("value1", servicePodLabelsMap.get("job-manage").get("label1"));
        assertEquals("value2", servicePodLabelsMap.get("job-manage").get("label2"));
        assertTrue(servicePodLabelsMap.containsKey("job-execute"));
        assertTrue(servicePodLabelsMap.get("job-execute").containsKey("bk.job.image/tag"));
        assertTrue(servicePodLabelsMap.get("job-execute").containsKey("label3"));
        assertEquals(2, servicePodLabelsMap.get("job-execute").size());
        assertEquals("3.6.0-latest", servicePodLabelsMap.get("job-execute").get("bk.job.image/tag"));
        assertEquals("value3", servicePodLabelsMap.get("job-execute").get("label3"));
    }
}
