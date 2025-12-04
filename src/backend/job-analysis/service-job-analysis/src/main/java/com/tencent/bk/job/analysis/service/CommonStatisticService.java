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

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.analysis.api.consts.StatisticsConstants;
import com.tencent.bk.job.analysis.api.dto.StatisticsDTO;
import com.tencent.bk.job.analysis.config.StatisticConfig;
import com.tencent.bk.job.analysis.consts.DistributionMetricEnum;
import com.tencent.bk.job.analysis.consts.TotalMetricEnum;
import com.tencent.bk.job.analysis.dao.CurrentTenantStatisticsDAO;
import com.tencent.bk.job.manage.model.remote.SimpleAppInfoDTO;
import com.tencent.bk.job.analysis.model.inner.PerAppStatisticDTO;
import com.tencent.bk.job.analysis.model.web.CommonDistributionVO;
import com.tencent.bk.job.analysis.model.web.CommonStatisticWithRateVO;
import com.tencent.bk.job.analysis.model.web.CommonTrendElementVO;
import com.tencent.bk.job.analysis.util.calc.SimpleMomYoyCalculator;
import com.tencent.bk.job.common.util.CustomCollectionUtils;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.remote.RemoteAppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service("commonStatisticService")
public class CommonStatisticService {

    protected final CurrentTenantStatisticsDAO currentTenantStatisticsDAO;
    protected final StatisticConfig statisticConfig;
    protected final MetricResourceReslover metricResourceReslover;
    protected final RemoteAppService remoteAppService;

    @Autowired
    public CommonStatisticService(CurrentTenantStatisticsDAO currentTenantStatisticsDAO,
                                  StatisticConfig statisticConfig,
                                  MetricResourceReslover metricResourceReslover,
                                  RemoteAppService remoteAppService) {
        this.currentTenantStatisticsDAO = currentTenantStatisticsDAO;
        this.statisticConfig = statisticConfig;
        this.metricResourceReslover = metricResourceReslover;
        this.remoteAppService = remoteAppService;
    }

    public List<Long> getJoinedAppIdList(String date) {
        StatisticsDTO statisticsDTO = currentTenantStatisticsDAO.getStatistics(
            StatisticsConstants.DEFAULT_APP_ID,
            StatisticsConstants.RESOURCE_APP,
            StatisticsConstants.DIMENSION_APP_STATISTIC_TYPE,
            StatisticsConstants.DIMENSION_VALUE_APP_STATISTIC_TYPE_APP_LIST,
            date
        );
        List<SimpleAppInfoDTO> applicationDTOList = JsonUtils.fromJson(
            statisticsDTO.getValue(),
            new TypeReference<List<SimpleAppInfoDTO>>() {
            }
        );
        return applicationDTOList.stream().map(SimpleAppInfoDTO::getId).collect(Collectors.toList());
    }

    /**
     * @param statisticsDTO    统计信息
     * @param momStatisticsDTO 环比参考对象
     * @param yoyStatisticsDTO 同比参考对象
     * @return 带有变化比率的通用统计信息
     */
    public CommonStatisticWithRateVO calcMomYoyStatistic(StatisticsDTO statisticsDTO,
                                                         StatisticsDTO momStatisticsDTO,
                                                         StatisticsDTO yoyStatisticsDTO) {
        return new SimpleMomYoyCalculator(statisticsDTO, momStatisticsDTO, yoyStatisticsDTO).calc();
    }

    public CommonStatisticWithRateVO getCommonTotalStatistics(TotalMetricEnum metric,
                                                              List<Long> appIdList,
                                                              String date) {
        List<StatisticsDTO> statisticsDTOList = currentTenantStatisticsDAO.getStatisticsList(
            appIdList,
            null,
            StatisticsConstants.RESOURCE_GLOBAL,
            StatisticsConstants.DIMENSION_GLOBAL_STATISTIC_TYPE,
            metric.name(),
            date
        );
        // 按日期聚合
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setDate(date);
        long totalValue = 0L;
        for (StatisticsDTO sDTO : statisticsDTOList) {
            totalValue += Long.parseLong(sDTO.getValue());
        }
        statisticsDTO.setValue(Long.toString(totalValue));
        // 与昨天的数据对比计算环比
        List<StatisticsDTO> momStatisticsDTOList = currentTenantStatisticsDAO.getStatisticsList(
            appIdList,
            null,
            StatisticsConstants.RESOURCE_GLOBAL,
            StatisticsConstants.DIMENSION_GLOBAL_STATISTIC_TYPE,
            metric.name(),
            DateUtils.getPreviousDateStr(date, StatisticsConstants.DATE_PATTERN, statisticConfig.getMomDays())
        );
        StatisticsDTO momStatisticsDTO = new StatisticsDTO();
        momStatisticsDTO.setDate(date);
        totalValue = 0L;
        for (StatisticsDTO sDTO : momStatisticsDTOList) {
            totalValue += Long.parseLong(sDTO.getValue());
        }
        momStatisticsDTO.setValue(Long.toString(totalValue));
        // 与上周的数据对比计算同比
        List<StatisticsDTO> yoyStatisticsDTOList = currentTenantStatisticsDAO.getStatisticsList(
            appIdList,
            null,
            StatisticsConstants.RESOURCE_GLOBAL,
            StatisticsConstants.DIMENSION_GLOBAL_STATISTIC_TYPE,
            metric.name(),
            DateUtils.getPreviousDateStr(date, StatisticsConstants.DATE_PATTERN, statisticConfig.getYoyDays())
        );
        StatisticsDTO yoyStatisticsDTO = new StatisticsDTO();
        yoyStatisticsDTO.setDate(date);
        totalValue = 0L;
        for (StatisticsDTO sDTO : yoyStatisticsDTOList) {
            totalValue += Long.parseLong(sDTO.getValue());
        }
        yoyStatisticsDTO.setValue(Long.toString(totalValue));
        return calcMomYoyStatistic(statisticsDTO, momStatisticsDTO, yoyStatisticsDTO);
    }

