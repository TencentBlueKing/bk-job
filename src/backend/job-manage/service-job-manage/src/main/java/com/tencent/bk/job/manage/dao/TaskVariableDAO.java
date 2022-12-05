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

import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;

import java.util.List;

/**
 * @since 3/10/2019 20:14
 */
public interface TaskVariableDAO {

    /**
     * 根据父 ID 查询变量列表（模版 ID，计划 ID，实例 ID）
     *
     * @param parentId 父 ID
     * @return 变量信息列表
     */
    List<TaskVariableDTO> listVariablesByParentId(long parentId);

    /**
     * 根据变量 ID 和父 ID 查询变量信息
     *
     * @param id       变量 ID
     * @param parentId 父 ID
     * @return 变量信息
     */
    TaskVariableDTO getVariableById(long parentId, long id);

    /**
     * 根据执行方案 ID 和 变量名称查询变量信息
     *
     * @param parentId 父资源ID
     * @param varName 变量名称
     * @return 变量信息
     */
    TaskVariableDTO getVariableByName(long parentId, String varName);

    /**
     * 新增变量
     *
     * @param variable 变量信息
     * @return 新增变量的 ID
     */
    long insertVariable(TaskVariableDTO variable);

    /**
     * 批量新增变量
     *
     * @param variableList 变量信息列表
     * @return 新增的变量 ID 列表
     */
    List<Long> batchInsertVariables(List<TaskVariableDTO> variableList);

    /**
     * 根据变量 ID 更新变量信息
     *
     * @param variable 变量信息
     * @return 是否更新成功
     */
    boolean updateVariableById(TaskVariableDTO variable);

    /**
     * 根据变量 ID 和父 ID 删除变量信息
     *
     * @param parentId 父 ID
     * @param id       变量 ID
     * @return 是否删除成功
     */
    boolean deleteVariableById(long parentId, long id);

    /**
     * 根据 父资源ID 删除变量信息
     *
     * @param parentId 父资源ID
     * @return 是否删除成功
     */
    int deleteVariableByParentId(long parentId);

    /**
     * 保留变量 ID 批量插入变量信息
     *
     * @param variableList 变量信息列表
     * @return 是否插入成功
     */
    boolean batchInsertVariableWithId(List<TaskVariableDTO> variableList);

    boolean updateVariableByName(TaskVariableDTO variable);
}
