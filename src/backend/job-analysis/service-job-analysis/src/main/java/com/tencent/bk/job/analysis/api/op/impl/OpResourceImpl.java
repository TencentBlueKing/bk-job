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

package com.tencent.bk.job.analysis.api.op.impl;

import com.tencent.bk.job.analysis.api.op.OpResource;
import com.tencent.bk.job.analysis.config.listener.StatisticConfig;
import com.tencent.bk.job.analysis.dao.StatisticsDAO;
import com.tencent.bk.job.analysis.model.op.*;
import com.tencent.bk.job.analysis.task.statistics.StatisticsTaskScheduler;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.util.json.JsonUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OpResourceImpl implements OpResource {

    private final StatisticsTaskScheduler statisticsTaskScheduler;
    private final StatisticConfig statisticConfig;
    private final StatisticsDAO statisticsDAO;

    @Autowired
    public OpResourceImpl(StatisticsTaskScheduler statisticsTaskScheduler, StatisticConfig statisticConfig,
                          StatisticsDAO statisticsDAO) {
        this.statisticsTaskScheduler = statisticsTaskScheduler;
        this.statisticConfig = statisticConfig;
        this.statisticsDAO = statisticsDAO;
    }

    @Override
    public ServiceResponse<String> getStatisticsConfig(String username) {
        return ServiceResponse.buildSuccessResp(JsonUtils.toJson(statisticConfig));
    }

    @Override
    public ServiceResponse<Boolean> configStatistics(String username, ConfigStatisticsReq req) {
        if (req.getEnableExpire() != null) {
            statisticConfig.setEnableExpire(req.getEnableExpire());
        }
        if (req.getExpireDays() != null) {
            statisticConfig.setExpireDays(req.getExpireDays());
        }
        if (req.getIntervalHours() != null) {
            statisticConfig.setIntervalHours(req.getIntervalHours());
        }
        if (req.getEnable() != null) {
            statisticConfig.setEnable(req.getEnable());
        }
        if (req.getMomDays() != null) {
            statisticConfig.setMomDays(req.getMomDays());
        }
        if (req.getYoyDays() != null) {
            statisticConfig.setYoyDays(req.getYoyDays());
        }
        if (req.getMaxTagNum() != null) {
            statisticConfig.setMaxTagNum(req.getMaxTagNum());
        }
        return ServiceResponse.buildSuccessResp(true);
    }

    @Override
    public ServiceResponse<Integer> clearStatistics(String username, ClearStatisticsReq req) {
        List<String> dateStrList = req.getDateStrList();
        int totalCount = 0;
        if (dateStrList != null) {
            for (String s : dateStrList) {
                totalCount += statisticsDAO.deleteStatisticsByDate(s);
            }
            return ServiceResponse.buildSuccessResp(totalCount);
        } else {
            return ServiceResponse.buildSuccessResp(0);
        }
    }

    @Override
    public ServiceResponse<Boolean> configThreads(String username, ConfigThreadsReq req) {
        return ServiceResponse.buildSuccessResp(
            statisticsTaskScheduler.configThreads(
                req.getCurrentStatisticThreadsNum(),
                req.getPastStatisticThreadsNum()
            )
        );
    }

    @Override
    public ServiceResponse<List<String>> startTasks(String username, StartTasksReq req) {
        List<String> startedTaskNameList = statisticsTaskScheduler.startTasks(req.getStartDateStr(),
            req.getEndDateStr(), req.getTaskNameList());
        return ServiceResponse.buildSuccessResp(startedTaskNameList);
    }

    @Override
    public ServiceResponse<List<String>> cancelAllTasks(String username) {
        List<String> canceledTaskNameList = statisticsTaskScheduler.cancelAllTasks();
        return ServiceResponse.buildSuccessResp(canceledTaskNameList);
    }

    @Override
    public ServiceResponse<List<String>> cancelTasks(String username, CancelTasksReq req) {
        List<String> canceledTaskNameList = statisticsTaskScheduler.cancelTasks(req.getTaskNameList());
        return ServiceResponse.buildSuccessResp(canceledTaskNameList);
    }

    @Override
    public ServiceResponse<List<String>> taskList(String username) {
        return ServiceResponse.buildSuccessResp(statisticsTaskScheduler.listAllTasks());
    }

    @Override
    public ServiceResponse<List<Pair<String, Integer>>> arrangedTaskList(String username) {
        return ServiceResponse.buildSuccessResp(statisticsTaskScheduler.listArrangedTasks());
    }
}
