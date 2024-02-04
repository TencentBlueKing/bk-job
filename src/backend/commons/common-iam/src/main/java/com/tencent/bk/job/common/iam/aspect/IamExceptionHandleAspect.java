package com.tencent.bk.job.common.iam.aspect;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.sdk.iam.exception.IamException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * 调用IAM-SDK鉴权过程中对SDK抛出的异常进行处理
 */
@Slf4j
@Aspect
public class IamExceptionHandleAspect {

    @Pointcut("within(com.tencent.bk.sdk.iam.service.impl.PolicyServiceImpl) " +
        "&& execution (* com.tencent.bk.sdk.iam.service.impl.PolicyServiceImpl.*(..))")
    public void processPolicyServiceAction() {
    }

    @Pointcut("within(com.tencent.bk.sdk.iam.helper.AuthHelper) " +
        "&& execution (* com.tencent.bk.sdk.iam.helper.AuthHelper.isAllowed(..))")
    public void processIsAllowedAction() {
    }

    @Around("processIsAllowedAction()||processPolicyServiceAction()")
    public Object handleIamExceptionDuringAuth(ProceedingJoinPoint pjp) throws Throwable {
        String username = null;
        try {
            Object[] args = pjp.getArgs();
            if (args.length > 0 && args[0] instanceof String) {
                username = (String) args[0];
            }
            return pjp.proceed();
        } catch (IamException e) {
            return handIamException(username, e);
        }
    }

    /**
     * 对IAM异常进行包装处理
     *
     * @param username 用户名
     * @param e        权限中心异常
     */
    private Object handIamException(String username, IamException e) {
        long errCode = e.getErrorCode();
        long iamErrorCodeUserAccountFrozen = 1901403L;
        if (errCode == iamErrorCodeUserAccountFrozen) {
            throw new FailedPreconditionException(e, ErrorCode.IAM_USER_ACCOUNT_FROZEN, new String[]{username});
        }
        throw new InternalException(e, ErrorCode.IAM_API_DATA_ERROR);
    }
}
