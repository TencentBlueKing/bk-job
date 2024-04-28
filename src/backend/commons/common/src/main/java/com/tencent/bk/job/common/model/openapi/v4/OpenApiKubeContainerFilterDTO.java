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

package com.tencent.bk.job.common.model.openapi.v4;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.validation.KubeContainerFilterNotEmpty;
import lombok.Data;

/**
 * 执行目标-容器选择过滤器
 */
@Data
@KubeContainerFilterNotEmpty
public class OpenApiKubeContainerFilterDTO {

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

    /**
     * 标识一个没有设置任何条件的过滤器；默认值为 false。如果设置为 true, 将忽略其他的条件（kube_cluster_filter/kube_namespace_filter
     * /kube_workload_filter/kube_pod_filter/kube_container_prop_filter)，返回业务下的所有容器
     */
    @JsonProperty("is_empty_filter")
    private boolean emptyFilter;

    /**
     * 是否从过滤结果集中选择任意一个容器作为执行对象（只有一个容器会被执行）
     */
    @JsonProperty("fetch_any_one_container")
    private boolean fetchAnyOneContainer;

}
