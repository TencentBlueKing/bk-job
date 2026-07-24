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

package com.tencent.bk.job.common.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 动态条件过滤器-单条拓扑路径（Web 层入参契约面）。
 * <p>
 * 一条 topo 表示一个 cluster→namespace→workloads 路径：cluster 必填，namespace 可选，workloads 可选且可多选
 * （同一 cluster/namespace 下允许一次选多个 workload）。多条 topo 与外层的 propConditions 组合，语义为
 * 「这些拓扑路径共用同一组字段级条件」。运行时对每条 topo 取其最精细的已选节点：选了 workloads 则逐个展开为
 * workload 节点，否则退到 namespace，再退到 cluster。
 * <p>
 * 字段完整性与跨字段约束（cluster 必填、workloads 需先选 namespace 等）由 {@code WebContainerConditionFilterValidator} 统一校验。
 */
@Data
@Schema(description = "动态条件过滤器-拓扑路径（Web 层入参）")
public class WebKubeTopo {

    @Schema(description = "集群拓扑对象，必填")
    private WebKubeClusterObject cluster;

    @Schema(description = "namespace 拓扑对象；可选，未传则在所选集群下不收窄 namespace")
    private WebKubeNamespaceObject namespace;

    @Schema(description = "workload 拓扑对象列表；可选，可多选，每项携带 kind 表示具体 workload 类型")
    private List<WebKubeWorkloadObject> workloads;
}
