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

import com.beust.jcommander.JCommander;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.ThreadUtils;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1EndpointAddress;
import io.kubernetes.client.openapi.models.V1EndpointSubset;
import io.kubernetes.client.openapi.models.V1Endpoints;
import io.kubernetes.client.openapi.models.V1EndpointsList;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1ObjectReference;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodStatus;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceList;
import io.kubernetes.client.openapi.models.V1ServiceSpec;
import io.kubernetes.client.util.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 启动控制器，用于控制在K8s部署时各Service的启动顺序
 */
@Slf4j
public class StartupController {

    // K8s API
    private static final CoreV1Api api;

    static {
        ApiClient client = null;
        try {
            client = Config.defaultClient();
        } catch (IOException e) {
            log.error("Fail to get k8s api defaultClient", e);
        }
        Configuration.setDefaultApiClient(client);
        api = new CoreV1Api();
    }

    // 主程序入口
    public static void main(String[] args) {
        // 解析需要的依赖参数
        ServiceDependModel serviceDependModel = parseDependModelFromArgsOrEnv(args);
        String namespace = serviceDependModel.getNamespace();
        String currentService = serviceDependModel.getServiceName();
        String dependenciesStr = serviceDependModel.getDependenciesStr();
        String expectLabelsCommon = serviceDependModel.getExpectLabelsCommon();
        String expectLabelsService = serviceDependModel.getExpectLabelsService();
        log.info("namespace={}", namespace);
        log.info("dependenciesStr={}", dependenciesStr);
        log.info("currentService={}", currentService);
        log.info("expectLabelsCommon={}", expectLabelsCommon);
        log.info("expectLabelsService={}", expectLabelsService);
        // 解析出结构化的依赖映射表
        Map<String, List<String>> dependencyMap = parseDependencyMap(dependenciesStr);
        printDependencyMap(dependencyMap);
        if (StringUtils.isBlank(currentService)) {
            log.warn("currentService is blank, ignore dependency check");
            // 异常退出
            System.exit(1);
        }
        // 获取依赖服务列表
        List<String> dependServiceList = dependencyMap.get(currentService);
        if (CollectionUtils.isEmpty(dependServiceList)) {
            log.info("There is no depend service for {}", currentService);
            return;
        }
        log.info("{} depend service found for {}:{}", dependServiceList.size(), currentService, dependServiceList);
        // 获取服务Pod需要拥有的标签信息
        Map<String, Map<String, String>> servicePodLabelsMap =
            parseExpectPodLabelsForService(expectLabelsCommon, expectLabelsService, dependServiceList);
        // 等待所有依赖服务启动完成
        long sleepMillsOnce = 3000;
        while (!isAllDependServiceReady(namespace, dependServiceList, servicePodLabelsMap)) {
            ThreadUtils.sleep(sleepMillsOnce);
        }
        log.info("all depend services are ready, it`s time for {} to start", currentService);
    }

