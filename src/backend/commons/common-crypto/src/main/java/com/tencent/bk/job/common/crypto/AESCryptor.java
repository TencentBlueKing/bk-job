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

package com.tencent.bk.job.common.crypto;

import com.tencent.bk.job.common.crypto.util.AESUtils;
import com.tencent.bk.sdk.crypto.annotation.Cryptor;
import com.tencent.bk.sdk.crypto.annotation.CryptorTypeEnum;
import com.tencent.bk.sdk.crypto.cryptor.AbstractSymmetricCryptor;
import com.tencent.bk.sdk.crypto.exception.CryptoException;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * 使用AES/CBC/PKCS5Padding的加密实现
 */
@Cryptor(name = JobCryptorNames.AES_CBC, type = CryptorTypeEnum.SYMMETRIC)
public class AESCryptor extends AbstractSymmetricCryptor {

    @Override
    public String getName() {
        return JobCryptorNames.AES_CBC;
    }

    @Override
    public byte[] encryptIndeed(byte[] key, byte[] message) {
        try {
            return AESUtils.encrypt(message, key);
        } catch (Exception e) {
            FormattingTuple msg = MessageFormatter.arrayFormat(
                "Fail to encrypt using {}, key.len={}, message.len={}",
                new Object[]{
                    getName(),
                    key.length,
                    message.length
                }
            );
            throw new CryptoException(msg.getMessage(), e);
        }
    }

    @Override
    public byte[] decryptIndeed(byte[] key, byte[] encryptedMessage) {
        try {
            return AESUtils.decrypt(encryptedMessage, key);
        } catch (Exception e) {
            FormattingTuple msg = MessageFormatter.arrayFormat(
                "Fail to decrypt using {}, key.len={}, encryptedMessage.len={}",
                new Object[]{
                    getName(),
                    key.length,
                    encryptedMessage.length
                }
            );
            throw new CryptoException(msg.getMessage(), e);
        }
    }

    @Override
    public void encryptIndeed(String key, InputStream in, OutputStream out) {
        try {
            AESUtils.encrypt(in, out, key);
        } catch (Exception e) {
            FormattingTuple msg = MessageFormatter.arrayFormat(
                "Fail to encrypt using {}, key.len={}",
                new Object[]{
                    getName(),
                    key.length()
                }
            );
            throw new CryptoException(msg.getMessage(), e);
        }
    }

    @Override
    public void decryptIndeed(String key, InputStream in, OutputStream out) {
        try {
            AESUtils.decrypt(in, out, key);
        } catch (Exception e) {
            FormattingTuple msg = MessageFormatter.arrayFormat(
                "Fail to decrypt using {}, key.len={}",
                new Object[]{
                    getName(),
                    key.length()
                }
            );
            throw new CryptoException(msg.getMessage(), e);
        }
    }
}
