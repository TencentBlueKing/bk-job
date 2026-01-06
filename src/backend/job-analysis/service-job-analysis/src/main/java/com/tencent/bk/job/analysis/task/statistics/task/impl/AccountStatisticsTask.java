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

package com.tencent.bk.job.analysis.task.statistics.task.impl;

import com.tencent.bk.job.analysis.api.consts.StatisticsConstants;
import com.tencent.bk.job.analysis.api.dto.StatisticsDTO;
import com.tencent.bk.job.analysis.dao.CurrentTenantStatisticsDAO;
import com.tencent.bk.job.analysis.dao.NoTenantStatisticsDAO;
import com.tencent.bk.job.analysis.service.BasicServiceManager;
import com.tencent.bk.job.analysis.task.statistics.anotation.StatisticsTask;
import com.tencent.bk.job.analysis.task.statistics.task.BaseStatisticsTask;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.tenant.TenantService;
import com.tencent.bk.job.manage.api.common.constants.account.AccountTypeEnum;
import com.tencent.bk.job.manage.api.inner.ServiceMetricsResource;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 统计账号分布情况
 */
@StatisticsTask
@Slf4j
@Service
public class AccountStatisticsTask extends BaseStatisticsTask {

    private final ServiceMetricsResource manageMetricResource;

    protected AccountStatisticsTask(BasicServiceManager basicServiceManager,
                                    CurrentTenantStatisticsDAO currentTenantStatisticsDAO,
                                    NoTenantStatisticsDAO noTenantStatisticsDAO,
                                    @Qualifier("job-analysis-dsl-context") DSLContext dslContext,
                                    ServiceMetricsResource manageMetricResource,
                                    TenantService tenantService) {
        super(basicServiceManager, currentTenantStatisticsDAO, noTenantStatisticsDAO, dslContext, tenantService);
        this.manageMetricResource = manageMetricResource;
    }

    private StatisticsDTO genStatisticsDTO(String dateStr, String value, String dimensionValue) {
        StatisticsDTO statisticsDTO = getBasicStatisticsDTO();
        statisticsDTO.setAppId(-1L);
        statisticsDTO.setDate(dateStr);
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_ACCOUNT_OF_ALL_APP);
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_ACCOUNT_TYPE);
        statisticsDTO.setDimensionValue(dimensionValue);
        statisticsDTO.setValue(value);
        return statisticsDTO;
    }

    private StatisticsDTO genLinuxStatisticsDTO(String dateStr, String value) {
        return genStatisticsDTO(dateStr, value, StatisticsConstants.DIMENSION_VALUE_ACCOUNT_TYPE_LINUX);
    }

    private StatisticsDTO genWindowsStatisticsDTO(String dateStr, String value) {
        return genStatisticsDTO(dateStr, value, StatisticsConstants.DIMENSION_VALUE_ACCOUNT_TYPE_WINDOWS);
    }

    private StatisticsDTO genDBStatisticsDTO(String dateStr, String value) {
        return genStatisticsDTO(dateStr, value, StatisticsConstants.DIMENSION_VALUE_ACCOUNT_TYPE_DB);
    }

    public void calcAndSaveAccountStatistics(String dateStr) {
        String tenantId = getCurrentTenantId();
        // Linux
        InternalResponse<Integer> resp = manageMetricResource.countAccounts(tenantId, AccountTypeEnum.LINUX);
        if (resp == null || !resp.isSuccess()) {
            log.warn("Fail to call remote countAccounts, resp:{}", resp);
            return;
        }
        Integer linuxCount = resp.getData();
        currentTenantStatisticsDAO.upsertStatistics(dslContext, genLinuxStatisticsDTO(dateStr, linuxCount.toString()));
        // Windows
        resp = manageMetricResource.countAccounts(tenantId, AccountTypeEnum.WINDOW);
        if (resp == null || !resp.isSuccess()) {
            log.warn("Fail to call remote countAccounts, resp:{}", resp);
            return;
        }
        Integer windowsCount = resp.getData();
        currentTenantStatisticsDAO.upsertStatistics(dslContext, genWindowsStatisticsDTO(dateStr,
            windowsCount.toString()));
        // DB
        resp = manageMetricResource.countAccounts(tenantId, AccountTypeEnum.MYSQL);
        if (resp == null || !resp.isSuccess()) {
            log.warn("Fail to call remote countAccounts, resp:{}", resp);
            return;
        }
        Integer mysqlCount = resp.getData();
        resp = manageMetricResource.countAccounts(tenantId, AccountTypeEnum.ORACLE);
        if (resp == null || !resp.isSuccess()) {
            log.warn("Fail to call remote countAccounts, resp:{}", resp);
            return;
        }
        Integer oracleCount = resp.getData();
        resp = manageMetricResource.countAccounts(tenantId, AccountTypeEnum.DB2);
        if (resp == null || !resp.isSuccess()) {
            log.warn("Fail to call remote countAccounts, resp:{}", resp);
            return;
        }
        Integer db2Count = resp.getData();
        int dbCount = mysqlCount + oracleCount + db2Count;
        currentTenantStatisticsDAO.upsertStatistics(
            dslContext,
            genDBStatisticsDTO(dateStr, Integer.toString(dbCount))
        );
    }

    @Override
    public void genStatisticsByDay(LocalDateTime dateTime) {
        calcAndSaveAccountStatistics(getDayTimeStr(dateTime));
    }
}
