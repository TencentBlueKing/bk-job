package com.tencent.bk.job.api.v3.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 账号信息
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EsbAccountV3BasicDTO {
    /**
     * 账号ID
     */
    private Long id;
    /**
     * 账号名称
     */
    private String name;
    /**
     * 账号别名
     */
    private String alias;
}
