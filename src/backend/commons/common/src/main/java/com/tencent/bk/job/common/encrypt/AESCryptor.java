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

package com.tencent.bk.job.common.encrypt;

import com.tencent.bk.job.common.util.crypto.AESUtils;
import com.tencent.bk.sdk.gm.annotation.Cryptor;
import com.tencent.bk.sdk.gm.annotation.CryptorTypeEnum;
import com.tencent.bk.sdk.gm.cryptor.AbstractSymmetricCryptor;

/**
 * 使用AES/CBC/PKCS5Padding的加密实现
 */
@Cryptor(name = CryptorNames.AES, type = CryptorTypeEnum.SYMMETRIC)
public class AESCryptor extends AbstractSymmetricCryptor {
    @Override
    public byte[] encrypt(byte[] key, byte[] message) {
        try {
            return AESUtils.encrypt(message, key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] decrypt(byte[] key, byte[] encryptedMessage) {
        try {
            return AESUtils.decrypt(encryptedMessage, key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
