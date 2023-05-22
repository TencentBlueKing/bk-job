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

}
