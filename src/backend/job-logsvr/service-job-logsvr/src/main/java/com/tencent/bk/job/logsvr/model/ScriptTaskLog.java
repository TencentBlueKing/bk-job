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

package com.tencent.bk.job.logsvr.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Comparator;

/**
 * 脚本任务日志
 */
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@NoArgsConstructor
@Document
public class ScriptTaskLog {
    public static final ScriptTaskLogOffsetComparator LOG_OFFSET_COMPARATOR = new ScriptTaskLogOffsetComparator();
    /**
     * 作业步骤实例ID
     */
    @JsonProperty("stepId")
    @Field("stepId")
    private Long stepInstanceId;
    /**
     * 执行任务的主机ip
     */
    @JsonProperty("ip")
    @Field("ip")
    private String ip;
    /**
     * 执行次数
     */
    @JsonProperty("executeCount")
    @Field("executeCount")
    private Integer executeCount;
    /**
     * 日志内容
     */
    @JsonProperty("content")
    @Field("content")
    private String content;
    /**
     * 日志偏移 - 字节
     */
    @JsonProperty("offset")
    @Field("offset")
    private Integer offset;

    public ScriptTaskLog(Long stepInstanceId, String ip, Integer executeCount, String content, Integer offset) {
        this.stepInstanceId = stepInstanceId;
        this.ip = ip;
        this.executeCount = executeCount;
        this.content = content;
        this.offset = offset;
    }

    private static class ScriptTaskLogOffsetComparator implements Comparator<ScriptTaskLog> {

        @Override
        public int compare(ScriptTaskLog log1, ScriptTaskLog log2) {
            if (log1 == null || log2 == null) {
                return 0;
            }
            return log1.getOffset().compareTo(log2.getOffset());
        }
    }
}
