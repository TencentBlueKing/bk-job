package com.tencent.bk.job.common.gse.util;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * GSE Agent 工具类
 */
public class AgentUtils {
    private static final Pattern GSE_V1_AGENT_ID_PATTERN = Pattern.compile(
        "(\\d+):((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.(" +
            "(?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])");

    /**
     * 是否是GSE1.0 AgentId
     *
     * @param agentId agentId
     */
    public static boolean isGseV1AgentId(String agentId) {
        if (StringUtils.isEmpty(agentId)) {
            return false;
        }
        return GSE_V1_AGENT_ID_PATTERN.matcher(agentId).matches();
    }

    /**
     * 是否是GSE2.0 AgentId
     *
     * @param agentId agentId
     */
    public static boolean isGseV2AgentId(String agentId) {
        if (StringUtils.isEmpty(agentId)) {
            return false;
        }
        return !isGseV1AgentId(agentId);
    }

    /**
     * 对接 GSE V1 的 agentId 值为 云区域:ip（内部实现，产品上 GSE V1 Agent 并没有 AgentId 的概念）。
     * 只有GSE V2 Agent 才会在 cmdb 注册真实的 agentId。
     * 为了避免与cmdb 主机 AgentId 属性的理解上的歧义，需要把内部实现上的 GSE V1 agentId 隐藏
     */
    public static String displayAsRealAgentId(String agentId) {
        return AgentUtils.isGseV1AgentId(agentId) ? "" : agentId;
    }

}
