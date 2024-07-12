/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.job.common.validation;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import lombok.extern.slf4j.Slf4j;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;


/**
 * 条件枚举验证器
 */
@Slf4j
public class ConditionalCheckEnumValidator implements ConstraintValidator<ConditionalCheckEnum, Object> {
    private CheckEnum.Validator checkEnumValidator = new CheckEnum.Validator();
    private String baseField;
    private String dependentField;

    @Override
    public void initialize(ConditionalCheckEnum constraintAnnotation) {
        this.baseField = constraintAnnotation.baseField();
        this.dependentField = constraintAnnotation.dependentField();

        checkEnumValidator.initialize(new CheckEnum() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return CheckEnum.class;
            }
            @Override
            public String message() {
                return constraintAnnotation.message();
            }
            @Override
            public Class<?>[] groups() {
                return constraintAnnotation.groups();
            }
            @Override
            public Class<? extends Payload>[] payload() {
                return constraintAnnotation.payload();
            }
            @Override
            public Class<? extends Enum<?>> enumClass() {
                return constraintAnnotation.enumClass();
            }
            @Override
            public String enumMethod() {
                return constraintAnnotation.enumMethod();
            }
            @Override
            public boolean notNull() {
                return constraintAnnotation.notNull();
            }
        });
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            Field baseFieldObj = value.getClass().getDeclaredField(baseField);
            Field dependentFieldObj = value.getClass().getDeclaredField(dependentField);

            baseFieldObj.setAccessible(true);
            dependentFieldObj.setAccessible(true);

            Object baseFieldValue = baseFieldObj.get(value);
            Object dependentFieldValue = dependentFieldObj.get(value);

            if (ValidationUtil.isValuePositiveOrNonEmpty(baseFieldValue)) {
                return validateDependentField(dependentFieldValue, context);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.error("conditional check enum exception={}, baseField={}, dependentField={}",
                e.getMessage(), baseField, dependentField);
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        return true;
    }

    private boolean validateDependentField(Object dependentFieldValue, ConstraintValidatorContext context) {
        if (dependentFieldValue instanceof List) {
            for (Object item : (List<?>) dependentFieldValue) {
                if (!checkEnumValidator.isValid(item, context)) {
                    return false;
                }
            }
            return true;
        } else {
            return checkEnumValidator.isValid(dependentFieldValue, context);
        }
    }
}
