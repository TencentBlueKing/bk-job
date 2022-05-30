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

@Slf4j
public class I18nUtil {
    private static MessageI18nService i18nService;

    public static String getI18nMessage(String key, Object[] params) {
        initI18nService();
        if (i18nService == null) {
            log.warn("Can not find available i18nService");
            return "";
        }
        if (params != null && params.length > 0) {
            return i18nService.getI18nWithArgs(key, params);
        } else {
            return i18nService.getI18n(key);
        }
    }

    public static String getI18nMessage(String key) {
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