    public CommonDistributionVO metricDistributionStatistics(DistributionMetricEnum metric,
                                                             List<Long> appIdList,
                                                             String date) {
        String resource = metricResourceReslover.resloveResource(metric);
        List<StatisticsDTO> statisticsDTOList = currentTenantStatisticsDAO.getStatisticsList(
            appIdList,
            resource,
            metric.name(),
            date
        );
        if (statisticsDTOList == null || statisticsDTOList.isEmpty()) {
            return null;
        }
        CommonDistributionVO commonDistributionVO = new CommonDistributionVO();
        Map<String, Long> map = new HashMap<>();
        // 按DimensionValue聚合多个业务的数据
        for (StatisticsDTO statisticsDTO : statisticsDTOList) {
            String key = statisticsDTO.getDimensionValue();
            if (map.containsKey(key)) {
                map.put(key, map.get(key) + Long.parseLong(statisticsDTO.getValue()));
            } else {
                map.put(key, Long.parseLong(statisticsDTO.getValue()));
            }
        }
        commonDistributionVO.setLabelAmountMap(map);
        return commonDistributionVO;
    }

    public List<CommonTrendElementVO> getTrendByMetric(TotalMetricEnum metric, List<Long> appIdList, String startDate
        , String endDate) {
        List<CommonTrendElementVO> trendElementVOList = new ArrayList<>();
        List<StatisticsDTO> statisticsDTOList = currentTenantStatisticsDAO.getStatisticsListBetweenDate(appIdList, null,
            StatisticsConstants.RESOURCE_GLOBAL, StatisticsConstants.DIMENSION_GLOBAL_STATISTIC_TYPE,
            StatisticsConstants.DIMENSION_VALUE_GLOBAL_STATISTIC_TYPE_PREFIX + metric.name(), startDate, endDate);
        // 按日期聚合多个业务的数据
        Map<String, Long> map = new HashMap<>();
        for (StatisticsDTO statisticsDTO : statisticsDTOList) {
            String key = statisticsDTO.getDate();
            if (map.containsKey(key)) {
                map.put(key, map.get(key) + Long.parseLong(statisticsDTO.getValue()));
            } else {
                map.put(key, Long.parseLong(statisticsDTO.getValue()));
            }
        }
        for (Map.Entry<String, Long> entry : map.entrySet()) {
            String key = entry.getKey();
            Long value = entry.getValue();
            CommonTrendElementVO commonTrendElementVO = new CommonTrendElementVO(key, value);
            trendElementVOList.add(commonTrendElementVO);
        }
        trendElementVOList.sort(Comparator.comparing(CommonTrendElementVO::getDate));
        return trendElementVOList;
    }

    public List<PerAppStatisticDTO> listByPerApp(String resource, TotalMetricEnum metric, List<Long> appIdList,
                                                 String date) {
        // 增加筛选范围：已接入的业务
        List<Long> scopedAppIdList = CustomCollectionUtils.mergeList(appIdList, getJoinedAppIdList(date));
        List<StatisticsDTO> statisticsDTOList = currentTenantStatisticsDAO.getStatisticsList(
            scopedAppIdList,
            null,
            resource,
            StatisticsConstants.DIMENSION_GLOBAL_STATISTIC_TYPE,
            StatisticsConstants.DIMENSION_VALUE_GLOBAL_STATISTIC_TYPE_PREFIX + metric.name(), date);
        List<PerAppStatisticDTO> perAppStatisticDTOList = new ArrayList<>();
        long totalValue = 0L;
        for (StatisticsDTO statisticsDTO : statisticsDTOList) {
            Long appId = statisticsDTO.getAppId();
            long value = Long.parseLong(statisticsDTO.getValue());
            totalValue += value;
            PerAppStatisticDTO perAppStatisticDTO = new PerAppStatisticDTO();
            perAppStatisticDTO.setAppId(appId);
            perAppStatisticDTO.setValue(value);
            perAppStatisticDTOList.add(perAppStatisticDTO);
        }
        for (PerAppStatisticDTO perAppStatisticDTO : perAppStatisticDTOList) {
            perAppStatisticDTO.setScopeName(remoteAppService.getAppNameFromCache(perAppStatisticDTO.getAppId()));
            perAppStatisticDTO.setRatio(perAppStatisticDTO.getValue().floatValue() / totalValue);
        }
        perAppStatisticDTOList.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        return perAppStatisticDTOList;
    }
}
