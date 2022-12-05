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

package com.tencent.bk.job.manage.service;

import com.tencent.bk.job.manage.dao.TaskVariableDAO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;

import java.util.List;

/**
 * @since 16/10/2019 19:40
 */
public abstract class AbstractTaskVariableService {

    protected TaskVariableDAO taskVariableDAO;

    /**
     * 根据父 ID 查询变量列表
     *
     * @param parentId 父资源 ID
     * @return 变量列表
     */
    public List<TaskVariableDTO> listVariablesByParentId(Long parentId) {
        return taskVariableDAO.listVariablesByParentId(parentId);
    }

    /**
     * 新增变量
     *
     * @param variable 变量信息
     * @return 新增的变量 ID
     */
    public long insertVariable(TaskVariableDTO variable) {
        return taskVariableDAO.insertVariable(variable);
    }

    /**
     * 批量新增变量
     *
     * @param variableList 变量列表
     * @return 新增的变量 ID 列表
     */
    public List<Long> batchInsertVariable(List<TaskVariableDTO> variableList) {
        return taskVariableDAO.batchInsertVariables(variableList);
    }

    /**
     * 根据 ID 更新变量
     *
     * @param variable 变量信息
     * @return 是否更新成功
     */
    public boolean updateVariableById(TaskVariableDTO variable) {
        return taskVariableDAO.updateVariableById(variable);
    }

    /**
     * 根据 ID 删除变量
     *
     * @param parentId 父资源 ID
     * @param id       变量 ID
     * @return 是否删除成功
     */
    public boolean deleteVariableById(Long parentId, Long id) {
        return taskVariableDAO.deleteVariableById(parentId, id);
    }

    /**
     * 根据 父资源ID 删除变量
     *
     * @param parentId 父资源 ID
     * @return 删除的数据条数
     */
    public int deleteVariableByParentId(Long parentId) {
        return taskVariableDAO.deleteVariableByParentId(parentId);
    }

    /**
     * 根据变量 ID 拉取变量信息
     *
     * @param parentId 父资源 ID
     * @param id       变量 ID
     * @return 变量信息
     */
    public abstract TaskVariableDTO getVariableById(Long parentId, Long id);

    /**
     * 根据变量 Name 拉取变量信息
     *
     * @param parentId 父资源 ID
     * @param name     变量 Name
     * @return 变量信息
     */
    public abstract TaskVariableDTO getVariableByName(Long parentId, String name);

    /**
     * 批量插入变量信息并保留 ID
     *
     * @param variableList 变量信息列表
     * @return 是否插入成功
     */
    public boolean batchInsertVariableWithId(List<TaskVariableDTO> variableList) {
        return taskVariableDAO.batchInsertVariableWithId(variableList);
    }

    public abstract boolean batchUpdateVariableByName(List<TaskVariableDTO> variableList);
}
