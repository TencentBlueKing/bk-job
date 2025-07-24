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

package com.tencent.bk.job.analysis.service;

import com.tencent.bk.job.analysis.api.consts.StatisticsConstants;
import com.tencent.bk.job.analysis.api.dto.StatisticsDTO;
import com.tencent.bk.job.analysis.dao.StatisticsDAO;
import com.tencent.bk.job.analysis.model.web.CommonDistributionVO;
import com.tencent.bk.job.analysis.model.web.DayDistributionElementVO;
import com.tencent.bk.job.common.util.date.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RollingTaskStatisticService extends BaseStatisticService {

    private final StatisticsDAO statisticsDAO;
    private final ExecutedTaskStatisticService executedTaskStatisticService;

    @Autowired
    public RollingTaskStatisticService(StatisticsDAO statisticsDAO,
                                       ExecutedTaskStatisticService executedTaskStatisticService) {
        super();
        this.statisticsDAO = statisticsDAO;
        this.executedTaskStatisticService = executedTaskStatisticService;
    }

    private List<StatisticsDTO> getRollingTaskFailedStatisticsDTOList(List<Long> appIdList, String startDate, String endDate) {
        List<StatisticsDTO> failedStatisticsDTOList;
        if (appIdList == null) {
            // 全局指标
            failedStatisticsDTOList =
                statisticsDAO.getStatisticsListBetweenDate(
                    Collections.singletonList(StatisticsConstants.DEFAULT_APP_ID),
                    null,
                    StatisticsConstants.RESOURCE_ROLLING_FAILED_TASK,
                    StatisticsConstants.DIMENSION_TIME_UNIT,
                    StatisticsConstants.DIMENSION_VALUE_TIME_UNIT_DAY,
                    startDate,
                    endDate
                );
            if (failedStatisticsDTOList == null || failedStatisticsDTOList.isEmpty()) {
                failedStatisticsDTOList = statisticsDAO.getStatisticsListBetweenDate(appIdList, null,
                    StatisticsConstants.RESOURCE_ROLLING_FAILED_TASK, StatisticsConstants.DIMENSION_TIME_UNIT,
                    StatisticsConstants.DIMENSION_VALUE_TIME_UNIT_DAY, startDate, endDate);
            }
        } else {
            // 按业务聚合
            failedStatisticsDTOList = statisticsDAO.getStatisticsListBetweenDate(appIdList,
                StatisticsConstants.GLOBAL_APP_ID_LIST, StatisticsConstants.RESOURCE_ROLLING_FAILED_TASK,
                StatisticsConstants.DIMENSION_TIME_UNIT, StatisticsConstants.DIMENSION_VALUE_TIME_UNIT_DAY, startDate
                , endDate);
        }
        log.debug("RollingTaskFailedStatisticsDTOList={}", failedStatisticsDTOList);
        return failedStatisticsDTOList;
    }

    public List<DayDistributionElementVO> rollingTaskByTaskTypeDayDetail(List<Long> appIdList, String startDate,
                                                                         String endDate) {
        // 滚动执行统计
        List<DayDistributionElementVO> rollingTaskDayDetailList = getByTaskTypeDayDetail(appIdList, startDate, endDate);
        // 任务执行统计
        List<DayDistributionElementVO> totalTaskDayDetailList =
            executedTaskStatisticService.getByTaskTypeDayDetail(appIdList, startDate, endDate);

        // 将任务执行统计(总执行次数)合入到对应的滚动执行统计中,key以'_TOTAL'结尾
        List<DayDistributionElementVO> mixTaskDayDetailList = new ArrayList<>();
        for (int i = 0; i < rollingTaskDayDetailList.size(); i++) {
            DayDistributionElementVO rollingEleVO = rollingTaskDayDetailList.get(i);
            CommonDistributionVO rollingVO = rollingEleVO.getDistribution();
            Map<String, Long> rollingMap = rollingVO.getLabelAmountMap();
            Map<String, Long> mixMap = new HashMap<>();
            for (String key : rollingMap.keySet()) {
                mixMap.put(key, rollingMap.get(key));
                try {
                    DayDistributionElementVO elementVO = totalTaskDayDetailList.get(i);
                    CommonDistributionVO distributionVO = elementVO.getDistribution();
                    Map<String, Long> amountMap = distributionVO.getLabelAmountMap();
                    long total = amountMap.get(key);
                    mixMap.put(key + "_TOTAL", total);
                } catch (Exception e) {
                    mixMap.put(key + "_TOTAL", 0L);
                    log.error("taskTypeDayDetail may be empty!", e);
                }
            }
            rollingVO.setLabelAmountMap(mixMap);
            rollingEleVO.setDistribution(rollingVO);
            mixTaskDayDetailList.add(rollingEleVO);
        }
        return mixTaskDayDetailList;
    }

    private List<DayDistributionElementVO> getByTaskTypeDayDetail(List<Long> appIdList, String startDate, String endDate) {
        List<StatisticsDTO> statisticsDTOList;
        if (appIdList == null) {
            // 全部业务，直接拿离线聚合后的数据
            statisticsDTOList = statisticsDAO.getStatisticsListBetweenDate(appIdList, null,
                StatisticsConstants.RESOURCE_ONE_DAY_ROLLING_TASK_OF_ALL_APP,
                StatisticsConstants.DIMENSION_TASK_TYPE, startDate, endDate);
            if (statisticsDTOList == null
                || statisticsDTOList.size() < DateUtils.calcDaysBetween(startDate, endDate) + 1) {
                log.info("offline data not ready, calc in mem, startDate={}, endDate={}", startDate, endDate);
                // 离线聚合数据暂未统计完成
                statisticsDTOList = statisticsDAO.getStatisticsListBetweenDate(appIdList,
                    Collections.singletonList(StatisticsConstants.DEFAULT_APP_ID),
                    StatisticsConstants.RESOURCE_ROLLING_TASK, StatisticsConstants.DIMENSION_TASK_TYPE, startDate,
                    endDate);
            }
        } else {
            statisticsDTOList = statisticsDAO.getStatisticsListBetweenDate(appIdList,
                Collections.singletonList(StatisticsConstants.DEFAULT_APP_ID),
                StatisticsConstants.RESOURCE_ROLLING_TASK, StatisticsConstants.DIMENSION_TASK_TYPE, startDate,
                endDate);
        }
        log.debug("statisticsDTOList={}", statisticsDTOList);
        List<DayDistributionElementVO> dayDistributionElementVOList = groupByDateAndDimensionValue(statisticsDTOList,
            getRollingTaskFailedStatisticsDTOList(appIdList, startDate, endDate));
        log.debug("dayDistributionElementVOList={}", dayDistributionElementVOList);
        return dayDistributionElementVOList;
    }
}
