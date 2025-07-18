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

package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.dao.TaskInstanceVariableDAO;
import com.tencent.bk.job.execute.dao.common.IdGen;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class TaskInstanceVariableServiceImpl implements com.tencent.bk.job.execute.service.TaskInstanceVariableService {
    private final TaskInstanceVariableDAO taskInstanceVariableDAO;
    private final IdGen idGen;

    @Autowired
    public TaskInstanceVariableServiceImpl(TaskInstanceVariableDAO taskInstanceVariableDAO, IdGen idGen) {
        this.taskInstanceVariableDAO = taskInstanceVariableDAO;
        this.idGen = idGen;
    }

    @Override
    public List<TaskVariableDTO> getByTaskInstanceId(long taskInstanceId) {
        List<TaskVariableDTO> taskVarList = taskInstanceVariableDAO.getByTaskInstanceId(taskInstanceId);
        if (taskVarList != null) {
            for (TaskVariableDTO taskVariable : taskVarList) {
                if (taskVariable.getType() == TaskVariableTypeEnum.HOST_LIST.getType()
                    && taskVariable.getValue() != null) {
                    taskVariable.setExecuteTarget(
                        JsonUtils.fromJson(taskVariable.getValue(), ExecuteTargetDTO.class));
                }
            }
        }
        return taskVarList;
    }

    @Override
    public void batchDeleteByTaskInstanceIds(List<Long> taskInstanceIdList) {
        taskInstanceVariableDAO.batchDeleteByTaskInstanceIds(taskInstanceIdList);
    }

    @Override
    public void deleteByTaskInstanceId(long taskInstanceId) {
        taskInstanceVariableDAO.deleteByTaskInstanceId(taskInstanceId);
    }

    @Override
    public void saveTaskInstanceVariables(List<TaskVariableDTO> taskVarList) {
        if (taskVarList == null || taskVarList.isEmpty()) {
            return;
        }
        for (TaskVariableDTO taskVariable : taskVarList) {
            if (taskVariable.getType() == TaskVariableTypeEnum.HOST_LIST.getType()
                && taskVariable.getExecuteTarget() != null) {
                taskVariable.setValue(JsonUtils.toJson(taskVariable.getExecuteTarget()));
            }
            taskVariable.setId(idGen.genTaskInstanceVariableId());
        }
        taskInstanceVariableDAO.saveTaskInstanceVariables(taskVarList);
    }
}
