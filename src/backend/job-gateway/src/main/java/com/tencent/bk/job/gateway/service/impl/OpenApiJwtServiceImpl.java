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

package com.tencent.bk.job.gateway.service.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.gateway.config.BkGatewayConfig;
import com.tencent.bk.job.gateway.model.esb.BkGwJwtInfo;
import com.tencent.bk.job.gateway.service.OpenApiJwtPublicKeyService;
import com.tencent.bk.job.gateway.service.OpenApiJwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class OpenApiJwtServiceImpl implements OpenApiJwtService {
    private final OpenApiJwtPublicKeyService openApiJwtPublicKeyService;
    private volatile PublicKey esbJwtPublicKey;
    private volatile PublicKey bkApiGatewayJwtPublicKey;
    private final BkGatewayConfig bkApiGatewayConfig;
    private final TenantEnvService tenantEnvService;


    /**
     * 蓝鲸网关请求标识
     */
    private static final String REQUEST_FROM_BK_API_GW = "bk-job-apigw";

    private final Cache<String, BkGwJwtInfo> tokenCache = CacheBuilder.newBuilder()
        .maximumSize(99999).expireAfterWrite(30, TimeUnit.SECONDS).build();

    @Autowired
    public OpenApiJwtServiceImpl(OpenApiJwtPublicKeyService openApiJwtPublicKeyService,
                                 BkGatewayConfig bkApiGatewayConfig,
                                 TenantEnvService tenantEnvService) {
        this.openApiJwtPublicKeyService = openApiJwtPublicKeyService;
        this.bkApiGatewayConfig = bkApiGatewayConfig;
        this.tenantEnvService = tenantEnvService;
        getJwtPublicKeyByPolicy();
    }

    private void getJwtPublicKeyByPolicy() {
        boolean publicKeyGotten = tryToGetAndCachePublicKeyOnce();
        if (publicKeyGotten) {
            log.info("Get and cache bkApiGateway/esb public key success");
        } else if ("abort".equalsIgnoreCase(bkApiGatewayConfig.getJwtPublicKeyFailPolicy())) {
            throw new InternalException("Failed to get jwt public key, abort policy triggered");
        } else if ("retry".equalsIgnoreCase(bkApiGatewayConfig.getJwtPublicKeyFailPolicy())) {
            getJwtPublicKeyWithBackgroundRetry();
        } else {
            throw new InternalException("Illegal jwt public key get fail policy");
        }
    }

    private void getJwtPublicKeyWithBackgroundRetry() {
        Thread openApiPublicKeyGetter = new Thread(() -> {
            boolean keyGotten;
            int retryCount = 0;
            int sleepMillsOnce = 5000;
            // 最多重试3天
            int maxRetryCount = 3 * 24 * 3600 / 5;
            do {
                log.warn("Gateway public key not gotten, retry {} after 5s", ++retryCount);
                ThreadUtils.sleep(sleepMillsOnce);
                keyGotten = tryToGetAndCachePublicKeyOnce();
            } while (!keyGotten && retryCount <= maxRetryCount);
            if (!keyGotten) {
                log.error("Gateway public key not gotten after {} retry (3 days), plz check esb", maxRetryCount);
            }
        });
        openApiPublicKeyGetter.setDaemon(true);
        openApiPublicKeyGetter.setName("gatewayPublicKeyGetter");
        openApiPublicKeyGetter.start();
    }

    private boolean tryToGetAndCachePublicKeyOnce() {
        try {
            if (!tenantEnvService.isTenantEnabled()) {
                // 非多租户环境，需要兼容 ESB
                if (this.esbJwtPublicKey == null) {
                    String esbJwtPublicKey = openApiJwtPublicKeyService.getEsbJWTPublicKey();
                    if (StringUtils.isEmpty(esbJwtPublicKey)) {
                        log.error("Esb jwt public key is not configured!");
                        return false;
                    }
                    this.esbJwtPublicKey = buildPublicKey(esbJwtPublicKey);
                    log.info("Init esb jwt public key success");
                }
            }

            if (this.bkApiGatewayJwtPublicKey == null && bkApiGatewayConfig.isEnabled()) {
                String bkApiGatewayJwtPublicKey = openApiJwtPublicKeyService.getBkApiGatewayJWTPublicKey();
                if (StringUtils.isEmpty(bkApiGatewayJwtPublicKey)) {
                    log.error("BkApiGateway jwt public key is not configured!");
                    return false;
                }
                this.bkApiGatewayJwtPublicKey = buildPublicKey(bkApiGatewayJwtPublicKey);
                log.info("Init bkApiGateway jwt public key success");
            }
            return true;
        } catch (Throwable e) {
            // Catch all exception
            log.error("Build jwt public key caught error!", e);
            return false;
        }
    }

    private PublicKey buildPublicKey(String pemContent)
        throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        PemReader pemReader = new PemReader(new StringReader(pemContent));
        PemObject pemObject = pemReader.readPemObject();
        if (pemObject == null) {
            log.error("Public key pem is illegal!");
            return null;
        }
        java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(pemObject.getContent());
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return keyFactory.generatePublic(pubKeySpec);
    }

    @Override
    public BkGwJwtInfo extractFromJwt(String token) {
        if (requestFromApiGw()) {
            log.debug("Extract bkApiGateway jwt");
            return extractFromJwt(token, this.bkApiGatewayJwtPublicKey);
        } else {
            log.debug("Extract esb jwt");
            return extractFromJwt(token, this.esbJwtPublicKey);
        }
    }

    @Override
    public BkGwJwtInfo extractFromJwt(String token, PublicKey publicKey) {
        long start = System.currentTimeMillis();
        BkGwJwtInfo cacheJwtInfo = tokenCache.getIfPresent(token);
        if (cacheJwtInfo != null) {
            Long tokenExpireAt = cacheJwtInfo.getTokenExpireAt();
            // 如果未超时
            if (tokenExpireAt > Instant.now().getEpochSecond()) {
                return cacheJwtInfo;
            }
        }

        BkGwJwtInfo bkGwJwtInfo;
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(publicKey)
                .parseClaimsJws(token)
                .getBody();
            String appCode = null;
            String username = null;
            if (claims.get("app") != null) {
                LinkedHashMap appProps = claims.get("app", LinkedHashMap.class);
                if (appProps == null) {
                    log.warn("Invalid JWT token, app is null!");
                    return null;
                }
                boolean isVerified = appProps.get("verified") != null && (boolean) appProps.get("verified");
                if (!isVerified) {
                    log.warn("App info not verified");
                    return null;
                }
                appCode = extractAppCode(appProps);
            }

            if (claims.get("user") != null) {
                LinkedHashMap userProps = claims.get("user", LinkedHashMap.class);
                if (userProps == null) {
                    log.warn("Invalid JWT token, user is null!");
                    return null;
                }
                username = extractUsername(userProps);
            }

            Date expireAt = claims.get("exp", Date.class);
            if (expireAt == null) {
                log.warn("Invalid JWT token, exp is null!");
                return null;
            }
            bkGwJwtInfo = new BkGwJwtInfo(expireAt.getTime(), username, appCode);
            tokenCache.put(token, bkGwJwtInfo);
        } catch (Exception e) {
            log.warn("Verify jwt caught exception", e);
            if (log.isDebugEnabled()) {
                log.debug("Parse jwt error, token: {}", token);
            }
            return null;
        } finally {
            long cost = System.currentTimeMillis() - start;
            if (cost > 10) {
                log.warn("Verify jwt cost too much, cost:{}", cost);
            }
        }
        return bkGwJwtInfo;
    }

    private String extractAppCode(LinkedHashMap appProps) {
        String appCode = (String) appProps.get("app_code");
        if (StringUtils.isEmpty(appCode)) {
            appCode = (String) appProps.get("bk_app_code");
        }
        return appCode;
    }

    /**
     * 获取用户账号 ID
     *
     * @param userProps 用户信息
     * @return 用户账号 ID
     */
    private String extractUsername(LinkedHashMap userProps) {
        String username = (String) userProps.get("username");
        if (StringUtils.isEmpty(username)) {
            username = (String) userProps.get("bk_username");
        }
        return username;
    }

    // 请求是否来自蓝鲸网关
    private boolean requestFromApiGw() {
        String requestFrom = JobContextUtil.getRequestFrom();
        if (bkApiGatewayConfig.isEnabled() && StringUtils.equals(requestFrom, REQUEST_FROM_BK_API_GW)) {
            return true;
        }
        return false;
    }
}
