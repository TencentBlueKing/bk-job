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
import com.tencent.bk.job.analysis.consts.TotalMetricEnum;
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
import java.util.List;

/**
 * 统计执行方案总数
 */
@StatisticsTask
@Slf4j
@Service
public class TaskPlanPerAppStatisticsTask extends BasePerAppStatisticsTask {

    private final ManageMetricsClient manageMetricsClient;

    protected TaskPlanPerAppStatisticsTask(BasicServiceManager basicServiceManager, StatisticsDAO statisticsDAO,
                                           DSLContext dslContext, ManageMetricsClient manageMetricsClient) {
        super(basicServiceManager, statisticsDAO, dslContext);
        this.manageMetricsClient = manageMetricsClient;
    }

    private StatisticsDTO genTaskPlanTotalStatisticsDTO(String dateStr, Long appId, String value) {
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(appId);
        statisticsDTO.setDate(dateStr);
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_GLOBAL);
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_GLOBAL_STATISTIC_TYPE);
        statisticsDTO.setDimensionValue(
            StatisticsConstants.DIMENSION_VALUE_GLOBAL_STATISTIC_TYPE_PREFIX
            + TotalMetricEnum.TASK_PLAN_COUNT.name()
        );
        statisticsDTO.setValue(value);
        return statisticsDTO;
    }

    public List<StatisticsDTO> calcAppTaskPlanTotalStatistics(String dateStr, Long appId) {
        List<StatisticsDTO> statisticsDTOList = new ArrayList<>();
        InternalResponse<Integer> resp = manageMetricsClient.countTaskPlans(appId);
        if (resp == null || !resp.isSuccess()) {
            log.warn("Fail to call remote countTaskPlans, resp:{}", resp);
            return statisticsDTOList;
        }
        Integer taskPlanCount = resp.getData();
        statisticsDTOList.add(genTaskPlanTotalStatisticsDTO(dateStr, appId, taskPlanCount.toString()));
        return statisticsDTOList;
    }

    @Override
    public List<StatisticsDTO> getStatisticsFrom(ServiceApplicationDTO app, Long fromTime, Long toTime,
                                                 String timeTag) {
        // 统计总数
        return calcAppTaskPlanTotalStatistics(timeTag, app.getId());
    }
}
