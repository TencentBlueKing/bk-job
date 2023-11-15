package com.tencent.bk.job.api.v3.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.api.model.BaseEsbReq;
import lombok.Getter;
import lombok.Setter;

/**
 * 步骤操作请求
 */
@Getter
@Setter
public class EsbOperateStepInstanceV3Request extends BaseEsbReq {
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
     * 步骤实例ID
     */
    @JsonProperty("step_instance_id")
    private Long stepInstanceId;

    /**
     * 操作类型：2、失败IP重做，3、忽略错误，4、执行，5、跳过，6、确认继续 8、全部重试，9、终止确认流程，10、重新发起确认，11、进入下一步
     */
    @JsonProperty("operation_code")
    private Integer operationCode;

}
