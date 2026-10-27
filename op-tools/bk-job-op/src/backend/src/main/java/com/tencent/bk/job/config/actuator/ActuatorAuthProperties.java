/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 */

package com.tencent.bk.job.config.actuator;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Actuator HTTP Basic认证配置。
 */
@Data
@ConfigurationProperties(prefix = "job.op.actuator.auth")
public class ActuatorAuthProperties {

    private String username;

    private String password;

    @PostConstruct
    void validate() {
        boolean hasUsername = StringUtils.hasText(username);
        boolean hasPassword = StringUtils.hasText(password);
        if (hasUsername != hasPassword) {
            throw new IllegalStateException(
                "job.op.actuator.auth.username and job.op.actuator.auth.password must be configured together"
            );
        }
    }

    public boolean isConfigured() {
        return StringUtils.hasText(username) && StringUtils.hasText(password);
    }
}
