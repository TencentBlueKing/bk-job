package com.tencent.bk.job.common.gse.v2.model.req;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.gse.v2.model.GseExecutionContext;
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

    @JsonIgnore
    private GseExecutionContext context;

}
