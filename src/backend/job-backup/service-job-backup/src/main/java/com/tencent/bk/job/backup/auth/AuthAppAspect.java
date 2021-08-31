package com.tencent.bk.job.backup.auth;

import com.tencent.bk.job.common.iam.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class AuthAppAspect {

    private final AuthService authService;

    @Autowired
    public AuthAppAspect(AuthService authService) {
        this.authService = authService;
    }

    @Pointcut("execution (* com.tencent.bk.job.backup.api.web.impl.*.*(..))")
    public void processAppScopedRequest() {
    }


    @Around("processAppScopedRequest()")
    public Object authAppBeforeProcessAppScopedRequest(ProceedingJoinPoint pjp) throws Throwable {
        return authService.execAfterAuthAppInMethodParams(pjp);
    }
}
