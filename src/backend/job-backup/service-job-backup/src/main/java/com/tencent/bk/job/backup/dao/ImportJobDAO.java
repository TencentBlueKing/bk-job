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

package com.tencent.bk.job.backup.dao;

import com.tencent.bk.job.backup.model.dto.ImportJobInfoDTO;

import java.util.List;

/**
 * @since 24/7/2020 14:32
 */
public interface ImportJobDAO {
    /**
     * 插入导入任务
     *
     * @param importJobInfo 导入任务信息
     * @return 导入任务 ID
     */
    String insertImportJob(ImportJobInfoDTO importJobInfo);

    /**
     * 根据导入任务 ID 拉取导入任务信息
     *
     * @param appId 业务 ID
     * @param jobId 任务 ID
     * @return 导入任务信息
     */
    ImportJobInfoDTO getImportJobById(Long appId, String jobId);

    /**
     * 根据用户获取未完成的导入任务列表
     *
     * @param appId    业务 ID
     * @param username 用户名
     * @return 导入任务信息列表
     */
    List<ImportJobInfoDTO> getImportJobByUser(Long appId, String username);

    /**
     * 更新导入任务信息
     *
     * @param importJobInfo 导入任务信息
     * @return 是否更新成功
     */
    boolean updateImportJobById(ImportJobInfoDTO importJobInfo);

    /**
     * 获取 7 天前未清理的导入任务列表
     *
     * @return 导入任务信息列表
     */
    List<ImportJobInfoDTO> listOldImportJob();

    /**
     * 设置导入任务的清理状态
     *
     * @param appId 业务 ID
     * @param jobId 导入任务 ID
     * @return 是否更新成功
     */
    boolean setCleanMark(Long appId, String jobId);
}