    /**
     * 从命令行参数或环境变量解析出程序运行需要的服务依赖参数
     *
     * @param args 命令行参数
     * @return 服务依赖数据
     */
    static ServiceDependModel parseDependModelFromArgsOrEnv(String[] args) {
        ServiceDependModel serviceDependModel = new ServiceDependModel();
        JCommander.newBuilder()
            .addObject(serviceDependModel)
            .build()
            .parse(args);
        String namespace = serviceDependModel.getNamespace();
        if (StringUtils.isBlank(namespace)) {
            namespace = System.getenv(Consts.KEY_KUBERNETES_NAMESPACE);
            log.info(
                "Commandline param [-n,--namespace] is null or blank, use env variable {}={}",
                Consts.KEY_KUBERNETES_NAMESPACE,
                namespace
            );
            serviceDependModel.setNamespace(namespace);
        }
        String serviceName = serviceDependModel.getServiceName();
        if (StringUtils.isBlank(serviceName)) {
            serviceName = System.getenv(Consts.KEY_CURRENT_SERVICE_NAME);
            log.info(
                "Commandline param [-s,--service] is null or blank, use env variable {}={}",
                Consts.KEY_CURRENT_SERVICE_NAME,
                serviceName
            );
            serviceDependModel.setServiceName(serviceName);
        }
        String dependenciesStr = serviceDependModel.getDependenciesStr();
        if (StringUtils.isBlank(dependenciesStr)) {
            dependenciesStr = System.getenv(Consts.KEY_STARTUP_DEPENDENCIES_STR);
            log.info(
                "Commandline param [-d,--dependencies] is null or blank, use env variable {}={}",
                Consts.KEY_STARTUP_DEPENDENCIES_STR,
                dependenciesStr
            );
            serviceDependModel.setDependenciesStr(dependenciesStr);
        }
        String expectLabelsCommonStr = serviceDependModel.getExpectLabelsCommon();
        if (StringUtils.isBlank(expectLabelsCommonStr)) {
            expectLabelsCommonStr = System.getenv(Consts.KEY_EXPECT_POD_LABELS_COMMON);
            log.info(
                "Commandline param [-lc,--expect-pod-labels-common] is null or blank, use env variable {}={}",
                Consts.KEY_EXPECT_POD_LABELS_COMMON,
                expectLabelsCommonStr
            );
            serviceDependModel.setExpectLabelsCommon(expectLabelsCommonStr);
        }
        String expectLabelsServiceStr = serviceDependModel.getExpectLabelsService();
        if (StringUtils.isBlank(expectLabelsServiceStr)) {
            expectLabelsServiceStr = System.getenv(Consts.KEY_EXPECT_POD_LABELS_SERVICE);
            log.info(
                "Commandline param [-ls,--expect-pod-labels-service] is null or blank, use env variable {}={}",
                Consts.KEY_EXPECT_POD_LABELS_SERVICE,
                expectLabelsServiceStr
            );
            serviceDependModel.setExpectLabelsService(expectLabelsServiceStr);
        }
        return serviceDependModel;
    }

    /**
     * 根据依赖定义字符串解析出依赖关系Map
     *
     * @param dependenciesStr 依赖定义字符串，模式：(service1:service2,service3),(service2:service4),...
     *                        含义：service1依赖于service2、service3，service2依赖于service4
     * @return 服务间依赖关系Map<服务名 ， 依赖的服务列表>
     */
    static Map<String, List<String>> parseDependencyMap(String dependenciesStr) {
        if (StringUtils.isBlank(dependenciesStr)) {
            return Collections.emptyMap();
        }
        dependenciesStr = dependenciesStr.trim();
        dependenciesStr = dependenciesStr.replace(" ", "");
        Map<String, List<String>> dependencyMap = new HashMap<>();
        String separator = "\\),\\(";
        String[] dependencyArr = dependenciesStr.split(separator);
        for (String serviceDepStr : dependencyArr) {
            serviceDepStr = StringUtil.removePrefix(serviceDepStr, "(");
            serviceDepStr = StringUtil.removeSuffix(serviceDepStr, ")");
            String[] parts = serviceDepStr.split(":");
            if (parts.length != 2) {
                log.warn("illegal dependency:{}", serviceDepStr);
                continue;
            }
            String serviceName = parts[0];
            String depServiceStr = parts[1];
            List<String> dependServiceList = new ArrayList<>(Arrays.asList(depServiceStr.split(",")));
            if (dependencyMap.containsKey(serviceName)) {
                dependencyMap.get(serviceName).addAll(dependServiceList);
            } else {
                dependencyMap.put(serviceName, dependServiceList);
            }
        }
        return dependencyMap;
    }

    /**
     * 打印服务间依赖关系Map
     *
     * @param dependencyMap 服务间依赖关系Map
     */
    private static void printDependencyMap(Map<String, List<String>> dependencyMap) {
        dependencyMap.forEach(
            (serviceName, dependServiceList) -> log.info("{} depends on {}", serviceName, dependServiceList)
        );
    }

