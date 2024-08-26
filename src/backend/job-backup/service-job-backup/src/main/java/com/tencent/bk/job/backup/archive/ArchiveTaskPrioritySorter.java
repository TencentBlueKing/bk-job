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

package com.tencent.bk.job.backup.archive;

import com.tencent.bk.job.backup.archive.model.JobInstanceArchiveTaskInfo;
import org.apache.commons.collections4.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ArchiveTaskPrioritySorter {

    public static void sort(List<JobInstanceArchiveTaskInfo> runningTasks,
                            Integer dbNodeCount,
                            List<JobInstanceArchiveTaskInfo> candidateTasks) {
        if (CollectionUtils.isEmpty(candidateTasks)) {
            return;
        }
        List<Integer> priorityDbIndexList =
            computeDbPriorityByRunningTasks(runningTasks, dbNodeCount);
        sortArchiveTask(candidateTasks, priorityDbIndexList);
    }

    private static void sortArchiveTask(List<JobInstanceArchiveTaskInfo> candidateTasks,
                                        List<Integer> priorityDbIndexList) {
        candidateTasks.sort((task1, task2) -> {
            int task1DbPriority = priorityDbIndexList.indexOf(task1.getDbDataNode().getDbIndex());
            int task2DbPriority = priorityDbIndexList.indexOf(task2.getDbDataNode().getDbIndex());
            if (task1DbPriority < task2DbPriority) {
                return -1;
            } else if (task1DbPriority > task2DbPriority) {
                return 1;
            } else {
                int day1 = task1.getDay();
                int day2 = task2.getDay();
                if (day1 < day2) {
                    return -1;
                } else if (day1 > day2) {
                    return 1;
                } else {
                    int hour1 = task1.getHour();
                    int hour2 = task2.getHour();
                    return Integer.compare(hour1, hour2);
                }
            }
        });
    }

    /**
     * 根据正在运行中的任务在 DB 实例的分布情况，计算下一个归档任务对应 DB 的优先级
     *
     * @param runningTasks 正在运行中的归档任务
     * @param dbNodeCount  归档 DB 实例数量
     * @return 优先级队列。列表的值为 dbIndex, 按照优先级从高->低排序
     */
    private static List<Integer> computeDbPriorityByRunningTasks(List<JobInstanceArchiveTaskInfo> runningTasks,
                                                                 int dbNodeCount) {
        Map<Integer, Integer> dbTaskCountMap = computeDbTaskCount(runningTasks, dbNodeCount);

        return dbTaskCountMap.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }


    /**
     * 计算归档任务在 db 实例的分布情况
     *
     * @param runningTasks 正在运行中的归档任务
     * @param dbNodeCount  归档 DB 实例数量
     * @return 任务分布情况。 key-db序号;value-正在运行中的归档任务数量
     */
    private static Map<Integer, Integer> computeDbTaskCount(List<JobInstanceArchiveTaskInfo> runningTasks,
                                                            int dbNodeCount) {
        Map<Integer, Integer> dbTaskCountMap = new HashMap<>();
        if (CollectionUtils.isEmpty(runningTasks)) {
            return dbTaskCountMap;
        }

        runningTasks.forEach(task -> {
            Integer dbIndex = task.getDbDataNode().getDbIndex();
            dbTaskCountMap.compute(dbIndex, (k, v) -> {
                if (v == null) {
                    v = 1;
                } else {
                    v++;
                }
                return v;
            });
        });

        for (int dbIndex = 0; dbIndex < dbNodeCount; dbIndex++) {
            dbTaskCountMap.putIfAbsent(dbIndex, 0);
        }
        return dbTaskCountMap;
    }


}
