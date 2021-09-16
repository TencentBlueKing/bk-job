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
    @Value("${notification-policy.failed.start.begin}")
    private Integer begin;
    @Value("${notification-policy.failed.start.frequency}")
    private Integer frequency;
    @Value("${notification-policy.failed.start.totalTimes}")
    private Integer totalTimes;
}
