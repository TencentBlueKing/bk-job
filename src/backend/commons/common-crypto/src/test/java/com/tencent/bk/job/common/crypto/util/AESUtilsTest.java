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

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;

public class AESUtilsTest {
    @Test
    void testEncryptToBase64EncodedCipherText() {
        String text = "job";
        String encryptPassword = "job#123";
        String encryptedData = AESUtils.encryptToBase64EncodedCipherText(text, encryptPassword);
        AssertionsForClassTypes.assertThat(encryptedData).isNotEmpty();
    }

    @Test
    void testDecryptBase64EncodedCipherText() {
        String encryptedBase64EncodedData = "GQ6kLqtMevL8z/kXGVANQ+VP5o2Bt30yzXALfZbeOoY=";
        String encryptPassword = "job#123";
        String decodeData = AESUtils.decryptBase64EncodedDataToPlainText(encryptedBase64EncodedData, encryptPassword);
        AssertionsForClassTypes.assertThat(decodeData).isEqualTo("job");
    }

}
