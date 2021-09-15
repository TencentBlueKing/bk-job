package com.tencent.bk.job.crontab.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author wadema
 * @version 1.0
 * @date 2021/9/15
 */
@Getter
@AllArgsConstructor
public enum NotificationPolicyEnum {
    /**
     * 默认，连续失败只通知第一次
     */
    DEFAULT(0),
    /**
     * 每次失败都发通知
     */
    EVERYTIME(1),
    ;

    @JsonValue
    private int value;

    @JsonCreator
    public static NotificationPolicyEnum valueOf(int type) {
        for (NotificationPolicyEnum notificationPolicy : values()) {
            if (notificationPolicy.value == type) {
                return notificationPolicy;
            }
        }
        return null;
    }
}
