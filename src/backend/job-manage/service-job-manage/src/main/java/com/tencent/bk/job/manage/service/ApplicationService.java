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

package com.tencent.bk.job.manage.service;

import com.tencent.bk.job.common.annotation.DeprecatedAppLogic;
import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 业务Service
 */
public interface ApplicationService {

    /**
     * 根据资源范围获取Job业务ID
     *
     * @param resourceScope 资源范围
     * @return Job业务ID
     */
    Long getAppIdByScope(ResourceScope resourceScope);

    /**
     * 根据Job业务ID获取资源范围
     *
     * @param appId Job业务ID
     * @return 资源范围
     */
    ResourceScope getScopeByAppId(Long appId);

    /**
     * 批量根据业务ID列表获取资源范围
     *
     * @param appIds 业务ID列表
     * @return 业务ID与资源范围的映射关系Map
     */
    Map<Long, ResourceScope> getScopeByAppIds(Collection<Long> appIds);

    /**
     * 批量根据资源范围获取业务ID
     *
     * @param scopeList 资源范围列表
     * @return 资源范围与业务ID的映射关系Map
     */
    Map<ResourceScope, Long> getAppIdByScopeList(Collection<ResourceScope> scopeList);

    /**
     * 根据Job业务ID获取Job业务
     *
     * @param appId Job业务ID
     * @return Job业务
     * @throws NotFoundException 业务不存在
     */
    ApplicationDTO getAppByAppId(Long appId) throws NotFoundException;

    /**
     * 根据资源范围拉取业务信息
     *
     * @param scope 资源范围
     * @return 业务信息
     */
    ApplicationDTO getAppByScope(ResourceScope scope);

    /**
     * 根据资源范围拉取业务信息
     *
     * @param scopeType 资源范围类型
     * @param scopeId   资源范围ID
     * @return 业务
     */
    ApplicationDTO getAppByScope(String scopeType, String scopeId);

    /**
     * 根据Job业务ID批量查询Job业务
     *
     * @param appIds Job业务Id列表
     * @return Job业务列表
     */
    List<ApplicationDTO> listAppsByAppIds(Collection<Long> appIds);

    /**
     * 根据业务ID批量查询业务
     *
     * @param bizIds 业务Id列表
     * @return 业务列表
     */
    List<ApplicationDTO> listBizAppsByBizIds(Collection<Long> bizIds);

    /**
     * 根据当前业务Id查询包含该业务的业务集
     *
     * @param appId 业务ID
     * @return 包含该业务的业务集ID列表
     */
    List<Long> getBizSetAppIdsForBiz(Long appId);

    /**
     * 根据业务类型获取业务列表
     *
     * @param appType 业务类型
     * @return 业务列表
     */
    @DeprecatedAppLogic
    List<ApplicationDTO> listAppsByType(AppTypeEnum appType);

    /**
     * 根据资源范围类型获取业务列表
     *
     * @param scopeType 资源范围类型
     * @return 业务列表
     */
    List<ApplicationDTO> listAppsByScopeType(ResourceScopeTypeEnum scopeType);

    /**
     * 获取作业平台所有业务
     *
     * @return 业务列表
     */
    List<ApplicationDTO> listAllApps();

    /**
     * 判断用户是否有业务权限
     * <p>
     * 接入权限中心后不应使用本接口
     *
     * @param appId    业务 ID
     * @param username 用户名
     * @return 是否有业务权限
     */
    @DeprecatedAppLogic
    boolean checkAppPermission(long appId, String username);

    /**
     * 创建业务
     *
     * @param application 业务
     * @return 业务ID
     */
    Long createApp(ApplicationDTO application);

    /**
     * 创建业务-指定业务ID
     *
     * @param application 业务
     * @return 业务ID
     */
    @DeprecatedAppLogic
    Long createAppWithSpecifiedAppId(ApplicationDTO application);

    /**
     * 获取Job业务数量
     *
     * @return 业务数量
     */
    Integer countApps();

    /**
     * 更新业务
     *
     * @param application 业务
     */
    void updateApp(ApplicationDTO application);

    /**
     * 删除业务
     *
     * @param appId Job业务ID
     */
    void deleteApp(Long appId);

    /**
     * 恢复已删除的Job业务
     *
     * @param appId Job业务ID
     */
    void restoreDeletedApp(long appId);

    /**
     * 根据资源范围获取业务，包含已经被逻辑删除的业务
     *
     * @param scope 资源范围
     * @return 业务
     */
    ApplicationDTO getAppByScopeIncludingDeleted(ResourceScope scope);
}
