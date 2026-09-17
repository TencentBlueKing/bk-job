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

package com.tencent.bk.job.manage.api.iam.v4.impl;

import com.tencent.bk.job.common.iam.constant.IamV4ResourceTypeId;
import com.tencent.bk.job.common.iam.service.BaseIamV4CallbackService;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.sdk.iamv4.callback.dto.request.V4CallbackRequestDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.response.V4InstanceInfoDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.response.V4ListInstanceDataDTO;
import com.tencent.bk.sdk.iamv4.service.IamV4AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 命名空间的 IAM v4 回调实现。
 *
 * <p>实例 ID 用 CMDB 原生命名空间 ID。<b>两个方法都待 IAM 回调协议补齐祖先信息后实现</b>，当前均返回空。
 * 根因是 CMDB 的容器接口强制要求 bizId，而命名空间的直接上级是集群：{@code list_instance} 的 parent 只给
 * clusterId、{@code fetch_instance_info} 更是只给 ids，都定位不到业务。详见 openspec design.md D13。</p>
 */
@Slf4j
@Component
public class IamV4NamespaceCallbackService extends BaseIamV4CallbackService {

    @Autowired
    public IamV4NamespaceCallbackService(AppScopeMappingService appScopeMappingService,
                                              TenantEnvService tenantEnvService,
                                              IamV4AuthService iamV4AuthService) {
        super(appScopeMappingService, tenantEnvService, iamV4AuthService);
    }

    @Override
    public String getResourceType() {
        return IamV4ResourceTypeId.NAMESPACE;
    }

    @Override
    protected V4ListInstanceDataDTO listInstance(V4CallbackRequestDTO request) {
        // TODO 依赖 IAM v4 回调协议新增的祖先信息字段（字段名待 IAM 侧确定，见 openspec design.md D13）：
        //  届时实现为 从回调请求取 bizId，配合 parent 的 clusterId 查 CMDB 取该集群下的全部命名空间。
        //  当前 filter.parent 只给直接上级集群，拿不到业务，故返回空列表。
        log.warn("list_instance for namespace is not supported until IAM callback carries "
            + "ancestor info|parent={}", request.getFilter() == null ? null : request.getFilter().getParent());
        V4ListInstanceDataDTO data = new V4ListInstanceDataDTO();
        data.setCount(0L);
        data.setResults(new ArrayList<>());
        return data;
    }

    @Override
    protected List<V4InstanceInfoDTO> fetchInstanceInfo(V4CallbackRequestDTO request) {
        // TODO 依赖 IAM v4 回调协议新增的祖先信息字段（字段名待 IAM 侧确定，见 openspec design.md D13）：
        //  届时实现为 从回调请求取 bizId 与 clusterId -> 批量查 CMDB 取命名空间名称，
        //  路径 /biz_container,{bizId}/cluster,{clusterId}/。
        //  当前 fetch_instance_info 的 filter 只有 ids，定位不到业务与集群，故返回空列表，
        //  权限中心侧会按「已删除」渲染。
        int idCount = request.getFilter() == null || request.getFilter().getIds() == null
            ? 0 : request.getFilter().getIds().size();
        log.warn("fetch_instance_info for namespace is not supported until IAM callback carries "
            + "ancestor info|idCount={}", idCount);
        return Collections.emptyList();
    }
}
