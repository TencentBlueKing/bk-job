package com.tencent.bk.job.gateway.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ToString
@Getter
@Setter
@ConfigurationProperties(prefix = "job.gateway.sub-path")
public class SubPathProperties {
    /**
     * 是否开启
     */
    private boolean enabled = false;
    /**
     * 根路径前缀
     */
    private String rootPrefix = "/job";
}
