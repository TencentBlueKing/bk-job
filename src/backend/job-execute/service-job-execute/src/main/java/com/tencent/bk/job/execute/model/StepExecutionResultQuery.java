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
import org.jooq.generated.tables.GseTaskIpLog;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@ToString
@Builder
public class StepExecutionResultQuery {
    public static final String ORDER_FIELD_TOTAL_TIME = "totalTime";
    public static final String ORDER_FIELD_CLOUD_AREA_ID = "cloudAreaId";
    public static final String ORDER_FIELD_EXIT_CODE = "exitCode";
    private static final Map<String, String> ORDER_FIELD_IN_DB = new HashMap<>();

    static {
        ORDER_FIELD_IN_DB.put(ORDER_FIELD_TOTAL_TIME, GseTaskIpLog.GSE_TASK_IP_LOG.TOTAL_TIME.getName());
        ORDER_FIELD_IN_DB.put(ORDER_FIELD_EXIT_CODE, GseTaskIpLog.GSE_TASK_IP_LOG.EXIT_CODE.getName());
        // 表中不包含cloud_area_id字段，可以使用IP达到相同的目的
        ORDER_FIELD_IN_DB.put(ORDER_FIELD_CLOUD_AREA_ID, GseTaskIpLog.GSE_TASK_IP_LOG.IP.getName());
    }

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
    private Integer maxAgentTasksForResultGroup;
    /**
     * 排序字段
     */
    private String orderField;
    /**
     * 排序
     */
    private Order order;

    private Set<Long> matchHostIds;

    public boolean hasIpCondition() {
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
