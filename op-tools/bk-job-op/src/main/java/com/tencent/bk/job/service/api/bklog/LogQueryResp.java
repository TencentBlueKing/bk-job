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

package com.tencent.bk.job.service.api.bklog;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class LogQueryResp {

    /**
     * 查询耗时
     */
    private Integer took;

    /**
     * 是否超时
     */
    @JsonProperty("timed_out")
    private Boolean timedOut;

    /**
     * 分片信息
     */
    @JsonProperty("_shards")
    private Shards shards;

    /**
     * 命中结果
     */
    private Hits hits;
    
    @Data
    public static class Shards {
        /**
         * 总分片数
         */
        private Integer total;

        /**
         * 成功分片数
         */
        private Integer successful;

        /**
         * 跳过分片数
         */
        private Integer skipped;

        /**
         * 失败分片数
         */
        private Integer failed;
    }
    
    @Data
    public static class Hits {
        /**
         * 总命中数
         */
        private Integer total;

        /**
         * 最大分数
         */
        @JsonProperty("max_score")
        private Double maxScore;

        /**
         * 命中结果列表
         */
        private List<Hit> hits;
    }

    /**
     * 命中的条目
     */
    @Data
    public static class Hit {
        /**
         * 索引名称
         */
        @JsonProperty("_index")
        private String index;

        /**
         * 文档类型
         */
        @JsonProperty("_type")
        private String type;

        /**
         * 文档ID
         */
        @JsonProperty("_id")
        private String id;

        /**
         * 文档分数
         */
        @JsonProperty("_score")
        private Double score;

        /**
         * 源数据
         */
        @JsonProperty("_source")
        private Source source;
    }

    /**
     * 真实日志条目，包含日志内容和其他字段
     */
    @Data
    public static class Source {
        /**
         * 日志条目来自的服务器IP
         */
        private String serverIp;

        /**
         * 日志级别
         */
        private String level;

        /**
         * 扩展字段
         */
        @JsonProperty("__ext")
        private Map<String, String> ext;

        /**
         * 事件时间戳（单位毫秒）
         */
        private String dtEventTimeStamp;

        /**
         * GSE索引
         */
        private Integer gseIndex;

        /**
         * 请求requestId
         */
        private String requestId;

        /**
         * 服务名称，如job-crontab等
         */
        private String service;

        /**
         * 迭代索引
         */
        private Integer iterationIndex;

        /**
         * 原始日志内容
         */
        private String content;

        /**
         * 日志内容，完整，包含时间、level、requestId、logger、content等
         */
        private String log;

        /**
         * 日志路径
         */
        private String path;

        /**
         * 时间（单位毫秒）
         */
        private String time;
    }
}
