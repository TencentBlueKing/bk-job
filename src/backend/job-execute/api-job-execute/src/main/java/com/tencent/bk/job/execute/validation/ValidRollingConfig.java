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

import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.execute.model.web.request.WebFastPushFileRequest;
import com.tencent.bk.job.execute.model.web.vo.RollingConfigVO;
import lombok.extern.slf4j.Slf4j;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Payload;
import jakarta.validation.Validator;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Set;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 类级校验：仅当开启滚动执行（{@code rollingEnabled=true}）时才对滚动配置 {@code rollingConfig} 做级联校验。
 * <p>
 * 由于前端在「未开启滚动」时仍会固定提交一个内容为空的 {@code rollingConfig}，若在字段上直接使用 {@code @Valid}
 * 会无条件级联校验，导致未开启滚动也报出「滚动分批策略表达式不可为空」等错误。这里改为按条件细化校验：
 * <ul>
 *     <li>未开启滚动：忽略 {@code rollingConfig}，不做任何校验；</li>
 *     <li>开启滚动：复用 {@link RollingConfigVO} 上既有的约束（含 type × executionMode 联合分组）做完整校验，
 *     并将违规信息挂回 {@code rollingConfig.xxx} 属性路径，保持与原生级联校验一致的报错定位。</li>
 * </ul>
 */
@Target({ElementType.TYPE})
@Documented
@Constraint(validatedBy = ValidRollingConfig.RollingConfigValidator.class)
@Retention(RUNTIME)
public @interface ValidRollingConfig {

    String message() default "{validation.constraints.RollingConfig_NotNull.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Slf4j
    class RollingConfigValidator implements ConstraintValidator<ValidRollingConfig, WebFastPushFileRequest> {

        private Validator validator;

        @Override
        public void initialize(ValidRollingConfig constraintAnnotation) {
            // 使用项目定制的校验器工厂 Bean（含 i18n 消息插值与 failFast），保证嵌套校验的报错文案与全局一致
            this.validator = ApplicationContextRegister.getBean("jobLocalValidatorFactoryBean", Validator.class);
        }

        @Override
        public boolean isValid(WebFastPushFileRequest request, ConstraintValidatorContext context) {
            // 未开启滚动执行时不校验滚动配置（前端可能带上内容为空的 rollingConfig）
            if (request == null || !request.isRollingEnabled()) {
                return true;
            }
            RollingConfigVO rollingConfig = request.getRollingConfig();
            if (rollingConfig == null) {
                // 开启滚动执行但未提供滚动配置
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        "{validation.constraints.RollingConfig_NotNull.message}")
                    .addPropertyNode("rollingConfig")
                    .addConstraintViolation();
                return false;
            }
            // 复用 RollingConfigVO 既有约束（含分组序列 provider 按 type × executionMode 的联合分组）做完整校验
            Set<ConstraintViolation<RollingConfigVO>> violations = validator.validate(rollingConfig);
            if (violations.isEmpty()) {
                return true;
            }
            context.disableDefaultConstraintViolation();
            for (ConstraintViolation<RollingConfigVO> violation : violations) {
                // 将违规信息挂回 rollingConfig.<field> 路径，保持与字段级 @Valid 级联一致的定位
                context.buildConstraintViolationWithTemplate(violation.getMessage())
                    .addPropertyNode("rollingConfig")
                    .addPropertyNode(violation.getPropertyPath().toString())
                    .addConstraintViolation();
            }
            return false;
        }
    }
}
