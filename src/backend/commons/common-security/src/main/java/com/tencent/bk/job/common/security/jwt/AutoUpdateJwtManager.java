package com.tencent.bk.job.common.security.jwt;

import com.tencent.bk.job.common.jwt.BasicJwtManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
public class AutoUpdateJwtManager extends BasicJwtManager {
    public AutoUpdateJwtManager(String privateKeyBase64, String publicKeyBase64) {
        super(privateKeyBase64, publicKeyBase64);
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void refreshToken() {
        log.info("Refresh token");
        generateToken();
    }
}
