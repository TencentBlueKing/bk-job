package com.tencent.bk.job.common.gse.v2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * GSE - 脚本
 */
@Data
public class GseScript {
    /**
     * 脚本文件名
     */
    @JsonProperty("script_name")
    private String name;

    /**
     * 脚本文件的存储路径，eg: /tmp/bkjob
     */
    @JsonProperty("script_store_dir")
    private String storeDir;

    /**
     * 脚本内容
     */
    @JsonProperty("script_content")
    private String content;
}
