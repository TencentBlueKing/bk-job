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

import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.ThreadUtils;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodStatus;
import io.kubernetes.client.openapi.models.V1Probe;
import io.kubernetes.client.util.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 启动控制器，用于控制在K8s部署时各微服务的启动顺序
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
        String namespace = System.getenv(Consts.KEY_KUBERNETES_NAMESPACE);
        String dependenciesStr = System.getenv(Consts.KEY_STARTUP_DEPENDENCIES_STR);
        String currentService = System.getenv(Consts.KEY_CURRENT_SERVICE);
        log.info("namespace={}", namespace);
        log.info("dependenciesStr={}", dependenciesStr);
        log.info("currentService={}", currentService);
        Map<String, List<String>> dependencyMap = parseDependencyMap(dependenciesStr);
        printDependencyMap(dependencyMap);
        if (StringUtils.isBlank(currentService)) {
            log.warn("currentService is blank, ignore dependency check");
            return;
        }
        List<String> dependServiceList = dependencyMap.get(currentService);
        if (CollectionUtils.isEmpty(dependServiceList)) {
            log.info("There is no depend service for {}", currentService);
            return;
        }
        log.info("{} depend service found for {}:{}", dependServiceList.size(), currentService, dependServiceList);
        while (!isAllDependServiceReady(namespace, dependServiceList)) {
            ThreadUtils.sleep(3000);
        }
        log.info("all depend services are ready, it`s time for {} to start", currentService);
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

    private static String buildServiceLabelSelector(String serviceName) {
        return "bk.job.scope=backend,app.kubernetes.io/component=" + serviceName;
    }

    /**
     * 根据服务名称获取Pod列表
     *
     * @param namespace   命名空间
     * @param serviceName 服务名称
     * @return Pod列表
     */
    private static List<V1Pod> listPodByServiceName(String namespace, String serviceName) {
        String labelSelector = buildServiceLabelSelector(serviceName);
        try {
            V1PodList podList = api.listNamespacedPod(
                namespace, null, null, null,
                null, labelSelector, null, null,
                null, null, null
            );
            return podList.getItems();
        } catch (ApiException e) {
            log.error("Fail to list pod", e);
            return Collections.emptyList();
        }
    }

    private static void printDependencyMap(Map<String, List<String>> dependencyMap) {
        dependencyMap.forEach(
            (serviceName, dependServiceList) -> log.info("{} depends on {}", serviceName, dependServiceList)
        );
    }

    /**
     * 根据服务名称对应的Pod状态判断服务是否准备好，依据：所有Pod均准备好
     *
     * @param namespace   命名空间
     * @param serviceName 服务名称
     * @return 服务是否准备好布尔值
     */
    private static boolean isServiceReady(String namespace, String serviceName) {
        List<V1Pod> servicePodList = listPodByServiceName(namespace, serviceName);
        if (CollectionUtils.isEmpty(servicePodList)) {
            log.info("no pod found by service {}", serviceName);
            return true;
        }
        int readyPodNum = 0;
        for (V1Pod v1Pod : servicePodList) {
            if (isPodReady(v1Pod)) {
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
     * 判断Pod是否准备好，判断依据：状态数据中的phase字段值
     *
     * @param v1Pod Pod实例信息
     * @return Pod是否准备好布尔值
     */
    private static boolean isPodReady(V1Pod v1Pod) {
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
        V1Probe readinessProbe = v1Pod.getSpec().getContainers().get(0).getReadinessProbe();
        log.info("readinessProbe={}", readinessProbe.getHttpGet());
        return v1PodStatus != null
            && "Running".equalsIgnoreCase(v1PodStatus.getPhase());
    }

}
