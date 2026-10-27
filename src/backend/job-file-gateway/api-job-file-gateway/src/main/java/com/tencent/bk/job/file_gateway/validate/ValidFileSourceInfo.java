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

package com.tencent.bk.job.file_gateway.validate;

import com.tencent.bk.job.common.util.LogUtil;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.file_gateway.consts.FileSourceInfoConsts;
import com.tencent.bk.job.file_gateway.model.req.esb.v3.EsbCreateOrUpdateFileSourceV3Req;
import com.tencent.bk.job.file_gateway.model.req.web.FileSourceCreateUpdateReq;
import com.tencent.bk.job.file_gateway.util.FilePrefixValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Map;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * spring validation java 文件源信息合法校验
 */
@Target({TYPE, FIELD, METHOD, PARAMETER, ANNOTATION_TYPE, TYPE_USE})
@Constraint(validatedBy = ValidFileSourceInfo.Validator.class)
@Documented
@Retention(RUNTIME)
public @interface ValidFileSourceInfo {

    String message() default "{validation.constraints.BkRepoBaseUrlInvalid.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Slf4j
    class Validator implements ConstraintValidator<ValidFileSourceInfo, Object> {

        private static final String FILE_PREFIX_INVALID_MESSAGE_TEMPLATE =
            "{validation.constraints.FileSourceFilePrefixInvalid.message}";

        @Override
        @SuppressWarnings("unchecked")
        public boolean isValid(Object value, ConstraintValidatorContext constraintValidatorContext) {
            if (value == null) {
                return true;
            }
            if (value instanceof FileSourceCreateUpdateReq) {
                FileSourceCreateUpdateReq req = (FileSourceCreateUpdateReq) value;
                return checkFilePrefix(req.getCode(), req.getFilePrefix(), "filePrefix", constraintValidatorContext);
            }
            if (value instanceof EsbCreateOrUpdateFileSourceV3Req) {
                EsbCreateOrUpdateFileSourceV3Req req = (EsbCreateOrUpdateFileSourceV3Req) value;
                return checkFilePrefix(req.getCode(), req.getFilePrefix(), "file_prefix", constraintValidatorContext);
            }
            if (value instanceof Map) {
                return isValidInfoMap((Map<String, Object>) value);
            }
            return true;
        }

        private boolean checkFilePrefix(String fileSourceCode,
                                        String filePrefix,
                                        String fieldName,
                                        ConstraintValidatorContext constraintValidatorContext) {
            String invalidReason = FilePrefixValidator.getInvalidReason(filePrefix);
            if (invalidReason == null) {
                return true;
            }
            log.warn("ValidFileSourceInfo rejected illegal {}: code={}, reason={}, value={}",
                fieldName,
                LogUtil.sanitizeForLog(fileSourceCode, 64),
                invalidReason,
                LogUtil.sanitizeForLog(filePrefix, 64));
            if (constraintValidatorContext != null) {
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext.buildConstraintViolationWithTemplate(FILE_PREFIX_INVALID_MESSAGE_TEMPLATE)
                    .addPropertyNode(fieldName)
                    .addConstraintViolation();
            }
            return false;
        }

        private boolean isValidInfoMap(Map<String, Object> value) {
            if (value == null || value.isEmpty()) {
                return true;
            }
            Object baseUrlObj = value.get(FileSourceInfoConsts.KEY_BK_ARTIFACTORY_BASE_URL);
            if (!(baseUrlObj instanceof String)) {
                return false;
            }
            String baseUrl = (String) baseUrlObj;
            if (StringUtils.isBlank(baseUrl)) {
                return false;
            }
            // 限制文件源根地址只能是 http(s)://domain 形式
            if (!isHttpOrHttpsUrl(baseUrl)) {
                return false;
            }
            String urlWithoutScheme = StringUtil.removeHttpOrHttpsSchemeOfUrl(baseUrl);
            return !urlWithoutScheme.contains("/");
        }

        private boolean isHttpOrHttpsUrl(String url) {
            return url.startsWith("http://") || url.startsWith("https://");
        }

    }
}
