/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 */

package com.tencent.bk.job.config.actuator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ActuatorAuthPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    @DisplayName("用户名和密码都为空时允许启动且保持未配置状态")
    void shouldStartWhenBothCredentialsAreEmpty() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(ActuatorAuthProperties.class).isConfigured()).isFalse();
        });
    }

    @Test
    @DisplayName("只配置用户名时启动失败")
    void shouldFailWhenOnlyUsernameIsConfigured() {
        contextRunner
            .withPropertyValues("job.op.actuator.auth.username=monitor")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasRootCauseMessage(
                        "job.op.actuator.auth.username and job.op.actuator.auth.password must be configured together"
                    );
            });
    }

    @Test
    @DisplayName("只配置密码时启动失败")
    void shouldFailWhenOnlyPasswordIsConfigured() {
        contextRunner
            .withPropertyValues("job.op.actuator.auth.password=synthetic-password")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasRootCauseMessage(
                        "job.op.actuator.auth.username and job.op.actuator.auth.password must be configured together"
                    );
            });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ActuatorAuthProperties.class)
    static class PropertiesConfiguration {
    }
}
