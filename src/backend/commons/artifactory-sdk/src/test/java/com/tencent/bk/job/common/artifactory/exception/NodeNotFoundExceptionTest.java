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

import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.HttpStatusException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NodeNotFoundExceptionTest {

    @Test
    void shouldRenderRuntimeClassNameWithCurrentLogback() {
        HttpStatusException cause = new HttpStatusException(
            "https://example.com/repo/file.tgz",
            400,
            "Bad Request",
            "{\"code\":251010,\"message\":\"Node [file.tgz] not found\"}"
        );
        NodeNotFoundException exception = new NodeNotFoundException(
            cause,
            ErrorCode.CAN_NOT_FIND_NODE_IN_ARTIFACTORY,
            new Object[]{"Node [file.tgz] not found"}
        );

        String stackTrace = ThrowableProxyUtil.asString(new ThrowableProxy(exception));

        assertThat(exception.toString())
            .startsWith(NodeNotFoundException.class.getName() + ": " + HttpStatusException.class.getName());
        assertThat(stackTrace)
            .startsWith(exception + System.lineSeparator())
            .contains("Caused by: " + cause);
    }
}
