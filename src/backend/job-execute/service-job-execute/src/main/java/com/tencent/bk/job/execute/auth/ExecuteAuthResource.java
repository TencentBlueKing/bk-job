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

package com.tencent.bk.job.execute.auth;

import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import lombok.Getter;
import lombok.ToString;

/**
 * 执行类操作所关联的业务内资源（脚本 / 公共脚本 / 作业模板 / 执行方案）。
 *
 * <p>{@code companionViewActionId} 是 v4 才需要的：v4 里每个操作只关联单一资源类型，执行类操作统一绑
 * {@code host}，资源本身的可见性改由伴随的 view 操作单独校验（如执行脚本要另外校验 {@code view_script}）。
 * v3 靠模型里的依赖操作保证，不需要显式校验，故 v3 执行器会忽略该字段。</p>
 */
@Getter
@ToString
public class ExecuteAuthResource {

    private final ResourceTypeEnum resourceType;
    private final String resourceId;
    /**
     * 上级资源类型，仅执行方案有（作业模板），其余为 null
     */
    private final ResourceTypeEnum parentResourceType;
    private final String parentResourceId;
    /**
     * v4 下需伴随校验的 view 操作 ID，无伴随校验时为 null（如公共脚本执行）
     */
    private final String companionViewActionId;

    private ExecuteAuthResource(ResourceTypeEnum resourceType,
                                String resourceId,
                                ResourceTypeEnum parentResourceType,
                                String parentResourceId,
                                String companionViewActionId) {
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.parentResourceType = parentResourceType;
        this.parentResourceId = parentResourceId;
        this.companionViewActionId = companionViewActionId;
    }

    public static ExecuteAuthResource of(ResourceTypeEnum resourceType,
                                         String resourceId,
                                         String companionViewActionId) {
        return new ExecuteAuthResource(resourceType, resourceId, null, null, companionViewActionId);
    }

    public static ExecuteAuthResource ofChild(ResourceTypeEnum resourceType,
                                              String resourceId,
                                              ResourceTypeEnum parentResourceType,
                                              String parentResourceId,
                                              String companionViewActionId) {
        return new ExecuteAuthResource(
            resourceType, resourceId, parentResourceType, parentResourceId, companionViewActionId);
    }
}
