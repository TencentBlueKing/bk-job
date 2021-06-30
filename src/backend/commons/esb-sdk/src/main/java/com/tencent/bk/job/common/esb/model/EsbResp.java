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

package com.tencent.bk.job.common.esb.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.model.iam.EsbApplyPermissionDTO;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.i18n.MessageI18nService;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.util.JobContextUtil;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Function;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EsbResp<T> {
    public static final Integer SUCCESS_CODE = 0;

    private Integer code;

    private Boolean result;

    @JsonProperty("request_id")
    private String requestId;

    private String message;

    private T data;

    /**
     * 无权限返回数据
     */
    private EsbApplyPermissionDTO permission;

    private EsbResp(Integer code, String message, T data) {
        this.code = code;
        this.data = data;
        this.message = message;
        this.result = code.equals(SUCCESS_CODE);
        this.requestId = JobContextUtil.getRequestId();
    }

    public static <T> EsbResp<T> buildSuccessResp(T data) {
        EsbResp<T> resp = new EsbResp<>(SUCCESS_CODE, null, data);
        resp.result = true;
        return resp;
    }

    public static <T> EsbResp<T> buildCommonFailResp(Integer errorCode, String msg) {
        EsbResp<T> resp = new EsbResp<>(errorCode, msg, null);
        resp.result = false;
        return resp;
    }

    public static <T> EsbResp<T> buildCommonFailResp(ServiceException e, MessageI18nService i18nService) {
        int errorCode = e.getErrorCode();
        String errorMsg = e.getMessage();
        if (StringUtils.isEmpty(errorMsg)) {
            errorMsg = i18nService.getI18nWithArgs(String.valueOf(errorCode), e.getErrorParams());
        }
        if (StringUtils.isEmpty(errorMsg)) {
            errorMsg = String.valueOf(errorCode);
        }
        return new EsbResp<>(e.getErrorCode(), errorMsg, null);
    }

    public static <T> EsbResp<T> buildCommonFailResp(int errorCode, MessageI18nService i18nService) {
        String errorMsg = i18nService.getI18n(String.valueOf(errorCode));
        if (StringUtils.isEmpty(errorMsg)) {
            errorMsg = String.valueOf(errorCode);
        }
        return new EsbResp<>(errorCode, errorMsg, null);
    }

    public static <T> EsbResp<T> buildCommonFailResp(int errorCode, MessageI18nService i18nService,
                                                     Object... errorParams) {
        String errorMsg = i18nService.getI18nWithArgs(String.valueOf(errorCode), errorParams);
        if (StringUtils.isEmpty(errorMsg)) {
            errorMsg = String.valueOf(errorCode);
        }
        return new EsbResp<>(errorCode, errorMsg, null);
    }

    public static <T> EsbResp<T> buildAuthFailResult(EsbApplyPermissionDTO permission, MessageI18nService i18nService) {
        EsbResp<T> esbResp = new EsbResp<>();
        esbResp.setPermission(permission);
        esbResp.setMessage(i18nService.getI18n(String.valueOf(ErrorCode.API_NO_PERMISSION)));
        esbResp.setCode(ErrorCode.API_NO_PERMISSION);
        return esbResp;
    }

    public static <T> EsbResp<T> buildCommonFailResp(MessageI18nService i18nService, ValidateResult validateResult) {
        if (validateResult.getErrorParams() != null && validateResult.getErrorParams().length > 0) {
            return EsbResp.buildCommonFailResp(validateResult.getErrorCode(),
                i18nService.getI18nWithArgs(String.valueOf(validateResult.getErrorCode()),
                    validateResult.getErrorParams()));
        } else {
            return EsbResp.buildCommonFailResp(validateResult.getErrorCode(),
                i18nService.getI18n(String.valueOf(validateResult.getErrorCode())));
        }
    }

    public static <T, R> EsbResp<R> convertData(EsbResp<T> esbResp, Function<T, R> converter) {
        EsbResp<R> newEsbResp = new EsbResp<>();
        newEsbResp.setCode(esbResp.getCode());
        newEsbResp.setMessage(esbResp.getMessage());
        newEsbResp.setRequestId(esbResp.getRequestId());
        newEsbResp.setPermission(esbResp.getPermission());
        newEsbResp.setResult(esbResp.getResult());
        newEsbResp.setData(converter.apply(esbResp.getData()));
        return newEsbResp;
    }
}
