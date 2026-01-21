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

package com.tencent.bk.job.common.esb.validate;

import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import org.apache.commons.lang3.StringUtils;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 资源范围不可为空校验
 */
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE, TYPE_USE})
@Constraint(validatedBy = ValidBkScope.Validator.class)
@Documented
@Retention(RUNTIME)
public @interface ValidBkScope {

    String message() default "资源范围相关参数bk_scope_type或bk_scope_id不正确";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidBkScope, EsbAppScopeReq> {

        @Override
        public boolean isValid(EsbAppScopeReq appScopeReq, ConstraintValidatorContext hibernateContext) {
            if (appScopeReq.getBizId() != null) {
                return true;
            }
            if (!ResourceScopeTypeEnum.isValid(appScopeReq.getScopeType())) {
                hibernateContext.disableDefaultConstraintViolation();
                hibernateContext
                    .buildConstraintViolationWithTemplate("{validation.constraints.InvalidBkScopeType.message}")
                    .addPropertyNode("scopeType")
                    .addConstraintViolation();
                return false;
            }
            if (StringUtils.isBlank(appScopeReq.getScopeId())) {
                hibernateContext.disableDefaultConstraintViolation();
                hibernateContext
                    .buildConstraintViolationWithTemplate("{validation.constraints.InvalidBkScopeId.message}")
                    .addPropertyNode("scopeId")
                    .addConstraintViolation();
                return false;
            }
            return true;
        }
    }
}
