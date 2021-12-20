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

import com.tencent.bk.job.analysis.client.ManageMetricsClient;
import com.tencent.bk.job.analysis.dao.StatisticsDAO;
import com.tencent.bk.job.analysis.service.BasicServiceManager;
import com.tencent.bk.job.analysis.task.statistics.anotation.StatisticsTask;
import com.tencent.bk.job.analysis.task.statistics.task.BasePerAppStatisticsTask;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.statistics.consts.StatisticsConstants;
import com.tencent.bk.job.common.statistics.model.dto.StatisticsDTO;
import com.tencent.bk.job.manage.model.inner.ServiceApplicationDTO;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 统计脚本引用情况
 */
@StatisticsTask
@Slf4j
@Service
public class ScriptCiteInfoPerAppStatisticsTask extends BasePerAppStatisticsTask {

    private final ManageMetricsClient manageMetricsClient;

    protected ScriptCiteInfoPerAppStatisticsTask(BasicServiceManager basicServiceManager, StatisticsDAO statisticsDAO
        , DSLContext dslContext, ManageMetricsClient manageMetricsClient) {
        super(basicServiceManager, statisticsDAO, dslContext);
        this.manageMetricsClient = manageMetricsClient;
    }

    private StatisticsDTO genScriptCountStatisticsDTO(String dateStr, Long appId, String value) {
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(appId);
        statisticsDTO.setDate(dateStr);
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_SCRIPT_CITE_INFO);
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_SCRIPT_CITE_INFO_METRIC);
        statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_SCRIPT_CITE_INFO_METRIC_SCRIPT_COUNT);
        statisticsDTO.setValue(value);
        return statisticsDTO;
    }

    private StatisticsDTO genCitedScriptCountDTO(String dateStr, Long appId, String value) {
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(appId);
        statisticsDTO.setDate(dateStr);
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_SCRIPT_CITE_INFO);
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_SCRIPT_CITE_INFO_METRIC);
        statisticsDTO.setDimensionValue(
            StatisticsConstants.DIMENSION_VALUE_SCRIPT_CITE_INFO_METRIC_CITED_SCRIPT_COUNT
        );
        statisticsDTO.setValue(value);
        return statisticsDTO;
    }

    private StatisticsDTO genCitedScriptStepCountDTO(String dateStr, Long appId, String value) {
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(appId);
        statisticsDTO.setDate(dateStr);
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_SCRIPT_CITE_INFO);
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_SCRIPT_CITE_INFO_METRIC);
        statisticsDTO.setDimensionValue(
            StatisticsConstants.DIMENSION_VALUE_SCRIPT_CITE_INFO_METRIC_CITED_SCRIPT_STEP_COUNT
        );
        statisticsDTO.setValue(value);
        return statisticsDTO;
    }


    public List<StatisticsDTO> calcAppScriptCiteInfo(String dateStr, Long appId) {
        List<StatisticsDTO> statisticsDTOList = new ArrayList<>();
        // 1.统计脚本总数
        InternalResponse<Integer> resp = manageMetricsClient.countScripts(
            appId,
            null,
            null
        );
        if (resp == null || !resp.isSuccess()) {
            log.warn("Fail to call remote countScripts, resp:{}", resp);
            return statisticsDTOList;
        }
        Integer scriptCount = resp.getData();
        statisticsDTOList.add(genScriptCountStatisticsDTO(dateStr, appId, scriptCount.toString()));
        // 2.统计被引用的脚本总数
        resp = manageMetricsClient.countCiteScripts(appId);
        if (resp == null || !resp.isSuccess()) {
            log.warn("Fail to call remote countCiteScripts, resp:{}", resp);
            return statisticsDTOList;
        }
        Integer citedScriptCount = resp.getData();
        statisticsDTOList.add(genCitedScriptCountDTO(dateStr, appId, citedScriptCount.toString()));
        // 3.统计引用脚本的步骤总数
        resp = manageMetricsClient.countCiteScriptSteps(appId);
        if (resp == null || !resp.isSuccess()) {
            log.warn("Fail to call remote countCiteScriptSteps, resp:{}", resp);
            return statisticsDTOList;
        }
        Integer citedScriptStepCount = resp.getData();
        statisticsDTOList.add(genCitedScriptStepCountDTO(dateStr, appId, citedScriptStepCount.toString()));
        return statisticsDTOList;
    }

    @Override
    public void afterDailyStatisticsUpdated(String dayTimeStr) {
        // 1.统计脚本总数
        List<StatisticsDTO> statisticsDTOList = statisticsDAO.getStatisticsList(
            null,
            Collections.singletonList(StatisticsConstants.DEFAULT_APP_ID),
            StatisticsConstants.RESOURCE_SCRIPT_CITE_INFO,
            StatisticsConstants.DIMENSION_SCRIPT_CITE_INFO_METRIC,
            StatisticsConstants.DIMENSION_VALUE_SCRIPT_CITE_INFO_METRIC_SCRIPT_COUNT,
            dayTimeStr);
        long totalValue = 0L;
        for (StatisticsDTO dto : statisticsDTOList) {
            totalValue += Long.parseLong(dto.getValue());
        }
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(StatisticsConstants.DEFAULT_APP_ID);
        statisticsDTO.setCreateTime(System.currentTimeMillis());
        statisticsDTO.setDate(dayTimeStr);
        statisticsDTO.setValue(Long.toString(totalValue));
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_GLOBAL);
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_GLOBAL_STATISTIC_TYPE);
        statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_SCRIPT_CITE_INFO_METRIC_SCRIPT_COUNT);
        statisticsDAO.upsertStatistics(dslContext, statisticsDTO);
        // 2.统计被引用的脚本总数
        statisticsDTOList = statisticsDAO.getStatisticsList(
            null,
            Collections.singletonList(StatisticsConstants.DEFAULT_APP_ID),
            StatisticsConstants.RESOURCE_SCRIPT_CITE_INFO,
            StatisticsConstants.DIMENSION_SCRIPT_CITE_INFO_METRIC,
            StatisticsConstants.DIMENSION_VALUE_SCRIPT_CITE_INFO_METRIC_CITED_SCRIPT_COUNT,
            dayTimeStr);
        totalValue = 0L;
        for (StatisticsDTO dto : statisticsDTOList) {
            totalValue += Long.parseLong(dto.getValue());
        }
        statisticsDTO.setCreateTime(System.currentTimeMillis());
        statisticsDTO.setValue(Long.toString(totalValue));
        statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_SCRIPT_CITE_INFO_METRIC_CITED_SCRIPT_COUNT);
        statisticsDAO.upsertStatistics(dslContext, statisticsDTO);
        // 3.统计引用脚本的步骤总数
        statisticsDTOList = statisticsDAO.getStatisticsList(
            null,
            Collections.singletonList(StatisticsConstants.DEFAULT_APP_ID),
            StatisticsConstants.RESOURCE_SCRIPT_CITE_INFO,
            StatisticsConstants.DIMENSION_SCRIPT_CITE_INFO_METRIC,
            StatisticsConstants.DIMENSION_VALUE_SCRIPT_CITE_INFO_METRIC_CITED_SCRIPT_STEP_COUNT,
            dayTimeStr);
        totalValue = 0L;
        for (StatisticsDTO dto : statisticsDTOList) {
            totalValue += Long.parseLong(dto.getValue());
        }
        statisticsDTO.setCreateTime(System.currentTimeMillis());
        statisticsDTO.setValue(Long.toString(totalValue));
        statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_SCRIPT_CITE_INFO_METRIC_CITED_SCRIPT_STEP_COUNT);
        statisticsDAO.upsertStatistics(dslContext, statisticsDTO);
    }

    @Override
    public List<StatisticsDTO> getStatisticsFrom(ServiceApplicationDTO app, Long fromTime, Long toTime,
                                                 String timeTag) {
        return new ArrayList<>(calcAppScriptCiteInfo(timeTag, app.getId()));
    }
}
