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

package com.tencent.bk.job.common.iam.service;

import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.dto.AppResourceScopeResult;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;

import java.util.List;

/**
 * 业务范围级鉴权执行器：纯鉴权，返回原始结果（不做 AuthResult 包装）。
 *
 * <p>由 v3 / v4 各自实现，各自构造本版本的资源 DTO；路由层按灰度选择具体实现并包装结果。</p>
 */
public interface AppAuthExecutor {

    /**
     * 业务范围下单操作鉴权。
     *
     * @return 是否有权限
     */
    boolean auth(User user, String actionId, AppResourceScope appResourceScope);

    /**
     * 批量鉴权（按资源类型 + 资源 ID 列表）。
     *
     * @return 有权限的资源 ID 列表
     */
    List<String> batchAuth(User user,
                           String actionId,
                           AppResourceScope appResourceScope,
                           ResourceTypeEnum resourceType,
                           List<String> resourceIdList);

    /**
     * 批量鉴权（按资源列表）。
     *
     * @return 有权限的资源 ID 列表
     */
    List<String> batchAuth(User user,
                           String actionId,
                           AppResourceScope appResourceScope,
                           List<PermissionResource> resourceList);

    /**
     * 批量鉴权并返回有权限的资源 ID 列表（供路由层构造 AuthResult 用）。
     *
     * @return 有权限的资源 ID 列表
     */
    List<String> listAllowedResourceIds(User user,
                                        String actionId,
                                        AppResourceScope appResourceScope,
                                        List<PermissionResource> resources);

    /**
     * 获取用户有权限访问的业务范围列表。
     */
    AppResourceScopeResult getAppResourceScopeList(User user, List<AppResourceScope> allAppResourceScopeList);

    /**
     * 获取业务访问权限申请 URL。
     */
    String getBusinessApplyUrl(String tenantId, AppResourceScope appResourceScope);
}
