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
import com.tencent.bk.job.common.model.dto.AppResourceScope;

import java.util.List;

/**
 * 脚本相关操作鉴权接口
 */
public interface ScriptAuthService {
    /**
     * 资源范围下创建脚本鉴权
     *
     * @param username         用户名
     * @param appResourceScope 资源范围
     * @return 鉴权结果
     */
    AuthResult authCreateScript(String username, AppResourceScope appResourceScope);

    /**
     * 资源范围下查看脚本鉴权
     *
     * @param username         用户名
     * @param appResourceScope 资源范围
     * @param scriptId         脚本ID
     * @param scriptName       脚本名称，如果传入为空，则会调用ResourceNameQueryService查询
     * @return 鉴权结果
     */
    AuthResult authViewScript(String username,
                              AppResourceScope appResourceScope,
                              String scriptId,
                              String scriptName);

    /**
     * 资源范围下管理脚本鉴权
     *
     * @param username         用户名
     * @param appResourceScope 资源范围
     * @param scriptId         脚本ID
     * @param scriptName       脚本名称，如果传入为空，则会调用ResourceNameQueryService查询
     * @return 鉴权结果
     */
    AuthResult authManageScript(String username,
                                AppResourceScope appResourceScope,
                                String scriptId,
                                String scriptName);

    /**
     * 资源范围下查看脚本批量鉴权
     *
     * @param username         用户名
     * @param appResourceScope 资源范围
     * @param scriptIdList     脚本ID列表
     * @return 有权限的脚本ID
     */
    List<String> batchAuthViewScript(String username,
                                     AppResourceScope appResourceScope,
                                     List<String> scriptIdList);

    /**
     * 资源范围下管理脚本批量鉴权
     *
     * @param username         用户名
     * @param appResourceScope 资源范围
     * @param scriptIdList     脚本ID列表
     * @return 有权限的脚本ID
     */
    List<String> batchAuthManageScript(String username,
                                       AppResourceScope appResourceScope,
                                       List<String> scriptIdList);

    /**
     * 资源范围下管理脚本批量鉴权并返回鉴权结果
     *
     * @param username         用户名
     * @param appResourceScope 资源范围
     * @param scriptIdList     脚本ID列表
     * @return 鉴权结果
     */
    AuthResult batchAuthResultManageScript(String username,
                                           AppResourceScope appResourceScope,
                                           List<String> scriptIdList);

    /**
     * 资源范围下查看脚本批量鉴权并返回鉴权结果
     *
     * @param username         用户名
     * @param appResourceScope 资源范围
     * @param scriptIdList     脚本ID列表
     * @return 鉴权结果
     */
    AuthResult batchAuthResultViewScript(String username,
                                         AppResourceScope appResourceScope,
                                         List<String> scriptIdList);

    /**
     * 注册脚本实例
     *
     * @param id      资源实例 ID
     * @param name    资源实例名称
     * @param creator 资源实例创建者
     * @return 是否注册成功
     */
    boolean registerScript(String id, String name, String creator);
}
