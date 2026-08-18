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

package com.tencent.bk.job.execute.validation;

import com.tencent.bk.job.common.model.BasicApp;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.execute.config.ResourceScopeTaskTimeoutParser;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 单元测试 - 超时时间上限校验。
 * <p>
 * 重点锁定：{@link ResourceScopeTaskTimeoutParser} 只在 job-execute 注册，而带该注解的请求体会被
 * job-analysis 的带审批接口继承复用，取不到 Bean 时必须跳过上限校验而不是让整个请求失败（HV000032）。
 */
class ValidTimeoutLimitTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.byProvider(HibernateValidator.class)
            .configure()
            // 用 ParameterMessageInterpolator 避免单测依赖 EL 实现
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();
    }

    @AfterEach
    void tearDown() {
        JobContextUtil.unsetContext();
    }

    @Test
    @DisplayName("上限配置 Bean 不可用时跳过上限校验，不因初始化失败让请求整体失败")
    void givenTimeoutParserUnavailableThenSkipMaxCheck() {
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean(ResourceScopeTaskTimeoutParser.class))
            .thenThrow(new NoSuchBeanDefinitionException("ResourceScopeTaskTimeoutParser"));
        new ApplicationContextRegister().setApplicationContext(context);

        Set<ConstraintViolation<TimeoutHolder>> violations = validator.validate(new TimeoutHolder(99999999));

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("上限配置 Bean 可用时仍严格校验上限")
    void givenTimeoutParserAvailableThenCheckMax() {
        mockTimeoutParser(3600);

        Set<ConstraintViolation<TimeoutHolder>> violations = validator.validate(new TimeoutHolder(3601));

        assertThat(violations).hasSize(1);
    }

    @Test
    @DisplayName("上限之内正常通过")
    void givenTimeoutWithinMaxThenPass() {
        mockTimeoutParser(3600);

        assertThat(validator.validate(new TimeoutHolder(3600))).isEmpty();
    }

    @Test
    @DisplayName("小于最小值一律拒绝：该判定不依赖上限配置 Bean")
    void givenTimeoutSmallerThanMinThenReject() {
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean(ResourceScopeTaskTimeoutParser.class))
            .thenThrow(new NoSuchBeanDefinitionException("ResourceScopeTaskTimeoutParser"));
        new ApplicationContextRegister().setApplicationContext(context);

        Set<ConstraintViolation<TimeoutHolder>> violations = validator.validate(new TimeoutHolder(0));

        assertThat(violations).hasSize(1);
    }

    @Test
    @DisplayName("未传超时时间时不校验")
    void givenNullTimeoutThenPass() {
        assertThat(validator.validate(new TimeoutHolder(null))).isEmpty();
    }

    private void mockTimeoutParser(int maxTimeout) {
        ResourceScopeTaskTimeoutParser timeoutParser = mock(ResourceScopeTaskTimeoutParser.class);
        when(timeoutParser.getMaxTimeoutOrDefault(anyLong(), anyInt())).thenReturn(maxTimeout);
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean(ResourceScopeTaskTimeoutParser.class)).thenReturn(timeoutParser);
        new ApplicationContextRegister().setApplicationContext(context);

        BasicApp app = new BasicApp();
        app.setId(2L);
        JobContextUtil.setApp(app);
    }

    private static class TimeoutHolder {

        @ValidTimeoutLimit
        private Integer timeout;

        TimeoutHolder(Integer timeout) {
            this.timeout = timeout;
        }
    }
}
