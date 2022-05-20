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

package com.tencent.bk.job.common.config;

import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.util.List;

@ConfigurationProperties(prefix = "job.feature")
@ToString
@Getter
@Setter
@Slf4j
public class FeatureToggleConfig {
    /**
     * 是否对接cmdb业务集开关
     */
    private ToggleConfig cmdbBizSet = new ToggleConfig();
    /**
     * 是否兼容ESB bk_biz_id 参数开关
     */
    private ToggleConfig esbApiParamBkBizId = new ToggleConfig();


    @ToString
    @Getter
    @Setter
    public static class ToggleConfig {
        /**
         * 特性开关是否开启
         */
        private boolean enabled = true;
        /**
         * 是否支持灰度开启
         */
        private boolean gray;
        /**
         * 按灰度业务，生效的业务,gray=true条件下该参数生效
         */
        private List<Long> includeApps;
        /**
         * 按业务灰度，排除掉的业务,gray=true的条件下该参数生效
         */
        private List<Long> excludeApps;
    }

    @PostConstruct
    public void print() {
        log.info("FeatureToggleConfig init: {}", JsonUtils.toJson(this));
    }

}
