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

package com.tencent.bk.job.common.util;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Base64UtilTest {

    @Test
    void testCalcOriginBytesLength() {
        for (int i = 0; i < 100; i++) {
            String originStr = genRandomString();
            assertCalc(originStr);
        }
        String specialStrWithChinese = "# 在当前脚本执行时，第一行输出当前时间和进程ID，详见上面函数：job_get_now\n"
            + "job_start\n"
            + "\n"
            + "###### 作业平台中执行脚本成功和失败的标准只取决于脚本最后一条执行语句的返回值\n"
            + "###### 如果返回值为0，则认为此脚本执行成功，如果非0，则认为脚本执行失败\n"
            + "###### 可在此处开始编写您的脚本逻辑代码";
        assertCalc(specialStrWithChinese);

    }

    private void assertCalc(String originStr) {

        String encodedStr = Base64Util.encodeContentToStr(originStr);
        int calcLengthByUtil = Base64Util.calcOriginBytesLength(encodedStr);

        byte[] bytes = Base64Util.decodeContentToByte(encodedStr);
        int originBytesLength = bytes.length;

        System.out.println("originStr:" + originStr);
        System.out.println("originBytesLength:" + originBytesLength);
        System.out.println("calcLengthByUtil:" + calcLengthByUtil);

        assertEquals(originBytesLength, calcLengthByUtil,
            "bytes length should be " + originBytesLength + ", but is " + calcLengthByUtil);
    }

    private String genRandomString() {
        final int ASCII_LOW = 32;
        final int ASCII_HIGH = 126;
        final int SHORTEST = 1;
        final int LONGEST = 500;
        Random rand = new Random();
        int length = rand.nextInt(LONGEST - SHORTEST) + SHORTEST;
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomAscii = ASCII_LOW + rand.nextInt(ASCII_HIGH - ASCII_LOW + 1);
            sb.append((char) randomAscii);
        }
        return sb.toString();
    }
}
