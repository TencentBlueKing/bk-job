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

package com.tencent.bk.job.common.crypto.scenario;

import com.tencent.bk.job.common.constant.AccountCategoryEnum;
import com.tencent.bk.job.common.crypto.CryptoScenarioEnum;
import com.tencent.bk.job.common.crypto.JobCryptorNames;
import com.tencent.bk.job.common.crypto.SymmetricCryptoService;
import com.tencent.bk.sdk.crypto.cryptor.consts.CryptorNames;
import com.tencent.bk.sdk.crypto.util.CryptorMetaUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * DB账号密码相关加解密服务
 */
@Slf4j
public class DbPasswordCryptoService {

    private final SymmetricCryptoService symmetricCryptoService;

    public DbPasswordCryptoService(SymmetricCryptoService symmetricCryptoService) {
        this.symmetricCryptoService = symmetricCryptoService;
    }

    public String getDbPasswordEncryptAlgorithmByCipher(AccountCategoryEnum accountCategoryEnum, String cipher) {
        if (!isDbAccount(accountCategoryEnum) || StringUtils.isEmpty(cipher)) {
            return CryptorNames.NONE;
        }
        String algorithm = CryptorMetaUtil.getCryptorNameFromCipher(cipher);
        if (algorithm != null) {
            return algorithm;
        }
        return JobCryptorNames.AES_CBC;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isDbAccount(AccountCategoryEnum accountCategoryEnum) {
        return AccountCategoryEnum.DB == accountCategoryEnum;
    }

    public String encryptDbPasswordIfNeeded(AccountCategoryEnum accountCategoryEnum, String dbPassword) {
        if (!isDbAccount(accountCategoryEnum)) {
            return dbPassword;
        }
        return symmetricCryptoService.encryptToBase64Str(dbPassword, CryptoScenarioEnum.DATABASE_PASSWORD);
    }

    public String decryptDbPasswordIfNeeded(AccountCategoryEnum accountCategoryEnum,
                                            String encryptedDbPassword) {
        if (!isDbAccount(accountCategoryEnum)) {
            return encryptedDbPassword;
        }
        String algorithm = getDbPasswordEncryptAlgorithmByCipher(accountCategoryEnum, encryptedDbPassword);
        if (StringUtils.isBlank(algorithm)) {
            return encryptedDbPassword;
        }
        return symmetricCryptoService.decrypt(encryptedDbPassword, algorithm);
    }

}
