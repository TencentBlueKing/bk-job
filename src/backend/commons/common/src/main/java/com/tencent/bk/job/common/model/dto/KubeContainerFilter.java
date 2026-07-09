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

package com.tencent.bk.job.common.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.tencent.bk.job.common.annotation.PersistenceObject;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 执行目标-容器选择过滤器。
 * <p>
 * 是后端从 WebResource 之后唯一的「容器过滤器流转 DTO」，job-manage / job-crontab / job-execute 共享；
 * 既参与运行时 ExecuteTargetDTO.containerFilters 流转，也参与 TaskTargetDTO / ServerDTO 的 JSON 持久化。
 * <p>
 * 拓扑维度按入口来源分两套字段：
 * <ul>
 *   <li>OpenAPI：{@link #clusterFilter}/{@link #namespaceFilter}/{@link #workloadFilter}
 *       —— 字符串 UID/名称形态，不含 CMDB 内部 ID，沿用既有契约不动</li>
 *   <li>Web 动态条件入口：{@link #kubeTopoList} —— 每条是一个精确的 cluster→namespace→workload 路径
 *       （cluster 必填、其余可选），仅含 CMDB 内部 {@code id}（workload 还有 {@code kind}），ID 用于 CMDB 查询；
 *       展示名不落库，详情页回显时前端拿 id 走别的接口查名</li>
 * </ul>
 * 同一实例两套字段不应同时出现：openapi 转换器只填前者、Web 转换器只填后者。
 */
@Data
@PersistenceObject
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class KubeContainerFilter implements Cloneable {

    /**
     * 集群过滤器（openapi 入口，字符串 UID）
     */
    private KubeClusterFilter clusterFilter;

    /**
     * namespace 过滤器（openapi 入口，字符串名称）
     */
    private KubeNamespaceFilter namespaceFilter;

    /**
     * workload 过滤器（openapi 入口，{kind, 名称列表}）
     */
    private KubeWorkloadFilter workloadFilter;

    /**
     * pod 属性过滤器
     */
    private KubePodFilter podFilter;

    /**
     * 容器属性过滤器
     */
    private KubeContainerPropFilter containerPropFilter;

    /**
     * 标识一个没有设置任何条件的过滤器
     */
    private boolean emptyFilter;

    /**
     * 是否从过滤结果集中选择任意一个容器作为执行对象（只有一个容器会被执行）
     */
    private boolean fetchAnyOneContainer;

    /**
     * 拓扑路径列表（Web 入口；每条是一个 cluster→namespace→workload 路径，共用 propConditions）
     */
    private List<KubeTopoDTO> kubeTopoList;

    /**
     * 字段级 AND 条件，承载动态条件过滤器（field/operator/value 三元组）。
     * 与 cluster/namespace/workload 同级共存；为空时回退到拓扑过滤行为。
     */
    private List<KubePropCondition> propConditions;

    /**
     * 过滤之后的容器列表（运行时已解析结果；保存模板/方案/定时时为 null，由 NON_EMPTY 序列化跳过）
     */
    private List<Container> containers;

    @Override
    public KubeContainerFilter clone() {
        KubeContainerFilter clone = new KubeContainerFilter();
        if (clusterFilter != null) {
            clone.setClusterFilter(clusterFilter.clone());
        }
        if (namespaceFilter != null) {
            clone.setNamespaceFilter(namespaceFilter.clone());
        }
        if (workloadFilter != null) {
            clone.setWorkloadFilter(workloadFilter.clone());
        }
        if (CollectionUtils.isNotEmpty(kubeTopoList)) {
            List<KubeTopoDTO> cloneTopos = new ArrayList<>(kubeTopoList.size());
            kubeTopoList.forEach(topo -> cloneTopos.add(topo.clone()));
            clone.setKubeTopoList(cloneTopos);
        }
        if (podFilter != null) {
            clone.setPodFilter(podFilter.clone());
        }
        if (containerPropFilter != null) {
            clone.setContainerPropFilter(containerPropFilter.clone());
        }
        if (CollectionUtils.isNotEmpty(propConditions)) {
            List<KubePropCondition> clonePropConditions = new ArrayList<>(propConditions.size());
            propConditions.forEach(condition -> clonePropConditions.add(condition.clone()));
            clone.setPropConditions(clonePropConditions);
        }
        clone.setEmptyFilter(emptyFilter);
        clone.setFetchAnyOneContainer(fetchAnyOneContainer);
        if (CollectionUtils.isNotEmpty(containers)) {
            List<Container> cloneContainers = new ArrayList<>(containers.size());
            containers.forEach(container -> cloneContainers.add(container.clone()));
            clone.setContainers(cloneContainers);
        }

        return clone;
    }

    /**
     * 是否包含容器拓扑(cluster/namespace/workload)相关的过滤条件
     */
    @JsonIgnore
    public boolean hasKubeNodeFilter() {
        if (isEmptyFilter()) {
            return false;
        }
        return clusterFilter != null || namespaceFilter != null || workloadFilter != null
            || CollectionUtils.isNotEmpty(kubeTopoList);
    }

    /**
     * 是否携带 Web 入口形态的拓扑路径（任一非空）。
     */
    @JsonIgnore
    public boolean hasKubeTopoObjects() {
        return CollectionUtils.isNotEmpty(kubeTopoList);
    }

    /**
     * 是否包含字段级 AND 条件（propConditions）。
     */
    @JsonIgnore
    public boolean hasPropConditions() {
        return CollectionUtils.isNotEmpty(propConditions);
    }
}
