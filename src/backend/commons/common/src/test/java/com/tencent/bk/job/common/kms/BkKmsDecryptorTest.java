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

package com.tencent.bk.job.common.kms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BkKmsDecryptorTest {

    @Test
    void flattenKeepsFlatSecretKeys() {
        Map<String, String> properties = BkKmsDecryptor.flatten(
            "{\"mariadb-password\":\"p\",\"mariadb-truststore-password\":\"t\"}");

        assertEquals("p", properties.get("mariadb-password"));
        assertEquals("t", properties.get("mariadb-truststore-password"));
    }

    @Test
    void flattenExpandsNestedObject() {
        Map<String, String> properties = BkKmsDecryptor.flatten(
            "{\"mysql\":{\"password\":\"p\"}}");

        assertEquals("p", properties.get("mysql.password"));
    }

    @Test
    void flattenExpandsArrayByIndex() {
        Map<String, String> properties = BkKmsDecryptor.flatten(
            "{\"job.encrypt.usedPasswords\":[\"a,b\",\"c\"]}");

        assertEquals(2, properties.size());
        assertEquals("a,b", properties.get("job.encrypt.usedPasswords[0]"));
        assertEquals("c", properties.get("job.encrypt.usedPasswords[1]"));
    }

    @Test
    void flattenedArrayBindsToStringList() {
        Map<String, String> properties = BkKmsDecryptor.flatten(
            "{\"job.encrypt.usedPasswords\":[\"a,b\",\"c\"]}");

        List<String> usedPasswords = new Binder(new MapConfigurationPropertySource(properties))
            .bind("job.encrypt.used-passwords", Bindable.listOf(String.class))
            .get();

        assertEquals(List.of("a,b", "c"), usedPasswords);
    }

    @Test
    void flattenExpandsObjectInsideArray() {
        Map<String, String> properties = BkKmsDecryptor.flatten(
            "{\"ds\":[{\"password\":\"p\"}]}");

        assertEquals("p", properties.get("ds[0].password"));
    }

    @Test
    void flattenSkipsEmptyArray() {
        Map<String, String> properties = BkKmsDecryptor.flatten(
            "{\"job.encrypt.usedPasswords\":[],\"encrypt-password\":\"p\"}");

        assertEquals(Map.of("encrypt-password", "p"), properties);
    }

    @Test
    void flattenRejectsNonObject() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> BkKmsDecryptor.flatten("[\"p\"]"));

        assertTrue(exception.getMessage().contains("JSON object"));
    }
}
