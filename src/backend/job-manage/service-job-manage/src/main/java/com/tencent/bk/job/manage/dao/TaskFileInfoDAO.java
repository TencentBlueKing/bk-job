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

import com.tencent.bk.job.manage.model.dto.task.TaskFileInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetDTO;

import java.util.List;
import java.util.Map;

/**
 * @since 10/10/2019 15:54
 */
public interface TaskFileInfoDAO {

    /**
     * 根据步骤 ID 列表获取文件信息列表
     *
     * @param stepIdList 步骤 ID 列表
     * @return 文件信息列表
     */
    Map<Long, List<TaskFileInfoDTO>> listFileInfosByStepIds(List<Long> stepIdList);

    /**
     * 根据步骤 ID 获取文件列表
     *
     * @param stepId 步骤 ID
     * @return 文件列表
     */
    List<TaskFileInfoDTO> listFileInfoByStepId(long stepId);

    /**
     * 根据步骤 ID 和文件 ID 查询文件信息
     *
     * @param stepId 步骤 ID
     * @param fileId 文件 ID
     * @return 审批信息
     */
    TaskFileInfoDTO getFileInfoById(long stepId, long fileId);

    /**
     * 新增文件信息
     *
     * @param fileInfo 文件信息
     * @return 新增文件信息的 ID
     */
    long insertFileInfo(TaskFileInfoDTO fileInfo);

    /**
     * 批量新增文件信息
     *
     * @param fileInfoList 文件信息列表
     * @return 新增的文件信息 ID
     */
    List<Long> batchInsertFileInfo(List<TaskFileInfoDTO> fileInfoList);

    /**
     * 根据步骤 ID 和文件 ID 更新文件信息
     *
     * @param fileInfo 文件信息
     * @return 是否更新成功
     */
    boolean updateFileInfoById(TaskFileInfoDTO fileInfo);

    /**
     * 根据步骤 ID 和文件 ID 删除文件
     *
     * @param stepId 步骤 ID
     * @param fileId 文件 ID
     * @return 是否删除成功
     */
    boolean deleteFileInfoById(long stepId, long fileId);

    /**
     * 根据步骤 ID 批量删除文件
     *
     * @param stepId 步骤 ID
     * @return 是否删除成功
     */
    boolean deleteFileInfosByStepId(long stepId);

    List<String> listLocalFileByStepId(List<Long> stepIdList);

    /**
     * 查询所有的步骤的源文件的主机
     */
    Map<Long, TaskTargetDTO> listStepFileHosts();

    /**
     * 更新步骤文件源的主机的值
     *
     * @param recordId 记录id
     * @param value    值
     * @return 更新结果
     */
    boolean updateStepFileHosts(Long recordId, String value);
}
