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

package com.tencent.bk.job.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.model.error.ErrorDetailDTO;
import com.tencent.bk.job.common.model.error.ErrorType;
import com.tencent.bk.job.common.model.iam.AuthResultDTO;
import com.tencent.bk.job.common.util.I18nUtil;
import com.tencent.bk.job.common.util.JobContextUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@ToString
@NoArgsConstructor
@ApiModel("job内部微服务间调用通用返回结构")
public class InternalResponse<T> {

    @ApiModelProperty("是否成功")
    private boolean success;

    @ApiModelProperty("返回码")
    private Integer code;

    @ApiModelProperty("错误类型")
    private Integer errorType;

    @ApiModelProperty("错误信息")
    private String errorMsg;

    @ApiModelProperty("请求成功返回的数据")
    private T data;

    @ApiModelProperty("请求 ID")
    private String requestId;

    @ApiModelProperty("鉴权结果，当返回码为1238001时，该字段有值")
    @JsonProperty("authResult")
    private AuthResultDTO authResult;

    @ApiModelProperty("错误详情")
    @JsonProperty("errorDetail")
    private ErrorDetailDTO errorDetail;

    public InternalResponse(ErrorType errorType, Integer errorCode, T data) {
        this.code = errorCode;
        this.success = errorCode != null && errorCode.equals(ErrorCode.RESULT_OK);
        this.errorMsg = I18nUtil.getI18nMessage(String.valueOf(errorCode));
        this.errorType = errorType.getType();
        this.data = data;
        this.requestId = JobContextUtil.getRequestId();
    }

    public InternalResponse(ErrorType errorType, Integer errorCode, Object[] errorParams, T data) {
        this.code = errorCode;
        this.success = errorCode != null && errorCode.equals(ErrorCode.RESULT_OK);
        this.errorMsg = I18nUtil.getI18nMessage(String.valueOf(errorCode), errorParams);
        this.errorType = errorType.getType();
        this.data = data;
        this.requestId = JobContextUtil.getRequestId();
    }

    public static <T> InternalResponse<T> buildSuccessResp(T data) {
        return new InternalResponse<>(ErrorType.OK, ErrorCode.RESULT_OK, data);
    }

    public static <T> InternalResponse<T> buildAuthFailResp(AuthResultDTO authResult) {
        InternalResponse<T> resp = new InternalResponse<>(ErrorType.PERMISSION_DENIED,
            ErrorCode.PERMISSION_DENIED, null);
        resp.authResult = authResult;
        return resp;
    }

    public static <T> InternalResponse<T> buildCommonFailResp(ErrorType errorType, Integer errorCode) {
        return new InternalResponse<>(errorType, errorCode, null);
    }

    public static <T> InternalResponse<T> buildCommonFailResp(ErrorType errorType, Integer errorCode, Object[] params) {
        return new InternalResponse<>(errorType, errorCode, params, null);
    }

    public static <T> InternalResponse<T> buildCommonFailResp(ServiceException e) {
        return new InternalResponse<>(e.getErrorType(), e.getErrorCode(), e.getErrorParams(), null);
    }

    public static <T> InternalResponse<T> buildValidateFailResp(ValidateResult validateResult) {
        return new InternalResponse<>(ErrorType.INVALID_PARAM, validateResult.getErrorCode(),
            validateResult.getErrorParams(), null);
    }

    public static <T> InternalResponse<T> buildCommonFailResp(ErrorType errorType, Integer errorCode,
                                                              ErrorDetailDTO errorDetail) {
        InternalResponse<T> resp = buildCommonFailResp(errorType, errorCode);
        resp.setErrorDetail(errorDetail);
        return resp;
    }
}
