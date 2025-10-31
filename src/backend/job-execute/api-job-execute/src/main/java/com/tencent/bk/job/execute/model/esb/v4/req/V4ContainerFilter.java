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

package com.tencent.bk.job.execute.model.esb.v4.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.model.openapi.v4.OpenApiKubeClusterFilterDTO;
import com.tencent.bk.job.common.model.openapi.v4.OpenApiKubeContainerPropFilterDTO;
import com.tencent.bk.job.common.model.openapi.v4.OpenApiKubeNamespaceFilterDTO;
import com.tencent.bk.job.common.model.openapi.v4.OpenApiKubePodFilterDTO;
import com.tencent.bk.job.common.model.openapi.v4.OpenApiKubeWorkloadFilterDTO;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

@Data
public class V4ContainerFilter {

    /**
     * 集群过滤器
     */
    @JsonProperty("kube_cluster_filter")
    private OpenApiKubeClusterFilterDTO clusterFilter;

    /**
     * namespace 过滤器
     */
    @JsonProperty("kube_namespace_filter")
    private OpenApiKubeNamespaceFilterDTO namespaceFilter;

    /**
     * workload 过滤器
     */
    @JsonProperty("kube_workload_filter")
    private OpenApiKubeWorkloadFilterDTO workloadFilter;

    /**
     * pod 属性过滤器
     */
    @JsonProperty("kube_pod_filter")
    private OpenApiKubePodFilterDTO podFilter;

    /**
     * 容器属性过滤器
     */
    @JsonProperty("kube_container_prop_filter")
    private OpenApiKubeContainerPropFilterDTO containerPropFilter;

    public boolean isAllCluster() {
        return clusterFilter != null
            && (namespaceFilter == null || CollectionUtils.isEmpty(namespaceFilter.getNamespaces()))
            && (workloadFilter == null || CollectionUtils.isEmpty(workloadFilter.getWorkloadNames()))
            && (podFilter == null || CollectionUtils.isEmpty(podFilter.getPodNames()))
            && (podFilter == null || podFilter.isEmpty())
            && (containerPropFilter == null || CollectionUtils.isEmpty(containerPropFilter.getContainerNames()));

    }
}
