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
import com.tencent.bk.job.common.model.dto.KubePropCondition;
import com.tencent.bk.job.common.model.dto.KubeTopoDTO;
import com.tencent.bk.job.common.model.vo.WebContainerConditionFilter;
import com.tencent.bk.job.common.model.vo.WebKubeClusterObject;
import com.tencent.bk.job.common.model.vo.WebKubeNamespaceObject;
import com.tencent.bk.job.common.model.vo.WebKubeTopo;
import com.tencent.bk.job.common.model.vo.WebKubeWorkloadObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 校验 Web ↔ 内部 双向转换：Web 每条拓扑路径只把 ID 逐条映射到内部 KubeContainerFilter 的
 * kubeTopoList；name 不落库、不回显；不污染 v4 sub-filter；Web 不暴露 emptyFilter/fetchAnyOneContainer 两个内部开关。
 */
@DisplayName("WebContainerConditionFilterConverter: Web 入参 ↔ 内部 DTO")
class WebContainerConditionFilterConverterTest {

    @Test
    @DisplayName("toKubeContainerFilter: null 入参 → null")
    void toKubeFilterNullInput() {
        assertThat(WebContainerConditionFilterConverter.toKubeContainerFilter(null)).isNull();
    }

    @Test
    @DisplayName("toKubeContainerFilter: 完整 Web 入参 → 内部 kubeTopoList；v4 字段保持 null；开关固定 false")
    void toKubeFilterFullMapping() {
        WebContainerConditionFilter web = buildFullWebFilter();

        KubeContainerFilter kube = WebContainerConditionFilterConverter.toKubeContainerFilter(web);

        assertThat(kube).isNotNull();
        assertThat(kube.getKubeTopoList()).hasSize(2);
        KubeTopoDTO topo0 = kube.getKubeTopoList().get(0);
        assertThat(topo0.getCluster().getId()).isEqualTo(1000L);
        assertThat(topo0.getNamespace().getId()).isEqualTo(10000L);
        assertThat(topo0.getWorkload().getKind()).isEqualTo("deployment");
        assertThat(topo0.getWorkload().getId()).isEqualTo(20000L);
        KubeTopoDTO topo1 = kube.getKubeTopoList().get(1);
        assertThat(topo1.getCluster().getId()).isEqualTo(1001L);
        assertThat(topo1.getNamespace().getId()).isEqualTo(1L);
        assertThat(topo1.getWorkload().getKind()).isEqualTo("daemonSet");
        assertThat(kube.getPropConditions()).hasSize(2);
        assertThat(kube.getPropConditions().get(0).getField()).isEqualTo("container_container_uid");
        assertThat(kube.getPropConditions().get(1).getValue()).isEqualTo("pod-a");
        // v4 sub-filter 不被污染
        assertThat(kube.getClusterFilter()).isNull();
        assertThat(kube.getNamespaceFilter()).isNull();
        assertThat(kube.getWorkloadFilter()).isNull();
        // Web 不暴露这两个内部开关
        assertThat(kube.isEmptyFilter()).isFalse();
        assertThat(kube.isFetchAnyOneContainer()).isFalse();
    }

    @Test
    @DisplayName("toKubeContainerFilter: 单条 topo 仅 cluster 时 namespace/workload/propConditions 保持 null")
    void toKubeFilterMinimal() {
        WebContainerConditionFilter web = new WebContainerConditionFilter();
        web.setKubeTopoList(Collections.singletonList(topo(cluster(1000L), null, null)));

        KubeContainerFilter kube = WebContainerConditionFilterConverter.toKubeContainerFilter(web);

        assertThat(kube.getKubeTopoList()).hasSize(1);
        KubeTopoDTO topo = kube.getKubeTopoList().get(0);
        assertThat(topo.getCluster().getId()).isEqualTo(1000L);
        assertThat(topo.getNamespace()).isNull();
        assertThat(topo.getWorkload()).isNull();
        assertThat(kube.getPropConditions()).isNull();
    }

