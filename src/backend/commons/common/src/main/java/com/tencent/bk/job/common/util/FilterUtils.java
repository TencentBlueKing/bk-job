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


import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public class FilterUtils {
    private static Pattern commonPunctuationPattern = Pattern.compile("[_\\-|!#@$&%^~=+. ]");
    private static Pattern commonLetterPattern = Pattern.compile("[A-Za-z]");
    private static Pattern commonNumberPattern = Pattern.compile("[0-9]");
    private static Pattern commonChinesePattern = Pattern.compile("[\\p{IsHan}]");

    /**
     * 原字符串是否只包含英文字母、数字和常用符号
     *
     * @param origin 原字符串
     * @return 是否符合要求
     */
    public static boolean checkOnlyLetterAndNumberAndPunctuation(String origin) {
        return checkOnlyLetterAndNumberAndPunctuation(origin, Integer.MAX_VALUE);
    }

    /**
     * 原字符串是否只包含英文字母、数字和常用符号，且长度满足要求
     *
     * @param origin 原字符串
     * @param length 长度限制
     * @return 是否符合要求
     */
    public static boolean checkOnlyLetterAndNumberAndPunctuation(String origin, int length) {
        if (!checkLength(origin, length)) {
            return false;
        }
        if (StringUtils.isBlank(origin)) {
            return true;
        }
        origin = removeCommonPunctuations(origin);
        origin = removeCommonLetters(origin);
        origin = removeNumbers(origin);
        return StringUtils.isEmpty(origin);
    }

    /**
     * 原字符串是否只包含汉字、英文字母、数字和常用符号
     *
     * @param origin 原字符串
     * @return 是否符合要求
     */
    public static boolean checkOnlyChineseAndLetterAndNumberAndPunctuation(String origin) {
        return checkOnlyChineseAndLetterAndNumberAndPunctuation(origin, Integer.MAX_VALUE);
    }

    /**
     * 原字符串是否只包含汉字、英文字母、数字和常用符号，且长度满足要求
     *
     * @param origin 原字符串
     * @param length 长度限制
     * @return 是否符合要求
     */
    public static boolean checkOnlyChineseAndLetterAndNumberAndPunctuation(String origin, int length) {
        if (!checkLength(origin, length)) {
            return false;
        }
        if (StringUtils.isBlank(origin)) {
            return true;
        }
        origin = removeCommonPunctuations(origin);
        origin = removeCommonLetters(origin);
        origin = removeNumbers(origin);
        origin = removeChinese(origin);
        return StringUtils.isEmpty(origin);
    }

    public static String removeCommonPunctuations(String originString) {
        return commonPunctuationPattern.matcher(originString).replaceAll("");
    }

    public static String removeCommonLetters(String originString) {
        return commonLetterPattern.matcher(originString).replaceAll("");
    }

    public static String removeChinese(String originString) {
        return commonChinesePattern.matcher(originString).replaceAll("");
    }

    public static String removeNumbers(String originString) {
        return commonNumberPattern.matcher(originString).replaceAll("");
    }

    private static boolean checkLength(String origin, int length) {
        if (length <= 0) {
            return false;
        }
        Matcher matcher = commonChinesePattern.matcher(origin);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count + origin.length() <= length;
    }
}
