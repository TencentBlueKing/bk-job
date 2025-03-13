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

package com.tencent.bk.job.analysis.service.ai.impl;

import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import org.springframework.stereotype.Service;

/**
 * AI消息国际化服务
 */
@Service
public class AIMessageI18nService {
    private final MessageI18nService messageI18nService;

    public AIMessageI18nService(MessageI18nService messageI18nService) {
        this.messageI18nService = messageI18nService;
    }

    public String getAIGreetingMessage() {
        return messageI18nService.getI18n("job.analysis.ai.greetingMessage");
    }

    /**
     * 获取无日志输出任务的AI分析结果信息
     *
     * @return 国际化的AI分析结果信息
     */
    public String getEmptyLogTaskAIAnswerMessage() {
        return messageI18nService.getI18n("job.analysis.ai.emptyLogTaskAnswerMessage");
    }

    /**
     * 获取非失败状态任务的AI分析结果信息
     *
     * @return 国际化的AI分析结果信息
     */
    public String getNotFailTaskAIAnswerMessage() {
        return messageI18nService.getI18n("job.analysis.ai.notFailTaskAnswerMessage");
    }

    /**
     * 获取指定环境语言的系统信息
     *
     * @return 国际化的指定环境语言的系统信息
     */
    public String getLanguageSpecifySystemMessage() {
        return messageI18nService.getI18n("job.analysis.ai.languageSpecifySystemMessage");
    }

    /**
     * 获取指定环境语言的AI答复信息
     *
     * @return 国际化的指定环境语言的AI答复信息
     */
    public String getLanguageSpecifyAIReplyMessage() {
        return messageI18nService.getI18n("job.analysis.ai.languageSpecifyAIReplyMessage");
    }

    /**
     * 根据国际化Key获取国际化信息
     *
     * @param i18nKey 国际化Key
     * @return 国际化信息
     */
    public String getI18nMessage(String i18nKey) {
        return messageI18nService.getI18n(i18nKey);
    }
}
