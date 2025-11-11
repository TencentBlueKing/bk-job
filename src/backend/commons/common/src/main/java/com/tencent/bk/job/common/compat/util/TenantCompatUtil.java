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

package com.tencent.bk.job.common.compat.util;

import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.constant.CompatibleType;
import com.tencent.bk.job.common.constant.TenantIdConstants;
import com.tencent.bk.job.common.util.StackTraceUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 多租户兼容工具类
 */
@Slf4j
public class TenantCompatUtil {

    @Deprecated
    @CompatibleImplementation(
        name = "tenant",
        explain = "兼容发布过程中老的调用，发布完成后删除",
        deprecatedVersion = "3.12.x",
        type = CompatibleType.DEPLOY
    )
    public static String getTenantIdWithDefault(String tenantId) {
        if (StringUtils.isNotBlank(tenantId)) {
            return tenantId;
        }
        log.warn(
            "CompatibleImplementation: getTenantIdWithDefault is still work with default, please check stack:{}",
            StackTraceUtil.getCurrentStackTrace()
        );
        return TenantIdConstants.DEFAULT_TENANT_ID;
    }
}
