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

package com.tencent.bk.job.execute.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * GSE配置
 */
@Configuration("gseConfig")
@Getter
@Setter
@ToString(exclude = {"gseSSLKeystorePassword", "gseSSLTruststorePassword"})
public class GseConfig {
    @Value("${gse.taskserver.host:}")
    private String taskServerHost;

    @Value("${gse.taskserver.port:48673}")
    private int taskServerPort;

    @Value("${gse.cache.apiserver.host:}")
    private String gseCacheApiServerHost;

    @Value("${gse.cache.apiserver.port:59313}")
    private int gseCacheApiServerPort;

    @Value("${gse.ssl.enable:true}")
    private boolean gseSSLEnable;

    @Value("${gse.ssl.keystore.path:}")
    private String gseSSLKeystore;

    @Value("${gse.ssl.keystore.password:}")
    private String gseSSLKeystorePassword;

    @Value("${gse.ssl.truststore.path:}")
    private String gseSSLTruststore;

    @Value("${gse.ssl.truststore.password:}")
    private String gseSSLTruststorePassword;

    @Value("${gse.ssl.truststore.manager-type:SunX509}")
    private String gseSSLTruststoreManagerType;

    @Value("${gse.ssl.truststore.store-type:JKS}")
    private String gseSSLTruststoreStoreType;

    @Value("${gse.query.threads.num:5}")
    private int gseQueryThreadsNum;

    @Value("${gse.query.batchSize:5000}")
    private int gseQueryBatchSize;
}
