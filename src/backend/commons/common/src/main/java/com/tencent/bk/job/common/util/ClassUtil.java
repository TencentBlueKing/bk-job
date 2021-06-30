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

package com.tencent.bk.job.common.util;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * @Date 2020/3/6
 * @Version 1.0
 */
@Slf4j
public class ClassUtil {
    private static <SrcClass, TargetClass> boolean isTargetPrimitive(
        SrcClass srcInstance,
        Class<TargetClass> targetClass
    ) {
        Object[] enumConstants = srcInstance.getClass().getEnumConstants();
        return (targetClass.isPrimitive()
            || targetClass == Boolean.class
            || targetClass == Byte.class
            || targetClass == Character.class
            || targetClass == Short.class
            || targetClass == Integer.class
            || targetClass == Long.class
            || targetClass == Float.class
            || targetClass == Double.class
            || (enumConstants != null && enumConstants.length > 0));
    }

    private static <SrcClass, TargetClass> TargetClass convertCollection(
        SrcClass srcInstance,
        Object targetInstance
    ) {
        try {
            //取得add方法
            Method addMethod = targetInstance.getClass().getDeclaredMethod("add", Object.class);
            addMethod.setAccessible(true);
            Object finalTargetInstance = targetInstance;
            ((Collection) srcInstance).forEach(item -> {
                try {
                    addMethod.invoke(finalTargetInstance, copyAttrsBetweenClasses(item, item.getClass()));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    log.error("error occurred when convert", e);
                }
            });
            return (TargetClass) targetInstance;
        } catch (NoSuchMethodException e) {
            log.error("error occurred when convert", e);
            return null;
        }
    }

    private static <SrcClass, TargetClass> TargetClass convertMap(
        SrcClass srcInstance,
        Object targetInstance
    ) {
        try {
            //取得put方法
            Method putMethod = targetInstance.getClass().getDeclaredMethod("put", Object.class, Object.class);
            putMethod.setAccessible(true);
            Object finalTargetInstance = targetInstance;
            ((Map) srcInstance).forEach((key, value) -> {
                try {
                    putMethod.invoke(finalTargetInstance, copyAttrsBetweenClasses(key, key.getClass()),
                        copyAttrsBetweenClasses(value, value.getClass()));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    log.error("error occurred when convert", e);
                }
            });
            return (TargetClass) targetInstance;
        } catch (NoSuchMethodException e) {
            log.error("error occurred when convert", e);
            return null;
        }
    }

    /**
     * 通过反射实现有属性交集的不同类实例属性复制
     *
     * @param srcInstance   源实例
     * @param targetClass   目标类Class对象
     * @param <SrcClass>    源类
     * @param <TargetClass> 目标类
     * @return 目标类实例
     */
    public static <SrcClass, TargetClass> TargetClass copyAttrsBetweenClasses(
        SrcClass srcInstance,
        Class<TargetClass> targetClass) {
        Object targetInstance = null;
        if (srcInstance == null) {
            return null;
        }
        //基本数据类型：直接强转
        if (isTargetPrimitive(srcInstance, targetClass)) {
            return (TargetClass) srcInstance;
        }
        //String类型：toString
        if (targetClass == String.class) {
            return (TargetClass) srcInstance.toString();
        }
        //集合与Map类型：分别单独处理
        int targetClassModifiers = targetClass.getModifiers();
        if (Modifier.isAbstract(targetClassModifiers)
            || Modifier.isInterface(targetClassModifiers)) {
            //目标类为抽象类或接口：用来源对象的类型实例化
            try {
                Constructor constructor = srcInstance.getClass().getDeclaredConstructor();
                constructor.setAccessible(true);
                targetInstance = constructor.newInstance();
            } catch (InstantiationException
                | IllegalAccessException
                | NoSuchMethodException
                | InvocationTargetException e) {
                log.error("error occurred when convert", e);
            }
        } else {
            //目标类可以实例化
            try {
                Constructor<TargetClass> constructor = targetClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                targetInstance = constructor.newInstance();
            } catch (InstantiationException
                | IllegalAccessException
                | NoSuchMethodException
                | InvocationTargetException e
            ) {
                log.error("error occurred when convert", e);
            }
        }
        if (srcInstance instanceof Collection) {
            TargetClass result = convertCollection(srcInstance, targetInstance);
            if (result != null) return result;
        } else if (srcInstance instanceof Map) {
            TargetClass result = convertMap(srcInstance, targetInstance);
            if (result != null) return result;
        }
        //自定义数据类型：遍历所有字段分别处理
        Field[] srcFields = srcInstance.getClass().getDeclaredFields();
        List<String> ignoredFields = new ArrayList<>();
        for (Field srcField : srcFields) {
            try {
                //设置可访问
                srcField.setAccessible(true);
                Field targetField = targetClass.getDeclaredField(srcField.getName());
                targetField.setAccessible(true);
                Class targetFieldType = targetField.getType();
                Object srcFieldValue = srcField.get(srcInstance);
                Type srcFieldGenericType = srcField.getGenericType();
                Type targetFieldGenericType = targetField.getGenericType();
                //泛型参数的转换
                if (srcFieldGenericType instanceof ParameterizedType) {
                    //目标泛型参数
                    ParameterizedType targetParameterizedType = (ParameterizedType) targetFieldGenericType;
                    if (srcFieldValue == null) {
                        targetField.set(targetInstance, null);
                    } else {
                        //处理Collection与Map
                        if (srcFieldValue instanceof Collection) {
                            Type targetActualType = targetParameterizedType.getActualTypeArguments()[0];
                            Collection targetFieldValue = (Collection) srcFieldValue.getClass().newInstance();
                            ((Collection) srcFieldValue).forEach(
                                item -> targetFieldValue.add(copyAttrsBetweenClasses(item, (Class) targetActualType)));
                            targetField.set(targetInstance, targetFieldValue);
                        } else if (srcFieldValue instanceof Map) {
                            Type[] targetActualTypes = targetParameterizedType.getActualTypeArguments();
                            Type targetActualKeyType = targetActualTypes[0];
                            Type targetActualValueType = targetActualTypes[1];
                            Map targetFieldValue = (Map) srcFieldValue.getClass().newInstance();
                            ((Map) srcFieldValue).forEach(
                                (key, value) -> {
                                    targetFieldValue.put(copyAttrsBetweenClasses(key, (Class) targetActualKeyType),
                                        copyAttrsBetweenClasses(value, (Class) targetActualValueType));
                                });
                            targetField.set(targetInstance, targetFieldValue);
                        } else {
                            //自定义类型中含泛型参数：递归拷贝
                            targetField.set(targetInstance, copyAttrsBetweenClasses(srcField.get(srcInstance),
                                targetFieldType));
                        }
                    }
                } else {
                    //递归拷贝
                    targetField.set(targetInstance, copyAttrsBetweenClasses(srcField.get(srcInstance),
                        targetFieldType));
                }
            } catch (NoSuchFieldException | IllegalAccessException ignore) {
                ignoredFields.add(srcField.getName());
            } catch (InstantiationException e) {
                log.error("error occurred when convert", e);
            } finally {
                if (!ignoredFields.isEmpty()) {
                    log.debug(String.format("%s-->%s:ignored fields:%s", srcInstance.getClass().getSimpleName(),
                        targetClass.getSimpleName(), String.join(",", ignoredFields)));
                }
            }
        }
        return (TargetClass) targetInstance;
    }
}
