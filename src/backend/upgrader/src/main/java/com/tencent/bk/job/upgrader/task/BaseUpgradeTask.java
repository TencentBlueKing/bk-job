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

package com.tencent.bk.job.upgrader.task;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.job.common.util.http.BaseHttpHelper;
import com.tencent.bk.job.common.util.http.ExtHttpHelper;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.upgrader.anotation.UpgradeTask;
import com.tencent.bk.job.upgrader.task.param.ParamNameConsts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.helpers.MessageFormatter;

import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

@Slf4j
public abstract class BaseUpgradeTask implements IUpgradeTask {

    private Properties properties;
    private static final ExtHttpHelper HTTP_HELPER;

    static {
        HTTP_HELPER = new ExtHttpHelper(new BaseHttpHelper(getHttpClient()));
    }

    BaseUpgradeTask() {

    }

    BaseUpgradeTask(Properties properties) {
        this.properties = properties;
    }

    public Properties getProperties() {
        return properties;
    }

    @Override
    public void init() {
    }

    public <T> Response<T> post(String url, String content) throws InternalException {
        String jobAuthToken = getProperties().getProperty(
            ParamNameConsts.CONFIG_PROPERTY_JOB_SECURITY_PUBLIC_KEY_BASE64
        );
        if (StringUtils.isBlank(jobAuthToken)) {
            log.error("{} is not configured", ParamNameConsts.CONFIG_PROPERTY_JOB_SECURITY_PUBLIC_KEY_BASE64);
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, new String[]{
                ParamNameConsts.CONFIG_PROPERTY_JOB_SECURITY_PUBLIC_KEY_BASE64
            });
        }
        jobAuthToken = Base64Util.decodeContentToStr(jobAuthToken);

        Header[] headers = new Header[2];
        headers[0] = new BasicHeader("x-job-auth-token", jobAuthToken);
        headers[1] = new BasicHeader("Content-Type", "application/json");

        try {
            String respStr = HTTP_HELPER.post(url, content, headers);
            log.info("Post {}, content: {}, response: {}", url, content, respStr);
            if (StringUtils.isBlank(respStr)) {
                String errorMsg =
                    MessageFormatter.format("Fail: response is blank|url={}", url).getMessage();
                log.error(errorMsg);
                throw new InternalException(errorMsg, ErrorCode.INTERNAL_ERROR);
            }
            Response<T> resp = JsonUtils.fromJson(respStr,
                new TypeReference<Response<T>>() {
                });
            if (resp == null) {
                String errorMsg =
                    MessageFormatter.format("Fail: parse response|url={}", url).getMessage();
                throw new InternalException(errorMsg, ErrorCode.INTERNAL_ERROR);
            }
            return resp;
        } catch (Exception e) {
            log.error("Fail: caught exception", e);
            throw new InternalException(e, ErrorCode.INTERNAL_ERROR);
        }
    }

    protected final String buildMigrationTaskUrl(String serviceUrl, String migrationTaskUri) {
        String url;
        if (!serviceUrl.endsWith("/")) {
            url = serviceUrl + migrationTaskUri;
        } else {
            url = serviceUrl.substring(0, serviceUrl.length() - 1) + migrationTaskUri;
        }
        return url;
    }

    protected final String getJobManageUrl() {
        String serverUrl = getProperties().getProperty(
            ParamNameConsts.INPUT_PARAM_JOB_MANAGE_SERVER_ADDRESS
        );
        return addMissingSchema(serverUrl);
    }

    protected final String getJobCrontabUrl() {
        String serverUrl = getProperties().getProperty(
            ParamNameConsts.INPUT_PARAM_JOB_CRONTAB_SERVER_ADDRESS
        );
        return addMissingSchema(serverUrl);
    }

    private String addMissingSchema(String url) {
        String newUrl = url;
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            newUrl = "http://" + url;
        }
        return newUrl;
    }


    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getDataStartVersion() {
        return this.getClass().getAnnotation(UpgradeTask.class).dataStartVersion();
    }

    @Override
    public String getTargetVersion() {
        return this.getClass().getAnnotation(UpgradeTask.class).targetVersion();
    }

    @Override
    public int getPriority() {
        return this.getClass().getAnnotation(UpgradeTask.class).priority();
    }

    private static CloseableHttpClient getHttpClient() {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create()
            .setDefaultConnectionConfig(
                ConnectionConfig.custom()
                    .setBufferSize(102400)
                    .setCharset(StandardCharsets.UTF_8)
                    .build()
            ).setDefaultRequestConfig(
                RequestConfig.custom()
                    .setConnectTimeout(15000)
                    // 12h,等待足够长时间，等待升级任务返回结果
                    .setSocketTimeout(43200000)
                    .build()
            )
            .disableAutomaticRetries();

        CloseableHttpClient httpClient;
        try {
            httpClient = httpClientBuilder.setSSLSocketFactory(
                new SSLConnectionSocketFactory(
                    SSLContexts.custom()
                        .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                        .build()
                )
            ).build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            log.error("Set ssl config error", e);
            httpClient = httpClientBuilder.build();
        }
        return httpClient;
    }
}
