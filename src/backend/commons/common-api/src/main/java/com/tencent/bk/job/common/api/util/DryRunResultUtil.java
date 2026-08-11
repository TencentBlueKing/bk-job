/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.bk.job.common.api.util;

import com.tencent.bk.job.common.api.model.DryRunResult;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.AbortedException;
import com.tencent.bk.job.common.exception.AlreadyExistsException;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.exception.NotImplementedException;
import com.tencent.bk.job.common.exception.ResourceExhaustedException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.exception.TimeoutException;
import com.tencent.bk.job.common.exception.UnauthenticatedException;
import com.tencent.bk.job.common.exception.UnavailableException;
import com.tencent.bk.job.common.model.error.ErrorType;
import lombok.extern.slf4j.Slf4j;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 审批相关 inner 接口的统一返回处理。
 * <p>
 * 落实"校验失败必须以 HTTP 200 + {@link DryRunResult} 返回"的硬性契约：一旦以异常传播，
 * FeignErrorDecoder 会把下游的 INVALID_PARAM 一律吞成 InternalException，
 * 具体校验信息全部丢失，用户只会看到一句"内部错误"。
 * <p>
 * 同时补齐 inner 路径的 Bean Validation 缺口：v4 请求体的格式校验靠注解（含 GroupSequenceProvider
 * 分组校验）完成，而 inner 接口不经网关、没有 Spring MVC 的自动校验，若不显式跑一次 Validator，
 * 就会出现"ESB 拦得住、审批预检拦不住"的不对称 —— 那等价于审批预检形同虚设。
 */
@Slf4j
public final class DryRunResultUtil {

    private DryRunResultUtil() {
    }

    /**
     * 先跑 Bean Validation，再执行业务动作，并把校验类异常转成正常返回值。
     *
     * @param request   待校验的请求体，其注解约束等价于 ESB 层由网关触发的那一套
     * @param validator Bean Validation 校验器
     * @param action    业务动作，内部可自由抛校验类异常
     * @return 校验通过时为 action 的返回值，否则为带错误码的 invalid 结果
     */
    public static <T> DryRunResult<T> call(Object request, Validator validator, Supplier<DryRunResult<T>> action) {
        DryRunResult<T> violationResult = validateBean(request, validator);
        if (violationResult != null) {
            return violationResult;
        }
        try {
            return action.get();
        } catch (ServiceException e) {
            if (isInternalError(e)) {
                // 内部错误/依赖不可用不是用户的参数问题，让它继续以异常传播，
                // 由调用方按内部异常处理（可重试、可告警），不要伪装成"参数校验不通过"
                throw e;
            }
            log.info("Dry run rejected, errorCode: {}, errorType: {}", e.getErrorCode(), e.getErrorType(), e);
            return DryRunResult.invalid(e.getErrorCode(), e.getErrorParams(), errorTypeValue(e));
        }
    }

    /**
     * 显式跑一次 Bean Validation
     *
     * @return 校验通过返回 null，否则返回 invalid 结果
     */
    public static <T> DryRunResult<T> validateBean(Object request, Validator validator) {
        if (request == null || validator == null) {
            return null;
        }
        Set<ConstraintViolation<Object>> violations = validator.validate(request);
        if (violations.isEmpty()) {
            return null;
        }
        // 按属性路径排序，保证同一份非法请求每次返回的错误信息一致，便于用户与自动化调用方处理
        List<ConstraintViolation<Object>> sorted = violations.stream()
            .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
            .collect(Collectors.toList());
        String paramNames = sorted.stream()
            .map(violation -> violation.getPropertyPath().toString())
            .collect(Collectors.joining(","));
        String reasons = sorted.stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.joining("; "));
        log.info("Dry run request violates constraints, params: {}, reasons: {}", paramNames, reasons);
        return DryRunResult.invalid(
            ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
            new Object[]{paramNames, reasons},
            ErrorType.INVALID_PARAM.getType()
        );
    }

    /**
     * 调用方侧的反向转换：把 {@code valid=false} 的结果还原成对应语义的异常，原样带上错误码与占位参数。
     * <p>
     * 错误码与占位参数一路透传、由最外层按调用方语言渲染，因此下游的具体校验信息不会在中转过程中丢失
     * —— 这正是"校验失败以返回值而非异常传播"的收益所在。
     *
     * @param result 下游返回的失败结果
     * @return 与 errorType 语义对应的异常
     */
    public static ServiceException toException(DryRunResult<?> result) {
        Integer errorCode = result.getErrorCode() == null ? ErrorCode.INTERNAL_ERROR : result.getErrorCode();
        Object[] errorParams = result.getErrorParams();
        ErrorType errorType = ErrorType.valOf(result.getErrorType());
        if (errorType == null) {
            return new InternalException(errorCode, errorParams);
        }
        switch (errorType) {
            case INVALID_PARAM:
                return new InvalidParamException(errorCode, errorParams);
            case NOT_FOUND:
                return new NotFoundException(errorCode, errorParams);
            case ALREADY_EXISTS:
                return new AlreadyExistsException(errorCode, errorParams);
            case RESOURCE_EXHAUSTED:
                return new ResourceExhaustedException(errorCode, errorParams);
            case ABORTED:
                return new AbortedException(errorCode, errorParams);
            case UNAUTHENTICATED:
                return new UnauthenticatedException(errorCode, errorParams);
            case UNIMPLEMENTED:
                return new NotImplementedException(errorCode, errorParams);
            case UNAVAILABLE:
                return new UnavailableException(errorCode, errorParams);
            case TIMEOUT:
                return new TimeoutException(errorCode, errorParams);
            case FAILED_PRECONDITION:
            // PERMISSION_DENIED 只能退化为 FAILED_PRECONDITION：PermissionDeniedException 需要一个
            // AuthResult 才能构造，而 DryRunResult 里没有可申请权限的信息（在下游被 call() 转成返回值时就丢了）。
            // 错误码原样保留，用户仍能看到"权限不足"的文案，但拿不到"去申请权限"的跳转信息。
            // TODO: 若要补齐，需在 DryRunResult 中增加可申请权限的字段，属协议层改动，不在本期范围内。
            case PERMISSION_DENIED:
                return new FailedPreconditionException(errorCode, errorParams);
            default:
                return new InternalException(errorCode, errorParams);
        }
    }

    private static boolean isInternalError(ServiceException e) {
        ErrorType errorType = e.getErrorType();
        return errorType == null
            || errorType == ErrorType.INTERNAL
            || errorType == ErrorType.UNAVAILABLE
            || errorType == ErrorType.TIMEOUT;
    }

    private static Integer errorTypeValue(ServiceException e) {
        return e.getErrorType() == null ? null : e.getErrorType().getType();
    }
}
