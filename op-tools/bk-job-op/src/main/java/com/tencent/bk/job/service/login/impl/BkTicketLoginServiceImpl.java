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

import com.fasterxml.jackson.databind.JsonNode;
import com.tencent.bk.job.config.web.WebAuthProperties;
import com.tencent.bk.job.service.login.BkTicketLoginService;
import com.tencent.bk.job.service.login.LoginServiceUnavailableException;
import com.tencent.bk.job.utils.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于可配置HTTP接口的登录态校验实现。
 *
 * <p>接口协议的每个细节都来自配置（见 {@link WebAuthProperties.Verify}），代码中不硬编码URL、参数名、
 * 用户名字段路径与失效判定规则，联调时只改配置不改代码。</p>
 */
@Slf4j
@Service
public class BkTicketLoginServiceImpl implements BkTicketLoginService {

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
        WebAuthProperties.Verify verify = webAuthProperties.getAuth().getLogin().getVerify();
        if (StringUtils.isBlank(verify.getUrl())) {
            // 未配置校验接口时按“未登录”处理而不是抛异常：没配置与服务故障是两回事，
            // 前者引导用户去登录页更符合直觉，也不产生无意义的告警噪音
            log.warn("job.op.web.auth.login.verify.url is not configured, treat all access as unauthenticated");
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

        String username = requestUsername(verify, ticket);
        if (StringUtils.isBlank(username)) {
            writeCache(cacheKey, INVALID_SENTINEL, webAuthProperties.getAuth().getLogin().getCache()
                .getInvalidSeconds());
            return null;
        }
        writeCache(cacheKey, username, webAuthProperties.getAuth().getLogin().getCache().getValidSeconds());
        return username;
    }

    /**
     * 调用校验接口并解析用户名
     *
     * @return 用户名；ticket无效时返回null
     */
    private String requestUsername(WebAuthProperties.Verify verify, String ticket) {
        ResponseEntity<String> response;
        try {
            response = doRequest(verify, ticket);
        } catch (HttpStatusCodeException e) {
            int status = e.getStatusCode().value();
            if (verify.getNormalizedInvalidHttpStatuses().contains(status)) {
                log.info("Ticket is rejected by login service, status={}", status);
                return null;
            }
            throw new LoginServiceUnavailableException(
                "Login service responded with unexpected status " + status, e);
        } catch (RestClientException e) {
            throw new LoginServiceUnavailableException("Failed to call login service", e);
        }

        int status = response.getStatusCode().value();
        if (verify.getNormalizedInvalidHttpStatuses().contains(status)) {
            return null;
        }
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new LoginServiceUnavailableException("Login service responded with unexpected status " + status);
        }

        JsonNode root = JsonUtils.toJsonNode(response.getBody());
        if (root == null) {
            log.warn("Cannot parse login service response as JSON");
            return null;
        }
        if (StringUtils.isNotBlank(verify.getResultJsonPath())) {
            String result = readJsonPath(root, verify.getResultJsonPath());
            if (!verify.getNormalizedResultSuccessValues().contains(result)) {
                log.info("Ticket is rejected by login service, result={}", result);
                return null;
            }
        }
        String username = readJsonPath(root, verify.getUsernameJsonPath());
        if (StringUtils.isBlank(username)) {
            log.info("Username is absent in login service response, usernameJsonPath={}",
                verify.getUsernameJsonPath());
            return null;
        }
        return username;
    }

    private ResponseEntity<String> doRequest(WebAuthProperties.Verify verify, String ticket) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(verify.getUrl());
        verify.getNormalizedExtraQueryParams().forEach(uriBuilder::queryParam);

        HttpHeaders headers = new HttpHeaders();
        verify.getNormalizedExtraHeaders().forEach(headers::set);

        String body = null;
        switch (verify.getTicketParamIn()) {
            case HEADER:
                headers.set(verify.getTicketParamName(), ticket);
                break;
            case BODY_JSON:
                headers.setContentType(MediaType.APPLICATION_JSON);
                Map<String, String> bodyMap = new HashMap<>(1);
                bodyMap.put(verify.getTicketParamName(), ticket);
                body = JsonUtils.toJson(bodyMap);
                break;
            case QUERY:
            default:
                uriBuilder.queryParam(verify.getTicketParamName(), ticket);
                break;
        }

        HttpMethod method = "POST".equalsIgnoreCase(verify.getMethod()) ? HttpMethod.POST : HttpMethod.GET;
        return loginRestTemplate.exchange(
            uriBuilder.build(false).toUri(),
            method,
            new HttpEntity<>(body, headers),
            String.class
        );
    }

    /**
     * 按点号分隔的路径逐段下钻取值。路径任一段不存在时返回空串而不是抛NPE
     */
    private String readJsonPath(JsonNode root, String path) {
        if (StringUtils.isBlank(path)) {
            return null;
        }
        JsonNode node = root;
        for (String segment : StringUtils.split(path, '.')) {
            node = node.path(segment);
        }
        return node.isValueNode() ? node.asText() : null;
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
}
