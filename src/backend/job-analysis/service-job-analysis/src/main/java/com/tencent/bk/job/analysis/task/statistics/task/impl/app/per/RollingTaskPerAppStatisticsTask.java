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
import com.tencent.bk.job.manage.model.inner.resp.ServiceApplicationDTO;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@StatisticsTask
@Service
public class RollingTaskPerAppStatisticsTask extends ExecuteBasePerAppStatisticsTask {

    @Autowired
    public RollingTaskPerAppStatisticsTask(ExecuteMetricsClient executeMetricsClient,
                                           BasicServiceManager basicServiceManager, StatisticsDAO statisticsDAO,
                                           DSLContext dslContext) {
        super(executeMetricsClient, basicServiceManager, statisticsDAO, dslContext);
    }

    private StatisticsDTO getTaskTypeBaseStatisticsDTO(ServiceApplicationDTO app, String timeTag) {
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(app.getId());
        statisticsDTO.setCreateTime(System.currentTimeMillis());
        statisticsDTO.setDate(timeTag);
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_ROLLING_TASK);//滚动执行过的任务
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_TASK_TYPE);//作业类型
        return statisticsDTO;
    }

    @Override
    public List<StatisticsDTO> getStatisticsFrom(ServiceApplicationDTO app, Long fromTime, Long toTime,
                                                 String timeTag) {
        List<StatisticsDTO> statisticsDTOList = new ArrayList<>();
        // 1.普通作业
        StatisticsDTO statisticsDTO = getTaskTypeBaseStatisticsDTO(app, timeTag);
        statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_TASK_TYPE_EXECUTE_TASK);
        addExecuteStatisticsDTO(statisticsDTO, statisticsDTOList);
        // 2.快速执行脚本
        statisticsDTO = getTaskTypeBaseStatisticsDTO(app, timeTag);
        statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_TASK_TYPE_FAST_EXECUTE_SCRIPT);
        addExecuteStatisticsDTO(statisticsDTO, statisticsDTOList);
        // 3.快速分发文件
        statisticsDTO = getTaskTypeBaseStatisticsDTO(app, timeTag);
        statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_TASK_TYPE_FAST_PUSH_FILE);
        addExecuteStatisticsDTO(statisticsDTO, statisticsDTOList);
        return statisticsDTOList;
    }

    /**
     * 聚合多个业务的任务类型数据
     *
     * @param dayTimeStr
     */
    public void aggregateAllAppTaskTypeStatistics(String dayTimeStr) {
        aggregateAllAppTaskTypeStatistics(dayTimeStr, StatisticsConstants.DIMENSION_VALUE_TASK_TYPE_EXECUTE_TASK);
        aggregateAllAppTaskTypeStatistics(dayTimeStr,
            StatisticsConstants.DIMENSION_VALUE_TASK_TYPE_FAST_EXECUTE_SCRIPT);
        aggregateAllAppTaskTypeStatistics(dayTimeStr, StatisticsConstants.DIMENSION_VALUE_TASK_TYPE_FAST_PUSH_FILE);
    }

    public void aggregateAllAppTaskTypeStatistics(String dayTimeStr, String timeConsumingDimensionValue) {
        Long totalValue = statisticsDAO.getTotalValueOfStatisticsList(null,
            Collections.singletonList(StatisticsConstants.DEFAULT_APP_ID), StatisticsConstants.RESOURCE_ROLLING_TASK
            , StatisticsConstants.DIMENSION_TASK_TYPE, timeConsumingDimensionValue, dayTimeStr);
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(StatisticsConstants.DEFAULT_APP_ID);
        statisticsDTO.setCreateTime(System.currentTimeMillis());
        statisticsDTO.setDate(dayTimeStr);
        statisticsDTO.setValue(totalValue.toString());
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_ONE_DAY_ROLLING_TASK_OF_ALL_APP);
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_TASK_TYPE);
        statisticsDTO.setDimensionValue(timeConsumingDimensionValue);
        statisticsDAO.upsertStatistics(dslContext, statisticsDTO);
    }

    @Override
    public void genStatisticsByDay(LocalDateTime dateTime) {
        super.genStatisticsByDay(dateTime);
        // 聚合多个业务的数据
        String dayTimeStr = getDayTimeStr(dateTime);
        aggregateAllAppTaskTypeStatistics(dayTimeStr);
    }

    @Override
    public boolean isDataComplete(String targetDateStr) {
        boolean executedTaskByTaskTypeDataExists = statisticsDAO.existsStatistics(null, null,
            StatisticsConstants.RESOURCE_ROLLING_TASK, StatisticsConstants.DIMENSION_TASK_TYPE, null, targetDateStr);
        boolean allAppExecutedTaskByTaskTypeDataExists = statisticsDAO.existsStatistics(null, null,
            StatisticsConstants.RESOURCE_ONE_DAY_ROLLING_TASK_OF_ALL_APP, StatisticsConstants.DIMENSION_TASK_TYPE,
            null, targetDateStr);
        return executedTaskByTaskTypeDataExists && allAppExecutedTaskByTaskTypeDataExists;
    }
}
