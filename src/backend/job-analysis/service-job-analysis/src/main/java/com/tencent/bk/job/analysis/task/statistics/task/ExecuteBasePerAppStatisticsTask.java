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

package com.tencent.bk.job.analysis.task.statistics.task;

import com.tencent.bk.job.analysis.client.ExecuteMetricsClient;
import com.tencent.bk.job.analysis.dao.StatisticsDAO;
import com.tencent.bk.job.analysis.service.BasicServiceManager;
import com.tencent.bk.job.common.statistics.model.dto.StatisticsDTO;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;

import java.util.List;

@Slf4j
public abstract class ExecuteBasePerAppStatisticsTask extends BasePerAppStatisticsTask {

    private final ExecuteMetricsClient executeMetricsClient;

    protected ExecuteBasePerAppStatisticsTask(ExecuteMetricsClient executeMetricsClient,
                                              BasicServiceManager basicServiceManager, StatisticsDAO statisticsDAO,
                                              DSLContext dslContext) {
        super(basicServiceManager, statisticsDAO, dslContext);
        this.executeMetricsClient = executeMetricsClient;
    }

    public void addExecuteStatisticsDTO(StatisticsDTO searchStatisticsDTO, List<StatisticsDTO> statisticsDTOList) {
        StatisticsDTO remoteStatisticsDTO = executeMetricsClient.getStatistics(searchStatisticsDTO.getAppId(),
            searchStatisticsDTO.getResource(), searchStatisticsDTO.getDimension(),
            searchStatisticsDTO.getDimensionValue(), searchStatisticsDTO.getDate()).getData();
        if (remoteStatisticsDTO != null) {
            statisticsDTOList.add(remoteStatisticsDTO);
        } else {
            remoteStatisticsDTO = searchStatisticsDTO.clone();
            remoteStatisticsDTO.setValue("0");
            remoteStatisticsDTO.setCreateTime(System.currentTimeMillis());
            remoteStatisticsDTO.setLastModifyTime(System.currentTimeMillis());
            statisticsDTOList.add(remoteStatisticsDTO);
            if (log.isDebugEnabled()) {
                log.debug("Cannot find statisticsDTO by appId={},resource={},dimension={},dimensionValue={},date={}",
                    searchStatisticsDTO.getAppId(), searchStatisticsDTO.getResource(),
                    searchStatisticsDTO.getDimension(), searchStatisticsDTO.getDimensionValue(),
                    searchStatisticsDTO.getDate());
            }
        }
    }
}
