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

package com.tencent.bk.job.k8s;

import com.beust.jcommander.ParameterException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ServiceDependModelParserTest {

    @Test
    void testParseDependModelFromArgsOrEnv() {
        ServiceDependModelParser dependModelParser = new ServiceDependModelParser();
        // 测试异常参数解析
        assertThrows(ParameterException.class, () -> dependModelParser.parseDependModelFromArgsOrEnv(
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
        ServiceDependModel serviceDependModel = dependModelParser.parseDependModelFromArgsOrEnv(args);
        assertNotNull(serviceDependModel);
        assertEquals("ns1", serviceDependModel.getNamespace());
        assertEquals("job-execute", serviceDependModel.getServiceName());
        assertEquals("(job-execute:job-manage,job-logsvr)", serviceDependModel.getDependenciesStr());
        assertEquals("bk.job.image/tag=3.6.0-latest", serviceDependModel.getExpectLabelsCommon());
        assertEquals(
            "(job-manage:label1=value1,label2=value2),(job-execute:label3=value3)",
            serviceDependModel.getExpectLabelsService()
        );
        Assertions.assertFalse(serviceDependModel.getExternalDependencyCheckEnabled());
        Assertions.assertNull(serviceDependModel.getExternalDependencyCheckUrl());
        // 测试全写参数解析
        args = new String[]{
            "--namespace", "ns1",
            "--service", " job-execute",
            "--dependencies", "(job-execute:job-manage,job-logsvr)",
            "--expect-pod-labels-common", "bk.job.image/tag=3.6.0-latest",
            "--expect-pod-labels-service", "(job-manage:label1=value1,label2=value2),(job-execute:label3=value3)",
            "--external-dependency-check-enabled", "true",
            "--external-dependency-check-url", "https://example.com/checkServiceDependency"
        };
        serviceDependModel = dependModelParser.parseDependModelFromArgsOrEnv(args);
        assertNotNull(serviceDependModel);
        assertEquals("ns1", serviceDependModel.getNamespace());
        assertEquals("job-execute", serviceDependModel.getServiceName());
        assertEquals("(job-execute:job-manage,job-logsvr)", serviceDependModel.getDependenciesStr());
        assertEquals("bk.job.image/tag=3.6.0-latest", serviceDependModel.getExpectLabelsCommon());
        assertEquals(
            "(job-manage:label1=value1,label2=value2),(job-execute:label3=value3)",
            serviceDependModel.getExpectLabelsService()
        );
        Assertions.assertTrue(serviceDependModel.getExternalDependencyCheckEnabled());
        assertEquals(
            "https://example.com/checkServiceDependency",
            serviceDependModel.getExternalDependencyCheckUrl()
        );
        args = new String[]{
            "--external-dependency-check-enabled", "TRUE"
        };
        serviceDependModel = dependModelParser.parseDependModelFromArgsOrEnv(args);
        Assertions.assertTrue(serviceDependModel.getExternalDependencyCheckEnabled());
        args = new String[]{
            "--external-dependency-check-enabled", "false"
        };
        serviceDependModel = dependModelParser.parseDependModelFromArgsOrEnv(args);
        Assertions.assertFalse(serviceDependModel.getExternalDependencyCheckEnabled());
        args = new String[]{
            "--external-dependency-check-enabled", "FALSE"
        };
        serviceDependModel = dependModelParser.parseDependModelFromArgsOrEnv(args);
        Assertions.assertFalse(serviceDependModel.getExternalDependencyCheckEnabled());
    }
}
