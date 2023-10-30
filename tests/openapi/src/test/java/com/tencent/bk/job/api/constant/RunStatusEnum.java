package com.tencent.bk.job.api.constant;

/**
 * 作业执行状态
 */
public enum RunStatusEnum {
    BLANK(1, "等待执行"), RUNNING(2, "正在执行"), SUCCESS(3, "执行成功"),
    FAIL(4, "执行失败"), SKIPPED(5, "跳过"), IGNORE_ERROR(6, "忽略错误"),
    WAITING(7, "等待用户"), TERMINATED(8, "手动结束"), ABNORMAL_STATE(9, "状态异常"),
    STOPPING(10, "强制终止中"), STOP_SUCCESS(11, "强制终止成功"), STOP_FAIL(12, "强制终止失败");

    private final Integer value;
    private final String name;

    RunStatusEnum(Integer val, String name) {
        this.value = val;
        this.name = name;
    }

    public Integer getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public static RunStatusEnum valueOf(int status) {
        for (RunStatusEnum runStatusEnum : values()) {
            if (runStatusEnum.getValue() == status) {
                return runStatusEnum;
            }
        }
        return null;
    }

    public String getI18nKey() {
        return "task.run.status." + this.name().toLowerCase();
    }
}
