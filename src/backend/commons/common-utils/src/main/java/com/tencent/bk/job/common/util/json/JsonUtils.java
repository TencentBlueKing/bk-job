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

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * JSON工具
 */
@Slf4j
public class JsonUtils {

    private static final Map<String, JsonMapper> JSON_MAPPERS = new HashMap<>();

    /**
     * 序列化时忽略bean中的某些字段,字段需要使用SkipLogFields注解
     * 注意：只对第一层的字段生效，嵌套的不生效
     *
     * @param bean
     * @return Json
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
     * 序列化时忽略bean中的某些字段,字段需要使用SkipLogFields注解
     * 支持递归处理嵌套对象中的@SkipLogFields注解
     *
     * @param bean 待序列化的对象
     * @param <T>  对象类型
     * @return Json字符串
     * @see SkipLogFields
     */
    public static <T> String toJsonWithoutSkippedFieldsRecursively(T bean) {
        if (bean == null) {
            return "null";
        }

        String cacheKey = "__skipLogFieldsRecursive__" + bean.getClass().getName();
        
        return JSON_MAPPERS.computeIfAbsent(cacheKey, (String s) -> {
            JsonMapper nonEmptyMapper = JsonMapper.nonEmptyMapper();

            Map<Class<?>, Set<String>> classSkipFieldsMap = new HashMap<>();
            Set<Class<?>> visitedClasses = new HashSet<>();
            collectSkipFieldsRecursively(bean.getClass(), classSkipFieldsMap, visitedClasses);
            
            if (!classSkipFieldsMap.isEmpty()) {
                for (Class<?> clazz : classSkipFieldsMap.keySet()) {
                    nonEmptyMapper.getMapper().addMixIn(clazz, SkipLogFields.class);
                }
                SimpleBeanPropertyFilter customFilter = new SimpleBeanPropertyFilter() {
                    @Override
                    protected boolean include(BeanPropertyWriter writer) {
                        return includeProperty(writer);
                    }

                    @Override
                    protected boolean include(PropertyWriter writer) {
                        return includeProperty(writer);
                    }

                    private boolean includeProperty(PropertyWriter writer) {
                        Class<?> declaringClass = writer.getMember().getDeclaringClass();
                        String fieldName = writer.getName();

                        Set<String> skipFields = classSkipFieldsMap.get(declaringClass);
                        if (skipFields != null && skipFields.contains(fieldName)) {
                            return false;
                        }
                        return true;
                    }
                };
                
                FilterProvider filterProvider = new SimpleFilterProvider()
                    .addFilter(SkipLogFields.class.getAnnotation(JsonFilter.class).value(), customFilter);
                nonEmptyMapper.getMapper().setFilterProvider(filterProvider);
            }
            
            return nonEmptyMapper;
        }).toJson(bean);
    }

    /**
     * 循环收集类及其嵌套类中标记了@SkipLogFields的字段，避免递归
     *
     * @param clazz              当前处理的类
     * @param classSkipFieldsMap 存储每个类需要跳过的字段
     * @param visitedClasses     已访问的类
     */
    private static void collectSkipFieldsRecursively(Class<?> clazz, 
                                                     Map<Class<?>, Set<String>> classSkipFieldsMap,
                                                     Set<Class<?>> visitedClasses) {
        if (clazz == null) {
            return;
        }

        Deque<Class<?>> classQueue = new ArrayDeque<>();
        classQueue.add(clazz);
        while (!classQueue.isEmpty()) {
            Class<?> currentTopClass = classQueue.poll();
            if (visitedClasses.contains(currentTopClass)) {
                continue;
            }

            // 基本类型已是最小单位，跳过
            if (currentTopClass.isPrimitive() 
                || currentTopClass.getName().startsWith("java.") 
                || currentTopClass.getName().startsWith("javax.")) {
                continue;
            }
            visitedClasses.add(currentTopClass);
            // 遍历当前类和父类
            Class<?> currentClass = currentTopClass;
            while (currentClass != null && currentClass != Object.class) {
                Field[] fields = currentClass.getDeclaredFields();
                for (Field field : fields) {
                    if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                        continue;
                    }
                    SkipLogFields fieldAnnotation = field.getAnnotation(SkipLogFields.class);
                    if (fieldAnnotation != null) {
                        Set<String> skipFields = classSkipFieldsMap.computeIfAbsent(currentClass, k -> new HashSet<>());
                        if (fieldAnnotation.value().trim().length() > 0) {
                            skipFields.add(fieldAnnotation.value());
                        } else {
                            skipFields.add(field.getName());
                        }
                    }

                    Class<?> fieldType = field.getType();
                    // 集合和map类型需处理泛型
                    if (Collection.class.isAssignableFrom(fieldType) || Map.class.isAssignableFrom(fieldType)) {
                        Type genericType = field.getGenericType();
                        if (genericType instanceof ParameterizedType) {
                            ParameterizedType parameterizedType = (ParameterizedType) genericType;
                            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                            for (Type typeArg : actualTypeArguments) {
                                if (typeArg instanceof Class) {
                                    Class<?> typeArgClass = (Class<?>) typeArg;
                                    if (!visitedClasses.contains(typeArgClass)) {
                                        classQueue.add(typeArgClass);
                                    }
                                }
                            }
                        }
                    } else if (!fieldType.isPrimitive() && !fieldType.isEnum() && !fieldType.isArray()) {
                        if (!visitedClasses.contains(fieldType)) {
                            classQueue.add(fieldType);
                        }
                    }
                }
                
                currentClass = currentClass.getSuperclass();
            }
        }
    }

    /**
     * 从Json串中解析成bean对象,支持参数泛型
     *
     * @param jsonString    json
     * @param typeReference 类型
     * @param <T>           bean Class 类型
     * @return bean
     */
    public static <T> T fromJson(String jsonString, TypeReference<T> typeReference) throws JsonParseException {
        return JSON_MAPPERS.computeIfAbsent("__all__", s -> JsonMapper.getAllOutPutMapper()).fromJson(jsonString,
            typeReference);
    }

    /**
     * 从Json串中解析成bean对象
     *
     * @param jsonString json
     * @param beanClass  bean Class 类型
     * @param <T>        bean Class 类型
     * @return bean
     */
    public static <T> T fromJson(String jsonString, Class<T> beanClass) throws JsonParseException {
        return JSON_MAPPERS.computeIfAbsent("__all__", s -> JsonMapper.getAllOutPutMapper()).fromJson(jsonString,
            beanClass);
    }

    /**
     * 创建输出所有字段的Json，不管字段值是默认值 还是等于 null 还是空集合的字段，全输出,可用于外部接口协议输出
     *
     * @param bean bean
     * @param <T>  bean
     * @return json
     */
    public static <T> String toJson(T bean) throws JsonParseException {
        return JSON_MAPPERS.computeIfAbsent("__all__", s -> JsonMapper.getAllOutPutMapper()).toJson(bean);
    }

    public static <T> String toNonEmptyJson(T bean) {
        return JSON_MAPPERS.computeIfAbsent("__non_empty__", s -> JsonMapper.nonEmptyMapper()).toJson(bean);
    }

    public static <T> String toNonDefault(T bean) {
        return JSON_MAPPERS.computeIfAbsent("__non_default__", s -> JsonMapper.nonDefaultMapper()).toJson(bean);
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
