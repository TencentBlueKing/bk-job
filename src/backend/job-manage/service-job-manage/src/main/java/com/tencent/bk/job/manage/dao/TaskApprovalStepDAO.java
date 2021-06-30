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

import com.tencent.bk.job.manage.model.dto.task.TaskApprovalStepDTO;

import java.util.List;
import java.util.Map;

/**
 * @since 3/10/2019 20:15
 */
public interface TaskApprovalStepDAO {

    /**
     * 根据步骤 ID 列表批量查询审批步骤信息
     *
     * @param stepIdList 步骤 ID 列表
     * @return 审批信息列表
     */
    Map<Long, TaskApprovalStepDTO> listApprovalsByIds(List<Long> stepIdList);

    /**
     * 根据步骤 ID 查询审批步骤信息
     *
     * @param stepId 步骤 ID
     * @return 审批信息
     */
    TaskApprovalStepDTO getApprovalById(long stepId);

    /**
     * 新增审批步骤信息
     *
     * @param approvalStep 审批步骤信息
     * @return 新增审批步骤信息的 ID
     */
    long insertApproval(TaskApprovalStepDTO approvalStep);

    /**
     * 根据 ID 和步骤 ID 更新审批步骤
     *
     * @param approvalStep 审批步骤信息
     * @return 是否更新成功
     */
    boolean updateApprovalById(TaskApprovalStepDTO approvalStep);

    /**
     * 根据步骤 ID 删除审批步骤
     *
     * @param stepId 步骤 ID
     * @return 是否删除成功
     */
    boolean deleteApprovalById(long stepId);

    int countApprovalSteps(Long appId);
}
