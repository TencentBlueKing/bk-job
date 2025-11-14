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
public class LogQueryReq {

    /**
     * 索引列表
     */
    private String indices;

    /**
     * 时间标识符["15m", "30m", "1h", "4h", "12h", "1d", "customized"]（非必填，默认15m）
     */
    @JsonProperty("time_range")
    private String timeRange;

    /**
     * 索引集ID
     */
    @JsonProperty("index_set_id")
    private Integer indexSetId;

    /**
     * 搜索语句query_string(非必填，默认为*)
     */
    @JsonProperty("query_string")
    private String queryString;

    /**
     * ES接入场景(非必填） 默认为log，原生ES：es 日志采集：log
     */
    @JsonProperty("scenario_id")
    private String scenarioId;

    /**
     * 当scenario_id为es或log时候需要传入
     */
    @JsonProperty("storage_cluster_id")
    private Integer storageClusterId;

    /**
     * 是否使用时间范围
     */
    @JsonProperty("use_time_range")
    private Boolean useTimeRange;

    /**
     * 时间字段（非必填，bkdata内部为dtEventTimeStamp，外部如果传入时间范围需要指定时间字段）
     */
    @JsonProperty("time_field")
    private String timeField;

    /**
     * 开始时间
     */
    @JsonProperty("start_time")
    private String startTime;

    /**
     * 结束时间
     */
    @JsonProperty("end_time")
    private String endTime;

    /**
     * 搜索过滤条件（非必填，默认为没有过滤，默认的操作符是is） 操作符支持 is、is one of、is not、is not one of
     */
    private List<String> filter;

    /**
     * 排序列表
     */
    @JsonProperty("sort_list")
    private List<List<String>> sortList;

    /**
     * 起始位置（非必填，类似数组切片，默认为0）
     */
    private Integer start;

    /**
     * 条数（非必填，控制返回条目）
     */
    private Integer size;

    /**
     * ES的聚合参数
     */
    private Map<String, Object> aggs;
}
