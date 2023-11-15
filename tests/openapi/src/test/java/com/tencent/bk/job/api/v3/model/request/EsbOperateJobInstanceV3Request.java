package com.tencent.bk.job.api.v3.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.api.model.BaseEsbReq;
import lombok.Getter;
import lombok.Setter;

/**
 * 作业实例操作请求
 */
@Getter
@Setter
public class EsbOperateJobInstanceV3Request extends BaseEsbReq {
    /**
     * 业务ID
     */
    @JsonProperty("bk_biz_id")
    private Long appId;

    /**
     * 作业实例ID
     */
    @JsonProperty("job_instance_id")
    private Long taskInstanceId;

    /**
     * 操作类型：1、终止作业
     */
    @JsonProperty("operation_code")
    private Integer operationCode;

}
