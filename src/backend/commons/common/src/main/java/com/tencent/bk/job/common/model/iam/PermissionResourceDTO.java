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

package com.tencent.bk.job.common.model.iam;

import lombok.Data;

import java.util.List;

/**
 * 权限-资源
 */
@Data
public class PermissionResourceDTO {
    /**
     * 接入系统ID
     */
    private String systemId;
    /**
     * 资源类型
     */
    private String resourceType;
    /**
     * 子资源类型。比如cmdb主机，具有静态主机、动态topo、动态分组三种子类型。可取值：host/topo/dynamic_group
     */
    private String subResourceType;
    /**
     * 资源ID
     */
    private String resourceId;
    /**
     * iam path
     */
    private PathInfoDTO pathInfo;
    /**
     * 资源名称
     */
    private String resourceName;
    /**
     * 层级节点的资源类型
     */
    private String type;

    /**
     * 层级节点（比如CMDB的主机)的父资源
     */
    private List<PermissionResourceDTO> parentHierarchicalResources;

    public PermissionResourceDTO() {
    }
}
