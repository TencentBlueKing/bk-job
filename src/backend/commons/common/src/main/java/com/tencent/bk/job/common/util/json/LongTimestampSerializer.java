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

package com.tencent.bk.job.common.util.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.tencent.bk.job.common.util.JobContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 将整形序列化成可读时间的 Json 序列化器
 *
 * @since 15/1/2020 19:52
 */
@Slf4j
public class LongTimestampSerializer extends JsonSerializer<Object> {
    private static final long MAX_TIMESTAMP = 9999999999L;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            return;
        }
        if (value instanceof Long) {
            // Process simple long value
            gen.writeString(convertLongToDateString((Long) value));
        } else if (value instanceof List) {
            // Process a list of long value
            gen.writeStartArray();
            if (CollectionUtils.isNotEmpty((List) value)) {
                if (((List) value).get(0) instanceof Long) {
                    ((List<Long>) value).forEach(time -> {
                        try {
                            gen.writeString(convertLongToDateString(time));
                        } catch (IOException e) {
                            log.error("Fail to serialize", e);
                        }
                    });
                }
            }
            gen.writeEndArray();

        }
    }

    private String convertLongToDateString(Long timestamp) {
        ZonedDateTime zonedDateTime;
        // Determining timestamp unit
        if (timestamp > MAX_TIMESTAMP) {
            // millisecond
            zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), JobContextUtil.getTimeZone());
        } else {
            // second
            zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamp), JobContextUtil.getTimeZone());
        }
        return FORMATTER.format(zonedDateTime);
    }
}
