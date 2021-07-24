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

package com.tencent.bk.job.gateway.service.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tencent.bk.job.gateway.model.esb.EsbJwtInfo;
import com.tencent.bk.job.gateway.service.EsbJwtPublicKeyService;
import com.tencent.bk.job.gateway.service.EsbJwtService;
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
public class EsbJwtServiceImpl implements EsbJwtService {
    private final EsbJwtPublicKeyService esbJwtPublicKeyService;
    private PublicKey publicKey;
    private final Cache<String, EsbJwtInfo> tokenCache = CacheBuilder.newBuilder()
        .maximumSize(99999).expireAfterWrite(30, TimeUnit.SECONDS).build();

    @Autowired
    public EsbJwtServiceImpl(EsbJwtPublicKeyService esbJwtPublicKeyService){
        this.esbJwtPublicKeyService = esbJwtPublicKeyService;
        getAndCachePublicKey();
    }

    private void getAndCachePublicKey() {
        try {
            String esbJwtPublicKey = esbJwtPublicKeyService.getEsbJWTPublicKey();
            if (StringUtils.isEmpty(esbJwtPublicKey)) {
                log.error("Esb jwt public key is not configured!");
                return;
            }
            this.publicKey = buildPublicKey(esbJwtPublicKey);
        } catch (Throwable e) {
            // Catch all exception
            log.error("Build esb jwt public key caught error!", e);
        }
    }

    private PublicKey buildPublicKey(String pemContent)
        throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        PemReader pemReader = new PemReader(new StringReader(pemContent));
        PemObject pemObject = pemReader.readPemObject();
        if (pemObject == null) {
            log.error("Esb public key pem is illegal!");
            return null;
        }
        java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(pemObject.getContent());
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return keyFactory.generatePublic(pubKeySpec);
    }

    @Override
    public EsbJwtInfo extractFromJwt(String token) {
        long start = System.currentTimeMillis();
        EsbJwtInfo cacheJwtInfo = tokenCache.getIfPresent(token);
        if (cacheJwtInfo != null) {
            Long tokenExpireAt = cacheJwtInfo.getTokenExpireAt();
            // 如果未超时
            if (tokenExpireAt > Instant.now().getEpochSecond()) {
                return cacheJwtInfo;
            }
        }

        EsbJwtInfo esbJwtInfo;
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(publicKey)
                .parseClaimsJws(token)
                .getBody();

            String appCode = "";
            if (claims.get("app") != null) {
                LinkedHashMap appProps = claims.get("app", LinkedHashMap.class);
                if (appProps == null) {
                    log.warn("Invalid JWT token, app is null!");
                    return null;
                }
                boolean isVerified = appProps.get("verified") != null && (boolean) appProps.get("verified");
                appCode = (String) appProps.get("app_code");
                if (StringUtils.isEmpty(appCode)) {
                    appCode = (String) appProps.get("bk_app_code");
                }
                if (!isVerified || StringUtils.isEmpty(appCode)) {
                    log.warn("App code not verified or empty, isVerified:{}, jwtAppCode:{}", isVerified, appCode);
                    return null;
                }
            }

            String username = "";
            if (claims.get("user") != null) {
                LinkedHashMap userProps = claims.get("user", LinkedHashMap.class);
                if (userProps == null) {
                    log.warn("Invalid JWT token, user is null!");
                    return null;
                }
                username = (String) userProps.get("username");
                if (StringUtils.isEmpty(username)) {
                    username = (String) userProps.get("bk_username");
                }
                if (StringUtils.isEmpty(username)) {
                    log.warn("Username is empty!");
                    return null;
                }
            }
            Date expireAt = claims.get("exp", Date.class);
            if (expireAt == null) {
                log.warn("Invalid JWT token, exp is null!");
                return null;
            }
            esbJwtInfo = new EsbJwtInfo(expireAt.getTime(), username, appCode);
            tokenCache.put(token, esbJwtInfo);
        } catch (Exception e) {
            log.warn("Verify jwt caught exception", e);
            return null;
        } finally {
            long cost = System.currentTimeMillis() - start;
            if (cost > 10) {
                log.warn("Verify jwt cost too much, cost:{}", cost);
            }
        }
        return esbJwtInfo;
    }

}