    /**
     * 根据配置参数与依赖服务列表解析出各依赖服务处于Ready状态时对应Pod所需要拥有的标签
     *
     * @param expectPodLabelsCommon  所有依赖服务都需要的公共标签
     * @param expectPodLabelsService 为每个服务单独定义的标签
     * @param dependServiceList      依赖服务列表
     * @return Map<服务名, Map < 标签Key, 标签Value>>
     */
    static Map<String, Map<String, String>> parseExpectPodLabelsForService(String expectPodLabelsCommon,
                                                                           String expectPodLabelsService,
                                                                           List<String> dependServiceList) {
        if (CollectionUtils.isEmpty(dependServiceList)) {
            return Collections.emptyMap();
        }
        // 公共标签
        Map<String, String> commonLabelMap = parseLabelsFromStr(expectPodLabelsCommon);
        Set<String> dependServiceSet = new HashSet<>(dependServiceList);
        Map<String, Map<String, String>> servicePodLabelsMap = new HashMap<>(dependServiceSet.size());
        dependServiceSet.forEach(service -> servicePodLabelsMap.put(service, new HashMap<>(commonLabelMap)));
        if (StringUtils.isBlank(expectPodLabelsService)) {
            return servicePodLabelsMap;
        }
        // 服务单独定义的标签
        String[] servicePodLabelsStrArr = expectPodLabelsService.split("\\),\\s*\\(");
        for (String servicePodLabelsStr : servicePodLabelsStrArr) {
            servicePodLabelsStr = servicePodLabelsStr.trim();
            servicePodLabelsStr = StringUtil.removePrefix(servicePodLabelsStr, "(");
            servicePodLabelsStr = StringUtil.removeSuffix(servicePodLabelsStr, ")");
            int serviceLabelsSeparatorIndex = servicePodLabelsStr.indexOf(":");
            if (serviceLabelsSeparatorIndex < 0) {
                log.warn("Invalid service label description(missing ':'):{}, ignore", servicePodLabelsStr);
                continue;
            }
            String serviceName = servicePodLabelsStr.substring(0, serviceLabelsSeparatorIndex).trim();
            String serviceLabelsStr = servicePodLabelsStr.substring(serviceLabelsSeparatorIndex + 1).trim();
            // 忽略不在依赖服务列表中的服务标签定义
            if (!servicePodLabelsMap.containsKey(serviceName)) {
                log.warn("service {} not in dependServices, ignore expectPodLabels", serviceName);
                continue;
            }
            Map<String, String> serviceLabelsMap = parseLabelsFromStr(serviceLabelsStr);
            servicePodLabelsMap.get(serviceName).putAll(serviceLabelsMap);
        }
        return servicePodLabelsMap;
    }

    /**
     * 从逗号分隔的label字符串中解析出多个label
     *
     * @param labelsStr 逗号分隔的label字符串
     * @return 解析出的标签Map
     */
    private static Map<String, String> parseLabelsFromStr(String labelsStr) {
        if (StringUtils.isBlank(labelsStr)) {
            return Collections.emptyMap();
        }
        labelsStr = labelsStr.trim();
        String labelSeparator = ",";
        String keyValueSeparator = "=";
        String[] labelArr = labelsStr.split(labelSeparator);
        Map<String, String> labelMap = new HashMap<>();
        for (String labelStr : labelArr) {
            int keyValueSeparatorIndex = labelStr.indexOf(keyValueSeparator);
            if (keyValueSeparatorIndex < 0) {
                log.warn("Invalid label(missing '='):{}, ignore", labelsStr);
                continue;
            }
            String key = labelStr.substring(0, keyValueSeparatorIndex).trim();
            String value = labelStr.substring(keyValueSeparatorIndex + 1).trim();
            labelMap.put(key, value);
        }
        return labelMap;
    }

    /**
     * 检查所有依赖服务是否准备好
     *
     * @param namespace         命名空间
     * @param dependServiceList 依赖服务列表
     * @return 所有依赖服务是否准备好
     */
    private static boolean isAllDependServiceReady(String namespace,
                                                   List<String> dependServiceList,
                                                   Map<String, Map<String, String>> servicePodLabelsMap) {
        for (String dependService : dependServiceList) {
            Map<String, String> requiredPodLabelsMap = servicePodLabelsMap.get(dependService);
            if (!isServiceReady(namespace, dependService, requiredPodLabelsMap)) {
                log.info("{} is not ready, waiting...", dependService);
                return false;
            }
        }
        return true;
    }

