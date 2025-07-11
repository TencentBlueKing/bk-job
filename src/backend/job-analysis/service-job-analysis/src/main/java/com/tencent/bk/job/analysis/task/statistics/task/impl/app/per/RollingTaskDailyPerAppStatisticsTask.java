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

package com.tencent.bk.job.analysis.task.statistics.task.impl.app.per;

import com.tencent.bk.job.analysis.api.consts.StatisticsConstants;
import com.tencent.bk.job.analysis.api.dto.StatisticsDTO;
import com.tencent.bk.job.analysis.dao.StatisticsDAO;
import com.tencent.bk.job.analysis.service.BasicServiceManager;
import com.tencent.bk.job.analysis.task.statistics.anotation.StatisticsTask;
import com.tencent.bk.job.analysis.task.statistics.task.ExecuteBasePerAppStatisticsTask;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.api.inner.ServiceMetricsResource;
import com.tencent.bk.job.manage.model.inner.resp.ServiceApplicationDTO;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@StatisticsTask
@Service
public class RollingTaskDailyPerAppStatisticsTask extends ExecuteBasePerAppStatisticsTask {

    @Autowired
    public RollingTaskDailyPerAppStatisticsTask(ServiceMetricsResource executeMetricsResource,
                                                BasicServiceManager basicServiceManager,
                                                StatisticsDAO statisticsDAO,
                                                @Qualifier("job-analysis-dsl-context") DSLContext dslContext) {
        super(executeMetricsResource, basicServiceManager, statisticsDAO, dslContext);
    }

    private StatisticsDTO getFailedTaskBaseStatisticsDTO(ServiceApplicationDTO app, String timeTag) {
        StatisticsDTO statisticsDTO = getTimeUnitBaseStatisticsDTO(app, timeTag);
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_ROLLING_FAILED_TASK);//失败的任务
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
        StatisticsDTO statisticsDTO = getFailedTaskBaseStatisticsDTO(app, timeTag);
        statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_TIME_UNIT_DAY);
        addExecuteStatisticsDTO(statisticsDTO, statisticsDTOList);
        return statisticsDTOList;
    }

    /**
     * 汇总所有业务的失败执行总量
     *
     * @param dayTimeStr
     */
    public void updateTotalFailedTaskStatistics(String dayTimeStr) {
        List<StatisticsDTO> statisticsDTOList = statisticsDAO.getStatisticsList(
            null,
            Collections.singletonList(StatisticsConstants.DEFAULT_APP_ID),
            StatisticsConstants.RESOURCE_ROLLING_FAILED_TASK,
            StatisticsConstants.DIMENSION_TIME_UNIT,
            StatisticsConstants.DIMENSION_VALUE_TIME_UNIT_DAY,
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
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_ROLLING_FAILED_TASK);
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_TIME_UNIT);
        statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_TIME_UNIT_DAY);
        statisticsDAO.upsertStatistics(dslContext, statisticsDTO);
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
            updateTotalFailedTaskStatistics(timeStr);
            timeStr = DateUtils.getNextDateStr(timeStr);
            count += 1;
        }
    }
}
