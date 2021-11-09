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

import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.dto.AppIdResult;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.model.PermissionActionResource;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.dto.resource.ResourceDTO;

import java.util.List;


/**
 * 公共鉴权服务
 */
public interface AuthService {

    void setResourceAppInfoQueryService(ResourceAppInfoQueryService resourceAppInfoQueryService);

    void setResourceNameQueryService(ResourceNameQueryService resourceNameQueryService);

    /**
     * 无关联资源、单个操作操作鉴权
     *
     * @param returnApplyUrl 是否返回权限申请url
     * @param username       用户名
     * @param actionId       操作ID
     * @return 鉴权结果
     */
    AuthResult auth(boolean returnApplyUrl, String username, String actionId);

    /**
     * 关联单个资源、单个操作鉴权
     *
     * @param returnApplyUrl 是否返回权限申请url
     * @param username       用户名
     * @param actionId       操作ID
     * @param resourceType   资源类型
     * @param resourceId     资源ID
     * @param pathInfo       资源路径
     * @return 鉴权结果
     */
    AuthResult auth(boolean returnApplyUrl, String username, String actionId, ResourceTypeEnum resourceType,
                    String resourceId, PathInfoDTO pathInfo);

    /**
     * 业务集/全业务鉴权
     *
     * @param username       用户名
     * @param resourceType   资源类型
     * @param resourceId     资源ID
     * @return 鉴权结果
     */
    boolean authSpecialAppByMaintainer(String username, ResourceTypeEnum resourceType,
                                       String resourceId);
    /**
     * 多个操作鉴权
     *
     * @param isReturnApplyUrl 是否返回权限申请url
     * @param username         用户名
     * @param actionResources  操作列表
     * @return 鉴权结果
     */
    AuthResult auth(boolean isReturnApplyUrl, String username, List<PermissionActionResource> actionResources);

    /**
     * 批量鉴权：用于Job自有非业务下资源（如运营视图）的批量鉴权
     *
     * @param username       用户名
     * @param actionId       操作ID
     * @param resourceType   资源类型
     * @param resourceIdList 资源ID列表
     * @return 有权限的资源ID列表
     */
    List<String> batchAuth(String username, String actionId, ResourceTypeEnum resourceType,
                           List<String> resourceIdList);

    /**
     * 批量鉴权
     *
     * @param username       用户名
     * @param actionId       操作ID
     * @param appId          业务ID
     * @param resourceType   资源类型
     * @param resourceIdList 资源ID列表
     * @return 有权限的资源ID列表
     */
    List<String> batchAuth(String username, String actionId, Long appId, ResourceTypeEnum resourceType,
                           List<String> resourceIdList);

    /**
     * 批量鉴权
     *
     * @param username  用户名
     * @param actionId  操作ID
     * @param appId     业务ID
     * @param resources 资源列表
     * @return 鉴权结果
     */
    AuthResult batchAuthResources(String username, String actionId, Long appId, List<PermissionResource> resources);

    /**
     * 批量鉴权
     *
     * @param username     用户名
     * @param actionId     操作 ID
     * @param appId        业务 ID
     * @param resourceList 资源列表
     * @return 有权限的资源 ID 列表
     */
    List<String> batchAuth(String username, String actionId, Long appId, List<PermissionResource> resourceList);

    AppIdResult getAppIdList(String username, List<Long> allAppIdList);

    /**
     * 获取权限申请URL
     *
     * @param actionId 操作ID
     * @return 权限申请URL
     */
    String getApplyUrl(String actionId);

    /**
     * 获取权限申请URL
     *
     * @param actionId     操作ID
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @return 权限申请URL
     */
    String getApplyUrl(String actionId, ResourceTypeEnum resourceType, String resourceId);

    /**
     * 获取权限申请URL
     *
     * @param permissionActionResources 依赖的权限
     * @return 权限申请URL
     */
    String getApplyUrl(List<PermissionActionResource> permissionActionResources);

    /**
     * 构造第三方鉴权失败返回结果
     *
     * @param permissionActionResources 依赖的权限
     * @return 第三方鉴权失败返回结果
     */
    <T> EsbResp<T> buildEsbAuthFailResp(List<PermissionActionResource> permissionActionResources);

    /**
     * 构造第三方鉴权失败返回结果
     *
     * @param exception 鉴权失败返回的异常
     * @return 第三方鉴权失败返回结果
     */
    <T> EsbResp<T> buildEsbAuthFailResp(PermissionDeniedException exception);

    String getBusinessApplyUrl(Long appId);

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
