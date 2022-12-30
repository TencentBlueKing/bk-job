package com.tencent.bk.job.common.gse.v2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GSE - 查询脚本任务的执行结果请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GetExecuteScriptResultRequest extends GseReq {
    /**
     * GSE 任务ID
     */
    @JsonProperty("task_id")
    private String taskId;

    /**
     * 脚本命令定义
     */
    @JsonProperty("agent_tasks")
    private List<AgentTask> agentTasks = new ArrayList<>();

    @Data
    public static class AgentTask {
        /**
         * 目标Agent ，数据格式分为两种。1. cloudId:ip（兼容老版本Agent没有agentId的情况) 2. agentId
         */
        @JsonProperty("bk_agent_id")
        private String agentId;

        /**
         * 脚本任务
         */
        @JsonProperty("atomic_tasks")
        private List<AtomicTask> atomicTasks;
    }

    @Data
    public static class AtomicTask {
        /**
         * id 编号，在当前任务里面唯一，需要取大于等于0的值
         */
        @JsonProperty("atomic_task_id")
        private Integer atomicTaskId;

        /**
         * 执行日志读取偏移量，单位byte
         */
        private int offset;
    }

    public void addAgentTaskQuery(String agentId, Integer atomicTaskId, int offset) {
        AgentTask agentTask = new AgentTask();
        agentTask.setAgentId(agentId);
        AtomicTask atomicTask = new AtomicTask();
        atomicTask.setAtomicTaskId(atomicTaskId);
        atomicTask.setOffset(offset);
        agentTask.setAtomicTasks(Collections.singletonList(atomicTask));
        agentTasks.add(agentTask);
    }


}
