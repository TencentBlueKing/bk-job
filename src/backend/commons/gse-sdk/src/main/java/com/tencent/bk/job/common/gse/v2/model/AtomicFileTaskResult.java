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
     * 传输的任务模式： 0 - 源agent上传任务；1 - 目标agent下载任务
     */
    private Integer mode;

    /**
     * 文件源 agent id
     */
    @JsonProperty("source_agent_id")
    private String sourceAgentId;

    /**
     * 源文件目录，下发任务时指定的源文件路径
     */
    @JsonProperty("source_file_dir")
    private String sourceFileDir;

    /**
     * 源文件名，下发任务时，指定的源文件名
     */
    @JsonProperty("source_file_name")
    private String sourceFileName;

    /**
     * 分发目标主机 agent id
     */
    @JsonProperty("target_agent_id")
    private String targetAgentId;

    /**
     * 目标目录
     */
    @JsonProperty("dest_file_dir")
    private String targetFileDir;

    /**
     * 目标文件名
     */
    @JsonProperty("dest_file_name")
    private String targetFileName;

    /**
     * 任务执行进度, 取值0～100，代表当前传输任务进度的百分比
     */
    @JsonProperty("process")
    private Integer process;

    /**
     * 文件大小，单位Byte，字节
     */
    @JsonProperty("size")
    private Integer size;

    /**
     * 任务状态
     */
    @JsonProperty("status")
    private Integer status;

    /**
     * 状态信息描述
     */
    @JsonProperty("status_info")
    private String statusInfo;

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

    /**
     * 任务开始时间
     */
    @JsonProperty("start_time")
    private Long startTime;

    /**
     * 任务结束时间
     */
    @JsonProperty("end_time")
    private Long endTime;
}
