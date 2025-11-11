package com.tencent.bk.job.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "job.gateway.user-map")
public class UserMapProperties {
    /**
     * 是否开启
     */
    private boolean enabled = false;
    /**
     * 用户名映射
     */
    private Map<String, String> map = new HashMap<>();
}
