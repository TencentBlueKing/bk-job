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

package com.tencent.bk.job.analysis.api.web.impl;

import com.tencent.bk.job.analysis.api.web.WebStatisticsResource;
import com.tencent.bk.job.analysis.config.listener.StatisticConfig;
import com.tencent.bk.job.analysis.consts.DimensionEnum;
import com.tencent.bk.job.analysis.consts.DistributionMetricEnum;
import com.tencent.bk.job.analysis.consts.ResourceEnum;
import com.tencent.bk.job.analysis.consts.TotalMetricEnum;
import com.tencent.bk.job.analysis.model.web.*;
import com.tencent.bk.job.analysis.service.*;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.statistics.consts.StatisticsConstants;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.jooq.tools.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
public class WebStatisticsResourceImpl implements WebStatisticsResource {

    private final AppStatisticService appStatisticService;
    private final ExecutedTaskStatisticService executedTaskStatisticService;
    private final FastScriptStatisticService fastScriptStatisticService;
    private final FastFileStatisticService fastFileStatisticService;
    private final TagStatisticService tagStatisticService;
    private final CommonStatisticService commonStatisticService;
    private final StatisticConfig statisticConfig;
    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public WebStatisticsResourceImpl(
        AppStatisticService appStatisticService,
        ExecutedTaskStatisticService executedTaskStatisticService,
        FastScriptStatisticService fastScriptStatisticService,
        FastFileStatisticService fastFileStatisticService,
        TagStatisticService tagStatisticService,
        CommonStatisticService commonStatisticService,
        StatisticConfig statisticConfig,
        RedisTemplate<String, String> redisTemplate
    ) {
        this.appStatisticService = appStatisticService;
        this.executedTaskStatisticService = executedTaskStatisticService;
        this.fastScriptStatisticService = fastScriptStatisticService;
        this.fastFileStatisticService = fastFileStatisticService;
        this.tagStatisticService = tagStatisticService;
        this.commonStatisticService = commonStatisticService;
        this.statisticConfig = statisticConfig;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public ServiceResponse<CommonStatisticWithRateVO> totalStatistics(
        String username,
        TotalMetricEnum metric,
        List<Long> appIdList,
        String date
    ) {
        if (StringUtils.isBlank(date)) {
            date = DateUtils.getCurrentDateStr();
        }
        CommonStatisticWithRateVO statisticWithRateVO = null;
        if (TotalMetricEnum.APP_COUNT == metric) {
            statisticWithRateVO = appStatisticService.getAppTotalStatistics(username, appIdList, date);
        } else if (TotalMetricEnum.ACTIVE_APP_COUNT == metric) {
            statisticWithRateVO = appStatisticService.getActiveAppTotalStatistics(username, appIdList, date);
        } else {
            statisticWithRateVO = commonStatisticService.getCommonTotalStatistics(metric, appIdList, date);
        }
        return ServiceResponse.buildSuccessResp(statisticWithRateVO);
    }

    @Override
    public ServiceResponse<List<CommonTrendElementVO>> trends(
        String username,
        TotalMetricEnum metric,
        List<Long> appIdList,
        String dataStartDate,
        String startDate,
        String endDate
    ) {
        if (StringUtils.isBlank(startDate)) {
            startDate = DateUtils.getCurrentDateStr();
        }
        if (StringUtils.isBlank(endDate)) {
            endDate = DateUtils.getCurrentDateStr();
        }
        List<CommonTrendElementVO> commonTrendElementVOList;
        if (TotalMetricEnum.APP_COUNT == metric) {
            commonTrendElementVOList = appStatisticService.getJoinedAppTrend(appIdList, startDate, endDate);
        } else if (TotalMetricEnum.ACTIVE_APP_COUNT == metric) {
            commonTrendElementVOList = appStatisticService.getActiveAppTrend(appIdList, startDate, endDate);
        } else {
            commonTrendElementVOList = commonStatisticService.getTrendByMetric(metric, appIdList, startDate, endDate);
        }
        // 按日期升序排列
        commonTrendElementVOList.sort(new Comparator<CommonTrendElementVO>() {
            @Override
            public int compare(CommonTrendElementVO o1, CommonTrendElementVO o2) {
                return o1.getDate().compareTo(o2.getDate());
            }
        });
        // 空数据补全
        if (commonTrendElementVOList.isEmpty()) {
            CommonTrendElementVO lastDayElementVO = new CommonTrendElementVO();
            lastDayElementVO.setDate(endDate);
            lastDayElementVO.setValue(0);
            commonTrendElementVOList.add(lastDayElementVO);
        }
        CommonTrendElementVO commonTrendElementVO = commonTrendElementVOList.get(0);
        String dateStr = commonTrendElementVO.getDate();
        int count = 0;
        while (!dateStr.equals(startDate) && count < 3650) {
            String lastDayDateStr = DateUtils.getLastDateStr(dateStr);
            CommonTrendElementVO lastDayElementVO = new CommonTrendElementVO();
            lastDayElementVO.setDate(lastDayDateStr);
            lastDayElementVO.setValue(0);
            log.debug("add empty commonTrendElementVO for {}", lastDayDateStr);
            commonTrendElementVOList.add(0, lastDayElementVO);
            count += 1;
            dateStr = lastDayDateStr;
        }
        log.debug("commonTrendElementVOList={}", commonTrendElementVOList);
        return ServiceResponse.buildSuccessResp(commonTrendElementVOList);
    }

    @Override
    public ServiceResponse<List<PerAppStatisticVO>> listByPerApp(
        String username,
        TotalMetricEnum metric,
        List<Long> appIdList,
        String date
    ) {
        if (StringUtils.isBlank(date)) {
            date = DateUtils.getCurrentDateStr();
        }
        List<PerAppStatisticVO> perAppStatisticVOList;
        if (TotalMetricEnum.APP_COUNT == metric) {
            perAppStatisticVOList = appStatisticService.listJoinedApp(appIdList, date);
        } else if (TotalMetricEnum.ACTIVE_APP_COUNT == metric) {
            perAppStatisticVOList = appStatisticService.listActiveApp(appIdList, date);
        } else if (TotalMetricEnum.EXECUTED_TASK_COUNT == metric) {
            perAppStatisticVOList = commonStatisticService.listByPerApp(StatisticsConstants.RESOURCE_EXECUTED_TASK,
                metric, appIdList, date);
        } else if (TotalMetricEnum.FAILED_TASK_COUNT == metric) {
            perAppStatisticVOList = commonStatisticService.listByPerApp(StatisticsConstants.RESOURCE_FAILED_TASK,
                metric, appIdList, date);
        } else {
            perAppStatisticVOList = commonStatisticService.listByPerApp(StatisticsConstants.RESOURCE_GLOBAL, metric,
                appIdList, date);
        }
        return ServiceResponse.buildSuccessResp(perAppStatisticVOList);
    }

    @Override
    public ServiceResponse<CommonDistributionVO> distributionStatistics(
        String username,
        DistributionMetricEnum metric,
        List<Long> appIdList,
        String date
    ) {
        if (StringUtils.isBlank(date)) {
            date = DateUtils.getCurrentDateStr();
        }
        CommonDistributionVO commonDistributionVO;
        if (DistributionMetricEnum.TAG == metric) {
            commonDistributionVO = tagStatisticService.tagDistributionStatistics(appIdList, date);
        } else {
            commonDistributionVO = commonStatisticService.metricDistributionStatistics(metric, appIdList, date);
        }
        return ServiceResponse.buildSuccessResp(commonDistributionVO);
    }

    private List<DayDistributionElementVO> executedTaskByStartupModeDayDetail(
        String username,
        List<Long> appIdList,
        String startDate,
        String endDate
    ) {
        return executedTaskStatisticService.getByStartupModeDayDetail(appIdList, startDate, endDate);
    }

    private List<DayDistributionElementVO> executedTaskByTaskTypeDayDetail(
        String username,
        List<Long> appIdList,
        String startDate,
        String endDate
    ) {
        return executedTaskStatisticService.getByTaskTypeDayDetail(appIdList, startDate, endDate);
    }

    private List<DayDistributionElementVO> executedTaskByTimeConsumingDayDetail(
        String username,
        List<Long> appIdList,
        String startDate,
        String endDate
    ) {
        return executedTaskStatisticService.getByTimeConsumingDayDetail(appIdList, startDate, endDate);
    }

    private List<DayDistributionElementVO> fastScriptByScriptTypeDayDetail(
        String username,
        List<Long> appIdList,
        String startDate,
        String endDate
    ) {
        return fastScriptStatisticService.getFastScriptByScriptTypeDayDetail(appIdList, startDate, endDate);
    }

    private List<DayDistributionElementVO> fastFileByTransferModeDayDetail(
        String username,
        List<Long> appIdList,
        String startDate,
        String endDate
    ) {
        return fastFileStatisticService.getFastFileByTransferModeDayDetail(appIdList, startDate, endDate);
    }

    @Override
    public ServiceResponse<List<DayDistributionElementVO>> dayDetailStatistics(
        String username,
        ResourceEnum resource,
        DimensionEnum dimension,
        List<Long> appIdList,
        String startDate,
        String endDate
    ) {
        if (StringUtils.isBlank(startDate)) {
            startDate = DateUtils.getCurrentDateStr();
        }
        if (StringUtils.isBlank(endDate)) {
            endDate = DateUtils.getCurrentDateStr();
        }
        List<DayDistributionElementVO> dayDistributionElementVOList = new ArrayList<>();
        if (ResourceEnum.EXECUTED_TASK == resource && DimensionEnum.TASK_STARTUP_MODE == dimension) {
            dayDistributionElementVOList = executedTaskByStartupModeDayDetail(username, appIdList, startDate, endDate);
        } else if (ResourceEnum.EXECUTED_TASK == resource && DimensionEnum.TASK_TYPE == dimension) {
            dayDistributionElementVOList = executedTaskByTaskTypeDayDetail(username, appIdList, startDate, endDate);
        } else if (ResourceEnum.EXECUTED_TASK == resource && DimensionEnum.TASK_TIME_CONSUMING == dimension) {
            dayDistributionElementVOList = executedTaskByTimeConsumingDayDetail(username, appIdList, startDate,
                endDate);
        } else if (ResourceEnum.EXECUTED_FAST_SCRIPT == resource && DimensionEnum.SCRIPT_TYPE == dimension) {
            dayDistributionElementVOList = fastScriptByScriptTypeDayDetail(username, appIdList, startDate, endDate);
        } else if (ResourceEnum.EXECUTED_FAST_FILE == resource && DimensionEnum.FILE_TRANSFER_MODE == dimension) {
            dayDistributionElementVOList = fastFileByTransferModeDayDetail(username, appIdList, startDate, endDate);
        } else {
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM, String.format("dimension %s of " +
                "resource %s not supported", dimension.name(), resource.name()));
        }
        return ServiceResponse.buildSuccessResp(dayDistributionElementVOList);
    }

    @Override
    public ServiceResponse<Map<String, String>> getStatisticsDataInfo(String username) {
        Map<String, String> statisticsDataInfoMap = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime targetDate = now.minusDays(statisticConfig.getExpireDays());
        String startDateStr = TimeUtil.getTimeStr(targetDate, StatisticsConstants.DATE_PATTERN);
        statisticsDataInfoMap.put(StatisticsConstants.KEY_DATA_START_DATE, startDateStr);
        String updateDateStr = redisTemplate.opsForValue().get(StatisticsConstants.KEY_DATA_UPDATE_TIME);
        if (updateDateStr == null) updateDateStr = "";
        statisticsDataInfoMap.put(StatisticsConstants.KEY_DATA_UPDATE_TIME, updateDateStr);
        return ServiceResponse.buildSuccessResp(statisticsDataInfoMap);
    }
}
