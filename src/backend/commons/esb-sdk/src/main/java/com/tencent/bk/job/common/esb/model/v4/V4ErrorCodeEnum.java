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

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * 蓝鲸 APIGW 新协议中，错误请求的 responseBody.error.code 字段，
 * 需要是语义化的英文标识, 整个蓝鲸会定义一套通用的错误大分类
 */
@Getter
public enum V4ErrorCodeEnum {

    // 参数不符合参数格式
    INVALID_ARGUMENT(400, "INVALID_ARGUMENT"),
    // 客户端指定了无效范围
    OUT_OF_RANGE(400, "OUT_OF_RANGE"),
    // 请求无法在当前系统状态下执行，例如删除非空目录
    FAILED_PRECONDITION(400, "FAILED_PRECONDITION"),


    // 未提供身份认证凭证
    UNAUTHENTICATED(401, "UNAUTHENTICATED"),


    // 权限中心没有相关权限
    IAM_NO_PERMISSION(403, "IAM_NO_PERMISSION"),
    // 没有相关权限(非权限中心)
    NO_PERMISSION(403, "NO_PERMISSION"),

    // 资源不存在
    NOT_FOUND(404, "NOT_FOUND"),

    // 客户端尝试创建的资源已存在
    ALREADY_EXISTS(409, "ALREADY_EXISTS"),
    // 并发冲突，例如读取/修改/写入冲突
    ABORTED(409, "ABORTED"),

    // 超过频率限制
    RATELIMIT_EXCEED(429, "RATELIMIT_EXCEED"),
    // 资源配额不足
    RESOURCE_EXHAUSTED(429, "RESOURCE_EXHAUSTED"),

    // 出现内部服务器错误
    INTERNAL(500, "INTERNAL"),
    // 出现未知的服务器错误
    UNKNOWN(500, "UNKNOWN"),

    // API 方法未通过服务器实现
    NOT_IMPLEMENTED(501, "NOT_IMPLEMENTED");


    private final Integer status;
    private final String code;

    V4ErrorCodeEnum(Integer status, String code) {
        this.status = status;
        this.code = code;
    }

    public static Integer getStatus(String code) {
        if (StringUtils.isBlank(code)) {
            throw new IllegalArgumentException("V4ErrorCode enum's code cannot be empty");
        }

        for (V4ErrorCodeEnum errorCode : V4ErrorCodeEnum.values()) {
            if (errorCode.code.equals(code)) {
                return errorCode.status;
            }
        }

        throw new IllegalArgumentException("V4ErrorCodeEnum enum has no code: " + code);
    }
}
