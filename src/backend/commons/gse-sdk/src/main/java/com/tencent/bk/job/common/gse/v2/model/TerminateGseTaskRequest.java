package com.tencent.bk.job.common.gse.v2.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
     * 脚本命令定义
     */
    @JsonProperty("agent_id_list")
    private List<String> agentIds = new ArrayList<>();

    /**
     * 是否是GSE V2 Task; 根据gseV2Task判断请求GSE V1/v2
     */
    @JsonIgnore
    private boolean gseV2Task;

    public TerminateGseTaskRequest(String taskId, List<String> agentIds) {
        this.taskId = taskId;
        this.agentIds = agentIds;
    }
}
