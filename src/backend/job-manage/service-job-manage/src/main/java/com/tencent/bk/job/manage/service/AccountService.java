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

package com.tencent.bk.job.manage.service;

import com.tencent.bk.job.common.constant.AccountCategoryEnum;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.manage.api.common.constants.account.AccountTypeEnum;
import com.tencent.bk.job.manage.model.dto.AccountDTO;
import com.tencent.bk.job.manage.model.dto.AccountDisplayDTO;
import com.tencent.bk.job.manage.model.web.request.AccountCreateUpdateReq;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 账号管理服务
 */
public interface AccountService {
    /**
     * 新增账号
     *
     * @param account 新增的账号信息
     * @return 新增的账号
     */
    AccountDTO createAccount(AccountDTO account);

    /**
     * 新增账号
     *
     * @param user    用户账号
     * @param account 新增的账号信息
     * @return 新增的账号
     */
    AccountDTO createAccount(User user, AccountDTO account);

    /**
     * 根据ID获取账号信息
     *
     * @param accountId 账号Id
     * @return 账号
     */
    AccountDTO getAccountById(Long accountId);

    /**
     * 根据ID获取账号信息
     *
     * @param user      用户账号
     * @param appId     业务 ID
     * @param accountId 账号Id
     * @return 账号
     */
    AccountDTO getAccount(User user, long appId, Long accountId);

    /**
     * 根据ID获取账号信息
     *
     * @param appId     业务 ID
     * @param accountId 账号Id
     * @return 账号
     */
    AccountDTO getAccount(long appId, Long accountId);

    /**
     * 根据ID批量获取账号信息
     *
     * @param accountIds 账号集合
     * @return Map<账号Id ， 账号内容>
     */
    Map<Long, AccountDisplayDTO> getAccountDisplayInfoMapByIds(Collection<Long> accountIds);

    /**
     * 根据ID获取账号信息
     *
     * @param appId   业务ID
     * @param account 账号名
     * @return 账号
     */
    AccountDTO getAccountByAccount(Long appId, String account);

    /**
     * 更新账号
     *
     * @param user    用户账号
     * @param account 更新账号内容
     * @return 更新完的账号信息
     */
    AccountDTO updateAccount(User user, AccountDTO account);

    /**
     * 删除账号
     *
     * @param user      用户账号
     * @param appId     业务 ID
     * @param accountId 账号ID
     */
    void deleteAccount(User user, long appId, Long accountId);

    /**
     * 分页查询账号列表
     *
     * @param baseSearchCondition 基础查询条件
     * @return 账号分页
     */
    PageData<AccountDTO> listPageAccount(AccountDTO accountQuery, BaseSearchCondition baseSearchCondition);

    /**
     * 分页搜索账号列表
     *
     * @param keyword             关键字
     * @param baseSearchCondition 基础查询条件
     * @return 账号分页
     */
    PageData<AccountDTO> searchPageAccount(
        Long appId, String keyword,
        BaseSearchCondition baseSearchCondition);

    /**
     * 根据别名获取账号信息
     *
     * @param appId    业务ID
     * @param category 账号类别
     * @param alias    账号别名
     * @return 账号
     */
    AccountDTO getAccount(Long appId, AccountCategoryEnum category, String alias);

    /**
     * 获取业务下账号列表
     *
     * @param appId    因为ID
     * @param category 账号类型，如果传入null，表示所有类型
     * @return 账号列表
     */
    List<AccountDTO> listAppAccount(Long appId, AccountCategoryEnum category);

    /**
     * 获取业务下账号列表
     *
     * @param appId    因为ID
     * @param category 账号类型，如果传入null，表示所有类型
     * @return 账号列表
     */
    List<AccountDTO> listAppAccount(Long appId,
                                    AccountCategoryEnum category,
                                    String account,
                                    String alias,
                                    BaseSearchCondition baseSearchCondition);

    Integer countAppAccount(Long appId, AccountCategoryEnum category, String account, String alias);

    boolean isAccountRefByAnyStep(Long accountId);

    /**
     * 系统账号是否被DB账号依赖
     *
     * @param accountId 系统账号ID
     */
    boolean isSystemAccountRefByDbAccount(Long accountId);

    boolean checkCreateParam(AccountCreateUpdateReq accountCreateUpdateReq, boolean checkAlias,
                             boolean checkAccountName);

    AccountDTO buildCreateAccountDTO(String username, long appId, AccountCreateUpdateReq accountCreateUpdateReq);

    void createDefaultAccounts(long appId);
}
