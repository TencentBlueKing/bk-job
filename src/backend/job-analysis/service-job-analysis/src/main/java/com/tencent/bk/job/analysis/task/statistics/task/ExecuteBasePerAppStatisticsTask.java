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

package com.tencent.bk.job.analysis.task.statistics.task;

import com.tencent.bk.job.analysis.api.dto.StatisticsDTO;
import com.tencent.bk.job.analysis.dao.CurrentTenantStatisticsDAO;
import com.tencent.bk.job.analysis.dao.NoTenantStatisticsDAO;
import com.tencent.bk.job.analysis.service.BasicServiceManager;
import com.tencent.bk.job.common.tenant.TenantService;
import com.tencent.bk.job.execute.api.inner.ServiceMetricsResource;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;

@Slf4j
public abstract class ExecuteBasePerAppStatisticsTask extends BasePerAppStatisticsTask {

    private final ServiceMetricsResource executeMetricsResource;

    protected ExecuteBasePerAppStatisticsTask(ServiceMetricsResource executeMetricsResource,
                                              BasicServiceManager basicServiceManager,
                                              CurrentTenantStatisticsDAO currentTenantStatisticsDAO,
                                              NoTenantStatisticsDAO noTenantStatisticsDAO,
                                              @Qualifier("job-analysis-dsl-context") DSLContext dslContext,
                                              TenantService tenantService) {
        super(basicServiceManager, currentTenantStatisticsDAO, noTenantStatisticsDAO, dslContext, tenantService);
        this.executeMetricsResource = executeMetricsResource;
    }

    public void addExecuteStatisticsDTO(StatisticsDTO searchStatisticsDTO, List<StatisticsDTO> statisticsDTOList) {
        StatisticsDTO remoteStatisticsDTO = executeMetricsResource.getStatistics(
            searchStatisticsDTO.getAppId(),
            searchStatisticsDTO.getResource(),
            searchStatisticsDTO.getDimension(),
            searchStatisticsDTO.getDimensionValue(),
            searchStatisticsDTO.getDate()
        ).getData();
        if (remoteStatisticsDTO != null) {
            remoteStatisticsDTO.setTenantId(getCurrentTenantId());
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
