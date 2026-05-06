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
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodStatus;
import io.kubernetes.client.openapi.models.V1Service;
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
 * 3. 命名空间维度的进程内 TTL 缓存（默认 5 秒），TTL 内重复触发命中缓存；
 * 4. listNamespacedPod 透传 labelSelector（默认 {@link #DEFAULT_POD_LABEL_SELECTOR}），
 *    apiserver 侧即裁剪掉非作业平台 Pod，减少返回体积与本进程反序列化开销；
 * 5. 服务过滤主路径直接复用 spring-cloud-kubernetes 的 informer 本地缓存
 *    （{@link Lister}&lt;{@link V1Service}&gt;），按 {@value #SERVICE_LABEL_APP_KUBERNETES_IO_NAME}=
 *    {@value #SERVICE_LABEL_APP_KUBERNETES_IO_NAME_VALUE_BK_JOB} 标签精确筛选作业平台 Service，
 *    避免按服务名 contains("job-") 误命中其它项目；informer 未注入或未 sync 完成时回退到
 *    {@link DiscoveryClient#getServices()} + 名称前缀兜底，避免启动期短暂空窗。
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

    /**
     * 仅拉取作业平台自身的 Pod，避免在共享 namespace 下把整个 namespace 内的 Pod 全部 List 回来，
     * 显著降低 apiserver 与本进程的传输/反序列化开销。
     */
    public static final String DEFAULT_POD_LABEL_SELECTOR = "app.kubernetes.io/name=bk-job";

    /**
     * 作业平台 Service / Pod 共享的 {@code app.kubernetes.io/name} 标签 key，
     * 与 Helm Chart 中 {@code _helpers.tpl} 的 selectorLabels 保持一致。
     */
    public static final String SERVICE_LABEL_APP_KUBERNETES_IO_NAME = "app.kubernetes.io/name";
    /**
     * 作业平台 Service 的 {@code app.kubernetes.io/name} 标签值，与 {@link #DEFAULT_POD_LABEL_SELECTOR}
     * 中的取值保持同源。
     */
    public static final String SERVICE_LABEL_APP_KUBERNETES_IO_NAME_VALUE_BK_JOB = "bk-job";

    /**
     * 兜底路径（informer 未启动 / 未 sync 时）使用的服务名包含子串，与历史实现保持一致。
     */
    public static final String FALLBACK_JOB_SERVICE_NAME_KEYWORD = "job-";

    /**
     * 主路径与兜底路径都需过滤掉的网关管理服务名子串。
     */
    public static final String SERVICE_NAME_GATEWAY_MANAGEMENT = "job-gateway-management";

    private final DiscoveryClient discoveryClient;
    private final CoreV1Api coreV1Api;
    private final long podCacheTtlMs;
    /**
     * 调用 {@code listNamespacedPod} 时透传给 apiserver 的 labelSelector。
     * 允许通过构造器在装配/测试时覆写，未传时使用 {@link #DEFAULT_POD_LABEL_SELECTOR}。
     */
    private final String podLabelSelector;
    /**
     * spring-cloud-kubernetes informer 维护的 Service 本地缓存。可为 {@code null}：
     * 表示当前装配链路下无可用 Lister（如 ConsulDiscoveryClient 场景或测试场景），
     * 此时主路径自动降级到旧的名称兜底逻辑。
     */
    private final Lister<V1Service> servicesLister;
    private final ConcurrentHashMap<String, PodCacheEntry> podCacheByNamespace = new ConcurrentHashMap<>();

    public K8SServiceInfoProvider(DiscoveryClient discoveryClient, CoreV1Api coreV1Api) {
        this(discoveryClient, coreV1Api, DEFAULT_POD_CACHE_TTL_MS, DEFAULT_POD_LABEL_SELECTOR, null);
    }

    public K8SServiceInfoProvider(DiscoveryClient discoveryClient, CoreV1Api coreV1Api, long podCacheTtlMs) {
        this(discoveryClient, coreV1Api, podCacheTtlMs, DEFAULT_POD_LABEL_SELECTOR, null);
    }

    public K8SServiceInfoProvider(DiscoveryClient discoveryClient,
                                  CoreV1Api coreV1Api,
                                  long podCacheTtlMs,
                                  String podLabelSelector) {
        this(discoveryClient, coreV1Api, podCacheTtlMs, podLabelSelector, null);
    }

    public K8SServiceInfoProvider(DiscoveryClient discoveryClient,
                                  CoreV1Api coreV1Api,
                                  long podCacheTtlMs,
                                  String podLabelSelector,
                                  Lister<V1Service> servicesLister) {
        this.discoveryClient = discoveryClient;
        this.coreV1Api = coreV1Api;
        this.podCacheTtlMs = podCacheTtlMs;
        this.podLabelSelector = podLabelSelector;
        this.servicesLister = servicesLister;
        log.info(
            "K8SServiceInfoProvider inited, podCacheTtlMs={}, podLabelSelector={}, servicesListerInjected={}",
            podCacheTtlMs, podLabelSelector, servicesLister != null
        );
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

    /**
     * 列出作业平台所有服务实例。
     * <p>
     * 主路径：从 informer 本地 Service 缓存按 {@value #SERVICE_LABEL_APP_KUBERNETES_IO_NAME}=
     * {@value #SERVICE_LABEL_APP_KUBERNETES_IO_NAME_VALUE_BK_JOB} 标签筛选 Service，再用每个命中
     * Service 的 metadata.name 作为 serviceId 调用 {@link DiscoveryClient#getInstances(String)}；
     * 主路径内即过滤掉 {@value #SERVICE_NAME_GATEWAY_MANAGEMENT}（与现状一致），后续
     * {@link #isJobServiceInstance(ServiceInstance)} 还会再次确认。
     * <p>
     * 兜底路径：当 informer 未启动 / 未 sync 完成（servicesLister 为 null、抛异常或 list 返回空）时，
     * 回退到 {@link DiscoveryClient#getServices()} + 服务名 contains
     * {@value #FALLBACK_JOB_SERVICE_NAME_KEYWORD} 的旧逻辑，避免启动期短暂空窗导致 listAll 返回空。
     */
    private List<ServiceInstance> listJobServiceInstances() {
        List<String> jobServiceIds = listJobServiceIdsByLabel();
        if (jobServiceIds == null) {
            return listJobServiceInstancesByNameFallback();
        }
        List<ServiceInstance> serviceInstanceList = new ArrayList<>();
        for (String serviceId : jobServiceIds) {
            serviceInstanceList.addAll(discoveryClient.getInstances(serviceId));
        }
        return serviceInstanceList;
    }

    /**
     * 通过 servicesLister 按标签筛选作业平台 Service，返回服务名列表。
     * <ul>
     *   <li>返回 {@code null}：lister 为 null / list() 抛异常 / list() 返回 null 或空集合，
     *       说明 informer 未注入或未 sync 完成，调用方需走兜底路径；</li>
     *   <li>返回非 null 列表（即使为空）：说明 lister 可读，空列表是合法结果（确实没有匹配 Service）。</li>
     * </ul>
     */
    private List<String> listJobServiceIdsByLabel() {
        if (servicesLister == null) {
            log.warn(
                "servicesLister is not injected, fallback to discoveryClient.getServices() + name contains \"{}\"",
                FALLBACK_JOB_SERVICE_NAME_KEYWORD
            );
            return null;
        }
        List<V1Service> services;
        try {
            services = servicesLister.list();
        } catch (Throwable t) {
            log.warn(
                "Fail to list services from servicesLister, "
                    + "fallback to discoveryClient.getServices() + name contains \"{}\"",
                FALLBACK_JOB_SERVICE_NAME_KEYWORD, t
            );
            return null;
        }
        if (CollectionUtils.isEmpty(services)) {
            log.warn(
                "servicesLister returned empty result (informer not synced?), "
                    + "fallback to discoveryClient.getServices() + name contains \"{}\"",
                FALLBACK_JOB_SERVICE_NAME_KEYWORD
            );
            return null;
        }
        List<String> serviceIds = new ArrayList<>();
        for (V1Service service : services) {
            String serviceId = extractBkJobServiceName(service);
            if (serviceId == null) {
                continue;
            }
            // 主路径与兜底路径一致：保留对 job-gateway-management 的过滤
            if (serviceId.contains(SERVICE_NAME_GATEWAY_MANAGEMENT)) {
                continue;
            }
            serviceIds.add(serviceId);
        }
        return serviceIds;
    }

    /**
     * 提取 V1Service 中带 {@value #SERVICE_LABEL_APP_KUBERNETES_IO_NAME}=
     * {@value #SERVICE_LABEL_APP_KUBERNETES_IO_NAME_VALUE_BK_JOB} 标签的服务名；
     * 标签缺失或值不匹配返回 {@code null}。
     */
    private String extractBkJobServiceName(V1Service service) {
        if (service == null) {
            return null;
        }
        V1ObjectMeta metadata = service.getMetadata();
        if (metadata == null
            || metadata.getLabels() == null
            || StringUtils.isBlank(metadata.getName())) {
            return null;
        }
        String labelValue = metadata.getLabels().get(SERVICE_LABEL_APP_KUBERNETES_IO_NAME);
        if (!SERVICE_LABEL_APP_KUBERNETES_IO_NAME_VALUE_BK_JOB.equals(labelValue)) {
            return null;
        }
        return metadata.getName();
    }

    /**
     * 兜底路径：保留历史的 {@code discoveryClient.getServices()} + 服务名 contains
     * {@value #FALLBACK_JOB_SERVICE_NAME_KEYWORD} 的过滤逻辑。仅在 servicesLister 不可用时触发。
     */
    private List<ServiceInstance> listJobServiceInstancesByNameFallback() {
        List<String> serviceIdList = discoveryClient.getServices();
        List<ServiceInstance> serviceInstanceList = new ArrayList<>();
        for (String serviceId : serviceIdList) {
            if (serviceId != null && serviceId.contains(FALLBACK_JOB_SERVICE_NAME_KEYWORD)) {
                serviceInstanceList.addAll(discoveryClient.getInstances(serviceId));
            }
        }
        return serviceInstanceList;
    }

    private boolean isJobServiceInstance(ServiceInstance serviceInstance) {
        return serviceInstance != null
            && StringUtils.isNotBlank(serviceInstance.getServiceId())
            && !serviceInstance.getServiceId().contains(SERVICE_NAME_GATEWAY_MANAGEMENT)
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
            // io.kubernetes:client-java 19.0.0 中 listNamespacedPod 的 labelSelector 是第 6 个形参
            // (0-based index 5)：namespace, pretty, allowWatchBookmarks, _continue, fieldSelector,
            //  labelSelector, limit, resourceVersion, resourceVersionMatch, sendInitialEvents,
            //  timeoutSeconds, watch
            V1PodList podList = coreV1Api.listNamespacedPod(
                namespace, null, null, null,
                null, podLabelSelector, null, null,
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
