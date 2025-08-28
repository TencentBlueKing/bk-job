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

package com.tencent.bk.job.common.paas.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tencent.bk.job.common.esb.constants.BkApiTypeEnum;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "paas.login")
public class LoginProperties {
    /**
     * 蓝鲸标准登录接口类型，默认APIGW
     */
    private String apiType = BkApiTypeEnum.APIGW.getType();
    /**
     * 蓝鲸标准的登录url
     */
    private String url;

    /**
     * 自定义第三方登录系统配置
     */
    private CustomLoginConfig custom = new CustomLoginConfig();

    /**
     * 获取真正使用的登录url
     *
     * @return 登录url
     */
    @JsonIgnore
    public String getRealLoginUrl() {
        if (custom.isEnabled()) {
            return custom.getLoginUrl();
        } else {
            return url;
        }
    }

    @Getter
    @Setter
    public static class CustomLoginConfig {
        /**
         * 是否使用第三方登录系统
         */
        private boolean enabled = false;

        /**
         * 第三方登录系统用户token的cookie名称
         */
        private String tokenName = "bk_token";

        /**
         * 第三方登录系统登录url
         */
        private String loginUrl;

        /**
         * 第三方登录系统API url，用于根据token获取用户信息
         */
        private String apiUrl;
    }
}
