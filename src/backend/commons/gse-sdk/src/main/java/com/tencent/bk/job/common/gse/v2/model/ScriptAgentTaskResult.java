package com.tencent.bk.job.common.gse.v2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.util.json.SkipLogFields;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.StringJoiner;

/**
 * GSE - Agent脚本任务执行结果
 */
@Data
@NoArgsConstructor
public class ScriptAgentTaskResult {
    /**
     * agent id
     */
    @JsonProperty("bk_agent_id")
    private String agentId;

    /**
     * 脚本原子任务ID
     */
    @JsonProperty("atomic_task_id")
    private Integer atomicTaskId;

    /**
     * 任务状态
     */
    @JsonProperty("status")
    private Integer status;

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

    /**
     * 脚本执行 exit code
     */
    @JsonProperty("script_exit_code")
    private Integer exitCode;

    /**
     * 用户自定义结果分组标签
     */
    @JsonProperty("tag")
    private String tag;

    /**
     * 脚本输出日志
     */
    @JsonProperty("screen")
    @SkipLogFields
    private String screen;

    /**
     * 非协议内容，仅用于日志输出
     */
    private long contentLength;


    public void setScreen(String screen) {
        this.screen = screen;
        this.contentLength = StringUtils.isEmpty(screen) ? 0 : screen.length();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ScriptAgentTaskResult.class.getSimpleName() + "[", "]")
            .add("agentId=" + agentId)
            .add("atomicTaskId=" + atomicTaskId)
            .add("status=" + status)
            .add("errorCode=" + errorCode)
            .add("errorMsg='" + errorMsg + "'")
            .add("startTime=" + startTime)
            .add("endTime=" + endTime)
            .add("exitCode=" + exitCode)
            .add("tag='" + tag + "'")
            .add("contentLength=" + contentLength)
            .toString();
    }
}
