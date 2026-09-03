/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.bk.job.execute.service;

import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;

import java.util.Collection;
import java.util.List;

/**
 * 作业执行实例Service
 */
public interface TaskInstanceService {

    long addTaskInstance(TaskInstanceDTO taskInstance);

    /**
     * 按业务查询作业实例。只校验实例归属，<b>不做用户级鉴权</b>，仅供内部调用；
     * 外部入口（Web / ESB）请用带 username 的重载。
     */
    TaskInstanceDTO getTaskInstance(long appId, long taskInstanceId) throws NotFoundException;

    /**
     * 按 ID 查询作业实例。<b>既不校验归属也不鉴权</b>，仅供引擎内部调用；
     * 外部入口（Web / ESB）请用带 username 的重载，否则调用者可访问任意业务的实例。
     */
    TaskInstanceDTO getTaskInstance(long taskInstanceId) throws NotFoundException;

    /**
     * 按 (调用者, 业务) 查询作业实例：校验归属 + 用户级鉴权（非执行人要求 VIEW_HISTORY）+ 审计。
     * 外部入口读取或操作作业实例时应统一走这个重载。
     */
    TaskInstanceDTO getTaskInstance(String username, long appId, long taskInstanceId)
        throws NotFoundException, PermissionDeniedException;

    /**
     * 获取作业实例详情-包含步骤信息和全局变量信息
     *
     * @param username       用户名
     * @param appId          业务 ID
     * @param taskInstanceId 作业实例 ID
     * @return 作业实例
     */
    TaskInstanceDTO getTaskInstanceDetail(String username, long appId, long taskInstanceId)
        throws NotFoundException, PermissionDeniedException;

    void updateTaskStatus(long taskInstanceId, int status);

    void updateTaskCurrentStepId(long taskInstanceId, Long stepInstanceId);

    void resetTaskStatus(long taskInstanceId);

    /**
     * 作业恢复执行-重置作业执行状态
     *
     * @param taskInstanceId 作业实例ID
     */
    void resetTaskExecuteInfoForRetry(long taskInstanceId);

    /**
     * 更新作业的执行信息
     *
     * @param taskInstanceId 作业实例ID
     * @param status         作业状态
     * @param currentStepId  当前步骤实例ID
     * @param startTime      开始时间
     * @param endTime        结束时间
     * @param totalTime      总耗时
     */
    void updateTaskExecutionInfo(long taskInstanceId,
                                 RunStatusEnum status,
                                 Long currentStepId,
                                 Long startTime,
                                 Long endTime,
                                 Long totalTime);

    List<Long> getJoinedAppIdList();

    boolean hasExecuteHistory(Long appId, Long cronTaskId, Long fromTime, Long toTime);

    List<Long> listTaskInstanceId(Long appId, Long fromTime, Long toTime, int offset, int limit);

    /**
     * 保存作业实例与主机的关系，便于根据ip/ipv6检索作业实例
     *
     * @param appId          业务 ID
     * @param taskInstanceId 作业实例ID
     * @param hosts          主机列表
     */
    void saveTaskInstanceHosts(long appId, long taskInstanceId, Collection<HostDTO> hosts);
}
