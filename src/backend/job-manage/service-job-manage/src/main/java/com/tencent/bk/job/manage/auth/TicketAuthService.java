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

package com.tencent.bk.job.manage.auth;

import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.dto.AppResourceScope;

import java.util.List;

/**
 * 凭证相关操作鉴权接口
 */
public interface TicketAuthService {
    /**
     * 资源范围下创建凭证鉴权
     *
     * @param username      用户名
     * @param resourceScope 资源范围
     * @return 鉴权结果
     */
    AuthResult authCreateTicket(String username, AppResourceScope resourceScope);

    /**
     * 资源范围下管理凭证鉴权
     *
     * @param username      用户名
     * @param resourceScope 资源范围
     * @param ticketId      凭证ID
     * @param ticketName    凭证名称，如果传入为空，则会调用ResourceNameQueryService查询
     * @return 鉴权结果
     */
    AuthResult authManageTicket(String username,
                                AppResourceScope resourceScope,
                                String ticketId,
                                String ticketName);

    /**
     * 资源范围下使用凭证鉴权
     *
     * @param username      用户名
     * @param resourceScope 资源范围
     * @param ticketId      凭证ID
     * @param ticketName    凭证名称，如果传入为空，则会调用ResourceNameQueryService查询
     * @return 鉴权结果
     */
    AuthResult authUseTicket(String username,
                             AppResourceScope resourceScope,
                             String ticketId,
                             String ticketName);

    /**
     * 资源范围下管理凭证批量鉴权
     *
     * @param username      用户名
     * @param resourceScope 资源范围
     * @param ticketIdList  凭证ID列表
     * @return 有权限的凭证ID
     */
    List<String> batchAuthManageTicket(String username,
                                       AppResourceScope resourceScope,
                                       List<String> ticketIdList);

    /**
     * 资源范围下使用凭证批量鉴权
     *
     * @param username      用户名
     * @param resourceScope 资源范围
     * @param ticketIdList  凭证ID列表
     * @return 有权限的凭证ID
     */
    List<String> batchAuthUseTicket(String username,
                                    AppResourceScope resourceScope,
                                    List<String> ticketIdList);
}
