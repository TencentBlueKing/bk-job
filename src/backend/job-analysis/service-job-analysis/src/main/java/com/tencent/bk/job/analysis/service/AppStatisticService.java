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

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.analysis.config.StatisticConfig;
import com.tencent.bk.job.analysis.dao.StatisticsDAO;
import com.tencent.bk.job.analysis.model.dto.SimpleAppInfoDTO;
import com.tencent.bk.job.analysis.model.web.CommonStatisticWithRateVO;
import com.tencent.bk.job.analysis.model.web.CommonTrendElementVO;
import com.tencent.bk.job.analysis.model.web.PerAppStatisticVO;
import com.tencent.bk.job.analysis.util.calc.AppMomYoyCalculator;
import com.tencent.bk.job.common.statistics.consts.StatisticsConstants;
import com.tencent.bk.job.common.statistics.model.dto.StatisticsDTO;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AppStatisticService extends CommonStatisticService {

    @Autowired
    public AppStatisticService(StatisticsDAO statisticsDAO, StatisticConfig statisticConfig,
                               MetricResourceReslover metricResourceReslover, AppService appService) {
        super(statisticsDAO, statisticConfig, metricResourceReslover, appService);
    }

    /**
     * @param statisticsDTO
     * @param momStatisticsDTO 环比参考对象
     * @param yoyStatisticsDTO 同比参考对象
     * @return
     */
    public CommonStatisticWithRateVO calcAppMomYoyStatistic(StatisticsDTO statisticsDTO,
                                                            StatisticsDTO momStatisticsDTO,
                                                            StatisticsDTO yoyStatisticsDTO) {
        return new AppMomYoyCalculator(statisticsDTO, momStatisticsDTO, yoyStatisticsDTO).getResult();
    }

    public CommonStatisticWithRateVO getAppTotalStatistics(String username, List<Long> appIdList, String date) {
        StatisticsDTO statisticsDTO = statisticsDAO.getStatistics(StatisticsConstants.DEFAULT_APP_ID,
            StatisticsConstants.RESOURCE_APP, StatisticsConstants.DIMENSION_APP_STATISTIC_TYPE,
            StatisticsConstants.DIMENSION_VALUE_APP_STATISTIC_TYPE_APP_LIST, date);
        // 与昨天的数据对比计算环比
        StatisticsDTO momStatisticsDTO = statisticsDAO.getStatistics(StatisticsConstants.DEFAULT_APP_ID,
            StatisticsConstants.RESOURCE_APP, StatisticsConstants.DIMENSION_APP_STATISTIC_TYPE,
            StatisticsConstants.DIMENSION_VALUE_APP_STATISTIC_TYPE_APP_LIST, DateUtils.getPreviousDateStr(date,
                StatisticsConstants.DATE_PATTERN, statisticConfig.getMomDays()));
        // 与上周的数据对比计算同比
        StatisticsDTO yoyStatisticsDTO = statisticsDAO.getStatistics(StatisticsConstants.DEFAULT_APP_ID,
            StatisticsConstants.RESOURCE_APP, StatisticsConstants.DIMENSION_APP_STATISTIC_TYPE,
            StatisticsConstants.DIMENSION_VALUE_APP_STATISTIC_TYPE_APP_LIST, DateUtils.getPreviousDateStr(date,
                StatisticsConstants.DATE_PATTERN, statisticConfig.getYoyDays()));
        return calcAppMomYoyStatistic(statisticsDTO, momStatisticsDTO, yoyStatisticsDTO);
    }

    public CommonStatisticWithRateVO getActiveAppTotalStatistics(String username, List<Long> appIdList, String date) {
        StatisticsDTO statisticsDTO = statisticsDAO.getStatistics(StatisticsConstants.DEFAULT_APP_ID,
            StatisticsConstants.RESOURCE_APP, StatisticsConstants.DIMENSION_APP_STATISTIC_TYPE,
            StatisticsConstants.DIMENSION_VALUE_APP_STATISTIC_TYPE_ACTIVE_APP_LIST, date);
        // 与昨天的数据对比计算环比
        StatisticsDTO momStatisticsDTO = statisticsDAO.getStatistics(StatisticsConstants.DEFAULT_APP_ID,
            StatisticsConstants.RESOURCE_APP, StatisticsConstants.DIMENSION_APP_STATISTIC_TYPE,
            StatisticsConstants.DIMENSION_VALUE_APP_STATISTIC_TYPE_ACTIVE_APP_LIST, DateUtils.getPreviousDateStr(date
                , StatisticsConstants.DATE_PATTERN, statisticConfig.getMomDays()));
        // 与上周的数据对比计算同比
        StatisticsDTO yoyStatisticsDTO = statisticsDAO.getStatistics(StatisticsConstants.DEFAULT_APP_ID,
            StatisticsConstants.RESOURCE_APP, StatisticsConstants.DIMENSION_APP_STATISTIC_TYPE,
            StatisticsConstants.DIMENSION_VALUE_APP_STATISTIC_TYPE_ACTIVE_APP_LIST, DateUtils.getPreviousDateStr(date
                , StatisticsConstants.DATE_PATTERN, statisticConfig.getYoyDays()));
        return calcAppMomYoyStatistic(statisticsDTO, momStatisticsDTO, yoyStatisticsDTO);
    }

    private List<SimpleAppInfoDTO> getFilteredAppDTOList(List<Long> appIdList, StatisticsDTO statisticsDTO) {
        List<SimpleAppInfoDTO> applicationDTOList = JsonUtils.fromJson(statisticsDTO.getValue(),
            new TypeReference<List<SimpleAppInfoDTO>>() {
        });
        if (appIdList != null) {
            Set<Long> appIdSet = new HashSet<>(appIdList);
            applicationDTOList =
                applicationDTOList.parallelStream().filter(
                    applicationDTO -> appIdSet.contains(applicationDTO.getId())
                ).collect(Collectors.toList());
        }
        return applicationDTOList;
    }

    private List<PerAppStatisticVO> extractAppFromStatistics(List<Long> appIdList, StatisticsDTO statisticsDTO) {
        if (statisticsDTO == null) return null;
        List<SimpleAppInfoDTO> applicationDTOList = getFilteredAppDTOList(appIdList, statisticsDTO);
        List<PerAppStatisticVO> perAppStatisticVOList = new ArrayList<>();
        for (SimpleAppInfoDTO applicationDTO : applicationDTOList) {
            PerAppStatisticVO perAppStatisticVO = new PerAppStatisticVO();
            perAppStatisticVO.setAppId(applicationDTO.getId());
            perAppStatisticVO.setAppName(applicationDTO.getName());
            perAppStatisticVO.setValue(1L);
            perAppStatisticVO.setRatio(1.0f / applicationDTOList.size());
            perAppStatisticVOList.add(perAppStatisticVO);
        }
        return perAppStatisticVOList;
    }

    /**
     * 已接入业务列表
     *
     * @param appIdList
     * @param date
     * @return
     */
    public List<PerAppStatisticVO> listJoinedApp(List<Long> appIdList, String date) {
        StatisticsDTO statisticsDTO = statisticsDAO.getStatistics(StatisticsConstants.DEFAULT_APP_ID,
            StatisticsConstants.RESOURCE_APP, StatisticsConstants.DIMENSION_APP_STATISTIC_TYPE,
            StatisticsConstants.DIMENSION_VALUE_APP_STATISTIC_TYPE_APP_LIST, date);
        return extractAppFromStatistics(appIdList, statisticsDTO);
    }

    /**
     * 活跃业务列表
     *
     * @param appIdList
     * @param date
     * @return
     */
    public List<PerAppStatisticVO> listActiveApp(List<Long> appIdList, String date) {
        StatisticsDTO statisticsDTO = statisticsDAO.getStatistics(StatisticsConstants.DEFAULT_APP_ID,
            StatisticsConstants.RESOURCE_APP, StatisticsConstants.DIMENSION_APP_STATISTIC_TYPE,
            StatisticsConstants.DIMENSION_VALUE_APP_STATISTIC_TYPE_ACTIVE_APP_LIST, date);
        return extractAppFromStatistics(appIdList, statisticsDTO);
    }

    private List<CommonTrendElementVO> extractTrendFromStatisticsList(List<Long> appIdList,
                                                                      List<StatisticsDTO> statisticsDTOList) {
        List<CommonTrendElementVO> trendElementVOList = new ArrayList<>();
        for (StatisticsDTO statisticsDTO : statisticsDTOList) {
            List<SimpleAppInfoDTO> applicationDTOList = getFilteredAppDTOList(appIdList, statisticsDTO);
            CommonTrendElementVO commonTrendElementVO = new CommonTrendElementVO();
            commonTrendElementVO.setDate(statisticsDTO.getDate());
            commonTrendElementVO.setValue(Long.valueOf(applicationDTOList.size()));
            trendElementVOList.add(commonTrendElementVO);
        }
        return trendElementVOList;
    }

    public List<CommonTrendElementVO> getJoinedAppTrend(List<Long> appIdList, String startDate, String endDate) {
        List<StatisticsDTO> statisticsDTOList =
            statisticsDAO.getStatisticsListBetweenDate(StatisticsConstants.DEFAULT_APP_ID,
                StatisticsConstants.RESOURCE_APP, StatisticsConstants.DIMENSION_APP_STATISTIC_TYPE,
                StatisticsConstants.DIMENSION_VALUE_APP_STATISTIC_TYPE_APP_LIST, startDate, endDate);
        return extractTrendFromStatisticsList(appIdList, statisticsDTOList);
    }

    public List<CommonTrendElementVO> getActiveAppTrend(List<Long> appIdList, String startDate, String endDate) {
        List<StatisticsDTO> statisticsDTOList =
            statisticsDAO.getStatisticsListBetweenDate(StatisticsConstants.DEFAULT_APP_ID,
                StatisticsConstants.RESOURCE_APP, StatisticsConstants.DIMENSION_APP_STATISTIC_TYPE,
                StatisticsConstants.DIMENSION_VALUE_APP_STATISTIC_TYPE_ACTIVE_APP_LIST, startDate, endDate);
        return extractTrendFromStatisticsList(appIdList, statisticsDTOList);
    }
}
