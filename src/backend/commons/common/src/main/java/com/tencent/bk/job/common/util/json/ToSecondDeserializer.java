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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 将毫秒级或秒级时间戳时间转换为秒的反序列化器
 */
@Slf4j
public class ToSecondDeserializer extends JsonDeserializer<Object> {

    private static final long MAX_SECOND_TIMESTAMP = 9_999_999_999L;

    @Override
    public Object deserialize(JsonParser jsonParser,
                              DeserializationContext deserializationContext) throws IOException {
        JsonToken currentToken = jsonParser.getCurrentToken();
        
        if (currentToken == JsonToken.VALUE_NULL) {
            return null;
        }
        
        if (currentToken == JsonToken.VALUE_NUMBER_INT) {
            long timestamp = jsonParser.getLongValue();
            return convertToSecond(timestamp);
        }
        
        if (currentToken == JsonToken.START_ARRAY) {
            // 处理Long数组
            List<Long> result = new ArrayList<>();
            while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
                if (jsonParser.getCurrentToken() == JsonToken.VALUE_NUMBER_INT) {
                    long timestamp = jsonParser.getLongValue();
                    result.add(convertToSecond(timestamp));
                }
            }
            return result;
        }
        
        log.warn("Unexpected token type for timestamp deserialization: {}", currentToken);
        return null;
    }

    /**
     * 将时间戳转换为秒级时间戳
     * @param timestamp 时间戳（秒级或毫秒级）
     * @return 秒级时间戳
     */
    private Long convertToSecond(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        if (timestamp > MAX_SECOND_TIMESTAMP) {
            // 毫秒级时间戳，转换为秒级
            return timestamp / 1000;
        } else {
            // 已经是秒级时间戳
            return timestamp;
        }
    }
}
