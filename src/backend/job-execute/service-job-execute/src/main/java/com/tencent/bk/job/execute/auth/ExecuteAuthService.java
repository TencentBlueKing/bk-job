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

package com.tencent.bk.job.execute.auth;

import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
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
     * @param username         用户名
     * @param appResourceScope 业务范围
     * @param servers          服务器
     * @return 鉴权结果
     */
    AuthResult authFastPushFile(String username, AppResourceScope appResourceScope, ServersDTO servers);

    /**
     * 快速执行脚本鉴权
     *
     * @param username         用户名
     * @param appResourceScope 业务范围
     * @param servers          服务器
     * @return 鉴权结果
     */
    AuthResult authFastExecuteScript(String username, AppResourceScope appResourceScope, ServersDTO servers);

    /**
     * 执行业务脚本鉴权
     *
     * @param username         用户名
     * @param appResourceScope 业务范围
     * @param scriptId         脚本ID
     * @param scriptName       脚本名称，如果传入为空，则会调用ResourceNameQueryService查询
     * @param servers          服务器
     * @return 鉴权结果
     */
    AuthResult authExecuteAppScript(String username, AppResourceScope appResourceScope,
                                    String scriptId, String scriptName, ServersDTO servers);

    /**
     * 执行公共脚本鉴权
     *
     * @param username         用户名
     * @param appResourceScope 业务范围
     * @param scriptId         脚本ID
     * @param scriptName       脚本名称，如果传入为空，则会调用ResourceNameQueryService查询
     * @param servers          服务器
     * @return 鉴权结果
     */
    AuthResult authExecutePublicScript(String username, AppResourceScope appResourceScope,
                                       String scriptId, String scriptName, ServersDTO servers);

    /**
     * 执行执行方案鉴权
     *
     * @param username         用户名
     * @param appResourceScope 业务范围
     * @param planId           执行方案ID
     * @param planName         执行方案名称，如果传入为空，则会调用ResourceNameQueryService查询
     * @param servers          服务器
     * @return 鉴权结果
     */
    AuthResult authExecutePlan(String username, AppResourceScope appResourceScope, Long templateId,
                               Long planId, String planName, ServersDTO servers);

    /**
     * 作业模板调试鉴权
     *
     * @param username         用户名
     * @param appResourceScope 业务范围
     * @param templateId       作业模板ID
     * @param servers          服务器
     * @return 鉴权结果
     */
    AuthResult authDebugTemplate(String username, AppResourceScope appResourceScope,
                                 Long templateId, ServersDTO servers);

    /**
     * 作业执行实例查看权限鉴权
     *
     * @param username         用户名
     * @param appResourceScope 业务范围
     * @param taskInstance     作业实例
     * @throws PermissionDeniedException 用户无权限
     */
    void authViewTaskInstance(String username, AppResourceScope appResourceScope, TaskInstanceDTO taskInstance)
        throws PermissionDeniedException;

    /**
     * 检查是否拥有作业执行实例查看权限
     *
     * @param username         用户名
     * @param appResourceScope 业务范围
     * @param taskInstance     作业实例
     */
    AuthResult checkViewTaskInstancePermission(String username,
                                               AppResourceScope appResourceScope,
                                               TaskInstanceDTO taskInstance);

    /**
     * 用户是否具有查看所有作业实例权限
     *
     * @param username         用户名
     * @param appResourceScope 业务范围
     * @return 鉴权结果
     */
    AuthResult authViewAllTaskInstance(String username, AppResourceScope appResourceScope);

    /**
     * 账号执行权限鉴权
     *
     * @param accountId 账号ID
     * @return 鉴权结果
     */
    AuthResult authAccountExecutable(String username, AppResourceScope appResourceScope, Long accountId);

    /**
     * 账号执行权限鉴权
     *
     * @param accountIds 账号ID列表
     * @return 鉴权结果
     */
    AuthResult batchAuthAccountExecutable(String username, AppResourceScope appResourceScope,
                                          Collection<Long> accountIds);

}
