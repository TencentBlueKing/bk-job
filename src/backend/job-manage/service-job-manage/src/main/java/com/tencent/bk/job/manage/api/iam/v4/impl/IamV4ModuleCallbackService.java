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
 * 模块（CMDB 拓扑节点）的 IAM v4 回调实现。
 *
 * <p><b>两个方法都待 IAM 回调协议补齐祖先信息后实现</b>，当前均返回空。根因是现行协议的
 * {@code filter.parent} 只给<b>直接上级</b>的 {@code {type, id}}、不含祖先链：模块的上级是集群，回调只拿到
 * setId，而 CMDB 的拓扑接口把 {@code bk_biz_id} 写在 URL 路径里，没有业务就查不了。详见
 * openspec design.md D13。</p>
 */
@Slf4j
@Component
public class IamV4ModuleCallbackService extends BaseIamV4CallbackService {

    @Autowired
    public IamV4ModuleCallbackService(AppScopeMappingService appScopeMappingService,
                                           TenantEnvService tenantEnvService,
                                           IamV4AuthService iamV4AuthService) {
        super(appScopeMappingService, tenantEnvService, iamV4AuthService);
    }

    @Override
    public String getResourceType() {
        return IamV4ResourceTypeId.MODULE;
    }

    @Override
    protected V4ListInstanceDataDTO listInstance(V4CallbackRequestDTO request) {
        // TODO 依赖 IAM v4 回调协议新增的祖先信息字段（字段名待 IAM 侧确定，见 openspec design.md D13）：
        //  届时实现为 从回调请求取 bizId，配合 parent 的 setId 从拓扑树缓存取该集群下的全部模块。
        log.warn("list_instance for module is not supported until IAM callback carries "
            + "ancestor info|parent={}", request.getFilter() == null ? null : request.getFilter().getParent());
        V4ListInstanceDataDTO data = new V4ListInstanceDataDTO();
        data.setCount(0L);
        data.setResults(new ArrayList<>());
        return data;
    }

    @Override
    protected List<V4InstanceInfoDTO> fetchInstanceInfo(V4CallbackRequestDTO request) {
        // TODO 依赖 IAM v4 回调协议新增的祖先信息字段（字段名待 IAM 侧确定，见 openspec design.md D13）：
        //  届时实现为 从回调请求取 bizId -> 拓扑树缓存取模块名称与所属 setId，路径 /biz,{bizId}/set,{setId}/。
        int idCount = request.getFilter() == null || request.getFilter().getIds() == null
            ? 0 : request.getFilter().getIds().size();
        log.warn("fetch_instance_info for module is not supported until IAM callback carries "
            + "ancestor info|idCount={}", idCount);
        return Collections.emptyList();
    }
}
