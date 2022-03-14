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

import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Description 字符串处理工具类
 * @Date 2020/3/6
 * @Version 1.0
 */
@Slf4j
public class StringUtil {

    /**
     * 使用对象中的字段值替换路径中的占位符
     *
     * @param path 原始路径
     * @param obj  数据对象
     * @return 替换后的路径
     */
    public static String replacePathVariables(String path, Object obj) {
        if (obj == null) return path;
        List<String> placeholderList = findOneRegexPatterns(path, "(\\{.*?\\})");
        for (String placeholder : placeholderList) {
            String fieldName = placeholder.substring(1, placeholder.length() - 1);
            Object fieldValue = ReflectUtil.getFieldValue(obj, fieldName);
            if (null != fieldValue) {
                path = path.replace(placeholder, fieldValue.toString());
            } else {
                log.warn("Fail to parse path variable {} because cannot find field {} in {}", placeholder, fieldName,
                    JsonUtils.toJson(obj));
            }
        }
        return path;
    }

    /**
     * 去除字符串前后缀
     *
     * @param rawStr 原始字符串
     * @param fix    前缀/后缀
     * @return 去除前后缀后的字符串
     */
    public static String removePrefixAndSuffix(String rawStr, String fix) {
        rawStr = removePrefix(rawStr, fix);
        rawStr = removeSuffix(rawStr, fix);
        return rawStr;
    }

    /**
     * 去除字符串前缀
     *
     * @param rawStr 原始字符串
     * @param prefix 前缀
     * @return 去除前缀后的字符串
     */
    public static String removePrefix(String rawStr, String prefix) {
        if (rawStr == null) return null;
        if (prefix == null) return rawStr;
        while (rawStr.startsWith(prefix)) {
            rawStr = rawStr.substring(prefix.length());
        }
        return rawStr;
    }

    /**
     * 去除字符串后缀
     *
     * @param rawStr 原始字符串
     * @param suffix 后缀
     * @return 去除后缀后的字符串
     */
    public static String removeSuffix(String rawStr, String suffix) {
        if (rawStr == null) return null;
        if (suffix == null) return rawStr;
        while (rawStr.endsWith(suffix)) {
            rawStr = rawStr.substring(0, rawStr.length() - suffix.length());
        }
        return rawStr;
    }

    /**
     * 将集合拼接为字符串，默认使用英文逗号作为分隔符
     *
     * @param collection 原始集合
     * @param <T>        数据类型
     * @return 拼接后的字符串
     */
    public static <T> String concatCollection(Collection<T> collection) {
        return concatCollection(collection, ",");
    }

    /**
     * 将集合拼接为字符串
     *
     * @param collection 原始集合
     * @param separator  分隔符
     * @param <T>        数据类型
     * @return 拼接后的字符串
     */
    public static <T> String concatCollection(Collection<T> collection, String separator) {
        String str = null;
        if (collection != null) {
            str = collection.stream().map(Object::toString).collect(Collectors.joining(separator));
        }
        return str;
    }

    /**
     * 从字符串提取List
     *
     * @param str       字符串
     * @param clazz     目标类型Class对象
     * @param separator 分隔符
     * @param <T>       目标类型
     * @return list
     */
    public static <T> List<T> strToList(String str, Class<T> clazz, String separator) {
        if (str == null || str.isEmpty() || StringUtils.isBlank(str)) {
            return Collections.emptyList();
        }
        return Arrays.stream(str.trim().split(separator)).filter(StringUtils::isNotBlank)
            .map(it -> (T) forceConvert(it, clazz)).collect(Collectors.toList());
    }

    private static <T> Object forceConvert(String str, Class<T> clazz) {
        if (Long.class.equals(clazz)) {
            return Long.parseLong(str);
        } else if (Integer.class.equals(clazz)) {
            return Integer.parseInt(str);
        } else if (Short.class.equals(clazz)) {
            return Short.parseShort(str);
        } else if (Byte.class.equals(clazz)) {
            return Byte.parseByte(str);
        } else if (Float.class.equals(clazz)) {
            return Float.parseFloat(str);
        } else if (Double.class.equals(clazz)) {
            return Double.parseDouble(str);
        }
        return str;
    }

    /**
     * 从字符串中提取符合指定正则模式的占位符
     *
     * @param rawData 原始字符串
     * @param regex   正则表达式
     * @return 占位符字符串
     */
    public static List<String> findOneRegexPatterns(String rawData, String regex) {
        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(rawData);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    private static List<Pair<String, String>> findTwoRegexPatterns(String rawData, String regex) {
        List<Pair<String, String>> result = new ArrayList<>();
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(rawData);
        while (matcher.find()) {
            result.add(Pair.of(matcher.group(1), matcher.group(2)));
        }
        return result;
    }

    /**
     * 使用变量Map中指定的值对原始字符串的指定模式进行替换，默认替换深度为3
     *
     * @param rawStr       原始字符串
     * @param pattern      要替换的子串模式
     * @param variablesMap 变量表
     * @return 替换后的字符串
     */
    public static String replaceByRegex(String rawStr, String pattern, Map<String, String> variablesMap) {
        return replaceByRegex(rawStr, pattern, variablesMap, 3);
    }

    /**
     * 使用变量Map中指定的值对原始字符串的指定模式进行替换
     *
     * @param rawStr       原始字符串
     * @param pattern      要替换的子串模式
     * @param variablesMap 变量表
     * @param depth        变量中包含变量时的最大递归替换深度
     * @return 替换后的字符串
     */
    public static String replaceByRegex(String rawStr, String pattern, Map<String, String> variablesMap, int depth) {
        log.debug("rawStr={},pattern={},variablesMap={},depth={}", rawStr, pattern, variablesMap, depth);
        String resultStr = rawStr;
        List<Pair<String, String>> keys = findTwoRegexPatterns(rawStr, pattern);
        Set<String> keyset = variablesMap.keySet();
        for (Pair<String, String> pair : keys) {
            String placeHolder = pair.getLeft().trim();
            String key = pair.getRight().trim();
            log.debug("resultStr={},placeHolder={},key={}", resultStr, placeHolder, key);
            if (keyset.contains(key)) {
                String value = variablesMap.get(key);
                if (value != null) {
                    resultStr = resultStr.replace(placeHolder, value);
                }
            } else {
                log.warn("There is no value to replace {} in {},variablesMap:{}", placeHolder, rawStr, variablesMap);
            }
        }
        // 变量值中含有变量，递归替换
        if (rawStr.equals(resultStr) || depth <= 0) {
            return resultStr;
        } else {
            return replaceByRegex(resultStr, pattern, variablesMap, depth - 1);
        }
    }

    /**
     * 判断两个字符串对象是否不同
     *
     * @param str1 字符串1
     * @param str2 字符串2
     * @return 是否不同
     */
    public static boolean isDifferent(String str1, String str2) {
        if (str1 == null && str2 == null) return false;
        if (str1 != null) {
            return !str1.equals(str2);
        }
        return true;
    }

}
