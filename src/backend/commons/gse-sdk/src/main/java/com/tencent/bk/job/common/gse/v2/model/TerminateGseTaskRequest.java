package com.tencent.bk.job.common.gse.v2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * GSE - 终止GSE任务执行请求
 */
@Data
@NoArgsConstructor
public class TerminateGseTaskRequest {
    /**
     * GSE 任务ID
     */
    @JsonProperty("task_id")
    private String taskId;

    /**
     * 脚本命令定义
     */
    @JsonProperty("agent_id_list")
    private List<String> agentIds = new ArrayList<>();

    public TerminateGseTaskRequest(String taskId, List<String> agentIds) {
        this.taskId = taskId;
        this.agentIds = agentIds;
    }
}
