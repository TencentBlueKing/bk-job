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

package com.tencent.bk.job.common.util.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.io.IOException;
import java.util.List;

/**
 * 将秒级时间戳转换为毫秒级时间戳的 Json 序列化器
 * 可作用在序列化 Long、List<Long> 的场景
 */
@Slf4j
public class SecondToMillisSerializer extends JsonSerializer<Object> {

    private static final long MAX_SECOND_TIMESTAMP = 9_999_999_999L;

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            return;
        }
        if (value instanceof Long) {
            gen.writeNumber(convertSecondToMillis((Long) value));
        } else if (value instanceof List) {
            gen.writeStartArray();
            if (CollectionUtils.isNotEmpty((List<?>) value)) {
                if (((List<?>) value).get(0) instanceof Long) {
                    ((List<Long>) value).forEach(time -> {
                        try {
                            gen.writeNumber(convertSecondToMillis(time));
                        } catch (IOException e) {
                            log.error("Fail to serialize timestamp", e);
                        }
                    });
                }
            }
            gen.writeEndArray();
        } else {
            String fieldName = gen.getOutputContext().getCurrentName();
            log.warn(
                "Serialize fail because of unsupported type, fieldName: {}, fieldValue: {}, valueType: {}, "
                    + "ignore to serialize",
                fieldName, value, value.getClass()
            );
        }
    }

    /**
     * 将时间戳转换为毫秒级时间戳
     * @param timestamp 时间戳（秒级或毫秒级）
     * @return 毫秒级时间戳
     */
    private Long convertSecondToMillis(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        if (timestamp > MAX_SECOND_TIMESTAMP) {
            // 已经是毫秒级时间戳
            return timestamp;
        } else {
            // 秒级时间戳，转换为毫秒级
            return timestamp * 1000;
        }
    }
}
