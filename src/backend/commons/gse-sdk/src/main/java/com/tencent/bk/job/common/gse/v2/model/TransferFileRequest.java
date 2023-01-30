package com.tencent.bk.job.common.gse.v2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * GSE - 下发文件任务请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TransferFileRequest extends GseReq {

    /**
     * 文件任务
     */
    private List<FileTransferTask> tasks = new ArrayList<>();

    /**
     * 任务超时秒数
     */
    @JsonProperty("timeout_seconds")
    private int timeout;

    /**
     * 目录创建策略，true：自动创建，false：即使目录不存在也不自动创建
     */
    @JsonProperty("auto_mkdir")
    private boolean autoMkdir;

    /**
     * 文件上传速度限制(MB)
     */
    @JsonProperty("upload_speed")
    private Integer uploadSpeed;

    /**
     * 文件下载速度限制(MB)，0：无限制
     */
    @JsonProperty("download_speed")
    private Integer downloadSpeed;

    public void addFileTask(FileTransferTask task) {
        tasks.add(task);
    }
}
