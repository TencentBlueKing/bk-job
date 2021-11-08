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

package com.tencent.bk.job.manage.migration;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.util.crypto.AESUtils;
import com.tencent.bk.job.manage.common.consts.account.AccountCategoryEnum;
import com.tencent.bk.job.manage.config.JobManageConfig;
import com.tencent.bk.job.manage.dao.AccountDAO;
import com.tencent.bk.job.manage.model.dto.AccountDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class EncryptDbAccountPasswordMigrationTask {
    private final AccountDAO accountDAO;

    private final JobManageConfig jobManageConfig;

    @Autowired
    public EncryptDbAccountPasswordMigrationTask(AccountDAO accountDAO,
                                                 JobManageConfig jobManageConfig) {
        this.accountDAO = accountDAO;
        this.jobManageConfig = jobManageConfig;
    }

    /**
     * 加密DB账号的密码
     *
     * @return 结果
     */
    public Response<List<Long>> encryptDbAccountPassword() {
        log.info("Encrypt db account password start...");
        if (StringUtils.isBlank(jobManageConfig.getEncryptPassword())) {
            log.error("Encrypt password is blank");
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }

        List<AccountDTO> dbAccounts = accountDAO.listAccountByAccountCategory(AccountCategoryEnum.DB);
        if (CollectionUtils.isEmpty(dbAccounts)) {
            log.info("Db account is empty, skip");
            return Response.buildSuccessResp(Collections.emptyList());
        }

        try {
            List<Long> updateAccountIdList = new ArrayList<>();
            for (AccountDTO dbAccount : dbAccounts) {
                if (StringUtils.isNotEmpty(dbAccount.getDbPassword())) {
                    if (isEncrypted(dbAccount.getDbPassword(), jobManageConfig.getEncryptPassword())) {
                        log.info("Account[id={}] is encrypted", dbAccount.getId());
                        continue;
                    }
                    dbAccount.setDbPassword(AESUtils.encryptToBase64EncodedCipherText(dbAccount.getDbPassword(),
                        jobManageConfig.getEncryptPassword()));
                    updateAccountIdList.add(dbAccount.getId());
                }
            }
            accountDAO.batchUpdateDbAccountPassword(dbAccounts);
            log.info("Encrypt db account password successfully!");
            return Response.buildSuccessResp(updateAccountIdList);
        } catch (Throwable e) {
            log.error("Encrypt db account password error", e);
            throw new InternalException(ErrorCode.INTERNAL_ERROR, "Encrypt db account password error");
        }
    }

    private boolean isEncrypted(String base64EncodedCipherText, String key) {
        return tryDecrypt(base64EncodedCipherText, key);
    }

    private boolean tryDecrypt(String base64EncodedCipherText, String key) {
        try {
            AESUtils.decryptBase64EncodedDataToPlainText(base64EncodedCipherText, key);
            return true;
        } catch (Throwable e) {
            log.warn("Decrypt fail", e);
            return false;
        }
    }
}
