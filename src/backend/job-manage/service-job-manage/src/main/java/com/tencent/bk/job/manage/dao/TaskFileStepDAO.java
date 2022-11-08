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

import com.tencent.bk.job.common.constant.DuplicateHandlerEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.model.dto.task.TaskFileStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetDTO;

import java.util.List;
import java.util.Map;

/**
 * @since 3/10/2019 20:15
 */
public interface TaskFileStepDAO {

    /**
     * 根据步骤 ID 批量拉取文件步骤信息
     *
     * @param stepIdList 步骤 ID 列表
     * @return 文件步骤信息
     */
    Map<Long, TaskFileStepDTO> listFileStepsByIds(List<Long> stepIdList);

    /**
     * 根据步骤 ID 查询文件步骤信息
     *
     * @param stepId 步骤 ID
     * @return 审批信息
     */
    TaskFileStepDTO getFileStepById(long stepId);

    /**
     * 新增文件步骤信息
     *
     * @param fileStep 文件步骤信息
     * @return 新增文件步骤信息的 ID
     */
    long insertFileStep(TaskFileStepDTO fileStep);

    /**
     * 根据 ID 和步骤 ID 更新文件步骤
     *
     * @param fileStep 文件步骤信息
     * @return 是否更新成功
     */
    boolean updateFileStepById(TaskFileStepDTO fileStep);

    /**
     * 根据步骤 ID 删除文件步骤
     *
     * @param stepId 步骤 ID
     * @return 是否删除成功
     */
    boolean deleteFileStepById(long stepId);

    int countFileSteps(Long appId, TaskFileTypeEnum fileType);

    default void fixTargerLocation(TaskFileStepDTO fileStep) {
        if (fileStep.getDuplicateHandler() != null) {
            switch (fileStep.getDuplicateHandler()) {
                case OVERWRITE:
                    break;
                case GROUP_BY_IP:
                    fileStep.setDuplicateHandler(DuplicateHandlerEnum.OVERWRITE);
                    if (!fileStep.getDestinationFileLocation().endsWith("/")) {
                        fileStep.setDestinationFileLocation(fileStep.getDestinationFileLocation() + "/");
                    }
                    fileStep.setDestinationFileLocation(fileStep.getDestinationFileLocation() + "[FILESRCIP]");
                    break;
                case GROUP_BY_DATE_AND_IP:
                    fileStep.setDuplicateHandler(DuplicateHandlerEnum.OVERWRITE);
                    if (!fileStep.getDestinationFileLocation().endsWith("/")) {
                        fileStep.setDestinationFileLocation(fileStep.getDestinationFileLocation() + "/");
                    }
                    fileStep.setDestinationFileLocation(fileStep.getDestinationFileLocation() + "[YYYY-MM-DD" +
                        "]/[FILESRCIP]");
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 查询所有的步骤的目标主机
     */
    Map<Long, TaskTargetDTO> listStepTargets();

    /**
     * 更新步骤的目标主机的值
     *
     * @param recordId 记录id
     * @param value    值
     * @return 更新结果
     */
    boolean updateStepTargets(Long recordId, String value);
}
