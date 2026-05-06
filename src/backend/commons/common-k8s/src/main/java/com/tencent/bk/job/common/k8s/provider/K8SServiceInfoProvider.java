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

import com.tencent.bk.job.common.discovery.ServiceInfoProvider;
import com.tencent.bk.job.common.discovery.model.ServiceInstanceInfoDTO;
import com.tencent.bk.job.common.util.json.JsonUtils;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.kubernetes.commons.discovery.KubernetesServiceInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 基于 K8s API 的服务实例信息提供器
 * <p>
 * 高频触发（默认每 5s 一次）下避免直连 apiserver 风暴：
 * 1. 复用 spring-cloud-kubernetes 提供的单例 CoreV1Api（背后是单例 ApiClient/OkHttpClient）；
 * 2. 每次 listServiceInfo 在每个命名空间内仅触发一次 listNamespacedPod；
 * 3. 命名空间维度的进程内 TTL 缓存（默认 5 秒），TTL 内重复触发命中缓存。
 */
@Slf4j
public class K8SServiceInfoProvider implements ServiceInfoProvider {

    public static final String KEY_HELM_NAMESPACE = "meta.helm.sh/release-namespace";
    public static final String KEY_JOB_MS_VERSION = "bk.job.image/tag";
    public static final String VERSION_UNKNOWN = "-";
    public static final String PHASE_RUNNING = "Running";

    /**
     * Pod 缓存默认 TTL，单位毫秒。
     * 当前 listServiceInfo 由健康检查接口高频触发（约 5s/次），TTL 略小于触发间隔，
     * 既能合并同一轮内的多次 namespace 查询，也避免数据陈旧。
     */
    public static final long DEFAULT_POD_CACHE_TTL_MS = 5_000L;

    private final DiscoveryClient discoveryClient;
    private final CoreV1Api coreV1Api;
    private final long podCacheTtlMs;
    /**
     * namespace -> 缓存条目；条目内 podMap 的 key 为 pod uid。
     * 使用 ConcurrentHashMap 保证多线程并发触发时的可见性，
     * 单个命名空间的缓存失效后并发竞争允许多次 list（最多 1 次/线程），
     * 不引入额外锁以避免阻塞 apiserver 响应慢时的健康检查请求。
     */
    private final ConcurrentHashMap<String, PodCacheEntry> podCacheByNamespace = new ConcurrentHashMap<>();

    public K8SServiceInfoProvider(DiscoveryClient discoveryClient, CoreV1Api coreV1Api) {
        this(discoveryClient, coreV1Api, DEFAULT_POD_CACHE_TTL_MS);
    }

    public K8SServiceInfoProvider(DiscoveryClient discoveryClient, CoreV1Api coreV1Api, long podCacheTtlMs) {
        this.discoveryClient = discoveryClient;
        this.coreV1Api = coreV1Api;
        this.podCacheTtlMs = podCacheTtlMs;
        log.info("K8SServiceInfoProvider inited, podCacheTtlMs={}", podCacheTtlMs);
    }

