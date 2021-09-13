package com.tencent.bk.job.crontab.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author wadema
 * @version 1.0
 * @date 2021/9/13
 */
@Getter
@AllArgsConstructor
public enum LastExecuteStatusEnum {
    /**
     * 未执行
     */
    DEFAULT(0),
    /**
     * 成功
     */
    SUCCESS(1),
    /**
     * 失败
     */
    FAIL(2),
    ;

    @JsonValue
    private int value;

    @JsonCreator
    public static LastExecuteStatusEnum valueOf(int type) {
        for (LastExecuteStatusEnum lastExecuteStatus : values()) {
            if (lastExecuteStatus.value == type) {
                return lastExecuteStatus;
            }
        }
        return null;
    }
}
