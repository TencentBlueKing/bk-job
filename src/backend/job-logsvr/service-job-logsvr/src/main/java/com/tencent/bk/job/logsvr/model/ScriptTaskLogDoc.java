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
import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.constant.CompatibleType;
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
public class ScriptTaskLogDoc {
    public static final ScriptTaskLogOffsetComparator LOG_OFFSET_COMPARATOR = new ScriptTaskLogOffsetComparator();
    /**
     * 作业步骤实例ID
     */
    @Field(ScriptTaskLogDocField.STEP_ID)
    private Long stepInstanceId;
    /**
     * 执行次数
     */
    @Field(ScriptTaskLogDocField.EXECUTE_COUNT)
    private Integer executeCount;
    /**
     * 滚动执行批次
     */
    @Field(ScriptTaskLogDocField.BATCH)
    private Integer batch;
    /**
     * 执行对象 ID
     */
    @Field(ScriptTaskLogDocField.EXECUTE_OBJECT_ID)
    private String executeObjectId;
    /**
     * 执行任务的主机ipv4
     */
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA,
        explain = "兼容历史数据使用, 新版本将不再使用该字段")
    @Field(ScriptTaskLogDocField.IP)
    private String ip;
    /**
     * 执行任务的主机ipv6
     */
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA,
        explain = "兼容历史数据使用, 新版本将不再使用该字段")
    @Field(ScriptTaskLogDocField.IPV6)
    private String ipv6;
    /**
     * 执行任务的主机hostId
     */
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA,
        explain = "兼容历史数据使用, 新版本将不再使用该字段")
    @Field(ScriptTaskLogDocField.HOST_ID)
    private Long hostId;
    /**
     * 日志内容
     */
    @Field(ScriptTaskLogDocField.CONTENT)
    private String content;
    /**
     * 日志偏移 - 单位(byte)
     */
    @Field(ScriptTaskLogDocField.OFFSET)
    private Integer offset;

    public ScriptTaskLogDoc(Long stepInstanceId,
                            Integer executeCount,
                            Integer batch,
                            String executeObjectId,
                            String content,
                            Integer offset) {
        this.stepInstanceId = stepInstanceId;
        this.executeCount = executeCount;
        this.batch = batch;
        this.executeObjectId = executeObjectId;
        this.content = content;
        this.offset = offset;
    }

    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA,
        explain = "兼容历史数据使用")
    public ScriptTaskLogDoc(Long stepInstanceId,
                            Integer executeCount,
                            Integer batch,
                            Long hostId,
                            String ip,
                            String ipv6,
                            String content,
                            Integer offset) {
        this.stepInstanceId = stepInstanceId;
        this.executeCount = executeCount;
        this.batch = batch;
        this.hostId = hostId;
        this.ip = ip;
        this.ipv6 = ipv6;
        this.content = content;
        this.offset = offset;
    }

    private static class ScriptTaskLogOffsetComparator implements Comparator<ScriptTaskLogDoc> {

        @Override
        public int compare(ScriptTaskLogDoc log1, ScriptTaskLogDoc log2) {
            if (log1 == null || log2 == null) {
                return 0;
            }
            return log1.getOffset().compareTo(log2.getOffset());
        }
    }
}
