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

import com.tencent.bk.job.manage.common.consts.task.TaskScriptSourceEnum;
import com.tencent.bk.job.manage.model.dto.task.TaskScriptStepDTO;

import java.util.List;
import java.util.Map;

/**
 * @since 3/10/2019 20:15
 */
public interface TaskScriptStepDAO {

    /**
     * 根据父 ID 批量拉取脚本步骤信息
     *
     * @param parentId 父 ID
     * @return 脚本步骤信息列表
     */
    List<TaskScriptStepDTO> listScriptStepByParentId(long parentId);

    /**
     * 根据步骤 ID 列表批量拉脚本步骤信息
     *
     * @param stepIdList 步骤 ID 列表
     * @return 脚本步骤信息列表
     */
    Map<Long, TaskScriptStepDTO> listScriptStepByIds(List<Long> stepIdList);

    /**
     * 根据步骤 ID 查询脚本步骤信息
     *
     * @param stepId 步骤 ID
     * @return 审批信息
     */
    TaskScriptStepDTO getScriptStepById(long stepId);

    /**
     * 新增脚本步骤信息
     *
     * @param scriptStep 脚本步骤信息
     * @return 新增脚本步骤信息的 ID
     */
    long insertScriptStep(TaskScriptStepDTO scriptStep);

    /**
     * 根据 ID 和步骤 ID 更新脚本步骤
     *
     * @param scriptStep 脚本步骤信息
     * @return 是否更新成功
     */
    boolean updateScriptStepById(TaskScriptStepDTO scriptStep);

    /**
     * 根据步骤 ID 删除脚本步骤
     *
     * @param stepId 步骤 ID
     * @return 是否删除成功
     */
    boolean deleteScriptStepById(long stepId);

    /**
     * 根据模版 ID 列表批量获取脚本信息列表
     *
     * @param templateIdList 模版 ID 列表
     * @return 模版对应的脚本信息列表
     */
    List<TaskScriptStepDTO> batchListScriptStepIdByParentIds(List<Long> templateIdList);

    /**
     * 更新脚本步骤引用的脚本版本ID
     *
     * @param templateId      作业模板ID
     * @param stepId          步骤ID
     * @param scriptVersionId 脚本版本ID
     * @return 是否更新成功
     */
    default boolean updateScriptStepRefScriptVersionId(Long templateId, Long stepId, Long scriptVersionId) {
        return true;
    }

    /**
     * 统计脚本步骤数量
     *
     * @param appId        业务Id
     * @param scriptSource 脚本来源
     * @return
     */
    int countScriptSteps(Long appId, TaskScriptSourceEnum scriptSource);

    /**
     * 统计被引用的脚本数量
     *
     * @param appId        业务Id
     * @param scriptIdList 脚本Id列表
     * @return
     */
    int countScriptCitedByStepsByScriptIds(Long appId, List<String> scriptIdList);


    /**
     * 统计引用脚本的步骤数量
     *
     * @param appId        业务Id
     * @param scriptIdList 脚本Id列表
     * @return
     */
    int countScriptStepsByScriptIds(Long appId, List<String> scriptIdList);
}
