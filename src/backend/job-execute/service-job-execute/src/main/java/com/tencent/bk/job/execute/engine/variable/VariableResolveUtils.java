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

package com.tencent.bk.job.execute.engine.variable;

import com.tencent.bk.job.common.model.dto.Container;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * 变量解析工具类
 */
public class VariableResolveUtils {
    /**
     * 格式化主机值
     *
     * @param hosts 主机列表
     * @return 主机值
     */
    public static String formatHosts(Collection<HostDTO> hosts) {
        if (CollectionUtils.isEmpty(hosts)) {
            return null;
        }
        StringJoiner joiner = new StringJoiner(",");
        hosts.forEach(host -> joiner.add(host.getBkCloudId() + ":" + host.getPrimaryIp()));
        return joiner.toString();
    }

    /**
     * 格式化容器值
     * 格式：clusterName:namespace/podName/containerName
     *
     * @param containers 容器列表
     * @return 格式化后的容器值
     */
    public static String formatContainers(Collection<Container> containers) {
        if (CollectionUtils.isEmpty(containers)) {
            return null;
        }
        StringJoiner joiner = new StringJoiner(",");
        containers.forEach(container -> {
            String formatted = StringUtils.defaultString(container.getClusterName(), "") + ":"
                + StringUtils.defaultString(container.getNamespace(), "") + "/"
                + StringUtils.defaultString(container.getPodName(), "") + "/"
                + StringUtils.defaultString(container.getName(), "");
            joiner.add(formatted);
        });
        return joiner.toString();
    }

    /**
     * 格式化执行对象（主机+容器）
     *
     * @param executeTarget 执行目标
     * @return 格式化后的执行对象值
     */
    public static String formatExecuteObjects(ExecuteTargetDTO executeTarget) {
        if (executeTarget == null) {
            return "";
        }
        return formatExecuteObjects(executeTarget.getExecuteObjectsCompatibly());
    }

    /**
     * 格式化执行对象列表（主机+容器）
     *
     * @param executeObjects 执行对象列表
     * @return 格式化后的执行对象值
     */
    public static String formatExecuteObjects(List<ExecuteObject> executeObjects) {
        if (CollectionUtils.isEmpty(executeObjects)) {
            return null;
        }

        List<String> parts = new ArrayList<>();

        // 格式化主机
        List<HostDTO> hosts = executeObjects.stream()
            .filter(ExecuteObject::isHostExecuteObject)
            .map(ExecuteObject::getHost)
            .collect(Collectors.toList());
        String hostsPart = formatHosts(hosts);
        if (StringUtils.isNotEmpty(hostsPart)) {
            parts.add(hostsPart);
        }

        // 格式化容器
        List<Container> containers = executeObjects.stream()
            .filter(ExecuteObject::isContainerExecuteObject)
            .map(ExecuteObject::getContainer)
            .collect(Collectors.toList());
        String containersPart = formatContainers(containers);
        if (StringUtils.isNotEmpty(containersPart)) {
            parts.add(containersPart);
        }

        if (parts.isEmpty()) {
            return null;
        }
        return String.join(",", parts);
    }
}
