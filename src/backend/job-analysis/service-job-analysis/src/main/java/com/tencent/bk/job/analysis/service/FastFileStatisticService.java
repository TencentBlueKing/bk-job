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

package com.tencent.bk.job.analysis.service;

import com.tencent.bk.job.analysis.config.listener.StatisticConfig;
import com.tencent.bk.job.analysis.dao.StatisticsDAO;
import com.tencent.bk.job.analysis.model.web.DayDistributionElementVO;
import com.tencent.bk.job.common.statistics.consts.StatisticsConstants;
import com.tencent.bk.job.common.statistics.model.dto.StatisticsDTO;
import com.tencent.bk.job.common.util.date.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class FastFileStatisticService extends BaseStatisticService {

    private final StatisticsDAO statisticsDAO;
    private final StatisticConfig statisticConfig;

    @Autowired
    public FastFileStatisticService(StatisticsDAO statisticsDAO, StatisticConfig statisticConfig) {
        super();
        this.statisticsDAO = statisticsDAO;
        this.statisticConfig = statisticConfig;
    }

    public List<DayDistributionElementVO> getFastFileByTransferModeDayDetail(List<Long> appIdList, String startDate,
                                                                             String endDate) {
        List<StatisticsDTO> statisticsDTOList;
        if (appIdList == null) {
            statisticsDTOList = statisticsDAO.getStatisticsListBetweenDate(appIdList, null,
                StatisticsConstants.RESOURCE_ONE_DAY_EXECUTED_FAST_FILE_OF_ALL_APP,
                StatisticsConstants.DIMENSION_FILE_TRANSFER_MODE, startDate, endDate);
            if (statisticsDTOList == null
                || statisticsDTOList.size() < DateUtils.calcDaysBetween(startDate, endDate) + 1) {
                log.info("offline data not ready, calc in mem, startDate={}, endDate={}", startDate, endDate);
                statisticsDTOList = statisticsDAO.getStatisticsListBetweenDate(appIdList,
                    Collections.singletonList(StatisticsConstants.DEFAULT_APP_ID),
                    StatisticsConstants.RESOURCE_EXECUTED_FAST_FILE, StatisticsConstants.DIMENSION_FILE_TRANSFER_MODE
                    , startDate, endDate);
            }
        } else {
            statisticsDTOList = statisticsDAO.getStatisticsListBetweenDate(appIdList,
                Collections.singletonList(StatisticsConstants.DEFAULT_APP_ID),
                StatisticsConstants.RESOURCE_EXECUTED_FAST_FILE, StatisticsConstants.DIMENSION_FILE_TRANSFER_MODE,
                startDate, endDate);
        }
        log.debug("statisticsDTOList={}", statisticsDTOList);
        List<StatisticsDTO> failedStatisticsDTOList = statisticsDAO.getStatisticsListBetweenDate(
            appIdList,
            null,
            StatisticsConstants.RESOURCE_EXECUTED_FAST_FILE,
            StatisticsConstants.DIMENSION_STEP_RUN_STATUS,
            StatisticsConstants.DIMENSION_VALUE_STEP_RUN_STATUS_FAIL,
            startDate,
            endDate
        );
        log.debug("failedStatisticsDTOList={}", failedStatisticsDTOList);
        List<DayDistributionElementVO> dayDistributionElementVOList = groupByDateAndDimensionValue(statisticsDTOList,
            failedStatisticsDTOList);
        log.debug("dayDistributionElementVOList={}", dayDistributionElementVOList);
        return dayDistributionElementVOList;
    }
}
