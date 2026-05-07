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

package com.tencent.bk.job.execute.statistics;

import com.tencent.bk.job.execute.dao.StatisticsDAO;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class StatisticsFlushThread extends Thread {

    private final StatisticsDAO statisticsDAO;
    private final LinkedBlockingQueue<Map<String, Map<StatisticsKey, AtomicInteger>>> flushQueue;
    private volatile boolean running = true;

    public StatisticsFlushThread(StatisticsDAO statisticsDAO,
                                 LinkedBlockingQueue<Map<String, Map<StatisticsKey, AtomicInteger>>> flushQueue) {
        this.statisticsDAO = statisticsDAO;
        this.flushQueue = flushQueue;
    }

    /**
     * 停止线程：先标记为停止，再中断阻塞的 take()，线程会自动 drain 队列中的剩余数据后退出
     */
    public void shutdown() {
        running = false;
        this.interrupt();
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
        while (running) {
            try {
                Map<String, Map<StatisticsKey, AtomicInteger>> incrementMap = flushQueue.take();
                flushIncrementMap(incrementMap);
            } catch (InterruptedException e) {
                if (!running) {
                    log.info("StatisticsFlushThread interrupted during shutdown, draining remaining data");
                    break;
                }
                log.warn("StatisticsFlushThread interrupted unexpectedly", e);
            } catch (Throwable t) {
                if (!running) {
                    log.info("StatisticsFlushThread stopping, ignore exception during shutdown");
                    break;
                }
                log.error("Fail to flush statistics into DB", t);
            }
        }
        drainAndFlush();
        log.info("StatisticsFlushThread stopped");
    }

    /**
     * 停机时将队列中剩余的统计数据全部 flush 到 DB，确保不丢数据
     */
    private void drainAndFlush() {
        List<Map<String, Map<StatisticsKey, AtomicInteger>>> remaining = new ArrayList<>();
        flushQueue.drainTo(remaining);
        if (remaining.isEmpty()) {
            return;
        }
        log.info("Draining {} remaining statistics batches to DB", remaining.size());
        for (Map<String, Map<StatisticsKey, AtomicInteger>> incrementMap : remaining) {
            try {
                flushIncrementMap(incrementMap);
            } catch (Throwable t) {
                log.warn("Fail to flush remaining statistics during shutdown", t);
            }
        }
        log.info("Drained all remaining statistics batches");
    }
}
