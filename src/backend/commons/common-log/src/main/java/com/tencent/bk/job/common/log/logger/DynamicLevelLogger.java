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

package com.tencent.bk.job.common.log.logger;

import lombok.experimental.Delegate;
import org.slf4j.Logger;
import org.slf4j.event.Level;

/**
 * 能够动态指定日志级别的Logger
 */
public class DynamicLevelLogger implements Logger, LevelLogger {

    @Delegate
    private final Logger delegate;

    public DynamicLevelLogger(Logger delegate) {
        this.delegate = delegate;
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public void log(Level level, String format, Object... arguments) {
        switch (level) {
            case TRACE:
                delegate.trace(format, arguments);
                break;
            case DEBUG:
                delegate.debug(format, arguments);
                break;
            case INFO:
                delegate.info(format, arguments);
                break;
            case WARN:
                delegate.warn(format, arguments);
                break;
            case ERROR:
                delegate.error(format, arguments);
                break;
            default:
                delegate.warn("Unexpected level: {}, use INFO", level);
                delegate.info(format, arguments);
        }
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public void log(Level level, String msg, Throwable t) {
        switch (level) {
            case TRACE:
                delegate.trace(msg, t);
                break;
            case DEBUG:
                delegate.debug(msg, t);
                break;
            case INFO:
                delegate.info(msg, t);
                break;
            case WARN:
                delegate.warn(msg, t);
                break;
            case ERROR:
                delegate.error(msg, t);
                break;
            default:
                delegate.warn("Unexpected level: {}, use INFO", level);
                delegate.info(msg, t);
        }
    }
}
