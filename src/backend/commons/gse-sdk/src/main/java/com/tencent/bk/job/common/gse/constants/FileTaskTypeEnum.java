package com.tencent.bk.job.common.gse.constants;

/**
 * GSE 文件任务类型
 */
public enum FileTaskTypeEnum {
    /**
     * 文件分发
     */
    FILE(1),
    /**
     * 目录分发
     */
    DIR(2),
    /**
     * 按照正则表达式分发源文件
     */
    REGEX(3),
    /**
     * 按照通配符分发源文件
     */
    WILDCARD(4);

    private final int value;

    FileTaskTypeEnum(int taskType) {
        this.value = taskType;
    }

    public static FileTaskTypeEnum valueOf(Integer taskType) {
        if (taskType == null) {
            return null;
        }
        for (FileTaskTypeEnum inst : values()) {
            if (inst.value == taskType) {
                return inst;
            }
        }
        return null;
    }

    public final int getValue() {
        return value;
    }
}
