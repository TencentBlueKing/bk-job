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

package com.tencent.bk.job.common.mysql.listener;

import lombok.extern.slf4j.Slf4j;
import org.jooq.ExecuteContext;
import org.jooq.ExecuteListener;

import java.sql.Connection;

/**
 * 打印SQL连接信息的监听器，用于排查某些连接相关的问题
 */
@Slf4j
public class ConnectionLoggingListener implements ExecuteListener {
    @Override
    public void prepareStart(ExecuteContext ctx) {
        try {
            logConnectionInfo(ctx);
        } catch (Throwable t) {
            log.warn("Fail to logConnectionInfo", t);
        }
    }

    /**
     * 打印SQL连接信息
     *
     * @param ctx SQL执行上下文
     */
    private void logConnectionInfo(ExecuteContext ctx) {
        Connection connection = ctx.connection();
        int connectionHashId = connection != null ? System.identityHashCode(connection) : -1;
        if (connection == null) {
            log.warn("connection is null");
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("connectionHashId=" + connectionHashId);
        }
    }

    @Override
    public void renderEnd(ExecuteContext ctx) {
        if (log.isDebugEnabled()) {
            log.debug("SQL=" + ctx.sql());
        }
    }
}
