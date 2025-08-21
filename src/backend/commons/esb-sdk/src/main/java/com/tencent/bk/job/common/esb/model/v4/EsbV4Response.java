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

package com.tencent.bk.job.common.esb.model.v4;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.model.iam.OpenApiApplyPermissionDTO;
import com.tencent.bk.job.common.model.error.ErrorDetailDTO;
import com.tencent.bk.job.common.util.I18nUtil;
import com.tencent.bk.job.common.util.JobContextUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class EsbV4Response<T> {

    /**
     * 正常时的响应，http状态码20X
     * 异常时为Null
     */
    private T data;

    /**
     * 异常时的响应，http状态码不为20X
     * 正常时为null
     */
    private EsbV4RespError error = null;

    /**
     * Job平台的request_id
     */
    @JsonProperty("job_request_id")
    private String jobRequestId;

    public EsbV4Response() {
        this.jobRequestId = JobContextUtil.getRequestId();
    }

    public static <T> EsbV4Response<T> success(T data) {
        EsbV4Response<T> resp = new EsbV4Response<>();
        resp.setData(data);
        return resp;
    }

    public static <T> EsbV4Response<T> buildFailedResponse(V4ErrorCodeEnum v4ErrorCodeEnum,
                                                           Integer errorCode,
                                                           Object[] errorParams) {
        EsbV4Response<T> resp = new EsbV4Response<>();
        EsbV4RespError error = EsbV4RespError.buildCommonError(v4ErrorCodeEnum, errorCode, errorParams);
        resp.setError(error);
        return resp;
    }

    public static <T> EsbV4Response<T> buildFailedResponse(V4ErrorCodeEnum v4ErrorCodeEnum, Integer errorCode) {
        return buildFailedResponse(v4ErrorCodeEnum, errorCode, new Object[]{});
    }

    public static <T> EsbV4Response<T> buildPermissionDeniedResponse(OpenApiApplyPermissionDTO permissionDetail) {
        EsbV4RespError error = EsbV4RespError.buildPermissionDeniedError(permissionDetail);
        EsbV4Response<T> resp = new EsbV4Response<>();
        resp.setError(error);
        return resp;
    }

    public static <T> EsbV4Response<T> badRequestResponse() {
        EsbV4Response<T> resp = new EsbV4Response<>();
        resp.setError(EsbV4RespError.buildBadRequestError());
        return resp;
    }

    public static <T> EsbV4Response<T> badRequestResponse(Integer errorCode, Object[] errorParams) {
        EsbV4RespError error = EsbV4RespError.buildBadRequestError(errorCode, errorParams);
        EsbV4Response<T> resp = new EsbV4Response<>();
        resp.setError(error);
        return resp;
    }

    public static <T> EsbV4Response<T> badRequestResponse(Integer errorCode) {
        EsbV4RespError error = EsbV4RespError.buildBadRequestError(errorCode, new Object[]{});
        EsbV4Response<T> resp = new EsbV4Response<>();
        resp.setError(error);
        return resp;
    }

    /**
     * 多个字段报错
     */
    public static <T> EsbV4Response<T> paramValidateFail(V4ErrorCodeEnum v4ErrorCodeEnum, ErrorDetailDTO errorDetail) {
        String errMsg = "";
        if (errorDetail != null
            && errorDetail.getBadRequestDetail() != null
            && CollectionUtils.isNotEmpty(errorDetail.getBadRequestDetail().getFieldViolations())
        ) {
            errMsg = I18nUtil.getI18nMessage(
                String.valueOf(ErrorCode.FIELD_BIND_FAILED),
                new String[]{errorDetail.getBadRequestDetail().getViolationFieldsStr()});
        }
        EsbV4RespError error = EsbV4RespError.buildFieldViolationError(v4ErrorCodeEnum, errMsg, errorDetail);
        EsbV4Response<T> resp = new EsbV4Response<>();
        resp.setError(error);
        return resp;
    }

}
