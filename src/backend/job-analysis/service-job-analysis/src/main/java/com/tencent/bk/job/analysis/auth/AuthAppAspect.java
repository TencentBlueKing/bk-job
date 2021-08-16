package com.tencent.bk.job.analysis.auth;

import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.exception.InSufficientPermissionException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
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

    @Pointcut("execution (* com.tencent.bk.job.analysis.api.web.impl.*.*(..))")
    public void processAppScopedRequest() {
    }

    Pair<String, Long> findUserAndAppIdFromArgs(ProceedingJoinPoint pjp) {
        Signature sig = pjp.getSignature();
        MethodSignature msig = (MethodSignature) sig;
        String[] parameterNames = msig.getParameterNames();
        String username = null;
        Long appId = null;
        for (int i = 0; i < parameterNames.length; i++) {
            if ("username".equals(parameterNames[i])) {
                username = (String) pjp.getArgs()[i];
            } else if ("appId".equals(parameterNames[i])) {
                appId = (Long) pjp.getArgs()[i];
            }
            if (username != null && appId != null) {
                return Pair.of(username, appId);
            }
        }
        return null;
    }

    @Around("processAppScopedRequest()")
    public Object authAppBeforeProcessAppScopedRequest(ProceedingJoinPoint pjp) throws Throwable {
        Pair<String, Long> userAppIdPair = findUserAndAppIdFromArgs(pjp);
        if (userAppIdPair != null) {
            String username = userAppIdPair.getLeft();
            Long appId = userAppIdPair.getRight();
            log.info("auth {} access_business {}", username, appId);
            AuthResult authResult = authService.auth(true, username, ActionId.LIST_BUSINESS,
                ResourceTypeEnum.BUSINESS, appId.toString(), null);
            if (!authResult.isPass()) {
                throw new InSufficientPermissionException(authResult);
            }
        }
        return pjp.proceed();
    }
}
