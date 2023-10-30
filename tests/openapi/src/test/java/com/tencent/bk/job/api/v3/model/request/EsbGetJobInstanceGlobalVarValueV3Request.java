package com.tencent.bk.job.api.v3.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.api.model.BaseEsbReq;
import lombok.Getter;
import lombok.Setter;

/**
 * API:get_job_instance_global_var_value请求
 */
@Getter
@Setter
public class EsbGetJobInstanceGlobalVarValueV3Request extends BaseEsbReq {
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
}
