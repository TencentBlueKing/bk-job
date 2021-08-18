package com.tencent.bk.job.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
public class LoginExemptionConfig {
    /**
     * 是否开启登录豁免
     */
    @Value("${job.gateway.login-exemption.enable:false}")
    private boolean enableLoginExemption;
    /**
     * 开启登录豁免后的默认用户名
     */
    @Value("${job.gateway.login-exemption.default-user:admin}")
    private String defaultUser;
}
