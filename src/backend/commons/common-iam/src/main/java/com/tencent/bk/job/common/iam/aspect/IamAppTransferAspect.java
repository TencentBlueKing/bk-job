package com.tencent.bk.job.common.iam.aspect;

import com.tencent.bk.job.common.app.AppTransferService;
import com.tencent.bk.job.common.app.ResourceScope;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 鉴权执行前对Job业务与CMDB业务（集）进行转换的切面
 * 对ResourceScope中已有数据进行转换并填充
 */
@Slf4j
@Aspect
@Component
public class IamAppTransferAspect {

    private final AppTransferService appTransferService;

    @Autowired
    public IamAppTransferAspect(AppTransferService appTransferService) {
        this.appTransferService = appTransferService;
    }

    @Pointcut("execution (* com.tencent.bk.job.*.auth..impl.*.auth*(..))")
    public void processAuthResourceAction() {
    }

    private void fillResourceScope(ResourceScope resourceScope) {
        log.debug("before transfer, resourceScope={}", resourceScope);
        appTransferService.fillResourceScope(resourceScope);
        log.debug("after transfer, resourceScope={}", resourceScope);
    }

    @Around("processAuthResourceAction()")
    public Object logBeforeProcessCallbackRequest(ProceedingJoinPoint pjp) throws Throwable {
        try {
            Object[] args = pjp.getArgs();
            for (Object arg : args) {
                if (arg instanceof ResourceScope) {
                    ResourceScope resourceScope = (ResourceScope) arg;
                    fillResourceScope(resourceScope);
                }
            }
        } catch (Throwable throwable) {
            throw new Exception("Fail to execute transferResourceScope", throwable);
        }
        return pjp.proceed();
    }
}
