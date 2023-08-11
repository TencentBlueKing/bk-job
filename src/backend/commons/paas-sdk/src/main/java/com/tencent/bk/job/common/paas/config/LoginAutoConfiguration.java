/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.common.paas.config;

import com.tencent.bk.job.common.esb.config.AppProperties;
import com.tencent.bk.job.common.esb.config.EsbProperties;
import com.tencent.bk.job.common.i18n.locale.LocaleUtils;
import com.tencent.bk.job.common.paas.login.CustomLoginClient;
import com.tencent.bk.job.common.paas.login.EELoginClient;
import com.tencent.bk.job.common.paas.login.ILoginClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@Configuration(proxyBeanMethods = false)
@Slf4j
@Import(LoginConfiguration.class)
public class LoginAutoConfiguration {

    @Bean(name = "enLoginClient")
    @ConditionalOnProperty(value = "paas.login.custom.enabled", havingValue = "true")
    public ILoginClient innerEnLoginClient(@Autowired LoginConfiguration loginConfiguration) {
        log.info("Init custom en login client");
        return new CustomLoginClient(loginConfiguration.getCustomLoginApiUrl());
    }

    @Bean(name = "cnLoginClient")
    @ConditionalOnProperty(value = "paas.login.custom.enabled", havingValue = "true")
    public ILoginClient innerCnLoginClient(@Autowired LoginConfiguration loginConfiguration) {
        log.info("Init custom cn login client");
        return new CustomLoginClient(loginConfiguration.getCustomLoginApiUrl());
    }

    @Bean(name = "enLoginClient")
    @ConditionalOnProperty(value = "paas.login.custom.enabled", havingValue = "false", matchIfMissing = true)
    @Primary
    public ILoginClient enLoginClient(AppProperties appProperties, EsbProperties esbProperties) {
        log.info("Init standard en login client");
        return new EELoginClient(
            esbProperties.getService().getUrl(),
            appProperties.getCode(),
            appProperties.getSecret(),
            LocaleUtils.LANG_EN
        );
    }

    @Bean(name = "cnLoginClient")
    @ConditionalOnProperty(value = "paas.login.custom.enabled", havingValue = "false", matchIfMissing = true)
    @Primary
    public ILoginClient cnLoginClient(AppProperties appProperties, EsbProperties esbProperties) {
        log.info("Init standard cn login client");
        return new EELoginClient(
            esbProperties.getService().getUrl(),
            appProperties.getCode(),
            appProperties.getSecret(),
            LocaleUtils.LANG_ZH_CN
        );
    }
}
