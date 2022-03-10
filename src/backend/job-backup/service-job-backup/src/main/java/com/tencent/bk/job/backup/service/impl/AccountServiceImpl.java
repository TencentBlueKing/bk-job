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

package com.tencent.bk.job.backup.service.impl;

import com.tencent.bk.job.backup.client.ServiceAccountResourceClient;
import com.tencent.bk.job.backup.client.WebAccountResourceClient;
import com.tencent.bk.job.backup.service.AccountService;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.manage.model.inner.ServiceAccountDTO;
import com.tencent.bk.job.manage.model.web.request.AccountCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.AccountVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * @since 24/11/2020 21:08
 */
@Slf4j
@Service
public class AccountServiceImpl implements AccountService {
    private final ServiceAccountResourceClient serviceAccountResourceClient;
    private final WebAccountResourceClient webAccountResourceClient;
    private final AppScopeMappingService appScopeMappingService;

    @Autowired
    public AccountServiceImpl(ServiceAccountResourceClient serviceAccountResourceClient,
                              WebAccountResourceClient webAccountResourceClient,
                              AppScopeMappingService appScopeMappingService) {
        this.serviceAccountResourceClient = serviceAccountResourceClient;
        this.webAccountResourceClient = webAccountResourceClient;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    public ServiceAccountDTO getAccountAliasById(Long id) {
        InternalResponse<ServiceAccountDTO> accountResp = serviceAccountResourceClient.getAccountByAccountId(id);
        if (accountResp != null) {
            if (0 == accountResp.getCode()) {
                ServiceAccountDTO account = accountResp.getData();
                if (account != null) {
                    if (StringUtils.isNotBlank(account.getAlias())) {
                        return account;
                    } else {
                        log.error("Account alias of {} is empty!|{}", id, accountResp);
                    }
                } else {
                    log.error("Account response of {} is empty!|{}", id, accountResp);
                }
            } else {
                log.error("Get account failed!|{}|{}|{}|{}", id, accountResp.getCode(), accountResp.getErrorMsg(),
                    accountResp);
            }
        } else {
            log.error("Get account failed! Empty response!|{}", id);
        }
        throw new InternalException("Error while getting account info!", ErrorCode.INTERNAL_ERROR);
    }

    @Override
    public List<AccountVO> listAccountByAppId(String username, Long appId) {
        ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(appId);
        Response<List<AccountVO>> appAccountListResp = webAccountResourceClient.listAccounts(username, appId,
            resourceScope.getType().getValue(), resourceScope.getId(), null);
        if (appAccountListResp != null) {
            if (0 == appAccountListResp.getCode()) {
                List<AccountVO> accountList = appAccountListResp.getData();
                if (CollectionUtils.isNotEmpty(accountList)) {
                    return accountList;
                } else {
                    log.error("List app account response of {} is empty!|{}", appId, appAccountListResp);
                }
            } else {
                log.error("List app account failed!|{}|{}|{}|{}", appId, appAccountListResp.getCode(),
                    appAccountListResp.getErrorMsg(), appAccountListResp);
            }
        } else {
            log.error("List app account failed! Empty response!|{}", appId);
        }
        return Collections.emptyList();
    }

    @Override
    public Long saveAccount(String username, Long appId, ServiceAccountDTO account) {
        AccountCreateUpdateReq accountCreateUpdateReq = new AccountCreateUpdateReq();
        accountCreateUpdateReq.setAccount(account.getAccount());
        accountCreateUpdateReq.setAlias(account.getAlias());
        accountCreateUpdateReq.setType(account.getType());
        accountCreateUpdateReq.setCategory(account.getCategory());
        accountCreateUpdateReq.setRemark(account.getRemark());
        accountCreateUpdateReq.setOs(account.getOs());
        accountCreateUpdateReq.setPassword(account.getPassword());
        accountCreateUpdateReq.setDbPassword(account.getDbPassword());
        if (account.getDbSystemAccount() != null) {
            accountCreateUpdateReq.setDbSystemAccountId(account.getDbSystemAccount().getId());
        }
        accountCreateUpdateReq.setDbPassword(account.getDbPassword());
        accountCreateUpdateReq.setDbPort(account.getDbPort());

        ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(appId);
        Response<Long> saveAccountResp = webAccountResourceClient.saveAccount(username,
            resourceScope.getType().getValue(), resourceScope.getId(), accountCreateUpdateReq);

        Integer errorCode = -1;
        if (saveAccountResp != null) {
            if (0 == saveAccountResp.getCode()) {
                Long accountId = saveAccountResp.getData();
                if (accountId != null && accountId > 0) {
                    return accountId;
                } else {
                    log.error("Save account response of {} is empty!|{}", account.getAlias(), saveAccountResp);
                }
            } else {
                log.error("Save account failed!|{}|{}|{}|{}|{}", username, account.getAlias(),
                    saveAccountResp.getCode(), saveAccountResp.getErrorMsg(), saveAccountResp);
                errorCode = saveAccountResp.getCode();
            }
        } else {
            log.error("Save account failed! Empty response!|{}|{}", username, account.getAlias());
        }
        throw new InternalException("Error while save or get account info!", errorCode);
    }
}
