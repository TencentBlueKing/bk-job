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

package com.tencent.bk.job.common.iam.util;

import com.tencent.bk.job.common.util.toggle.ToggleEvaluateContext;
import com.tencent.bk.job.common.util.toggle.ToggleStrategyContextParams;
import com.tencent.bk.job.common.util.toggle.feature.FeatureIdConstants;
import com.tencent.bk.job.common.util.toggle.feature.FeatureToggle;
import lombok.extern.slf4j.Slf4j;

/**
 * IAM 鉴权版本路由判定：根据热更特性开关 {@code iamV4} 与用户名灰度策略，决定某用户走 v3 还是 v4。
 *
 * <p>上层业务无感知：鉴权服务内部据此选择调用 v3 或 v4 的底层实现。</p>
 */
@Slf4j
public class IamAuthVersionHelper {

    private IamAuthVersionHelper() {
    }

    /**
     * 判断指定用户是否走 IAM v4 鉴权。
     *
     * <p>迁移期以 v3 为主：开关未开启/用户未灰度返回 false；若特性开关框架不可用或评估异常，
     * 也默认回退 v3，避免影响鉴权可用性。</p>
     *
     * @param username 用户名
     * @return true 走 v4；false 走 v3（默认）
     */
    public static boolean useV4(String username) {
        try {
            return FeatureToggle.checkFeature(
                FeatureIdConstants.FEATURE_IAM_V4,
                ToggleEvaluateContext.builder()
                    .addContextParam(ToggleStrategyContextParams.CTX_PARAM_USERNAME, username)
            );
        } catch (Exception e) {
            log.warn("Check iamV4 feature toggle failed, fallback to iam v3", e);
            return false;
        }
    }
}
