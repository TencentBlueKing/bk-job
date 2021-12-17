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

package com.tencent.bk.job.common.web.i18n;

import com.tencent.bk.job.common.i18n.locale.LocaleUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.LocaleResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

/**
 * Job 自定义国际化LocaleResolver
 */
@Slf4j
public class JobLangHeaderLocaleResolver implements LocaleResolver {
    @Nullable
    private Locale defaultLocale;

    public JobLangHeaderLocaleResolver() {
    }

    @Nullable
    public Locale getDefaultLocale() {
        return this.defaultLocale;
    }

    public void setDefaultLocale(@Nullable Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public Locale resolveLocale(HttpServletRequest request) {
        Locale locale = null;
        if (StringUtils.isNotBlank(request.getHeader(LocaleUtils.COMMON_LANG_HEADER))) {
            String lang = request.getHeader(LocaleUtils.COMMON_LANG_HEADER);
            locale = LocaleUtils.getLocale(lang);
        }
        if (locale == null) {
            locale = getDefaultLocale();
        }
        if (locale != null) {
            log.info("Set locale for LocaleContextHolder, locale: {}", locale);
            LocaleContextHolder.setLocale(locale);
        }
        return locale;
    }

    public void setLocale(HttpServletRequest request, @Nullable HttpServletResponse response,
                          @Nullable Locale locale) {
        throw new UnsupportedOperationException(
            "Cannot change HTTP accept header - use a different locale resolution strategy");
    }
}
