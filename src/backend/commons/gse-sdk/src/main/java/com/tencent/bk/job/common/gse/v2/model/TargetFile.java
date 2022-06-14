package com.tencent.bk.job.common.gse.v2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * GSE 目标文件
 */
@Data
public class TargetFile {
    /**
     * 目标文件名
     */
    @JsonProperty("file_name")
    private String fileName;

    /**
     * 目标文件目录
     */
    @JsonProperty("store_dir")
    private String storeDir;

    /**
     * 文件所有者
     */
    @JsonProperty("owner")
    private String owner;

    /**
     * 源文件的权限配置
     */
    @JsonProperty("permission")
    private Integer permission;


    /**
     * 目标agent列表
     */
    @JsonProperty("agents")
    private List<Agent> agents;

}
