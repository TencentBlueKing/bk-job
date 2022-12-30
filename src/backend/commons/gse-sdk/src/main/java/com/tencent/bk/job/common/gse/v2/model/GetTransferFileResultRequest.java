package com.tencent.bk.job.common.gse.v2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GSE - 查询文件任务的执行结果请求
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GetTransferFileResultRequest extends GseReq {
    /**
     * GSE 任务ID
     */
    @JsonProperty("task_id")
    private String taskId;

    /**
     * 过滤结果的agentId
     */
    @JsonProperty("agent_id_list")
    private List<String> agentIds;
}
