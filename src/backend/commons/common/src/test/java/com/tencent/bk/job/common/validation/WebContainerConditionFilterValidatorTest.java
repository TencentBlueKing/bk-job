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

package com.tencent.bk.job.common.validation;

import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.dto.KubePropCondition;
import com.tencent.bk.job.common.model.vo.WebContainerConditionFilter;
import com.tencent.bk.job.common.model.vo.WebKubeClusterObject;
import com.tencent.bk.job.common.model.vo.WebKubeNamespaceObject;
import com.tencent.bk.job.common.model.vo.WebKubeTopo;
import com.tencent.bk.job.common.model.vo.WebKubeWorkloadObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 校验 {@link WebContainerConditionFilterValidator}：
 * <ul>
 *   <li>kubeTopoList 必填非空、每条 topo 的 cluster 必填（程序式兜底，Bean Validation 缺位时也能拦）</li>
 *   <li>propConditions 逐条委托给 {@link KubePropConditionValidator}</li>
 *   <li>namespace/workload 字段级 id/name 非空由 Bean Validation 兜底，本类不重复校验</li>
 * </ul>
 */
@DisplayName("WebContainerConditionFilterValidator: 程序式校验入口")
class WebContainerConditionFilterValidatorTest {

    @Test
    @DisplayName("入参 null 视为非法")
    void nullFilterRejected() {
        assertThatThrownBy(() -> WebContainerConditionFilterValidator.validate((WebContainerConditionFilter) null))
            .isInstanceOf(InvalidParamException.class);
    }

    @Test
    @DisplayName("kubeTopoList 缺失 → 非法")
    void kubeTopoListMissing() {
        WebContainerConditionFilter filter = new WebContainerConditionFilter();
        assertThatThrownBy(() -> WebContainerConditionFilterValidator.validate(filter))
            .isInstanceOf(InvalidParamException.class);
    }

    @Test
    @DisplayName("kubeTopoList 为空集合 → 非法")
    void kubeTopoListEmpty() {
        WebContainerConditionFilter filter = new WebContainerConditionFilter();
        filter.setKubeTopoList(Collections.emptyList());
        assertThatThrownBy(() -> WebContainerConditionFilterValidator.validate(filter))
            .isInstanceOf(InvalidParamException.class);
    }

    @Test
    @DisplayName("某条 topo 缺 cluster → 非法")
    void topoClusterMissing() {
        WebContainerConditionFilter filter = new WebContainerConditionFilter();
        filter.setKubeTopoList(Collections.singletonList(topo(null, null, null)));
        assertThatThrownBy(() -> WebContainerConditionFilterValidator.validate(filter))
            .isInstanceOf(InvalidParamException.class);
    }

    @Test
    @DisplayName("仅 cluster 必填的最小合法 filter 通过")
    void clusterOnlyHappyPath() {
        WebContainerConditionFilter filter = buildWithCluster();
        assertThatCode(() -> WebContainerConditionFilterValidator.validate(filter)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("多条 topo 混合不同 workload kind → 通过")
    void mixedWorkloadKindsOk() {
        WebContainerConditionFilter filter = new WebContainerConditionFilter();
        filter.setKubeTopoList(Arrays.asList(
            topo(cluster(1000L), null, workload("deployment", 20000L)),
            topo(cluster(1001L), null, workload("daemonSet", 20001L))
        ));
        assertThatCode(() -> WebContainerConditionFilterValidator.validate(filter)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("namespace 可选：topo 仅传 namespace 不传 workload 也通过")
    void namespaceOptional() {
        WebContainerConditionFilter filter = new WebContainerConditionFilter();
        filter.setKubeTopoList(Collections.singletonList(
            topo(cluster(1000L), namespace(10000L), null)));
        assertThatCode(() -> WebContainerConditionFilterValidator.validate(filter)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("propConditions 字段非法时委托给 KubePropConditionValidator 报错")
    void propConditionsDelegated() {
        WebContainerConditionFilter filter = buildWithCluster();
        filter.setPropConditions(Collections.singletonList(
            new KubePropCondition("unknown_field", "equal", "x")));
        assertThatThrownBy(() -> WebContainerConditionFilterValidator.validate(filter))
            .isInstanceOf(InvalidParamException.class);
    }

    @Test
    @DisplayName("propConditions 合法时通过")
    void propConditionsOk() {
        WebContainerConditionFilter filter = buildWithCluster();
        filter.setPropConditions(Collections.singletonList(
            new KubePropCondition("pod_labels", "equal", "name=label_value1")));
        assertThatCode(() -> WebContainerConditionFilterValidator.validate(filter)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("列表入口：null / 空列表 no-op")
    void listEntryNoOp() {
        assertThatCode(() -> WebContainerConditionFilterValidator.validate(
            (List<WebContainerConditionFilter>) null)).doesNotThrowAnyException();
        assertThatCode(() -> WebContainerConditionFilterValidator.validate(
            Collections.emptyList())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("列表入口：逐个校验，遇到非法立即抛")
    void listEntryShortCircuits() {
        WebContainerConditionFilter bad = new WebContainerConditionFilter(); // 没有 topo
        assertThatThrownBy(() -> WebContainerConditionFilterValidator.validate(Collections.singletonList(bad)))
            .isInstanceOf(InvalidParamException.class);
    }

    private static WebContainerConditionFilter buildWithCluster() {
        WebContainerConditionFilter filter = new WebContainerConditionFilter();
        filter.setKubeTopoList(Collections.singletonList(topo(cluster(1000L), null, null)));
        return filter;
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
