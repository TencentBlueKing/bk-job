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

package com.tencent.bk.job.manage.dao;

import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.manage.common.consts.account.AccountCategoryEnum;
import com.tencent.bk.job.manage.common.consts.account.AccountTypeEnum;
import com.tencent.bk.job.manage.model.dto.AccountDTO;

import java.util.List;

/**
 * 账号管理DAO
 */
public interface AccountDAO {
    /**
     * 新增账号
     *
     * @param account
     * @return 账号ID
     */
    long saveAccountWithId(AccountDTO account);

    /**
     * 新增账号
     *
     * @param account
     * @return 账号ID
     */
    long saveAccount(AccountDTO account);

    /**
     * 根据ID获取账号信息
     *
     * @param accountId
     * @return
     */
    AccountDTO getAccountById(Long accountId);

    /**
     * 根据appId与account获取账号信息
     *
     * @param appId
     * @param account
     * @return
     */
    AccountDTO getAccountByAccount(Long appId, String account);

    /**
     * 更新账号
     *
     * @param account
     */
    void updateAccount(AccountDTO account);

    /**
     * 删除账号
     *
     * @param accountId
     */
    void deleteAccount(Long accountId);

    /**
     * 分页查询账号列表
     *
     * @param accountQuery
     * @param baseSearchCondition
     * @return
     */
    PageData<AccountDTO> listPageAccount(AccountDTO accountQuery, BaseSearchCondition baseSearchCondition);

    /**
     * 分页搜索账号列表
     *
     * @param keyword
     * @param baseSearchCondition
     * @return
     */
    PageData<AccountDTO> searchPageAccount(Long appId, String keyword, BaseSearchCondition baseSearchCondition);

    /**
     * 获取业务下所有指定分类的账号
     *
     * @param appId    业务ID
     * @param category 账号类型，如果传入null，表示所有类型
     * @return
     */
    List<AccountDTO> listAllAppAccount(Long appId, AccountCategoryEnum category,
                                       BaseSearchCondition baseSearchCondition);

    Integer countAllAppAccount(Long appId, AccountCategoryEnum category);

    /**
     * 根据别名获取账号信息
     *
     * @param appId    业务ID
     * @param category 账号类型
     * @param alias    账号别名
     * @return 账号
     */
    AccountDTO getAccount(Long appId, AccountCategoryEnum category, String alias);

    AccountDTO getAccount(Long appId, AccountCategoryEnum category, AccountTypeEnum type, String account, String alias);

    /**
     * 账号是否被任一脚本步骤引用
     *
     * @param accountId 账号ID
     * @return 是否被引用
     */
    boolean isAccountRefByAnyScriptStep(Long accountId);

    /**
     * 账号是否被任一文件步骤引用
     *
     * @param accountId 账号ID
     * @return 是否被引用
     */
    boolean isAccountRefByAnyFileStep(Long accountId);

    /**
     * 账号是否被任一源文件引用
     *
     * @param accountId 账号ID
     * @return 是否被引用
     */
    boolean isAccountRefByAnySourceFile(Long accountId);

    /**
     * 判断系统账号是否被DB账号引用
     *
     * @param accountId 系统账号ID
     * @return
     */
    boolean isAccountRefByDbAccount(Long accountId);

    Integer countAccounts(AccountTypeEnum accountType);

    List<AccountDTO> listAccountByAccountCategory(AccountCategoryEnum accountCategoryEnum);

    void batchUpdateDbAccountPassword(List<AccountDTO> updatedAccounts);
}
