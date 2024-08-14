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

import com.tencent.bk.job.backup.archive.model.HourArchiveTask;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ArchiveTaskPriorityEvaluatorTest {

    @Test
    void getHighestPriorityTask() {
        Integer dbNodeCount = 2;

        List<HourArchiveTask> runningTasks = new ArrayList<>();
        runningTasks.add(genTask("0:0", 20240808, 1));
        runningTasks.add(genTask("0:1", 20240808, 2));
        runningTasks.add(genTask("1:0", 20240808, 3));

        List<HourArchiveTask> candidateTasks = new ArrayList<>();
        candidateTasks.add(genTask("0:0", 20240807, 21));
        candidateTasks.add(genTask("0:0", 20240809, 22));
        candidateTasks.add(genTask("1:0", 20240808, 21));
        candidateTasks.add(genTask("1:0", 20240808, 22));
        candidateTasks.add(genTask("1:0", 20240809, 21));
        candidateTasks.add(genTask("1:0", 20240809, 22));

        ArchiveTaskPriorityEvaluator.sort(runningTasks, dbNodeCount, candidateTasks);
        assertThat(candidateTasks.get(0)).isNotNull();
        assertThat(candidateTasks.get(0).getDataNode()).isEqualTo("1:0");
        assertThat(candidateTasks.get(0).getDay()).isEqualTo(20240808);
        assertThat(candidateTasks.get(0).getHour()).isEqualTo(21);
        assertThat(candidateTasks.get(1)).isNotNull();
        assertThat(candidateTasks.get(1).getDataNode()).isEqualTo("1:0");
        assertThat(candidateTasks.get(1).getDay()).isEqualTo(20240808);
        assertThat(candidateTasks.get(1).getHour()).isEqualTo(22);
        assertThat(candidateTasks.get(2)).isNotNull();
        assertThat(candidateTasks.get(2).getDataNode()).isEqualTo("1:0");
        assertThat(candidateTasks.get(2).getDay()).isEqualTo(20240809);
        assertThat(candidateTasks.get(2).getHour()).isEqualTo(21);
        assertThat(candidateTasks.get(3)).isNotNull();
        assertThat(candidateTasks.get(3).getDataNode()).isEqualTo("1:0");
        assertThat(candidateTasks.get(3).getDay()).isEqualTo(20240809);
        assertThat(candidateTasks.get(3).getHour()).isEqualTo(22);
        assertThat(candidateTasks.get(4)).isNotNull();
        assertThat(candidateTasks.get(4).getDataNode()).isEqualTo("0:0");
        assertThat(candidateTasks.get(4).getDay()).isEqualTo(20240807);
        assertThat(candidateTasks.get(4).getHour()).isEqualTo(21);
        assertThat(candidateTasks.get(5)).isNotNull();
        assertThat(candidateTasks.get(5).getDataNode()).isEqualTo("0:0");
        assertThat(candidateTasks.get(5).getDay()).isEqualTo(20240809);
        assertThat(candidateTasks.get(5).getHour()).isEqualTo(22);

    }

    private HourArchiveTask genTask(String dataNodeIndex, Integer day, Integer hour) {
        HourArchiveTask task = new HourArchiveTask();
        task.setDataNode(dataNodeIndex);
        String[] dbAndTableParts = dataNodeIndex.split(":");
        task.setDbNodeIndex(Integer.parseInt(dbAndTableParts[0]));
        task.setTableIndex(Integer.parseInt(dbAndTableParts[1]));
        task.setDay(day);
        task.setHour(hour);
        return task;
    }
}
