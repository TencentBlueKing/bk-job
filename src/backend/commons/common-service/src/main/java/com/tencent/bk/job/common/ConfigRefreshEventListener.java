package com.tencent.bk.job.common;

import com.tencent.bk.job.common.util.feature.FeatureToggle;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.StringJoiner;

/**
 * 配置刷新监听
 */
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
        reloadFeatureToggleIfChanged(event.getKeys());
    }

    private String printEnvironmentChangeEvent(EnvironmentChangeEvent event) {
        return new StringJoiner(", ", EnvironmentChangeEvent.class.getSimpleName() + "[", "]")
            .add("source=" + event.getSource())
            .add("timestamp=" + event.getTimestamp())
            .add("keys=" + event.getKeys())
            .toString();
    }


    /**
     * 重载特性开关配置
     */
    private void reloadFeatureToggleIfChanged(Set<String> changedKeys) {
        if (CollectionUtils.isEmpty(changedKeys)) {
            return;
        }
        boolean isFeatureToggleConfigChanged =
            changedKeys.stream().anyMatch(changedKey -> changedKey.startsWith("job.features."));
        if (isFeatureToggleConfigChanged) {
            FeatureToggle.reload();
        }
    }
}
