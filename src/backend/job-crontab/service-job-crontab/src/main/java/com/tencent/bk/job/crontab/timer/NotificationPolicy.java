package com.tencent.bk.job.crontab.timer;

import lombok.Getter;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author wadema
 * @version 1.0
 * @date 2021/9/16
 */
@Getter
@ToString
@Component
public class NotificationPolicy {
    @Value("${notification-policy.failed.start.begin:1}")
    private Integer begin;
    @Value("${notification-policy.failed.start.frequency:5}")
    private Integer frequency;
    @Value("${notification-policy.failed.start.total:-1}")
    private Integer totalTimes;
}
