package com.tencent.bk.job.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Open API 通用返回数据结构
 *
 * @param <T> data 数据
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(value = "job_request_id")
public class EsbResp<T> {

    public static final Integer SUCCESS_CODE = 0;

    private Integer code;

    private T data;

    private Boolean result;

    private String message;
}
