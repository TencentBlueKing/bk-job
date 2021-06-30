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

package com.tencent.bk.job.manage.service.plan.impl;

import com.tencent.bk.job.manage.dao.TaskVariableDAO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import com.tencent.bk.job.manage.service.AbstractTaskVariableService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @since 16/10/2019 19:57
 */
@Slf4j
@Service("TaskPlanVariableServiceImpl")
public class TaskPlanVariableServiceImpl extends AbstractTaskVariableService {

    @Autowired
    public TaskPlanVariableServiceImpl(@Qualifier("TaskPlanVariableDAOImpl") TaskVariableDAO taskVariableDAO) {
        this.taskVariableDAO = taskVariableDAO;
    }

    @Override
    public TaskVariableDTO getVariableById(Long parentId, Long id) {
        return taskVariableDAO.getVariableById(parentId, id);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class, Error.class})
    public boolean batchUpdateVariableByName(List<TaskVariableDTO> variableList) {
        if (CollectionUtils.isEmpty(variableList)) {
            return false;
        }
        boolean hasChange = false;
        for (TaskVariableDTO variable : variableList) {
            if (variable.getType().isNeedMask() && variable.getType().getMask().equals(variable.getDefaultValue())) {
                continue;
            }
            hasChange = true;
            if (!taskVariableDAO.updateVariableByName(variable)) {
                log.error("Error while updating plan variable value!|{}", variable);
                return false;
            }
        }
        return hasChange;
    }

    @Override
    public TaskVariableDTO getVariableByName(Long parentId, String name) {
        return taskVariableDAO.getVariableByName(parentId, name);
    }
}
