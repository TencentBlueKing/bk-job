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

package com.tencent.bk.job.common.constant;

/**
 * 动态条件过滤器支持的可查询字段全集。
 * <p>
 * 字段名遵循 bk-cmdb filter 命名约定：全小写 snake_case，
 * 容器自身字段以 {@code container_} 前缀，所属 Pod 字段以 {@code pod_} 前缀。
 * <p>
 * 当前字段全集仅保留对动态容器条件过滤有产品化需求的字段：容器 ID、容器名称、容器 UID、
 * Pod ID、Pod 名称、Pod 标签。
 * <p>
 * 对外暴露的字段子集由后端 EXPOSED_FIELDS 配置点控制。
 */
public enum QueryableContainerField {
    CONTAINER_ID("container_id", CMDBContainerFieldType.NUMERIC),
    CONTAINER_NAME("container_name", CMDBContainerFieldType.STRING),
    CONTAINER_CONTAINER_UID("container_container_uid", CMDBContainerFieldType.STRING),
    POD_ID("pod_id", CMDBContainerFieldType.NUMERIC),
    POD_NAME("pod_name", CMDBContainerFieldType.STRING),
    POD_LABELS("pod_labels", CMDBContainerFieldType.OBJECT);

    private final String fieldName;
    private final CMDBContainerFieldType fieldType;

    QueryableContainerField(String fieldName, CMDBContainerFieldType fieldType) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public CMDBContainerFieldType getFieldType() {
        return fieldType;
    }

    public static QueryableContainerField fromFieldName(String fieldName) {
        if (fieldName == null) {
            return null;
        }
        for (QueryableContainerField field : values()) {
            if (field.fieldName.equals(fieldName)) {
                return field;
            }
        }
        return null;
    }
}
