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

package com.tencent.bk.job.analysis.task.statistics.task.impl.app.per;

import com.tencent.bk.job.analysis.client.ExecuteMetricsClient;
import com.tencent.bk.job.analysis.consts.TotalMetricEnum;
import com.tencent.bk.job.analysis.dao.StatisticsDAO;
import com.tencent.bk.job.analysis.service.BasicServiceManager;
import com.tencent.bk.job.analysis.task.statistics.anotation.StatisticsTask;
import com.tencent.bk.job.analysis.task.statistics.task.ExecuteBasePerAppStatisticsTask;
import com.tencent.bk.job.common.statistics.consts.StatisticsConstants;
import com.tencent.bk.job.common.statistics.model.dto.StatisticsDTO;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.manage.model.inner.ServiceApplicationDTO;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@StatisticsTask
@Service
public class ExecuteTaskDailyPerAppStatisticsTask extends ExecuteBasePerAppStatisticsTask {

    @Autowired
    public ExecuteTaskDailyPerAppStatisticsTask(ExecuteMetricsClient executeMetricsClient,
                                                BasicServiceManager basicServiceManager, StatisticsDAO statisticsDAO,
                                                DSLContext dslContext) {
        super(executeMetricsClient, basicServiceManager, statisticsDAO, dslContext);
    }

