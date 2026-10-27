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

/**
 * 动态条件过滤器-workload 拓扑对象（Web 层入参契约面）。
 * <p>
 * 与 cluster/namespace 不同，workload 的「类型」是节点本身的属性，独立携带 {@code kind}
 * （对应 {@code /topology/container} 返回中的 {@code objectId}，取值如 deployment / daemonSet /
 * statefulSet 等）；不同 topo 可携带不同 kind 的 workload，允许混合多种类型。
 * kind 非空且需为合法 workload 类型、id 非空，均由 {@code WebContainerConditionFilterValidator} 统一校验。
 */
@Data
@Schema(description = "动态条件过滤器-workload 拓扑对象（Web 层入参）")
public class WebKubeWorkloadObject {

    @Schema(description = "workload 类型，对应拓扑树 objectId，取值见 KubeTopoNodeTypeEnum 的 workload 类型："
        + "deployment / daemonSet / statefulSet / cronJob / job / customResource")
    private String kind;

    @Schema(description = "workload ID（CMDB 内部 ID，对应拓扑树 instanceId）")
    private Long id;
}
