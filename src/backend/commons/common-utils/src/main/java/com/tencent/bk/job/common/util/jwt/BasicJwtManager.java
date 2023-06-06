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

package com.tencent.bk.job.common.util.jwt;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tencent.bk.job.common.util.crypto.RSAUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;


@Slf4j
public class BasicJwtManager implements JwtManager {
    private volatile String token = null;
    private final PublicKey publicKey;
    private final PrivateKey privateKey;

    private final Cache<String, Long> tokenCache = CacheBuilder.newBuilder()
        .maximumSize(9999).expireAfterWrite(5, TimeUnit.MINUTES).build();

    public BasicJwtManager(String privateKeyBase64,
                           String publicKeyBase64) throws IOException, GeneralSecurityException {
        this.privateKey = RSAUtils.getPrivateKey(privateKeyBase64);
        this.publicKey = RSAUtils.getPublicKey(publicKeyBase64);
        log.info("Init JwtManager successfully!");
    }

    /**
     * 获取JWT jwt token
     *
     * @return
     */
    @Override
    public String getToken() {
        if (token != null) {
            return token;
        }
        return generateToken();
    }

    @Override
    public String generateToken(long expireMills) {
        // token 超时2h
        long expireAt = System.currentTimeMillis() + expireMills;
        this.token = Jwts.builder().setSubject("job-service-auth").setExpiration(new Date(expireAt))
            .signWith(SignatureAlgorithm.RS512, privateKey).compact();
        return token;
    }

    public String generateToken() {
        // token 默认超时2h
        return generateToken(120 * 60 * 1000);
    }

    /**
     * 验证JWT
     *
     * @param token jwt token
     * @return
     */
    @Override
    public boolean verifyJwt(String token) {
        long start = System.currentTimeMillis();
        Long tokenExpireAt = tokenCache.getIfPresent(token);
        if (tokenExpireAt != null) {
            // 如果未超时
            if (tokenExpireAt > Instant.now().getEpochSecond()) {
                return true;
            }
        }

        try {
            Claims claims = Jwts.parser()
                .setSigningKey(publicKey)
                .parseClaimsJws(token)
                .getBody();

            Date expireAt = claims.get("exp", Date.class);
            if (expireAt != null) {
                tokenCache.put(token, expireAt.getTime());
            }
        } catch (ExpiredJwtException e) {
            log.error("Token is expire!", e);
            return false;
        } catch (Exception e) {
            log.error("Verify jwt caught exception", e);
            return false;
        } finally {
            long cost = System.currentTimeMillis() - start;
            if (cost > 10) {
                log.warn("Verify jwt cost too much, cost:{}", cost);
            }
        }
        return true;
    }
}
