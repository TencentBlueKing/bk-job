package com.tencent.bk.job.common.gse.v2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GSE 源文件
 */
@Data
@NoArgsConstructor
public class SourceFile {
    /**
     * 文件名
     */
    @JsonProperty("file_name")
    private String fileName;

    /**
     * 文件所在目录
     */
    @JsonProperty("store_dir")
    private String storeDir;

    /**
     * 源agent
     */
    @JsonProperty("agent")
    private Agent agent;

    public SourceFile(String fileName, String storeDir, Agent agent) {
        this.fileName = fileName;
        this.storeDir = storeDir;
        this.agent = agent;
    }
}
