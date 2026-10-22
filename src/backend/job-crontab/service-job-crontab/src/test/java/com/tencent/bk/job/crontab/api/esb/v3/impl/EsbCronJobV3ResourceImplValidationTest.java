/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 */

package com.tencent.bk.job.crontab.api.esb.v3.impl;

import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.service.CommonAppService;
import com.tencent.bk.job.common.util.toggle.feature.FeatureToggle;
import com.tencent.bk.job.common.web.validation.EsbRequestValidator;
import com.tencent.bk.job.crontab.auth.CronAuthService;
import com.tencent.bk.job.crontab.service.CronJobService;
import com.tencent.bk.job.manage.api.inner.ServiceTaskPlanResource;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static com.tencent.bk.job.common.util.toggle.ToggleEvaluateContext.EMPTY;
import static com.tencent.bk.job.common.util.toggle.feature.FeatureIdConstants.FEATURE_BK_BIZ_ID_COMPATIBLE;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EsbCronJobV3ResourceImplValidationTest {

    private static jakarta.validation.ValidatorFactory validatorFactory;
    private static MockedStatic<FeatureToggle> featureToggle;
    private static EsbRequestValidator esbRequestValidator;

    private CronJobService cronJobService;
    private CronAuthService cronAuthService;
    private ServiceTaskPlanResource taskPlanResource;
    private AppScopeMappingService appScopeMappingService;
    private CommonAppService commonAppService;
    private EsbCronJobV3ResourceImpl resource;

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
        cronJobService = mock(CronJobService.class);
        cronAuthService = mock(CronAuthService.class);
        taskPlanResource = mock(ServiceTaskPlanResource.class);
        appScopeMappingService = mock(AppScopeMappingService.class);
        commonAppService = mock(CommonAppService.class);
        resource = new EsbCronJobV3ResourceImpl(
            cronJobService,
            cronAuthService,
            taskPlanResource,
            appScopeMappingService,
            commonAppService,
            esbRequestValidator
        );
    }

    @Test
    void shouldRejectInvalidScopeForBothGetEndpointsBeforeDependencies() {
        List<ScopeArgs> invalidScopes = List.of(
            new ScopeArgs(null, null),
            new ScopeArgs(ResourceScopeTypeEnum.BIZ.getValue(), null),
            new ScopeArgs(null, "100"),
            new ScopeArgs("invalid", "100")
        );

        invalidScopes.forEach(scope -> {
            assertThatThrownBy(() -> getCronList(null, scope.type, scope.id))
                .isInstanceOf(ConstraintViolationException.class);
            assertThatThrownBy(() -> resource.getCronDetail(
                "user", "app", null, scope.type, scope.id, 1L))
                .isInstanceOf(ConstraintViolationException.class);
        });

        verifyNoInteractions(appScopeMappingService, cronJobService, cronAuthService, taskPlanResource, commonAppService);
    }

    @Test
    void shouldKeepBizIdAndValidScopeListPathsWorking() {
        when(appScopeMappingService.getAppIdByScope(eq(ResourceScopeTypeEnum.BIZ.getValue()), eq("100")))
            .thenReturn(100L);
        when(cronJobService.listPageCronJobInfosWithoutVars(any(), any()))
            .thenReturn(PageData.emptyPageData(0, 20));

        assertThatCode(() -> getCronList(100L, null, null)).doesNotThrowAnyException();
        assertThatCode(() -> getCronList(null, ResourceScopeTypeEnum.BIZ.getValue(), "100"))
            .doesNotThrowAnyException();

        verify(appScopeMappingService, Mockito.times(2))
            .getAppIdByScope(ResourceScopeTypeEnum.BIZ.getValue(), "100");
        verify(cronJobService, Mockito.times(2)).listPageCronJobInfosWithoutVars(any(), any());
    }

    private void getCronList(Long bizId, String scopeType, String scopeId) {
        resource.getCronList(
            "user", "app", bizId, scopeType, scopeId, null, null, null, null,
            null, null, null, null, null, 0, 20
        );
    }

    private static class ScopeArgs {
        private final String type;
        private final String id;

        private ScopeArgs(String type, String id) {
            this.type = type;
            this.id = id;
        }
    }
}
