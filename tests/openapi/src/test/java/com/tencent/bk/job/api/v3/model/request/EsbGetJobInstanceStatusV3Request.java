package com.tencent.bk.job.api.v3.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.api.model.BaseEsbReq;
import lombok.Getter;
import lombok.Setter;

/**
 * get_job_instance_status,根据作业实例 ID 查询作业执行状态请求
 */
@Getter
@Setter
public class EsbGetJobInstanceStatusV3Request extends BaseEsbReq {
    /**
     * 业务 ID
     */
    @JsonProperty("bk_biz_id")
    private Long appId;

    /**
     * 作业执行实例 ID
     */
    @JsonProperty("job_instance_id")
    private Long taskInstanceId;

    /**
     * 是否返回每个ip上的任务详情
     */
    @JsonProperty("return_ip_result")
    private Boolean returnIpResult = false;
}
