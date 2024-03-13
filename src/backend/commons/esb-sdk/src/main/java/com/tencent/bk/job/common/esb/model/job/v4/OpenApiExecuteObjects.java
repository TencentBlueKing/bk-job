/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.job.common.esb.model.job.v4;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.tencent.bk.job.common.esb.model.job.EsbCmdbTopoNodeDTO;
import com.tencent.bk.job.common.esb.model.job.v3.EsbDynamicGroupDTO;
import lombok.Data;

import javax.validation.Valid;
import java.util.List;

/**
 * 执行对象定义
 */
@Data
public class OpenApiExecuteObjects {
    /**
     * 全局变量名
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyDescription("Variable name")
    private String variable;

    @JsonProperty("host_list")
    @JsonPropertyDescription("Hosts")
    @Valid
    private List<OpenApiHost> hosts;

    @JsonProperty("container_list")
    @JsonPropertyDescription("Containers")
    @Valid
    private List<OpenApiContainer> containers;


    /**
     * 动态分组ID列表
     */
    @JsonProperty("dynamic_group_list")
    @JsonPropertyDescription("Cmdb dynamic groups")
    @Valid
    private List<EsbDynamicGroupDTO> dynamicGroups;

    /**
     * 分布式拓扑节点列表
     */
    @JsonProperty("topo_node_list")
    @JsonPropertyDescription("Cmdb topo nodes")
    @Valid
    private List<EsbCmdbTopoNodeDTO> topoNodes;
}
