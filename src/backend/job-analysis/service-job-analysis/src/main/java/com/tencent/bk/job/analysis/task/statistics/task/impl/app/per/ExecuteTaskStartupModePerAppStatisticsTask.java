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
import com.tencent.bk.job.analysis.dao.StatisticsDAO;
import com.tencent.bk.job.analysis.service.BasicServiceManager;
import com.tencent.bk.job.analysis.task.statistics.anotation.StatisticsTask;
import com.tencent.bk.job.analysis.task.statistics.task.ExecuteBasePerAppStatisticsTask;
import com.tencent.bk.job.common.statistics.consts.StatisticsConstants;
import com.tencent.bk.job.common.statistics.model.dto.StatisticsDTO;
import com.tencent.bk.job.manage.model.inner.ServiceApplicationDTO;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@StatisticsTask
@Service
public class ExecuteTaskStartupModePerAppStatisticsTask extends ExecuteBasePerAppStatisticsTask {

    @Autowired
    public ExecuteTaskStartupModePerAppStatisticsTask(ExecuteMetricsClient executeMetricsClient,
                                                      BasicServiceManager basicServiceManager,
                                                      StatisticsDAO statisticsDAO, DSLContext dslContext) {
        super(executeMetricsClient, basicServiceManager, statisticsDAO, dslContext);
    }

    private StatisticsDTO getStartupModeBaseDTO(ServiceApplicationDTO app, String timeTag) {
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(app.getId());
        statisticsDTO.setCreateTime(System.currentTimeMillis());
        statisticsDTO.setDate(timeTag);
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_EXECUTED_TASK);//执行过的作业
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_TASK_STARTUP_MODE);//作业启动方式
        return statisticsDTO;
    }

    @Override
    public List<StatisticsDTO> getStatisticsFrom(ServiceApplicationDTO app, Long fromTime, Long toTime,
                                                 String timeTag) {
        List<StatisticsDTO> statisticsDTOList = new ArrayList<>();
        //页面执行次数
        StatisticsDTO statisticsDTO = getStartupModeBaseDTO(app, timeTag);
        statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_TASK_STARTUP_MODE_NORMAL);//页面执行
        addExecuteStatisticsDTO(statisticsDTO, statisticsDTOList);
        //API调用次数
        statisticsDTO = getStartupModeBaseDTO(app, timeTag);
        statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_TASK_STARTUP_MODE_API);//API调用
        addExecuteStatisticsDTO(statisticsDTO, statisticsDTOList);
        //定时任务次数
        statisticsDTO = getStartupModeBaseDTO(app, timeTag);
        statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_TASK_STARTUP_MODE_CRON);//定时任务
        addExecuteStatisticsDTO(statisticsDTO, statisticsDTOList);
        return statisticsDTOList;
    }

    /**
     * 聚合多个业务的启动模式数据
     *
     * @param dayTimeStr
     */
    public void aggAllAppStartupMode(String dayTimeStr) {
        aggAllAppStartupMode(dayTimeStr,
            StatisticsConstants.DIMENSION_VALUE_TASK_STARTUP_MODE_NORMAL);
        aggAllAppStartupMode(dayTimeStr, StatisticsConstants.DIMENSION_VALUE_TASK_STARTUP_MODE_API);
        aggAllAppStartupMode(dayTimeStr,
            StatisticsConstants.DIMENSION_VALUE_TASK_STARTUP_MODE_CRON);
    }

    public void aggAllAppStartupMode(String dayTimeStr, String startupModeDimensionValue) {
        Long totalValue = statisticsDAO.getTotalValueOfStatisticsList(null,
            Collections.singletonList(StatisticsConstants.DEFAULT_APP_ID), StatisticsConstants.RESOURCE_EXECUTED_TASK
            , StatisticsConstants.DIMENSION_TASK_STARTUP_MODE, startupModeDimensionValue, dayTimeStr);
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(StatisticsConstants.DEFAULT_APP_ID);
        statisticsDTO.setCreateTime(System.currentTimeMillis());
        statisticsDTO.setDate(dayTimeStr);
        statisticsDTO.setValue(totalValue.toString());
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_ONE_DAY_EXECUTED_TASK_OF_ALL_APP);
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_TASK_STARTUP_MODE);
        statisticsDTO.setDimensionValue(startupModeDimensionValue);
        statisticsDAO.upsertStatistics(dslContext, statisticsDTO);
    }

    @Override
    public void genStatisticsByDay(LocalDateTime dateTime) {
        super.genStatisticsByDay(dateTime);
        // 聚合多个业务的数据
        String dayTimeStr = getDayTimeStr(dateTime);
        aggAllAppStartupMode(dayTimeStr);
    }

    @Override
    public boolean isDataComplete(String targetDateStr) {
        boolean executedTaskByStartupModeDataExists = statisticsDAO.existsStatistics(null, null,
            StatisticsConstants.RESOURCE_EXECUTED_TASK, StatisticsConstants.DIMENSION_TASK_STARTUP_MODE, null,
            targetDateStr);
        boolean allAppExecutedTaskByStartupModeDataExists = statisticsDAO.existsStatistics(null, null,
            StatisticsConstants.RESOURCE_ONE_DAY_EXECUTED_TASK_OF_ALL_APP,
            StatisticsConstants.DIMENSION_TASK_STARTUP_MODE, null, targetDateStr);
        return executedTaskByStartupModeDataExists && allAppExecutedTaskByStartupModeDataExists;
    }
}
