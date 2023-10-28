package com.tencent.bk.job.api.constant;

public enum IpStatus {
    /**
     * 未知错误
     */
    UNKNOWN_ERROR(0),
    /**
     * Agent异常
     */
    AGENT_ERROR(1),
    /**
     * 上次已成功
     */
    LAST_SUCCESS(3),
    /**
     * 等待执行
     */
    WAITING(5),
    /**
     * 正在执行
     */
    RUNNING(7),
    /**
     * 执行成功
     */
    SUCCESS(9),
    /**
     * 执行失败
     */
    FAILED(11),
    /**
     * 任务下发失败
     */
    SUBMIT_FAILED(12),
    /**
     * 任务超时
     */
    TASK_TIMEOUT(13),
    /**
     * 任务日志错误
     */
    LOG_ERROR(15),
    /**
     * GSE脚本日志超时
     */
    GSE_SCRIPT_TIMEOUT(16),
    /**
     * GSE文件日志超时
     */
    GSE_FILE_TIMEOUT(17),
    /**
     * 脚本执行失败
     */
    SCRIPT_FAILED(101),
    /**
     * 脚本执行超时
     */
    SCRIPT_TIMEOUT(102),
    /**
     * 脚本执行被终止
     */
    SCRIPT_TERMINATE(103),
    /**
     * 脚本返回码非零
     */
    SCRIPT_NOT_ZERO_EXIT_CODE(104),
    /**
     * 文件传输失败
     */
    COPYFILE_FAILED(202),
    /**
     * 源文件不存在
     */
    COPYFILE_SOURCE_FILE_NOT_EXIST(203),
    /**
     * 文件任务系统错误-未分类的
     */
    FILE_ERROR_UNCLASSIFIED(301),
    /**
     * 文件任务超时
     */
    GSE_TIMEOUT(303),
    /**
     * Agent异常
     */
    GSE_AGENT_ERROR(310),
    /**
     * 用户名不存在
     */
    GSE_USER_ERROR(311),
    /**
     * 用户密码错误
     */
    GSE_USER_PWD_ERROR(312),
    /**
     * 文件获取失败
     */
    GSE_FILE_ERROR(320),
    /**
     * 文件超出限制
     */
    GSE_FILE_SIZE_EXCEED(321),
    /**
     * 文件传输错误
     */
    GSE_FILE_TASK_ERROR(329),
    /**
     * 任务执行出错
     */
    GSE_TASK_ERROR(399),
    /**
     * GSE 任务强制终止成功
     */
    GSE_TASK_TERMINATE_SUCCESS(403),
    /**
     * GSE 任务强制终止失败
     */
    GSE_TASK_TERMINATE_FAILED(404);


    IpStatus(int status) {
        this.status = status;
    }

    private final int status;

    public final int getValue() {
        return status;
    }

    public static IpStatus valueOf(Integer status) {
        if (status == null) {
            return null;
        }
        for (IpStatus ipStatus : values()) {
            if (ipStatus.status == status) {
                return ipStatus;
            }
        }
        return UNKNOWN_ERROR;
    }

    public String getI18nKey() {
        return "agent.task.status." + this.name().toLowerCase();
    }
}
