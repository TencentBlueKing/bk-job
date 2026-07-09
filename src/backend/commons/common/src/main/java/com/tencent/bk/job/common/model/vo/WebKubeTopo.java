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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 动态条件过滤器-单条拓扑路径（Web 层入参契约面）。
 * <p>
 * 一条 topo 表示一个精确的 cluster→namespace→workload 路径：cluster 必填，namespace / workload 可选。
 * 多条 topo 与外层的 propConditions 组合，语义为「这些拓扑路径共用同一组字段级条件」。
 * 运行时对每条 topo 各自取其最精细的已选节点（workload → namespace → cluster）作为 CMDB 拓扑节点。
 */
@Data
@Schema(description = "动态条件过滤器-拓扑路径（Web 层入参）")
public class WebKubeTopo {

    @Schema(description = "集群拓扑对象，必填")
    @NotNull(message = "{validation.constraints.WebKubeTopo_clusterMissing.message}")
    @Valid
    private WebKubeClusterObject cluster;

    @Schema(description = "namespace 拓扑对象；可选，未传则在所选集群下不收窄 namespace")
    @Valid
    private WebKubeNamespaceObject namespace;

    @Schema(description = "workload 拓扑对象；可选，携带 kind 表示具体 workload 类型")
    @Valid
    private WebKubeWorkloadObject workload;
}
