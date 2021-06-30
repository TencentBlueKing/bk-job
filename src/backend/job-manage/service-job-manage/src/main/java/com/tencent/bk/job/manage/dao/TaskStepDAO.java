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

import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;

import java.util.List;

/**
 * @since 3/10/2019 20:14
 */
public interface TaskStepDAO {

    /**
     * 根据父 ID 列步骤列表
     *
     * @param parentId 父 ID
     * @return 步骤列表
     */
    List<TaskStepDTO> listStepsByParentId(long parentId);

    /**
     * 根据步骤 ID 和父 ID 获取步骤信息
     *
     * @param id       步骤 ID
     * @param parentId 父 ID
     * @return 步骤信息
     */
    TaskStepDTO getStepById(long parentId, long id);

    /**
     * 新增步骤
     *
     * @param taskStep 步骤信息
     * @return 新增的步骤 ID
     */
    long insertStep(TaskStepDTO taskStep);

    /**
     * 根据步骤 ID 和父 ID 更新步骤信息
     *
     * @param taskStep 步骤信息
     * @return 更新是否成功
     */
    boolean updateStepById(TaskStepDTO taskStep);

    /**
     * 根据步骤 ID 和父 ID 删除步骤信息
     *
     * @param id       步骤 ID
     * @param parentId 父 ID
     * @return 删除是否成功
     */
    boolean deleteStepById(long parentId, long id);

    List<Long> listStepIdByParentId(List<Long> parentIdList);
}
