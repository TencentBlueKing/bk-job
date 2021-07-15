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

package com.tencent.bk.job.analysis.task.statistics.task.impl.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.analysis.client.ExecuteMetricsClient;
import com.tencent.bk.job.analysis.config.StatisticConfig;
import com.tencent.bk.job.analysis.dao.StatisticsDAO;
import com.tencent.bk.job.analysis.model.dto.SimpleAppInfoDTO;
import com.tencent.bk.job.analysis.service.BasicServiceManager;
import com.tencent.bk.job.analysis.task.statistics.anotation.StatisticsTask;
import com.tencent.bk.job.analysis.task.statistics.task.BaseStatisticsTask;
import com.tencent.bk.job.common.statistics.consts.StatisticsConstants;
import com.tencent.bk.job.common.statistics.model.dto.StatisticsDTO;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.model.inner.ServiceApplicationDTO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 统计接入业务与活跃业务情况
 */
@StatisticsTask
@Slf4j
@Service
public class AppStatisticsTask extends BaseStatisticsTask {

    private final ExecuteMetricsClient executeMetricsClient;
    private final StatisticConfig statisticConfig;

    protected AppStatisticsTask(BasicServiceManager basicServiceManager, StatisticsDAO statisticsDAO,
                                DSLContext dslContext, ExecuteMetricsClient executeMetricsClient,
                                StatisticConfig statisticConfig) {
        super(basicServiceManager, statisticsDAO, dslContext);
        this.executeMetricsClient = executeMetricsClient;
        this.statisticConfig = statisticConfig;
    }

    public void calcJoinedApps(LocalDateTime dateTime) {
        val resp = executeMetricsClient.getJoinedAppIdList();
        if (resp == null || !resp.isSuccess()) {
            log.warn("Fail to call remote getJoinedAppIdList, resp:{}", resp);
            return;
        }
        Set<Long> joinedAppIdSet = new HashSet<>(resp.getData());
        // 合并前一天的接入业务
        LocalDateTime lastDayTime = dateTime.minusDays(1);
        String lastDayTimeStr = getDayTimeStr(lastDayTime);
        StatisticsDTO lastDayStatisticsDTO = statisticsDAO.getStatistics(StatisticsConstants.DEFAULT_APP_ID,
            StatisticsConstants.RESOURCE_APP, StatisticsConstants.DIMENSION_APP_STATISTIC_TYPE,
            StatisticsConstants.DIMENSION_VALUE_APP_STATISTIC_TYPE_APP_LIST, lastDayTimeStr);
        if (lastDayStatisticsDTO != null) {
            String value = lastDayStatisticsDTO.getValue();
            try {
                List<SimpleAppInfoDTO> simpleAppInfoDTOList = JsonUtils.fromJson(value,
                    new TypeReference<List<SimpleAppInfoDTO>>() {
                });
                for (SimpleAppInfoDTO appInfoDTO : simpleAppInfoDTOList) {
                    joinedAppIdSet.add(appInfoDTO.getId());
                }
            } catch (Throwable t) {
                log.warn("Fail to parse app from statistics value {}", value, t);
            }
        } else {
            log.warn("lastDayStatisticsDTO of {} is null", lastDayTime);
        }
        List<SimpleAppInfoDTO> appInfoDTOList =
            basicServiceManager.getAppService().getSimpleAppInfoByIds(new ArrayList<>(joinedAppIdSet));
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(StatisticsConstants.DEFAULT_APP_ID);
        statisticsDTO.setDate(getDayTimeStr(dateTime));
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_APP);
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_APP_STATISTIC_TYPE);
        statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_APP_STATISTIC_TYPE_APP_LIST);
        statisticsDTO.setValue(JsonUtils.toJson(appInfoDTOList));
        statisticsDAO.upsertStatistics(dslContext, statisticsDTO);
    }

    public void calcActiveApps(LocalDateTime dateTime) {
        String dayTimeStr = getDayTimeStr(dateTime);
        List<ServiceApplicationDTO> appDTOList = getTargetApps();
        if (appDTOList == null) {
            log.error("Fail to getTargetApps of {}, ignore statistics", dayTimeStr);
            return;
        }
        LocalDateTime fromTime = dateTime.minusDays(statisticConfig.getAppActiveDays());
        List<Long> activeAppIdList = new ArrayList<>();
        for (ServiceApplicationDTO serviceApplicationDTO : appDTOList) {
            Long appId = serviceApplicationDTO.getId();
            val resp = executeMetricsClient.hasExecuteHistory(appId, -1L, TimeUtil.localDateTime2Long(fromTime),
                TimeUtil.localDateTime2Long(dateTime));
            if (resp == null || !resp.isSuccess()) {
                log.warn("Fail to call remote hasExecuteHistory, resp:{}", resp);
            } else {
                if (resp.getData()) {
                    activeAppIdList.add(appId);
                }
            }
        }
        List<SimpleAppInfoDTO> appInfoDTOList =
            basicServiceManager.getAppService().getSimpleAppInfoByIds(activeAppIdList);
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(-1L);
        statisticsDTO.setDate(dayTimeStr);
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_APP);
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_APP_STATISTIC_TYPE);
        statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_APP_STATISTIC_TYPE_ACTIVE_APP_LIST);
        statisticsDTO.setValue(JsonUtils.toJson(appInfoDTOList));
        statisticsDAO.upsertStatistics(dslContext, statisticsDTO);
    }

    @Override
    public void genStatisticsByDay(LocalDateTime dateTime) {
        calcJoinedApps(dateTime);
        calcActiveApps(dateTime);
    }
}
