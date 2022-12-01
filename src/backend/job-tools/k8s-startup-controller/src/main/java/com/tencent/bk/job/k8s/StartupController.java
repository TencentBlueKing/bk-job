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
import io.kubernetes.client.openapi.models.V1ObjectReference;
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
import java.util.List;
import java.util.Map;

/**
 * 启动控制器，用于控制在K8s部署时各Service的启动顺序
 */
@Slf4j
public class StartupController {

    private final static CoreV1Api api;

    static {
        ApiClient client = null;
        try {
            client = Config.defaultClient();
        } catch (IOException e) {
            log.error("Fail to get defaultClient", e);
        }
        Configuration.setDefaultApiClient(client);
        api = new CoreV1Api();
    }

    public static void main(String[] args) {
        // 解析需要的依赖参数
        ServiceDependModel serviceDependModel = parseDependModelFromArgsOrEnv(args);
        String namespace = serviceDependModel.getNamespace();
        String currentService = serviceDependModel.getServiceName();
        String dependenciesStr = serviceDependModel.getDependenciesStr();
        log.info("namespace={}", namespace);
        log.info("dependenciesStr={}", dependenciesStr);
        log.info("currentService={}", currentService);
        // 解析出结构化的依赖映射表
        Map<String, List<String>> dependencyMap = parseDependencyMap(dependenciesStr);
        printDependencyMap(dependencyMap);
        if (StringUtils.isBlank(currentService)) {
            log.warn("currentService is blank, ignore dependency check");
            System.exit(1);
        }
        // 获取依赖服务列表
        List<String> dependServiceList = dependencyMap.get(currentService);
        if (CollectionUtils.isEmpty(dependServiceList)) {
            log.info("There is no depend service for {}", currentService);
            return;
        }
        log.info("{} depend service found for {}:{}", dependServiceList.size(), currentService, dependServiceList);
        // 等待所有依赖服务启动完成
        while (!isAllDependServiceReady(namespace, dependServiceList)) {
            ThreadUtils.sleep(3000);
        }
        log.info("all depend services are ready, it`s time for {} to start", currentService);
    }

    /**
     * 从命令行参数或环境变量解析出程序运行需要的服务依赖参数
     *
     * @param args 命令行参数
     * @return 服务依赖数据
     */
    private static ServiceDependModel parseDependModelFromArgsOrEnv(String[] args) {
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
        return serviceDependModel;
    }

    /**
     * 根据依赖定义字符串解析出依赖关系Map
     *
     * @param dependenciesStr 依赖定义字符串，模式：(service1:service2,service3),(service2:service4),...
     *                        含义：service1依赖于service2、service3，service2依赖于service4
     * @return 服务间依赖关系Map<服务名 ， 依赖的服务列表>
     */
    public static Map<String, List<String>> parseDependencyMap(String dependenciesStr) {
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
     * 检查所有依赖服务是否准备好
     *
     * @param namespace         命名空间
     * @param dependServiceList 依赖服务列表
     * @return 所有依赖服务是否准备好
     */
    private static boolean isAllDependServiceReady(String namespace,
                                                   List<String> dependServiceList) {
        for (String dependService : dependServiceList) {
            if (!isServiceReady(namespace, dependService)) {
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
        String nameFieldSelector = buildServiceEndpointNameSelector(serviceName);
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

    private static String buildServiceEndpointNameSelector(String serviceName) {
        return "metadata.name=" + serviceName;
    }

    /**
     * 根据服务名称对应的Pod状态判断服务是否准备好，依据：服务所有Container的Readiness探针已准备好，EndPointAddress全部处于Ready状态
     *
     * @param namespace   命名空间
     * @param serviceName 服务名称
     * @return 服务是否准备好布尔值
     */
    private static boolean isServiceReady(String namespace, String serviceName) {
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
        return readyAddressNum > 0 && readyAddressNum == allAddressNum;
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
