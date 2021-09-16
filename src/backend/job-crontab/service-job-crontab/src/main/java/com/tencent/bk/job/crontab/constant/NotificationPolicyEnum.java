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
     * 通知次数无限制
     */
    INFINITE(-1),
    /**
     * 不通知
     */
    NO_NOTIFY(0),
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
