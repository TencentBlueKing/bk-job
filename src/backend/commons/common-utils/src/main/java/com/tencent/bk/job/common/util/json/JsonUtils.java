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

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * JSON工具
 */
@Slf4j
public class JsonUtils {

    private static final Map<String, JsonMapper> JSON_MAPPERS = new HashMap<>();

    /**
     * 序列化时忽略bean中的某些字段,字段需要使用SkipLogFields注解
     *
     * @param bean
     * @return
     * @see SkipLogFields
     */
    public static <T> String toJsonWithoutSkippedFields(T bean) {
        return JSON_MAPPERS.computeIfAbsent("__skipLogFields__" + bean.getClass().getName(), (String s) -> {
            JsonMapper nonEmptyMapper = JsonMapper.nonEmptyMapper();
            Class<?> aClass = bean.getClass();
            Set<String> skipFields = new HashSet<>();
            while (aClass != null) {
                Field[] fields = aClass.getDeclaredFields();
                for (Field field : fields) {
                    SkipLogFields fieldAnnotation = field.getAnnotation(SkipLogFields.class);
                    if (fieldAnnotation == null) {
                        continue;
                    }
                    if (fieldAnnotation.value().trim().length() > 0) {
                        skipFields.add(fieldAnnotation.value());
                    } else {
                        skipFields.add(field.getName());
                    }
                }
                aClass = aClass.getSuperclass();
            }
            if (!skipFields.isEmpty()) {
                nonEmptyMapper.getMapper().addMixIn(bean.getClass(), SkipLogFields.class);
                FilterProvider filterProvider = new SimpleFilterProvider()
                    .addFilter(SkipLogFields.class.getAnnotation(JsonFilter.class).value(),
                        SimpleBeanPropertyFilter.serializeAllExcept(skipFields));
                nonEmptyMapper.getMapper().setFilterProvider(filterProvider);

            }
            return nonEmptyMapper;
        }).toJson(bean);
    }

    /**
     * 从Json串中解析成bean对象,支持参数泛型
     *
     * @param jsonString
     * @param typeReference
     * @param <T>
     * @return
     */
    public static <T> T fromJson(String jsonString, TypeReference<T> typeReference) {
        return JSON_MAPPERS.computeIfAbsent("__all__", s -> JsonMapper.getAllOutPutMapper()).fromJson(jsonString,
            typeReference);
    }

    /**
     * 从Json串中解析成bean对象
     *
     * @param jsonString
     * @param beanClass
     * @param <T>
     * @return
     */
    public static <T> T fromJson(String jsonString, Class<T> beanClass) {
        return JSON_MAPPERS.computeIfAbsent("__all__", s -> JsonMapper.getAllOutPutMapper()).fromJson(jsonString,
            beanClass);
    }

    /**
     * 创建输出所有字段的Json，不管字段值是默认值 还是等于 null 还是空集合的字段，全输出,可用于外部接口协议输出
     *
     * @param bean
     * @param <T>
     * @return
     */
    public static <T> String toJson(T bean) {
        return JSON_MAPPERS.computeIfAbsent("__all__", s -> JsonMapper.getAllOutPutMapper()).toJson(bean);
    }

    public static <T> String toNonEmptyJson(T bean) {
        return JSON_MAPPERS.computeIfAbsent("__non_empty__", s -> JsonMapper.nonEmptyMapper()).toJson(bean);
    }

    public static <T> String toNonDefault(T bean) {
        return JSON_MAPPERS.computeIfAbsent("__non_default__", s -> JsonMapper.nonDefaultMapper()).toJson(bean);
    }

    public static <T> String toJson(String timeZoneStr, T bean) {

        return JSON_MAPPERS.computeIfAbsent(timeZoneStr + "_tz__all__", s -> {
            JsonMapper allOutPutMapper = JsonMapper.getAllOutPutMapper();
            if (timeZoneStr != null) {
                try {
                    TimeZone timeZone = TimeZone.getTimeZone(timeZoneStr);
                    allOutPutMapper.getMapper().setTimeZone(timeZone).setDateFormat(new SimpleDateFormat("yyyy-MM-dd " +
                        "HH:mm:ss Z"));
                } catch (Exception ignored) {
                }
            }
            return allOutPutMapper;
        }).toJson(bean);
    }

    public static JsonNode toJsonNode(String jsonStr) {
        try {
            return JSON_MAPPERS.computeIfAbsent("__all__", s -> JsonMapper.getAllOutPutMapper())
                .getMapper().readTree(jsonStr);
        } catch (IOException e) {
            String errorMsg = "Read JsonNode from string fail, jsonStr:" + jsonStr;
            log.error(errorMsg, e);
            return null;
        }
    }

}
