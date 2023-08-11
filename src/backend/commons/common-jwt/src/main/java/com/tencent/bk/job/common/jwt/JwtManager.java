package com.tencent.bk.job.common.jwt;

public interface JwtManager {
    String getToken();

    String generateToken(long expireMills);

    boolean verifyJwt(String token);
}
