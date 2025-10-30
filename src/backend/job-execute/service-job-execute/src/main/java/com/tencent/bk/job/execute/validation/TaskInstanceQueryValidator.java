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

package com.tencent.bk.job.execute.validation;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.config.TaskHistoryQueryProperties;
import com.tencent.bk.job.execute.model.TaskInstanceQuery;
import com.tencent.bk.job.execute.service.AverageTaskNumQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static com.tencent.bk.job.execute.constants.Consts.MAX_SEARCH_TASK_HISTORY_RANGE_MILLS;

/**
 * 任务实例查询参数校验器
 */
@Slf4j
@Service
public class TaskInstanceQueryValidator {

    private final AverageTaskNumQueryService averageTaskNumQueryService;
    private final TaskHistoryQueryProperties taskHistoryQueryProperties;

    @Autowired
    public TaskInstanceQueryValidator(AverageTaskNumQueryService averageTaskNumQueryService,
                                      TaskHistoryQueryProperties taskHistoryQueryProperties) {
        this.averageTaskNumQueryService = averageTaskNumQueryService;
        this.taskHistoryQueryProperties = taskHistoryQueryProperties;
    }

    /**
     * 校验并设置查询时间范围相关参数
     *
     * @param taskInstanceQuery 任务实例查询条件
     * @param startTime         开始时间
     * @param endTime           结束时间
     * @param timeRange         耗时范围
     */
    public void validateAndSetQueryTimeRange(TaskInstanceQuery taskInstanceQuery,
                                             String startTime,
                                             String endTime,
                                             Integer timeRange) {
        Long start = null;
        Long end = null;
        if (timeRange != null) {
            if (timeRange < 1) {
                log.warn("Param timeRange should greater than 0");
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
            }
            if (timeRange > 30) {
                log.warn("Param timeRange should less then 30");
                throw new FailedPreconditionException(ErrorCode.TASK_INSTANCE_QUERY_TIME_SPAN_MORE_THAN_30_DAYS);
            }
            // 当天结束时间 - 往前的天数
            start = DateUtils.getUTCCurrentDayEndTimestamp() - timeRange * 24 * 3600 * 1000L;
        } else {
            if (StringUtils.isNotBlank(startTime)) {
                start = DateUtils.convertUnixTimestampFromDateTimeStr(startTime, "yyyy-MM-dd HH:mm:ss",
                    ChronoUnit.MILLIS, ZoneId.systemDefault());
            }
            if (StringUtils.isNotBlank(endTime)) {
                end = DateUtils.convertUnixTimestampFromDateTimeStr(endTime, "yyyy-MM-dd HH:mm:ss",
                    ChronoUnit.MILLIS, ZoneId.systemDefault());
            }

            if (start == null) {
                log.info("StartTime should not be empty!");
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
            }
            if (end == null) {
                end = System.currentTimeMillis();
            }
            if (end - start > MAX_SEARCH_TASK_HISTORY_RANGE_MILLS) {
                log.info("Query task instance history time span must be less than 30 days");
                throw new FailedPreconditionException(ErrorCode.TASK_INSTANCE_QUERY_TIME_SPAN_MORE_THAN_30_DAYS);
            }
        }

        // 验证复杂查询范围限制
        validateComplexQueryTimeRange(taskInstanceQuery, start, end);

        taskInstanceQuery.setStartTime(start);
        taskInstanceQuery.setEndTime(end);
    }

    /**
     * 验证复杂查询时间范围限制，对复杂查询进行范围限制以避免大范围扫描造成慢查询影响整个系统
     * 判断依据：根据过去一段时间内每天的任务执行量估算本次查询需要扫描的任务数据量，限定其最大值
     *
     * @param taskInstanceQuery 任务查询条件
     * @param start             开始时间
     * @param end               结束时间
     * @throws InvalidParamException 如果复杂查询时间范围过大，抛出参数不合法异常
     */
    public void validateComplexQueryTimeRange(TaskInstanceQuery taskInstanceQuery, Long start, Long end) {
        // 检查是否启用复杂查询限制
        if (!taskHistoryQueryProperties.getComplexQueryLimit().isEnabled()) {
            return;
        }

        // 检查当前查询是否为复杂查询
        if (!taskInstanceQuery.isComplexQuery()) {
            return;
        }

        Long appId = taskInstanceQuery.getAppId();
        try {
            // 计算过去几天内平均每天任务量
            int sampleDays = taskHistoryQueryProperties.getComplexQueryLimit().getSampleDays();
            long averageDailyTaskNum = averageTaskNumQueryService.getAverageDailyTaskNum(appId, sampleDays);

            // 如果平均每天任务量为0，则不进行限制
            if (averageDailyTaskNum == 0) {
                log.debug("Average daily task num is 0 for appId={}, skip complex query limit", appId);
                return;
            }

            // 计算最大允许的时间范围（天）
            long maxQueryDataNum = taskHistoryQueryProperties.getComplexQueryLimit().getMaxQueryDataNum();
            long maxAllowedDays = maxQueryDataNum / averageDailyTaskNum;

            // 计算查询时间范围（天）
            double queryTimeRangeDays = (end - start) / (24 * 60 * 60 * 1000.0);

            log.debug("Complex query validation for appId={}: averageDailyTaskNum={}, maxQueryDataNum={}, " +
                    "maxAllowedDays={}, queryTimeRangeDays={}",
                appId, averageDailyTaskNum, maxQueryDataNum, maxAllowedDays, queryTimeRangeDays);

            // 如果查询时间范围超过最大允许范围，则抛出异常
            if (queryTimeRangeDays > maxAllowedDays) {
                log.warn("ComplexQueryTimeRangeExceedsLimit: appId={}: queryTimeRangeDays={}, " +
                        "maxAllowedDays={}, averageDailyTaskNum={}",
                    appId, queryTimeRangeDays, maxAllowedDays, averageDailyTaskNum);
                throw new InvalidParamException(
                    ErrorCode.TASK_HISTORY_QUERY_RANGE_TOO_LARGE,
                    new Object[]{maxAllowedDays}
                );
            }
        } catch (InvalidParamException e) {
            throw e;
        } catch (Exception e) {
            // 如果验证过程中出现异常，为了不影响正常查询，这里不抛出异常
            String message = MessageFormatter.format(
                "Failed to validate complex query time range for appId={}", appId
            ).getMessage();
            log.error(message, e);
        }
    }
}
