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

package com.tencent.bk.job.manage.api.inner.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.ArrayUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.manage.api.inner.ServiceAccountResource;
import com.tencent.bk.job.manage.auth.AccountAuthService;
import com.tencent.bk.job.manage.common.consts.account.AccountCategoryEnum;
import com.tencent.bk.job.manage.model.dto.AccountDTO;
import com.tencent.bk.job.manage.model.inner.ServiceAccountDTO;
import com.tencent.bk.job.manage.model.web.request.AccountCreateUpdateReq;
import com.tencent.bk.job.manage.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
public class ServiceAccountResourceImpl implements ServiceAccountResource {
    private final AccountService accountService;
    private final AccountAuthService accountAuthService;
    private final AppScopeMappingService appScopeMappingService;

    @Autowired
    public ServiceAccountResourceImpl(AccountService accountService,
                                      AccountAuthService accountAuthService,
                                      AppScopeMappingService appScopeMappingService) {
        this.accountService = accountService;
        this.accountAuthService = accountAuthService;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    public InternalResponse<ServiceAccountDTO> getAccountByAccountId(Long accountId) {
        AccountDTO accountDTO = accountService.getAccountById(accountId);
        if (accountDTO == null) {
            log.warn("Account is not exist, accountId={}", accountId);
            throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST, ArrayUtil.toArray(accountId));
        }
        ServiceAccountDTO result = accountDTO.toServiceAccountDTO();
        if (accountDTO.getCategory() == AccountCategoryEnum.DB) {
            long systemAccountId = accountDTO.getDbSystemAccountId();
            AccountDTO dbSystemAccount = accountService.getAccountById(systemAccountId);
            if (dbSystemAccount == null) {
                log.warn("Db related system account is not exist, accountId={}", accountId);
                throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST, ArrayUtil.toArray(systemAccountId));
            }
            result.setDbSystemAccount(dbSystemAccount.toServiceAccountDTO());
        }
        return InternalResponse.buildSuccessResp(result);
    }

    @Override
    public InternalResponse<ServiceAccountDTO> getAccountByAccountName(Long appId, String account) {
        AccountDTO accountDTO = accountService.getAccountByAccount(appId, account);
        if (accountDTO == null) {
            log.warn("Account is not exist, appId={},account={}", appId, account);
            throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST, ArrayUtil.toArray(account));
        }
        ServiceAccountDTO result = accountDTO.toServiceAccountDTO();
        if (accountDTO.getCategory() == AccountCategoryEnum.DB) {
            long systemAccountId = accountDTO.getDbSystemAccountId();
            AccountDTO dbSystemAccount = accountService.getAccountById(systemAccountId);
            if (dbSystemAccount == null) {
                log.warn("Db related system account is not exist,appId={}, account={}", appId, account);
                throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST);
            }
            result.setDbSystemAccount(dbSystemAccount.toServiceAccountDTO());
        }
        return InternalResponse.buildSuccessResp(result);
    }


    @Override
    public InternalResponse<ServiceAccountDTO> getAccountByCategoryAndAliasInApp(Long appId, Integer category,
                                                                                 String alias) {
        AccountDTO accountDTO = accountService.getAccount(appId, AccountCategoryEnum.valOf(category), alias);
        if (accountDTO == null) {
            log.warn("Account is not exist, appId={}, category={}, alias={}", appId, category, alias);
            throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST, ArrayUtil.toArray(alias));
        }

        return InternalResponse.buildSuccessResp(toServiceAccountDTO(accountDTO));
    }

    private ServiceAccountDTO toServiceAccountDTO(AccountDTO accountDTO) {
        ServiceAccountDTO result = accountDTO.toServiceAccountDTO();
        if (accountDTO.getCategory() == AccountCategoryEnum.DB) {
            long systemAccountId = accountDTO.getDbSystemAccountId();
            AccountDTO dbSystemAccount = accountService.getAccountById(systemAccountId);
            if (dbSystemAccount == null) {
                log.warn("Db related system account is not exist, account: {}", accountDTO);
                throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST, ArrayUtil.toArray(systemAccountId));
            }
            result.setDbSystemAccount(dbSystemAccount.toServiceAccountDTO());
        }
        return result;
    }

    @Override
    public InternalResponse<ServiceAccountDTO> saveOrGetAccount(String username, Long createTime, Long lastModifyTime,
                                                                String lastModifyUser, Long appId,
                                                                AccountCreateUpdateReq accountCreateUpdateReq) {
        if (!accountService.checkCreateParam(accountCreateUpdateReq, true, true)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        AccountDTO accountDTO =
            accountService.getAccount(appId, AccountCategoryEnum.SYSTEM, accountCreateUpdateReq.getAlias());
        if (accountDTO != null) {
            return InternalResponse.buildSuccessResp(accountDTO.toServiceAccountDTO());
        }
        AccountDTO newAccount = accountService.buildCreateAccountDTO(username, appId, accountCreateUpdateReq);
        newAccount.setCreator(username);
        if (createTime != null && createTime > 0) {
            newAccount.setCreateTime(createTime);
        } else {
            newAccount.setCreateTime(DateUtils.currentTimeSeconds());
        }
        if (lastModifyTime != null && lastModifyTime > 0) {
            newAccount.setLastModifyTime(lastModifyTime);
        } else {
            newAccount.setLastModifyTime(DateUtils.currentTimeSeconds());
        }
        if (StringUtils.isNotBlank(lastModifyUser)) {
            newAccount.setLastModifyUser(lastModifyUser);
        } else {
            newAccount.setLastModifyUser(username);
        }

        long accountId = accountService.saveAccount(newAccount);
        newAccount.setId(accountId);
        return InternalResponse.buildSuccessResp(newAccount.toServiceAccountDTO());
    }

    @Override
    public Response<Long> saveAccount(String username,
                                      Long appId,
                                      AccountCreateUpdateReq accountCreateUpdateReq) {
        AppResourceScope appResourceScope = appScopeMappingService.getAppResourceScope(appId, null, null);
        AuthResult authResult = accountAuthService.authCreateAccount(username, appResourceScope);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        accountService.checkCreateParam(accountCreateUpdateReq, true, true);

        AccountDTO newAccount = accountService.buildCreateAccountDTO(username, appResourceScope.getAppId(),
            accountCreateUpdateReq);
        long accountId = accountService.saveAccount(newAccount);
        accountAuthService.registerAccount(username, accountId, newAccount.getAlias());
        return Response.buildSuccessResp(accountId);
    }

    @Override
    public Response<List<ServiceAccountDTO>> listAccounts(Long appId, Integer category) {
        List<AccountDTO> accountDTOS =
            accountService.listAllAppAccount(appId, AccountCategoryEnum.valOf(category));
        List<ServiceAccountDTO> accounts = new ArrayList<>();
        if (accountDTOS != null && !accountDTOS.isEmpty()) {
            for (AccountDTO accountDTO : accountDTOS) {
                ServiceAccountDTO account = toServiceAccountDTO(accountDTO);
                accounts.add(account);
            }
        }
        return Response.buildSuccessResp(accounts);
    }
}
