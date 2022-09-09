/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.execute.engine.gse.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.execute.common.constants.FileDistModeEnum;
import com.tencent.bk.job.execute.engine.util.FilePathUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.util.StringJoiner;

/**
 * GSE 文件任务结果
 */
@Getter
@Setter
@ToString
public class GSEFileTaskResult {

    /**
     * 0:upload， 1:download
     *
     * @see FileDistModeEnum
     */
    private Integer mode;

    /**
     * 完成进度，取值0-100
     */
    private Integer process;

    /**
     * 文件总字节数
     */
    private Long size;

    /**
     * 传输的速度，单位bps
     */
    private Integer speed;

    /**
     * 任务状态信息，比如checksum/upload/download
     */
    private String type;

    /**
     * 任务开始时间
     */
    @JsonProperty("start_time")
    private Long startTime;

    /**
     * 任务开始时间-兼容字段
     */
    @JsonProperty("starttime")
    private String startTimeStr;

    /**
     * 任务结束时间-兼容字段
     */
    @JsonProperty("endtime")
    private String endTimeStr;

    /**
     * 任务结束时间
     */
    @JsonProperty("end_time")
    private Long endTime;

    /**
     * 文件源IP
     */
    @JsonProperty("source")
    private String sourceIp;

    /**
     * 文件源云区域
     */
    @JsonProperty("source_cloudid")
    private Long sourceCloudId;

    /**
     * 文件源主机AgentId
     */
    @JsonProperty("source_agent_id")
    private String sourceAgentId;

    /**
     * 分发目标IP
     */
    @JsonProperty("dest")
    private String destIp;

    /**
     * 分发目标云区域
     */
    @JsonProperty("dest_cloudid")
    private Long destCloudId;

    /**
     * 文件目标主机AgentId
     */
    @JsonProperty("dest_agent_id")
    private String destAgentId;

    /**
     * 源文件目录
     */
    @JsonProperty("source_file_dir")
    private String srcDirPath;

    /**
     * 源文件名
     */
    @JsonProperty("source_file_name")
    private String srcFileName;

    /**
     * 目标文件目录
     */
    @JsonProperty("dest_file_dir")
    private String destDirPath;

    /**
     * 目标文件名
     */
    @JsonProperty("dest_file_name")
    private String destFileName;

    /**
     * GSE 文件任务状态
     */
    @JsonProperty("status_code")
    private Integer status;

    /**
     * GSE 文件任务状态描述
     */
    @JsonProperty("status_info")
    private String statusDesc;

    /**
     * GSE 协议版本(0 - 未知版本；1 - 初始版本 ; 2 - 解除valuekey依赖版本)
     */
    @JsonProperty("protover")
    private Integer protocolVersion;

    // ------------------ 非协议字段 ----------------------
    /**
     * 用来表示文件任务ID
     */
    private String taskId;

    /**
     * 标准化之后的源文件路径
     */
    private String standardSourceFilePath;

    /**
     * 标准化之后的目标文件路径
     */
    private String standardDestFilePath;

    /**
     * 任务类型，1-文件分发，2-目录分发，3-正则分发，4-通配符分发
     */
    private TaskType taskType;

    /**
     * 文件源主机agentId
     * 按照与GSE的约定，在没有bk_agent_id的场景下，使用{云区域:IP}作为agentId
     */
    public String getSourceAgentId() {
        if (sourceAgentId == null) {
            sourceAgentId = sourceCloudId + ":" + sourceIp;
        }
        return sourceAgentId;
    }

    /**
     * 目标主机agentId
     * 按照与GSE的约定，在没有bk_agent_id的场景下，使用{云区域:IP}作为agentId
     */
    public String getDestAgentId() {
        if (destAgentId == null) {
            destAgentId = destCloudId + ":" + destIp;
        }
        return destAgentId;
    }

    public boolean isDownloadMode() {
        return FileDistModeEnum.DOWNLOAD.getValue().equals(this.mode);
    }

    public String getStandardSourceFilePath() {
        if (standardSourceFilePath != null) {
            return standardSourceFilePath;
        } else {
            standardSourceFilePath = FilePathUtils.appendFileName(srcDirPath, srcFileName);
        }
        return standardSourceFilePath;
    }

    public String getStandardDestFilePath() {
        if (standardDestFilePath != null) {
            return standardDestFilePath;
        } else {
            if (protocolVersion != null && protocolVersion == 2) {
                if (StringUtils.isEmpty(srcFileName)) {
                    //目录分发
                    String srcDirName = FilePathUtils.parseDirName(srcDirPath);
                    standardDestFilePath = FilePathUtils.appendDirName(destDirPath, srcDirName);
                } else {
                    // 文件分发、正则分发、通配符分发
                    standardDestFilePath = FilePathUtils.appendFileName(destDirPath, srcFileName);
                }
            } else {
                standardDestFilePath = FilePathUtils.appendFileName(destDirPath, destFileName);
            }
        }
        return standardDestFilePath;
    }

    public String getTaskId() {
        if (taskId == null) {
            this.taskId = buildTaskId(mode, getSourceAgentId(), getStandardSourceFilePath(), getDestAgentId(),
                getStandardDestFilePath());
        }
        return taskId;
    }

    public static String buildTaskId(Integer mode, String sourceAgentId, String sourceFilePath, String destAgentId,
                                     String destFilePath) {
        String taskId;
        if (FileDistModeEnum.getFileDistMode(mode) == FileDistModeEnum.DOWNLOAD) {
            taskId = concat(mode.toString(), sourceAgentId, FilePathUtils.standardizedGSEFilePath(sourceFilePath),
                destAgentId, destFilePath);
        } else {
            taskId = concat(mode.toString(), sourceAgentId, FilePathUtils.standardizedGSEFilePath(sourceFilePath));
        }
        return taskId;
    }

    private static String concat(String... strArgs) {
        StringJoiner sj = new StringJoiner(":");
        for (String strArg : strArgs) {
            if (StringUtils.isEmpty(strArg)) {
                sj.add("");
            } else {
                sj.add(strArg);
            }
        }
        return sj.toString();
    }

    public Long getStartTime() {
        if (startTime != null) {
            return startTime;
        }
        if (StringUtils.isNotEmpty(startTimeStr)) {
            return Long.valueOf(startTimeStr);
        }
        return null;
    }

    public Long getEndTime() {
        if (endTime != null) {
            return endTime;
        }
        if (StringUtils.isNotEmpty(endTimeStr)) {
            return Long.valueOf(endTimeStr);
        }
        return null;
    }

    private enum TaskType {
        FILE(1), DIR(2), REGEX(3), WILDCARD(4);

        private final int value;

        TaskType(int taskType) {
            this.value = taskType;
        }

        public static TaskType valueOf(Integer taskType) {
            if (taskType == null) {
                return null;
            }
            for (TaskType inst : values()) {
                if (inst.value == taskType) {
                    return inst;
                }
            }
            return null;
        }

        public final int getValue() {
            return value;
        }
    }
}
