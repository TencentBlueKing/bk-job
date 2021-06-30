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

import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.execute.model.AccountDTO;
import com.tencent.bk.job.manage.common.consts.account.AccountCategoryEnum;

/**
 * 账号服务
 */
public interface AccountService {
    /**
     * 获取账号信息
     *
     * @param accountId
     * @return
     */
    AccountDTO getAccountById(Long accountId) throws ServiceException;

    /**
     * 获取某个账号名的账号
     *
     * @param appId        业务ID
     * @param accountAlias 账号名
     * @return 账号信息
     * @throws ServiceException
     */
    AccountDTO getAccountByAccountName(Long appId, String accountAlias) throws ServiceException;

    /**
     * 根据业务ID和账号别名获取系统账号信息
     *
     * @param alias
     * @param appId
     * @return
     */
    AccountDTO getSystemAccountByAlias(String alias, Long appId) throws ServiceException;

    /**
     * 根据业务ID和账号别名获取系统账号信息
     *
     * @param accountCategory 账号类型
     * @param appId           业务ID
     * @param alias           账号别名
     * @return
     */
    AccountDTO getAccountByAlias(AccountCategoryEnum accountCategory, Long appId,
                                 String alias) throws ServiceException;

    /**
     * 根据业务ID和账号别名获取账号信息
     *
     * @param accountId       账号ID
     * @param accountCategory 账号类型
     * @param accountAlias    账号别名
     * @param appId           业务ID
     * @return 账号信息
     */
    AccountDTO getAccount(Long accountId, AccountCategoryEnum accountCategory, String accountAlias,
                          Long appId) throws ServiceException;

    /**
     * 根据业务ID和账号别名获取DB账号信息。使用缓存
     *
     * @param accountId       账号ID
     * @param accountCategory 账号类型
     * @param accountAlias    账号别名
     * @param appId           业务ID
     * @return 账号信息
     */
    AccountDTO getAccountPreferCache(Long accountId, AccountCategoryEnum accountCategory, String accountAlias,
                                     Long appId) throws ServiceException;
}
