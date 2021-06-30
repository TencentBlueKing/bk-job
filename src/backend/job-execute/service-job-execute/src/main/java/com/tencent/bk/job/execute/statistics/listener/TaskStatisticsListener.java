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

package com.tencent.bk.job.execute.statistics.listener;

import com.tencent.bk.job.execute.statistics.StatisticsService;
import com.tencent.bk.job.execute.statistics.consts.StatisticsActionEnum;
import com.tencent.bk.job.execute.statistics.message.TaskStatisticsProcessor;
import com.tencent.bk.job.execute.statistics.model.TaskStatisticsCmd;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.stereotype.Component;

@Component
@EnableBinding({TaskStatisticsProcessor.class})
@Slf4j
public class TaskStatisticsListener {

    private final StatisticsService statisticsService;

    @Autowired
    public TaskStatisticsListener(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /**
     * 处理作业统计指令
     */
    @StreamListener(TaskStatisticsProcessor.INPUT)
    public void handleMessage(TaskStatisticsCmd taskStatisticsCmd) {
        log.info("Receive task statistic message, taskInstanceId={}, action={}, msgSendTime={}",
            taskStatisticsCmd.getTaskInstanceId(), taskStatisticsCmd.getAction(), taskStatisticsCmd.getTime());
        String action = taskStatisticsCmd.getAction();
        long taskInstanceId = taskStatisticsCmd.getTaskInstanceId();
        try {
            if (StatisticsActionEnum.START_JOB.name().equals(action)) {
                // 启动作业触发某些统计数据生成
                statisticsService.updateStartJobStatistics(taskInstanceId);
            } else if (StatisticsActionEnum.END_JOB.name().equals(action)) {
                // 作业完成触发某些统计数据生成
                statisticsService.updateEndJobStatistics(taskInstanceId);
            } else {
                log.warn("action {} not support yet", action);
            }
        } catch (Throwable t) {
            log.error("Error occured when computing statistics", t);
        }
    }
}
