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

import com.beust.jcommander.JCommander;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 服务依赖模型解析器
 */
@Slf4j
public class ServiceDependModelParser {

    /**
     * 从命令行参数或环境变量解析出程序运行需要的服务依赖参数
     *
     * @param args 命令行参数
     * @return 服务依赖数据
     */
    public ServiceDependModel parseDependModelFromArgsOrEnv(String[] args) {
        ServiceDependModel serviceDependModel = new ServiceDependModel();
        JCommander.newBuilder()
            .addObject(serviceDependModel)
            .build()
            .parse(args);
        fillNamespaceWithEnvVariableIfAbsent(serviceDependModel);
        fillServiceNameWithEnvVariableIfAbsent(serviceDependModel);
        fillDependenciesStrWithEnvVariableIfAbsent(serviceDependModel);
        fillExpectLabelsCommonStrWithEnvVariableIfAbsent(serviceDependModel);
        fillExpectLabelsServiceStrWithEnvVariableIfAbsent(serviceDependModel);
        fillExternalDependencyCheckEnabledWithEnvVariableIfAbsent(serviceDependModel);
        fillExternalDependencyCheckUrlWithEnvVariableIfAbsent(serviceDependModel);
        return serviceDependModel;
    }

    /**
     * 使用环境变量填充服务依赖模型中的命名空间取值
     *
     * @param serviceDependModel 服务依赖模型
     */
    private void fillNamespaceWithEnvVariableIfAbsent(ServiceDependModel serviceDependModel) {
        String namespace = serviceDependModel.getNamespace();
        if (StringUtils.isNotBlank(namespace)) {
            return;
        }
        namespace = System.getenv(Consts.KEY_KUBERNETES_NAMESPACE);
        log.info(
            "Commandline param [-n,--namespace] is null or blank, use env variable {}={}",
            Consts.KEY_KUBERNETES_NAMESPACE,
            namespace
        );
        if (StringUtils.isBlank(namespace)) {
            namespace = Consts.VALUE_NAMESPACE_DEFAULT;
            log.warn("use default namespace:{}", namespace);
        }
        serviceDependModel.setNamespace(namespace);
    }

    /**
     * 使用环境变量填充服务依赖模型中的服务名称取值
     *
     * @param serviceDependModel 服务依赖模型
     */
    private void fillServiceNameWithEnvVariableIfAbsent(ServiceDependModel serviceDependModel) {
        String serviceName = serviceDependModel.getServiceName();
        if (StringUtils.isNotBlank(serviceName)) {
            return;
        }
        serviceName = System.getenv(Consts.KEY_CURRENT_SERVICE_NAME);
        log.info(
            "Commandline param [-s,--service] is null or blank, use env variable {}={}",
            Consts.KEY_CURRENT_SERVICE_NAME,
            serviceName
        );
        serviceDependModel.setServiceName(serviceName);
    }

    /**
     * 使用环境变量填充服务依赖模型中的依赖关系取值
     *
     * @param serviceDependModel 服务依赖模型
     */
    private void fillDependenciesStrWithEnvVariableIfAbsent(ServiceDependModel serviceDependModel) {
        String dependenciesStr = serviceDependModel.getDependenciesStr();
        if (StringUtils.isNotBlank(dependenciesStr)) {
            return;
        }
        dependenciesStr = System.getenv(Consts.KEY_STARTUP_DEPENDENCIES_STR);
        log.info(
            "Commandline param [-d,--dependencies] is null or blank, use env variable {}={}",
            Consts.KEY_STARTUP_DEPENDENCIES_STR,
            dependenciesStr
        );
        serviceDependModel.setDependenciesStr(dependenciesStr);
    }

    /**
     * 使用环境变量填充服务依赖模型中的期望Pod公共标签取值
     *
     * @param serviceDependModel 服务依赖模型
     */
    private void fillExpectLabelsCommonStrWithEnvVariableIfAbsent(ServiceDependModel serviceDependModel) {
        String expectLabelsCommonStr = serviceDependModel.getExpectLabelsCommon();
        if (StringUtils.isNotBlank(expectLabelsCommonStr)) {
            return;
        }
        expectLabelsCommonStr = System.getenv(Consts.KEY_EXPECT_POD_LABELS_COMMON);
        log.info(
            "Commandline param [-lc,--expect-pod-labels-common] is null or blank, use env variable {}={}",
            Consts.KEY_EXPECT_POD_LABELS_COMMON,
            expectLabelsCommonStr
        );
        serviceDependModel.setExpectLabelsCommon(expectLabelsCommonStr);
    }

    /**
     * 使用环境变量填充服务依赖模型中的期望Pod服务标签取值
     *
     * @param serviceDependModel 服务依赖模型
     */
    private void fillExpectLabelsServiceStrWithEnvVariableIfAbsent(ServiceDependModel serviceDependModel) {
        String expectLabelsServiceStr = serviceDependModel.getExpectLabelsService();
        if (StringUtils.isNotBlank(expectLabelsServiceStr)) {
            return;
        }
        expectLabelsServiceStr = System.getenv(Consts.KEY_EXPECT_POD_LABELS_SERVICE);
        log.info(
            "Commandline param [-ls,--expect-pod-labels-service] is null or blank, use env variable {}={}",
            Consts.KEY_EXPECT_POD_LABELS_SERVICE,
            expectLabelsServiceStr
        );
        serviceDependModel.setExpectLabelsService(expectLabelsServiceStr);
    }

    /**
     * 使用环境变量填充服务依赖模型中的外部依赖检查开启状态
     *
     * @param serviceDependModel 服务依赖模型
     */
    private void fillExternalDependencyCheckEnabledWithEnvVariableIfAbsent(ServiceDependModel serviceDependModel) {
        Boolean externalDependencyCheckEnabled = serviceDependModel.getExternalDependencyCheckEnabled();
        if (externalDependencyCheckEnabled!=null) {
            return;
        }
        String enabledStr = System.getenv(Consts.KEY_EXTERNAL_DEPENDENCY_CHECK_ENABLED);
        log.info(
            "Commandline param [--external-dependency-check-enabled] is null or blank, use env variable {}={}",
            Consts.KEY_EXTERNAL_DEPENDENCY_CHECK_ENABLED,
            enabledStr
        );
        boolean enabled = StringUtils.isNotBlank(enabledStr) && Boolean.parseBoolean(enabledStr.trim());
        if (!enabled) {
            log.info("Service external dependency check disabled");
        }
        serviceDependModel.setExternalDependencyCheckEnabled(enabled);
    }

    /**
     * 使用环境变量填充服务依赖模型中的外部依赖检查URL
     *
     * @param serviceDependModel 服务依赖模型
     */
    private void fillExternalDependencyCheckUrlWithEnvVariableIfAbsent(ServiceDependModel serviceDependModel) {
        String externalDependencyCheckUrl = serviceDependModel.getExternalDependencyCheckUrl();
        if (StringUtils.isNotBlank(externalDependencyCheckUrl)) {
            return;
        }
        externalDependencyCheckUrl = System.getenv(Consts.KEY_EXTERNAL_DEPENDENCY_CHECK_URL);
        log.info(
            "Commandline param [--external-dependency-check-url] is null or blank, use env variable {}={}",
            Consts.KEY_EXTERNAL_DEPENDENCY_CHECK_URL,
            externalDependencyCheckUrl
        );
        serviceDependModel.setExternalDependencyCheckUrl(externalDependencyCheckUrl);
    }

}
