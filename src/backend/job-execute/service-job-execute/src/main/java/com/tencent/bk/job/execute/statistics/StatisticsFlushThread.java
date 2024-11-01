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

import com.tencent.bk.job.execute.dao.StatisticsDAO;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class StatisticsFlushThread extends Thread {

    private final StatisticsDAO statisticsDAO;
    private volatile LinkedBlockingQueue<Map<String, Map<StatisticsKey, AtomicInteger>>> flushQueue;

    public StatisticsFlushThread(StatisticsDAO statisticsDAO,
                                 LinkedBlockingQueue<Map<String, Map<StatisticsKey, AtomicInteger>>> flushQueue) {
        this.statisticsDAO = statisticsDAO;
        this.flushQueue = flushQueue;
    }

    private void flushIncrementMap(Map<String, Map<StatisticsKey, AtomicInteger>> incrementMap) {
        incrementMap.forEach((dateStr, metricsMap) -> {
            metricsMap.forEach((statisticsKey, value) -> {
                final Integer incrementValue = value.get();
                int affectRows;
                do {
                    affectRows = statisticsDAO.increaseStatisticValue(dateStr, statisticsKey, incrementValue);
                } while (affectRows == 0);
            });
        });
    }

    @Override
    public void run() {
        while (true) {
            try {
                Map<String, Map<StatisticsKey, AtomicInteger>> incrementMap = flushQueue.take();
                flushIncrementMap(incrementMap);
            } catch (Throwable t) {
                log.error("Fail to flush statistics into DB", t);
            }
        }
    }
}
