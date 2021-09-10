package com.tencent.bk.job.common.iam.aspect;

import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO;
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
public class IamCallbackAspect {

    @Autowired
    public IamCallbackAspect() {
    }

    @Pointcut("execution (* com.tencent.bk.job.*.api.iam.impl.*.callback(..))")
    public void processCallbackRequest() {
    }

    private void logRequest(CallbackRequestDTO callbackRequest) {
        if (log.isDebugEnabled()) {
            log.debug("callbackRequest={}", JsonUtils.toJson(callbackRequest));
        } else {
            log.info(
                "Received iam callbackRequest:[{}|{}|{}|{}]",
                callbackRequest.getMethod().getMethod(),
                callbackRequest.getType(),
                callbackRequest.getFilter(),
                callbackRequest.getPage()
            );
        }
    }

    @Around("processCallbackRequest()")
    public Object logBeforeProcessCallbackRequest(ProceedingJoinPoint pjp) throws Exception {
        try {
            Object[] args = pjp.getArgs();
            if (args.length < 1) {
                log.warn("unexpected ProceedingJoinPoint, please check");
                return pjp.proceed();
            }
            CallbackRequestDTO callbackRequest = (CallbackRequestDTO) args[0];
            logRequest(callbackRequest);
            return pjp.proceed();
        } catch (Throwable throwable) {
            throw new Exception("Fail to execute logBeforeProcessCallbackRequest", throwable);
        }
    }
}
