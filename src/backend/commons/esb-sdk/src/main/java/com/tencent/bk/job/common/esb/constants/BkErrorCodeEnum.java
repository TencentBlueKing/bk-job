package com.tencent.bk.job.common.esb.constants;

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

/**
 * 蓝鲸通用错误分类定义
 */
public enum BkErrorCodeEnum {
    /**
     * 参数不合法
     */
    INVALID_ARGUMENT("INVALID_ARGUMENT", 400),
    /**
     * 参数符合参数格式，但参数不符合业务规则
     */
    INVALID_REQUEST("INVALID_REQUEST", 400),
    /**
     * 客户端指定了无效范围
     */
    OUT_OF_RANGE("OUT_OF_RANGE", 400),
    /**
     * 请求无法在当前系统状态下执行，例如删除非空目录
     */
    FAILED_PRECONDITION("FAILED_PRECONDITION", 400),
    /**
     * 未提供身份认证凭证
     */
    UNAUTHENTICATED("UNAUTHENTICATED", 401),
    /**
     * 权限中心没有相关权限(有协议要求)
     */
    IAM_NO_PERMISSION("IAM_NO_PERMISSION", 403),
    /**
     * 没有相关权限(非权限中心)
     */
    NO_PERMISSION("NO_PERMISSION", 403),
    /**
     * 资源不存在
     */
    NOT_FOUND("NOT_FOUND", 404),
    /**
     * 客户端尝试创建的资源已存在
     */
    ALREADY_EXISTS("ALREADY_EXISTS", 409),
    /**
     * 并发冲突，例如读取/修改/写入冲突
     */
    ABORTED("ABORTED", 409),
    /**
     * 超过频率限制
     */
    RATELIMIT_EXCEED("RATELIMIT_EXCEED", 429),
    /**
     * 资源配额不足
     */
    RESOURCE_EXHAUSTED("RESOURCE_EXHAUSTED", 429),
    /**
     * 出现内部服务器错误
     */
    INTERNAL("INTERNAL", 500),
    /**
     * 出现未知的服务器错误
     */
    UNKNOWN("UNKNOWN", 500),
    /**
     * API 方法未通过服务器实现
     */
    NOT_IMPLEMENTED("NOT_IMPLEMENTED", 501),
    /**
     * 服务不可用。通常是由于服务器宕机了
     */
    UNAVAILABLE("UNAVAILABLE", 503);

    private final String errorCode;
    private final int statusCode;

    BkErrorCodeEnum(String errorCode, int statusCode) {
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public static BkErrorCodeEnum valOf(String errorCode) {
        for (BkErrorCodeEnum errorType : values()) {
            if (errorType.getErrorCode().equals(errorCode)) {
                return errorType;
            }
        }
        throw new IllegalArgumentException("No BkErrorCodeEnum constant: " + errorCode);
    }
}
