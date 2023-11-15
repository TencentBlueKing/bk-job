package com.tencent.bk.job.api.util;

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 测试值生成器
 */
public class TestValueGenerator {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final AtomicLong counter = new AtomicLong(1);

    /**
     * 生成一个全局自增长的 ID
     */
    public static long nextAutoIncrementId() {
        return counter.incrementAndGet();
    }

    /**
     * 生成指定长度的字符串
     *
     * @param length 字符串长度
     */
    public static String generateRandomString(int length) {
        StringBuilder builder = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            builder.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }

        return builder.toString();
    }

    /**
     * 生成一个用于测试的唯一的字符值
     *
     * @param str       原始字符值
     * @param maxLength 最大字符串长度
     */
    public static String generateUniqueStrValue(String str, int maxLength) {
        String result = "api_" + TestGlobalTaskId.get() + "_" + nextAutoIncrementId() + "_" + str;
        return result.length() > maxLength ? result.substring(0, maxLength) : result;
    }

}
