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

package com.tencent.bk.job.execute.service.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.execute.client.AccountResourceClient;
import com.tencent.bk.job.execute.model.AccountDTO;
import com.tencent.bk.job.execute.service.AccountService;
import com.tencent.bk.job.manage.common.consts.account.AccountCategoryEnum;
import com.tencent.bk.job.manage.common.consts.account.AccountTypeEnum;
import com.tencent.bk.job.manage.model.inner.ServiceAccountDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AccountServiceImpl implements AccountService {
    private final AccountResourceClient accountResourceClient;

    private LoadingCache<String, AccountDTO> accountCache = CacheBuilder.newBuilder()
        .maximumSize(10000).expireAfterWrite(1, TimeUnit.MINUTES).
            build(new CacheLoader<String, AccountDTO>() {
                      @Override
                      public AccountDTO load(String accountKey) {
                          String[] accountProps = accountKey.split("#");
                          String keyType = accountProps[0];
                          if ("account_alias".equals(keyType)) {
                              AccountCategoryEnum accountCategory =
                                  AccountCategoryEnum.valOf(Integer.parseInt(accountProps[1]));
                              long appId = Long.parseLong(accountProps[2]);
                              String accountAlias = accountProps[3];
                              return getAccountByAlias(accountCategory, appId, accountAlias);
                          } else if ("account_id".equals(keyType)) {
                              long accountId = Long.parseLong(accountProps[1]);
                              return getAccountById(accountId);
                          } else {
                              return null;
                          }
                      }
                  }
            );

    @Autowired
    public AccountServiceImpl(AccountResourceClient accountResourceClient) {
        this.accountResourceClient = accountResourceClient;
    }

    @Override
    public AccountDTO getAccountById(Long accountId) throws ServiceException {
        InternalResponse<ServiceAccountDTO> resp = accountResourceClient.getAccountByAccountId(accountId);
        if (!resp.isSuccess()) {
            log.warn("Get account by accountId:{} return fail resp", accountId);
            throw new InternalException(resp.getCode());
        }
        if (resp.getData() == null) {
            log.warn("Get account by accountId:{} return empty account", accountId);
            return null;
        }
        return convertToAccountInfo(resp.getData());
    }

    @Override
    public AccountDTO getAccountByAccountName(Long appId, String accountName) throws ServiceException {
        InternalResponse<ServiceAccountDTO> resp = accountResourceClient.getAccountByAccountName(appId,
            accountName);
        if (!resp.isSuccess()) {
            log.warn("Get accountName by appId:{}, accountName:{} return fail resp", appId, accountName);
            throw new InternalException(resp.getCode());
        }
        if (resp.getData() == null) {
            log.warn("Get accountName by appId:{}, accountName:{} return empty accountName", appId, accountName);
            return null;
        }
        return convertToAccountInfo(resp.getData());
    }

    private AccountDTO convertToAccountInfo(ServiceAccountDTO accountDTO) {
        if (accountDTO == null) {
            return null;
        }
        AccountDTO accountInfo = new AccountDTO();
        accountInfo.setId(accountDTO.getId());
        accountInfo.setAccount(accountDTO.getAccount());
        accountInfo.setAlias(accountDTO.getAlias());
        AccountCategoryEnum accountCategory = AccountCategoryEnum.valOf(accountDTO.getCategory());
        accountInfo.setCategory(accountCategory);
        accountInfo.setType(AccountTypeEnum.valueOf(accountDTO.getType()));
        accountInfo.setGrantees(accountDTO.getGrantees());
        if (accountCategory == AccountCategoryEnum.DB) {
            accountInfo.setDbPassword(accountDTO.getDbPassword());
            accountInfo.setDbPort(accountDTO.getDbPort());
            accountInfo.setDbSystemAccountId(accountDTO.getDbSystemAccount().getId());
        }
        accountInfo.setAppId(accountDTO.getAppId());
        accountInfo.setPassword(accountDTO.getPassword());
        return accountInfo;
    }

    @Override
    public AccountDTO getSystemAccountByAlias(String alias, Long appId) throws ServiceException {
        return getAccountByAlias(AccountCategoryEnum.SYSTEM, appId, alias);
    }

    @Override
    public AccountDTO getAccountByAlias(AccountCategoryEnum accountCategory, Long appId,
                                        String alias) throws ServiceException {
        InternalResponse<ServiceAccountDTO> resp = accountResourceClient.getAccountByCategoryAndAliasInApp(appId,
            accountCategory.getValue(), alias);
        if (!resp.isSuccess()) {
            log.warn("Get account by category: {}, alias:{}, appId:{} return fail resp", accountCategory, alias, appId);
            throw new InternalException(resp.getCode());
        }
        if (resp.getData() == null) {
            log.warn("Get account by category: {}, alias:{}, appId:{} return empty account", accountCategory, alias,
                appId);
            throw new InternalException(ErrorCode.INTERNAL_ERROR);
        }
        return convertToAccountInfo(resp.getData());
    }

    @Override
    public AccountDTO getAccount(Long accountId, AccountCategoryEnum accountCategory, String accountAlias,
                                 Long appId) throws ServiceException {
        AccountDTO accountInfo = null;
        if (accountId != null && accountId > 0) {
            accountInfo = getAccountById(accountId);
        } else if (StringUtils.isNotBlank(accountAlias)) { //原account传的是account,改为支持alias，减少用户API调用增加参数的成本
            accountInfo = getAccountByAlias(accountCategory, appId, accountAlias);
        }
        // 可能帐号已经被删除了的情况：如从执行历史中点重做/克隆的方式。
        if (accountInfo == null && StringUtils.isNotBlank(accountAlias)) {//兼容老的传参，直接传递没有密码的只有帐号名称的认证
            accountInfo = new AccountDTO();
            accountInfo.setAccount(accountAlias);
            accountInfo.setAlias(accountAlias);
        }
        return accountInfo;
    }

    @Override
    public AccountDTO getAccountPreferCache(Long accountId, AccountCategoryEnum accountCategory,
                                            String accountAlias, Long appId) throws ServiceException {
        String accountCacheKey;
        if (accountId != null && accountId > 0) {
            accountCacheKey = "account_id#" + accountId;
        } else if (StringUtils.isNotBlank(accountAlias)) {
            accountCacheKey = "account_alias#" + accountCategory.getValue() + "#" + appId + "#" + accountAlias;
        } else {
            return null;
        }
        AccountDTO accountInfo;
        try {
            accountInfo = accountCache.get(accountCacheKey);
        } catch (ExecutionException e) {
            log.warn("Get account from cache fail", e);
            return getAccount(accountId, accountCategory, accountAlias, appId);
        }
        // 可能帐号已经被删除了的情况：如从执行历史中点重做/克隆的方式。
        if (accountInfo == null && StringUtils.isNotBlank(accountAlias)) {//兼容老的传参，直接传递没有密码的只有帐号名称的认证
            accountInfo = new AccountDTO();
            accountInfo.setAccount(accountAlias);
            accountInfo.setAlias(accountAlias);
        }
        return accountInfo;
    }
}
