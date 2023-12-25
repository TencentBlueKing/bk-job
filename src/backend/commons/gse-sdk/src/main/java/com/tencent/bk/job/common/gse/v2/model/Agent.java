package com.tencent.bk.job.common.gse.v2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * GSE - 目标Agent
 */
@Data
public class Agent {
    /**
     * 目标Agent ，数据格式分为两种。1. cloudId:ip（兼容老版本Agent没有agentId的情况) 2. agentId
     */
    @JsonProperty("bk_agent_id")
    private String agentId;

    /**
     * 目标容器 ID, 空则为主机
     */
    @JsonProperty("bk_container_id")
    private String containerId;

    /**
     * 执行账号名
     */
    private String user;

    /**
     * 与user 用户名对应的密码
     */
    private String pwd;
}
