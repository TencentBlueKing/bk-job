package com.tencent.bk.job.common.gse.v2.model.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * GSE - 查询Agent状态请求
 */
@Data
public class ListAgentStateReq {
    /**
     * Agent ID列表
     */
    @JsonProperty("agent_id_list")
    private List<String> agentIdList;

}
