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

import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;

@Slf4j
public class I18nUtil {
    private static MessageI18nService i18nService;

    public static String getI18nMessage(Locale locale, String key, Object[] params) {
        initI18nService();
        if (i18nService == null) {
            log.warn("Can not find available i18nService");
            return "";
        }
        if (params != null && params.length > 0) {
            // 如果参数中包含需要国际化的字符(格式:{i18n_key})，那么需要国际化参数
            buildI18nParams(params);
            return i18nService.getI18nWithArgs(locale, key, params);
        } else {
            return i18nService.getI18n(locale, key);
        }
    }

    public static String getI18nMessage(String key, Object[] params) {
        initI18nService();
        if (i18nService == null) {
            log.warn("Can not find available i18nService");
            return "";
        }
        if (params != null && params.length > 0) {
            // 如果参数中包含需要国际化的字符(格式:{i18n_key})，那么需要国际化参数
            buildI18nParams(params);
            return i18nService.getI18nWithArgs(key, params);
        } else {
            return i18nService.getI18n(key);
        }
    }

    private static void buildI18nParams(Object[] params) {
        for (int i = 0; i < params.length; i++) {
            Object errorParam = params[i];
            if (errorParam instanceof String) {
                String paramStr = (String) errorParam;
                if (paramStr.startsWith("{") && paramStr.endsWith("}")) {
                    // 国际化处理
                    String i18nKey = paramStr.substring(1, paramStr.length() - 1);
                    params[i] = getI18nMessage(i18nKey);
                }
            }
        }
    }

    public static String getI18nMessage(String key) {
        return getI18nMessage(key, null);
    }

    public static String getI18nMessage(Locale locale, String key) {
        return getI18nMessage(key, null);
    }

    private static void initI18nService() {
        if (i18nService == null) {
            synchronized (I18nUtil.class) {
                if (i18nService == null) {
                    i18nService = ApplicationContextRegister.getBean(MessageI18nService.class);
                }
            }
        }
    }
}
