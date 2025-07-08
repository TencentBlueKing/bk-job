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

package com.tencent.bk.job.common.crypto.config;

import com.tencent.bk.job.common.crypto.CryptoConfigService;
import com.tencent.bk.job.common.crypto.EncryptConfig;
import com.tencent.bk.job.common.crypto.SymmetricCryptoService;
import com.tencent.bk.job.common.crypto.scenario.CipherVariableCryptoService;
import com.tencent.bk.job.common.crypto.scenario.DbPasswordCryptoService;
import com.tencent.bk.job.common.crypto.scenario.SensitiveParamCryptoService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(EncryptConfig.class)
public class CryptoAutoConfiguration {

    @Bean
    CryptoConfigService cryptoConfigService(EncryptConfig encryptConfig) {
        return new CryptoConfigService(encryptConfig);
    }

    @Bean
    SymmetricCryptoService symmetricCryptoService(CryptoConfigService cryptoConfigService) {
        return new SymmetricCryptoService(cryptoConfigService);
    }

    @Bean
    CipherVariableCryptoService cipherVariableCryptoService(SymmetricCryptoService symmetricCryptoService) {
        return new CipherVariableCryptoService(symmetricCryptoService);
    }

    @Bean
    DbPasswordCryptoService dbPasswordCryptoService(SymmetricCryptoService symmetricCryptoService) {
        return new DbPasswordCryptoService(symmetricCryptoService);
    }

    @Bean
    SensitiveParamCryptoService sensitiveParamCryptoService(SymmetricCryptoService symmetricCryptoService) {
        return new SensitiveParamCryptoService(symmetricCryptoService);
    }
}
