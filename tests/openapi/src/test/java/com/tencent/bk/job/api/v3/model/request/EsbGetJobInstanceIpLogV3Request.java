package com.tencent.bk.job.api.v3.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.api.model.EsbAppScopeReq;
import lombok.Getter;
import lombok.Setter;

/**
 * get_job_instance_log,根据作业实例ID查询作业执行日志请求
 */
@Getter
@Setter
public class EsbGetJobInstanceIpLogV3Request extends EsbAppScopeReq {

    /**
     * 作业执行实例 ID
     */
    @JsonProperty("job_instance_id")
    private Long taskInstanceId;

    /**
     * 作业步骤实例ID
     */
    @JsonProperty("step_instance_id")
    private Long stepInstanceId;

    /**
     * 云区域ID
     */
    @JsonProperty("bk_cloud_id")
    private Long cloudAreaId;

    private String ip;

    /**
     * 主机ID
     */
    @JsonProperty("bk_host_id")
    private Long hostId;
}
