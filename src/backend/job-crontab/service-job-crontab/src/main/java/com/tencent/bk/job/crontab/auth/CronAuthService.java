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

package com.tencent.bk.job.crontab.auth;

import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;

import java.util.List;

/**
 * 定时任务相关操作鉴权接口
 */
public interface CronAuthService {
    /**
     * 资源范围下创建定时任务鉴权
     *
     * @param user             用户
     * @param appResourceScope 资源范围
     * @return 鉴权结果
     */
    AuthResult authCreateCron(User user, AppResourceScope appResourceScope);

    /**
     * 资源范围下管理定时任务鉴权
     *
     * @param user             用户
     * @param appResourceScope 资源范围
     * @param cronId           定时任务ID
     * @param cronName         定时任务名称，如果传入为空，则会调用ResourceNameQueryService查询
     * @return 鉴权结果
     */
    AuthResult authManageCron(User user,
                              AppResourceScope appResourceScope,
                              Long cronId,
                              String cronName);

    /**
     * 资源范围下管理定时任务批量鉴权
     *
     * @param user             用户
     * @param appResourceScope 资源范围
     * @param cronIdList       定时任务ID列表
     * @return 有权限的定时任务ID
     */
    List<Long> getPermissionAllowedCronIds(User user,
                                           AppResourceScope appResourceScope,
                                           List<Long> cronIdList);

    /**
     * 定时任务管理批量鉴权
     *
     * @param user             用户
     * @param appResourceScope 资源范围
     * @param cronIdList       定时任务ID列表
     * @throws PermissionDeniedException 鉴权未通过
     */
    void batchAuthManageCron(User user,
                             AppResourceScope appResourceScope,
                             List<Long> cronIdList) throws PermissionDeniedException;

    /**
     * 注册定时任务实例
     *
     * @param creator 资源实例创建者
     * @param id      资源实例 ID
     * @param name    资源实例名称
     * @return 是否注册成功
     */
    boolean registerCron(User creator, Long id, String name);
}
