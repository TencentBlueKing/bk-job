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

package com.tencent.bk.job.manage.api.esb.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.manage.api.esb.EsbGetOSAccountResource;
import com.tencent.bk.job.manage.common.consts.account.AccountCategoryEnum;
import com.tencent.bk.job.manage.model.dto.AccountDTO;
import com.tencent.bk.job.manage.model.esb.EsbAccountDTO;
import com.tencent.bk.job.manage.model.esb.request.EsbGetOSAccountListRequest;
import com.tencent.bk.job.manage.service.AccountService;
import com.tencent.bk.job.manage.service.auth.EsbAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
public class EsbGetOSAccountResourceImpl implements EsbGetOSAccountResource {
    private final AccountService accountService;
    private final MessageI18nService i18nService;
    private final EsbAuthService authService;

    @Autowired
    public EsbGetOSAccountResourceImpl(AccountService accountService, MessageI18nService i18nService,
                                       EsbAuthService authService) {
        this.accountService = accountService;
        this.i18nService = i18nService;
        this.authService = authService;
    }

    @Override
    @EsbApiTimed(value = "esb.api", extraTags = {"api_name", "v2_get_os_account"})
    public EsbResp<List<EsbAccountDTO>> getAppOsAccountList(EsbGetOSAccountListRequest request) {
        ValidateResult checkResult = checkRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Get system account list, request is illegal!");
            throw new InvalidParamException(checkResult);
        }
        long appId = request.getAppId();
        List<AccountDTO> systemAccounts = accountService.listAllAppAccount(appId, AccountCategoryEnum.SYSTEM);
        return EsbResp.buildSuccessResp(convertToEsbAccountDTOList(systemAccounts));
    }

    private List<EsbAccountDTO> convertToEsbAccountDTOList(List<AccountDTO> accounts) {
        List<EsbAccountDTO> esbAccounts = new ArrayList<>();
        if (accounts == null || accounts.isEmpty()) {
            return esbAccounts;
        }
        for (AccountDTO account : accounts) {
            EsbAccountDTO esbAccount = new EsbAccountDTO();
            esbAccount.setId(account.getId());
            esbAccount.setAlias(account.getAlias());
            esbAccount.setAccount(account.getAccount());
            esbAccount.setAppId(account.getAppId());
            esbAccount.setCreateTime(DateUtils.formatUnixTimestamp(account.getCreateTime(), ChronoUnit.MILLIS, "yyyy" +
                "-MM-dd HH:mm:ss", ZoneId.of("UTC")));
            esbAccount.setOs(account.getOs());
            esbAccount.setCreator(account.getCreator());
            esbAccounts.add(esbAccount);
        }
        return esbAccounts;
    }

    private ValidateResult checkRequest(EsbGetOSAccountListRequest request) {
        if (request.getAppId() == null || request.getAppId() < 1) {
            log.warn("AppId is empty or illegal!");
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "id");
        }
        return ValidateResult.pass();
    }
}