    private StatisticsDTO getExecutedTaskBaseStatisticsDTO(ServiceApplicationDTO app, String timeTag) {
        StatisticsDTO statisticsDTO = getTimeUnitBaseStatisticsDTO(app, timeTag);
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_EXECUTED_TASK);//执行过的任务
        return statisticsDTO;
    }

    private StatisticsDTO getFailedTaskBaseStatisticsDTO(ServiceApplicationDTO app, String timeTag) {
        StatisticsDTO statisticsDTO = getTimeUnitBaseStatisticsDTO(app, timeTag);
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_FAILED_TASK);//失败的任务
        return statisticsDTO;
    }

    private StatisticsDTO getTimeUnitBaseStatisticsDTO(ServiceApplicationDTO app, String timeTag) {
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(app.getId());
        statisticsDTO.setCreateTime(System.currentTimeMillis());
        statisticsDTO.setDate(timeTag);
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_TIME_UNIT);//统计的单位时间
        return statisticsDTO;
    }

    public List<StatisticsDTO> getStatisticsFrom(ServiceApplicationDTO app, Long fromTime, Long toTime,
                                                 String timeTag) {
        List<StatisticsDTO> statisticsDTOList = new ArrayList<>();
        // 1.一天的作业量
        StatisticsDTO statisticsDTO = getExecutedTaskBaseStatisticsDTO(app, timeTag);
        statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_TIME_UNIT_DAY);
        addExecuteStatisticsDTO(statisticsDTO, statisticsDTOList);
        // 2.一天的失败作业量
        statisticsDTO = getFailedTaskBaseStatisticsDTO(app, timeTag);
        statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_TIME_UNIT_DAY);
        addExecuteStatisticsDTO(statisticsDTO, statisticsDTOList);
        return statisticsDTOList;
    }

    /**
     * 汇总所有业务的执行总量
     *
     * @param dayTimeStr
     */
    public void updateTotalExecutedTaskStatistics(String dayTimeStr) {
        List<StatisticsDTO> statisticsDTOList = statisticsDAO.getStatisticsList(
            null,
            null,
            StatisticsConstants.RESOURCE_EXECUTED_TASK,
            StatisticsConstants.DIMENSION_GLOBAL_STATISTIC_TYPE,
            StatisticsConstants.DIMENSION_VALUE_GLOBAL_STATISTIC_TYPE_PREFIX
                + TotalMetricEnum.EXECUTED_TASK_COUNT,
            dayTimeStr);
        Long totalValue = 0L;
        for (StatisticsDTO dto : statisticsDTOList) {
            totalValue += Long.parseLong(dto.getValue());
        }
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(StatisticsConstants.DEFAULT_APP_ID);
        statisticsDTO.setCreateTime(System.currentTimeMillis());
        statisticsDTO.setDate(dayTimeStr);
        statisticsDTO.setValue(totalValue.toString());
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_GLOBAL);
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_GLOBAL_STATISTIC_TYPE);
        statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_GLOBAL_STATISTIC_TYPE_PREFIX
            + TotalMetricEnum.EXECUTED_TASK_COUNT);
        statisticsDAO.upsertStatistics(dslContext, statisticsDTO);
    }

    /**
     * 汇总所有业务的失败执行总量
     *
     * @param dayTimeStr
     */
    public void updateTotalFailedTaskStatistics(String dayTimeStr) {
        List<StatisticsDTO> statisticsDTOList = statisticsDAO.getStatisticsList(
            null,
            null,
            StatisticsConstants.RESOURCE_FAILED_TASK,
            StatisticsConstants.DIMENSION_GLOBAL_STATISTIC_TYPE,
            StatisticsConstants.DIMENSION_VALUE_GLOBAL_STATISTIC_TYPE_PREFIX
                + TotalMetricEnum.FAILED_TASK_COUNT,
            dayTimeStr);
        log.debug("statisticsDTOList.size={}", statisticsDTOList.size());
        Long totalValue = 0L;
        for (StatisticsDTO dto : statisticsDTOList) {
            log.debug("add {} data", dto.getDate());
            totalValue += Long.parseLong(dto.getValue());
        }
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(StatisticsConstants.DEFAULT_APP_ID);
        statisticsDTO.setCreateTime(System.currentTimeMillis());
        statisticsDTO.setDate(dayTimeStr);
        statisticsDTO.setValue(totalValue.toString());
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_GLOBAL);
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_GLOBAL_STATISTIC_TYPE);
        statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_GLOBAL_STATISTIC_TYPE_PREFIX
            + TotalMetricEnum.FAILED_TASK_COUNT);
        statisticsDAO.upsertStatistics(dslContext, statisticsDTO);
    }

    /**
     * 汇总单个业务的执行总量（增量统计）
     *
     * @param appId
     * @param dayTimeStr
     */
    public void updateAppTotalExecutedTask(Long appId, String dayTimeStr) {
        // 今日累计执行总量=昨日累计执行总量+今日执行量
        String lastDayTimeStr = DateUtils.getLastDateStr(dayTimeStr);
        // 昨日累计执行总量
        StatisticsDTO lastDayStatisticsDTO = statisticsDAO.getStatistics(
            appId,
            StatisticsConstants.RESOURCE_EXECUTED_TASK,
            StatisticsConstants.DIMENSION_GLOBAL_STATISTIC_TYPE,
            StatisticsConstants.DIMENSION_VALUE_GLOBAL_STATISTIC_TYPE_PREFIX
                + TotalMetricEnum.EXECUTED_TASK_COUNT,
            lastDayTimeStr);
        // 今日执行量
        StatisticsDTO todayStatisticsDTO = statisticsDAO.getStatistics(appId,
            StatisticsConstants.RESOURCE_EXECUTED_TASK, StatisticsConstants.DIMENSION_TIME_UNIT,
            StatisticsConstants.DIMENSION_VALUE_TIME_UNIT_DAY, dayTimeStr);
        Long totalValue = 0L;
        if (todayStatisticsDTO != null) {
            totalValue += Long.parseLong(todayStatisticsDTO.getValue());
        } else {
            log.warn("Cannot find today data:{},{},{},{},ignore", dayTimeStr,
                StatisticsConstants.RESOURCE_EXECUTED_TASK, StatisticsConstants.DIMENSION_TIME_UNIT,
                StatisticsConstants.DIMENSION_VALUE_TIME_UNIT_DAY);
        }
        if (lastDayStatisticsDTO != null) {
            totalValue += Long.parseLong(lastDayStatisticsDTO.getValue());
        } else {
            log.warn("Cannot find lastDay data:{},{},{},{},ignore", lastDayTimeStr,
                StatisticsConstants.RESOURCE_EXECUTED_TASK, StatisticsConstants.DIMENSION_GLOBAL_STATISTIC_TYPE,
                StatisticsConstants.DIMENSION_VALUE_GLOBAL_STATISTIC_TYPE_PREFIX + TotalMetricEnum.EXECUTED_TASK_COUNT);
        }
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(appId);
        statisticsDTO.setCreateTime(System.currentTimeMillis());
        statisticsDTO.setDate(dayTimeStr);
        statisticsDTO.setValue(totalValue.toString());
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_EXECUTED_TASK);
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_GLOBAL_STATISTIC_TYPE);
        statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_GLOBAL_STATISTIC_TYPE_PREFIX
            + TotalMetricEnum.EXECUTED_TASK_COUNT);
        statisticsDAO.upsertStatistics(dslContext, statisticsDTO);
    }

    /**
     * 汇总单个业务的失败执行总量
     *
     * @param appId
     * @param dayTimeStr
     */
    public void updateAppTotalFailedTaskStatistics(Long appId, String dayTimeStr) {
        // 今日累计失败执行总量=昨日累计失败执行总量+今日失败执行量
        String lastDayTimeStr = DateUtils.getLastDateStr(dayTimeStr);
        // 昨日累计失败执行总量
        StatisticsDTO lastDayStatisticsDTO = statisticsDAO.getStatistics(
            appId,
            StatisticsConstants.RESOURCE_FAILED_TASK,
            StatisticsConstants.DIMENSION_GLOBAL_STATISTIC_TYPE,
            StatisticsConstants.DIMENSION_VALUE_GLOBAL_STATISTIC_TYPE_PREFIX
                + TotalMetricEnum.FAILED_TASK_COUNT,
            lastDayTimeStr);
        // 今日失败执行量
        StatisticsDTO todayStatisticsDTO = statisticsDAO.getStatistics(appId,
            StatisticsConstants.RESOURCE_FAILED_TASK, StatisticsConstants.DIMENSION_TIME_UNIT,
            StatisticsConstants.DIMENSION_VALUE_TIME_UNIT_DAY, dayTimeStr);
        Long totalValue = 0L;
        if (todayStatisticsDTO != null) {
            totalValue += Long.parseLong(todayStatisticsDTO.getValue());
        } else {
            log.warn("Cannot find today data:{},{},{},{},ignore", dayTimeStr,
                StatisticsConstants.RESOURCE_FAILED_TASK, StatisticsConstants.DIMENSION_TIME_UNIT,
                StatisticsConstants.DIMENSION_VALUE_TIME_UNIT_DAY);
        }
        if (lastDayStatisticsDTO != null) {
            totalValue += Long.parseLong(lastDayStatisticsDTO.getValue());
        } else {
            log.warn("Cannot find lastDay data:{},{},{},{},ignore", lastDayTimeStr,
                StatisticsConstants.RESOURCE_FAILED_TASK, StatisticsConstants.DIMENSION_GLOBAL_STATISTIC_TYPE,
                StatisticsConstants.DIMENSION_VALUE_GLOBAL_STATISTIC_TYPE_PREFIX + TotalMetricEnum.EXECUTED_TASK_COUNT);
        }
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(appId);
        statisticsDTO.setCreateTime(System.currentTimeMillis());
        statisticsDTO.setDate(dayTimeStr);
        statisticsDTO.setValue(totalValue.toString());
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_FAILED_TASK);
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_GLOBAL_STATISTIC_TYPE);
        statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_GLOBAL_STATISTIC_TYPE_PREFIX
            + TotalMetricEnum.FAILED_TASK_COUNT);
        statisticsDAO.upsertStatistics(dslContext, statisticsDTO);
    }

    @Override
    public void afterAppDailyStatisticsUpdated(Long appId, LocalDateTime dateTime) {
        // 更新单业务汇总数据
        String dayTimeStr = getDayTimeStr(dateTime);
        String todayDateStr = getDayTimeStr(LocalDateTime.now());
        String timeStr = dayTimeStr;
        int maxCount = 3650;
        int count = 0;
        while (timeStr.compareTo(todayDateStr) <= 0 && count < maxCount) {
            log.debug("update app total TaskStatistics of {}, count={}", timeStr, count);
            updateAppTotalExecutedTask(appId, timeStr);
            updateAppTotalFailedTaskStatistics(appId, timeStr);
            timeStr = DateUtils.getNextDateStr(timeStr);
            count += 1;
        }
    }

    @Override
    public void genStatisticsByDay(LocalDateTime dateTime) {
        super.genStatisticsByDay(dateTime);
        String dayTimeStr = getDayTimeStr(dateTime);
        String todayDateStr = getDayTimeStr(LocalDateTime.now());
        // 更新全业务全局汇总数据
        // 比统计日期晚的统计数据都需要更新
        String timeStr = dayTimeStr;
        int maxCount = 3650;
        int count = 0;
        while (timeStr.compareTo(todayDateStr) <= 0 && count < maxCount) {
            log.debug("update total TaskStatistics of {}, count={}", timeStr, count);
            updateTotalExecutedTaskStatistics(timeStr);
            updateTotalFailedTaskStatistics(timeStr);
            timeStr = DateUtils.getNextDateStr(timeStr);
            count += 1;
        }
    }
}
