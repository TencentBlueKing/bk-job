/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.job.common.log.pojo.encoder;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.tencent.bk.job.common.log.pojo.event.MutableLevelLoggingEventWrapper;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * 自定义encoder，可对特定logger(s)打出的日志做日志级别的调整，此encoder包含 {@link PatternLayoutEncoder} 所有功能
 * 使用方法：
 *  将所需调整日志级别的logger路径用<targetLogger>包裹起来（以<包名>.<类名>为路径）
 *  将所需要调整的日志级别包裹在<originLoggingLevel>内
 *  将调整的目标日志级别包裹在<targetLoggingLevel>内
 *
 */
public class SpecificLoggerAdjustLevelEncoder extends PatternLayoutEncoder {
    private final Set<String> targetLoggerSet = new HashSet<>();
    @Setter
    private Level originLoggingLevel;
    @Setter
    private Level targetLoggingLevel;

    public void addTargetLogger(String targetLogger) {
        targetLoggerSet.add(targetLogger);
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        if (!isBegin()) {
            addWarn("[SpecificLoggerAdjustLevelEncoder]Not provide origin or target logging level");
            return super.encode(event);
        }

        ILoggingEvent wrappedEvent = event;
        if (targetLoggerSet.contains(event.getLoggerName()) && originLoggingLevel.equals(event.getLevel())) {
            wrappedEvent = new MutableLevelLoggingEventWrapper(event, targetLoggingLevel);
        }

        return super.encode(wrappedEvent);
    }

    private boolean isBegin() {
        return originLoggingLevel != null && targetLoggingLevel != null;
    }
}
