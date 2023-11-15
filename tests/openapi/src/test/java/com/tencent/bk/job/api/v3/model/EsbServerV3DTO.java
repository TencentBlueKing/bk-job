package com.tencent.bk.job.api.v3.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 服务器定义-ESB
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EsbServerV3DTO {
    /**
     * 目标服务器对应的主机变量
     */
    private String variable;

    @JsonProperty("ip_list")
    private List<HostDTO> ips;

    @JsonProperty("host_id_list")
    private List<Long> hostIds;

    /**
     * 动态分组ID列表
     */
    @JsonProperty("dynamic_group_list")
    private List<EsbDynamicGroupDTO> dynamicGroups;

    /**
     * 分布式拓扑节点列表
     */
    @JsonProperty("topo_node_list")
    private List<EsbCCTopoNodeDTO> topoNodes;
}
