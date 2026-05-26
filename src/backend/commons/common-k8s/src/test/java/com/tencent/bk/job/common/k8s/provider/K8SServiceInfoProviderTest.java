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

package com.tencent.bk.job.common.k8s.provider;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tencent.bk.job.common.discovery.model.ServiceInstanceInfoDTO;
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodStatus;
import io.kubernetes.client.openapi.models.V1Service;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.kubernetes.commons.discovery.DefaultKubernetesServiceInstance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class K8SServiceInfoProviderTest {

    private static final String NAMESPACE = "bk-job";

    private CoreV1Api coreV1Api;
    private Logger providerLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        coreV1Api = mock(CoreV1Api.class);
        providerLogger = (Logger) LoggerFactory.getLogger(K8SServiceInfoProvider.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        providerLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        if (providerLogger != null && logAppender != null) {
            providerLogger.detachAppender(logAppender);
            logAppender.stop();
        }
    }

    @Test
    void shouldBuildServiceInfoFromPodMetadata() throws Exception {
        ServiceInstance instance = createKubernetesServiceInstance(
            "pod-uid-1", "job-manage", "127.0.0.1", 19801, NAMESPACE
        );
        DiscoveryClient discoveryClient = createDiscoveryClient(Collections.singletonList(instance));
        V1Pod pod = buildPod("pod-uid-1", "job-manage-0", "v3.9.0", "Running", null);
        mockListNamespacedPod(NAMESPACE, Collections.singletonList(pod));

        K8SServiceInfoProvider provider = new K8SServiceInfoProvider(discoveryClient, coreV1Api);
        List<ServiceInstanceInfoDTO> result = provider.listServiceInfo();

        assertEquals(1, result.size());
        ServiceInstanceInfoDTO detail = result.get(0);
        assertEquals("job-manage", detail.getServiceName());
        assertEquals("job-manage-0", detail.getName());
        assertEquals("v3.9.0", detail.getVersion());
        assertEquals(ServiceInstanceInfoDTO.STATUS_OK, detail.getStatusCode());
        assertNull(detail.getStatusMessage());
        assertEquals("127.0.0.1", detail.getIp());
        assertEquals(19801, detail.getPort());
    }

    @Test
    void shouldMapPodPhaseAndReasonCorrectly() throws Exception {
        ServiceInstance running = createKubernetesServiceInstance(
            "uid-running", "job-execute", "127.0.0.1", 19803, NAMESPACE
        );
        ServiceInstance failed = createKubernetesServiceInstance(
            "uid-failed", "job-crontab", "127.0.0.2", 19809, NAMESPACE
        );
        ServiceInstance phaseBlank = createKubernetesServiceInstance(
            "uid-blank", "job-logsvr", "127.0.0.3", 19808, NAMESPACE
        );
        DiscoveryClient discoveryClient = createDiscoveryClient(Arrays.asList(running, failed, phaseBlank));
        List<V1Pod> pods = Arrays.asList(
            buildPod("uid-running", "job-execute-0", "v3.9.0", "Running", null),
            buildPod("uid-failed", "job-crontab-0", "v3.9.0", "Failed", "CrashLoopBackOff"),
            buildPod("uid-blank", "job-logsvr-0", null, null, null)
        );
        mockListNamespacedPod(NAMESPACE, pods);

        K8SServiceInfoProvider provider = new K8SServiceInfoProvider(discoveryClient, coreV1Api);
        Map<String, ServiceInstanceInfoDTO> resultMap = provider.listServiceInfo().stream()
            .collect(Collectors.toMap(ServiceInstanceInfoDTO::getServiceName, dto -> dto));

        assertEquals(ServiceInstanceInfoDTO.STATUS_OK, resultMap.get("job-execute").getStatusCode());
        assertNull(resultMap.get("job-execute").getStatusMessage());

        assertEquals(ServiceInstanceInfoDTO.STATUS_ERROR, resultMap.get("job-crontab").getStatusCode());
        assertEquals("CrashLoopBackOff", resultMap.get("job-crontab").getStatusMessage());

        assertEquals(ServiceInstanceInfoDTO.STATUS_UNKNOWN, resultMap.get("job-logsvr").getStatusCode());
        assertEquals(K8SServiceInfoProvider.VERSION_UNKNOWN, resultMap.get("job-logsvr").getVersion());
    }

    @Test
    void shouldFallbackWhenPodUidNotFound() throws Exception {
        ServiceInstance instance = createKubernetesServiceInstance(
            "missing-uid", "job-manage", "127.0.0.1", 19801, NAMESPACE
        );
        DiscoveryClient discoveryClient = createDiscoveryClient(Collections.singletonList(instance));
        // Pod uid 与服务实例 uid 不匹配，触发 fallback
        V1Pod otherPod = buildPod("some-other-uid", "job-manage-0", "v3.9.0", "Running", null);
        mockListNamespacedPod(NAMESPACE, Collections.singletonList(otherPod));

        K8SServiceInfoProvider provider = new K8SServiceInfoProvider(discoveryClient, coreV1Api);
        List<ServiceInstanceInfoDTO> result = provider.listServiceInfo();

        assertEquals(1, result.size());
        ServiceInstanceInfoDTO detail = result.get(0);
        assertEquals("missing-uid", detail.getName());
        assertEquals(K8SServiceInfoProvider.VERSION_UNKNOWN, detail.getVersion());
        assertEquals(ServiceInstanceInfoDTO.STATUS_UNKNOWN, detail.getStatusCode());
    }

    @Test
    void shouldFallbackWhenListNamespacedPodThrows() throws Exception {
        ServiceInstance instance = createKubernetesServiceInstance(
            "pod-uid-1", "job-manage", "127.0.0.1", 19801, NAMESPACE
        );
        DiscoveryClient discoveryClient = createDiscoveryClient(Collections.singletonList(instance));
        when(coreV1Api.listNamespacedPod(eq(NAMESPACE), any(), any(), any(), any(), any(), any(), any(),
            any(), any(), any(), any())).thenThrow(new ApiException(500, "boom"));

        K8SServiceInfoProvider provider = new K8SServiceInfoProvider(discoveryClient, coreV1Api);
        List<ServiceInstanceInfoDTO> result = provider.listServiceInfo();

        assertEquals(1, result.size());
        ServiceInstanceInfoDTO detail = result.get(0);
        assertEquals("pod-uid-1", detail.getName());
        assertEquals(K8SServiceInfoProvider.VERSION_UNKNOWN, detail.getVersion());
        assertEquals(ServiceInstanceInfoDTO.STATUS_UNKNOWN, detail.getStatusCode());
    }

    @Test
    void shouldOnlyCallListNamespacedPodOncePerCycle() throws Exception {
        ServiceInstance a = createKubernetesServiceInstance("uid-a", "job-manage", "127.0.0.1", 19801, NAMESPACE);
        ServiceInstance b = createKubernetesServiceInstance("uid-b", "job-execute", "127.0.0.2", 19803, NAMESPACE);
        ServiceInstance c = createKubernetesServiceInstance("uid-c", "job-crontab", "127.0.0.3", 19809, NAMESPACE);
        DiscoveryClient discoveryClient = createDiscoveryClient(Arrays.asList(a, b, c));
        List<V1Pod> pods = Arrays.asList(
            buildPod("uid-a", "job-manage-0", "v3.9.0", "Running", null),
            buildPod("uid-b", "job-execute-0", "v3.9.0", "Running", null),
            buildPod("uid-c", "job-crontab-0", "v3.9.0", "Running", null)
        );
        mockListNamespacedPod(NAMESPACE, pods);

        K8SServiceInfoProvider provider = new K8SServiceInfoProvider(discoveryClient, coreV1Api);
        provider.listServiceInfo();

        verify(coreV1Api, times(1)).listNamespacedPod(eq(NAMESPACE), any(), any(), any(), any(), any(),
            any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldHitCacheWithinTtl() throws Exception {
        ServiceInstance instance = createKubernetesServiceInstance(
            "uid-a", "job-manage", "127.0.0.1", 19801, NAMESPACE
        );
        DiscoveryClient discoveryClient = createDiscoveryClient(Collections.singletonList(instance));
        mockListNamespacedPod(NAMESPACE, Collections.singletonList(
            buildPod("uid-a", "job-manage-0", "v3.9.0", "Running", null)
        ));

        K8SServiceInfoProvider provider = new K8SServiceInfoProvider(discoveryClient, coreV1Api, 60_000L);
        provider.listServiceInfo();
        provider.listServiceInfo();
        provider.listServiceInfo();

        verify(coreV1Api, times(1)).listNamespacedPod(eq(NAMESPACE), any(), any(), any(), any(), any(),
            any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldRefreshAfterTtlExpired() throws Exception {
        ServiceInstance instance = createKubernetesServiceInstance(
            "uid-a", "job-manage", "127.0.0.1", 19801, NAMESPACE
        );
        DiscoveryClient discoveryClient = createDiscoveryClient(Collections.singletonList(instance));
        mockListNamespacedPod(NAMESPACE, Collections.singletonList(
            buildPod("uid-a", "job-manage-0", "v3.9.0", "Running", null)
        ));

        // 0ms TTL：每次调用都视为已过期，强制刷新
        K8SServiceInfoProvider provider = new K8SServiceInfoProvider(discoveryClient, coreV1Api, 0L);
        provider.listServiceInfo();
        provider.listServiceInfo();

        verify(coreV1Api, times(2)).listNamespacedPod(eq(NAMESPACE), any(), any(), any(), any(), any(),
            any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldFilterNonJobOrMissingNamespaceInstances() throws Exception {
        ServiceInstance noNamespace = createKubernetesServiceInstance(
            "uid-no-ns", "job-manage", "127.0.0.1", 19801, null
        );
        ServiceInstance gatewayManagement = createKubernetesServiceInstance(
            "uid-gw", "job-gateway-management", "127.0.0.2", 19810, NAMESPACE
        );
        DiscoveryClient discoveryClient = createDiscoveryClient(Arrays.asList(noNamespace, gatewayManagement));

        K8SServiceInfoProvider provider = new K8SServiceInfoProvider(discoveryClient, coreV1Api);
        List<ServiceInstanceInfoDTO> result = provider.listServiceInfo();

        assertEquals(0, result.size());
        verify(coreV1Api, times(0)).listNamespacedPod(any(), any(), any(), any(), any(), any(),
            any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldPassDefaultLabelSelectorWhenInstantiatedByDefaultConstructor() throws Exception {
        ServiceInstance instance = createKubernetesServiceInstance(
            "uid-a", "job-manage", "127.0.0.1", 19801, NAMESPACE
        );
        DiscoveryClient discoveryClient = createDiscoveryClient(Collections.singletonList(instance));
        mockListNamespacedPod(NAMESPACE, Collections.singletonList(
            buildPod("uid-a", "job-manage-0", "v3.9.0", "Running", null)
        ));

        K8SServiceInfoProvider provider = new K8SServiceInfoProvider(discoveryClient, coreV1Api);
        provider.listServiceInfo();

        // 显式断言：listNamespacedPod 的第 6 形参（labelSelector）等于默认值 app.kubernetes.io/name=bk-job
        // 其它形参保持 null，避免任何形参被错误带值
        verify(coreV1Api).listNamespacedPod(
            eq(NAMESPACE), isNull(), isNull(), isNull(),
            isNull(), eq(K8SServiceInfoProvider.DEFAULT_POD_LABEL_SELECTOR), isNull(), isNull(),
            isNull(), isNull(), isNull(), isNull()
        );
        assertEquals("app.kubernetes.io/name=bk-job", K8SServiceInfoProvider.DEFAULT_POD_LABEL_SELECTOR);
    }

    @Test
    void shouldPassCustomLabelSelectorWhenInjectedViaConstructor() throws Exception {
        ServiceInstance instance = createKubernetesServiceInstance(
            "uid-a", "job-manage", "127.0.0.1", 19801, NAMESPACE
        );
        DiscoveryClient discoveryClient = createDiscoveryClient(Collections.singletonList(instance));
        mockListNamespacedPod(NAMESPACE, Collections.singletonList(
            buildPod("uid-a", "job-manage-0", "v3.9.0", "Running", null)
        ));

        String customSelector = "app.kubernetes.io/name=custom-job,app.kubernetes.io/instance=ut-instance";
        K8SServiceInfoProvider provider = new K8SServiceInfoProvider(
            discoveryClient, coreV1Api, K8SServiceInfoProvider.DEFAULT_POD_CACHE_TTL_MS, customSelector
        );
        provider.listServiceInfo();

        // 通过 ArgumentCaptor 取出第 6 形参，断言自定义 selector 被原样透传，未被默认值覆盖
        ArgumentCaptor<String> labelSelectorCaptor = ArgumentCaptor.forClass(String.class);
        verify(coreV1Api).listNamespacedPod(
            eq(NAMESPACE), isNull(), isNull(), isNull(),
            isNull(), labelSelectorCaptor.capture(), isNull(), isNull(),
            isNull(), isNull(), isNull(), isNull()
        );
        assertEquals(customSelector, labelSelectorCaptor.getValue());
    }

    @Test
    void shouldFilterServicesByAppKubernetesIoNameLabelOnMainPath() throws Exception {
        // 主路径命中：3 个 Service，2 个带 app.kubernetes.io/name=bk-job 标签（job-manage、job-execute），
        // 1 个标签为其它值（monitor-foo），断言只对前两个调用 getInstances，最终聚合只来自前两个
        ServiceInstance manageInstance = createKubernetesServiceInstance(
            "uid-manage", "job-manage", "127.0.0.1", 19801, NAMESPACE
        );
        ServiceInstance executeInstance = createKubernetesServiceInstance(
            "uid-execute", "job-execute", "127.0.0.2", 19803, NAMESPACE
        );
        // 故意准备一个 monitor-foo 实例：即使被错误命中也能通过断言暴露问题
        ServiceInstance monitorInstance = createKubernetesServiceInstance(
            "uid-monitor", "monitor-foo", "127.0.0.3", 9090, NAMESPACE
        );

        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        when(discoveryClient.getInstances("job-manage"))
            .thenReturn(Collections.singletonList(manageInstance));
        when(discoveryClient.getInstances("job-execute"))
            .thenReturn(Collections.singletonList(executeInstance));
        when(discoveryClient.getInstances("monitor-foo"))
            .thenReturn(Collections.singletonList(monitorInstance));

        Lister<V1Service> servicesLister = mockServicesLister(Arrays.asList(
            buildBkJobService("job-manage"),
            buildBkJobService("job-execute"),
            buildServiceWithLabelValue("monitor-foo", "monitoring-app")
        ));
        mockListNamespacedPod(NAMESPACE, Arrays.asList(
            buildPod("uid-manage", "job-manage-0", "v3.9.0", "Running", null),
            buildPod("uid-execute", "job-execute-0", "v3.9.0", "Running", null)
        ));

        K8SServiceInfoProvider provider = newProviderWithLister(discoveryClient, servicesLister);
        Map<String, ServiceInstanceInfoDTO> resultMap = provider.listServiceInfo().stream()
            .collect(Collectors.toMap(ServiceInstanceInfoDTO::getServiceName, dto -> dto));

        // 主路径只对带 bk-job 标签的 service 调用 getInstances
        verify(discoveryClient, times(1)).getInstances("job-manage");
        verify(discoveryClient, times(1)).getInstances("job-execute");
        verify(discoveryClient, never()).getInstances("monitor-foo");
        // 主路径不会再走兜底 getServices()
        verify(discoveryClient, never()).getServices();

        assertEquals(2, resultMap.size());
        assertTrue(resultMap.containsKey("job-manage"));
        assertTrue(resultMap.containsKey("job-execute"));
        assertFalse(resultMap.containsKey("monitor-foo"));
        assertEquals(ServiceInstanceInfoDTO.STATUS_OK, resultMap.get("job-manage").getStatusCode());
        assertEquals(ServiceInstanceInfoDTO.STATUS_OK, resultMap.get("job-execute").getStatusCode());
    }

    @Test
    void shouldStillFilterGatewayManagementOnMainPath() throws Exception {
        // 主路径同时命中 job-gateway-management（同样带 app.kubernetes.io/name=bk-job 标签），
        // 断言它仍被过滤掉，不会出现在最终结果中
        ServiceInstance manageInstance = createKubernetesServiceInstance(
            "uid-manage", "job-manage", "127.0.0.1", 19801, NAMESPACE
        );
        ServiceInstance gatewayManagementInstance = createKubernetesServiceInstance(
            "uid-gw-mgmt", "job-gateway-management", "127.0.0.2", 19810, NAMESPACE
        );

        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        when(discoveryClient.getInstances("job-manage"))
            .thenReturn(Collections.singletonList(manageInstance));
        when(discoveryClient.getInstances("job-gateway-management"))
            .thenReturn(Collections.singletonList(gatewayManagementInstance));

        Lister<V1Service> servicesLister = mockServicesLister(Arrays.asList(
            buildBkJobService("job-manage"),
            buildBkJobService("job-gateway-management")
        ));
        mockListNamespacedPod(NAMESPACE, Collections.singletonList(
            buildPod("uid-manage", "job-manage-0", "v3.9.0", "Running", null)
        ));

        K8SServiceInfoProvider provider = newProviderWithLister(discoveryClient, servicesLister);
        List<ServiceInstanceInfoDTO> result = provider.listServiceInfo();

        // 主路径在 listJobServiceIdsByLabel 阶段已剔除 job-gateway-management，不会触发 getInstances
        verify(discoveryClient, never()).getInstances("job-gateway-management");
        verify(discoveryClient, times(1)).getInstances("job-manage");

        assertEquals(1, result.size());
        assertEquals("job-manage", result.get(0).getServiceName());
    }

    @Test
    void shouldFallbackToNameKeywordWhenServicesListerThrows() throws Exception {
        // 兜底路径：servicesLister.list() 抛异常，断言走旧的 getServices() + contains("job-") 逻辑
        ServiceInstance manageInstance = createKubernetesServiceInstance(
            "uid-manage", "job-manage", "127.0.0.1", 19801, NAMESPACE
        );
        ServiceInstance otherInstance = createKubernetesServiceInstance(
            "uid-monitor", "monitor-foo", "127.0.0.3", 9090, NAMESPACE
        );

        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        when(discoveryClient.getServices()).thenReturn(Arrays.asList("job-manage", "monitor-foo"));
        when(discoveryClient.getInstances("job-manage"))
            .thenReturn(Collections.singletonList(manageInstance));
        when(discoveryClient.getInstances("monitor-foo"))
            .thenReturn(Collections.singletonList(otherInstance));

        @SuppressWarnings("unchecked")
        Lister<V1Service> servicesLister = (Lister<V1Service>) mock(Lister.class);
        when(servicesLister.list()).thenThrow(new RuntimeException("informer not synced"));

        mockListNamespacedPod(NAMESPACE, Collections.singletonList(
            buildPod("uid-manage", "job-manage-0", "v3.9.0", "Running", null)
        ));

        K8SServiceInfoProvider provider = newProviderWithLister(discoveryClient, servicesLister);
        List<ServiceInstanceInfoDTO> result = provider.listServiceInfo();

        // 兜底确实触发了 getServices()
        verify(discoveryClient, times(1)).getServices();
        // 兜底路径只对名称含 "job-" 的 service 调用 getInstances，monitor-foo 不命中
        verify(discoveryClient, times(1)).getInstances("job-manage");
        verify(discoveryClient, never()).getInstances("monitor-foo");

        assertEquals(1, result.size());
        assertEquals("job-manage", result.get(0).getServiceName());
        assertTrue(containsWarnLog("fallback to discoveryClient.getServices()"),
            "expected WARN log when servicesLister.list() throws");
    }

    @Test
    void shouldFallbackToNameKeywordWhenServicesListerReturnsEmpty() throws Exception {
        // 兜底路径：servicesLister.list() 返回空集合（informer 未 sync 完成），断言走兜底逻辑
        ServiceInstance manageInstance = createKubernetesServiceInstance(
            "uid-manage", "job-manage", "127.0.0.1", 19801, NAMESPACE
        );

        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        when(discoveryClient.getServices()).thenReturn(Collections.singletonList("job-manage"));
        when(discoveryClient.getInstances("job-manage"))
            .thenReturn(Collections.singletonList(manageInstance));

        Lister<V1Service> servicesLister = mockServicesLister(Collections.emptyList());

        mockListNamespacedPod(NAMESPACE, Collections.singletonList(
            buildPod("uid-manage", "job-manage-0", "v3.9.0", "Running", null)
        ));

        K8SServiceInfoProvider provider = newProviderWithLister(discoveryClient, servicesLister);
        List<ServiceInstanceInfoDTO> result = provider.listServiceInfo();

        verify(discoveryClient, times(1)).getServices();
        verify(discoveryClient, times(1)).getInstances("job-manage");

        assertEquals(1, result.size());
        assertEquals("job-manage", result.get(0).getServiceName());
        assertTrue(containsWarnLog("informer not synced"),
            "expected WARN log when servicesLister.list() returns empty");
    }

    @Test
    void shouldFallbackToNameKeywordWhenServicesListerNotInjected() throws Exception {
        // 兜底路径：servicesLister 为 null（默认 3 参构造器场景），断言走兜底逻辑且记 WARN
        ServiceInstance manageInstance = createKubernetesServiceInstance(
            "uid-manage", "job-manage", "127.0.0.1", 19801, NAMESPACE
        );
        DiscoveryClient discoveryClient = createDiscoveryClient(Collections.singletonList(manageInstance));
        mockListNamespacedPod(NAMESPACE, Collections.singletonList(
            buildPod("uid-manage", "job-manage-0", "v3.9.0", "Running", null)
        ));

        // 不传 servicesLister：使用默认 2 参构造器
        K8SServiceInfoProvider provider = new K8SServiceInfoProvider(discoveryClient, coreV1Api);
        List<ServiceInstanceInfoDTO> result = provider.listServiceInfo();

        assertEquals(1, result.size());
        assertEquals("job-manage", result.get(0).getServiceName());
        assertTrue(containsWarnLog("servicesLister is not injected"),
            "expected WARN log when servicesLister is null");
    }

    @Test
    void shouldNotFallbackWhenListerHasNonEmptyResultButNoBkJobLabel() throws Exception {
        // 主路径成功但过滤后无命中：lister 返回非空集合（仅含其它项目 service），主路径返回空列表也是合法结果，
        // 不应再走兜底，最终聚合结果为空
        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);

        Lister<V1Service> servicesLister = mockServicesLister(Collections.singletonList(
            buildServiceWithLabelValue("monitor-foo", "monitoring-app")
        ));

        K8SServiceInfoProvider provider = newProviderWithLister(discoveryClient, servicesLister);
        List<ServiceInstanceInfoDTO> result = provider.listServiceInfo();

        assertEquals(0, result.size());
        // 主路径 lister 可读，不应再触发兜底的 getServices()
        verify(discoveryClient, never()).getServices();
        verify(discoveryClient, never()).getInstances("monitor-foo");
    }

    private K8SServiceInfoProvider newProviderWithLister(DiscoveryClient discoveryClient,
                                                        Lister<V1Service> servicesLister) {
        return new K8SServiceInfoProvider(
            discoveryClient,
            coreV1Api,
            K8SServiceInfoProvider.DEFAULT_POD_CACHE_TTL_MS,
            K8SServiceInfoProvider.DEFAULT_POD_LABEL_SELECTOR,
            servicesLister
        );
    }

    @SuppressWarnings("unchecked")
    private Lister<V1Service> mockServicesLister(List<V1Service> services) {
        Lister<V1Service> lister = (Lister<V1Service>) mock(Lister.class);
        when(lister.list()).thenReturn(new ArrayList<>(services));
        return lister;
    }

    private V1Service buildBkJobService(String name) {
        return buildServiceWithLabelValue(
            name, K8SServiceInfoProvider.SERVICE_LABEL_APP_KUBERNETES_IO_NAME_VALUE_BK_JOB
        );
    }

    private V1Service buildServiceWithLabelValue(String name, String appKubernetesIoNameLabelValue) {
        V1ObjectMeta metadata = new V1ObjectMeta().name(name);
        if (appKubernetesIoNameLabelValue != null) {
            Map<String, String> labels = new HashMap<>();
            labels.put(
                K8SServiceInfoProvider.SERVICE_LABEL_APP_KUBERNETES_IO_NAME,
                appKubernetesIoNameLabelValue
            );
            metadata.setLabels(labels);
        }
        return new V1Service().metadata(metadata);
    }

    private boolean containsWarnLog(String fragment) {
        Set<Level> warnLevels = Collections.singleton(Level.WARN);
        return logAppender.list.stream()
            .filter(event -> warnLevels.contains(event.getLevel()))
            .anyMatch(event -> event.getFormattedMessage().contains(fragment));
    }

    private void mockListNamespacedPod(String namespace, List<V1Pod> pods) throws ApiException {
        V1PodList podList = new V1PodList().items(pods);
        when(coreV1Api.listNamespacedPod(eq(namespace), any(), any(), any(), any(), any(), any(), any(),
            any(), any(), any(), any())).thenReturn(podList);
    }

    private V1Pod buildPod(String uid, String name, String versionLabel, String phase, String reason) {
        V1ObjectMeta metadata = new V1ObjectMeta()
            .uid(uid)
            .name(name);
        if (versionLabel != null) {
            Map<String, String> labels = new HashMap<>();
            labels.put(K8SServiceInfoProvider.KEY_JOB_MS_VERSION, versionLabel);
            metadata.setLabels(labels);
        }
        V1PodStatus status = new V1PodStatus().phase(phase).reason(reason);
        return new V1Pod().metadata(metadata).status(status);
    }

    private ServiceInstance createKubernetesServiceInstance(String instanceId,
                                                            String serviceId,
                                                            String host,
                                                            int port,
                                                            String namespace) {
        return new DefaultKubernetesServiceInstance(
            instanceId, serviceId, host, port, new HashMap<>(), false, namespace, null
        );
    }

    private DiscoveryClient createDiscoveryClient(List<ServiceInstance> serviceInstances) {
        return new DiscoveryClient() {
            private final Map<String, List<ServiceInstance>> serviceInstanceMap = serviceInstances.stream()
                .collect(Collectors.groupingBy(ServiceInstance::getServiceId));

            @Override
            public String description() {
                return "fake-k8s-discovery-client";
            }

            @Override
            public List<ServiceInstance> getInstances(String serviceId) {
                return serviceInstanceMap.getOrDefault(serviceId, Collections.emptyList());
            }

            @Override
            public List<String> getServices() {
                return serviceInstanceMap.keySet().stream().sorted().collect(Collectors.toList());
            }

            @Override
            public int getOrder() {
                return 0;
            }
        };
    }

}
