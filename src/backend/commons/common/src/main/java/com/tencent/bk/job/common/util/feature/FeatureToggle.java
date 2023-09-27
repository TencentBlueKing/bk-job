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

package com.tencent.bk.job.common.util.feature;

import com.tencent.bk.job.common.util.ApplicationContextRegister;
import lombok.extern.slf4j.Slf4j;

/**
 * 特性开关
 */
@Slf4j
public class FeatureToggle {

    /**
     * 判断特性是否开启
     *
     * @param featureId 特性ID
     * @param ctx       特性运行上下文
     * @return 是否开启
     */
    public static boolean checkFeature(String featureId, FeatureExecutionContext ctx) {
        FeatureStore featureStore = ApplicationContextRegister.getBean(FeatureStore.class);
        if (featureStore == null) {
            if (log.isDebugEnabled()) {
                log.debug("Feature store is null, featureId: {}", featureId);
                return false;
            }
        }

        Feature feature = featureStore.getFeature(featureId);
        if (log.isDebugEnabled()) {
            log.debug("Check feature, featureId: {}, config: {}", featureId, feature);
        }
        if (feature == null) {
            if (log.isDebugEnabled()) {
                log.debug("Feature: {} is not exist!", featureId);

            }
            return false;
        }
        if (!feature.isEnabled()) {
            if (log.isDebugEnabled()) {
                log.debug("Feature: {} is disabled!", featureId);
            }
            return false;
        }

        ToggleStrategy strategy = feature.getStrategy();
        if (strategy == null) {
            if (log.isDebugEnabled()) {
                log.debug("Feature:{} strategy is empty!", featureId);
            }
            // 如果没有配置特性开启策略，且enabled=true，判定为特性开启
            return true;
        }

        boolean result = strategy.evaluate(featureId, ctx);
        if (log.isDebugEnabled()) {
            log.debug("Apply feature toggle strategy, featureId: {}, strategy: {}, context: {}, result: {}",
                featureId, strategy, ctx, result);
        }
        return result;
    }
}
