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

package com.tencent.bk.job.manage.api.esb.impl.v3;

import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.toggle.feature.FeatureToggle;
import com.tencent.bk.job.common.web.validation.EsbRequestValidator;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static com.tencent.bk.job.common.util.toggle.ToggleEvaluateContext.EMPTY;
import static com.tencent.bk.job.common.util.toggle.feature.FeatureIdConstants.FEATURE_BK_BIZ_ID_COMPATIBLE;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EsbPlanV3ResourceImplTest {

    private static jakarta.validation.ValidatorFactory validatorFactory;
    private static MockedStatic<FeatureToggle> featureToggle;
    private static EsbRequestValidator esbRequestValidator;

    private TaskPlanService taskPlanService;
    private AppScopeMappingService appScopeMappingService;
    private EsbPlanV3ResourceImpl resource;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        esbRequestValidator = new EsbRequestValidator(validatorFactory.getValidator());
        featureToggle = Mockito.mockStatic(FeatureToggle.class);
        featureToggle.when(() -> FeatureToggle.checkFeature(FEATURE_BK_BIZ_ID_COMPATIBLE, EMPTY))
            .thenReturn(true);
    }

    @AfterAll
    static void tearDownValidator() {
        featureToggle.close();
        validatorFactory.close();
    }

    @BeforeEach
    void setUp() {
        taskPlanService = mock(TaskPlanService.class);
        appScopeMappingService = mock(AppScopeMappingService.class);
        resource = new EsbPlanV3ResourceImpl(
            taskPlanService,
            appScopeMappingService,
            esbRequestValidator
        );
    }

    @Test
    void shouldRejectMissingScopeBeforeMapping() {
        assertInvalidScope(null, null, null);
    }

    @Test
    void shouldRejectMissingScopeIdBeforeMapping() {
        assertInvalidScope(null, ResourceScopeTypeEnum.BIZ.getValue(), null);
    }

    @Test
    void shouldRejectMissingScopeTypeBeforeMapping() {
        assertInvalidScope(null, null, "100");
    }

    @Test
    void shouldRejectIllegalScopeTypeBeforeMapping() {
        assertInvalidScope(null, "invalid", "100");
    }

    @Test
    void shouldKeepBizIdCompatiblePathWorking() {
        when(appScopeMappingService.getAppIdByScope(ResourceScopeTypeEnum.BIZ.getValue(), "100"))
            .thenReturn(100L);
        mockEmptyPlanPage();

        assertThatCode(() -> getPlanList(100L, null, null)).doesNotThrowAnyException();

        verify(appScopeMappingService).getAppIdByScope(ResourceScopeTypeEnum.BIZ.getValue(), "100");
        verify(taskPlanService).listPageTaskPlansBasicInfo(any(), any(), isNull());
    }

    @Test
    void shouldKeepValidScopePathWorking() {
        when(appScopeMappingService.getAppIdByScope(ResourceScopeTypeEnum.BIZ.getValue(), "100"))
            .thenReturn(100L);
        mockEmptyPlanPage();

        assertThatCode(() -> getPlanList(null, ResourceScopeTypeEnum.BIZ.getValue(), "100"))
            .doesNotThrowAnyException();

        verify(appScopeMappingService).getAppIdByScope(ResourceScopeTypeEnum.BIZ.getValue(), "100");
        verify(taskPlanService).listPageTaskPlansBasicInfo(any(), any(), isNull());
    }

    private void assertInvalidScope(Long bizId, String scopeType, String scopeId) {
        assertThatThrownBy(() -> getPlanList(bizId, scopeType, scopeId))
            .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(appScopeMappingService, taskPlanService);
    }

    private void mockEmptyPlanPage() {
        PageData<TaskPlanInfoDTO> emptyPage = PageData.emptyPageData(0, 20);
        when(taskPlanService.listPageTaskPlansBasicInfo(any(), any(), isNull())).thenReturn(emptyPage);
    }

    private void getPlanList(Long bizId, String scopeType, String scopeId) {
        resource.getPlanList(
            "user",
            "app",
            bizId,
            scopeType,
            scopeId,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }
}
