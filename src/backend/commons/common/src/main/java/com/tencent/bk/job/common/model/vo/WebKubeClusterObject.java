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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 动态条件过滤器-集群拓扑对象（Web 层入参契约面）。
 * <p>
 * 前端从 {@code /topology/container} 拉到的拓扑树节点 {@code (instanceId, instanceName)} 直接喂给后端：
 * {@code id} 对应 CMDB 集群内部 ID，{@code name} 是用户可读的展示名，后端持久化两者以支撑详情页回显
 * 时无需二次访问 CMDB。
 */
@Data
@Schema(description = "动态条件过滤器-集群拓扑对象（Web 层入参）")
public class WebKubeClusterObject {

    @Schema(description = "集群 ID（CMDB 内部 ID，对应拓扑树 instanceId）")
    @NotNull(message = "{validation.constraints.WebKubeClusterObject_idMissing.message}")
    private Long id;

    @Schema(description = "集群展示名（对应拓扑树 instanceName，仅用于回显）")
    @NotBlank(message = "{validation.constraints.WebKubeClusterObject_nameMissing.message}")
    private String name;
}
