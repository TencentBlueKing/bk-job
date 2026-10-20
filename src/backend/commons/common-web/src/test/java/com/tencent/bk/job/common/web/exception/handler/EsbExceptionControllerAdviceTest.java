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

package com.tencent.bk.job.common.web.exception.handler;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.esb.model.v4.V4ErrorCodeEnum;
import com.tencent.bk.job.common.i18n.messagesource.MultiReloadableResourceBundleMessageSource;
import com.tencent.bk.job.common.i18n.service.impl.MessageI18nServiceImpl;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.error.BadRequestDetailDTO;
import com.tencent.bk.job.common.model.error.ErrorDetailDTO;
import com.tencent.bk.job.common.model.error.FieldViolationDTO;
import com.tencent.bk.job.common.util.I18nUtil;
import com.tencent.bk.job.common.web.validation.ValidationConfiguration;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EsbExceptionControllerAdviceTest {

    private static LocalValidatorFactoryBean validatorFactory;
    private static Validator validator;
    private static EsbExceptionControllerAdvice advice;

    @BeforeAll
    static void setUp() {
        MultiReloadableResourceBundleMessageSource messageSource =
            new MultiReloadableResourceBundleMessageSource();
        messageSource.setBasenames(
            "classpath*:i18n/exception/message",
            "classpath*:i18n/validation/ValidationMessages"
        );
        messageSource.setDefaultEncoding("UTF-8");
        ReflectionTestUtils.setField(
            I18nUtil.class,
            "i18nService",
            new MessageI18nServiceImpl(messageSource)
        );
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        ValidationConfiguration validationConfiguration = new ValidationConfiguration();
        validatorFactory = validationConfiguration.localValidatorFactoryBean(messageSource);
        validatorFactory.afterPropertiesSet();
        validator = validatorFactory.getValidator();
        advice = new EsbExceptionControllerAdvice(mock(AuthService.class), validatorFactory);
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
        LocaleContextHolder.resetLocaleContext();
        ReflectionTestUtils.setField(I18nUtil.class, "i18nService", null);
    }

    @Test
    void shouldReturnExternalJsonFieldAndEnglishReason() {
        JsonRequest request = new JsonRequest();

        EsbResp<?> response = handle(new ConstraintViolationException(validator.validate(request)));

        assertThat(response.getCode()).isEqualTo(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON);
        assertThat(response.getMessage()).isEqualTo(
            "Invalid request parameter [job_instance_id], reason: Job instance id must be a positive number"
        );
    }

    @Test
    void shouldKeepNestedCollectionPath() {
        NestedRequest request = new NestedRequest();
        request.steps = Collections.singletonList(new Step());

        EsbResp<?> response = handle(new ConstraintViolationException(validator.validate(request)));

        assertThat(response.getMessage()).isEqualTo(
            "Invalid request parameter [steps[0].account_id], reason: account is required"
        );
    }

    @Test
    void shouldUseRequestParamNameWithoutJavaMethodPrefix() throws Exception {
        Method method = TestController.class.getDeclaredMethod("query", String.class);
        Set<ConstraintViolation<TestController>> violations = validator.forExecutables().validateParameters(
            new TestController(),
            method,
            new Object[]{""}
        );

        EsbResp<?> response = handle(new ConstraintViolationException(violations));

        assertThat(response.getMessage()).isEqualTo(
            "Invalid request parameter [bk_scope_type], reason: Invalid bk_scope_type"
        );
    }

    @Test
    void shouldSortMultipleErrorsAndReturnOnlyFirstWithoutRejectedValue() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError(
            "request", "z_field", "secret-z", false, null, null, "z reason"
        ));
        bindingResult.addError(new FieldError(
            "request", "a_field", "secret-a", false, null, null, "a reason"
        ));
        Method method = TestController.class.getDeclaredMethod("body", JsonRequest.class);
        MethodArgumentNotValidException exception =
            new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);

        ResponseEntity<Object> entity = advice.handleMethodArgumentNotValid(
            exception,
            HttpHeaders.EMPTY,
            HttpStatus.BAD_REQUEST,
            new ServletWebRequest(new MockHttpServletRequest())
        );
        EsbResp<?> response = (EsbResp<?>) entity.getBody();

        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON);
        assertThat(response.getMessage())
            .isEqualTo("Invalid request parameter [a_field], reason: a reason")
            .doesNotContain("secret");
    }

    @Test
    void shouldNotChangeWebAndV4ValidationResponses() {
        BadRequestDetailDTO badRequestDetail = new BadRequestDetailDTO();
        badRequestDetail.addFieldViolation(new FieldViolationDTO("field", "secret", "reason"));
        ErrorDetailDTO errorDetail = new ErrorDetailDTO(badRequestDetail);

        Response<?> webResponse = Response.buildValidateFailResp(errorDetail);
        EsbV4Response<?> v4Response =
            EsbV4Response.paramValidateFail(V4ErrorCodeEnum.INVALID_ARGUMENT, errorDetail);

        assertThat(webResponse.getCode()).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(v4Response.getError().getCode()).isEqualTo(V4ErrorCodeEnum.INVALID_ARGUMENT.getCode());
        assertThat(v4Response.getError().getDetails())
            .singleElement()
            .satisfies(detail -> assertThat(detail.getCode()).isEqualTo(String.valueOf(ErrorCode.FIELD_BIND_FAILED)));
    }

    private EsbResp<?> handle(ConstraintViolationException exception) {
        ResponseEntity<?> entity = advice.handleConstraintViolationException(
            new MockHttpServletRequest(),
            exception
        );
        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (EsbResp<?>) entity.getBody();
    }

    private static class JsonRequest {
        @JsonProperty("job_instance_id")
        @NotNull(message = "{validation.constraints.InvalidJobInstanceId.message}")
        private Long taskInstanceId;
    }

    private static class NestedRequest {
        @Valid
        private java.util.List<Step> steps;
    }

    private static class Step {
        @JsonProperty("account_id")
        @NotBlank(message = "account is required")
        private String accountId;
    }

    private static class TestController {
        void query(
            @RequestParam("bk_scope_type")
            @NotBlank(message = "{validation.constraints.InvalidBkScopeType.message}")
            String scopeType
        ) {
        }

        void body(JsonRequest request) {
        }
    }
}