    /**
     * 获取所有服务实例信息
     *
     * @return 服务实例信息
     */
    @Override
    public List<ServiceInstanceInfoDTO> listServiceInfo() {
        List<ServiceInstance> serviceInstanceList = listJobServiceInstances();
        tryToLogServiceInstanceList(serviceInstanceList);
        List<ServiceInstance> validInstances = serviceInstanceList.stream()
            .filter(this::isJobServiceInstance)
            .toList();
        if (validInstances.isEmpty()) {
            return Collections.emptyList();
        }
        // 收集本轮涉及的 namespace 集合，每个 namespace 在一轮内最多触发 1 次 listNamespacedPod
        Set<String> namespaces = validInstances.stream()
            .map(this::getNameSpace)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toCollection(HashSet::new));
        Map<String, Map<String, V1Pod>> podMapByNamespace = new HashMap<>(namespaces.size());
        for (String namespace : namespaces) {
            podMapByNamespace.put(namespace, getOrLoadPodMap(namespace));
        }
        return validInstances.stream()
            .map(serviceInstance -> buildServiceInstanceInfo(serviceInstance, podMapByNamespace))
            .collect(Collectors.toList());
    }

    private List<ServiceInstance> listJobServiceInstances() {
        String jobServiceSymbol = "job-";
        List<String> serviceIdList = discoveryClient.getServices();
        List<ServiceInstance> serviceInstanceList = new ArrayList<>();
        for (String serviceId : serviceIdList) {
            if (serviceId != null && serviceId.contains(jobServiceSymbol)) {
                serviceInstanceList.addAll(discoveryClient.getInstances(serviceId));
            }
        }
        return serviceInstanceList;
    }

    private boolean isJobServiceInstance(ServiceInstance serviceInstance) {
        return serviceInstance != null
            && StringUtils.isNotBlank(serviceInstance.getServiceId())
            && !serviceInstance.getServiceId().contains("job-gateway-management")
            && StringUtils.isNotBlank(getNameSpace(serviceInstance));
    }

    /**
     * 从服务实例中提取命名空间：优先使用 KubernetesServiceInstance.getNamespace()，
     * 兜底读取 helm 注解。
     */
    private String getNameSpace(ServiceInstance serviceInstance) {
        if (serviceInstance instanceof KubernetesServiceInstance) {
            String namespace = ((KubernetesServiceInstance) serviceInstance).getNamespace();
            if (StringUtils.isNotBlank(namespace)) {
                return namespace;
            }
        }
        Map<String, String> metadata = serviceInstance.getMetadata();
        if (metadata == null) {
            return null;
        }
        return metadata.get(KEY_HELM_NAMESPACE);
    }

    /**
     * 从缓存读取或重新拉取指定 namespace 下所有 Pod，并按 uid 建立索引。
     * 拉取异常时优先返回上一份缓存（即便已过期），避免短暂 apiserver 抖动放大成全量 UNKNOWN。
     */
    private Map<String, V1Pod> getOrLoadPodMap(String namespace) {
        long now = System.currentTimeMillis();
        PodCacheEntry cached = podCacheByNamespace.get(namespace);
        if (cached != null && cached.expiryEpochMs > now) {
            return cached.podMapByUid;
        }
        try {
            V1PodList podList = coreV1Api.listNamespacedPod(
                namespace, null, null, null,
                null, null, null, null,
                null, null, null, null
            );
            Map<String, V1Pod> podMapByUid = buildPodMapByUid(podList);
            podCacheByNamespace.put(namespace, new PodCacheEntry(now + podCacheTtlMs, podMapByUid));
            return podMapByUid;
        } catch (ApiException e) {
            log.error(
                "Fail to listNamespacedPod, namespace={}, code={}, body={}",
                namespace, e.getCode(), e.getResponseBody(), e
            );
        } catch (Throwable t) {
            log.error("Fail to listNamespacedPod, namespace={}", namespace, t);
        }
        // listNamespacedPod 失败：返回上一份（即便已过期）保持兜底语义；都没有则返回空 map
        return cached != null ? cached.podMapByUid : Collections.emptyMap();
    }

    private Map<String, V1Pod> buildPodMapByUid(V1PodList podList) {
        if (podList == null || CollectionUtils.isEmpty(podList.getItems())) {
            return Collections.emptyMap();
        }
        Map<String, V1Pod> podMapByUid = new LinkedHashMap<>(podList.getItems().size());
        for (V1Pod pod : podList.getItems()) {
            V1ObjectMeta metadata = pod.getMetadata();
            if (metadata == null || StringUtils.isBlank(metadata.getUid())) {
                continue;
            }
            podMapByUid.put(metadata.getUid(), pod);
        }
        return podMapByUid;
    }

    private ServiceInstanceInfoDTO buildServiceInstanceInfo(ServiceInstance serviceInstance,
                                                            Map<String, Map<String, V1Pod>> podMapByNamespace) {
        ServiceInstanceInfoDTO dto = new ServiceInstanceInfoDTO();
        dto.setServiceName(serviceInstance.getServiceId());
        dto.setIp(serviceInstance.getHost());
        dto.setPort(serviceInstance.getPort());
        String namespace = getNameSpace(serviceInstance);
        Map<String, V1Pod> podMap = podMapByNamespace.getOrDefault(namespace, Collections.emptyMap());
        V1Pod pod = podMap.get(serviceInstance.getInstanceId());
        if (pod == null) {
            fillFallback(dto, serviceInstance, "Pod not found in namespace cache");
            return dto;
        }
        try {
            fillFromPod(dto, serviceInstance, pod);
        } catch (Throwable t) {
            log.warn(
                "Fail to extract pod detail, serviceId={}, instanceId={}",
                serviceInstance.getServiceId(), serviceInstance.getInstanceId(), t
            );
            fillFallback(dto, serviceInstance, "Fail to parse pod detail");
        }
        return dto;
    }

    private void fillFromPod(ServiceInstanceInfoDTO dto, ServiceInstance serviceInstance, V1Pod pod) {
        V1ObjectMeta metadata = pod.getMetadata();
        if (metadata != null && StringUtils.isNotBlank(metadata.getName())) {
            dto.setName(metadata.getName());
        } else {
            dto.setName(serviceInstance.getInstanceId());
        }
        dto.setVersion(extractVersion(metadata));
        V1PodStatus podStatus = pod.getStatus();
        dto.setStatusCode(convertPodStatus(podStatus));
        dto.setStatusMessage(podStatus == null ? null : podStatus.getReason());
        if (log.isDebugEnabled()) {
            log.debug("podStatus={}", JsonUtils.toJson(podStatus));
        }
    }

    private String extractVersion(V1ObjectMeta metadata) {
        if (metadata == null || metadata.getLabels() == null) {
            return VERSION_UNKNOWN;
        }
        String version = metadata.getLabels().get(KEY_JOB_MS_VERSION);
        return StringUtils.isBlank(version) ? VERSION_UNKNOWN : version;
    }

    private Byte convertPodStatus(V1PodStatus podStatus) {
        if (podStatus == null) {
            return ServiceInstanceInfoDTO.STATUS_UNKNOWN;
        }
        String phase = podStatus.getPhase();
        if (StringUtils.isBlank(phase)) {
            return ServiceInstanceInfoDTO.STATUS_UNKNOWN;
        }
        if (PHASE_RUNNING.equalsIgnoreCase(phase)) {
            return ServiceInstanceInfoDTO.STATUS_OK;
        }
        return ServiceInstanceInfoDTO.STATUS_ERROR;
    }

    private void fillFallback(ServiceInstanceInfoDTO dto, ServiceInstance serviceInstance, String message) {
        dto.setName(serviceInstance.getInstanceId());
        dto.setVersion(VERSION_UNKNOWN);
        dto.setStatusCode(ServiceInstanceInfoDTO.STATUS_UNKNOWN);
        dto.setStatusMessage(message);
    }

    private void tryToLogServiceInstanceList(List<ServiceInstance> serviceInstanceList) {
        try {
            for (ServiceInstance serviceInstance : serviceInstanceList) {
                Map<String, String> metaData = serviceInstance.getMetadata();
                // 清除null key，防止后续序列化失败
                if (metaData != null && metaData.containsKey(null)) {
                    log.debug("Ignore null key value:{}", metaData.get(null));
                    metaData.remove(null);
                }
                if (log.isDebugEnabled()) {
                    log.debug("serviceInstance={}", JsonUtils.toJson(serviceInstance));
                }
            }
        } catch (Throwable t) {
            log.warn("Fail to logServiceInstanceList", t);
        }
    }

    /**
     * Pod 缓存条目：携带过期时间戳与按 uid 索引的 Pod Map。
     */
    private static final class PodCacheEntry {
        private final long expiryEpochMs;
        private final Map<String, V1Pod> podMapByUid;

        private PodCacheEntry(long expiryEpochMs, Map<String, V1Pod> podMapByUid) {
            this.expiryEpochMs = expiryEpochMs;
            this.podMapByUid = podMapByUid;
        }
    }

}
