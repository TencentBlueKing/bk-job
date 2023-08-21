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

package com.tencent.bk.job.backup.crypto;

import com.tencent.bk.job.common.crypto.CryptoScenarioEnum;
import com.tencent.bk.job.common.crypto.JobCryptorNames;
import com.tencent.bk.job.common.crypto.SymmetricCryptoService;
import com.tencent.bk.sdk.crypto.exception.CryptoException;
import com.tencent.bk.sdk.crypto.util.CryptorMetaUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * 导入导出文件相关加解密服务
 */
@Slf4j
@Service
public class BackupFileCryptoService {

    private final SymmetricCryptoService symmetricCryptoService;

    @Autowired
    public BackupFileCryptoService(SymmetricCryptoService symmetricCryptoService) {
        this.symmetricCryptoService = symmetricCryptoService;
    }

    public void encryptBackupFile(String password, File inFile, File outFile) {
        try (FileInputStream in = new FileInputStream(inFile); FileOutputStream out = new FileOutputStream(outFile)) {
            symmetricCryptoService.encrypt(password, in, out, CryptoScenarioEnum.BACKUP_FILE);
        } catch (Exception e) {
            throw new CryptoException("Fail to encrypt backupFile", e);
        }
    }

    public void decryptBackupFile(String password, File inFile, File outFile) {
        try {
            FileInputStream in = new FileInputStream(inFile);
            log.debug("in.available={}", in.available());
            BufferedInputStream bis = new BufferedInputStream(in);
            String algorithm = CryptorMetaUtil.getCryptorNameFromCipherStream(bis);
            if (StringUtils.isBlank(algorithm)) {
                algorithm = JobCryptorNames.AES_CBC;
            }
            try (FileOutputStream out = new FileOutputStream(outFile)) {
                log.debug("Use {} to decryptBackupFile, bis.available={}", algorithm, bis.available());
                symmetricCryptoService.decrypt(password, bis, out, algorithm);
            }
        } catch (Exception e) {
            throw new CryptoException("Fail to decrypt backupFile", e);
        }
    }

}
