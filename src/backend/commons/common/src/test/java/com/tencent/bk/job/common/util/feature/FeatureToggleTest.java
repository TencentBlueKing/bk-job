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

import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.config.FeatureToggleConfig;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.feature.strategy.JobInstanceAttrToggleStrategy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;

import static com.tencent.bk.job.common.util.feature.strategy.ToggleStrategyContextParams.CTX_PARAM_RESOURCE_SCOPE;
import static org.assertj.core.api.Assertions.assertThat;

class FeatureToggleTest {

    @BeforeAll
    static void load() {
        Yaml yaml = new Yaml(new Constructor(FeatureToggleConfig.class));
        InputStream inputStream = FeatureToggleTest.class.getClassLoader()
            .getResourceAsStream("features_1.yaml");
        FeatureToggleConfig featureToggleConfig = yaml.load(inputStream);

        Mockito.mockStatic(ApplicationContextRegister.class)
            .when(() -> ApplicationContextRegister.getBean(FeatureToggleConfig.class))
            .thenReturn(featureToggleConfig);

        FeatureToggle.load();
    }

    @Test
    void checkFeature() {
        FeatureExecutionContext ctx =
            FeatureExecutionContext.builder()
                .addContextParam(CTX_PARAM_RESOURCE_SCOPE, new ResourceScope(ResourceScopeTypeEnum.BIZ, "1000"))
                .addContextParam(JobInstanceAttrToggleStrategy.CTX_PARAM_IS_ALL_GSE_V2_AGENT_AVAILABLE, true)
                .addContextParam(JobInstanceAttrToggleStrategy.CTX_PARAM_STARTUP_MODE, "web")
                .addContextParam(JobInstanceAttrToggleStrategy.CTX_PARAM_OPERATOR, "admin");
        assertThat(FeatureToggle.checkFeature(FeatureIdConstants.FEATURE_GSE_V2, ctx)).isTrue();

        ctx = FeatureExecutionContext.builder()
            .addContextParam(CTX_PARAM_RESOURCE_SCOPE, new ResourceScope(ResourceScopeTypeEnum.BIZ, "2"))
            .addContextParam(JobInstanceAttrToggleStrategy.CTX_PARAM_IS_ALL_GSE_V2_AGENT_AVAILABLE, true)
            .addContextParam(JobInstanceAttrToggleStrategy.CTX_PARAM_STARTUP_MODE, "web")
            .addContextParam(JobInstanceAttrToggleStrategy.CTX_PARAM_OPERATOR, "admin");
        assertThat(FeatureToggle.checkFeature(FeatureIdConstants.FEATURE_GSE_V2, ctx)).isFalse();

        ctx = FeatureExecutionContext.builder()
            .addContextParam(CTX_PARAM_RESOURCE_SCOPE, new ResourceScope(ResourceScopeTypeEnum.BIZ, "100"))
            .addContextParam(JobInstanceAttrToggleStrategy.CTX_PARAM_IS_ALL_GSE_V2_AGENT_AVAILABLE, false)
            .addContextParam(JobInstanceAttrToggleStrategy.CTX_PARAM_STARTUP_MODE, "web")
            .addContextParam(JobInstanceAttrToggleStrategy.CTX_PARAM_OPERATOR, "admin");
        assertThat(FeatureToggle.checkFeature(FeatureIdConstants.FEATURE_GSE_V2, ctx)).isFalse();

        ctx = FeatureExecutionContext.builder()
            .addContextParam(CTX_PARAM_RESOURCE_SCOPE, new ResourceScope(ResourceScopeTypeEnum.BIZ, "100"))
            .addContextParam(JobInstanceAttrToggleStrategy.CTX_PARAM_IS_ALL_GSE_V2_AGENT_AVAILABLE, true)
            .addContextParam(JobInstanceAttrToggleStrategy.CTX_PARAM_STARTUP_MODE, "web")
            .addContextParam(JobInstanceAttrToggleStrategy.CTX_PARAM_OPERATOR, "admin");
        assertThat(FeatureToggle.checkFeature(FeatureIdConstants.FEATURE_GSE_V2, ctx)).isTrue();

        ctx = FeatureExecutionContext.builder()
            .addContextParam(CTX_PARAM_RESOURCE_SCOPE, new ResourceScope(ResourceScopeTypeEnum.BIZ, "200"))
            .addContextParam(JobInstanceAttrToggleStrategy.CTX_PARAM_IS_ALL_GSE_V2_AGENT_AVAILABLE, false)
            .addContextParam(JobInstanceAttrToggleStrategy.CTX_PARAM_STARTUP_MODE, "web")
            .addContextParam(JobInstanceAttrToggleStrategy.CTX_PARAM_OPERATOR, "admin");
        assertThat(FeatureToggle.checkFeature(FeatureIdConstants.FEATURE_GSE_V2, ctx)).isTrue();
        ctx = FeatureExecutionContext.builder()
            .addContextParam(CTX_PARAM_RESOURCE_SCOPE, new ResourceScope(ResourceScopeTypeEnum.BIZ, "200"))
            .addContextParam(JobInstanceAttrToggleStrategy.CTX_PARAM_IS_ALL_GSE_V2_AGENT_AVAILABLE, false)
            .addContextParam(JobInstanceAttrToggleStrategy.CTX_PARAM_STARTUP_MODE, "web")
            .addContextParam(JobInstanceAttrToggleStrategy.CTX_PARAM_OPERATOR, "job");
        assertThat(FeatureToggle.checkFeature(FeatureIdConstants.FEATURE_GSE_V2, ctx)).isFalse();

    }
}
