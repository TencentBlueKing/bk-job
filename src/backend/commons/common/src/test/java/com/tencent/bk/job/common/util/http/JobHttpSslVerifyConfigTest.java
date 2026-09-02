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

package com.tencent.bk.job.common.util.http;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobHttpSslVerifyConfigTest {

    @AfterEach
    void restoreDefault() {
        JobHttpSslVerifyConfig.setProperties(new JobHttpSslVerifyProperties());
    }

    @Test
    @DisplayName("默认所有外部系统都校验证书")
    void verifyEnabledByDefault() {
        JobHttpSslVerifyProperties properties = new JobHttpSslVerifyProperties();
        assertTrue(properties.isEnabled());
        for (ExternalSystemEnum system : ExternalSystemEnum.values()) {
            assertTrue(properties.isVerifyEnabled(system));
        }
    }

    @Test
    @DisplayName("系统级未配置时继承全局配置")
    void inheritGlobalConfig() {
        JobHttpSslVerifyProperties properties = new JobHttpSslVerifyProperties();
        properties.setEnabled(false);
        for (ExternalSystemEnum system : ExternalSystemEnum.values()) {
            assertFalse(properties.isVerifyEnabled(system));
        }
    }

    @Test
    @DisplayName("系统级配置优先于全局配置")
    void systemConfigTakesPrecedence() {
        JobHttpSslVerifyProperties properties = new JobHttpSslVerifyProperties();
        properties.setEnabled(true);
        properties.getSystems().setCmdb(false);
        properties.getSystems().setBkRepo(false);

        assertFalse(properties.isVerifyEnabled(ExternalSystemEnum.CMDB));
        assertFalse(properties.isVerifyEnabled(ExternalSystemEnum.BK_REPO));
        assertTrue(properties.isVerifyEnabled(ExternalSystemEnum.IAM));
        assertTrue(properties.isVerifyEnabled(ExternalSystemEnum.BK_NOTICE));

        properties.setEnabled(false);
        properties.getSystems().setGse(true);
        assertTrue(properties.isVerifyEnabled(ExternalSystemEnum.GSE));
        assertFalse(properties.isVerifyEnabled(ExternalSystemEnum.IAM));
    }

    @Test
    @DisplayName("静态入口读取已设置的配置")
    void staticConfigReadsProperties() {
        JobHttpSslVerifyProperties properties = new JobHttpSslVerifyProperties();
        properties.setEnabled(false);
        properties.getSystems().setBkNotice(true);
        JobHttpSslVerifyConfig.setProperties(properties);

        assertFalse(JobHttpSslVerifyConfig.isGlobalVerifyEnabled());
        assertFalse(JobHttpSslVerifyConfig.isVerifyEnabled(ExternalSystemEnum.BK_REPO));
        assertTrue(JobHttpSslVerifyConfig.isVerifyEnabled(ExternalSystemEnum.BK_NOTICE));
    }

    @Test
    @DisplayName("仅设置全局开关时各系统继承该开关")
    void setGlobalVerifyEnabledOnly() {
        JobHttpSslVerifyConfig.setGlobalVerifyEnabled(false);
        assertFalse(JobHttpSslVerifyConfig.isVerifyEnabled(ExternalSystemEnum.CMDB));

        JobHttpSslVerifyConfig.setGlobalVerifyEnabled(true);
        assertTrue(JobHttpSslVerifyConfig.isVerifyEnabled(ExternalSystemEnum.CMDB));
    }
}
