package com.tencent.bk.job.api.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;

public class Base64Util {
    /**
     * BASE64 解码字符，返回解码后的字符
     *
     * @param content
     * @return
     */
    public static String base64DecodeContentToStr(String content) {
        if (StringUtils.isEmpty(content)) {
            return null;
        }
        try {
            return new String(Base64.decodeBase64(content), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    /**
     * BASE64 编码字符，返回编码后的字符
     *
     * @param content
     * @return
     */
    public static String base64EncodeContentToStr(String content) {
        if (StringUtils.isEmpty(content)) {
            return null;
        }
        try {
            return Base64.encodeBase64String(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    /**
     * BASE64 编码字节流，返回编码后的字符
     *
     * @param byteContent
     * @return
     */
    public static String base64EncodeContentToStr(byte[] byteContent) {
        if (byteContent == null || byteContent.length == 0) {
            return null;
        }
        try {
            return Base64.encodeBase64String(byteContent);
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    /**
     * BASE64 解码字符，返回解码后的字节流
     *
     * @param content
     * @return
     */
    public static byte[] base64DecodeToByte(String content) {
        if (StringUtils.isEmpty(content)) {
            return new byte[0];
        }
        try {
            return Base64.decodeBase64(content);
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }
}
