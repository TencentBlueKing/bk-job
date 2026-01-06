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

package com.tencent.bk.job.manage.auth;

import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.User;

import java.util.List;

/**
 * 资源范围无关的相关操作鉴权接口
 */
public interface NoResourceScopeAuthService {
    /**
     * 创建IP白名单鉴权
     *
     * @param user 用户
     * @return 鉴权结果
     */
    AuthResult authCreateWhiteList(User user);

    /**
     * 管理IP白名单鉴权
     *
     * @param user 用户
     * @return 鉴权结果
     */
    AuthResult authManageWhiteList(User user);

    /**
     * 创建公共脚本鉴权
     *
     * @param user 用户
     * @return 鉴权结果
     */
    AuthResult authCreatePublicScript(User user);

    /**
     * 管理公共脚本鉴权
     *
     * @param user     用户
     * @param scriptId 公共脚本ID
     * @return 鉴权结果
     */
    AuthResult authManagePublicScript(User user, String scriptId);

    /**
     * 管理公共脚本批量鉴权
     *
     * @param user         用户
     * @param scriptIdList 公共脚本ID列表
     * @return 有权限的公共脚本ID
     */
    List<String> batchAuthManagePublicScript(User user, List<String> scriptIdList);

    /**
     * 管理公共脚本批量鉴权并返回鉴权结果
     *
     * @param user         用户
     * @param scriptIdList 公共脚本ID列表
     * @return 鉴权结果
     */
    AuthResult batchAuthResultManagePublicScript(User user, List<String> scriptIdList);

    /**
     * 全局设置鉴权
     *
     * @param user 用户
     * @return 鉴权结果
     */
    AuthResult authGlobalSetting(User user);

    /**
     * 运营视图查看鉴权
     *
     * @param user        用户
     * @param dashBoardId 运营视图ID
     * @return 鉴权结果
     */
    AuthResult authViewDashBoard(User user, String dashBoardId);

    /**
     * 服务状态查看鉴权
     *
     * @param user 用户
     * @return 鉴权结果
     */
    AuthResult authViewServiceState(User user);

    /**
     * 高危语句规则管理鉴权
     *
     * @param user 用户
     * @return 鉴权结果
     */
    AuthResult authHighRiskDetectRule(User user);

    /**
     * 高危语句拦截记录查看鉴权
     *
     * @param user 用户
     * @return 鉴权结果
     */
    AuthResult authHighRiskDetectRecord(User user);

    /**
     * 注册公共脚本实例
     *
     * @param creator 资源实例创建者
     * @param id      资源实例 ID
     * @param name    资源实例名称
     * @return 是否注册成功
     */
    boolean registerPublicScript(User creator, String id, String name);
}
