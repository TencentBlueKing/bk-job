package com.tencent.bk.job.common.gse.constants;

public class DefaultBeanNames {
    // Agent状态查询线程池Bean名称
    public static final String AGENT_STATUS_QUERY_THREAD_POOL_EXECUTOR = "agentStatusQueryExecutor";
    // 优先使用GSE V2的Agent状态查询Bean名称
    public static final String PREFER_V2_AGENT_STATE_CLIENT = "PreferV2AgentStateClient";
    // 基于特性配置决定是否使用GSE V2的Agent状态查询Bean名称
    public static final String USE_V2_BY_FEATURE_AGENT_STATE_CLIENT = "UseV2ByFeatureAgentStateClient";
}
