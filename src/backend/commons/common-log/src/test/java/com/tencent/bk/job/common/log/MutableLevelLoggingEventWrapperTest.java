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

package com.tencent.bk.job.common.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.tencent.bk.job.common.log.pojo.event.MutableLevelLoggingEventWrapper;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MutableLevelLoggingEventWrapperTest {

    @Test
    public void testDelegate() {
        ILoggingEvent event = new LoggingEvent(
            "fqcn",
            (Logger) LoggerFactory.getLogger(MutableLevelLoggingEventWrapperTest.class),
            Level.WARN,
            "message",
            new Throwable("simple-t"),
            new Object[]{}
        );
        ILoggingEvent wrappedEvent = new MutableLevelLoggingEventWrapper(event, Level.DEBUG);

        assertEquals(event.getLoggerName(), wrappedEvent.getLoggerName());
        assertEquals(event.getFormattedMessage(), wrappedEvent.getFormattedMessage());
        assertEquals(event.getMessage(), wrappedEvent.getMessage());
        assertEquals(event.getThreadName(), wrappedEvent.getThreadName());
        assertEquals(event.getArgumentArray(), wrappedEvent.getArgumentArray());
        assertEquals(event.getCallerData(), wrappedEvent.getCallerData());
        assertEquals(event.getLoggerContextVO(), wrappedEvent.getLoggerContextVO());
        assertEquals(event.getMarker(), wrappedEvent.getMarker());
        assertEquals(event.getMDCPropertyMap(), wrappedEvent.getMDCPropertyMap());
        assertEquals(event.getThrowableProxy(), wrappedEvent.getThrowableProxy());
        assertEquals(event.getTimeStamp(), wrappedEvent.getTimeStamp());
        assertEquals(event.hasCallerData(), wrappedEvent.hasCallerData());
    }
}
