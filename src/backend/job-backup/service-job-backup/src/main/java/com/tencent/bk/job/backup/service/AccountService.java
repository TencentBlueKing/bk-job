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

package com.tencent.bk.job.backup.service;

import com.tencent.bk.job.manage.model.inner.ServiceAccountDTO;

import java.util.List;

/**
 * @since 24/11/2020 20:58
 */
public interface AccountService {
    /**
     * 按账号 ID 拉账号信息
     *
     * @param id 账号 ID
     * @return 账号信息
     */
    ServiceAccountDTO getAccountAliasById(Long id);

    /**
     * 按业务 ID 批量拉取账号信息
     *
     * @param username 用户名
     * @param appId    业务 ID
     * @return 账号信息列表
     */
    List<ServiceAccountDTO> listAccountByAppId(String username, Long appId);

    /**
     * 新建/修改账号
     *
     * @param username 用户名
     * @param appId    业务 ID
     * @param account  账号信息
     * @return 账号 ID
     */
    Long saveAccount(String username, Long appId, ServiceAccountDTO account);
}
