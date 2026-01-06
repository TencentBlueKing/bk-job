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
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.model.iam.OpenApiApplyPermissionDTO;
import com.tencent.bk.job.common.model.error.ErrorDetailDTO;
import com.tencent.bk.job.common.model.error.FieldViolationDTO;
import com.tencent.bk.job.common.util.I18nUtil;
import com.tencent.bk.job.common.util.JobContextUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EsbV4RespError {

    /**
     * 错误码,语义化的英文标识, 整个蓝鲸会定义一套通用的错误大分类;
     * 作用: 上游编码基于这个做代码层面的逻辑判断(所以必须是确定的枚举); 枚举; 不会有 .ABC 子分类, 不允许各系统自定义
     *
     * @see V4ErrorCodeEnum
     *
     */
    private String code;

    /**
     * 给用户看到, 能看懂的说明, 需要支持国际化
     */
    private String message;

    /**
     * 返回的数据用于给调用方针对这个code做相应的一些处理, 例如无权限返回申请权限信息, 登录认证失败返回跳转url等
     * 如无 IAM 权限的响应：
     * "data": {
     *   "system": "bk_job",
     *   "system_name": "作业平台",
     *   "actions": []
     * }
     *
     */
    private Object data;

    /**
     * 错误详情(这个不会展示给用户, 在前端弹窗隐藏但是可以复制出来; 会出现在日志中)
     * 场景:
     * a) 前端表单校验多个字段报错的场景
     * b) 后台多个错误的场景
     * c) 后台 error wrap 的场景
     *
     */
    private List<V4RespSubError> details;

    /**
     * 根据Job内部的 ErrorCode 构造一般失败的请求
     *
     * @param v4ErrorCodeEnum 新蓝鲸协议的状态码, 为HTTP状态码的描述, 为可读的英文字符串
     * @see V4ErrorCodeEnum
     * @param errorCode Job内部错误码, 为数字
     * @see ErrorCode
     */
    public static EsbV4RespError buildCommonError(V4ErrorCodeEnum v4ErrorCodeEnum,
                                                  Integer errorCode,
                                                  Object[] errorParams) {
        EsbV4RespError error = new EsbV4RespError();
        error.setCode(v4ErrorCodeEnum.getCode());
        String message = I18nUtil.getI18nMessage(String.valueOf(errorCode), errorParams);
        error.setMessage(message);
        V4RespSubError subError = V4RespSubError.builder().code(String.valueOf(errorCode)).message(message).build();
        error.setDetails(Collections.singletonList(subError));
        return error;
    }

    public static EsbV4RespError buildFieldViolationError(V4ErrorCodeEnum v4ErrorCodeEnum,
                                                          String errorMsg,
                                                          ErrorDetailDTO errorDetail) {
        EsbV4RespError error = new EsbV4RespError();
        error.setCode(v4ErrorCodeEnum.getCode());
        error.setMessage(errorMsg);
        // 填充子错误
        if (errorDetail != null
            && errorDetail.getBadRequestDetail() != null
            && CollectionUtils.isNotEmpty(errorDetail.getBadRequestDetail().getFieldViolations())) {
            List<V4RespSubError> details = new ArrayList<>();
            for (FieldViolationDTO fieldViolation : errorDetail.getBadRequestDetail().getFieldViolations()) {
                V4RespSubError subError = V4RespSubError.builder()
                    .code(String.valueOf(ErrorCode.FIELD_BIND_FAILED))
                    .message(fieldViolation.getDescription())
                    .build();
                details.add(subError);
            }
            error.setDetails(details);
        }
        return error;
    }

    public static EsbV4RespError buildBadRequestError(String errorMsg) {
        EsbV4RespError error = new EsbV4RespError();
        error.setCode(V4ErrorCodeEnum.INVALID_ARGUMENT.getCode());
        error.setMessage(errorMsg);
        return error;
    }

    public static EsbV4RespError buildBadRequestError() {
        return buildBadRequestError(I18nUtil.getI18nMessage(String.valueOf(ErrorCode.BAD_REQUEST)));
    }

    public static EsbV4RespError buildBadRequestError(Integer errorCode, Object[] errorParams) {
        String message = I18nUtil.getI18nMessage(String.valueOf(errorCode), errorParams);
        EsbV4RespError error = buildBadRequestError(message);
        V4RespSubError subError = V4RespSubError.builder().code(String.valueOf(errorCode)).message(message).build();
        error.setDetails(Collections.singletonList(subError));
        return error;
    }

    /**
     * 权限不足
     * 蓝鲸协议要求在data中写入权限信息
     * "data": {
     *    "system": "bk_job",
     *    "system_name": "作业平台",
     *    "actions": []
     * }
     */
    public static EsbV4RespError buildPermissionDeniedError(OpenApiApplyPermissionDTO applyPermissionDTO) {
        EsbV4RespError error = new EsbV4RespError();
        error.setCode(V4ErrorCodeEnum.NO_PERMISSION.getCode());
        error.setMessage(I18nUtil.getI18nMessage(
            String.valueOf(ErrorCode.BK_PERMISSION_DENIED),
            new String[]{JobContextUtil.getUsername()})
        );
        error.setData(applyPermissionDTO);
        return error;
    }

}
