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

package com.tencent.bk.job.manage.api.esb.v4.impl;

import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.toggle.feature.FeatureManager;
import com.tencent.bk.job.common.util.toggle.feature.FeatureToggle;
import com.tencent.bk.job.manage.model.esb.v4.req.V4CreateJobPlanRequest;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobPlanVariableItem;
import com.tencent.bk.job.manage.model.esb.v4.req.V4SyncJobPlanRequest;
import com.tencent.bk.job.manage.model.esb.v4.req.V4UpdateJobPlanRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 执行方案写入请求的 Bean Validation 校验。
 *
 * <p>重点是更新接口的 enable_steps：它是声明式的，漏传等于禁用全部步骤，因此必须在校验层拦住而不是当作缺省值放行。
 */
class V4JobPlanWriteRequestValidationTest {

    private static final String SCOPE_TYPE = ResourceScopeTypeEnum.BIZ.getValue();
    private static final String SCOPE_ID = "2";

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        // EsbAppScopeReq 的分组provider会读特性开关，脱离Spring容器时需要先塞一个可用的上下文
        mockSpringContextForFeatureToggle();
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
        resetStaticField(FeatureToggle.class, "featureManager");
        resetStaticField(ApplicationContextRegister.class, "context");
    }

    private static void mockSpringContextForFeatureToggle() {
        FeatureManager featureManager = mock(FeatureManager.class);
        when(featureManager.checkFeature(anyString(), any())).thenReturn(false);
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean(FeatureManager.class)).thenReturn(featureManager);
        new ApplicationContextRegister().setApplicationContext(context);
    }

    /**
     * 静态缓存会串到同一 JVM 里的其它用例，用完必须清掉。
     */
    private static void resetStaticField(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("reset static field fail: " + clazz.getName() + "#" + fieldName, e);
        }
    }

    @Test
    @DisplayName("合法的更新请求无校验错误")
    void valid_update_request_passes() {
        assertThat(validate(updateRequest())).isEmpty();
    }

    @Test
    @DisplayName("更新：enable_steps 缺省时报错，避免被当成禁用全部步骤")
    void update_without_enable_steps_is_rejected() {
        V4UpdateJobPlanRequest request = updateRequest();
        request.setEnableSteps(null);
        assertThat(violatedPaths(request)).contains("enableSteps");
    }

    @Test
    @DisplayName("更新：enable_steps 传空数组时报错")
    void update_with_empty_enable_steps_is_rejected() {
        V4UpdateJobPlanRequest request = updateRequest();
        request.setEnableSteps(Collections.emptyList());
        assertThat(violatedPaths(request)).contains("enableSteps");
    }

    @Test
    @DisplayName("更新：job_plan_id 缺省或非正数时报错")
    void update_requires_positive_plan_id() {
        V4UpdateJobPlanRequest missingId = updateRequest();
        missingId.setJobPlanId(null);
        assertThat(violatedPaths(missingId)).contains("jobPlanId");

        V4UpdateJobPlanRequest zeroId = updateRequest();
        zeroId.setJobPlanId(0L);
        assertThat(violatedPaths(zeroId)).contains("jobPlanId");
    }

    @Test
    @DisplayName("更新：方案名称为空或超长时报错")
    void update_rejects_blank_or_too_long_name() {
        V4UpdateJobPlanRequest blankName = updateRequest();
        blankName.setName("  ");
        assertThat(violatedPaths(blankName)).contains("name");

        V4UpdateJobPlanRequest longName = updateRequest();
        longName.setName(StringUtils.repeat("a", 61));
        assertThat(violatedPaths(longName)).contains("name");
    }

    @Test
    @DisplayName("更新：变量名为空时报错，定位到具体下标")
    void update_rejects_blank_variable_name() {
        V4UpdateJobPlanRequest request = updateRequest();
        V4JobPlanVariableItem variable = new V4JobPlanVariableItem();
        variable.setName("  ");
        variable.setValue("v");
        request.setVariables(Collections.singletonList(variable));

        assertThat(violatedPaths(request)).contains("variables[0].name");
    }

    @Test
    @DisplayName("创建：enable_steps 可以缺省，表示启用模板全部步骤；但传空数组要报错")
    void create_allows_absent_enable_steps_but_rejects_empty_list() {
        V4CreateJobPlanRequest absent = createRequest();
        absent.setEnableSteps(null);
        assertThat(validate(absent)).isEmpty();

        V4CreateJobPlanRequest empty = createRequest();
        empty.setEnableSteps(Collections.emptyList());
        assertThat(violatedPaths(empty)).contains("enableSteps");
    }

    @Test
    @DisplayName("同步：job_plan_id 缺省或非正数时报错")
    void sync_requires_positive_plan_id() {
        assertThat(validate(syncRequest(1L))).isEmpty();
        assertThat(violatedPaths(syncRequest(null))).contains("jobPlanId");
        assertThat(violatedPaths(syncRequest(0L))).contains("jobPlanId");
    }

    private V4UpdateJobPlanRequest updateRequest() {
        V4UpdateJobPlanRequest request = new V4UpdateJobPlanRequest();
        request.setScopeType(SCOPE_TYPE);
        request.setScopeId(SCOPE_ID);
        request.setJobPlanId(50001L);
        request.setName("my-plan");
        request.setEnableSteps(Arrays.asList(201L, 202L));
        return request;
    }

    private V4CreateJobPlanRequest createRequest() {
        V4CreateJobPlanRequest request = new V4CreateJobPlanRequest();
        request.setScopeType(SCOPE_TYPE);
        request.setScopeId(SCOPE_ID);
        request.setJobTemplateId(1000L);
        request.setName("my-plan");
        request.setEnableSteps(Collections.singletonList(101L));
        return request;
    }

    private V4SyncJobPlanRequest syncRequest(Long planId) {
        V4SyncJobPlanRequest request = new V4SyncJobPlanRequest();
        request.setScopeType(SCOPE_TYPE);
        request.setScopeId(SCOPE_ID);
        request.setJobPlanId(planId);
        return request;
    }

    private <T> Set<ConstraintViolation<T>> validate(T request) {
        return validator.validate(request);
    }

    private <T> Set<String> violatedPaths(T request) {
        return validate(request).stream()
            .map(violation -> violation.getPropertyPath().toString())
            .collect(Collectors.toSet());
    }
}
