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

package com.tencent.bk.job.analysis.config;

import com.tencent.bk.job.common.util.file.FileSizeUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI相关配置
 */
@Getter
@Setter
@ToString
@ConfigurationProperties(prefix = "ai")
public class AIProperties {

    private Boolean enabled = false;

    /**
     * 错误日志分析相关配置
     */
    private AnalyzeErrorLogConfig analyzeErrorLog = new AnalyzeErrorLogConfig();

    /**
     * 聊天记录相关配置
     */
    private ChatHistoryConfig chatHistory = new ChatHistoryConfig();

    @Getter
    @Setter
    @ToString
    public static class AnalyzeErrorLogConfig {
        /**
         * 支持分析的错误日志最大长度
         */
        private String logMaxLength = "5MB";

        public Long getLogMaxLengthBytes() {
            return FileSizeUtil.parseFileSizeBytes(logMaxLength);
        }
    }

    @Getter
    @Setter
    @ToString
    public static class ChatHistoryConfig {
        /**
         * 最大保留天数
         */
        private Integer maxKeepDays = 31;
        /**
         * 单个用户最大保留的聊天记录数量
         */
        private Integer maxHistoryPerUser = 1000;
    }
}
