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

package com.tencent.bk.job.service.login.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.Ticker;
import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.config.web.WebAuthProperties;
import com.tencent.bk.job.service.login.BkTicketLoginService;
import com.tencent.bk.job.service.login.LoginServiceUnavailableException;
import com.tencent.bk.job.utils.json.JsonUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Collections;
import java.util.Set;

/**
 * 基于蓝鲸统一登录服务的登录态校验实现。
 *
 * <p>协议与作业平台主项目的自定义登录客户端一致：
 * {@code GET {api-url}/user/get_info/?app_code={app-code}&{token-name}={ticket}}，
 * 响应 {@code {"ret":0,"data":{"username":"xxx"}}}，{@code ret} 为0且 {@code data} 非空才算登录有效。</p>
 *
 * <p>ticket是登录凭证，任何日志都不得打印其明文。</p>
 */
@Slf4j
@Service
public class BkTicketLoginServiceImpl implements BkTicketLoginService {

    private static final String API_GET_USER_INFO = "/user/get_info/";

    private final WebAuthProperties webAuthProperties;

    private final RestTemplate loginRestTemplate;

    private final Cache<String, LoginCacheEntry> loginCache;

    @Autowired
    public BkTicketLoginServiceImpl(WebAuthProperties webAuthProperties,
                                    @Qualifier("loginRestTemplate") RestTemplate loginRestTemplate) {
        this(webAuthProperties, loginRestTemplate, Ticker.systemTicker());
    }

    BkTicketLoginServiceImpl(WebAuthProperties webAuthProperties,
                             RestTemplate loginRestTemplate,
                             Ticker ticker) {
        this.webAuthProperties = webAuthProperties;
        this.loginRestTemplate = loginRestTemplate;
        this.loginCache = buildCache(webAuthProperties.getAuth().getLogin().getCache(), ticker);
    }

    @Override
    public String getUsername(String ticket) {
        if (StringUtils.isBlank(ticket)) {
            return null;
        }
        WebAuthProperties.Login login = webAuthProperties.getAuth().getLogin();
        if (StringUtils.isBlank(login.getApiUrl())) {
            // 未配置登录服务时按“未登录”处理而不是抛异常：没配置与服务故障是两回事，
            // 前者引导用户去登录页更符合直觉，也不产生无意义的告警噪音
            log.warn("job.op.web.auth.login.api-url is not configured, treat all access as unauthenticated");
            return null;
        }

        if (loginCache == null) {
            return requestUsername(login, ticket);
        }

        LoginCacheEntry entry = loginCache.get(hashTicket(ticket), ignored -> {
            String username = requestUsername(login, ticket);
            return StringUtils.isBlank(username) ? LoginCacheEntry.invalid() : LoginCacheEntry.valid(username);
        });
        return entry.isValid() ? entry.getUsername() : null;
    }

    private Cache<String, LoginCacheEntry> buildCache(WebAuthProperties.Cache cacheProperties, Ticker ticker) {
        if (cacheProperties.getMaxSize() == 0) {
            return null;
        }
        long validNanos = Duration.ofSeconds(cacheProperties.getValidSeconds()).toNanos();
        long invalidNanos = Duration.ofSeconds(cacheProperties.getInvalidSeconds()).toNanos();
        return Caffeine.<String, LoginCacheEntry>newBuilder()
            .maximumSize(cacheProperties.getMaxSize())
            .ticker(ticker)
            .expireAfter(new Expiry<String, LoginCacheEntry>() {
                @Override
                public long expireAfterCreate(String key, LoginCacheEntry value, long currentTime) {
                    return value.isValid() ? validNanos : invalidNanos;
                }

                @Override
                public long expireAfterUpdate(String key,
                                              LoginCacheEntry value,
                                              long currentTime,
                                              long currentDuration) {
                    return value.isValid() ? validNanos : invalidNanos;
                }

                @Override
                public long expireAfterRead(String key,
                                            LoginCacheEntry value,
                                            long currentTime,
                                            long currentDuration) {
                    return currentDuration;
                }
            })
            .build();
    }

    /**
     * 调用登录服务查询用户信息
     *
     * @return 用户名；ticket无效时返回null
     */
    private String requestUsername(WebAuthProperties.Login login, String ticket) {
        String apiPath = trimTrailingSlash(login.getApiUrl()) + API_GET_USER_INFO;
        URI uri = UriComponentsBuilder.fromUriString(apiPath)
            .queryParam("app_code", login.getAppCode())
            .queryParam(login.getTokenName(), ticket)
            .encode()
            .build()
            .toUri();

        String body;
        long start = System.currentTimeMillis();
        try {
            body = loginRestTemplate.getForObject(uri, String.class);
        } catch (RestClientException e) {
            // 日志中不能出现ticket，因此只打印接口路径与非敏感参数
            log.error("getUserInfo| url={}| params=app_code={}| time={}",
                apiPath, login.getAppCode(), System.currentTimeMillis() - start);
            throw new LoginServiceUnavailableException("Failed to call login service", e);
        }
        log.info("getUserInfo| url={}| params=app_code={}| time={}",
            apiPath, login.getAppCode(), System.currentTimeMillis() - start);

        if (StringUtils.isBlank(body)) {
            log.warn("Get user info, response is empty");
            return null;
        }
        Resp<UserDTO> resp = JsonUtils.fromJson(body, new TypeReference<Resp<UserDTO>>() {
        });
        if (resp == null) {
            log.warn("Get user info, resp is empty");
            return null;
        }
        if (!Resp.SUCCESS_CODE.equals(resp.getRet())) {
            log.warn("Get user info return fail code:{}", resp.getRet());
            return null;
        }
        if (resp.getData() == null) {
            log.warn("Get user info, resp data is empty");
            return null;
        }
        return resp.getData().getUsername();
    }

    private String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /**
     * ticket是登录凭证，即使只在内存中缓存也不得作为明文键
     */
    private String hashTicket(String ticket) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(ticket.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    long estimatedCacheSize() {
        if (loginCache == null) {
            return 0;
        }
        loginCache.cleanUp();
        return loginCache.estimatedSize();
    }

    Set<String> cacheKeys() {
        if (loginCache == null) {
            return Collections.emptySet();
        }
        return loginCache.asMap().keySet();
    }

    @Getter
    private static class LoginCacheEntry {

        private final String username;

        private final boolean valid;

        private LoginCacheEntry(String username, boolean valid) {
            this.username = username;
            this.valid = valid;
        }

        static LoginCacheEntry valid(String username) {
            return new LoginCacheEntry(username, true);
        }

        static LoginCacheEntry invalid() {
            return new LoginCacheEntry(null, false);
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    private static class Resp<T> {
        private static final Integer SUCCESS_CODE = 0;

        private Integer ret;

        private T data;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    private static class UserDTO {
        private String username;
    }
}
