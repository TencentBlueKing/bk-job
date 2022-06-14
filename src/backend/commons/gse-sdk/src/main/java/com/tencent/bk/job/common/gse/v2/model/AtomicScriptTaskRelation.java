package com.tencent.bk.job.common.gse.v2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * GSE - 定义脚本任务之间的依赖关系
 */
@Data
public class AtomicScriptTaskRelation {
    /**
     * id 编号，在当前任务里面唯一，需要取大于等于0的值
     *
     * @see AtomicScriptTask
     */
    @JsonProperty("atomic_task_id")
    private Integer taskId;

    /**
     * 此值必须已经在atomic_tasks定义的atomic_task_id值且需要按照atomic_task_id依赖的任务的顺序设置此值
     */
    @JsonProperty("atomic_task_id_idx")
    private List<Integer> index;
}
