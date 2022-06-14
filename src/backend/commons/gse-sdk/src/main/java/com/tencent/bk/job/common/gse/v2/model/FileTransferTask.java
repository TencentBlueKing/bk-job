package com.tencent.bk.job.common.gse.v2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * GSE 源文件
 */
@Data
public class FileTransferTask {
    /**
     * 源文件
     */
    @JsonProperty("source")
    private SourceFile source;

    /**
     * 传输目标
     */
    @JsonProperty("target")
    private TargetFile target;

}
