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

public interface CmdbEventManager {

    /**
     * 初始化
     */
    void init();

    /**
     * 获取某个租户下的主机事件监听器
     *
     * @return 租户主机事件监听器
     */
    TenantHostEventWatcher getTenantHostEventWatcher(String tenantId);

    /**
     * 开启业务事件监听
     *
     * @return 操作是否成功
     */
    Boolean enableBizWatch();

    /**
     * 禁用业务事件监听
     *
     * @return 操作是否成功
     */
    Boolean disableBizWatch();

    /**
     * 开启主机事件监听
     *
     * @return 操作是否成功
     */
    Boolean enableHostWatch();

    /**
     * 禁用主机事件监听
     *
     * @return 操作是否成功
     */
    Boolean disableHostWatch();

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
    boolean watchBizEvent(String tenantId);

    /**
     * 监听业务集事件
     *
     * @param tenantId 租户ID
     * @return 是否监听成功
     */
    boolean watchBizSetEvent(String tenantId);

    /**
     * 监听业务集关系事件
     *
     * @param tenantId 租户ID
     * @return 是否监听成功
     */
    boolean watchBizSetRelationEvent(String tenantId);

    /**
     * 监听主机事件
     *
     * @param tenantId 租户ID
     * @return 是否监听成功
     */
    boolean watchHostEvent(String tenantId);

    /**
     * 监听主机关系事件
     *
     * @param tenantId 租户ID
     * @return 是否监听成功
     */
    boolean watchHostRelationEvent(String tenantId);
}
