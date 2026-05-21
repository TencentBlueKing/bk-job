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

package com.tencent.bk.job.execute.service.validation.impl;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.tencent.bk.job.common.config.BkConfig;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.execute.config.CheckCallbackUrlConfig;
import com.tencent.bk.job.execute.dao.CallbackUrlWhiteInfoDAO;
import com.tencent.bk.job.execute.service.validation.CallbackUrlValidateService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CallbackUrlValidateServiceImpl implements CallbackUrlValidateService {

    private static final String CACHE_KEY = "ALL";

    private final CheckCallbackUrlConfig config;
    private final BkConfig bkConfig;
    private final CallbackUrlWhiteInfoDAO callbackUrlWhiteInfoDAO;

    /**
     * DB 白名单缓存：全量 baseUrl 列表，TTL 由 config.dbCacheTtlSeconds 决定（默认 60s）
     */
    private LoadingCache<String, List<String>> dbBaseUrlCache;

    @Autowired
    public CallbackUrlValidateServiceImpl(CheckCallbackUrlConfig config,
                                          BkConfig bkConfig,
                                          CallbackUrlWhiteInfoDAO callbackUrlWhiteInfoDAO) {
        this.config = config;
        this.bkConfig = bkConfig;
        this.callbackUrlWhiteInfoDAO = callbackUrlWhiteInfoDAO;
    }

    @PostConstruct
    private void init() {
        int ttl = config.getDbCacheTtlSeconds() <= 0 ? 60 : config.getDbCacheTtlSeconds();
        this.dbBaseUrlCache = Caffeine.newBuilder()
            .maximumSize(1)
            .expireAfterWrite(ttl, TimeUnit.SECONDS)
            .build(key -> {
                try {
                    List<String> baseUrls = callbackUrlWhiteInfoDAO.listAllBaseUrls();
                    return baseUrls == null ? Collections.emptyList() : baseUrls;
                } catch (Exception e) {
                    log.warn("Load callback url whitelist from DB failed, use empty list", e);
                    return Collections.emptyList();
                }
            });
        log.info("CallbackUrlValidateService initialized: enabled={}, allowedBaseUrls={}, dbCacheTtlSeconds={}",
            config.isEnabled(), config.getAllowedBaseUrls(), ttl);
    }

    @Override
    public boolean isValid(String callbackUrl) {
        if (StringUtils.isBlank(callbackUrl)) {
            // 空值由外层 @NotBlank 等控制，本 Service 不在此报错（视为通过）
            return true;
        }
        // 1. 基本合法性校验
        URI uri = parseUri(callbackUrl);
        if (uri == null) {
            return false;
        }
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
            || StringUtils.isBlank(host)) {
            return false;
        }
        // 全局拒绝包含 userinfo 的 URL：阻断 https://trusted.com@evil.com/x 类 SSRF 绕过。
        // 合法回调不应携带 userinfo，统一在此拦截
        if (uri.getRawUserInfo() != null) {
            return false;
        }
        // 2. 关闭白名单校验时，仅做合法性校验
        if (!config.isEnabled()) {
            return true;
        }
        // 3. 命中配置白名单 baseUrl
        if (matchAnyBaseUrl(uri, config.getAllowedBaseUrls())) {
            return true;
        }
        // 4. 命中当前环境 bkDomain 子域
        if (isHostOfCurrentEnv(host)) {
            return true;
        }
        // 5. 命中 DB 白名单 baseUrl（带缓存）
        List<String> dbBaseUrls = dbBaseUrlCache.get(CACHE_KEY);
        return matchAnyBaseUrl(uri, dbBaseUrls);
    }

    @Override
    public void validateWhitelistBaseUrl(String baseUrl) {
        if (StringUtils.isBlank(baseUrl)
            || !(baseUrl.startsWith("http://") || baseUrl.startsWith("https://"))) {
            throw new InvalidParamException(
                ErrorCode.CALLBACK_URL_WHITELIST_INVALID_BASE_URL, baseUrl);
        }
        // 还要保证后续的字符不是空：至少要有 host
        URI uri = parseUri(baseUrl);
        if (uri == null || StringUtils.isBlank(uri.getHost())) {
            throw new InvalidParamException(
                ErrorCode.CALLBACK_URL_WHITELIST_INVALID_BASE_URL, baseUrl);
        }
        // baseUrl 不允许带 userinfo / query / fragment：这些片段会让匹配语义模糊，
        // 也提供了 https://trusted.com@evil.com 这类构造空间
        if (uri.getRawUserInfo() != null
            || StringUtils.isNotEmpty(uri.getRawQuery())
            || StringUtils.isNotEmpty(uri.getRawFragment())) {
            throw new InvalidParamException(
                ErrorCode.CALLBACK_URL_WHITELIST_INVALID_BASE_URL, baseUrl);
        }
    }

    @Override
    public void invalidateCache() {
        if (dbBaseUrlCache != null) {
            dbBaseUrlCache.invalidateAll();
        }
    }

    /**
     * 判定 host 是否为当前环境 bkDomain 或其子域。
     */
    private boolean isHostOfCurrentEnv(String host) {
        String bkDomain = bkConfig == null ? null : bkConfig.getBkDomain();
        if (StringUtils.isBlank(bkDomain) || StringUtils.isBlank(host)) {
            return false;
        }
        String trimmedDomain = bkDomain.trim();
        return host.equalsIgnoreCase(trimmedDomain)
            || host.toLowerCase().endsWith("." + trimmedDomain.toLowerCase());
    }

    /**
     * 用 URI 解析做精确匹配：scheme + host + 端口必须等同，path 以 / 作为边界做前缀匹配。
     *
     * <p>避免下列两类典型绕过：
     * <ul>
     *   <li>userinfo 注入：{@code https://trusted.com@evil.com/x}（已在 {@link #isValid} 入口
     *       全局拦截，此处再次防御深度兜底）；</li>
     *   <li>后缀混淆：{@code https://trusted.com.evil.com/x} 不会被 baseUrl
     *       {@code https://trusted.com/} 命中，因为 host 必须严格等值。</li>
     * </ul>
     */
    private boolean matchAnyBaseUrl(URI callbackUri, List<String> baseUrls) {
        if (baseUrls == null || baseUrls.isEmpty()) {
            return false;
        }
        for (String baseUrl : baseUrls) {
            if (StringUtils.isBlank(baseUrl)) {
                continue;
            }
            URI baseUri = parseUri(baseUrl.trim());
            if (baseUri == null) {
                continue;
            }
            if (matchBaseUrl(callbackUri, baseUri)) {
                return true;
            }
        }
        return false;
    }

    /**
     * scheme / host 大小写不敏感等值；port 按协议默认端口归一化后等值；
     * path 用 / 边界做前缀匹配，避免 {@code /foo} 命中 {@code /foobar}。
     */
    private boolean matchBaseUrl(URI callbackUri, URI baseUri) {
        if (!equalsIgnoreCase(callbackUri.getScheme(), baseUri.getScheme())) {
            return false;
        }
        if (!equalsIgnoreCase(callbackUri.getHost(), baseUri.getHost())) {
            return false;
        }
        if (effectivePort(callbackUri) != effectivePort(baseUri)) {
            return false;
        }
        // baseUri 不允许带 userinfo（validateWhitelistBaseUrl 已校验，此处再防御一次）
        if (baseUri.getRawUserInfo() != null) {
            return false;
        }
        String basePath = baseUri.getRawPath() == null ? "" : baseUri.getRawPath();
        String callbackPath = callbackUri.getRawPath() == null ? "" : callbackUri.getRawPath();
        // baseUrl 仅到 host 级别（path 为空或 /）：同 host 下任意 path 都允许
        if (basePath.isEmpty() || "/".equals(basePath)) {
            return true;
        }
        if (callbackPath.equals(basePath)) {
            return true;
        }
        // 用 / 作为边界做前缀匹配：basePath 若不以 / 结尾，要求 callback 形如 basePath + "/..."
        // 这样既支持 "https://api/x/" 也支持 "https://api/x"，且都不会命中 "https://api/xyz"
        if (basePath.endsWith("/")) {
            return callbackPath.startsWith(basePath);
        }
        return callbackPath.startsWith(basePath + "/");
    }

    private static boolean equalsIgnoreCase(String a, String b) {
        return a == null ? b == null : a.equalsIgnoreCase(b);
    }

    private static int effectivePort(URI uri) {
        int port = uri.getPort();
        if (port != -1) {
            return port;
        }
        if ("http".equalsIgnoreCase(uri.getScheme())) {
            return 80;
        }
        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return 443;
        }
        return -1;
    }

    private URI parseUri(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
