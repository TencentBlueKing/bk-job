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

package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.dao.StepInstanceRollingTaskDAO;
import com.tencent.bk.job.execute.model.StepInstanceRollingTaskDTO;
import com.tencent.bk.job.execute.service.StepInstanceRollingTaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StepInstanceRollingTaskServiceImpl implements StepInstanceRollingTaskService {

    private final StepInstanceRollingTaskDAO stepInstanceRollingTaskDAO;

    @Autowired
    public StepInstanceRollingTaskServiceImpl(
        StepInstanceRollingTaskDAO stepInstanceRollingTaskDAO) {
        this.stepInstanceRollingTaskDAO = stepInstanceRollingTaskDAO;
    }

    @Override
    public StepInstanceRollingTaskDTO queryRollingTask(long stepInstanceId,
                                                       int executeCount, int batch) {
        return stepInstanceRollingTaskDAO.queryRollingTask(stepInstanceId, executeCount, batch);
    }

    @Override
    public List<StepInstanceRollingTaskDTO> listLatestRollingTasks(long stepInstanceId, int executeCount) {
        List<StepInstanceRollingTaskDTO> stepInstanceRollingTasks =
            stepInstanceRollingTaskDAO.listRollingTasks(stepInstanceId);
        if (CollectionUtils.isEmpty(stepInstanceRollingTasks) || executeCount == 0) {
            return stepInstanceRollingTasks;
        }

        Map<String, StepInstanceRollingTaskDTO> latestRollingTasks = new HashMap<>();
        for (StepInstanceRollingTaskDTO stepInstanceRollingTask : stepInstanceRollingTasks) {
            if (stepInstanceRollingTask.getExecuteCount() > executeCount) {
                continue;
            }
            String key = stepInstanceRollingTask.getStepInstanceId() + ":" + stepInstanceRollingTask.getBatch();
            StepInstanceRollingTaskDTO existStepInstanceRollingTask = latestRollingTasks.get(key);
            if (existStepInstanceRollingTask == null) {
                latestRollingTasks.put(key, stepInstanceRollingTask);
            } else {
                if (stepInstanceRollingTask.getExecuteCount() > existStepInstanceRollingTask.getExecuteCount()) {
                    // 覆盖旧的滚动任务
                    latestRollingTasks.put(key, stepInstanceRollingTask);
                }
            }
        }
        return latestRollingTasks.values().stream()
            .sorted(Comparator.comparing(StepInstanceRollingTaskDTO::getBatch))
            .collect(Collectors.toList());
    }

    @Override
    public long saveRollingTask(StepInstanceRollingTaskDTO rollingTask) {
        return stepInstanceRollingTaskDAO.saveRollingTask(rollingTask);
    }

    @Override
    public void updateRollingTask(long stepInstanceId,
                                  int executeCount,
                                  int batch,
                                  RunStatusEnum status,
                                  Long startTime,
                                  Long endTime,
                                  Long totalTime) {
        stepInstanceRollingTaskDAO.updateRollingTask(stepInstanceId, executeCount, batch, status, startTime,
            endTime, totalTime);
    }

}
