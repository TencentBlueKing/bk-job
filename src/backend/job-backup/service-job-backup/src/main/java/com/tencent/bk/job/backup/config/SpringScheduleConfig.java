package com.tencent.bk.job.backup.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.ScheduledThreadPoolExecutor;

@Configuration
@Slf4j
public class SpringScheduleConfig implements SchedulingConfigurer {
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        log.info("Configure spring cron task scheduler");
        //设定一个长度5的定时任务线程池
        taskRegistrar.setScheduler(new ScheduledThreadPoolExecutor(5, (r, executor) -> log.error(
            "ScheduledThreadPoolExecutor rejected a runnable")));
    }

}
