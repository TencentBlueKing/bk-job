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
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 归档任务调度,DB 节点优先级计算，保证归档任务均衡分布在每个 db 节点上
 */
public class ArchiveDbNodePriorityEvaluator {

    /**
     * 归档任务调度优先级计算
     *
     * @param runningTasks               正在执行中的归档任务
     * @param scheduleTaskCountGroupByDb 待调度归档任务计数（根据 db)
     * @return DbNodeTasksInfo
     */
    public static DbNodeTasksInfo evaluateHighestPriorityDbNode(List<JobInstanceArchiveTaskInfo> runningTasks,
                                                                Map<String, Integer> scheduleTaskCountGroupByDb) {
        Map<String, Integer> runningTaskCountGroupByDb = groupRunningTaskCountByDb(runningTasks);

        List<DbNodeTasksInfo> dbNodeTasksInfos = new ArrayList<>();
        scheduleTaskCountGroupByDb.keySet().forEach(dbNodeId ->
            dbNodeTasksInfos.add(new DbNodeTasksInfo(
                dbNodeId,
                runningTaskCountGroupByDb.get(dbNodeId),
                scheduleTaskCountGroupByDb.get(dbNodeId)
            )));
        // 优先级排序
        sortDbNodeTaskCounters(dbNodeTasksInfos);
        return dbNodeTasksInfos.get(0);
    }

    @Data
    public static class DbNodeTasksInfo {
        private String dbNodeId;
        private int runningTaskCount;
        private int scheduleTaskCount;

        public DbNodeTasksInfo(String dbNodeId, Integer runningTaskCount, Integer scheduleTaskCount) {
            this.dbNodeId = dbNodeId;
            this.runningTaskCount = runningTaskCount == null ? 0 : runningTaskCount;
            this.scheduleTaskCount = scheduleTaskCount == null ? 0 : scheduleTaskCount;
        }
    }

    private static Map<String, Integer> groupRunningTaskCountByDb(List<JobInstanceArchiveTaskInfo> runningTasks) {
        Map<String, Integer> dbTaskCountMap = new HashMap<>();
        if (CollectionUtils.isEmpty(runningTasks)) {
            return dbTaskCountMap;
        }

        runningTasks.forEach(task -> {
            String dbNodeId = task.getDbDataNode().toDbNodeId();
            dbTaskCountMap.compute(dbNodeId, (k, v) -> {
                if (v == null) {
                    v = 1;
                } else {
                    v++;
                }
                return v;
            });
        });
        return dbTaskCountMap;
    }

    private static void sortDbNodeTaskCounters(List<DbNodeTasksInfo> dbNodeTasksInfos) {
        dbNodeTasksInfos.sort((node1, node2) -> {
            // 先比较每个 db 上正在运行的任务数量，任务数量越少优先级越高
            int node1RunningTaskCount = node1.getRunningTaskCount();
            int node2RunningTaskCount = node2.getRunningTaskCount();
            if (node1RunningTaskCount < node2RunningTaskCount) {
                return -1;
            } else if (node1RunningTaskCount > node2RunningTaskCount) {
                return 1;
            } else {
                // 再比较每个 db 上的待调度任务的数量，待调度任务数量越多优先级越高
                int node1ScheduleTaskCount = node1.getScheduleTaskCount();
                int node2ScheduleTaskCount = node2.getScheduleTaskCount();
                if (node1ScheduleTaskCount < node2ScheduleTaskCount) {
                    return 1;
                } else if (node1ScheduleTaskCount > node2ScheduleTaskCount) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
    }
}
