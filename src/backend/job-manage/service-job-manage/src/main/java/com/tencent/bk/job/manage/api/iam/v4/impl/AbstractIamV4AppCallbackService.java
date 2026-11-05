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

import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.iam.service.BaseIamV4CallbackService;
import com.tencent.bk.job.common.iam.util.IamV4RespUtil;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.sdk.iamv4.callback.dto.request.V4CallbackFilterDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.request.V4CallbackRequestDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.response.V4BaseInstanceDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.response.V4InstanceInfoDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.response.V4ListInstanceDataDTO;
import com.tencent.bk.sdk.iamv4.service.IamV4AuthService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 业务级资源（业务 / 业务集 / 租户集及其实例视图首段变体）的 v4 回调公共实现。
 *
 * <p>这几个资源类型的实例都来自 Job 本地已同步的业务表，查询逻辑只差一个资源范围类型，故在此统一实现，
 * 子类只需声明自己对应的 {@link ResourceScopeTypeEnum}。</p>
 *
 * <p>它们都是权限模型里的顶层资源（{@code ancestors} 为空），因此 {@code list_instance} 不带 parent，
 * {@code fetch_instance_info} 也不返回 {@code _bk_iam_path_}。权限中心侧的实例 ID 用的是 CMDB 的
 * scope ID（如 bizId），不是 Job 内部 appId。</p>
 */
@Slf4j
public abstract class AbstractIamV4AppCallbackService extends BaseIamV4CallbackService {

    protected final ApplicationService applicationService;

    protected AbstractIamV4AppCallbackService(ApplicationService applicationService,
                                               AppScopeMappingService appScopeMappingService,
                                               TenantEnvService tenantEnvService,
                                               IamV4AuthService iamV4AuthService) {
        super(appScopeMappingService, tenantEnvService, iamV4AuthService);
        this.applicationService = applicationService;
    }

    /**
     * 本资源类型对应的资源范围类型。
     */
    protected abstract ResourceScopeTypeEnum getScopeType();

    @Override
    protected V4ListInstanceDataDTO listInstance(V4CallbackRequestDTO request) {
        V4CallbackFilterDTO filter = request.getFilter();
        String keyword = filter == null ? null : filter.getKeyword();
        BaseSearchCondition condition = buildSearchCondition(request.getPage());

        PageData<ApplicationDTO> pageData = applicationService.listPageAppsByScopeType(
            JobContextUtil.getTenantId(),
            getScopeType(),
            keyword,
            condition.getStart(),
            condition.getLength()
        );
        return IamV4RespUtil.buildListInstanceData(pageData,
            app -> new V4BaseInstanceDTO(app.getScope().getId(), app.getName()));
    }

    @Override
    protected List<V4InstanceInfoDTO> fetchInstanceInfo(V4CallbackRequestDTO request) {
        V4CallbackFilterDTO filter = request.getFilter();
        if (filter == null || CollectionUtils.isEmpty(filter.getIds())) {
            return Collections.emptyList();
        }
        List<ApplicationDTO> apps = applicationService.listAppsByScopeIds(
            JobContextUtil.getTenantId(), getScopeType(), filter.getIds());
        List<V4InstanceInfoDTO> result = new ArrayList<>(apps.size());
        for (ApplicationDTO app : apps) {
            V4InstanceInfoDTO instance = new V4InstanceInfoDTO();
            instance.setId(app.getScope().getId());
            instance.setDisplayName(app.getName());
            result.add(instance);
        }
        if (result.size() < filter.getIds().size()) {
            log.warn("Some {} instances not found in IAM v4 callback|requested={}|found={}",
                getScopeType().getValue(), filter.getIds().size(), result.size());
        }
        return result;
    }
}
