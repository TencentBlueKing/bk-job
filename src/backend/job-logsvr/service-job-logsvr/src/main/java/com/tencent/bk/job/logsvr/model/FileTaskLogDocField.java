package com.tencent.bk.job.logsvr.model;

/**
 * 存储文件任务执行日志的 MongoDB Doc 字段名
 */
public interface FileTaskLogDocField {
    String STEP_ID = "stepId";
    String EXECUTE_COUNT = "executeCount";
    String BATCH = "batch";
    String TASK_ID = "taskId";
    String MODE = "mode";
    String IP = "ip";
    String HOST_ID = "hostId";
    String SRC_IP = "srcIp";
    String SRC_IPV6 = "srcIpv6";
    String SRC_HOST_ID = "srcHostId";
    String SRC_FILE_TYPE = "srcFileType";
    String SRC_FILE = "srcFile";
    String DISPLAY_SRC_FILE = "displaySrcFile";
    String DEST_IP = "destIp";
    String DEST_IPV6 = "destIpv6";
    String DEST_HOST_ID = "destHostId";
    String DEST_FILE = "destFile";
    String SIZE = "size";
    String STATUS = "status";
    String STATUS_DESC = "statusDesc";
    String SPEED = "speed";
    String PROCESS = "process";
    String CONTENT_LIST = "contentList";
}
