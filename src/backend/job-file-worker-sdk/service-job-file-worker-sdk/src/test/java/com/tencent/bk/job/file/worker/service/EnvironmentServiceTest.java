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

package com.tencent.bk.job.file.worker.service;

import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.file.worker.config.WorkerConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("EnvironmentService 获取 Worker 当前 IP 测试")
class EnvironmentServiceTest {

    private static EnvironmentService service(boolean inK8s,
                                              String accessHost,
                                              Map<String, String> env,
                                              List<String> machineIps) {
        WorkerConfig workerConfig = new WorkerConfig();
        workerConfig.setAccessHost(accessHost);
        EnvironmentService service = new EnvironmentService(workerConfig) {
            @Override
            String getEnv(String name) {
                return env.get(name);
            }

            @Override
            List<String> listMachineIps() {
                return machineIps;
            }
        };
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles())
            .thenReturn(inK8s ? new String[]{JobConstants.PROFILE_KUBERNETES} : new String[0]);
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getEnvironment()).thenReturn(environment);
        service.setApplicationContext(applicationContext);
        return service;
    }

    private static Map<String, String> podIpEnv(String podIp) {
        Map<String, String> env = new HashMap<>();
        env.put("BK_JOB_POD_IP", podIp);
        return env;
    }

    @Test
    @DisplayName("K8s 环境优先使用 BK_JOB_POD_IP，不混入本机其他地址")
    void useK8sPodIp() {
        EnvironmentService service = service(true, null, podIpEnv(" 127.0.0.1 "),
            Collections.singletonList("127.0.0.2"));

        assertThat(service.getExpectedIps()).containsExactly("127.0.0.1");
    }

    @Test
    @DisplayName("K8s 环境 BK_JOB_POD_IP 缺失或非法时回退为本机网卡地址")
    void fallbackToMachineIpsWhenPodIpAbsentInK8s() {
        List<String> machineIps = Arrays.asList("127.0.0.2", "::1");

        assertThat(service(true, null, new HashMap<>(), machineIps).getExpectedIps())
            .containsExactly("127.0.0.2", "::1");
        assertThat(service(true, null, podIpEnv("not-an-ip"), machineIps).getExpectedIps())
            .containsExactly("127.0.0.2", "::1");
    }

    @Test
    @DisplayName("二进制部署使用本机网卡地址，accessHost 为 IP 字面量时一并上报")
    void useMachineIpsAndLiteralAccessHostOutsideK8s() {
        EnvironmentService service = service(false, "127.0.0.3", podIpEnv("127.0.0.1"),
            Arrays.asList("127.0.0.2", "127.0.0.3"));

        assertThat(service.getExpectedIps()).containsExactly("127.0.0.2", "127.0.0.3");
    }

    @Test
    @DisplayName("二进制部署 accessHost 为域名时不加入上报 IP")
    void ignoreDomainAccessHostOutsideK8s() {
        EnvironmentService service = service(false, "worker.example.com", new HashMap<>(),
            Collections.singletonList("127.0.0.2"));

        assertThat(service.getExpectedIps()).containsExactly("127.0.0.2");
    }

    @Test
    @DisplayName("获取不到任何 IP 时返回 null，Gateway 退化为仅校验可解析")
    void returnNullWhenNoIp() {
        assertThat(service(true, null, new HashMap<>(), Collections.emptyList()).getExpectedIps()).isNull();
        assertThat(service(false, "worker.example.com", new HashMap<>(), Collections.emptyList())
            .getExpectedIps()).isNull();
    }
}
