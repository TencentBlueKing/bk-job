package com.tencent.bk.job.common.gse.constants;

/**
 * GSE 任务类型
 */
public enum GseTaskTypeEnum {
    SCRIPT(1), FILE(2);

    private final Integer value;

    GseTaskTypeEnum(Integer val) {
        this.value = val;
    }

    public static GseTaskTypeEnum getGseTaskType(Integer type) {
        if (type == null) {
            return null;
        }
        for (GseTaskTypeEnum typeEnum : values()) {
            if (typeEnum.getValue().equals(type)) {
                return typeEnum;
            }
        }
        throw new IllegalArgumentException("No GseTaskTypeEnum constant: " + type);
    }

    public Integer getValue() {
        return value;
    }
}
