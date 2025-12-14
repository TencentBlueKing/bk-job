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

package com.tencent.bk.job.manage.background.event.cmdb;

import com.tencent.bk.job.manage.api.common.constants.EventWatchTaskTypeEnum;

public interface CmdbEventManager {

    /**
     * 开启事件监听
     *
     * @param taskType 任务类型
     * @return 开启的事件监听器数量
     */
    Integer enableWatch(EventWatchTaskTypeEnum taskType);

    /**
     * 禁用事件监听
     *
     * @param taskType 任务类型
     * @return 禁用的事件监听器数量
     */
    Integer disableWatch(EventWatchTaskTypeEnum taskType);

    /**
     * 判断业务事件监听是否在运行
     *
     * @param tenantId 租户ID
     * @return 是否在运行
     */
    boolean isWatchBizEventRunning(String tenantId);

    /**
     * 判断业务集事件监听是否在运行
     *
     * @param tenantId 租户ID
     * @return 是否在运行
     */
    boolean isWatchBizSetEventRunning(String tenantId);

    /**
     * 判断业务集关系事件监听是否在运行
     *
     * @param tenantId 租户ID
     * @return 是否在运行
     */
    boolean isWatchBizSetRelationEventRunning(String tenantId);

    /**
     * 判断主机事件监听是否在运行
     *
     * @param tenantId 租户ID
     * @return 是否在运行
     */
    boolean isWatchHostEventRunning(String tenantId);

    /**
     * 判断主机关系事件监听是否在运行
     *
     * @param tenantId 租户ID
     * @return 是否在运行
     */
    boolean isWatchHostRelationEventRunning(String tenantId);

    /**
     * 监听业务事件
     *
     * @param tenantId 租户ID
     * @return 是否监听成功
     */
    boolean startWatchBizEvent(String tenantId);

    /**
     * 监听业务集事件
     *
     * @param tenantId 租户ID
     * @return 是否监听成功
     */
    boolean startWatchBizSetEvent(String tenantId);

    /**
     * 监听业务集关系事件
     *
     * @param tenantId 租户ID
     * @return 是否监听成功
     */
    boolean startWatchBizSetRelationEvent(String tenantId);

    /**
     * 监听主机事件
     *
     * @param tenantId 租户ID
     * @return 是否监听成功
     */
    boolean startWatchHostEvent(String tenantId);

    /**
     * 监听主机关系事件
     *
     * @param tenantId 租户ID
     * @return 是否监听成功
     */
    boolean startWatchHostRelationEvent(String tenantId);
}
