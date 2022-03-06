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

package com.tencent.bk.job.execute.service;

import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.execute.model.ServersDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;

import java.util.Collection;

/**
 * 作业执行鉴权服务
 */
public interface ExecuteAuthService {
    /**
     * 快速分发文件鉴权
     *
     * @param username 用户名
     * @param appId    业务ID
     * @param servers  服务器
     * @return 鉴权结果
     */
    AuthResult authFastPushFile(String username, Long appId, ServersDTO servers);

    /**
     * 快速执行脚本鉴权
     *
     * @param username 用户名
     * @param appId    业务ID
     * @param servers  服务器
     * @return 鉴权结果
     */
    AuthResult authFastExecuteScript(String username, Long appId, ServersDTO servers);

    /**
     * 执行业务脚本鉴权
     *
     * @param username   用户名
     * @param appId      业务ID
     * @param scriptId   脚本ID
     * @param scriptName 脚本名称，如果传入为空，则会调用ResourceNameQueryService查询
     * @param servers    服务器
     * @return 鉴权结果
     */
    AuthResult authExecuteAppScript(String username, Long appId,
                                    String scriptId, String scriptName, ServersDTO servers);

    /**
     * 执行公共脚本鉴权
     *
     * @param username   用户名
     * @param appId      业务ID
     * @param scriptId   脚本ID
     * @param scriptName 脚本名称，如果传入为空，则会调用ResourceNameQueryService查询
     * @param servers    服务器
     * @return 鉴权结果
     */
    AuthResult authExecutePublicScript(String username, Long appId,
                                       String scriptId, String scriptName, ServersDTO servers);

    /**
     * 执行执行方案鉴权
     *
     * @param username 用户名
     * @param appId    业务ID
     * @param planId   执行方案ID
     * @param planName 执行方案名称，如果传入为空，则会调用ResourceNameQueryService查询
     * @param servers  服务器
     * @return 鉴权结果
     */
    AuthResult authExecutePlan(String username, Long appId, Long templateId,
                               Long planId, String planName, ServersDTO servers);

    /**
     * 作业模板调试鉴权
     *
     * @param username   用户名
     * @param appId      业务ID
     * @param templateId 作业模板ID
     * @param servers    服务器
     * @return 鉴权结果
     */
    AuthResult authDebugTemplate(String username, Long appId,
                                 Long templateId, ServersDTO servers);

    /**
     * 作业执行实例查看权限鉴权
     *
     * @param username       用户名
     * @param resourceScope          范畴
     * @param taskInstanceId 作业实例ID
     * @return 鉴权结果
     */
    AuthResult authViewTaskInstance(String username, ResourceScope resourceScope, long taskInstanceId);

    /**
     * 作业执行实例查看权限鉴权
     *
     * @param username     用户名
     * @param appId        业务ID
     * @param taskInstance 作业实例
     * @return 鉴权结果
     */
    AuthResult authViewTaskInstance(String username, Long appId, TaskInstanceDTO taskInstance);

    /**
     * 用户是否具有查看所有作业实例权限
     *
     * @param username 用户名
     * @param appId    业务ID
     * @return 鉴权结果
     */
    AuthResult authViewAllTaskInstance(String username, Long appId);

    /**
     * 账号执行权限鉴权
     *
     * @param accountId 账号ID
     * @return 鉴权结果
     */
    AuthResult authAccountExecutable(String username, Long appId, Long accountId);

    /**
     * 账号执行权限鉴权
     *
     * @param accountIds 账号ID列表
     * @return 鉴权结果
     */
    AuthResult batchAuthAccountExecutable(String username, Long appId, Collection<Long> accountIds);

}
