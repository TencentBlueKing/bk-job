package com.tencent.bk.job.common.gse.v2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * GSE - 终止GSE任务执行请求
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TerminateGseTaskRequest extends GseReq {
    /**
     * GSE 任务ID
     */
    @JsonProperty("task_id")
    private String taskId;

    /**
     * 目标 Agent 列表
     */
    @JsonProperty("agents")
    private List<Agent> agents = new ArrayList<>();


    public TerminateGseTaskRequest(String taskId,
                                   List<Agent> agents) {
        this.taskId = taskId;
        this.agents = agents;
    }
}
