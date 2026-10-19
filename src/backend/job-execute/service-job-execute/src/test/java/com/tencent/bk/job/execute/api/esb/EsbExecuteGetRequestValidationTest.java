/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 */

package com.tencent.bk.job.execute.api.esb;

import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.toggle.feature.FeatureToggle;
import com.tencent.bk.job.common.web.validation.EsbRequestValidator;
import com.tencent.bk.job.execute.api.esb.v2.impl.EsbGetJobInstanceLogResourceImpl;
import com.tencent.bk.job.execute.api.esb.v2.impl.EsbGetJobInstanceStatusResourceImpl;
import com.tencent.bk.job.execute.api.esb.v3.EsbGetJobInstanceGlobalVarValueV3ResourceImpl;
import com.tencent.bk.job.execute.api.esb.v3.EsbGetJobInstanceIpLogV3ResourceImpl;
import com.tencent.bk.job.execute.api.esb.v3.EsbGetJobInstanceListV3ResourceImpl;
import com.tencent.bk.job.execute.api.esb.v3.EsbGetJobInstanceStatusV3ResourceImpl;
import com.tencent.bk.job.execute.config.OpenApiProperties;
import com.tencent.bk.job.execute.service.FileExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.ScriptExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.StepInstanceVariableValueService;
import com.tencent.bk.job.execute.service.TaskInstanceAccessProcessor;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.TaskResultService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static com.tencent.bk.job.common.util.toggle.ToggleEvaluateContext.EMPTY;
import static com.tencent.bk.job.common.util.toggle.feature.FeatureIdConstants.FEATURE_BK_BIZ_ID_COMPATIBLE;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EsbExecuteGetRequestValidationTest {

    private static jakarta.validation.ValidatorFactory validatorFactory;
    private static MockedStatic<FeatureToggle> featureToggle;
    private static EsbRequestValidator esbRequestValidator;

    private AppScopeMappingService appScopeMappingService;
    private TaskResultService taskResultService;
    private TaskInstanceService taskInstanceService;
    private ScriptExecuteObjectTaskService scriptExecuteObjectTaskService;
    private FileExecuteObjectTaskService fileExecuteObjectTaskService;
    private StepInstanceService stepInstanceService;
    private LogService logService;
    private TaskInstanceAccessProcessor taskInstanceAccessProcessor;
    private StepInstanceVariableValueService stepInstanceVariableValueService;
    private List<ScopeEndpoint> endpoints;
    private Object[] businessServices;

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
        appScopeMappingService = mock(AppScopeMappingService.class);
        taskResultService = mock(TaskResultService.class);
        taskInstanceService = mock(TaskInstanceService.class);
        scriptExecuteObjectTaskService = mock(ScriptExecuteObjectTaskService.class);
        fileExecuteObjectTaskService = mock(FileExecuteObjectTaskService.class);
        stepInstanceService = mock(StepInstanceService.class);
        logService = mock(LogService.class);
        taskInstanceAccessProcessor = mock(TaskInstanceAccessProcessor.class);
        stepInstanceVariableValueService = mock(StepInstanceVariableValueService.class);

        EsbGetJobInstanceListV3ResourceImpl listResource = new EsbGetJobInstanceListV3ResourceImpl(
            taskResultService, appScopeMappingService, mock(OpenApiProperties.class), esbRequestValidator);
        EsbGetJobInstanceStatusV3ResourceImpl statusV3Resource = new EsbGetJobInstanceStatusV3ResourceImpl(
            taskInstanceService, scriptExecuteObjectTaskService, fileExecuteObjectTaskService,
            appScopeMappingService, stepInstanceService, esbRequestValidator);
        EsbGetJobInstanceIpLogV3ResourceImpl ipLogResource = new EsbGetJobInstanceIpLogV3ResourceImpl(
            logService, stepInstanceService, taskInstanceAccessProcessor, appScopeMappingService,
            esbRequestValidator);
        EsbGetJobInstanceGlobalVarValueV3ResourceImpl globalVarResource =
            new EsbGetJobInstanceGlobalVarValueV3ResourceImpl(
                stepInstanceVariableValueService, taskInstanceAccessProcessor, appScopeMappingService,
                esbRequestValidator);
        EsbGetJobInstanceStatusResourceImpl statusV2Resource = new EsbGetJobInstanceStatusResourceImpl(
            taskInstanceService, scriptExecuteObjectTaskService, fileExecuteObjectTaskService,
            appScopeMappingService, stepInstanceService, esbRequestValidator);
        EsbGetJobInstanceLogResourceImpl logV2Resource = new EsbGetJobInstanceLogResourceImpl(
            taskInstanceService, scriptExecuteObjectTaskService, fileExecuteObjectTaskService, logService,
            taskInstanceAccessProcessor, appScopeMappingService, stepInstanceService, esbRequestValidator);

        endpoints = List.of(
            (bizId, type, id) -> listResource.getJobInstanceList(
                "user", "app", bizId, type, id, 1L, 2L, null, null, null,
                null, null, null, null, null, 0, 20),
            (bizId, type, id) -> statusV3Resource.getJobInstanceStatus(
                "user", "app", bizId, type, id, 1L, false),
            (bizId, type, id) -> ipLogResource.getJobInstanceIpLog(
                "user", "app", bizId, type, id, 1L, 1L, 1L, null, null),
            (bizId, type, id) -> globalVarResource.getJobInstanceGlobalVarValue(
                "user", "app", bizId, type, id, 1L),
            (bizId, type, id) -> statusV2Resource.getJobInstanceStatus(
                "user", "app", bizId, type, id, 1L),
            (bizId, type, id) -> logV2Resource.getJobInstanceLog(
                "user", "app", bizId, type, id, 1L)
        );
        businessServices = new Object[]{
            taskResultService,
            taskInstanceService,
            scriptExecuteObjectTaskService,
            fileExecuteObjectTaskService,
            stepInstanceService,
            logService,
            taskInstanceAccessProcessor,
            stepInstanceVariableValueService
        };
        JobContextUtil.setUser(new User("default", "user", "user"));
    }

    @AfterEach
    void tearDown() {
        JobContextUtil.unsetContext();
    }

    @Test
    void shouldRejectInvalidScopeForAllSixGetEndpointsBeforeDependencies() {
        List<ScopeArgs> invalidScopes = List.of(
            new ScopeArgs(null, null),
            new ScopeArgs(ResourceScopeTypeEnum.BIZ.getValue(), null),
            new ScopeArgs(null, "100"),
            new ScopeArgs("invalid", "100")
        );

        endpoints.forEach(endpoint -> invalidScopes.forEach(scope ->
            assertThatThrownBy(() -> endpoint.invoke(null, scope.type, scope.id))
                .isInstanceOf(ConstraintViolationException.class)));

        verifyNoInteractions(appScopeMappingService);
        verifyNoInteractions(businessServices);
    }

    @Test
    void shouldKeepBizIdAndValidScopePathsWorking() {
        when(appScopeMappingService.getAppIdByScope(eq(ResourceScopeTypeEnum.BIZ.getValue()), eq("100")))
            .thenReturn(100L);
        when(stepInstanceVariableValueService.computeOutputVariableValuesForAllStep(anyLong()))
            .thenReturn(Collections.emptyList());
        ScopeEndpoint globalVarEndpoint = endpoints.get(3);

        assertThatCode(() -> globalVarEndpoint.invoke(100L, null, null)).doesNotThrowAnyException();
        assertThatCode(() -> globalVarEndpoint.invoke(null, ResourceScopeTypeEnum.BIZ.getValue(), "100"))
            .doesNotThrowAnyException();

        verify(appScopeMappingService, Mockito.times(2))
            .getAppIdByScope(ResourceScopeTypeEnum.BIZ.getValue(), "100");
        verify(stepInstanceVariableValueService, Mockito.times(2))
            .computeOutputVariableValuesForAllStep(1L);
    }

    @FunctionalInterface
    private interface ScopeEndpoint {
        void invoke(Long bizId, String scopeType, String scopeId);
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
