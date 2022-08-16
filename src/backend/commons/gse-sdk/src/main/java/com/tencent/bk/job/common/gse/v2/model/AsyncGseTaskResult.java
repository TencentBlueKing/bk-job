package com.tencent.bk.job.common.gse.v2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GSE 任务下发返回
 */
@Data
@NoArgsConstructor
public class AsyncGseTaskResult {
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
