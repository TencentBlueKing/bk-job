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

package com.tencent.bk.job.execute.model;

import com.tencent.bk.job.common.constant.Order;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@ToString
@Builder
public class StepExecutionResultQuery {
    public static final String ORDER_FIELD_TOTAL_TIME = "totalTime";
    public static final String ORDER_FIELD_EXIT_CODE = "exitCode";
    private static final Map<String, String> ORDER_FIELD_IN_DB = new HashMap<>();

    static {
        ORDER_FIELD_IN_DB.put(ORDER_FIELD_TOTAL_TIME, "total_time");
        ORDER_FIELD_IN_DB.put(ORDER_FIELD_EXIT_CODE, "exit_code");
    }
    /**
     * 作业实例ID
     */
    private Long taskInstanceId;
    /**
     * 步骤实例ID
     */
    private Long stepInstanceId;
    /**
     * 执行次数
     */
    private Integer executeCount;
    /**
     * 滚动执行批次
     */
    private Integer batch;
    /**
     * 是否根据步骤实例的最新滚动批次过滤；如果为true，那么batch将使用滚动任务当前执行的批次
     */
    private Boolean filterByLatestBatch;
    /**
     * 执行日志关键词(脚本任务)
     */
    private String logKeyword;
    /**
     * 目标主机IP过滤
     */
    private String searchIp;
    /**
     * 执行结果分组
     */
    private Integer status;
    /**
     * 执行结果输出的自定义分组tag
     */
    private String tag;
    /**
     * 执行结果分组下返回的最大任务数
     */
    private Integer maxTasksForResultGroup;
    /**
     * 是否获取所有分组下的所有主机执行数据（默认为false，只获取第一个分组中的数据）
     */
    private boolean fetchAllGroupData;
    /**
     * 排序字段
     */
    private String orderField;
    /**
     * 排序
     */
    private Order order;

    private Set<ExecuteObjectCompositeKey> matchExecuteObjectCompositeKeys;

    /**
     * 是否包含按照执行对象过滤的条件。如果查询条件包含日志关键字、主机 ip 等，需要先根据条件查询到匹配的执行对象
     */
    public boolean hasExecuteObjectFilterCondition() {
        return StringUtils.isNotEmpty(logKeyword) || StringUtils.isNotEmpty(searchIp);
    }

    public void transformOrderFieldToDbField() {
        if (orderField != null) {
            orderField = ORDER_FIELD_IN_DB.get(orderField);
        }
    }

    public boolean isFilterByLatestBatch() {
        return this.filterByLatestBatch != null && this.filterByLatestBatch;
    }
}
