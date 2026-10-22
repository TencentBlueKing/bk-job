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
import com.tencent.bk.job.common.annotation.EsbAPI;
import com.tencent.bk.job.common.annotation.EsbV4API;
import com.tencent.bk.job.common.annotation.WebAPI;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.web.config.CommonWebMvcAutoConfiguration;
import com.tencent.bk.job.common.web.validation.EsbRequestValidator;
import com.tencent.bk.job.common.web.validation.ValidationConfiguration;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig(EsbValidationProductionPathTest.TestConfiguration.class)
class EsbValidationProductionPathTest {

    private final WebApplicationContext context;
    private MockMvc mockMvc;

    EsbValidationProductionPathTest(WebApplicationContext context) {
        this.context = context;
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void shouldUseExternalFieldForV2GetValidation() throws Exception {
        mockMvc.perform(get("/esb/api/v2/test-validation"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1241010))
            .andExpect(jsonPath("$.message").value(
                "请求参数[bk_scope_type]不合法，原因：非法的资源范围类型"
            ));
    }

    @Test
    void shouldUseEnglishExternalFieldForV3GetValidation() throws Exception {
        mockMvc.perform(get("/esb/api/v3/test-validation")
                .header("blueking-language", "en"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1241010))
            .andExpect(jsonPath("$.message").value(
                "Invalid request parameter [bk_scope_type], reason: Invalid bk_scope_type"
            ));
    }

    @Test
    void shouldUseInterfaceRequestParamNameThroughMethodValidationProxy() throws Exception {
        Assertions.assertTrue(AopUtils.isAopProxy(context.getBean(ProxiedTestEsbResource.class)));

        mockMvc.perform(get("/esb/api/v3/test-proxied-validation")
                .param("job_instance_id", "0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1241010))
            .andExpect(jsonPath("$.message").value(
                "请求参数[job_instance_id]不合法，原因：任务实例ID必须为正整数"
            ));
    }

    @Test
    void shouldPreferExistingLangHeaderOverBluekingLanguage() throws Exception {
        mockMvc.perform(get("/esb/api/v3/test-validation")
                .header("lang", "zh_CN")
                .header("blueking-language", "en"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value(
                "请求参数[bk_scope_type]不合法，原因：非法的资源范围类型"
            ));
    }

    @Test
    void shouldUseEnglishExternalFieldForV3PostValidation() throws Exception {
        mockMvc.perform(post("/esb/api/v3/test-validation")
                .header("blueking-language", "en")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1241010))
            .andExpect(jsonPath("$.message").value(
                "Invalid request parameter [bk_scope_type], reason: Invalid bk_scope_type"
            ));
    }

    @Test
    void shouldMapEverySegmentOfNestedPostPath() throws Exception {
        mockMvc.perform(post("/esb/api/v3/test-nested-validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"host_password_list\":[{\"encrypted_password\":\"\"}]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1241010))
            .andExpect(jsonPath("$.message").value(
                "请求参数[host_password_list[0].encrypted_password]不合法，原因：密码不能为空"
            ));
    }

    @Test
    void shouldKeepWebValidationCodeAndDefaultJavaFieldName() throws Exception {
        mockMvc.perform(post("/web/test-validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(1240003))
            .andExpect(jsonPath(
                "$.errorDetail.badRequestDetail.fieldViolations[0].field"
            ).value("internalName"));
    }

    @Test
    void shouldKeepV4ValidationCodes() throws Exception {
        mockMvc.perform(post("/esb/api/v4/test-validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"))
            .andExpect(jsonPath("$.error.details[0].code").value("1241025"));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({
        com.tencent.bk.job.common.i18n.config.I18nAutoConfiguration.class,
        com.tencent.bk.job.common.web.config.I18nAutoConfiguration.class,
        ValidationConfiguration.class,
        CommonWebMvcAutoConfiguration.class
    })
    static class TestConfiguration implements WebMvcConfigurer {

        @Override
        public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
            converters.add(new MappingJackson2HttpMessageConverter());
        }

        @Bean
        Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
            return new Jackson2ObjectMapperBuilder();
        }

        @Bean
        ApplicationContextRegister applicationContextRegister() {
            return new ApplicationContextRegister();
        }

        @Bean
        AuthService authService() {
            return mock(AuthService.class);
        }

        @Bean
        WebAuthService webAuthService() {
            return mock(WebAuthService.class);
        }

        @Bean
        EsbExceptionControllerAdvice esbExceptionControllerAdvice(
            AuthService authService,
            @Qualifier("jobLocalValidatorFactoryBean") Validator validator
        ) {
            return new EsbExceptionControllerAdvice(authService, validator);
        }

        @Bean
        WebExceptionControllerAdvice webExceptionControllerAdvice(WebAuthService webAuthService) {
            return new WebExceptionControllerAdvice(webAuthService);
        }

        @Bean
        EsbV4ExceptionControllerAdvice esbV4ExceptionControllerAdvice(AuthService authService) {
            return new EsbV4ExceptionControllerAdvice(authService);
        }

        @Bean
        static MethodValidationPostProcessor methodValidationPostProcessor(
            MessageSource messageSource
        ) {
            LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
            validator.setValidationMessageSource(messageSource);
            validator.afterPropertiesSet();
            MethodValidationPostProcessor postProcessor = new MethodValidationPostProcessor();
            postProcessor.setValidator(validator);
            postProcessor.setProxyTargetClass(true);
            return postProcessor;
        }

        @Bean
        TestEsbController testEsbController(EsbRequestValidator validator) {
            return new TestEsbController(validator);
        }

        @Bean
        ProxiedTestEsbResourceImpl proxiedTestEsbResource() {
            return new ProxiedTestEsbResourceImpl();
        }

        @Bean
        TestWebController testWebController() {
            return new TestWebController();
        }

        @Bean
        TestV4Controller testV4Controller() {
            return new TestV4Controller();
        }
    }

    @EsbAPI
    @RestController
    static class TestEsbController {
        private final EsbRequestValidator validator;

        TestEsbController(EsbRequestValidator validator) {
            this.validator = validator;
        }

        @GetMapping({"/esb/api/v2/test-validation", "/esb/api/v3/test-validation"})
        void getValidation(
            @RequestParam(value = "bk_scope_type", required = false) String scopeType
        ) {
            ScopeRequest request = new ScopeRequest();
            request.scopeType = scopeType;
            validator.validate(request);
        }

        @PostMapping("/esb/api/v3/test-validation")
        void postValidation(@Valid @RequestBody ScopeRequest request) {
        }

        @PostMapping("/esb/api/v3/test-nested-validation")
        void postNestedValidation(@Valid @RequestBody NestedRequest request) {
        }
    }

    @Validated
    @EsbAPI
    @RequestMapping("/esb/api/v3")
    interface ProxiedTestEsbResource {
        @GetMapping("/test-proxied-validation")
        void validate(
            @RequestParam("job_instance_id")
            @Min(value = 1L, message = "{validation.constraints.InvalidJobInstanceId.message}")
            Long taskInstanceId
        );
    }

    @RestController
    static class ProxiedTestEsbResourceImpl implements ProxiedTestEsbResource {
        @Override
        public void validate(Long taskInstanceId) {
        }
    }

    @ValidScope
    static class ScopeRequest {
        @JsonProperty("bk_scope_type")
        private String scopeType;
    }

    static class NestedRequest {
        @JsonProperty("host_password_list")
        @Valid
        private List<HostPassword> hostPasswordList;
    }

    static class HostPassword {
        @JsonProperty("encrypted_password")
        @NotBlank(message = "密码不能为空")
        private String encryptedPassword;
    }

    static class DefaultMvcRequest {
        @JsonProperty("external_name")
        @NotBlank(message = "required")
        private String internalName;
    }

    @WebAPI
    @RestController
    static class TestWebController {
        @PostMapping("/web/test-validation")
        void post(@Valid @RequestBody DefaultMvcRequest request) {
        }
    }

    @EsbV4API
    @RestController
    static class TestV4Controller {
        @PostMapping("/esb/api/v4/test-validation")
        void post(@Valid @RequestBody DefaultMvcRequest request) {
        }
    }

    @Target({TYPE, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    @Constraint(validatedBy = ScopeValidator.class)
    @Documented
    @interface ValidScope {
        String message() default "{validation.constraints.InvalidBkScopeType.message}";

        Class<?>[] groups() default {};

        Class<? extends jakarta.validation.Payload>[] payload() default {};
    }

    static class ScopeValidator implements ConstraintValidator<ValidScope, ScopeRequest> {
        @Override
        public boolean isValid(ScopeRequest value, ConstraintValidatorContext context) {
            if (value != null && value.scopeType != null && !value.scopeType.trim().isEmpty()) {
                return true;
            }
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("scopeType")
                .addConstraintViolation();
            return false;
        }
    }
}
