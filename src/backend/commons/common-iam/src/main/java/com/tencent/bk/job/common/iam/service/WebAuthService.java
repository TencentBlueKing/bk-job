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

package com.tencent.bk.job.common.iam.service;

import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.model.PermissionActionResource;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.model.permission.AuthResultVO;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.dto.resource.ResourceDTO;

import java.util.List;

/**
 * 鉴权服务-web
 */
public interface WebAuthService {

    AuthService getAuthService();

    void setResourceAppInfoQueryService(ResourceAppInfoQueryService resourceAppInfoQueryService);

    /**
     * 关联资源操作鉴权
     *
     * @param returnApplyUrl 是否返回权限申请url
     * @param username       用户名
     * @param actionId       操作ID
     * @param resourceType   资源类型
     * @param resourceId     资源ID
     * @param pathInfo       资源路径
     * @return
     */
    AuthResultVO auth(boolean returnApplyUrl, String username, String actionId, ResourceTypeEnum resourceType,
                      String resourceId, PathInfoDTO pathInfo);

    /**
     * 多个操作鉴权
     *
     * @param isReturnApplyUrl 是否返回权限申请url
     * @param username         用户名
     * @param actionResources  操作列表
     * @return 鉴权结果
     */
    AuthResultVO auth(boolean isReturnApplyUrl, String username, List<PermissionActionResource> actionResources);

    String getApplyUrl(List<PermissionActionResource> permissionActionResources);

    AuthResultVO toAuthResultVO(AuthResult authResult);

    /**
     * 注册资源实例
     *
     * @param id        资源实例 ID
     * @param name      资源实例名称
     * @param type      资源实例类型
     * @param creator   资源实例创建者
     * @param ancestors 资源实例的祖先
     * @return 是否注册成功
     */
    boolean registerResource(String id, String name, String type, String creator, List<ResourceDTO> ancestors);
}
