package com.tencent.bk.job.api.v3.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 作业下发信息
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EsbJobExecuteV3DTO {
    /**
     * 作业实例ID
     */
    @JsonProperty("job_instance_id")
    private Long taskInstanceId;
    /**
     * 作业名称
     */
    @JsonProperty("job_instance_name")
    private String taskName;

    /**
     * 步骤实例 ID
     */
    @JsonProperty("step_instance_id")
    private Long stepInstanceId;
}