    @Test
    @DisplayName("toKubeContainerFilters: null/空 列表对称转换")
    void toKubeFiltersList() {
        assertThat(WebContainerConditionFilterConverter.toKubeContainerFilters(null)).isNull();
        assertThat(WebContainerConditionFilterConverter.toKubeContainerFilters(Collections.emptyList())).isEmpty();

        List<KubeContainerFilter> kubes = WebContainerConditionFilterConverter.toKubeContainerFilters(
            Arrays.asList(buildFullWebFilter(), buildFullWebFilter()));
        assertThat(kubes).hasSize(2);
    }

    @Test
    @DisplayName("fromKubeContainerFilter: 反向回显 kubeTopoList；只带 id 不带 name；不外露 v4 字段；不外露内部开关")
    void fromKubeFilterReverse() {
        KubeContainerFilter kube = WebContainerConditionFilterConverter.toKubeContainerFilter(buildFullWebFilter());
        // 模拟运行时把这两个开关置 true（v4 路径可能写过），反向回显不应回到 Web
        kube.setFetchAnyOneContainer(true);
        kube.setEmptyFilter(true);

        WebContainerConditionFilter back = WebContainerConditionFilterConverter.fromKubeContainerFilter(kube);

        assertThat(back).isNotNull();
        assertThat(back.getKubeTopoList()).hasSize(2);
        WebKubeTopo topo0 = back.getKubeTopoList().get(0);
        assertThat(topo0.getCluster().getId()).isEqualTo(1000L);
        assertThat(topo0.getNamespace().getId()).isEqualTo(10000L);
        assertThat(topo0.getWorkload().getKind()).isEqualTo("deployment");
        assertThat(back.getPropConditions()).hasSize(2);
    }

    @Test
    @DisplayName("propConditions 的 List value 做了防御性拷贝（KubePropCondition.clone 兜底）")
    void fromKubeFilterDefensiveCopyOnPropConditionValue() {
        KubeContainerFilter kube = new KubeContainerFilter();
        kube.setKubeTopoList(Collections.singletonList(
            new KubeTopoDTO(new KubeClusterObjectDTO(1000L), null, null)));
        kube.setPropConditions(Collections.singletonList(
            new KubePropCondition("container_container_uid", "in",
                new ArrayList<>(Arrays.asList("docker://nginx", "docker://redis")))));

        WebContainerConditionFilter web = WebContainerConditionFilterConverter.fromKubeContainerFilter(kube);

        @SuppressWarnings("unchecked")
        List<String> backValue = (List<String>) web.getPropConditions().get(0).getValue();
        backValue.clear();
        @SuppressWarnings("unchecked")
        List<String> sourceValue = (List<String>) kube.getPropConditions().get(0).getValue();
        assertThat(sourceValue).containsExactly("docker://nginx", "docker://redis");
    }

    private static WebContainerConditionFilter buildFullWebFilter() {
        WebContainerConditionFilter web = new WebContainerConditionFilter();
        web.setKubeTopoList(Arrays.asList(
            topo(cluster(1000L), namespace(10000L), workload("deployment", 20000L)),
            topo(cluster(1001L), namespace(1L), workload("daemonSet", 20001L))
        ));
        web.setPropConditions(Arrays.asList(
            new KubePropCondition("container_container_uid", "contains", "docker://abcdefg"),
            new KubePropCondition("pod_name", "equal", "pod-a")
        ));
        return web;
    }

    private static WebKubeTopo topo(WebKubeClusterObject cluster,
                                    WebKubeNamespaceObject namespace,
                                    WebKubeWorkloadObject workload) {
        WebKubeTopo t = new WebKubeTopo();
        t.setCluster(cluster);
        t.setNamespace(namespace);
        t.setWorkload(workload);
        return t;
    }

    private static WebKubeClusterObject cluster(Long id) {
        WebKubeClusterObject c = new WebKubeClusterObject();
        c.setId(id);
        return c;
    }

    private static WebKubeNamespaceObject namespace(Long id) {
        WebKubeNamespaceObject ns = new WebKubeNamespaceObject();
        ns.setId(id);
        return ns;
    }

    private static WebKubeWorkloadObject workload(String kind, Long id) {
        WebKubeWorkloadObject w = new WebKubeWorkloadObject();
        w.setKind(kind);
        w.setId(id);
        return w;
    }
}
