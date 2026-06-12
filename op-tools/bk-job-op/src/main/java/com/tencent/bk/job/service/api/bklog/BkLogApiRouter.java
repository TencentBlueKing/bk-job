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

package com.tencent.bk.job.service.api.bklog;

import com.tencent.bk.job.config.BkApiGwProperties;
import com.tencent.bk.job.config.BkLogProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * BkLog API 路由器：按 endpoint 名称分发到对应的 {@link BkLogApi} 实例。
 * <p>
 * 启动时根据 {@link BkLogProperties#getEndpoints()} 一次性构造好所有
 * 接入点对应的 {@link RetryableBklogApi}，运行期 O(1) 查找。
 * <p>
 * 同时基于旧配置 {@code bk-api-gateway.bkLog} 注册一个名为
 * {@link BkLogProperties#DEFAULT_ENDPOINT} 的兜底接入点，
 * 用于兼容未显式声明 endpoint 的 LogSource。
 */
@Slf4j
@Component
public class BkLogApiRouter {

    private final Map<String, BkLogApi> apis = new HashMap<>();

    public BkLogApiRouter(RestTemplate restTemplate,
                          BkApiGwProperties bkApiGwProperties,
                          BkLogProperties bkLogProperties) {
        // 1) 注册 yml 中显式声明的所有 endpoints
        if (bkLogProperties.getEndpoints() != null) {
            for (Map.Entry<String, BkLogProperties.Endpoint> entry
                : bkLogProperties.getEndpoints().entrySet()) {
                String name = entry.getKey();
                BkLogProperties.Endpoint ep = entry.getValue();
                apis.put(name, build(restTemplate, ep, bkApiGwProperties));
            }
        }

        // 2) 兼容兜底：用旧 bk-api-gateway.bkLog 拼一个名为 default 的 endpoint
        if (!apis.containsKey(BkLogProperties.DEFAULT_ENDPOINT)
            && bkApiGwProperties.getBkLog() != null
            && StringUtils.isNotBlank(bkApiGwProperties.getBkLog().getUrl())) {
            apis.put(BkLogProperties.DEFAULT_ENDPOINT, buildFromLegacy(restTemplate, bkApiGwProperties));
        }

        log.info("BkLogApiRouter initialized, endpoints: {}", apis.keySet());
    }

    /**
     * 根据 endpoint 名称获取对应的 {@link BkLogApi} 实例。
     * 入参为空时回退到 {@link BkLogProperties#DEFAULT_ENDPOINT}。
     */
    public BkLogApi get(String endpointName) {
        String key = StringUtils.isBlank(endpointName) ? BkLogProperties.DEFAULT_ENDPOINT : endpointName;
        BkLogApi api = apis.get(key);
        if (api == null) {
            throw new IllegalArgumentException(
                "未知的日志接入点: " + key + "，可选接入点: " + apis.keySet()
            );
        }
        return api;
    }

    private BkLogApi build(RestTemplate restTemplate,
                           BkLogProperties.Endpoint ep,
                           BkApiGwProperties apiGwDefault) {
        // 兜底：endpoint 没填 appCode/secret 时，使用 bk-api-gateway 全局默认
        String appCode = StringUtils.defaultIfBlank(ep.getBkAppCode(), apiGwDefault.getBkAppCode());
        String appSecret = StringUtils.defaultIfBlank(ep.getBkAppSecret(), apiGwDefault.getBkAppSecret());
        String username = StringUtils.defaultIfBlank(ep.getUsername(), "admin");
        String tenantId = StringUtils.defaultIfBlank(ep.getTenantId(), "default");
        return new RetryableBklogApi(
            restTemplate,
            ep.getUrl(),
            appCode,
            appSecret,
            username,
            tenantId,
            ep.getRetryCount() == null ? 1 : ep.getRetryCount(),
            ep.getRetryInterval() == null ? 5 : ep.getRetryInterval()
        );
    }

    private BkLogApi buildFromLegacy(RestTemplate restTemplate, BkApiGwProperties apiGwProperties) {
        BkApiGwProperties.ApiGwConfig legacy = apiGwProperties.getBkLog();
        String appCode = StringUtils.defaultIfBlank(legacy.getBkAppCode(), apiGwProperties.getBkAppCode());
        String appSecret = StringUtils.defaultIfBlank(legacy.getBkAppSecret(), apiGwProperties.getBkAppSecret());
        String username = StringUtils.defaultIfBlank(legacy.getUsername(), "admin");
        return new RetryableBklogApi(
            restTemplate,
            legacy.getUrl(),
            appCode,
            appSecret,
            username,
            "default",
            legacy.getRetryCount() == null ? 1 : legacy.getRetryCount(),
            legacy.getRetryInterval() == null ? 5 : legacy.getRetryInterval()
        );
    }
}
