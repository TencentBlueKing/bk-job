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

package com.tencent.bk.job.manage.api.tmp.impl;

import com.tencent.bk.job.common.constant.AccountCategoryEnum;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.AlreadyExistsException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.util.Utils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.api.common.constants.account.AccountTypeEnum;
import com.tencent.bk.job.manage.api.tmp.TmpAppAccountResource;
import com.tencent.bk.job.manage.model.dto.AccountDTO;
import com.tencent.bk.job.manage.model.tmp.TmpAccountCreateUpdateReq;
import com.tencent.bk.job.manage.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class TmpAppAccountResourceImpl implements TmpAppAccountResource {
    private final AccountService accountService;

    @Autowired
    public TmpAppAccountResourceImpl(AccountService accountService) {
        this.accountService = accountService;
    }


    public AccountDTO buildCreateAccountDTO(String operator, long appId, Long createTime, Long lastModifyTime,
                                            String lastModifyUser, TmpAccountCreateUpdateReq req) {
        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setId(req.getId());
        accountDTO.setAppId(appId);
        accountDTO.setAccount(req.getAccount());
        accountDTO.setCategory(AccountCategoryEnum.valOf(req.getCategory()));
        accountDTO.setType(AccountTypeEnum.valueOf(req.getType()));
        if (StringUtils.isBlank(req.getAlias())) {
            accountDTO.setAlias(req.getAccount());
        } else {
            accountDTO.setAlias(req.getAlias());
        }
        accountDTO.setRemark(req.getRemark());
        accountDTO.setGrantees(Utils.concatStringWithSeperator(req.getGrantees(), ","));

        if (AccountCategoryEnum.SYSTEM.getValue().equals(req.getCategory())) {
            if (AccountTypeEnum.WINDOW.getType().equals(req.getType())) {
                accountDTO.setOs("Windows");
            } else {
                accountDTO.setOs("Linux");
            }
            accountDTO.setPassword(req.getPassword());
        }

        if (AccountCategoryEnum.DB.getValue().equals(req.getCategory())) {
            accountDTO.setDbPort(req.getDbPort());
            accountDTO.setDbPassword(req.getDbPassword());
            accountDTO.setDbSystemAccountId(req.getDbSystemAccountId());
        }

        accountDTO.setCreator(operator);
        accountDTO.setCreateTime(createTime);
        accountDTO.setLastModifyUser(lastModifyUser);
        accountDTO.setLastModifyTime(lastModifyTime);

        return accountDTO;
    }

    @Override
    public Response saveAccount(String username, Long appId, Long createTime, Long lastModifyTime,
                                String lastModifyUser, Boolean useCurrentTime,
                                TmpAccountCreateUpdateReq accountCreateUpdateReq) {
        if (useCurrentTime != null && useCurrentTime) {
            if (createTime == null) {
                createTime = DateTimeUtils.currentTimeMillis();
            }
            if (lastModifyTime == null) {
                lastModifyTime = DateTimeUtils.currentTimeMillis();
            }
        }
        if (!accountService.checkCreateParam(accountCreateUpdateReq, false, false)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        Long accountId = accountCreateUpdateReq.getId();
        if (accountId != null) {
            AccountDTO accountDTO = accountService.getAccountById(accountId);
            if (accountDTO != null) {
                if (accountDTO.getAppId().longValue() == appId
                    && accountDTO.getAccount().equals(accountCreateUpdateReq.getAccount())
                    && accountDTO.getAlias().equals(accountCreateUpdateReq.getAlias())
                ) {
                    log.warn("same account exist, account={}, skip", JsonUtils.toJson(accountDTO));
                    return Response.buildSuccessResp(accountId);
                } else {
                    // 报错，相同ID账号已存在
                    throw new AlreadyExistsException(ErrorCode.ACCOUNT_ALIAS_EXIST);
                }
            }
        }

        if ((accountId == null || accountId == 0)
            && StringUtils.isNotBlank(accountCreateUpdateReq.getAlias())) {
            AccountDTO account;
            switch (accountCreateUpdateReq.getCategory()) {
                case 1:
                    account = accountService.getAccount(appId, AccountCategoryEnum.SYSTEM,
                        accountCreateUpdateReq.getAlias());
                    break;
                case 2:
                    account = accountService.getAccount(appId, AccountCategoryEnum.DB,
                        accountCreateUpdateReq.getAlias());
                    break;
                default:
                    throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
            }
            if (account != null) {
                if (account.getAccount().equals(accountCreateUpdateReq.getAccount())) {
                    return Response.buildSuccessResp(account.getId());
                }
            }
        }

        AccountDTO newAccount = buildCreateAccountDTO(username, appId, createTime, lastModifyTime, lastModifyUser,
            accountCreateUpdateReq);
        AccountDTO savedAccount = accountService.createAccount(newAccount);
        return Response.buildSuccessResp(savedAccount.getId());
    }
}
