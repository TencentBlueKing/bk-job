package com.tencent.bk.job.api.v3.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EsbGetPlanListV3Request extends EsbBaseListRequest {

    /**
     * 作业模版 ID
     */
    @JsonProperty("job_template_id")
    private Long templateId;

}
