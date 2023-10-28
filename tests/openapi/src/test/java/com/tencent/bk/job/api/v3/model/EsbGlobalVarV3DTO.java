package com.tencent.bk.job.api.v3.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 全局变量
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
public class EsbGlobalVarV3DTO {
    /**
     * 全局变量ID
     */
    private Long id;

    /**
     * 全局变量名称
     */
    private String name;

    /**
     * 全局变量值，当变量类型为字符、密码、数组时，此变量有效
     */
    private String value;

    @JsonProperty("server")
    private EsbServerV3DTO server;

    /**
     * 变量描述
     */
    private String description;

    /**
     * 变量类型
     */
    private Integer type;

    /**
     * 变量是否必填
     */
    private Integer required;
}
