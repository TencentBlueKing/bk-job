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

package com.tencent.bk.job.gateway.runner;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.gateway.config.BkConfig;
import com.tencent.bk.job.gateway.model.LicenseCheckResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.joda.time.DateTime;
import org.springframework.beans.BeansException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class LicenseCheckRunner implements CommandLineRunner, ApplicationContextAware {

    private ApplicationContext context;

    private final BkConfig bkConfig;

    private CloseableHttpClient httpClient;

    public LicenseCheckRunner(BkConfig bkConfig) {
        this.bkConfig = bkConfig;
    }

    @PostConstruct
    public void init() throws Exception {
        if (httpClient == null) {
            X509TrustManager tm = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] xcs, String string) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] xcs, String string) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };

            HostnameVerifier hostnameVerifier = (arg0, arg1) -> true;

            try {
                SSLContext ctx = SSLContext.getInstance("SSL");
                ctx.init(null, new TrustManager[]{tm}, null);

                httpClient = HttpClientBuilder.create()
                    .setDefaultConnectionConfig(
                        ConnectionConfig.custom()
                            .setBufferSize(8192)
                            .setCharset(org.apache.http.Consts.UTF_8)
                            .build())
                    .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(15000)
                        .setConnectTimeout(15000)
                        .setSocketTimeout(15000).build())
                    .setConnectionTimeToLive(180, TimeUnit.SECONDS)
                    .disableAutomaticRetries()
                    .disableAuthCaching()
                    .disableCookieManagement().setSSLContext(ctx)
                    .setSSLHostnameVerifier(hostnameVerifier).build();
            } catch (Exception e) {
                log.error("Init license check http client fail", e);
                throw e;
            }
        }
    }

    private void checkLicence() {
        LicenseCheckResultDTO licenseCheckResult = getLicenceCheckResult();
        if (licenseCheckResult.isOk()) {
            // License有效，一直保持至下次重启进程
            log.info("Check license on start finished, license is ok");
        } else {
            log.error("Fail to check license, please check whether the license server is available");
            ConfigurableApplicationContext ctx = (ConfigurableApplicationContext) context;
            ctx.close();
        }
    }


    private LicenseCheckResultDTO getLicenceCheckResult() {
        int maxRetryTimes = bkConfig.getLicenseCheckRetryTimes();
        int retryInterval = bkConfig.getLicenseCheckRetryInterval();

        int retryTimes = 0;
        LicenseCheckResultDTO licenseCheckResult = new LicenseCheckResultDTO();
        while (true) {
            try {
                HttpPost httpPost = new HttpPost(bkConfig.getLicenseCheckServiceUrl());
                httpPost.setHeader("Content-Type", "application/json");
                String requestBody = buildCheckRequestBody();
                httpPost.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));

                String responseBody = httpClient.execute(httpPost, new BasicResponseHandler());
                log.info("Check license, resp={}", responseBody);
                if (StringUtils.isNotBlank(responseBody)) {
                    licenseCheckResult = JsonUtils.fromJson(responseBody, LicenseCheckResultDTO.class);
                    if (licenseCheckResult != null && !licenseCheckResult.isStatus()) {
                        log.error("license is invalid, please check license server configuration");
                    }
                } else {
                    createFailCheck(licenseCheckResult);
                }
                break;
            } catch (IOException e) {
                retryTimes++;
                if (retryTimes <= maxRetryTimes) {
                    log.error("Exception while getting license! Retrying...|{}", retryTimes);
                    try {
                        Thread.sleep(1000 * retryInterval);
                    } catch (InterruptedException ignored) {
                        log.info("sleep interrupted");
                    }
                } else {
                    createFailCheck(licenseCheckResult);
                    log.error("Check license retry exceed max-retry-times:{}!", maxRetryTimes);
                    break;
                }
            } catch (Throwable e) {
                createFailCheck(licenseCheckResult);
                log.error("Check license failed", e);
                break;
            }
        }
        //证书校验是否通过
        if (licenseCheckResult != null) {
            licenseCheckResult.setOk(licenseCheckResult.isStatus() && licenseCheckResult.getResult() == 0);
        }
        return licenseCheckResult;
    }

    private void createFailCheck(LicenseCheckResultDTO licenseCheckDto) {
        licenseCheckDto.setStatus(false);
        licenseCheckDto.setMessage("License Server unreachable");
        licenseCheckDto.setResult(ErrorCode.LICENSE_ERROR);
    }

    private String buildCheckRequestBody() throws IOException {
        String licenseFilePath = bkConfig.getLicenseFilePath();
        String certContent = FileUtils.readFileToString(new File(licenseFilePath), "UTF-8");

        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("platform", "job");
        requestParams.put("time", DateTime.now().toString("yyyy-MM-dd HH:mm:ss"));
        requestParams.put("certificate", certContent);
        return JsonUtils.toJson(requestParams);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    @Override
    public void run(String... args) throws Exception {
        if (bkConfig.isEnableLicenseValidate()) {
            checkLicence();
        } else {
            log.info("License validation disabled!");
        }
    }
}
