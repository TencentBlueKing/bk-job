package com.tencent.bk.job.api.v3.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * IP对应的作业执行日志
 */
@Data
@NoArgsConstructor
public class EsbIpLogV3DTO {
    /**
     * 云区域ID
     */
    @JsonProperty("bk_cloud_id")
    private Long cloudAreaId;

    private String ip;

    /**
     * 日志内容
     */
    @JsonProperty("log_content")
    private String logContent;
}
