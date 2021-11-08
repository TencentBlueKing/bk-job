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

package com.tencent.bk.job.analysis.task.statistics.task.impl;

import com.tencent.bk.job.analysis.client.ManageMetricsClient;
import com.tencent.bk.job.analysis.dao.StatisticsDAO;
import com.tencent.bk.job.analysis.service.BasicServiceManager;
import com.tencent.bk.job.analysis.task.statistics.anotation.StatisticsTask;
import com.tencent.bk.job.analysis.task.statistics.task.BaseStatisticsTask;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.statistics.consts.StatisticsConstants;
import com.tencent.bk.job.common.statistics.model.dto.StatisticsDTO;
import com.tencent.bk.job.manage.common.consts.account.AccountTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 统计账号分布情况
 */
@StatisticsTask
@Slf4j
@Service
public class AccountStatisticsTask extends BaseStatisticsTask {

    private final ManageMetricsClient manageMetricsClient;

    protected AccountStatisticsTask(BasicServiceManager basicServiceManager, StatisticsDAO statisticsDAO,
                                    DSLContext dslContext, ManageMetricsClient manageMetricsClient) {
        super(basicServiceManager, statisticsDAO, dslContext);
        this.manageMetricsClient = manageMetricsClient;
    }

    private StatisticsDTO genStatisticsDTO(String dateStr, String value, String dimensionValue) {
        StatisticsDTO statisticsDTO = new StatisticsDTO();
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
        // Linux
        InternalResponse<Integer> resp = manageMetricsClient.countAccounts(AccountTypeEnum.LINUX);
        if (resp == null || !resp.isSuccess()) {
            log.warn("Fail to call remote countAccounts, resp:{}", resp);
            return;
        }
        Integer linuxCount = resp.getData();
        statisticsDAO.upsertStatistics(dslContext, genLinuxStatisticsDTO(dateStr, linuxCount.toString()));
        // Windows
        resp = manageMetricsClient.countAccounts(AccountTypeEnum.WINDOW);
        if (resp == null || !resp.isSuccess()) {
            log.warn("Fail to call remote countAccounts, resp:{}", resp);
            return;
        }
        Integer windowsCount = resp.getData();
        statisticsDAO.upsertStatistics(dslContext, genWindowsStatisticsDTO(dateStr, windowsCount.toString()));
        // DB
        resp = manageMetricsClient.countAccounts(AccountTypeEnum.MYSQL);
        if (resp == null || !resp.isSuccess()) {
            log.warn("Fail to call remote countAccounts, resp:{}", resp);
            return;
        }
        Integer mysqlCount = resp.getData();
        resp = manageMetricsClient.countAccounts(AccountTypeEnum.ORACLE);
        if (resp == null || !resp.isSuccess()) {
            log.warn("Fail to call remote countAccounts, resp:{}", resp);
            return;
        }
        Integer oracleCount = resp.getData();
        resp = manageMetricsClient.countAccounts(AccountTypeEnum.DB2);
        if (resp == null || !resp.isSuccess()) {
            log.warn("Fail to call remote countAccounts, resp:{}", resp);
            return;
        }
        Integer db2Count = resp.getData();
        Integer dbCount = mysqlCount + oracleCount + db2Count;
        statisticsDAO.upsertStatistics(dslContext, genDBStatisticsDTO(dateStr, dbCount.toString()));
    }

    @Override
    public void genStatisticsByDay(LocalDateTime dateTime) {
        calcAndSaveAccountStatistics(getDayTimeStr(dateTime));
    }
}
