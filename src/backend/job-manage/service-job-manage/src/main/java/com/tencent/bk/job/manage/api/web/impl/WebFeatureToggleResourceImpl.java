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

package com.tencent.bk.job.manage.api.web.impl;

import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.util.feature.Feature;
import com.tencent.bk.job.common.util.feature.FeatureIdConstants;
import com.tencent.bk.job.common.util.feature.FeatureManager;
import com.tencent.bk.job.manage.api.web.WebFeatureToggleResource;
import com.tencent.bk.job.manage.model.web.vo.FeatureVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class WebFeatureToggleResourceImpl implements WebFeatureToggleResource {

    private final FeatureManager featureManager;

    private static final Set<String> frontendFeatures = new HashSet<>();

    static {
        // 增强功能-容器执行
        frontendFeatures.add(FeatureIdConstants.FEATURE_CONTAINER_EXECUTE);
    }

    @Autowired
    public WebFeatureToggleResourceImpl(FeatureManager featureManager) {
        this.featureManager = featureManager;
    }

    @Override
    public Response<List<FeatureVO>> listFeatureToggle(String username) {
        Map<String, Feature> features = featureManager.listFeatures().stream()
            .collect(Collectors.toMap(Feature::getId, feature -> feature, (oldValue, newValue) -> newValue));

        if (features.isEmpty()) {
            return Response.buildSuccessResp(Collections.emptyList());
        }

        return Response.buildSuccessResp(
            frontendFeatures.stream()
                .map(frontendFeatureId -> {
                    Feature feature = features.get(frontendFeatureId);
                    FeatureVO featureVO = new FeatureVO();
                    featureVO.setId(frontendFeatureId);
                    // 产品需求-无需考虑灰度策略情况
                    featureVO.setEnabled(feature != null && feature.isEnabled());
                    return featureVO;
                })
                .collect(Collectors.toList()));
    }
}
