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

import com.tencent.bk.job.common.config.FeatureToggleConfig;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import org.apache.commons.collections4.CollectionUtils;

/**
 * 特性开关
 */
public class FeatureToggle {

    private FeatureToggle() {
    }

    private static FeatureToggleConfig get() {
        return Inner.instance;
    }

    private static class Inner {
        private static final FeatureToggleConfig instance =
            ApplicationContextRegister.getBean(FeatureToggleConfig.class);
    }

    /**
     * 业务是否对接cmdb业务集。临时实现，待核心功能完成之后使用第三方框架完成特性开关，支持运行时更新开关
     *
     * @param appId Job业务ID
     */
    public static boolean isCmdbBizSetEnabledForApp(Long appId) {
        FeatureToggleConfig featureToggleConfig = get();
        FeatureToggleConfig.ToggleConfig cmdbBizSetConfig = featureToggleConfig.getCmdbBizSet();
        return cmdbBizSetConfig.isEnabled()
            && (!cmdbBizSetConfig.isGray() // 未开启灰度，全量启用
            || (CollectionUtils.isEmpty(cmdbBizSetConfig.getIncludeApps())
            || cmdbBizSetConfig.getIncludeApps().contains(appId)) // 如果使用includeApps参数，需要判断是否包含该业务
            && (CollectionUtils.isEmpty(cmdbBizSetConfig.getExcludeApps())
            || !cmdbBizSetConfig.getExcludeApps().contains(appId))); // 如果使用excludeApps参数，需要判断是否排除该业务
    }

    /**
     * 是否对接cmdb业务集。临时实现，待核心功能完成之后使用第三方框架完成特性开关，支持运行时更新开关
     */
    public static boolean isCmdbBizSetEnabled() {
        FeatureToggleConfig featureToggleConfig = get();
        FeatureToggleConfig.ToggleConfig cmdbBizSetConfig = featureToggleConfig.getCmdbBizSet();
        return cmdbBizSetConfig.isEnabled();
    }

    /**
     * 是否兼容ESB bk_biz_id 参数
     */
    public static boolean isBkBizIdEnabled() {
        FeatureToggleConfig featureToggleConfig = get();
        FeatureToggleConfig.ToggleConfig toggleConfig = featureToggleConfig.getEsbApiParamBkBizId();
        return toggleConfig.isEnabled();
    }
}
