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

package com.tencent.bk.job.manage.service.template.impl;

import com.tencent.bk.job.common.util.RandomUtil;
import com.tencent.bk.job.manage.dao.TaskVariableDAO;
import com.tencent.bk.job.manage.dao.template.impl.TaskTemplateVariableDAOImpl;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import com.tencent.bk.job.manage.service.AbstractTaskVariableService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @since 17/10/2019 17:09
 */
class TaskTemplateVariableServiceImplTest {

    private static TaskVariableDAO taskVariableDAO;
    private static AbstractTaskVariableService taskVariableService;

    @BeforeAll
    static void init() {
        taskVariableDAO = mock(TaskTemplateVariableDAOImpl.class);
        taskVariableService = new TaskTemplateVariableServiceImpl(taskVariableDAO);
    }

    @Test
    void givenNormalParentIdReturnVariableList() {
        long parentId = RandomUtil.getRandomPositiveLong();
        taskVariableService.listVariablesByParentId(parentId);
        verify(taskVariableDAO).listVariablesByParentId(parentId);
    }

    @Test
    void givenVariableInfoReturnId() {
        TaskVariableDTO taskVariable = new TaskVariableDTO();

        taskVariableService.insertVariable(taskVariable);
        verify(taskVariableDAO).insertVariable(taskVariable);
    }

    @Test
    void givenVariableInfoReturnUpdateSuccess() {
        TaskVariableDTO taskVariable = new TaskVariableDTO();
        taskVariableService.updateVariableById(taskVariable);
        verify(taskVariableDAO).updateVariableById(taskVariable);
    }

    @Test
    void givenNormalParentIdAndVariableReturnDeleteSuccess() {
        long parentId = RandomUtil.getRandomPositiveLong();
        long variableId = RandomUtil.getRandomPositiveLong();

        taskVariableService.deleteVariableById(parentId, variableId);
        verify(taskVariableDAO).deleteVariableById(parentId, variableId);
    }

}
