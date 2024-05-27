package com.tencent.bk.job.common.gse.v2.model;

import com.fasterxml.jackson.annotation.JsonInclude;
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
         * 目标容器 ID, 空则为主机
         */
        @JsonProperty("bk_container_id")
        private String containerId;

        /**
         * 脚本任务
         */
        @JsonProperty("atomic_tasks")
        private List<AtomicTask> atomicTasks;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
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

        /**
         * 执行日志读取大小上限，单位byte
         */
        private Long limit;
    }

    /**
     * 新增 Agent 查询条件
     *
     * @param executeObjectGseKey 执行对象 GSE KEY
     * @param atomicTaskId        id 编号，在当前任务里面唯一，需要取大于等于0的值
     * @param offset              执行日志读取偏移量，单位byte
     * @param limit               执行日志读取大小上限，单位byte；传入 null 无效，表示不限制
     */
    public void addAgentTaskQuery(ExecuteObjectGseKey executeObjectGseKey,
                                  Integer atomicTaskId,
                                  int offset,
                                  Long limit) {
        AgentTask agentTask = new AgentTask();
        agentTask.setAgentId(executeObjectGseKey.getAgentId());
        agentTask.setContainerId(executeObjectGseKey.getContainerId());
        AtomicTask atomicTask = new AtomicTask();
        atomicTask.setAtomicTaskId(atomicTaskId);
        atomicTask.setOffset(offset);
        if (limit != null && limit > 0) {
            atomicTask.setLimit(limit);
        }
        agentTask.setAtomicTasks(Collections.singletonList(atomicTask));
        agentTasks.add(agentTask);
    }


}
