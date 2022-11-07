package com.tencent.bk.job.common.gse.v2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GSE - 任务执行结果(单个文件)
 */
@Data
@NoArgsConstructor
public class AtomicFileTaskResult {

    /**
     * 错误码
     */
    @JsonProperty("error_code")
    private Integer errorCode;

    /**
     * 错误信息
     */
    @JsonProperty("error_msg")
    private String errorMsg;

    @JsonProperty("content")
    private AtomicFileTaskResultContent content;

}
