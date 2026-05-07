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

package com.tencent.bk.job.common.cc.exception;

import com.tencent.bk.job.common.model.error.ErrorType;

/**
 * CMDB 动态分组不存在异常。
 * <p>
 * 当 CMDB 接口返回"未找到相关数据"（错误码 1199019）时抛出此异常，
 * 表示动态分组在 CMDB 中已被删除或不存在。这是一种已知的业务数据异常，
 * 不应触发重试，也不应打印 ERROR 日志；由上层业务逻辑决定如何处理（通常忽略并打印 INFO 日志）。
 */
public class CmdbDynamicGroupNotFoundException extends CmdbException {

    /**
     * 动态分组 ID
     */
    private final String dynamicGroupId;

    public CmdbDynamicGroupNotFoundException(ErrorType errorType,
                                             Integer errorCode,
                                             Object[] errorParams,
                                             String dynamicGroupId) {
        super(errorType, errorCode, errorParams);
        this.dynamicGroupId = dynamicGroupId;
    }

    public String getDynamicGroupId() {
        return dynamicGroupId;
    }
}
