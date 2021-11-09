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
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.util.I18nUtil;
import com.tencent.bk.job.common.util.JobContextUtil;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Slf4j
public class EsbResp<T> {

    private Integer code;

    private Boolean result;

    @JsonProperty("job_request_id")
    private String requestId;

    private String message;

    private T data;

    /**
     * 无权限返回数据
     */
    private EsbApplyPermissionDTO permission;

    private EsbResp(T data) {
        this.code = ErrorCode.RESULT_OK;
        this.data = data;
        this.result = true;
        this.requestId = JobContextUtil.getRequestId();
    }

    private EsbResp(Integer errorCode, String message, T data) {
        this.code = errorCode;
        this.data = data;
        this.message = message;
        this.result = code.equals(ErrorCode.RESULT_OK);
        this.requestId = JobContextUtil.getRequestId();
    }

    public static <T> EsbResp<T> buildSuccessResp(T data) {
        return new EsbResp<>(data);
    }

    public static <T> EsbResp<T> buildCommonFailResp(Integer errorCode, Object[] errorParams, T data) {
        String message = I18nUtil.getI18nMessage(String.valueOf(errorCode), errorParams);
        return new EsbResp<>(errorCode, message, data);
    }

    public static <T> EsbResp<T> buildCommonFailResp(Integer errorCode) {
        String message = I18nUtil.getI18nMessage(String.valueOf(errorCode));
        return new EsbResp<>(errorCode, message, null);
    }

    public static <T> EsbResp<T> buildCommonFailResp(ServiceException e) {
        return buildCommonFailResp(e.getErrorCode(), e.getErrorParams(), null);
    }

    public static <T> EsbResp<T> buildAuthFailResult(EsbApplyPermissionDTO permission) {
        EsbResp<T> esbResp = buildCommonFailResp(ErrorCode.BK_PERMISSION_DENIED, null, null);
        esbResp.setPermission(permission);
        return esbResp;
    }

    public static <T> EsbResp<T> buildCommonFailResp(ValidateResult validateResult) {
        return buildCommonFailResp(validateResult.getErrorCode(), validateResult.getErrorParams(), null);
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
