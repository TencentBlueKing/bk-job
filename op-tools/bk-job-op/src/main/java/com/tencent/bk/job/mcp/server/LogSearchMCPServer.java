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
import com.tencent.bk.job.service.JobLogQueryService;
import com.tencent.bk.job.service.model.PageData;
import com.tencent.bk.job.service.model.SimpleLogDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
@Service
public class LogSearchMCPServer {

    private final JobLogQueryService jobLogQueryService;

    /**
     * 通过stepInstance查询主流程日志的 查询语句模板
     */
    private static final String MAIN_PROCESS_QUERY_TEMPLATE = "log:%s AND log: \"Handle job event, event\"";

    @Autowired
    public LogSearchMCPServer(JobLogQueryService jobLogQueryService) {
        this.jobLogQueryService = jobLogQueryService;
    }

    @Tool(description = "通过时间、符合KQL语法的查询语句 搜索日志（支持分页）")
    public PageData<SimpleLogDTO> searchLogsByCondition(
            @JsonPropertyDescription("KQL语法的查询语句")
            String queryString,
            @JsonPropertyDescription("预定义时间范围，支持格式：1d、1h、15m，最大7天。使用了startTime和endTime时忽略。")
            String timeRange,
            @JsonPropertyDescription("自定义开始时间，格式：yyyy-MM-dd HH:mm:ss，与endTime同时使用，" +
                "优先级比timeRange高，使用timeRange的话不要传这个参数。")
            String startTime,
            @JsonPropertyDescription("自定义结束时间，格式：yyyy-MM-dd HH:mm:ss，与startTime同时使用，" +
                "优先级比timeRange高，使用timeRange的话不要传这个参数。")
            String endTime,
            @JsonPropertyDescription("分页起始位置，从0开始，默认0")
            Integer start,
            @JsonPropertyDescription("每页返回的日志条数，默认10，建议不超过100") 
            Integer size) {
        log.info("[MCP Tool Call] searchLogsByCondition - Input: queryString={}, " +
                "timeRange={}, startTime={}, endTime={}, start={}, size={}",
                queryString, timeRange, startTime, endTime, start, size);
        
        try {
            // 时间校验
            validateTimeParameters(timeRange, startTime, endTime);
            
            // 分页参数校验
            if (start == null || start < 0) {
                start = 0;
            }
            if (size == null || size <= 0) {
                size = 10; // 默认每页10条
            }
            
            PageData<SimpleLogDTO> result = jobLogQueryService.queryLogs(queryString, timeRange, startTime, endTime, start, size);
            
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

    @Tool(description = "通过step_instance_id 搜索该任务的 request_id")
    public String searchRequestIdByStepInstanceId(
            @JsonPropertyDescription("作业步骤实例ID")
            String stepInstanceId) {
        log.info("[MCP Tool Call] searchRequestIdByStepInstanceId - Input: stepInstanceId={}", stepInstanceId);
        
        try {
            // 构建特征查询条件：查找包含stepInstanceId且与MQ消费/生产相关的日志
            String queryString = String.format(MAIN_PROCESS_QUERY_TEMPLATE, stepInstanceId);
            
            // 查询最近1天的日志，使用分页查询，只取第一条
            PageData<SimpleLogDTO> logs = jobLogQueryService.queryLogs(queryString, "1d", null, null, 0, 1);
            
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

    /**
     * 时间参数校验
     */
    private void validateTimeParameters(String timeRange, String startTime, String endTime) {
        // 如果提供了自定义时间范围，进行校验
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
                
                // 校验时间范围不超过7天
                if (java.time.Duration.between(start, end).toDays() > 7) {
                    throw new IllegalArgumentException("查询时间范围不能超过7天");
                }
                
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("时间格式不正确，必须为yyyy-MM-dd HH:mm:ss格式");
            }
        } else if (timeRange != null) {
            // 校验预定义时间范围不超过7天
            long days = parseTimeRange(timeRange);
            if (days > 7) {
                throw new IllegalArgumentException("查询时间范围不能超过7天");
            }
        }
    }

    /**
     * 解析时间范围字符串，转换为天数
     */
    private long parseTimeRange(String timeRange) {
        if (timeRange == null) return 1; // 默认1天
        
        try {
            if (timeRange.endsWith("d")) {
                return Long.parseLong(timeRange.substring(0, timeRange.length() - 1));
            } else if (timeRange.endsWith("h")) {
                return Long.parseLong(timeRange.substring(0, timeRange.length() - 1)) / 24L;
            } else if (timeRange.endsWith("m")) {
                return Long.parseLong(timeRange.substring(0, timeRange.length() - 1)) / (24L * 60L);
            } else {
                return Long.parseLong(timeRange); // 默认为天数
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("时间范围格式不正确，支持格式如：1d、24h、60m");
        }
    }

}
