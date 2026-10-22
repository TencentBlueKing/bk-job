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

package com.tencent.bk.job.common.web.validation;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import org.hibernate.validator.spi.nodenameprovider.JavaBeanProperty;
import org.hibernate.validator.spi.nodenameprovider.Property;
import org.hibernate.validator.spi.nodenameprovider.PropertyNodeNameProvider;

public class JacksonPropertyNodeNameProvider implements PropertyNodeNameProvider {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName(Property property) {
        if (property instanceof JavaBeanProperty) {
            return getJavaBeanPropertyName((JavaBeanProperty) property);
        }

        return getDefaultName(property);
    }

    private String getJavaBeanPropertyName(JavaBeanProperty property) {
        JavaType type = objectMapper.constructType(property.getDeclaringClass());
        BeanDescription desc = objectMapper.getSerializationConfig().introspect(type);

        return desc.findProperties()
            .stream()
            .filter(prop -> prop.getInternalName().equals(property.getName()))
            .map(BeanPropertyDefinition::getName)
            .findFirst()
            .orElse(property.getName());
    }

    private String getDefaultName(Property property) {
        return property.getName();
    }

    /**
     * Resolve every Java property segment in a validation path to its Jackson external name.
     *
     * <p>Hibernate's {@link PropertyNodeNameProvider} is not invoked for nodes explicitly added by a custom
     * constraint validator. Spring MVC's {@code FieldError} also always exposes Java property names. This method
     * provides the same Jackson-based mapping for those two paths.</p>
     */
    public String getExternalPath(Class<?> rootBeanClass, String propertyPath) {
        if (rootBeanClass == null || propertyPath == null || propertyPath.isEmpty()) {
            return propertyPath;
        }
        JavaType currentType = objectMapper.constructType(rootBeanClass);
        String[] segments = propertyPath.split("\\.");
        StringBuilder externalPath = new StringBuilder();
        for (String segment : segments) {
            if (externalPath.length() > 0) {
                externalPath.append('.');
            }
            int containerIndex = segment.indexOf('[');
            String propertyName = containerIndex < 0 ? segment : segment.substring(0, containerIndex);
            String containerSuffix = containerIndex < 0 ? "" : segment.substring(containerIndex);
            BeanPropertyDefinition property = findProperty(currentType, propertyName);
            if (property == null) {
                externalPath.append(segment);
                currentType = null;
                continue;
            }
            externalPath.append(property.getName()).append(containerSuffix);
            currentType = getNestedType(property);
        }
        return externalPath.toString();
    }

    private BeanPropertyDefinition findProperty(JavaType type, String propertyName) {
        if (type == null) {
            return null;
        }
        BeanDescription description = objectMapper.getSerializationConfig().introspect(type);
        return description.findProperties()
            .stream()
            .filter(property -> property.getInternalName().equals(propertyName)
                || property.getName().equals(propertyName))
            .findFirst()
            .orElse(null);
    }

    private JavaType getNestedType(BeanPropertyDefinition property) {
        if (property.getPrimaryMember() == null) {
            return null;
        }
        JavaType propertyType = property.getPrimaryMember().getType();
        return propertyType.isContainerType() ? propertyType.getContentType() : propertyType;
    }
}
