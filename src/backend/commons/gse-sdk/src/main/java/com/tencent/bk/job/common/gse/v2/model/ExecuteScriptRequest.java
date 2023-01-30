package com.tencent.bk.job.common.gse.v2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * GSE - 下发脚本任务请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ExecuteScriptRequest extends GseReq {
    /**
     * 目标Agent
     */
    private List<Agent> agents = new ArrayList<>();

    /**
     * 任务脚本
     */
    private List<GseScript> scripts = new ArrayList<>();

    /**
     * atomic_tasks 包含的元素的个数
     */
    private int atomicTaskNum;

    /**
     * 脚本命令定义
     */
    @JsonProperty("atomic_tasks")
    private List<AtomicScriptTask> atomicTasks = new ArrayList<>();

    /**
     * 任务之间的依赖关系
     */
    @JsonProperty("atomic_tasks_relations")
    private List<AtomicScriptTaskRelation> atomicScriptTaskRelations = new ArrayList<>();

    public void addScript(GseScript script) {
        scripts.add(script);
    }

    public void addAtomicScriptTask(AtomicScriptTask atomicScriptTask) {
        atomicTasks.add(atomicScriptTask);
    }

    public void addAtomicTaskRelation(AtomicScriptTaskRelation relation) {
        atomicScriptTaskRelations.add(relation);
    }
}
