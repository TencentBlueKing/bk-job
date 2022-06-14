package com.tencent.bk.job.common.gse.v2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * GSE - 原子脚本任务定义
 */
@Data
public class AtomicScriptTask {
    /**
     * id 编号，在当前任务里面唯一，需要取大于等于0的值
     */
    @JsonProperty("atomic_task_id")
    private Integer taskId;

    /**
     * 目标机器上执行的脚本文件绝对路径，eg：/tmp/bkjob/root/xxxx.sh
     */
    private String command;

    /**
     * 当前任务执行的超时时间,单位秒
     */
    private int timeout;
}
