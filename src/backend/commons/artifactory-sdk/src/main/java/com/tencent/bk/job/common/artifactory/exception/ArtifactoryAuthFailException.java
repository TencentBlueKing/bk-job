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

package com.tencent.bk.job.common.artifactory.exception;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.UnauthenticatedException;

/**
 * 制品库认证失败异常（如：用户名/密码或访问令牌错误，bkrepo 返回 401）。
 * <p>
 * 直接继承 {@link UnauthenticatedException}，使其在 Web 层被映射为 HTTP 401，
 * 引导用户检查文件源凭证配置而非误以为作业平台异常。原始 bkrepo 报错保留在 cause 中（日志可见）。
 */
public class ArtifactoryAuthFailException extends UnauthenticatedException {

    public ArtifactoryAuthFailException(Throwable cause) {
        super(cause, ErrorCode.ARTIFACTORY_AUTH_FAILED);
    }
}
