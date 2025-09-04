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

package com.tencent.bk.job.common.i18n.locale;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 国际化工具类
 */
public class LocaleUtils {
    public static final String LANG_ZH_CN = "zh_CN";
    public static final String LANG_ZH = "zh";
    public static final String LANG_EN = "en";
    public static final String LANG_EN_US = "en_US";

    /**
     * 蓝鲸通用的LANG HEADER
     */
    public static final String BLUEKING_LANG_HEADER = "blueking_language";
    /**
     * 定义项目通用的LANG HEADER
     */
    public static final String COMMON_LANG_HEADER = "lang";

    private static final Map<String, String> localeMap = new HashMap<>();

    static {
        localeMap.put(BkConsts.HEADER_VALUE_LANG_ZH, LANG_ZH);
        localeMap.put(BkConsts.HEADER_VALUE_LANG_ZH_CN, LANG_ZH_CN);
        localeMap.put("zh_cn", LANG_ZH_CN);
        localeMap.put(BkConsts.HEADER_VALUE_LANG_EN, LANG_EN);
        localeMap.put(BkConsts.HEADER_VALUE_LANG_EN_US, LANG_EN_US);
        localeMap.put("en_us", LANG_EN_US);
    }

    private static final Map<String, String> bkLangMap = new HashMap<>();

    static {
        bkLangMap.put(LANG_ZH, BkConsts.HEADER_VALUE_LANG_ZH);
        bkLangMap.put(LANG_ZH_CN, BkConsts.HEADER_VALUE_LANG_ZH_CN);
        bkLangMap.put(LANG_EN, BkConsts.HEADER_VALUE_LANG_EN);
        bkLangMap.put(LANG_EN_US, BkConsts.HEADER_VALUE_LANG_EN_US);
    }

    /**
     * 对不同形式的language Header值进行统一
     *
     * @param lang 语言Header取值
     * @return 标准化后的语言Header取值
     */
    public static String getNormalLang(String lang) {
        lang = lang.toLowerCase();
        if (localeMap.containsKey(lang)) {
            return localeMap.get(lang);
        }
        return lang;
    }

    /**
     * 根据标准语言取值获取蓝鲸协议语言参数取值
     *
     * @param normalLang 标准语言取值
     * @return 蓝鲸协议语言参数取值
     */
    public static String getBkLang(String normalLang) {
        if (bkLangMap.containsKey(normalLang)) {
            return bkLangMap.get(normalLang);
        }
        return null;
    }

    public static Locale getLocale(String lang) {
        if (StringUtils.isBlank(lang)) {
            return Locale.SIMPLIFIED_CHINESE;
        } else if (lang.equalsIgnoreCase(LANG_ZH_CN)) {
            return Locale.SIMPLIFIED_CHINESE;
        } else if (lang.equalsIgnoreCase(LANG_ZH)) {
            return Locale.CHINESE;
        } else if (lang.equalsIgnoreCase(LANG_EN)) {
            return Locale.ENGLISH;
        } else if (lang.equalsIgnoreCase(LANG_EN_US)) {
            return Locale.US;
        } else {
            return Locale.SIMPLIFIED_CHINESE;
        }
    }
}
