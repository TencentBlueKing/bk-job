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

package com.tencent.bk.job.execute.auth;

import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;

import java.util.Collection;

/**
 * 作业执行鉴权服务
 */
public interface ExecuteAuthService {
    /**
     * 快速分发文件鉴权
     *
     * @param user             用户
     * @param appResourceScope 业务范围
     * @param executeTarget    执行目标
     * @return 鉴权结果
     */
    AuthResult authFastPushFile(User user, AppResourceScope appResourceScope, ExecuteTargetDTO executeTarget);

    /**
     * 快速执行脚本鉴权
     *
     * @param user             用户
     * @param appResourceScope 业务范围
     * @param executeTarget    执行目标
     * @return 鉴权结果
     */
    AuthResult authFastExecuteScript(User user,
                                     AppResourceScope appResourceScope,
                                     ExecuteTargetDTO executeTarget);

    /**
     * 执行业务脚本鉴权
     *
     * @param user             用户
     * @param appResourceScope 业务范围
     * @param scriptId         脚本ID
     * @param scriptName       脚本名称，如果传入为空，则会调用ResourceNameQueryService查询
     * @param executeTarget    执行目标
     * @return 鉴权结果
     */
    AuthResult authExecuteAppScript(User user,
                                    AppResourceScope appResourceScope,
                                    String scriptId,
                                    String scriptName,
                                    ExecuteTargetDTO executeTarget);

    /**
     * 执行公共脚本鉴权
     *
     * @param user             用户
     * @param appResourceScope 业务范围
     * @param scriptId         脚本ID
     * @param scriptName       脚本名称，如果传入为空，则会调用ResourceNameQueryService查询
     * @param executeTarget    执行目标
     * @return 鉴权结果
     */
    AuthResult authExecutePublicScript(User user,
                                       AppResourceScope appResourceScope,
                                       String scriptId,
                                       String scriptName,
                                       ExecuteTargetDTO executeTarget);

    /**
     * 执行执行方案鉴权
     *
     * @param user             用户
     * @param appResourceScope 业务范围
     * @param planId           执行方案ID
     * @param planName         执行方案名称，如果传入为空，则会调用ResourceNameQueryService查询
     * @param executeTarget    执行目标
     * @return 鉴权结果
     */
    AuthResult authExecutePlan(User user,
                               AppResourceScope appResourceScope,
                               Long templateId,
                               Long planId,
                               String planName,
                               ExecuteTargetDTO executeTarget);

    /**
     * 作业模板调试鉴权
     *
     * @param user             用户
     * @param appResourceScope 业务范围
     * @param templateId       作业模板ID
     * @param executeTarget    执行目标
     * @return 鉴权结果
     */
    AuthResult authDebugTemplate(User user,
                                 AppResourceScope appResourceScope,
                                 Long templateId,
                                 ExecuteTargetDTO executeTarget);

    /**
     * 作业执行实例查看权限鉴权
     *
     * @param user             用户
     * @param appResourceScope 业务范围
     * @param taskInstance     作业实例
     * @throws PermissionDeniedException 用户无权限
     */
    void authViewTaskInstance(User user, AppResourceScope appResourceScope, TaskInstanceDTO taskInstance)
        throws PermissionDeniedException;

    /**
     * 检查是否拥有作业执行实例查看权限
     *
     * @param user             用户
     * @param appResourceScope 业务范围
     * @param taskInstance     作业实例
     */
    AuthResult checkViewTaskInstancePermission(User user,
                                               AppResourceScope appResourceScope,
                                               TaskInstanceDTO taskInstance);

    /**
     * 用户是否具有查看所有作业实例权限
     *
     * @param user             用户
     * @param appResourceScope 业务范围
     * @return 鉴权结果
     */
    AuthResult authViewAllTaskInstance(User user, AppResourceScope appResourceScope);

    /**
     * 账号执行权限鉴权
     *
     * @param user             用户
     * @param appResourceScope 业务范围
     * @param accountId        账号ID
     * @return 鉴权结果
     */
    AuthResult authAccountExecutable(User user, AppResourceScope appResourceScope, Long accountId);

    /**
     * 账号执行权限鉴权
     *
     * @param user             用户
     * @param appResourceScope 业务范围
     * @param accountIds       账号ID列表
     * @return 鉴权结果
     */
    AuthResult batchAuthAccountExecutable(User user,
                                          AppResourceScope appResourceScope,
                                          Collection<Long> accountIds);

}
