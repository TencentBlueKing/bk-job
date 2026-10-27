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

package com.tencent.bk.job.manage.model.esb.v4.resp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.tencent.bk.job.common.model.openapi.v3.EsbCmdbTopoNodeDTO;
import com.tencent.bk.job.common.model.openapi.v3.EsbDynamicGroupDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.OpenApiV4HostDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateContainerDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateContainerFilterDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * OpenAPI V4 执行目标（作业模板详情响应）。
 * 与写接口的 V4JobTemplateExecuteTargetReq 逐字段对应，响应可原样回传给写接口。
 */
@Getter
@Setter
public class V4JobTemplateExecuteTargetDTO {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("variable")
    @JsonPropertyDescription("Referenced global variable name in template")
    private String variable;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("host_list")
    @JsonPropertyDescription("Host list")
    private List<OpenApiV4HostDTO> hostList;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("dynamic_group_list")
    @JsonPropertyDescription("Dynamic group list")
    private List<EsbDynamicGroupDTO> dynamicGroups;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("topo_node_list")
    @JsonPropertyDescription("CMDB topo node list")
    private List<EsbCmdbTopoNodeDTO> topoNodes;

    /**
     * 静态容器列表。除 container_id 外的字段来自落库快照，容器已从 CMDB 删除时仍会原样返回。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("container_list")
    @JsonPropertyDescription("Static container list")
    private List<V4JobTemplateContainerDTO> containerList;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("container_filter_list")
    @JsonPropertyDescription("Container filter list")
    private List<V4JobTemplateContainerFilterDTO> containerFilters;
}
