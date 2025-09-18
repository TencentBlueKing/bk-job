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

package com.tencent.bk.job.common.iam.mock;

import com.tencent.bk.job.common.iam.util.BusinessAuthHelper;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.sdk.iam.config.IamConfiguration;
import com.tencent.bk.sdk.iam.dto.InstanceDTO;
import com.tencent.bk.sdk.iam.dto.expression.ExpressionDTO;
import com.tencent.bk.sdk.iam.service.PolicyService;
import com.tencent.bk.sdk.iam.service.TokenService;
import com.tencent.bk.sdk.iam.service.TopoPathService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mock的鉴权工具类，所有鉴权请求全部通过
 */
@Slf4j
public class MockBusinessAuthHelper extends BusinessAuthHelper {

    public MockBusinessAuthHelper(TokenService tokenService,
                                  PolicyService policyService,
                                  TopoPathService topoPathService,
                                  IamConfiguration iamConfiguration) {
        super(tokenService, policyService, topoPathService, iamConfiguration);
    }

    @Override
    public boolean isAllowed(String tenantId, String username, String action) {
        return true;
    }

    @Override
    public boolean isAllowed(String tenantId, String username, String action, InstanceDTO instance) {
        return true;
    }

    @Override
    public List<String> isAllowed(String tenantId, String username, String action, List<InstanceDTO> instanceList) {
        if (CollectionUtils.isEmpty(instanceList)) {
            return Collections.emptyList();
        }
        return instanceList.stream().map(InstanceDTO::getId).collect(Collectors.toList());
    }

    @Override
    public boolean isAllowed(String tenantId, String username, String action, InstanceDTO selfInstance,
                             List<InstanceDTO> dependentInstanceList) {
        return true;
    }

    @Override
    public boolean validRequest(HttpServletRequest request) {
        return true;
    }

    @Override
    public List<AppResourceScope> getAuthedAppResourceScopeList(ExpressionDTO expression,
                                                                List<AppResourceScope> allAppResourceScopeList) {
        return allAppResourceScopeList;
    }
}
