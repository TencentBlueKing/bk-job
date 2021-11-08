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

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Description
 * @Date 2020/3/6
 * @Version 1.0
 */
@Slf4j
public class StringUtil {

    public static String replacePathVariables(String rawStr, Object obj) {
        if (obj == null) return rawStr;
        List<String> placeholderList = findOneRegexPatterns(rawStr, "(\\{.*?\\})");
        for (String placeholder : placeholderList) {
            String fieldName = placeholder.substring(1, placeholder.length() - 1);
            Object fieldValue = ReflectUtil.getFieldValue(obj, fieldName);
            if (null != fieldValue) {
                rawStr = rawStr.replace(placeholder, fieldValue.toString());
            } else {
                log.warn("Fail to parse path variable {} because cannot find field {} in {}", placeholder, fieldName,
                    JsonUtils.toJson(obj));
            }
        }
        return rawStr;
    }

    public static String removePrefixAndSuffix(String rawStr, String fix) {
        rawStr = removePrefix(rawStr, fix);
        rawStr = removeSuffix(rawStr, fix);
        return rawStr;
    }

    public static String removePrefix(String rawStr, String prefix) {
        if (rawStr == null) return null;
        if (prefix == null) return rawStr;
        while (rawStr.startsWith(prefix)) {
            rawStr = rawStr.substring(prefix.length());
        }
        return rawStr;
    }

    public static String removeSuffix(String rawStr, String suffix) {
        if (rawStr == null) return null;
        if (suffix == null) return rawStr;
        while (rawStr.endsWith(suffix)) {
            rawStr = rawStr.substring(0, rawStr.length() - suffix.length());
        }
        return rawStr;
    }

    public static <T> String listToStr(List<T> list) {
        return listToStr(list, ",");
    }

    public static <T> String listToStr(List<T> list, String separator) {
        String str = null;
        if (list != null) {
            str = list.stream().map(Object::toString).collect(Collectors.joining(separator));
        }
        return str;
    }

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

    public static String replaceByRegex(String rawStr, String pattern, Map<String, String> variablesMap) {
        return replaceByRegex(rawStr, pattern, variablesMap, 3);
    }

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

    public static void main(String[] args) {
        List<String> result = findOneRegexPatterns(
            "adfakld${1}fajlkj${2}flaksjdflkjds${3}", "(\\$\\{(.*?)\\})");
        result.forEach(System.out::println);
        Map<String, String> map = new HashMap<>();
        map.put("var1", "1111111");
        System.out.println(replaceByRegex(
            "ccc{{var1}}aaa{{var2}}ddd{{var1}}", "(\\{\\{(.*?)\\}\\})", map));
    }
}