    /**
     * 根据服务名称获取EndPointAddress列表
     *
     * @param namespace   命名空间
     * @param serviceName 服务名称
     * @return EndPointAddress列表，根据服务名称查询不到相关EndPointAddress信息则返回null
     */
    private static Pair<List<V1EndpointAddress>, List<V1EndpointAddress>> listEndPointAddressByServiceName(
        String namespace,
        String serviceName
    ) {
        String nameFieldSelector = buildFieldSelectorByResourceName(serviceName);
        try {
            V1EndpointsList endpointsList = api.listNamespacedEndpoints(namespace, null, null,
                null, nameFieldSelector, null, null,
                null, null, null, null);
            List<V1Endpoints> endPoints = endpointsList.getItems();
            if (CollectionUtils.isEmpty(endPoints)) {
                log.warn(
                    "Found no endPoints for service:{}/{}, nameFieldSelector={}, endpointsList={}",
                    namespace,
                    serviceName,
                    nameFieldSelector,
                    endpointsList
                );
                return null;
            } else {
                log.debug("{} endpoints found for service:{}/{}", endPoints.size(), namespace, serviceName);
            }
            V1Endpoints endpoint = endpointsList.getItems().get(0);
            List<V1EndpointSubset> endpointSubsets = endpoint.getSubsets();
            if (CollectionUtils.isEmpty(endpointSubsets)) {
                log.warn("Found no endpointSubsets for endpoint:{}", endpoint.getMetadata());
                return null;
            } else {
                log.debug("{} endpointSubsets found for endpoint:{}", endpointSubsets.size(), endpoint.getMetadata());
            }
            V1EndpointSubset endpointSubset = endpointSubsets.get(0);
            List<V1EndpointAddress> addressList = endpointSubset.getAddresses();
            List<V1EndpointAddress> notReadyAddressList = endpointSubset.getNotReadyAddresses();
            return Pair.of(addressList, notReadyAddressList);
        } catch (ApiException e) {
            log.error("Fail to listEndPointAddressByServiceName", e);
            return null;
        }
    }

    /**
     * 通过资源名称构建字段选择器
     *
     * @param resourceName 资源名称
     * @return 字段选择器字符串
     */
    private static String buildFieldSelectorByResourceName(String resourceName) {
        return "metadata.name=" + resourceName;
    }

    /**
     * 判断服务是否准备好，依据：服务EndPointAddress全部处于Ready状态，且服务下所有Pod拥有必须要有的所有标签
     *
     * @param namespace            命名空间
     * @param serviceName          服务名称
     * @param requiredPodLabelsMap 服务Pod需要拥有的所有标签
     * @return 服务是否准备好，布尔值
     */
    private static boolean isServiceReady(String namespace,
                                          String serviceName,
                                          Map<String, String> requiredPodLabelsMap) {
        return isServiceEndPointAddressReady(namespace, serviceName)
            && isServicePodReady(namespace, serviceName, requiredPodLabelsMap);
    }

    /**
     * 根据服务名称判断对应的EndAddress是否都已准备好，依据：服务所有Container的Readiness探针已准备好，EndPointAddress全部处于Ready状态
     *
     * @param namespace   命名空间
     * @param serviceName 服务名称
     * @return 服务EndPointAddress是否准备好，布尔值
     */
    private static boolean isServiceEndPointAddressReady(String namespace, String serviceName) {
        Pair<List<V1EndpointAddress>, List<V1EndpointAddress>> pair =
            listEndPointAddressByServiceName(namespace, serviceName);
        if (pair == null) {
            log.warn("cannot find endpoint address by service {}/{}, consider as not ready", namespace, serviceName);
            return false;
        }
        List<V1EndpointAddress> readyEndpointAddressList = pair.getLeft();
        List<V1EndpointAddress> notReadyEndpointAddressList = pair.getRight();
        if (log.isDebugEnabled()) {
            log.debug("Ready endpoints for {}:", serviceName);
            printEndPointAddressList(readyEndpointAddressList);
            log.debug("NotReady endpoints for {}:", serviceName);
            printEndPointAddressList(notReadyEndpointAddressList);
        }
        int readyAddressNum = safeGetEndPointListSize(readyEndpointAddressList);
        int allAddressNum = readyAddressNum + safeGetEndPointListSize(notReadyEndpointAddressList);
        log.info("{}: {}/{} EndpointAddress ready",
            serviceName,
            readyAddressNum,
            allAddressNum
        );
        return readyAddressNum > 0
            && readyAddressNum == allAddressNum;
    }

