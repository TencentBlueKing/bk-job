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

package com.tencent.bk.job.common.validation;

import com.tencent.bk.job.common.constant.MySQLTextDataType;
import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.job.common.util.I18nUtil;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 校验敏感参数的长度是否合法
 * 用于校验指定对象中某个敏感参数字段的长度是否超过指定的数据库字段最大长度限制
 */
@Target({ElementType.TYPE})
@Constraint(validatedBy = ValidSensitiveParamLength.Validator.class)
@Documented
@Retention(RUNTIME)
public @interface ValidSensitiveParamLength {
    String message() default "{paramName} {validation.constraints.NotExceedMySQLFieldLength.message} " +
        "limit:{mySQLDataType}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * 被校验的属性名
     */
    String paramName() default "scriptParam";

    /**
     * 敏感参数标识
     */
    String sensitiveFlag() default "isParamSensitive";

    /**
     * 被校验的内容是否做了base64编码
     */
    boolean usedBase64() default true;

    /**
     * 存储在数据库中的数据类型
     */
    MySQLTextDataType mySQLDataType() default MySQLTextDataType.TEXT;

    @Slf4j
    class Validator implements ConstraintValidator<ValidSensitiveParamLength, Object> {
        private static final String MESSAGE_FIELD_LENGTH_EXCEED = "validation.constraints" +
            ".NotExceedMySQLFieldLengthForEncrypted.message";
        private ValidSensitiveParamLength constraintAnnotation;

        @Override
        public void initialize(ValidSensitiveParamLength constraintAnnotation) {
            this.constraintAnnotation = constraintAnnotation;
        }

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            try {
                if (value == null) {
                    return true;
                }

                String message = constraintAnnotation.message();
                String paramName = constraintAnnotation.paramName();
                String sensitiveFlag = constraintAnnotation.sensitiveFlag();
                boolean usedBase64 = constraintAnnotation.usedBase64();
                MySQLTextDataType mySQLDataType = constraintAnnotation.mySQLDataType();

                Field paramField = getFieldRecursively(value.getClass(), paramName);
                Field flagField = getFieldRecursively(value.getClass(), sensitiveFlag);
                if (paramField == null || flagField == null) {
                    log.warn("The field is not in the current object, skip verification. fieldName={}|{}, class={}",
                        paramName, sensitiveFlag, value.getClass());
                    return true;
                }
                paramField.setAccessible(true);
                flagField.setAccessible(true);

                Object paramValue = paramField.get(value);
                Object flagValue = flagField.get(value);
                if (!(paramValue instanceof String)) return true;

                long currentLength = usedBase64 ? Base64Util.calcOriginBytesLength((String) paramValue) :
                    ((String) paramValue).getBytes(StandardCharsets.UTF_8).length;
                boolean isEncrypted = flagValue instanceof Integer && ((Integer) flagValue) == 1;
                long maxLength = isEncrypted ? mySQLDataType.getMaximumLengthForEncrypted() :
                    mySQLDataType.getMaximumLength();

                if (currentLength > maxLength) {
                    String errorMessage  = message;
                    if (isEncrypted) {
                        errorMessage = I18nUtil.getI18nMessage(MESSAGE_FIELD_LENGTH_EXCEED,
                            new Object[]{paramName, String.valueOf(maxLength)});
                    }
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(errorMessage)
                        .addPropertyNode(paramName)
                        .addConstraintViolation();
                    return false;
                }
                return true;
            } catch (Exception e) {
                log.warn("An exception occurred during the verification sensitive param, skip verification", e);
                return true;
            }
        }

        /**
         * 递归查找字段
         */
        private Field getFieldRecursively(Class<?> clazz, String fieldName) {
            while (clazz != null) {
                try {
                    Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            return null;
        }
    }
}
