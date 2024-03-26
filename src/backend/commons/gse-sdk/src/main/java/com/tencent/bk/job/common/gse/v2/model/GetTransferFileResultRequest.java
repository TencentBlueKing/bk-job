package com.tencent.bk.job.common.gse.v2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * GSE - 查询文件任务的执行结果请求
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GetTransferFileResultRequest extends GseReq {
    /**
     * GSE 任务ID
     */
    @JsonProperty("task_id")
    private String taskId;

    /**
     * 过滤结果的agent
     */
    @JsonProperty("agents")
    private List<Agent> agents;

    public void addAgentQuery(ExecuteObjectGseKey executeObjectGseKey) {
        if (agents == null) {
            agents = new ArrayList<>();
        }
        Agent agent = new Agent();
        agent.setAgentId(executeObjectGseKey.getAgentId());
        agent.setContainerId(executeObjectGseKey.getContainerId());
        agents.add(agent);
    }

    public void batchAddAgentQuery(Collection<ExecuteObjectGseKey> executeObjectGseKeys) {
        if (agents == null) {
            agents = new ArrayList<>();
        }
        executeObjectGseKeys.forEach(executeObjectGseKey -> {
            Agent agent = new Agent();
            agent.setAgentId(executeObjectGseKey.getAgentId());
            agent.setContainerId(executeObjectGseKey.getContainerId());
            agents.add(agent);
        });
    }
}