    /**
     * 判断服务对应的Pod是否准备好，依据：Pod全部处于Running状态，且拥有指定的所有标签
     *
     * @param namespace            命名空间
     * @param serviceName          服务名称
     * @param requiredPodLabelsMap 服务Pod需要拥有的所有标签
     * @return 服务Pod是否准备好，布尔值
     */
    private static boolean isServicePodReady(String namespace, String serviceName,
                                             Map<String, String> requiredPodLabelsMap) {
        List<V1Pod> servicePodList = listPodByServiceName(namespace, serviceName);
        if (CollectionUtils.isEmpty(servicePodList)) {
            log.info("no pod found by service {}", serviceName);
            return true;
        }
        int readyPodNum = 0;
        for (V1Pod v1Pod : servicePodList) {
            if (isPodReady(v1Pod, requiredPodLabelsMap)) {
                readyPodNum++;
            } else {
                V1ObjectMeta metaData = v1Pod.getMetadata();
                if (metaData != null) {
                    log.info("pod {} is not ready", metaData.getName());
                } else {
                    log.warn("pod {} is not ready", v1Pod);
                }
            }
        }
        log.info("{}: {}/{} pod ready", serviceName, readyPodNum, servicePodList.size());
        return readyPodNum == servicePodList.size();
    }

    /**
     * 根据服务名称获取Pod列表
     *
     * @param namespace   命名空间
     * @param serviceName 服务名称
     * @return Pod列表
     */
    private static List<V1Pod> listPodByServiceName(String namespace, String serviceName) {
        String labelSelector = buildServicePodLabelSelector(namespace, serviceName);
        log.info("list pod of service {}/{} using labelSelector:{}", namespace, serviceName, labelSelector);
        try {
            V1PodList v1PodList = api.listNamespacedPod(
                namespace, null, null, null,
                null, labelSelector, null, null,
                null, null, null
            );
            List<V1Pod> podList = v1PodList.getItems();
            log.info("{} pod found for {}/{}", podList.size(), namespace, serviceName);
            return podList;
        } catch (ApiException e) {
            log.error("Fail to list pod", e);
            return Collections.emptyList();
        }
    }

    /**
     * 使用Service资源的Selector信息构建Pod的标签选择器用于选取Pod
     *
     * @param namespace   命名空间
     * @param serviceName 服务名称
     * @return 用于选取Pod的标签选择器字符串
     */
    private static String buildServicePodLabelSelector(String namespace, String serviceName) {
        try {
            V1ServiceList v1ServiceList = api.listNamespacedService(
                namespace, null, null, null,
                buildFieldSelectorByResourceName(serviceName), null, null, null,
                null, null, null
            );
            List<V1Service> serviceList = v1ServiceList.getItems();
            if (CollectionUtils.isEmpty(serviceList)) {
                log.warn("serviceList is null/empty, service={}/{}", namespace, serviceName);
                return null;
            } else if (serviceList.size() > 1) {
                log.warn("Unexpected serviceList:{}", serviceList);
            }
            V1Service v1Service = serviceList.get(0);
            V1ServiceSpec serviceSpec = v1Service.getSpec();
            if (serviceSpec == null) {
                log.warn("serviceSpec is null, v1Service={}", v1Service);
                return null;
            }
            Map<String, String> selectorLabelMap = serviceSpec.getSelector();
            if (selectorLabelMap == null) {
                log.warn("selectorLabelMap is null, serviceSpec={}", serviceSpec);
                return null;
            }
            StringBuilder sb = new StringBuilder();
            String labelSeparator = ",";
            selectorLabelMap.forEach((labelKey, labelValue) -> {
                sb.append(labelKey);
                sb.append("=");
                sb.append(labelValue);
                sb.append(labelSeparator);
            });
            String selectorStr = sb.toString();
            if (selectorStr.endsWith(labelSeparator)) {
                selectorStr = StringUtil.removeSuffix(selectorStr, labelSeparator);
            }
            return selectorStr;
        } catch (ApiException e) {
            log.error("Fail to listNamespacedService", e);
            return null;
        }
    }

