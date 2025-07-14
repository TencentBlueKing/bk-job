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

package com.tencent.bk.job.execute.dao;

import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.StepInstanceFileBatchDTO;

import java.util.List;

/**
 * 文件步骤批次实例 DAO
 */
public interface StepInstanceFileBatchDAO {

    /**
     * 批量插入文件步骤批次数据
     *
     * @param stepInstanceFileBatchDTOList 文件步骤批次数据列表
     * @return 插入行数
     */
    int batchInsert(List<StepInstanceFileBatchDTO> stepInstanceFileBatchDTOList);

    /**
     * 获取文件步骤批次中的最大批次号
     *
     * @param taskInstanceId 作业实例ID
     * @param stepInstanceId 步骤实例ID
     * @return 最大批次号
     */
    Integer getMaxBatch(long taskInstanceId, long stepInstanceId);

    /**
     * 获取文件步骤批次数据
     *
     * @param taskInstanceId 作业实例ID
     * @param stepInstanceId 步骤实例ID
     * @param batch          批次号
     * @return 文件步骤批次数据
     */
    StepInstanceFileBatchDTO get(long taskInstanceId, long stepInstanceId, int batch);

    /**
     * 获取某个文件步骤的所有批次数据
     *
     * @param taskInstanceId 作业实例ID
     * @param stepInstanceId 步骤实例ID
     * @return 文件步骤批次数据
     */
    List<StepInstanceFileBatchDTO> list(long taskInstanceId, long stepInstanceId);

    /**
     * 更新变量解析之后的源文件
     *
     * @param taskInstanceId      作业实例ID
     * @param stepInstanceId      步骤实例ID
     * @param batch               批次号
     * @param resolvedFileSources 解析后的源文件信息
     * @return 更新行数
     */
    int updateResolvedSourceFile(long taskInstanceId,
                                 long stepInstanceId,
                                 int batch,
                                 List<FileSourceDTO> resolvedFileSources);
}
