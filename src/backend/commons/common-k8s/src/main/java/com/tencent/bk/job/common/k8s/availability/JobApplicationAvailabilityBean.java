package com.tencent.bk.job.common.k8s.availability;

import com.tencent.bk.job.common.util.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.availability.ApplicationAvailabilityBean;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;

@Slf4j
public class JobApplicationAvailabilityBean extends ApplicationAvailabilityBean {
    @Override
    public void onApplicationEvent(AvailabilityChangeEvent<?> event) {
        super.onApplicationEvent(event);
        if (ReadinessState.REFUSING_TRAFFIC == event.getState()) {
            // SpringCloud负载均衡缓存设置为20s，等待调用方缓存刷新后再真正关闭Spring容器
            int waitSeconds = 30;
            while (waitSeconds > 0) {
                ThreadUtils.sleep(1000);
                log.info("wait for GracefulShutdown, {}s left", waitSeconds--);
            }
        }
    }
}
