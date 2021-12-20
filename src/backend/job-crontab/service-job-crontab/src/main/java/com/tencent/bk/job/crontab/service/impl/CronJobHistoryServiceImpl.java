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

package com.tencent.bk.job.crontab.service.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.crontab.constant.ExecuteStatusEnum;
import com.tencent.bk.job.crontab.dao.CronJobHistoryDAO;
import com.tencent.bk.job.crontab.model.dto.CronJobHistoryDTO;
import com.tencent.bk.job.crontab.model.dto.CronJobLaunchResultStatistics;
import com.tencent.bk.job.crontab.service.CronJobHistoryService;
import com.tencent.bk.job.crontab.timer.AbstractQuartzTaskHandler;
import com.tencent.bk.job.crontab.timer.QuartzJob;
import com.tencent.bk.job.crontab.timer.QuartzJobBuilder;
import com.tencent.bk.job.crontab.timer.QuartzTrigger;
import com.tencent.bk.job.crontab.timer.QuartzTriggerBuilder;
import com.tencent.bk.job.crontab.timer.executor.CronHistoryCleanJobExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.quartz.CronExpression;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.PostConstruct;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 17/2/2020 00:12
 */
@Slf4j
@Component
public class CronJobHistoryServiceImpl implements CronJobHistoryService {

    private final CronJobHistoryDAO cronJobHistoryDAO;

    private final AbstractQuartzTaskHandler quartzTaskHandler;

    @Value("${job.crontab.history.clean.expression:0 25 3 * * ?}")
    private String cleanJobCronExp;

    @Autowired
    public CronJobHistoryServiceImpl(CronJobHistoryDAO cronJobHistoryDAO, AbstractQuartzTaskHandler quartzTaskHandler) {
        this.cronJobHistoryDAO = cronJobHistoryDAO;
        this.quartzTaskHandler = quartzTaskHandler;
    }

    @PostConstruct
    private void addCleanJob() {
        try {
            new CronExpression(cleanJobCronExp);
        } catch (ParseException e) {
            log.warn("Error while adding cron history clean job! Invalid expression|{}", cleanJobCronExp, e);
            return;
        }

        String cleanJobKey = "cron_history_clean";
        String systemId = "job-crontab";
        QuartzTrigger trigger = QuartzTriggerBuilder.newTrigger().ofType(QuartzTrigger.TriggerType.CRON)
            .withIdentity(cleanJobKey, systemId).withDescription("Auto clean cron job execute history table")
            .withCronExpression(cleanJobCronExp)
            .withMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT).build();

        if (trigger == null) {
            return;
        }

        QuartzJob job = QuartzJobBuilder.newJob().withIdentity(cleanJobKey, systemId)
            .forJob(CronHistoryCleanJobExecutor.class).withTrigger(trigger)
            .build();

        try {
            if (quartzTaskHandler.checkExists(job.getKey())) {
                quartzTaskHandler.deleteJob(job.getKey());
            }
            quartzTaskHandler.addJob(job);
        } catch (SchedulerException e) {
            log.error("Error while add job to quartz!", e);
            throw new InternalException("Add to quartz failed!", e, ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public long insertHistory(long appId, long cronJobId, long scheduledFireTime) {
        return cronJobHistoryDAO.insertCronJobHistory(appId, cronJobId, scheduledFireTime);
    }

    @Override
    public CronJobHistoryDTO getHistoryByIdAndTime(long appId, long cronJobId, long scheduledFireTime) {
        return cronJobHistoryDAO.getCronJobHistory(appId, cronJobId, scheduledFireTime);
    }

    @Override
    public boolean updateStatusByIdAndTime(long appId, long cronJobId, long scheduledFireTime,
                                           ExecuteStatusEnum status) {
        return cronJobHistoryDAO.updateStatusByIdAndTime(appId, cronJobId, scheduledFireTime, status.getValue());
    }

    @Override
    public boolean fillErrorInfo(long historyId, Long errorCode, String errorMsg) {
        if (errorCode == null) {
            return false;
        }
        return cronJobHistoryDAO.fillErrorInfo(historyId, errorCode, errorMsg);
    }

    @Override
    public boolean fillExecutor(long historyId, String executor) {
        if (StringUtils.isNoneBlank(executor)) {
            return cronJobHistoryDAO.fillExecutor(historyId, executor);
        }
        return false;
    }

    @Override
    public PageData<CronJobHistoryDTO> listPageHistoryByCondition(CronJobHistoryDTO historyCondition,
                                                                  BaseSearchCondition baseSearchCondition) {
        return cronJobHistoryDAO.listPageHistoryByCondition(historyCondition, baseSearchCondition);
    }

    @Override
    public Map<Long, CronJobLaunchResultStatistics> getCronTaskLaunchResultStatistics(Long appId,
                                                                                      List<Long> cronJobIdList) {
        if (cronJobIdList == null || cronJobIdList.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, CronJobLaunchResultStatistics> statisticsMap = new HashMap<>(cronJobIdList.size());
        StopWatch watch = new StopWatch("cron-job-launch-statistics");
        for (Long cronTaskId : cronJobIdList) {
            watch.start("get-last24h-history-" + cronTaskId);
            List<CronJobHistoryDTO> last24HourLaunchHistoryList = cronJobHistoryDAO.listLatestCronJobHistory(appId,
                cronTaskId, 86400L, null, null);

            boolean isGe10Within24Hour = false;
            // 已执行完成任务计数
            if (last24HourLaunchHistoryList.size() >= 10) {
                isGe10Within24Hour = true;
            }
            watch.stop();

            CronJobLaunchResultStatistics statistic = new CronJobLaunchResultStatistics();
            statistic.setCronTaskId(cronTaskId);

            // 24小时内执行超过10次，统计24小时内所有的数据
            if (isGe10Within24Hour) {
                statistic.setLast24HourExecuteRecords(last24HourLaunchHistoryList);
                statisticsMap.put(cronTaskId, statistic);
            } else {
                watch.start("get-last10-history-" + cronTaskId);
                // 如果24小时内执行次数少于10次，那么统计最近10次的数据。由于可能存在正在运行任务，所以默认返回最近11次的数据
                List<CronJobHistoryDTO> last10LaunchHistoryList = cronJobHistoryDAO.listLatestCronJobHistory(appId,
                    cronTaskId, null, null, 11);
                statistic.setLast10ExecuteRecords(last10LaunchHistoryList);
                statisticsMap.put(cronTaskId, statistic);
                watch.stop();
            }
        }
        if (watch.getTotalTimeMillis() > 1000) {
            log.warn("Get cron history statistics is slow, cost: {}", watch.prettyPrint());
        }
        return statisticsMap;
    }

    @Override
    public int cleanHistory(long cleanBefore, boolean cleanAll) {
        return cronJobHistoryDAO.cleanHistory(cleanBefore, cleanAll);
    }
}
