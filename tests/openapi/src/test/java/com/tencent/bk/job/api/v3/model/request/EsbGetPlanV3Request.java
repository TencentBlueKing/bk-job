package com.tencent.bk.job.api.v3.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.api.model.EsbAppScopeReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EsbGetPlanV3Request extends EsbAppScopeReq {

    /**
     * 执行方案 ID
     */
    @JsonProperty("job_plan_id")
    private Long planId;

}
