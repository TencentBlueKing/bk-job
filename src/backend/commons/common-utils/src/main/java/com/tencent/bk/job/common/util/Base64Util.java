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

package com.tencent.bk.job.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

@Slf4j
public class Base64Util {
    /**
     * BASE64 解码字符，返回解码后的字符
     *
     * @param content
     * @return
     */
    public static String decodeContentToStr(String content) {
        if (StringUtils.isEmpty(content)) {
            return null;
        }
        try {
            return new String(Base64.decodeBase64(content), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Decode content fail", e);
            return null;
        }
    }

    /**
     * BASE64 解码字符，仅当解码结果是一段合法的 UTF-8 文本时才返回，否则返回 null
     * <p>
     * {@link #decodeContentToStr(String)} 用的解码器会跳过非法字符，明文也能"解码成功"，
     * 解出的却是一串二进制垃圾（例如 "111" 解出 0xD7 0x5D）。要区分调用方传的是 BASE64 还是明文，
     * 只能靠解码结果是否为合法 UTF-8 来判断。
     *
     * @param content BASE64 编码后的字符串
     * @return 解码后的文本；content 为空、或解码结果不是合法 UTF-8 文本时返回 null
     */
    public static String decodeContentToStrStrictly(String content) {
        if (StringUtils.isEmpty(content)) {
            return null;
        }
        byte[] decoded;
        try {
            decoded = Base64.decodeBase64(content);
        } catch (Exception e) {
            log.warn("Decode content fail", e);
            return null;
        }
        if (decoded.length == 0) {
            return null;
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(decoded))
                .toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    /**
     * BASE64 编码字符，返回编码后的字符
     *
     * @param content
     * @return
     */
    public static String encodeContentToStr(String content) {
        if (StringUtils.isEmpty(content)) {
            return null;
        }
        try {
            return Base64.encodeBase64String(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("Encode content fail", e);
            return null;
        }
    }

    /**
     * BASE64 编码字节流，返回编码后的字符
     *
     * @param byteContent
     * @return
     */
    public static String encodeContentToStr(byte[] byteContent) {
        if (byteContent == null || byteContent.length == 0) {
            return null;
        }
        try {
            return Base64.encodeBase64String(byteContent);
        } catch (Exception e) {
            log.warn("Encode content fail", e);
            return null;
        }
    }

    /**
     * BASE64 解码字符，返回解码后的字节流
     *
     * @param content
     * @return
     */
    public static byte[] decodeContentToByte(String content) {
        if (StringUtils.isEmpty(content)) {
            return new byte[0];
        }
        try {
            return Base64.decodeBase64(content);
        } catch (Exception e) {
            log.warn("Decode content fail", e);
            throw e;
        }
    }

    /**
     * 根据BASE64编码后的字符串计算原始字节流长度，不用decode字符串
     *
     * @param encodedContent 编码后的字符串
     * @return 原始字符串的长度
     */
    public static int calcOriginBytesLength(String encodedContent) {
        if (StringUtils.isEmpty(encodedContent)) {
            return 0;
        }

        // 最多只会填充两个 =
        int fillCount = 0;
        for (int i = 0; i <= 1 && encodedContent.length() - 1 - i >= 0; i++) {
            if (encodedContent.charAt(encodedContent.length() - 1 - i) == '=') {
                fillCount++;
            } else {
                break;
            }
        }

        return encodedContent.length() * 3 / 4 - fillCount;
    }
}
