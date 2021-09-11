package com.tencent.bk.job.crontab.timer;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author wadema
 * @version 1.0
 * @date 2021/9/11
 */
@Getter
@Component
public class Notification {
    @Value("${notification.strategy}")
    private Integer strategy;
}
