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

package com.tencent.bk.job.manage.model.web.vo.chooser.container;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 容器拓扑节点最小引用（type + id）。
 * <p>
 * 用于详情/编辑页从持久化的 {@code KubeClusterObjectDTO / KubeNamespaceObjectDTO / KubeWorkloadObjectDTO}
 * 反查展示名。type 的取值直接使用 CMDB 的 objectId，即
 * {@link com.tencent.bk.job.common.constant.KubeTopoNodeTypeEnum} 中的枚举值：
 * cluster / namespace / deployment / daemonSet / statefulSet / cronJob / job / customResource。
 */
@Data
@Schema(description = "容器拓扑节点最小引用（type + id）")
public class WebKubeNodeRefVO {

    @Schema(description = "节点类型：cluster / namespace / deployment / daemonSet / statefulSet / cronJob / job "
        + "/ customResource")
    @NotBlank(message = "{validation.constraints.KubeNodeType_empty.message}")
    private String type;

    @Schema(description = "CMDB 中的实例 ID")
    @NotNull(message = "{validation.constraints.KubeNodeId_empty.message}")
    private Long id;
}
