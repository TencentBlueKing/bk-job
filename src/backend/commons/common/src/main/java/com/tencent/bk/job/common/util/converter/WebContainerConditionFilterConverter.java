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

package com.tencent.bk.job.common.util.converter;

import com.tencent.bk.job.common.model.dto.KubeClusterObjectDTO;
import com.tencent.bk.job.common.model.dto.KubeContainerFilter;
import com.tencent.bk.job.common.model.dto.KubeNamespaceObjectDTO;
import com.tencent.bk.job.common.model.dto.KubePropCondition;
import com.tencent.bk.job.common.model.dto.KubeWorkloadObjectDTO;
import com.tencent.bk.job.common.model.vo.WebContainerConditionFilter;
import com.tencent.bk.job.common.model.vo.WebKubeClusterObject;
import com.tencent.bk.job.common.model.vo.WebKubeNamespaceObject;
import com.tencent.bk.job.common.model.vo.WebKubeWorkloadObject;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Web 入参 ↔ 内部流转 DTO 转换器。
 * <p>
 * Web 入参的拓扑对象（{@code WebKubeXxxObject}）中的 ID 会被搬到内部 {@link KubeContainerFilter}
 * 的 Web 侧字段（{@code clusters/namespaces/workloads}）；展示名不落库，反向回显时也不透出。
 * 不触碰 v4 OpenAPI 入口的旧字符串 sub-filter。
 * Web 不暴露 {@code emptyFilter / fetchAnyOneContainer} 两个内部开关，转换时固定置 false。
 */
public final class WebContainerConditionFilterConverter {

    private WebContainerConditionFilterConverter() {
    }

    public static KubeContainerFilter toKubeContainerFilter(WebContainerConditionFilter web) {
        if (web == null) {
            return null;
        }
        KubeContainerFilter kube = new KubeContainerFilter();
        kube.setClusterNodes(toClusterObjects(web.getClusterList()));
        kube.setNamespaceNodes(toNamespaceObjects(web.getNamespaceList()));
        kube.setWorkloadNodes(toWorkloadObjects(web.getWorkloadList()));
        kube.setPropConditions(clonePropConditions(web.getPropConditions()));
        kube.setEmptyFilter(false);
        kube.setFetchAnyOneContainer(false);
        return kube;
    }

    private static List<KubeClusterObjectDTO> toClusterObjects(List<WebKubeClusterObject> webList) {
        if (webList == null) {
            return null;
        }
        return webList.stream()
            .map(web -> web == null ? null : new KubeClusterObjectDTO(web.getId()))
            .collect(Collectors.toList());
    }

    private static List<KubeNamespaceObjectDTO> toNamespaceObjects(List<WebKubeNamespaceObject> webList) {
        if (webList == null) {
            return null;
        }
        return webList.stream()
            .map(web -> web == null ? null : new KubeNamespaceObjectDTO(web.getId()))
            .collect(Collectors.toList());
    }

    private static List<KubeWorkloadObjectDTO> toWorkloadObjects(List<WebKubeWorkloadObject> webList) {
        if (webList == null) {
            return null;
        }
        return webList.stream()
            .map(web -> web == null ? null : new KubeWorkloadObjectDTO(web.getKind(), web.getId()))
            .collect(Collectors.toList());
    }

    private static List<KubePropCondition> clonePropConditions(List<KubePropCondition> propConditions) {
        if (propConditions == null) {
            return null;
        }
        return propConditions.stream()
            .map(condition -> condition == null ? null : condition.clone())
            .collect(Collectors.toList());
    }

    public static List<KubeContainerFilter> toKubeContainerFilters(List<WebContainerConditionFilter> webList) {
        if (webList == null) {
            return null;
        }
        return webList.stream()
            .map(WebContainerConditionFilterConverter::toKubeContainerFilter)
            .collect(Collectors.toList());
    }

    /**
     * 反向：KubeContainerFilter → Web 入参 DTO，用于回显模板/方案/定时任务详情。
     * 只透出 Web 入口形态的拓扑对象；v4 形态字段（clusterFilter 等）不外露给 Web，避免回显错位。
     */
    public static WebContainerConditionFilter fromKubeContainerFilter(KubeContainerFilter kube) {
        if (kube == null) {
            return null;
        }
        WebContainerConditionFilter web = new WebContainerConditionFilter();
        web.setClusterList(fromClusterObjects(kube.getClusterNodes()));
        web.setNamespaceList(fromNamespaceObjects(kube.getNamespaceNodes()));
        web.setWorkloadList(fromWorkloadObjects(kube.getWorkloadNodes()));
        web.setPropConditions(clonePropConditions(kube.getPropConditions()));
        return web;
    }

    private static List<WebKubeClusterObject> fromClusterObjects(List<KubeClusterObjectDTO> internalList) {
        if (internalList == null) {
            return null;
        }
        return internalList.stream()
            .map(internal -> {
                if (internal == null) {
                    return null;
                }
                WebKubeClusterObject web = new WebKubeClusterObject();
                web.setId(internal.getId());
                return web;
            })
            .collect(Collectors.toList());
    }

    private static List<WebKubeNamespaceObject> fromNamespaceObjects(List<KubeNamespaceObjectDTO> internalList) {
        if (internalList == null) {
            return null;
        }
        return internalList.stream()
            .map(internal -> {
                if (internal == null) {
                    return null;
                }
                WebKubeNamespaceObject web = new WebKubeNamespaceObject();
                web.setId(internal.getId());
                return web;
            })
            .collect(Collectors.toList());
    }

    private static List<WebKubeWorkloadObject> fromWorkloadObjects(List<KubeWorkloadObjectDTO> internalList) {
        if (internalList == null) {
            return null;
        }
        return internalList.stream()
            .map(internal -> {
                if (internal == null) {
                    return null;
                }
                WebKubeWorkloadObject web = new WebKubeWorkloadObject();
                web.setKind(internal.getKind());
                web.setId(internal.getId());
                return web;
            })
            .collect(Collectors.toList());
    }

    public static List<WebContainerConditionFilter> fromKubeContainerFilters(List<KubeContainerFilter> kubeList) {
        if (kubeList == null) {
            return null;
        }
        return kubeList.stream()
            .map(WebContainerConditionFilterConverter::fromKubeContainerFilter)
            .collect(Collectors.toList());
    }
}
