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

package com.tencent.bk.job.common.iam.util;

import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.sdk.iam.config.IamConfiguration;
import com.tencent.bk.sdk.iam.dto.InstanceDTO;
import com.tencent.bk.sdk.iam.dto.expression.ExpressionDTO;
import com.tencent.bk.sdk.iam.helper.AuthHelper;
import com.tencent.bk.sdk.iam.service.PolicyService;
import com.tencent.bk.sdk.iam.service.TokenService;
import com.tencent.bk.sdk.iam.util.PathBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class BusinessAuthHelper extends AuthHelper {

    public BusinessAuthHelper(TokenService tokenService, PolicyService policyService,
                              IamConfiguration iamConfiguration) {
        super(tokenService, policyService, iamConfiguration);
    }

    private InstanceDTO buildAppResourceScopeInstance(AppResourceScope appResourceScope) {
        InstanceDTO instance = new InstanceDTO();
        ResourceTypeEnum resourceType = IamUtil.getIamResourceTypeForResourceScope(appResourceScope);
        if (resourceType == ResourceTypeEnum.BUSINESS) {
            instance.setId(appResourceScope.getId());
        } else if (resourceType == ResourceTypeEnum.BUSINESS_SET) {
            instance.setPath(PathBuilder.newBuilder(resourceType.getId(), appResourceScope.getId()).build());
        }
        instance.setSystem(resourceType.getSystemId());
        instance.setType(ResourceTypeEnum.BUSINESS.getId());
        return instance;
    }

    public List<AppResourceScope> getAuthedAppResourceScopeList(ExpressionDTO expression,
                                                                List<AppResourceScope> allAppResourceScopeList) {
        log.debug("expression={}", JsonUtils.toJson(expression));
        List<AppResourceScope> resultList = new ArrayList<>();
        allAppResourceScopeList.forEach(appResourceScope -> {
            InstanceDTO instance = buildAppResourceScopeInstance(appResourceScope);
            Map<String, InstanceDTO> instanceMap = new HashMap<>(1);
            instanceMap.put(instance.getType(), instance);
            if (calculateExpression(instanceMap, expression)) {
                resultList.add(appResourceScope);
            }
        });
        log.debug("resultList={}", JsonUtils.toJson(resultList));
        return resultList;
    }
}
