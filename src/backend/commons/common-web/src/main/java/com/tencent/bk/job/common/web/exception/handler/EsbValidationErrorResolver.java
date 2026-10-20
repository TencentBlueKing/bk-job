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

import com.tencent.bk.job.common.model.error.FieldViolationDTO;
import org.springframework.validation.BindException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Selects a deterministic validation error for the single-message ESB V2/V3 response.
 */
final class EsbValidationErrorResolver {

    private static final Comparator<FieldViolationDTO> VALIDATION_ERROR_COMPARATOR =
        Comparator.comparing(FieldViolationDTO::getField, Comparator.nullsFirst(String::compareTo))
            .thenComparing(FieldViolationDTO::getDescription, Comparator.nullsFirst(String::compareTo));

    private EsbValidationErrorResolver() {
    }

    static FieldViolationDTO resolve(BindException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> new FieldViolationDTO(
                fieldError.getField(),
                null,
                fieldError.getDefaultMessage()
            ))
            .min(VALIDATION_ERROR_COMPARATOR)
            .orElse(null);
    }

    static FieldViolationDTO resolve(ConstraintViolationException ex) {
        return ex.getConstraintViolations().stream()
            .map(EsbValidationErrorResolver::toFieldViolation)
            .min(VALIDATION_ERROR_COMPARATOR)
            .orElse(null);
    }

    private static FieldViolationDTO toFieldViolation(ConstraintViolation<?> violation) {
        return new FieldViolationDTO(
            getExternalFieldPath(violation.getPropertyPath()),
            null,
            violation.getMessage()
        );
    }

    private static String getExternalFieldPath(Path propertyPath) {
        String path = propertyPath.toString();
        Iterator<Path.Node> nodes = propertyPath.iterator();
        if (!nodes.hasNext()) {
            return path;
        }
        ElementKind firstNodeKind = nodes.next().getKind();
        if (firstNodeKind != ElementKind.METHOD && firstNodeKind != ElementKind.CONSTRUCTOR) {
            return path;
        }
        int separatorIndex = path.indexOf('.');
        return separatorIndex < 0 ? path : path.substring(separatorIndex + 1);
    }
}
