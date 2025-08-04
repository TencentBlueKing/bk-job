package com.tencent.bk.job.gateway.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "job.gateway.csrf-check")
@NoArgsConstructor
public class CsrfCheckProperties {
    /**
     * 是否开启CSRF校验，默认开启
     */
    private boolean enabled = true;
}
