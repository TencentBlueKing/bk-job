package com.tencent.bk.job.common.gse.v2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GSE 文件分发任务
 */
@Data
@NoArgsConstructor
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

    public FileTransferTask(SourceFile source, TargetFile target) {
        this.source = source;
        this.target = target;
    }
}
