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

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "job.feature")
@ToString
@Slf4j
public class FeatureToggleConfig {

    private ToggleConfig pullFileResultByIp = new ToggleConfig();

    public void setPullFileResultByIp(ToggleConfig pullFileResultByIp) {
        this.pullFileResultByIp = pullFileResultByIp;
        log.info("FeatureToggleConfig.pullFileResultByIp:{}", pullFileResultByIp);
    }

    @ToString
    public static class ToggleConfig {
        private boolean enabled = true;
        private boolean gray;
        private String grayApps;
        private final List<Long> grayAppList = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isGray() {
            return gray;
        }

        public void setGray(boolean gray) {
            this.gray = gray;
        }

        public String getGrayApps() {
            return grayApps;
        }

        public void setGrayApps(String grayApps) {
            if (StringUtils.isNotEmpty(grayApps)) {
                this.grayApps = grayApps;

                grayAppList.addAll(Arrays.stream(grayApps.split(","))
                    .map(Long::valueOf).collect(Collectors.toSet()));
            }
        }
    }

    public boolean enablePullFileResultByIp(Long appId) {
        return pullFileResultByIp.enabled
            && (!pullFileResultByIp.gray || pullFileResultByIp.grayAppList.contains(appId));
    }

}
