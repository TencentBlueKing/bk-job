package com.tencent.bk.job.common.gse.v2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * GSE 源文件
 */
@Data
public class FileTransferTask {
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
     * 源agent列表
     */
    @JsonProperty("agents")
    private List<Agent> agents;

}
