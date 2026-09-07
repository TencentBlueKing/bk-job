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

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 调用外部系统时的 HTTPS 证书校验配置。
 * <p>
 * 所有系统的开关都集中在 {@code job.http.ssl.verify} 之下：{@code enabled} 为全局默认值，
 * {@code systems} 下各系统的开关未配置时继承全局默认值。
 */
@Getter
@Setter
@ToString
@ConfigurationProperties(prefix = "job.http.ssl.verify")
public class JobHttpSslVerifyProperties {

    /**
     * 全局是否校验 HTTPS 证书，默认开启。各外部系统未单独配置时继承该值
     */
    private boolean enabled = true;

    /**
     * 各外部系统单独的证书校验开关
     */
    private SystemProperties systems = new SystemProperties();

    /**
     * 解析指定外部系统最终是否校验证书
     *
     * @param system 外部系统
     * @return true 表示校验证书
     */
    public boolean isVerifyEnabled(ExternalSystemEnum system) {
        Boolean systemEnabled = systems == null ? null : systems.getEnabled(system);
        return systemEnabled != null ? systemEnabled : enabled;
    }

    /**
     * 各外部系统的证书校验开关，null 表示继承全局配置
     */
    @Getter
    @Setter
    @ToString
    public static class SystemProperties {

        private Boolean gse;
        private Boolean cmdb;
        private Boolean iam;
        private Boolean bkLogin;
        private Boolean bkUser;
        private Boolean bkRepo;
        private Boolean bkNotice;
        private Boolean bkCmsi;
        private Boolean bkAiDev;

        Boolean getEnabled(ExternalSystemEnum system) {
            if (system == null) {
                return null;
            }
            switch (system) {
                case GSE:
                    return gse;
                case CMDB:
                    return cmdb;
                case IAM:
                    return iam;
                case BK_LOGIN:
                    return bkLogin;
                case BK_USER:
                    return bkUser;
                case BK_REPO:
                    return bkRepo;
                case BK_NOTICE:
                    return bkNotice;
                case BK_CMSI:
                    return bkCmsi;
                case BK_AI_DEV:
                    return bkAiDev;
                default:
                    return null;
            }
        }
    }
}
