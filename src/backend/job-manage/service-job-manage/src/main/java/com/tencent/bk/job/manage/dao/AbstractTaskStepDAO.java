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

package com.tencent.bk.job.manage.dao;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.List;
import java.util.Map;

/**
 * @since 17/2/2020 16:42
 */
@Slf4j
public abstract class AbstractTaskStepDAO implements TaskStepDAO {

    protected void sortStep(List<TaskStepDTO> taskStepList, Map<Long, TaskStepDTO> taskStepMap) {
        try {
            if (CollectionUtils.isNotEmpty(taskStepList)) {
                int pos = 0;
                while (true) {
                    if (pos >= taskStepList.size()) {
                        if (MapUtils.isNotEmpty(taskStepMap)) {
                            for (TaskStepDTO step : taskStepMap.values()) {
                                taskStepList.add(step);
                            }
                        }
                        break;
                    }
                    if (taskStepList.get(pos).getNextStepId() > 0) {
                        TaskStepDTO nextStep = taskStepMap.remove(taskStepList.get(pos).getNextStepId());
                        if (nextStep != null) {
                            taskStepList.add(nextStep);
                        }
                    } else {
                        break;
                    }
                    if (MapUtils.isEmpty(taskStepMap)) {
                        break;
                    }
                    pos++;
                }
            }
        } catch (Exception e) {
            log.error("Error while processing step records! Sort failed!", e);
            throw new InternalException("Sort step failed!", e, ErrorCode.INTERNAL_ERROR);
        }
    }
}
