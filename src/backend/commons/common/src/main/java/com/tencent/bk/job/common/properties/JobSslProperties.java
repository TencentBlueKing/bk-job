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

package com.tencent.bk.job.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Getter
@Setter
public class JobSslProperties {

    /**
     * 连接mongo是否启用ssl
     */
    private boolean enabled = false;

    /**
     * 信任库类型，默认PKCS12
     */
    private String trustStoreType = "PKCS12";

    /**
     * 信任库地址，暂时以文件形式（运维提供）
     */
    private String trustStore;

    /**
     * 信任库密码
     */
    private String trustStorePassword;

    /**
     * 客户端证书存储类型，默认PKCS12
     */
    private String keyStoreType = "PKCS12";

    /**
     * 客户端的证书地址，文件形式
     */
    private String keyStore;

    /**
     * 客户端证书存储密钥
     */
    private String keyStorePassword;

    /**
     * 是否校验主机名
     */
    private boolean verifyHostname = false;

    public boolean isMutualTlsConfigured() {
        return StringUtils.hasText(keyStore);
    }
}
