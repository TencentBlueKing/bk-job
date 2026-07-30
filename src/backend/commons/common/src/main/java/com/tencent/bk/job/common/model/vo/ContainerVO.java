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

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

/**
 * 容器
 */
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "容器")
public class ContainerVO {

    @Schema(description = "容器资源 ID, 容器在 cmdb 中注册资源的 ID")
    private Long id;

    @Schema(description = "容器 ID", example = "docker://8812391923...")
    private String uid;

    @Schema(description = "容器名称")
    private String name;

    @Schema(description = "Pod名称")
    private String podName;

    @Schema(description = "所属 pod labels")
    private Map<String, String> podLabels;

    @Schema(description = "所属 Node hostId")
    private Long nodeHostId;

    @Schema(description = "所属 Node Ip")
    private String nodeIp;

    @Schema(description = "所属 Node GSE agent 状态")
    private String nodeAgentStatus;

    @Schema(description = "cluster在cmdb中的唯一ID")
    private Long clusterId;

    @Schema(description = "集群 ID", example = "BCS-K8S-00000")
    private String clusterUID;

    @Schema(description = "集群名称")
    private String clusterName;

    @Schema(description = "命名空间在 cmdb 的唯一 ID")
    private Long namespaceId;

    @Schema(description = "命名空间名称")
    private String namespace;

    @Schema(description = "workload 类型")
    private String workloadType;
}
