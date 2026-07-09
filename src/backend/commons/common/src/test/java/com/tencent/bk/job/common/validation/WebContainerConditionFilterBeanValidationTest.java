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

import com.tencent.bk.job.common.model.vo.WebContainerConditionFilter;
import com.tencent.bk.job.common.model.vo.WebKubeClusterObject;
import com.tencent.bk.job.common.model.vo.WebKubeNamespaceObject;
import com.tencent.bk.job.common.model.vo.WebKubeTopo;
import com.tencent.bk.job.common.model.vo.WebKubeWorkloadObject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 校验 Bean Validation 在 {@link WebContainerConditionFilter} 及拓扑 Object 上的约束触发。
 * <p>
 * 使用 {@code ParameterMessageInterpolator} 避免测试环境依赖 Jakarta EL 实现。
 */
@DisplayName("WebContainerConditionFilter: Bean Validation")
class WebContainerConditionFilterBeanValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;
    private static Locale originalLocale;

    @BeforeAll
    static void setup() {
        originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.ROOT);
        factory = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
        Locale.setDefault(originalLocale);
    }

    @Test
    @DisplayName("kubeTopoList = null → 触发 @NotEmpty")
    void kubeTopoListNullViolation() {
        WebContainerConditionFilter filter = new WebContainerConditionFilter();
        Set<ConstraintViolation<WebContainerConditionFilter>> violations = validator.validate(filter);
        assertThat(violations).anySatisfy(v ->
            assertThat(v.getPropertyPath().toString()).isEqualTo("kubeTopoList"));
    }

    @Test
    @DisplayName("kubeTopoList 为空集合 → 触发 @NotEmpty")
    void kubeTopoListEmptyViolation() {
        WebContainerConditionFilter filter = new WebContainerConditionFilter();
        filter.setKubeTopoList(Collections.emptyList());
        Set<ConstraintViolation<WebContainerConditionFilter>> violations = validator.validate(filter);
        assertThat(violations).anySatisfy(v ->
            assertThat(v.getPropertyPath().toString()).isEqualTo("kubeTopoList"));
    }

    @Test
    @DisplayName("topo 缺 cluster → @Valid 级联触发 WebKubeTopo.cluster @NotNull")
    void topoClusterMissingViolation() {
        WebContainerConditionFilter filter = new WebContainerConditionFilter();
        filter.setKubeTopoList(Collections.singletonList(new WebKubeTopo())); // cluster=null
        Set<ConstraintViolation<WebContainerConditionFilter>> violations = validator.validate(filter);
        assertThat(violations).anySatisfy(v ->
            assertThat(v.getPropertyPath().toString()).isEqualTo("kubeTopoList[0].cluster"));
    }

    @Test
    @DisplayName("topo.cluster 元素 id 缺失 → @Valid 两层级联触发 @NotNull")
    void clusterObjectFieldLevelViolations() {
        WebContainerConditionFilter filter = new WebContainerConditionFilter();
        filter.setKubeTopoList(Collections.singletonList(topo(new WebKubeClusterObject(), null, null)));
        Set<ConstraintViolation<WebContainerConditionFilter>> violations = validator.validate(filter);
        assertThat(violations).anySatisfy(v ->
            assertThat(v.getPropertyPath().toString()).isEqualTo("kubeTopoList[0].cluster.id"));
    }

    @Test
    @DisplayName("topo.workload 元素 kind/id 缺失 → @Valid 级联触发字段级约束")
    void workloadObjectFieldLevelViolations() {
        WebContainerConditionFilter filter = new WebContainerConditionFilter();
        filter.setKubeTopoList(Collections.singletonList(
            topo(cluster(1000L), null, new WebKubeWorkloadObject())));
        Set<ConstraintViolation<WebContainerConditionFilter>> violations = validator.validate(filter);
        assertThat(violations).anySatisfy(v ->
            assertThat(v.getPropertyPath().toString()).isEqualTo("kubeTopoList[0].workload.kind"));
        assertThat(violations).anySatisfy(v ->
            assertThat(v.getPropertyPath().toString()).isEqualTo("kubeTopoList[0].workload.id"));
    }

    @Test
    @DisplayName("完整合法 filter（多条 topo、多 kind workload）无任何 violation")
    void fullyValidFilterPasses() {
        WebContainerConditionFilter filter = new WebContainerConditionFilter();
        filter.setKubeTopoList(Arrays.asList(
            topo(cluster(1000L), namespace(10000L), workload("deployment", 20000L)),
            topo(cluster(1001L), null, workload("daemonSet", 20001L))
        ));
        Set<ConstraintViolation<WebContainerConditionFilter>> violations = validator.validate(filter);
        assertThat(violations).isEmpty();
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
