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

package com.tencent.bk.job.execute.statistics;

import com.tencent.bk.job.common.statistics.consts.StatisticsConstants;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.execute.config.StatisticConfig;
import com.tencent.bk.job.execute.dao.StatisticsDAO;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
public class ClearExpiredStatisticsTask extends Thread {
    private final StatisticsDAO statisticsDAO;
    private final StatisticConfig statisticConfig;

    public ClearExpiredStatisticsTask(StatisticsDAO statisticsDAO, StatisticConfig statisticConfig) {
        this.statisticsDAO = statisticsDAO;
        this.statisticConfig = statisticConfig;
    }

    @Override
    public void run() {
        if (!statisticConfig.getEnableExpire()) {
            log.info("ClearExpiredStatisticsTask not enabled, you can enable it by set job.execute.statistics.expire" +
                ".enable=true in config file and restart process");
            return;
        }
        log.info("ClearExpiredStatisticsTask start");
        LocalDateTime now = LocalDateTime.now();
        try {
            LocalDateTime targetDate = now.minusDays(statisticConfig.getExpireDays());
            statisticsDAO.deleteStatisticsByDate(TimeUtil.getTimeStr(targetDate, StatisticsConstants.DATE_PATTERN));
        } catch (Throwable t) {
            log.error("Error occurs during ClearExpiredStatisticsTask", t);
        }
        log.info("ClearExpiredStatisticsTask end");
    }
}
