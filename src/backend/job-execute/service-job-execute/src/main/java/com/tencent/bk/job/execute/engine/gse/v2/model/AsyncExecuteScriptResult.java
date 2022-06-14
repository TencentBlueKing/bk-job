package com.tencent.bk.job.execute.engine.gse.v2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GSE 脚本任务下发返回
 */
@Data
@NoArgsConstructor
public class AsyncExecuteScriptResult {
    /**
     * 执行结果
     */
    private GseTaskId result;

    @Data
    public static class GseTaskId {
        @JsonProperty("task_id")
        private String taskId;
    }
}
