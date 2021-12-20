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

import com.tencent.bk.job.analysis.client.CronMetricsResourceClient;
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
 * 统计定时任务分布情况
 */
@StatisticsTask
@Slf4j
@Service
public class CronPerAppStatisticsTask extends BasePerAppStatisticsTask {

    private final CronMetricsResourceClient cronMetricsResourceClient;

    protected CronPerAppStatisticsTask(BasicServiceManager applicationResourceClient, StatisticsDAO statisticsDAO,
                                       DSLContext dslContext, CronMetricsResourceClient cronMetricsResourceClient) {
        super(applicationResourceClient, statisticsDAO, dslContext);
        this.cronMetricsResourceClient = cronMetricsResourceClient;
    }

    private StatisticsDTO genCronStatusStatisticsDTO(String dateStr, Long appId, String value, String dimensionValue) {
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(appId);
        statisticsDTO.setDate(dateStr);
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_CRON);
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_CRON_STATUS);
        statisticsDTO.setDimensionValue(dimensionValue);
        statisticsDTO.setValue(value);
        return statisticsDTO;
    }

    private StatisticsDTO genCronTypeStatisticsDTO(String dateStr, Long appId, String value, String dimensionValue) {
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(appId);
        statisticsDTO.setDate(dateStr);
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_CRON);
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_CRON_TYPE);
        statisticsDTO.setDimensionValue(dimensionValue);
        statisticsDTO.setValue(value);
        return statisticsDTO;
    }

    private StatisticsDTO genActiveCronStatisticsDTO(String dateStr, Long appId, String value) {
        return genCronStatusStatisticsDTO(dateStr, appId, value, StatisticsConstants.DIMENSION_VALUE_CRON_STATUS_OPEN);
    }

    private StatisticsDTO genInActiveCronStatisticsDTO(String dateStr, Long appId, String value) {
        return genCronStatusStatisticsDTO(dateStr, appId, value,
            StatisticsConstants.DIMENSION_VALUE_CRON_STATUS_CLOSED);
    }

    private StatisticsDTO genSimpleCronStatisticsDTO(String dateStr, Long appId, String value) {
        return genCronTypeStatisticsDTO(dateStr, appId, value, StatisticsConstants.DIMENSION_VALUE_CRON_TYPE_SIMPLE);
    }

    private StatisticsDTO genCronCronStatisticsDTO(String dateStr, Long appId, String value) {
        return genCronTypeStatisticsDTO(dateStr, appId, value, StatisticsConstants.DIMENSION_VALUE_CRON_TYPE_CRON);
    }

    public List<StatisticsDTO> calcAppCronStatistics(String dateStr, Long appId) {
        List<StatisticsDTO> statisticsDTOList = new ArrayList<>();
        // 开启的
        InternalResponse<Integer> resp = cronMetricsResourceClient.countCronJob(appId, true, null);
        if (resp == null || !resp.isSuccess()) {
            log.warn("Fail to call remote countCronJob, resp:{}", resp);
            return Collections.emptyList();
        }
        Integer activeCronCount = resp.getData();
        statisticsDTOList.add(genActiveCronStatisticsDTO(dateStr, appId, activeCronCount.toString()));
        // 关闭的
        resp = cronMetricsResourceClient.countCronJob(appId, false, null);
        if (resp == null || !resp.isSuccess()) {
            log.warn("Fail to call remote countCronJob, resp:{}", resp);
            return statisticsDTOList;
        }
        Integer inActiveCronCount = resp.getData();
        statisticsDTOList.add(genInActiveCronStatisticsDTO(dateStr, appId, inActiveCronCount.toString()));
        // 简单的
        resp = cronMetricsResourceClient.countCronJob(appId, null, false);
        if (resp == null || !resp.isSuccess()) {
            log.warn("Fail to call remote countCronJob, resp:{}", resp);
            return statisticsDTOList;
        }
        Integer simpleCronCount = resp.getData();
        statisticsDTOList.add(genSimpleCronStatisticsDTO(dateStr, appId, simpleCronCount.toString()));
        // 周期的
        resp = cronMetricsResourceClient.countCronJob(appId, null, true);
        if (resp == null || !resp.isSuccess()) {
            log.warn("Fail to call remote countCronJob, resp:{}", resp);
            return statisticsDTOList;
        }
        Integer cronCronCount = resp.getData();
        statisticsDTOList.add(genCronCronStatisticsDTO(dateStr, appId, cronCronCount.toString()));
        return statisticsDTOList;
    }

    @Override
    public List<StatisticsDTO> getStatisticsFrom(ServiceApplicationDTO app, Long fromTime, Long toTime,
                                                 String timeTag) {
        return calcAppCronStatistics(timeTag, app.getId());
    }
}
