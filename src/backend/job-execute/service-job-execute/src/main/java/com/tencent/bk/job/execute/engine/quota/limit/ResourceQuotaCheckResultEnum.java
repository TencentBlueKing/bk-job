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

package com.tencent.bk.job.execute.engine.quota.limit;

public enum ResourceQuotaCheckResultEnum {

    NO_LIMIT("no_limit"),
    RESOURCE_SCOPE_LIMIT("resource_scope_quota_limit"),
    APP_LIMIT("app_quota_limit"),
    SYSTEM_LIMIT("system_quota_limit");

    private final String value;

    ResourceQuotaCheckResultEnum(String value) {
        this.value = value;
    }

    public static ResourceQuotaCheckResultEnum valOf(String value) {
        for (ResourceQuotaCheckResultEnum resultEnum : values()) {
            if (resultEnum.value.equals(value)) {
                return resultEnum;
            }
        }
        throw new IllegalArgumentException("No ResourceQuotaCheckResultEnum constant: " + value);
    }

    public boolean isExceedLimit() {
        return this != NO_LIMIT;
    }
}
