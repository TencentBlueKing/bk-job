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
 * 任务相关操作鉴权接口
 */
public interface TaskAuthService {

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
}
