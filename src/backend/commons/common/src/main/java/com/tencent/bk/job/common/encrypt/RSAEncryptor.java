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

import com.tencent.bk.job.common.util.crypto.RSAUtils;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;

public class RSAEncryptor implements AsymmetricEncryptor {
    private PublicKey publicKey;

    public RSAEncryptor(File rsaPubPermFile) throws IOException, GeneralSecurityException {
        publicKey = RSAUtils.getPublicKey(rsaPubPermFile);
    }

    public RSAEncryptor(String rsaPublicKeyBase64) throws IOException, GeneralSecurityException {
        publicKey = RSAUtils.getPublicKey(rsaPublicKeyBase64);
    }

    public String encrypt(String rawText) {
        try {
            return RSAUtils.encrypt(rawText, publicKey);
        } catch (IOException | GeneralSecurityException e) {
            return null;
        }
    }

    public boolean verify(String message, String signature) {
        try {
            return RSAUtils.verify(publicKey, message, signature);
        } catch (Exception e) {
            return false;
        }
    }
}
