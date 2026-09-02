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

import lombok.extern.slf4j.Slf4j;

/**
 * HTTP 客户端证书校验开关的静态入口，供无法注入 Spring Bean 的场景（静态工厂、升级工具等）使用。
 * <p>
 * 配置来源为 {@code job.http.ssl.verify}，默认所有系统都校验证书。
 */
@Slf4j
public final class JobHttpSslVerifyConfig {

    private static volatile JobHttpSslVerifyProperties properties = new JobHttpSslVerifyProperties();

    private JobHttpSslVerifyConfig() {
    }

    public static void setProperties(JobHttpSslVerifyProperties sslVerifyProperties) {
        if (sslVerifyProperties == null) {
            return;
        }
        properties = sslVerifyProperties;
        log.info("job.http.ssl.verify={}", sslVerifyProperties);
    }

    /**
     * 仅设置全局开关，用于只能读取扁平化配置的场景（如升级工具）
     */
    public static void setGlobalVerifyEnabled(boolean enabled) {
        JobHttpSslVerifyProperties sslVerifyProperties = new JobHttpSslVerifyProperties();
        sslVerifyProperties.setEnabled(enabled);
        setProperties(sslVerifyProperties);
    }

    public static boolean isGlobalVerifyEnabled() {
        return properties.isEnabled();
    }

    /**
     * 解析指定外部系统是否校验证书，系统级未配置时继承全局配置
     *
     * @param system 外部系统
     * @return true 表示校验证书
     */
    public static boolean isVerifyEnabled(ExternalSystemEnum system) {
        return properties.isVerifyEnabled(system);
    }
}