    /**
     * 判断Pod是否准备好，判断依据：状态数据中的phase字段值
     *
     * @param v1Pod                Pod实例信息
     * @param requiredPodLabelsMap Pod实例所需要拥有的标签
     * @return Pod是否准备好布尔值
     */
    private static boolean isPodReady(V1Pod v1Pod, Map<String, String> requiredPodLabelsMap) {
        V1PodStatus v1PodStatus = v1Pod.getStatus();
        if (log.isDebugEnabled()) {
            if (v1Pod.getMetadata() == null) {
                log.debug("unexpected pod:{}", v1Pod);
                return false;
            }
            if (v1PodStatus == null) {
                log.debug("status of pod {} is null", v1Pod.getMetadata().getName());
                return false;
            }
            log.debug("phase of pod {}:{}", v1Pod.getMetadata().getName(), v1PodStatus.getPhase());
        }
        String targetPhase = "Running";
        return v1PodStatus != null
            && targetPhase.equalsIgnoreCase(v1PodStatus.getPhase())
            && isPodHasLabels(v1Pod, requiredPodLabelsMap);
    }

    /**
     * 判断Pod是否拥有指定的所有标签
     *
     * @param v1Pod                Pod实例信息
     * @param requiredPodLabelsMap 所需要拥有的标签
     * @return Pod是否拥有所有标签布尔值
     */
    private static boolean isPodHasLabels(V1Pod v1Pod, Map<String, String> requiredPodLabelsMap) {
        V1ObjectMeta metaData = v1Pod.getMetadata();
        if (metaData == null) {
            log.warn("metaData is null, pod={}", v1Pod);
            return false;
        }
        Map<String, String> podLabelsMap = metaData.getLabels();
        if (podLabelsMap == null || podLabelsMap.isEmpty()) {
            log.warn("podLabelsMap is null/empty, metaData={}", metaData);
            return false;
        }
        log.debug("pod={},requiredPodLabelsMap={},podLabelsMap={}", v1Pod, requiredPodLabelsMap, podLabelsMap);
        for (Map.Entry<String, String> entry : requiredPodLabelsMap.entrySet()) {
            String requiredLabelKey = entry.getKey();
            String requiredLabelValue = entry.getValue();
            if (!podLabelsMap.containsKey(requiredLabelKey)) {
                log.warn("pod labels do not contain required labelKey:{}", requiredLabelKey);
                return false;
            }
            String labelValue = podLabelsMap.get(requiredLabelKey);
            if (!requiredLabelValue.equals(labelValue)) {
                log.warn(
                    "pod label value does not match: requiredLabelKey:{}, requiredLabelValue:{}, labelValue:{}",
                    requiredLabelKey,
                    requiredLabelValue,
                    labelValue
                );
                return false;
            }
        }
        return true;
    }

    /**
     * 可视化打印V1EndpointAddress列表数据
     *
     * @param endpointAddressList EndpointAddress列表数据
     */
    private static void printEndPointAddressList(List<V1EndpointAddress> endpointAddressList) {
        if (endpointAddressList == null) {
            log.debug("endpointAddressList is null");
            return;
        }
        endpointAddressList.forEach(endpointAddress -> {
            V1ObjectReference targetRef = endpointAddress.getTargetRef();
            assert targetRef != null;
            log.debug("{}: {}", targetRef.getKind(), targetRef.getName());
        });
    }

    /**
     * 安全获取endpointAddressList的元素数量，避免空指针异常
     *
     * @param endpointAddressList 列表数据
     * @return 列表内元素数量
     */
    private static int safeGetEndPointListSize(List<V1EndpointAddress> endpointAddressList) {
        if (CollectionUtils.isEmpty(endpointAddressList)) {
            return 0;
        }
        return endpointAddressList.size();
    }

}
