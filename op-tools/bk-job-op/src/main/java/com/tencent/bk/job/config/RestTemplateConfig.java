/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.bk.job.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Value("${http.client.connect-timeout:10}")
    private int connectTimeout;

    @Value("${http.client.read-timeout:60}")
    private int readTimeout;

    /**
     * 通用RestTemplate。容器内存在多个RestTemplate时，按类型注入的已有代码（如BaseApi）应当拿到这一个，
     * 因此标记为@Primary
     */
    @Bean
    @Primary
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(connectTimeout));
        requestFactory.setReadTimeout(Duration.ofSeconds(readTimeout)); // 日志查询耗时较长，这里给个大一点的readTimeout
        
        return new RestTemplate(requestFactory);
    }

    /**
     * 登录态校验专用RestTemplate。
     *
     * <p>不复用通用RestTemplate：它的readTimeout是为日志查询留的60秒，而登录校验发生在每个Web请求的前置链路上，
     * 登录服务一旦卡顿就会把Tomcat工作线程挂住，进而拖垮OP全部接口。</p>
     */
    @Bean("loginRestTemplate")
    public RestTemplate loginRestTemplate(
        @Value("${job.op.web.auth.login.verify.connect-timeout-ms:2000}") int loginConnectTimeoutMs,
        @Value("${job.op.web.auth.login.verify.read-timeout-ms:3000}") int loginReadTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(loginConnectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(loginReadTimeoutMs));

        return new RestTemplate(requestFactory);
    }
}
