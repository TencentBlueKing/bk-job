package com.tencent.bk.job.common.iam.aspect;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.job.common.constant.TenantIdConstants;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Slf4j
@Aspect
public class IamCallbackAspect {

    private final TenantEnvService tenantEnvService;

    public IamCallbackAspect(TenantEnvService tenantEnvService) {
        this.tenantEnvService = tenantEnvService;
    }

    @Pointcut("within(com.tencent.bk.job..*) && execution (* com.tencent.bk.job.*.api.iam.impl.*.callback(..))")
    public void processCallbackRequest() {
    }

    @Around("processCallbackRequest()")
    public Object doProcessCallbackRequest(ProceedingJoinPoint pjp) throws Exception {
        try {
            Object[] args = pjp.getArgs();
            if (args.length < 1) {
                log.warn("unexpected ProceedingJoinPoint, please check");
                return pjp.proceed();
            }
            String tenantId = (String) args[0];
            checkAndSetTenantIdToContext(tenantId);
            CallbackRequestDTO callbackRequest = (CallbackRequestDTO) args[1];
            logRequest(callbackRequest);
            return pjp.proceed();
        } catch (Throwable throwable) {
            throw new Exception("Fail to execute logBeforeProcessCallbackRequest", throwable);
        }
    }

    private void checkAndSetTenantIdToContext(String tenantId) {
        if (!tenantEnvService.isTenantEnabled() && StringUtils.isBlank(tenantId)) {
            // 单租户模式下，兼容不传租户的调用
            tenantId = TenantIdConstants.DEFAULT_TENANT_ID;
            log.debug("Add default tenantId({}) to JobContext", tenantId);
        }
        if (StringUtils.isBlank(tenantId)) {
            throw new InvalidParamException(ErrorCode.TENANT_ID_CANNOT_BE_BLANK, JobCommonHeaders.BK_TENANT_ID);
        }
        JobContextUtil.setUser(new User(tenantId, null, null));
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
}
