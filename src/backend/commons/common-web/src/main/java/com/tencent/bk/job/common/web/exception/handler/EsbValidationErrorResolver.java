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
import com.tencent.bk.job.common.web.validation.JacksonPropertyNodeNameProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Selects a deterministic validation error for the single-message ESB V2/V3 response.
 */
final class EsbValidationErrorResolver {

    private static final JacksonPropertyNodeNameProvider EXTERNAL_NAME_RESOLVER =
        new JacksonPropertyNodeNameProvider();

    private static final Comparator<FieldViolationDTO> VALIDATION_ERROR_COMPARATOR =
        Comparator.comparing(FieldViolationDTO::getField, Comparator.nullsFirst(String::compareTo))
            .thenComparing(FieldViolationDTO::getDescription, Comparator.nullsFirst(String::compareTo));

    private EsbValidationErrorResolver() {
    }

    static FieldViolationDTO resolve(BindException ex) {
        Object target = ex.getBindingResult().getTarget();
        Class<?> targetClass = target == null ? null : target.getClass();
        return ex.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> new FieldViolationDTO(
                EXTERNAL_NAME_RESOLVER.getExternalPath(targetClass, fieldError.getField()),
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
        Class<?> rootBeanClass = violation.getRootBeanClass();
        String requestParameterName = getRequestParameterName(violation, rootBeanClass);
        return new FieldViolationDTO(
            requestParameterName == null
                ? EXTERNAL_NAME_RESOLVER.getExternalPath(
                    rootBeanClass,
                    getFieldPathWithoutExecutablePrefix(violation.getPropertyPath())
                )
                : requestParameterName,
            null,
            violation.getMessage()
        );
    }

    private static String getRequestParameterName(ConstraintViolation<?> violation, Class<?> rootBeanClass) {
        if (rootBeanClass == null) {
            return null;
        }
        Iterator<Path.Node> nodes = violation.getPropertyPath().iterator();
        if (!nodes.hasNext()) {
            return null;
        }
        Path.Node executableNode = nodes.next();
        if (executableNode.getKind() != ElementKind.METHOD || !nodes.hasNext()) {
            return null;
        }
        Path.Node parameterNode = nodes.next();
        if (parameterNode.getKind() != ElementKind.PARAMETER || nodes.hasNext()) {
            return null;
        }
        Path.MethodNode methodNode = executableNode.as(Path.MethodNode.class);
        Path.ParameterNode directParameterNode = parameterNode.as(Path.ParameterNode.class);
        return findRequestParameterName(
            rootBeanClass,
            methodNode.getName(),
            methodNode.getParameterTypes(),
            directParameterNode.getParameterIndex(),
            new HashSet<>()
        );
    }

    private static String findRequestParameterName(Class<?> type,
                                                   String methodName,
                                                   List<Class<?>> parameterTypes,
                                                   int parameterIndex,
                                                   Set<Class<?>> visitedTypes) {
        if (type == null || !visitedTypes.add(type)) {
            return null;
        }
        try {
            Method method = type.getDeclaredMethod(methodName, parameterTypes.toArray(new Class<?>[0]));
            Parameter parameter = method.getParameters()[parameterIndex];
            RequestParam requestParam =
                AnnotatedElementUtils.findMergedAnnotation(parameter, RequestParam.class);
            if (requestParam != null) {
                if (StringUtils.hasText(requestParam.name())) {
                    return requestParam.name();
                }
                if (StringUtils.hasText(requestParam.value())) {
                    return requestParam.value();
                }
            }
        } catch (NoSuchMethodException ignored) {
            // Continue with interfaces and the superclass.
        }
        for (Class<?> interfaceType : type.getInterfaces()) {
            String name = findRequestParameterName(
                interfaceType,
                methodName,
                parameterTypes,
                parameterIndex,
                visitedTypes
            );
            if (name != null) {
                return name;
            }
        }
        return findRequestParameterName(
            type.getSuperclass(),
            methodName,
            parameterTypes,
            parameterIndex,
            visitedTypes
        );
    }

    private static String getFieldPathWithoutExecutablePrefix(Path propertyPath) {
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
