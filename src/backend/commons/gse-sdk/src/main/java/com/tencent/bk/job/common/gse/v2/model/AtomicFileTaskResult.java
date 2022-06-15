package com.tencent.bk.job.common.gse.v2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.gse.constants.FileDistModeEnum;
import com.tencent.bk.job.common.gse.util.FilePathUtils;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.StringJoiner;

/**
 * GSE - 任务执行结果(单个文件)
 */
@Data
@NoArgsConstructor
public class AtomicFileTaskResult {

    /**
     * 传输的任务模式： 0 - 源agent上传任务；1 - 目标agent下载任务
     * @see FileDistModeEnum
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
    @JsonProperty("dest_agent_id")
    private String destAgentId;

    /**
     * 目标目录
     */
    @JsonProperty("dest_file_dir")
    private String destFileDir;

    /**
     * 目标文件名
     */
    @JsonProperty("dest_file_name")
    private String destFileName;

    /**
     * 任务执行进度, 取值0～100，代表当前传输任务进度的百分比
     */
    @JsonProperty("process")
    private Integer process;

    /**
     * 文件传输速度，单位KB/S
     */
    @JsonProperty("speed")
    private Integer speed;

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


    public boolean isDownloadMode() {
        return FileDistModeEnum.DOWNLOAD.getValue().equals(this.mode);
    }

    public String getStandardSourceFilePath() {
        if (standardSourceFilePath != null) {
            return standardSourceFilePath;
        } else {
            standardSourceFilePath = FilePathUtils.appendFileName(sourceFileDir, sourceFileName);
        }
        return standardSourceFilePath;
    }

    public String getStandardDestFilePath() {
        if (standardDestFilePath != null) {
            return standardDestFilePath;
        } else {
            if (StringUtils.isEmpty(sourceFileName)) {
                //目录分发
                String srcDirName = FilePathUtils.parseDirName(sourceFileDir);
                standardDestFilePath = FilePathUtils.appendDirName(destFileDir, srcDirName);
            } else {
                // 文件分发、正则分发、通配符分发
                standardDestFilePath = FilePathUtils.appendFileName(destFileDir, sourceFileName);
            }
        }
        return standardDestFilePath;
    }

    public String getTaskId() {
        if (taskId == null) {
            this.taskId = buildTaskId(mode, sourceAgentId, getStandardSourceFilePath(), destAgentId,
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
