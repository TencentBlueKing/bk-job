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

package com.tencent.bk.job.mcp.server;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.tencent.bk.job.config.BkLogProperties;
import com.tencent.bk.job.service.JobLogQueryService;
import com.tencent.bk.job.service.model.LogSourceInfo;
import com.tencent.bk.job.service.model.PageData;
import com.tencent.bk.job.service.model.SimpleLogDTO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LogSearchMCPServer {

    private final JobLogQueryService jobLogQueryService;
    private final BkLogProperties bkLogProperties;

    /**
     * 通过stepInstance查询主流程日志的 查询语句模板
     * 根据优先级查找，为防止丢失日志，根据多个模版反复确认
     */
    // 最开始出现的，带有stepInstance的日志
    private static final String MAIN_PROCESS_QUERY_TEMPLATE_0 = "log:%s AND log: \"Begin to dispatch step event\"";
    // 与上面一个模版不一定在一个实例，防止一起丢失
    private static final String MAIN_PROCESS_QUERY_TEMPLATE_1 = "log:%s AND log: \"Handle step event\"";
    private static final String MAIN_PROCESS_QUERY_TEMPLATE_2 = "log:%s AND log: \"Handle gse task event\"";

    private static final List<String> MAIN_PROCESS_QUERY_TEMPLATE_LIST = Arrays.asList(
        MAIN_PROCESS_QUERY_TEMPLATE_0,
        MAIN_PROCESS_QUERY_TEMPLATE_1,
        MAIN_PROCESS_QUERY_TEMPLATE_2
    );

    @Autowired
    public LogSearchMCPServer(JobLogQueryService jobLogQueryService, BkLogProperties bkLogProperties) {
        this.jobLogQueryService = jobLogQueryService;
        this.bkLogProperties = bkLogProperties;
    }

    @PostConstruct
    private void init() {
        log.info("LogSearchMCPServer initialized, available sources: {}", bkLogProperties.getSources().keySet());
    }

    @Tool(description = "通过requestId搜索日志（支持分页与时间范围指定），支持指定日志源查询不同索引集的日志")
    public PageData<SimpleLogDTO> searchLogsByRequestId(
            @Nullable
            @JsonPropertyDescription("日志源key。不传使用默认日志源。可通过listAvailableLogSources获取可选值。")
            String source,
            @JsonPropertyDescription("作业平台请求的requestId，某些场景下也称traceId")
            String requestId,
            @Nullable
            @JsonPropertyDescription("预定义时间范围，支持格式：1d、1h、15m，最大7天。使用了startTime和endTime时忽略。不传默认1d")
            String timeRange,
            @Nullable
            @JsonPropertyDescription("自定义开始时间，格式：yyyy-MM-dd HH:mm:ss，与endTime同时使用，" +
                "优先级比timeRange高，使用timeRange的话不要传这个参数。")
            String startTime,
            @Nullable
            @JsonPropertyDescription("自定义结束时间，格式：yyyy-MM-dd HH:mm:ss，与startTime同时使用，" +
                "优先级比timeRange高，使用timeRange的话不要传这个参数。")
            String endTime,
            @Nullable
            @JsonPropertyDescription("分页起始位置，从0开始，默认0")
            Integer start,
            @Nullable
            @JsonPropertyDescription("每页返回的日志条数，默认10，建议不超过100")
            Integer size) {
        log.info("[MCP Tool Call] searchLogsByRequestId - Input: source={}, requestId={}, " +
                "timeRange={}, startTime={}, endTime={}, start={}, size={}",
            source, requestId, timeRange, startTime, endTime, start, size);
        String requestIdKql = buildRequestIdKql(requestId);
        return searchLogsByCondition(source, requestIdKql, timeRange, startTime, endTime, start, size, false);
    }

    private String buildRequestIdKql(String requestId) {
        return "request_id: " + requestId;
    }

    @Tool(description = "通过时间、符合KQL语法的查询语句搜索日志（支持分页），支持指定日志源查询不同索引集的日志")
    public PageData<SimpleLogDTO> searchLogsByCondition(
            @Nullable
            @JsonPropertyDescription("日志源key。不传使用默认日志源。可通过listAvailableLogSources获取可选值。")
            String source,
            @JsonPropertyDescription("KQL语法的查询语句")
            String queryString,
            @Nullable
            @JsonPropertyDescription("预定义时间范围，支持格式：1d、1h、15m，最大7天。使用了startTime和endTime时忽略。不传默认1d")
            String timeRange,
            @Nullable
            @JsonPropertyDescription("自定义开始时间，格式：yyyy-MM-dd HH:mm:ss，与endTime同时使用，" +
                "优先级比timeRange高，使用timeRange的话不要传这个参数。")
            String startTime,
            @Nullable
            @JsonPropertyDescription("自定义结束时间，格式：yyyy-MM-dd HH:mm:ss，与startTime同时使用，" +
                "优先级比timeRange高，使用timeRange的话不要传这个参数。")
            String endTime,
            @Nullable
            @JsonPropertyDescription("分页起始位置，从0开始，默认0")
            Integer start,
            @Nullable
            @JsonPropertyDescription("每页返回的日志条数，默认10，建议不超过100")
            Integer size,
            @Nullable
            @JsonPropertyDescription("是否按时间升序排序，不传默认false（降序）。对于寻找异常的日志，建议使用降序；" +
                "对于寻找任务启动阶段的日志，建议使用升序")
            Boolean asc) {
        log.info("[MCP Tool Call] searchLogsByCondition - Input: source={}, queryString={}, " +
                "timeRange={}, startTime={}, endTime={}, start={}, size={}",
                source, queryString, timeRange, startTime, endTime, start, size);

        try {
            BkLogProperties.LogSource logSource = bkLogProperties.getSource(source);

            if (timeRange == null && startTime == null && endTime == null) {
                timeRange = "1d";
            }

            validateTimeParameters(logSource, timeRange, startTime, endTime);

            if (start == null || start < 0) {
                start = 0;
            }
            if (size == null || size <= 0) {
                size = 10;
            }

            PageData<SimpleLogDTO> result = jobLogQueryService.queryLogs(
                source, queryString, timeRange, startTime, endTime, start, size, asc
            );

            log.info("[MCP Tool Call] searchLogsByCondition - Output: total={}, pageSize={}, dataCount={}",
                    result != null ? result.getTotal() : 0,
                    result != null ? result.getSize() : 0,
                    result != null && result.getData() != null ? result.getData().size() : 0);

            return result;
        } catch (Exception e) {
            log.error("[MCP Tool Call] searchLogsByCondition - Error: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Tool(description = "通过step_instance_id搜索该任务的request_id，支持指定日志源")
    public String searchRequestIdByStepInstanceId(
            @Nullable
            @JsonPropertyDescription("日志源key。不传使用默认日志源。可通过listAvailableLogSources获取可选值。")
            String source,
            @JsonPropertyDescription("作业步骤实例ID")
            String stepInstanceId) {
        log.info("[MCP Tool Call] searchRequestIdByStepInstanceId - Input: source={}, stepInstanceId={}",
            source, stepInstanceId);

        try {
            PageData<SimpleLogDTO> logs = null;

            // 构建特征查询条件：查找包含stepInstanceId且与MQ消费/生产相关的日志
            for (String template : MAIN_PROCESS_QUERY_TEMPLATE_LIST) {
                String queryString = String.format(template, stepInstanceId);
                // 查询最近7天的日志，使用分页查询，只取第一条
                logs = jobLogQueryService.queryLogs(
                    source, queryString, "7d", null, null, 0, 1, null);
                if (logs != null && CollectionUtils.isNotEmpty(logs.getData())) {
                    break;
                }
            }

            String result;
            if (logs != null && logs.getData() != null && !logs.getData().isEmpty()) {
                // 返回第一个匹配日志的requestId
                SimpleLogDTO simpleLog = logs.getData().get(0);
                log.debug("SimpleLogDTO: {}", simpleLog);
                result = simpleLog.getRequestId() != null ? simpleLog.getRequestId() : "未找到requestId";
            } else {
                result = "未找到包含stepInstanceId: " + stepInstanceId + " 的日志";
            }

            log.info("[MCP Tool Call] searchRequestIdByStepInstanceId - Output: {}", result);
            return result;
        } catch (Exception e) {
            log.error("[MCP Tool Call] searchRequestIdByStepInstanceId - Error: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Tool(description = "列出当前可用的日志源（key、名称、是否默认、日志保留天数），" +
        "在不确定要查询哪个日志源时，应先调用此工具获取列表")
    public Map<String, LogSourceInfo> listAvailableLogSources() {
        log.info("[MCP Tool Call] listAvailableLogSources");
        String defaultSource = bkLogProperties.getDefaultSource();
        Map<String, LogSourceInfo> result = new LinkedHashMap<>();
        for (Map.Entry<String, BkLogProperties.LogSource> entry : bkLogProperties.getSources().entrySet()) {
            String key = entry.getKey();
            BkLogProperties.LogSource source = entry.getValue();
            result.put(key, new LogSourceInfo(
                source.getLabel(),
                key.equals(defaultSource),
                source.getExpireTime()
            ));
        }
        log.info("[MCP Tool Call] listAvailableLogSources - Output: {}", result);
        return result;
    }

    /**
     * 时间参数校验
     */
    private void validateTimeParameters(BkLogProperties.LogSource logSource,
                                        String timeRange,
                                        String startTime,
                                        String endTime) {
        // 如果提供了自定义时间范围，进行校验
        int expireTime = logSource.getExpireTime();
        if (startTime != null || endTime != null) {
            if (startTime == null || endTime == null) {
                throw new IllegalArgumentException("开始时间和结束时间必须同时提供");
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            try {
                LocalDateTime start = LocalDateTime.parse(startTime, formatter);
                LocalDateTime end = LocalDateTime.parse(endTime, formatter);

                if (start.isAfter(end)) {
                    throw new IllegalArgumentException("开始时间不能晚于结束时间");
                }

                // 校验开始时间不能早于配置的天数前
                LocalDateTime expireDaysAgo = LocalDateTime.now().minusDays(expireTime);
                if (start.isBefore(expireDaysAgo)) {
                    throw new IllegalArgumentException("开始时间不能早于" + expireTime + "天前");
                }

            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("时间格式不正确，必须为yyyy-MM-dd HH:mm:ss格式");
            }
        } else if (timeRange != null) {
            // 校验预定义时间天数范围超限
            long days = parseTimeRange(timeRange);
            if (days > expireTime) {
                throw new IllegalArgumentException("查询时间范围不能超过" + expireTime + "天");
            }
        }
    }

    /**
     * 解析时间范围字符串，转换为天数（向上取整）
     */
    private long parseTimeRange(String timeRange) {
        if (timeRange == null) return 1; // 默认1天

        long time = Long.parseLong(timeRange.substring(0, timeRange.length() - 1));
        try {
            if (timeRange.endsWith("d")) {
                return time;
            } else if (timeRange.endsWith("h")) {
                // 向上取整：(time + 23) / 24，确保25h算作2天
                return (time + 23) / 24L;
            } else if (timeRange.endsWith("m")) {
                // 向上取整：(time + 1439) / 1440，确保1441m算作2天
                return (time + 1439) / (24L * 60L);
            } else {
                return Long.parseLong(timeRange);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("时间范围格式不正确，支持格式如：1d、24h、60m");
        }
    }

}
