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

package com.tencent.bk.job.common.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 公共内置标签国际化工具
 */
public class PublicTagI18nUtil {

    private static final Map<String, String> PUBLIC_TAG_NAME_I18N_KEY_MAP;

    static {
        Map<String, String> map = new HashMap<>();
        map.put("持续集成", "common.public.tag.continuousIntegration");
        map.put("测试专用", "common.public.tag.testing");
        map.put("常用工具", "common.public.tag.commonTools");
        map.put("发布部署", "common.public.tag.releaseDeployment");
        map.put("故障处理", "common.public.tag.troubleshooting");
        map.put("运营发布", "common.public.tag.opsRelease");
        PUBLIC_TAG_NAME_I18N_KEY_MAP = Collections.unmodifiableMap(map);
    }

    public static boolean matchesName(String tagName, String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return true;
        }
        if (StringUtils.isBlank(tagName)) {
            return false;
        }
        String lowerCaseKeyword = keyword.toLowerCase(Locale.ROOT);
        return tagName.contains(keyword)
            || getI18nName(tagName).toLowerCase(Locale.ROOT).contains(lowerCaseKeyword);
    }

    public static String getI18nName(String tagName) {
        if (StringUtils.isBlank(tagName)) {
            return tagName;
        }
        String i18nKey = PUBLIC_TAG_NAME_I18N_KEY_MAP.get(tagName);
        if (i18nKey == null) {
            return tagName;
        }
        return StringUtils.defaultIfBlank(I18nUtil.getI18nMessage(i18nKey), tagName);
    }

    private PublicTagI18nUtil() {
    }
}
