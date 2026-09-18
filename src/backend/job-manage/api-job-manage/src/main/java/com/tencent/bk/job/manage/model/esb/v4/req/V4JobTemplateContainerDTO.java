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

package com.tencent.bk.job.manage.model.esb.v4.req;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * OpenAPI V4 作业模板静态容器。
 * 写入只取 container_id，其余字段是落库快照的回显，写入时忽略，因此响应可原样回传。
 */
@Getter
@Setter
@NoArgsConstructor
public class V4JobTemplateContainerDTO {

    @JsonProperty("container_id")
    @NotNull(message = "{validation.constraints.InvalidTemplateContainerId.message}")
    private Long containerId;

    /**
     * 容器运行时 UID。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("container_uid")
    private String containerUID;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("name")
    private String name;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("pod_name")
    private String podName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("namespace")
    private String namespace;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("cluster_uid")
    private String clusterUID;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("node_ip")
    private String nodeIp;

    public V4JobTemplateContainerDTO(Long containerId) {
        this.containerId = containerId;
    }
}
