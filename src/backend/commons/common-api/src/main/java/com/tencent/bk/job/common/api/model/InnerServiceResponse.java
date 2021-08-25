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

package com.tencent.bk.job.common.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.model.error.ErrorDetail;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.JobContextUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@Getter
@Setter
@ToString
@NoArgsConstructor
@ApiModel("服务间调用通用返回结构")
public class InnerServiceResponse<T> {
    public static final Integer SUCCESS_CODE = 0;
    public static final Integer COMMON_FAIL_CODE = 1;
    private static volatile MessageI18nService i18nService;

    @ApiModelProperty("是否成功")
    private boolean success;

    @ApiModelProperty("返回码")
    private Integer code;

    @ApiModelProperty("错误信息")
    private String errorMsg;

    @ApiModelProperty("请求成功返回的数据")
    private T data;

    @ApiModelProperty("请求 ID")
    private String requestId;

    @ApiModelProperty("鉴权结果，当返回码为1238001时，该字段有值")
    @JsonProperty("authResult")
    private AuthResult authResult;

    @ApiModelProperty("错误详情")
    @JsonProperty("errorDetail")
    private ErrorDetail errorDetail;

    public InnerServiceResponse(Integer code, String errorMsg, T data) {
        this.code = code;
        this.errorMsg = errorMsg;
        this.data = data;
        this.requestId = JobContextUtil.getRequestId();
    }

    private static MessageI18nService getI18nService() {
        if (i18nService == null) {
            synchronized (InnerServiceResponse.class) {
                if (i18nService == null) {
                    i18nService = ApplicationContextRegister.getBean(MessageI18nService.class);
                }
            }
        }
        return i18nService;
    }

    public static <T> InnerServiceResponse<T> buildSuccessResp(T data) {
        InnerServiceResponse<T> resp = new InnerServiceResponse<>(SUCCESS_CODE, null, data);
        resp.success = true;
        return resp;
    }

    public static <T> InnerServiceResponse<T> buildAuthFailResp(AuthResult authResult) {
        MessageI18nService i18nService = getI18nService();
        String message = null;
        if (i18nService != null) {
            message = i18nService.getI18n(String.valueOf(ErrorCode.USER_NO_PERMISSION_COMMON));
        } else {
            log.warn("cannot find available i18nService");
        }
        InnerServiceResponse<T> resp = new InnerServiceResponse<T>(ErrorCode.USER_NO_PERMISSION_COMMON, message, null);
        resp.success = false;
        resp.authResult = authResult;
        return resp;
    }

    public static <T> InnerServiceResponse<T> buildCommonFailResp(String msg) {
        InnerServiceResponse<T> resp = new InnerServiceResponse<>(COMMON_FAIL_CODE, msg, null);
        resp.success = false;
        return resp;
    }

    public static <T> InnerServiceResponse<T> buildCommonFailResp(Integer errorCode, String msg) {
        InnerServiceResponse<T> resp = new InnerServiceResponse<>(errorCode, msg, null);
        resp.success = false;
        return resp;
    }

    public static <T> InnerServiceResponse<T> buildCommonFailResp(Integer errorCode) {
        try {
            getI18nService();
        } catch (Exception e) {
            log.warn("cannot get i18nService from spring context");
        }
        String errorMsg = null;
        if (i18nService != null) {
            errorMsg = i18nService.getI18n(errorCode.toString());
        }
        InnerServiceResponse<T> resp = new InnerServiceResponse<>(errorCode, errorMsg, null);
        resp.success = false;
        return resp;
    }

    public static <T> InnerServiceResponse<T> buildCommonFailResp(Integer errorCode, MessageI18nService i18nService) {
        String errorMsg = i18nService.getI18n(String.valueOf(errorCode));
        if (StringUtils.isEmpty(errorMsg)) {
            errorMsg = String.valueOf(errorCode);
        }
        InnerServiceResponse<T> resp = new InnerServiceResponse<>(errorCode, errorMsg, null);
        resp.success = false;
        return resp;
    }

    public static <T> InnerServiceResponse<T> buildCommonFailResp(Integer errorCode, Object[] params) {
        getI18nService();
        return buildCommonFailResp(errorCode, params, i18nService);
    }

    public static <T> InnerServiceResponse<T> buildCommonFailResp(Integer errorCode, Object[] params,
                                                                  MessageI18nService i18nService) {
        String errorMsg = "";
        if (params != null && params.length > 0) {
            errorMsg = i18nService.getI18nWithArgs(String.valueOf(errorCode), params);
        } else {
            errorMsg = i18nService.getI18n(String.valueOf(errorCode));
        }
        if (StringUtils.isEmpty(errorMsg)) {
            errorMsg = String.valueOf(errorCode);
        }
        InnerServiceResponse<T> resp = new InnerServiceResponse<>(errorCode, errorMsg, null);
        resp.success = false;
        return resp;
    }

    public static <T> InnerServiceResponse<T> buildCommonFailResp(ServiceException e, MessageI18nService i18nService) {
        log.info("exception: {}|{}|{}", e.getErrorCode(), e.getErrorMsg(), e.getErrorParams());
        int errorCode = e.getErrorCode();
        String errorMsg = e.getErrorMsg();
        if (StringUtils.isEmpty(errorMsg)) {
            errorMsg = i18nService.getI18nWithArgs(String.valueOf(errorCode), e.getErrorParams());
            log.info("{}", errorMsg);
        }
        if (StringUtils.isEmpty(errorMsg)) {
            errorMsg = String.valueOf(errorCode);
        }
        return new InnerServiceResponse<>(e.getErrorCode(), errorMsg, null);
    }

    public static <T> InnerServiceResponse<T> buildValidateFailResp(MessageI18nService i18nService,
                                                                    ValidateResult validateResult) {
        if (validateResult.getErrorParams() != null && validateResult.getErrorParams().length > 0) {
            return InnerServiceResponse.buildCommonFailResp(validateResult.getErrorCode(),
                i18nService.getI18nWithArgs(String.valueOf(validateResult.getErrorCode()),
                    validateResult.getErrorParams()));
        } else {
            return InnerServiceResponse.buildCommonFailResp(validateResult.getErrorCode(),
                i18nService.getI18n(String.valueOf(validateResult.getErrorCode())));
        }
    }

    public static <T> InnerServiceResponse<T> buildCommonFailResp(int errorCode, ErrorDetail errorDetail,
                                                                  MessageI18nService i18nService) {
        String errorMsg = i18nService.getI18n(String.valueOf(errorCode));
        InnerServiceResponse<T> esbResp = new InnerServiceResponse<>(errorCode, errorMsg, null);
        esbResp.setErrorDetail(errorDetail);
        return esbResp;
    }
}
