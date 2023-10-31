package com.tencent.bk.job.api.v3.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 作业实例全局变量值
 */
@Data
public class EsbJobInstanceGlobalVarValueV3DTO {

    @JsonProperty("job_instance_id")
    private Long taskInstanceId;

    @JsonProperty("step_instance_var_list")
    private List<EsbStepInstanceGlobalVarValues> stepGlobalVarValues;

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EsbStepInstanceGlobalVarValues {
        @JsonProperty("step_instance_id")
        private Long stepInstanceId;

        @JsonProperty("global_var_list")
        private List<EsbStepInstanceGlobalVarValue> stepInstanceGlobalVarValues;
    }

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EsbStepInstanceGlobalVarValue {
        @JsonProperty("name")
        private String name;

        @JsonProperty("value")
        private String value;

        /**
         * 变量类型
         */
        @JsonProperty("type")
        private Integer type;
    }
}
