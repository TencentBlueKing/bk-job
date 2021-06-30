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

package com.tencent.bk.job.manage.service;

import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.manage.common.consts.account.AccountCategoryEnum;
import com.tencent.bk.job.manage.common.consts.account.AccountTypeEnum;
import com.tencent.bk.job.manage.model.dto.AccountDTO;
import com.tencent.bk.job.manage.model.web.request.AccountCreateUpdateReq;

import java.util.List;

/**
 * 账号管理服务
 */
public interface AccountService {
    /**
     * 新增账号
     *
     * @param account
     * @return 账号ID
     */
    long saveAccount(AccountDTO account) throws ServiceException;

    /**
     * 根据ID获取账号信息
     *
     * @param accountId
     * @return
     */
    AccountDTO getAccountById(Long accountId) throws ServiceException;

    /**
     * 根据ID获取账号信息
     *
     * @param appId
     * @param account
     * @return
     */
    AccountDTO getAccountByAccount(Long appId, String account) throws ServiceException;

    /**
     * 更新账号
     *
     * @param account
     */
    void updateAccount(AccountDTO account) throws ServiceException;

    /**
     * 删除账号
     *
     * @param accountId
     */
    void deleteAccount(Long accountId) throws ServiceException;

    /**
     * 分页查询账号列表
     *
     * @param accountQuery
     * @param baseSearchCondition
     * @return
     */
    PageData<AccountDTO> listPageAccount(AccountDTO accountQuery, BaseSearchCondition baseSearchCondition)
        throws ServiceException;

    /**
     * 分页搜索账号列表
     *
     * @param keyword
     * @param baseSearchCondition
     * @return
     */
    PageData<AccountDTO> searchPageAccount(
        Long appId, String keyword,
        BaseSearchCondition baseSearchCondition) throws ServiceException;

    /**
     * 根据别名获取账号信息
     *
     * @param appId
     * @param category
     * @param alias
     * @return
     */
    AccountDTO getAccount(Long appId, AccountCategoryEnum category, String alias);

    /**
     * 获取业务下账号列表
     *
     * @param appId    因为ID
     * @param category 账号类型，如果传入null，表示所有类型
     * @return 账号列表
     */
    List<AccountDTO> listAllAppAccount(Long appId, AccountCategoryEnum category);

    /**
     * 获取业务下账号列表
     *
     * @param appId    因为ID
     * @param category 账号类型，如果传入null，表示所有类型
     * @return 账号列表
     */
    List<AccountDTO> listAllAppAccount(Long appId, AccountCategoryEnum category,
                                       BaseSearchCondition baseSearchCondition);

    Integer countAllAppAccount(Long appId, AccountCategoryEnum category);

    boolean isAccountRefByAnyStep(Long accountId);

    /**
     * 系统账号是否被DB账号依赖
     *
     * @param accountId 系统账号ID
     * @return
     */
    boolean isSystemAccountRefByDbAccount(Long accountId);

    boolean checkCreateParam(AccountCreateUpdateReq accountCreateUpdateReq, boolean checkAlias,
                             boolean checkAccountName);

    AccountDTO buildCreateAccountDTO(String username, long appId, AccountCreateUpdateReq accountCreateUpdateReq);

    Integer countAccounts(AccountTypeEnum accountType);

    void createDefaultAccounts(long appId);
}
