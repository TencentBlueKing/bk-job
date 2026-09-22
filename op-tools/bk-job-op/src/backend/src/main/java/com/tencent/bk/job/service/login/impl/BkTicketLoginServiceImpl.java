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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

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

    private static final String CACHE_KEY_PREFIX = "job:op:login:ticket:";

    /**
     * 负缓存哨兵值
     */
    private static final String INVALID_SENTINEL = "__INVALID__";

    private final WebAuthProperties webAuthProperties;

    private final RestTemplate loginRestTemplate;

    private final RedisTemplate<String, String> redisTemplate;

    public BkTicketLoginServiceImpl(WebAuthProperties webAuthProperties,
                                    @Qualifier("loginRestTemplate") RestTemplate loginRestTemplate,
                                    RedisTemplate<String, String> redisTemplate) {
        this.webAuthProperties = webAuthProperties;
        this.loginRestTemplate = loginRestTemplate;
        this.redisTemplate = redisTemplate;
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

        String cacheKey = buildCacheKey(ticket);
        String cached = readCache(cacheKey);
        if (INVALID_SENTINEL.equals(cached)) {
            return null;
        }
        if (cached != null) {
            return cached;
        }

        String username = requestUsername(login, ticket);
        if (StringUtils.isBlank(username)) {
            writeCache(cacheKey, INVALID_SENTINEL, login.getCache().getInvalidSeconds());
            return null;
        }
        writeCache(cacheKey, username, login.getCache().getValidSeconds());
        return username;
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
     * ticket是登录凭证，明文进Redis key等于把它落到一个运维可随手SCAN到的地方，这里先做哈希
     */
    private String buildCacheKey(String ticket) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(ticket.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return CACHE_KEY_PREFIX + sb;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /**
     * Redis不可用时降级为直连登录服务（不缓存），不因缓存故障阻断登录
     */
    private String readCache(String cacheKey) {
        try {
            return redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.warn("Failed to read login cache, fallback to calling login service directly", e);
            return null;
        }
    }

    private void writeCache(String cacheKey, String value, int seconds) {
        if (seconds <= 0) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(cacheKey, value, Duration.ofSeconds(seconds));
        } catch (Exception e) {
            log.warn("Failed to write login cache", e);
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
