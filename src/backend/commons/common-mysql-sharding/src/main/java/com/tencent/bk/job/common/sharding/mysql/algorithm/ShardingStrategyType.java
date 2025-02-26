package com.tencent.bk.job.common.sharding.mysql.algorithm;

import lombok.Getter;

/**
 * 分片策略类型
 */
@Getter
public enum ShardingStrategyType {
    STANDARD(Constants.STANDARD),
    COMPLEX(Constants.COMPLEX),
    HINT(Constants.HINT);

    ShardingStrategyType(String type) {
        this.type = type;
    }

    public static class Constants {
        public static final String STANDARD = "standard";
        public static final String COMPLEX = "complex";
        public static final String HINT = "hint";
    }

    private final String type;

    public static ShardingStrategyType valOf(String type) {
        for (ShardingStrategyType typeEnum : values()) {
            if (typeEnum.type.equals(type)) {
                return typeEnum;
            }
        }
        throw new IllegalArgumentException("No ShardingStrategyType constant: " + type);
    }
}
