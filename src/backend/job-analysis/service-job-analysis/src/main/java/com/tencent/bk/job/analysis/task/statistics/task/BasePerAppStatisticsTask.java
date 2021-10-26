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

import com.tencent.bk.job.analysis.dao.StatisticsDAO;
import com.tencent.bk.job.analysis.service.BasicServiceManager;
import com.tencent.bk.job.common.statistics.model.dto.StatisticsDTO;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.manage.model.inner.ServiceApplicationDTO;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public abstract class BasePerAppStatisticsTask extends BaseStatisticsTask {

    protected BasePerAppStatisticsTask(BasicServiceManager basicServiceManager, StatisticsDAO statisticsDAO,
                                       DSLContext dslContext) {
        super(basicServiceManager, statisticsDAO, dslContext);
    }

    public abstract List<StatisticsDTO> getStatisticsFrom(ServiceApplicationDTO app, Long fromTime, Long toTime,
                                                          String timeTag);

    public void afterAppDailyStatisticsUpdated(Long appId, LocalDateTime dateTime) {
        // 默认无任何行为，待子类重写
        // 可用于汇总单个业务在一段时间内的数据
    }

    public void afterDailyStatisticsUpdated(String dayTimeStr) {
        // 默认无任何行为，待子类重写
        // 可用于汇总多个业务的数据
    }

    @Override
    public void genStatisticsByDay(LocalDateTime dateTime) {
        String dayTimeStr = getDayTimeStr(dateTime);
        List<ServiceApplicationDTO> apps = getTargetApps();
        if (apps == null) {
            log.error("Fail to getTargetApps, ignore statistics of day {}", dayTimeStr);
            return;
        }
        log.debug("targetApps:{}",
            apps.parallelStream().map(ServiceApplicationDTO::getId).collect(Collectors.toList()));
        apps.forEach(app -> {
            try {
                StopWatch stopWatch = new StopWatch();
                //统计今天的数据
                LocalDateTime dayStartTime = TimeUtil.getDayStartTime(dateTime);
                LocalDateTime nextDayStartTime = TimeUtil.getDayStartTime(dateTime.plusDays(1));
                stopWatch.start(String.format("statisticTask for app %d of %s", app.getId(), dayTimeStr));
                List<StatisticsDTO> statisticsDTOList = getStatisticsFrom(app,
                    TimeUtil.localDateTime2Long(dayStartTime), TimeUtil.localDateTime2Long(nextDayStartTime),
                    dayTimeStr);
                statisticsDTOList.forEach(statisticsDTO -> {
                    log.debug("upsert {}", statisticsDTO);
                    statisticsDAO.upsertStatistics(dslContext, statisticsDTO);
                });
                afterAppDailyStatisticsUpdated(app.getId(), dateTime);
                stopWatch.stop();
                if (stopWatch.getLastTaskTimeMillis() > 3000) {
                    log.info("Long statisticTask end, trigger a 3s protect wait, detail:{}", stopWatch.prettyPrint());
                    Thread.sleep(3000);
                }
            } catch (Throwable t) {
                log.warn("Fail to genStatisticsByDay, dateTime={}, app={}", dateTime, app, t);
            }
        });
        afterDailyStatisticsUpdated(dayTimeStr);
    }
}
