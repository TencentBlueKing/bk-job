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

package com.tencent.bk.job.common.esb.model.job.v3;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.tencent.bk.job.common.esb.model.job.EsbIpDTO;
import com.tencent.bk.job.common.model.openapi.v3.EsbCmdbTopoNodeDTO;
import com.tencent.bk.job.common.model.openapi.v3.EsbDynamicGroupDTO;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import javax.validation.Valid;
import java.util.List;

/**
 * 主机定义-ESB
 */
@Data
public class EsbServerV3DTO {
    /**
     * 目标服务器对应的主机变量
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyDescription("Host variable name")
    private String variable;

    @JsonProperty("ip_list")
    @JsonPropertyDescription("Hosts with ip")
    @Valid
    private List<EsbIpDTO> ips;

    @JsonProperty("host_id_list")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonPropertyDescription("Host ids")
    private List<Long> hostIds;

    /**
     * 动态分组ID列表
     */
    @JsonProperty("dynamic_group_list")
    @JsonPropertyDescription("Cmdb dynamic groups")
    private List<EsbDynamicGroupDTO> dynamicGroups;

    /**
     * 分布式拓扑节点列表
     */
    @JsonProperty("topo_node_list")
    @JsonPropertyDescription("Cmdb topo nodes")
    private List<EsbCmdbTopoNodeDTO> topoNodes;

    /**
     * 检查执行主机的参数是否非空
     */
    public boolean checkHostParamsNonEmpty() {
        return CollectionUtils.isNotEmpty(hostIds)
            || CollectionUtils.isNotEmpty(ips)
            || CollectionUtils.isNotEmpty(topoNodes)
            || CollectionUtils.isNotEmpty(dynamicGroups);
    }
}
