package com.tencent.bk.job.common;

import com.tencent.bk.job.common.util.feature.FeatureToggle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ConfigRefreshEventListener {
    public ConfigRefreshEventListener() {
        log.info("Init ConfigRefreshEventListener");
    }
    /**
     * 监听并处理配置更新事件
     *
     * @param event 配置更新事件
     */
    @EventListener
    public void onEvent(EnvironmentChangeEvent event) {
        log.info("Handle EnvironmentChangeEvent, event: {}", event);
        log.info("Reload feature toggle start ..");
        FeatureToggle.getInstance().reload();
        log.info("Reload feature toggle successfully");
    }
}
