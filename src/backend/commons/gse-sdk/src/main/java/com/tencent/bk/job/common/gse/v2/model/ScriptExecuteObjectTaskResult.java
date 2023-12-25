package com.tencent.bk.job.common.gse.v2.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.StringJoiner;

/**
 * GSE - 脚本任务执行结果
 */
@Data
@NoArgsConstructor
public class ScriptExecuteObjectTaskResult {
    /**
     * agent id
     */
    @JsonProperty("bk_agent_id")
    private String agentId;

    /**
     * container id
     */
    @JsonProperty("bk_container_id")
    private String containerId;

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
    @JsonProperty(value = "screen", access = JsonProperty.Access.WRITE_ONLY)
    private String screen;

    /**
     * 非协议内容，仅用于日志输出
     */
    private long contentLength;

    /**
     * 执行目标对应的 GSE KEY。非协议内容
     */
    @JsonIgnore
    private ExecuteObjectGseKey executeObjectGseKey;


    public void setScreen(String screen) {
        this.screen = screen;
        this.contentLength = StringUtils.isEmpty(screen) ? 0 : screen.length();
    }

    @JsonIgnore
    public ExecuteObjectGseKey getExecuteObjectGseKey() {
        if (executeObjectGseKey != null) {
            return executeObjectGseKey;
        }
        if (StringUtils.isNotEmpty(containerId)) {
            // bk_container_id 不为空，说明是容器执行对象
            executeObjectGseKey = ExecuteObjectGseKey.ofContainer(agentId, containerId);
        } else {
            executeObjectGseKey = ExecuteObjectGseKey.ofHost(agentId);
        }
        return executeObjectGseKey;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ScriptExecuteObjectTaskResult.class.getSimpleName() + "[", "]")
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
