package com.tencent.bk.job.api.v3.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EsbCCTopoNodeDTO {
    @JsonProperty("id")
    private Long id;
    @JsonProperty("node_type")
    private String nodeType;
}
