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

package com.tencent.bk.job.k8s.external;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.constant.HttpMethodEnum;
import com.tencent.bk.job.common.util.http.HttpHelper;
import com.tencent.bk.job.common.util.http.HttpHelperFactory;
import com.tencent.bk.job.common.util.http.HttpRequest;
import com.tencent.bk.job.common.util.http.HttpResponse;
import com.tencent.bk.job.common.util.http.RetryModeEnum;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;

/**
 * 基于Http实现的服务外部依赖检查器
 */
@Slf4j
public class HttpServiceExternalDependencyChecker implements IServiceExternalDependencyCheck {

    /**
     * 用于检查的目标URL根地址
     */
    private final String baseUrl;
    /**
     * Http请求工具
     */
    private final HttpHelper httpHelper;

    public HttpServiceExternalDependencyChecker(String baseUrl) {
        this.baseUrl = checkAndGetBaseUrl(baseUrl);
        this.httpHelper = HttpHelperFactory.getDefaultHttpHelper();
    }

    /**
     * 检查URL是否合法并返回清洗后的值
     *
     * @param baseUrl 目标URL根地址
     * @return 清洗后的URL根地址
     */
    private String checkAndGetBaseUrl(String baseUrl) {
        if (StringUtils.isBlank(baseUrl)) {
            throw new IllegalArgumentException("baseUrl can not be blank");
        }
        return baseUrl.trim();
    }

    /**
     * 检查服务外部依赖是否已就绪
     *
     * @param namespace   服务所在命名空间
     * @param serviceName 服务名称
     * @return 服务是否已就绪
     */
    @Override
    public boolean isReady(String namespace, String serviceName) {
        String checkUrl = buildCheckUrl(namespace, serviceName);
        HttpRequest httpRequest = HttpRequest.builder(HttpMethodEnum.GET, checkUrl)
            .setKeepAlive(true)
            .setRetryMode(RetryModeEnum.NEVER)
            .build();
        try {
            HttpResponse response = httpHelper.request(httpRequest);
            int statusCode = response.getStatusCode();
            int statusOk = 200;
            if (statusCode != statusOk) {
                log.warn(
                    "statusCode({}) of check resp is not 200, external dependency of service {} not ready",
                    statusCode,
                    serviceName
                );
                return false;
            }
            String respBody = response.getEntity();
            log.info("Check service external dependency, respBody={}", respBody);
            CheckStatusResp resp = JsonUtils.fromJson(respBody, new TypeReference<CheckStatusResp>() {
            });
            return resp.isReady();
        } catch (Throwable t) {
            String message = MessageFormatter.format(
                "Fail to check service external dependency, namespace={}, serviceName={}",
                namespace,
                serviceName
            ).getMessage();
            log.warn(message, t);
            return false;
        }
    }

    /**
     * 构建完整的检查URL
     *
     * @param namespace   命名空间
     * @param serviceName 服务名称
     * @return 完整的检查URL
     */
    private String buildCheckUrl(String namespace, String serviceName) {
        return baseUrl
            + "?namespace=" + namespace +
            "&serviceName=" + serviceName;
    }
}
