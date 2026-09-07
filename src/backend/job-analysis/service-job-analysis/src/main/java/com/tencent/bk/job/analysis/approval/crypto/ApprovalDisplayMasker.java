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

package com.tencent.bk.job.analysis.approval.crypto;

import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 敏感值在审批内容里的替代文本。
 */
@Slf4j
@Service
public class ApprovalDisplayMasker {

    /**
     * 掩码占位符。长度固定，连原值长度都不泄露
     */
    private static final String MASK = "******";

    private static final String I18N_PREFIX = "task.approval.content.value.";

    private final MessageI18nService i18nService;

    public ApprovalDisplayMasker(MessageI18nService i18nService) {
        this.i18nService = i18nService;
    }

    public String mask() {
        return MASK;
    }

    /**
     * 只披露"提供了自定义密码"这一事实，连长度都不暴露
     */
    public String passwordProvided() {
        return label("passwordProvided");
    }

    /**
     * 脚本正文在单独的章节里展示，参数区只留一个指向该章节的说明
     */
    public String scriptInSection() {
        return label("scriptInSection");
    }

    private String label(String keySuffix) {
        String key = I18N_PREFIX + keySuffix;
        try {
            return i18nService.getI18n(key);
        } catch (Exception e) {
            log.warn("Missing i18n message for key {}", key);
            return keySuffix;
        }
    }
}
