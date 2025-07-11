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

package com.tencent.bk.job.common.crypto.util;

import com.google.common.collect.Lists;
import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.sdk.crypto.exception.CryptoException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;

import javax.crypto.Cipher;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

/**
 * RSAUtils 加解密
 */
public class RSAUtils {
    private static final String KEY_ALGORITHM = "RSA";

    private static final String BEGIN_ENCRYPTED_PRIVATE_KEY = "-----BEGIN ENCRYPTED PRIVATE KEY-----";
    private static final String END_ENCRYPTED_PRIVATE_KEY = "-----END ENCRYPTED PRIVATE KEY-----";
    private static final String BEGIN_RSA_PRIVATE_KEY = "-----BEGIN RSA PRIVATE KEY-----";
    private static final String END_RSA_PRIVATE_KEY = "-----END RSA PRIVATE KEY-----";

    private static final String BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----";
    private static final String END_PRIVATE_KEY = "-----END PRIVATE KEY-----";
    private static final String BEGIN_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----";
    private static final String END_PUBLIC_KEY = "-----END PUBLIC KEY-----";

    private static final String CHARSET_NAME = "UTF-8";
    private static final String LINE_SPLIT = "\n";

    private static final List<String> SKIP_STR = Lists.newArrayList(
        BEGIN_PRIVATE_KEY, END_PRIVATE_KEY, BEGIN_PUBLIC_KEY, END_PUBLIC_KEY,
        BEGIN_ENCRYPTED_PRIVATE_KEY, END_ENCRYPTED_PRIVATE_KEY, BEGIN_RSA_PRIVATE_KEY, END_RSA_PRIVATE_KEY
    );

    public static RSAPrivateKey getPrivateKey(String rsaPrivateKeyBase64) throws CryptoException {
        try {
            String privateKeyPEM = getPermKey(rsaPrivateKeyBase64);
            byte[] encoded = Base64.decodeBase64(privateKeyPEM);
            return (RSAPrivateKey) KeyFactory.getInstance(KEY_ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(encoded));
        } catch (Exception e) {
            String msg = MessageFormatter.arrayFormat(
                "Fail to getPrivateKey using {}, rsaPrivateKeyBase64.len={}",
                new Object[]{KEY_ALGORITHM, rsaPrivateKeyBase64.length()}
            ).getMessage();
            throw new CryptoException(msg, e);
        }
    }

    public static RSAPublicKey getPublicKey(String rsaPublicKeyBase64) throws CryptoException {
        try {
            byte[] encoded = Base64.decodeBase64(getPermKey(rsaPublicKeyBase64));
            return (RSAPublicKey) KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(new X509EncodedKeySpec(encoded));
        } catch (Exception e) {
            String msg = MessageFormatter.arrayFormat(
                "Fail to getPublicKey using {}, rsaPublicKeyBase64={}",
                new Object[]{KEY_ALGORITHM, rsaPublicKeyBase64}
            ).getMessage();
            throw new CryptoException(msg, e);
        }
    }

    public static String encrypt(String rawText, PublicKey publicKey) throws CryptoException {
        try {
            return encrypt(rawText.getBytes(CHARSET_NAME), publicKey);
        } catch (Exception e) {
            String msg = MessageFormatter.arrayFormat(
                "Fail to getPublicKey using {}, rawText.len={}, publicKey={}",
                new Object[]{KEY_ALGORITHM, rawText.length(), publicKey}
            ).getMessage();
            throw new CryptoException(msg, e);
        }
    }

    private static String getPermKey(String permBase64) throws IOException {
        String perm = Base64Util.decodeContentToStr(permBase64);
        if (StringUtils.isEmpty(perm)) {
            throw new IOException("Perm content is empty");
        }
        StringBuilder strKeyPEM = new StringBuilder(2048);
        try (BufferedReader br = new BufferedReader(new StringReader(perm))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (SKIP_STR.contains(line)) {
                    continue;
                }
                strKeyPEM.append(line).append(LINE_SPLIT);
            }
        }
        return strKeyPEM.toString();
    }

    private static String encrypt(byte[] messageBytes,
                                  PublicKey publicKey) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return Base64.encodeBase64String(cipher.doFinal(messageBytes));
    }
}
