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

package com.tencent.bk.job.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
public class BkConfig {
    //---------------------------- Esb Config ---------------------------------
    /**
     * 是否对接ESB测试环境,内部环境使用
     */
    @Value("${esb.use.test.env:false}")
    private boolean useEsbTestEnv;
    /**
     * 应用code
     */
    @Value("${app.code}")
    private String appCode;

    /**
     * ESB App Secret
     */
    @Value("${app.secret}")
    private String appSecret;

    /**
     * ESB service url
     */
    @Value("${esb.service.url:}")
    private String esbUrl;

    //---------------------------- Login Config ---------------------------------
    /**
     * 蓝鲸标准的登录url
     */
    @Value("${paas.login.url:}")
    private String loginUrl;

    /**
     * 是否使用第三方登录系统
     */
    @Value("${paas.login.custom.enabled:false}")
    private boolean customPaasLoginEnabled;

    /**
     * 第三方登录系统用户token的cookie名称
     */
    @Value("${paas.login.custom.token-name:bk_token}")
    private String customLoginToken;

    /**
     * 第三方登录系统登录url
     */
    @Value("${paas.login.custom.login-url:}")
    private String customLoginUrl;

    /**
     * 第三方登录系统API url，用于根据token获取用户信息
     */
    @Value("${paas.login.custom.api-url:}")
    private String customLoginApiUrl;


    //---------------------------- Job Config ---------------------------------
    @Value("${job.edition:ee}")
    private String jobEdition;

    /**
     * 作业平台web访问地址，可配置多个，用","分隔
     */
    @Value("${job.web.url:}")
    private String jobWebUrl;

    @Value("${swagger.url:swagger.job.com}")
    private String swaggerUrl;

    //---------------------------- License Config ---------------------------------
    /**
     * license 校验服务url
     */
    @Value("${license.enabled:false}")
    private boolean enableLicenseValidate;
    /**
     * license 校验服务url
     */
    @Value("${license.service.url:}")
    private String licenseCheckServiceUrl;

    /**
     * license 校验服务url
     */
    @Value("${license.file:}")
    private String licenseFilePath;

    /**
     * license 校验执行次数
     */
    @Value("${license.check.retry-times:3}")
    private Integer licenseCheckRetryTimes;

    /**
     * license 校验重试间隔，单位秒
     */
    @Value("${license.check.retry-interval:1}")
    private Integer licenseCheckRetryInterval;


}
