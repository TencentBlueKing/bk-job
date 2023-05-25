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

package com.tencent.bk.job.common.encrypt.scenario;

import com.tencent.bk.job.common.encrypt.CryptoScenarioEnum;
import com.tencent.bk.job.common.encrypt.CryptorNames;
import com.tencent.bk.job.common.encrypt.SymmetricCryptoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SensitiveParamService {

    private final SymmetricCryptoService symmetricCryptoService;

    @Autowired
    public SensitiveParamService(SymmetricCryptoService symmetricCryptoService) {
        this.symmetricCryptoService = symmetricCryptoService;
    }

    public String getSecureParamEncryptAlgorithm(Boolean secureParam) {
        if (secureParam == null || !secureParam) {
            return CryptorNames.NONE;
        }
        return symmetricCryptoService.getAlgorithmByScenario(CryptoScenarioEnum.SCRIPT_SENSITIVE_PARAM);
    }

    public String encryptParamIfNeeded(boolean secureParam, String param) {
        if (!secureParam) {
            return param;
        }
        return symmetricCryptoService.encryptToBase64Str(param, CryptoScenarioEnum.SCRIPT_SENSITIVE_PARAM);
    }

    public String encryptParamIfNeeded(boolean secureParam, String param, String encryptAlgorithm) {
        if (!secureParam) {
            return param;
        }
        return symmetricCryptoService.encryptToBase64Str(param, encryptAlgorithm);
    }

    public String decryptParamIfNeeded(boolean secureParam, String encryptedParam, String algorithm) {
        if (!secureParam) {
            return encryptedParam;
        }
        return symmetricCryptoService.decrypt(encryptedParam, algorithm);
    }

}
