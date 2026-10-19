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

package com.tencent.bk.job.common.web.validation;

import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import com.tencent.bk.job.common.util.toggle.feature.FeatureToggle;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Set;
import java.util.stream.Collectors;

import static com.tencent.bk.job.common.util.toggle.feature.FeatureIdConstants.FEATURE_BK_BIZ_ID_COMPATIBLE;
import static com.tencent.bk.job.common.util.toggle.ToggleEvaluateContext.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EsbRequestValidatorTest {

    private static jakarta.validation.ValidatorFactory validatorFactory;
    private static MockedStatic<FeatureToggle> featureToggle;
    private static EsbRequestValidator esbRequestValidator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        Validator validator = validatorFactory.getValidator();
        esbRequestValidator = new EsbRequestValidator(validator);
        featureToggle = Mockito.mockStatic(FeatureToggle.class);
        featureToggle.when(() -> FeatureToggle.checkFeature(FEATURE_BK_BIZ_ID_COMPATIBLE, EMPTY))
            .thenReturn(true);
    }

    @AfterAll
    static void tearDown() {
        featureToggle.close();
        validatorFactory.close();
    }

    @Test
    void shouldPassForValidRequest() {
        SimpleRequest request = new SimpleRequest();
        request.name = "valid";

        assertThatCode(() -> esbRequestValidator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void shouldThrowOriginalConstraintViolationsForInvalidRequest() {
        SimpleRequest request = new SimpleRequest();

        assertThatThrownBy(() -> esbRequestValidator.validate(request))
            .isInstanceOf(ConstraintViolationException.class)
            .satisfies(ex -> {
                ConstraintViolationException violationException = (ConstraintViolationException) ex;
                assertThat(violationException.getConstraintViolations())
                    .singleElement()
                    .satisfies(violation -> {
                        assertThat(violation.getPropertyPath().toString()).isEqualTo("name");
                        assertThat(violation.getMessageTemplate()).isEqualTo("name is required");
                    });
            });
    }

    @Test
    void shouldApplyDynamicScopeValidationGroups() {
        assertScopeViolations(new ScopeRequest(), "scopeType");

        ScopeRequest onlyType = new ScopeRequest();
        onlyType.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
        assertScopeViolations(onlyType, "scopeId");

        ScopeRequest onlyId = new ScopeRequest();
        onlyId.setScopeId("100");
        assertScopeViolations(onlyId, "scopeType");

        ScopeRequest illegalType = new ScopeRequest();
        illegalType.setScopeType("invalid");
        illegalType.setScopeId("100");
        assertScopeViolations(illegalType, "scopeType");

        ScopeRequest bizIdWithIllegalScope = new ScopeRequest();
        bizIdWithIllegalScope.setBizId(100L);
        bizIdWithIllegalScope.setScopeType("invalid");
        bizIdWithIllegalScope.setScopeId("100");
        assertScopeViolations(bizIdWithIllegalScope, "scopeType");

        ScopeRequest bizIdOnly = new ScopeRequest();
        bizIdOnly.setBizId(100L);
        assertThatCode(() -> esbRequestValidator.validate(bizIdOnly)).doesNotThrowAnyException();

        ScopeRequest validScope = new ScopeRequest();
        validScope.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
        validScope.setScopeId("100");
        assertThatCode(() -> esbRequestValidator.validate(validScope)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectBizIdOnlyWhenCompatibilityIsDisabled() {
        featureToggle.when(() -> FeatureToggle.checkFeature(FEATURE_BK_BIZ_ID_COMPATIBLE, EMPTY))
            .thenReturn(false);
        try {
            ScopeRequest bizIdOnly = new ScopeRequest();
            bizIdOnly.setBizId(100L);
            assertScopeViolations(bizIdOnly, "scopeId");
        } finally {
            featureToggle.when(() -> FeatureToggle.checkFeature(FEATURE_BK_BIZ_ID_COMPATIBLE, EMPTY))
                .thenReturn(true);
        }
    }

    private void assertScopeViolations(ScopeRequest request, String... expectedPaths) {
        assertThatThrownBy(() -> esbRequestValidator.validate(request))
            .isInstanceOf(ConstraintViolationException.class)
            .satisfies(ex -> {
                ConstraintViolationException violationException = (ConstraintViolationException) ex;
                Set<String> propertyPaths = violationException.getConstraintViolations().stream()
                    .map(violation -> violation.getPropertyPath().toString())
                    .collect(Collectors.toSet());
                assertThat(propertyPaths).contains(expectedPaths);
            });
    }

    private static class SimpleRequest {
        @NotBlank(message = "name is required")
        private String name;
    }

    private static class ScopeRequest extends EsbAppScopeReq {
    }
}
