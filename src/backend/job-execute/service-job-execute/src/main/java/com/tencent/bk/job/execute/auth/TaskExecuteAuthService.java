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

import com.tencent.bk.job.common.app.ResourceScope;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.execute.model.ServersDTO;

/**
 * 任务执行相关操作鉴权接口
 */
public interface TaskExecuteAuthService {

    /**
     * 资源范围下快速分发文件鉴权
     *
     * @param username      用户名
     * @param resourceScope 资源范围
     * @param servers       执行目标
     * @return 鉴权结果
     */
    AuthResult authFastPushFile(String username,
                                ResourceScope resourceScope,
                                ServersDTO servers);

    /**
     * 快速执行脚本鉴权
     *
     * @param username      用户名
     * @param resourceScope 资源范围
     * @param servers       服务器
     * @return 鉴权结果
     */
    AuthResult authFastExecuteScript(String username,
                                     ResourceScope resourceScope,
                                     ServersDTO servers);

    /**
     * 查看执行历史鉴权
     *
     * @param username      用户名
     * @param resourceScope 资源范围
     * @return 鉴权结果
     */
    AuthResult authViewHistory(String username,
                               ResourceScope resourceScope);

    /**
     * 资源范围下执行脚本鉴权
     *
     * @param username      用户名
     * @param resourceScope 资源范围
     * @param scriptId      脚本ID
     * @param scriptName    脚本名称，如果传入为空，则会调用ResourceNameQueryService查询
     * @param servers       目标服务器
     * @return 鉴权结果
     */
    AuthResult authExecuteScript(String username,
                                 ResourceScope resourceScope,
                                 String scriptId,
                                 String scriptName,
                                 ServersDTO servers);

    /**
     * 资源范围下运行执行方案鉴权
     *
     * @param username      用户名
     * @param resourceScope 资源范围
     * @param jobTemplateId 作业模板ID
     * @param jobPlanId     执行方案ID
     * @param jobPlanName   执行方案名称，如果传入为空，则会调用ResourceNameQueryService查询
     * @return 鉴权结果
     */
    AuthResult authLaunchJobPlan(String username,
                                 ResourceScope resourceScope,
                                 Long jobTemplateId,
                                 Long jobPlanId,
                                 String jobPlanName);

    /**
     * 执行公共脚本鉴权
     *
     * @param username   用户名
     * @param scriptId   公共脚本ID
     * @param serversDTO 执行目标
     * @return 鉴权结果
     */
    AuthResult authExecutePublicScript(String username,
                                       String scriptId,
                                       ServersDTO serversDTO);

    /**
     * 高危语句拦截记录查看鉴权
     *
     * @param username 用户名
     * @return 鉴权结果
     */
    AuthResult authHighRiskDetectRecord(String username);
}
