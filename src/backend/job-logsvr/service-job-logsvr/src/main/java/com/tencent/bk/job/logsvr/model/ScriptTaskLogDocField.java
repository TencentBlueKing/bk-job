package com.tencent.bk.job.logsvr.model;

/**
 * 存储脚本任务执行日志的 MongoDB Doc 字段名
 */
public interface ScriptTaskLogDocField {
    String STEP_ID = "stepId";
    String EXECUTE_COUNT = "executeCount";
    String BATCH = "batch";
    String HOST_ID = "hostId";
    String IP = "ip";
    String IPV6 = "ipv6";
    String CONTENT = "content";
    String OFFSET = "offset";
}
