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

package com.tencent.bk.job.common.crypto.util;

import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.sdk.crypto.exception.CryptoException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Arrays;

@Slf4j
public class AESUtils {
    /**
     * 加密/解密算法/工作模式/填充方式
     */
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";

    /**
     * 加密数据，并是使用base64编码
     *
     * @param data     待加密数据
     * @param password 密钥
     * @return byte[] 加密后的数据
     */
    public static String encryptToBase64EncodedCipherText(String data, String password) throws CryptoException {
        return Base64Util.encodeContentToStr(encrypt(data, password));
    }

    /**
     * 解密base64编码的数据，并转换成字符明文
     *
     * @param data     待解密数据，base64编码
     * @param password 密钥
     * @return String 解密后的数据
     */
    public static String decryptBase64EncodedDataToPlainText(String data, String password) throws CryptoException {
        return decryptToPlainText(Base64Util.decodeContentToByte(data), password);
    }

    /**
     * 加密数据
     *
     * @param data 待加密数据
     * @param key  密钥
     * @return byte[] 加密后的数据
     */
    public static byte[] encrypt(byte[] data, byte[] key) throws CryptoException {
        byte[] iv;
        byte[] finalData;
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getKeySpec(cipher, key));

            iv = cipher.getIV();
            finalData = cipher.doFinal(data);
        } catch (Exception e) {
            String msg = MessageFormatter.arrayFormat(
                "Fail to encrypt using {}, data.len={}, key.len={}",
                new Object[]{CIPHER_ALGORITHM, data.length, key.length}
            ).getMessage();
            throw new CryptoException(msg, e);
        }
        if (iv == null) {
            throw new RuntimeException(String.format("CIPHER_ALGORITHM %s is invalid", CIPHER_ALGORITHM));
        }
        byte[] finalDataWithIv = new byte[finalData.length + iv.length];
        System.arraycopy(iv, 0, finalDataWithIv, 0, iv.length);
        System.arraycopy(finalData, 0, finalDataWithIv, iv.length, finalData.length);
        return finalDataWithIv;
    }

    /**
     * 解密数据
     *
     * @param data 待解密数据
     * @param key  密钥
     * @return byte[] 解密后的数据
     */
    public static byte[] decrypt(byte[] data, byte[] key) throws CryptoException {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getKeySpec(cipher, key), getIvSpec(cipher, data));
            byte[] dataWithoutIv = new byte[data.length - cipher.getBlockSize()];
            System.arraycopy(data, cipher.getBlockSize(), dataWithoutIv,
                0, data.length - cipher.getBlockSize());
            return cipher.doFinal(dataWithoutIv);
        } catch (Exception e) {
            String msg = MessageFormatter.arrayFormat(
                "Fail to decrypt using {}, data.len={}, key.len={}",
                new Object[]{CIPHER_ALGORITHM, data.length, key.length}
            ).getMessage();
            throw new CryptoException(msg, e);
        }
    }

    public static void encrypt(InputStream in, OutputStream out, String password) throws CryptoException {
        byte[] key = password.getBytes(StandardCharsets.UTF_8);
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getKeySpec(cipher, key));
            byte[] arr = cipher.getIV();
            if (arr == null) {
                throw new RuntimeException(String.format("CIPHER_ALGORITHM %s is invalid", CIPHER_ALGORITHM));
            }
            out.write(arr);
            write(in, out, cipher);
        } catch (Exception e) {
            String msg = MessageFormatter.format(
                "Fail to encrypt using {}, password.len={}", CIPHER_ALGORITHM, password.length()
            ).getMessage();
            throw new CryptoException(msg, e);
        }
    }

    public static void decrypt(InputStream in, OutputStream out, String password) throws CryptoException {
        try {
            log.debug("decrypt: in.available={}", in.available());
            byte[] key = password.getBytes(StandardCharsets.UTF_8);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            byte[] iv = new byte[cipher.getBlockSize()];
            if (in.read(iv) < iv.length) {
                throw new RuntimeException();
            }
            log.debug("decrypt: iv={}", Arrays.toString(iv));
            cipher.init(Cipher.DECRYPT_MODE, getKeySpec(cipher, key), new IvParameterSpec(iv));
            write(in, out, cipher);
        } catch (Exception e) {
            String msg = MessageFormatter.format(
                "Fail to decrypt using {}, password.len={}", CIPHER_ALGORITHM, password.length()
            ).getMessage();
            throw new CryptoException(msg, e);
        }
    }

    /**
     * 解密数据
     *
     * @param data     待解密数据
     * @param password 密钥
     * @return byte[] 解密后的数据
     */
    private static String decryptToPlainText(byte[] data, String password) throws CryptoException {
        return new String(decrypt(data, password.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }

    /**
     * 加密数据
     *
     * @param data     待加密数据
     * @param password 密钥
     * @return byte[] 加密后的数据
     */
    private static byte[] encrypt(String data, String password) throws CryptoException {
        return encrypt(data.getBytes(StandardCharsets.UTF_8), password.getBytes(StandardCharsets.UTF_8));
    }

    private static IvParameterSpec getIvSpec(Cipher cipher, byte[] data) {
        byte[] iv = new byte[cipher.getBlockSize()];
        System.arraycopy(data, 0, iv, 0, iv.length);
        return new IvParameterSpec(iv);
    }

    private static SecretKeySpec getKeySpec(Cipher cipher, byte[] key)
        throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
        random.setSeed(key);
        kgen.init(cipher.getBlockSize() * 8, random);
        return new SecretKeySpec(kgen.generateKey().getEncoded(), "AES");
    }

    private static void write(InputStream in, OutputStream out, Cipher cipher) throws Exception {
        byte[] iBuffer = new byte[1024];
        int len;
        while ((len = in.read(iBuffer)) != -1) {
            byte[] oBuffer = cipher.update(iBuffer, 0, len);
            if (oBuffer != null) {
                out.write(oBuffer);
            }
        }
        byte[] oBuffer = cipher.doFinal();
        if (oBuffer != null) {
            out.write(oBuffer);
        }
    }
}
