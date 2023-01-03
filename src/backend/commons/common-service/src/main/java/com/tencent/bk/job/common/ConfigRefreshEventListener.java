package com.tencent.bk.job.common;

import com.tencent.bk.job.common.util.feature.FeatureToggle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.StringJoiner;

@Component
@Slf4j
public class ConfigRefreshEventListener {
    public ConfigRefreshEventListener() {
        log.info("Init ConfigRefreshEventListener");
    }

    /**
     * 监听并处理Spring cloud 配置更新事件(通过/actuator/refresh 和 /actuator/busrefresh endpoint 触发)
     *
     * @param event 配置更新事件
     */
    @EventListener
    public void onEvent(EnvironmentChangeEvent event) {
        if (log.isInfoEnabled()) {
            log.info("Handle EnvironmentChangeEvent, event: {}", printEnvironmentChangeEvent(event));
        }
        reloadFeatureToggle();
    }

    private String printEnvironmentChangeEvent(EnvironmentChangeEvent event) {
        return new StringJoiner(", ", EnvironmentChangeEvent.class.getSimpleName() + "[", "]")
            .add("source=" + event.getSource())
            .add("timestamp=" + event.getTimestamp())
            .add("keys=" + event.getKeys())
            .toString();
    }


    /**
     * 重载特型开关配置
     */
    private void reloadFeatureToggle() {
        log.info("Reload feature toggle start ..");
        FeatureToggle.getInstance().reload();
        log.info("Reload feature toggle successfully");
    }
}
