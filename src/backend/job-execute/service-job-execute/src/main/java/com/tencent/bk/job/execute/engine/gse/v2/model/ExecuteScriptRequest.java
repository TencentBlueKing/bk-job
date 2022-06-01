package com.tencent.bk.job.execute.engine.gse.v2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * GSE - 下发脚本任务请求
 */
@Data
public class ExecuteScriptRequest {
    /**
     * 目标Agent
     */
    private List<Agent> agents;

    /**
     * 任务脚本
     */
    private List<GseScript> scripts;

    /**
     * atomic_tasks 包含的元素的个数
     */
    private int atomicTaskNum;

    /**
     * 脚本命令定义
     */
    @JsonProperty("atomic_tasks")
    private List<AtomicScriptTask> atomicTasks;

    /**
     * 任务之间的依赖关系
     */
    @JsonProperty("atomic_tasks_relations")
    private List<AtomicScriptTaskRelation> atomicScriptTaskRelations;
}
