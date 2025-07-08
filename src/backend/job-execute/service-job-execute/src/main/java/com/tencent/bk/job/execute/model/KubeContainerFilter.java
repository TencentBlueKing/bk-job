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

package com.tencent.bk.job.execute.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tencent.bk.job.common.annotation.PersistenceObject;
import com.tencent.bk.job.common.model.dto.Container;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 执行目标-容器选择过滤器
 */
@Data
@PersistenceObject
public class KubeContainerFilter implements Cloneable {

    /**
     * 集群过滤器
     */
    private KubeClusterFilter clusterFilter;

    /**
     * namespace 过滤器
     */
    private KubeNamespaceFilter namespaceFilter;

    /**
     * workload 过滤器
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
     * 过滤之后的容器列表
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
        if (podFilter != null) {
            clone.setPodFilter(podFilter.clone());
        }
        if (containerPropFilter != null) {
            clone.setContainerPropFilter(containerPropFilter.clone());
        }
        clone.setFetchAnyOneContainer(fetchAnyOneContainer);
        if (CollectionUtils.isNotEmpty(containers)) {
            List<Container> cloneContainers = new ArrayList<>(containers.size());
            containers.forEach(container -> cloneContainers.add(container.clone()));
            clone.setContainers(cloneContainers);
        }

        return clone;
    }

    /**
     * 是否包含容器拓扑(cluster/namespace/workload)相关的 filter
     */
    @JsonIgnore
    public boolean hasKubeNodeFilter() {
        return !isEmptyFilter() && (clusterFilter != null || namespaceFilter != null || workloadFilter != null);
    }
}
