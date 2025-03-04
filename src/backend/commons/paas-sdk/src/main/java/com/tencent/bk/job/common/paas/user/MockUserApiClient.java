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

package com.tencent.bk.job.common.paas.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.constant.HttpMethodEnum;
import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.job.common.constant.TenantIdConstants;
import com.tencent.bk.job.common.esb.config.AppProperties;
import com.tencent.bk.job.common.esb.config.BkApiGatewayProperties;
import com.tencent.bk.job.common.esb.model.BkApiAuthorization;
import com.tencent.bk.job.common.esb.model.OpenApiRequestInfo;
import com.tencent.bk.job.common.esb.model.OpenApiResponse;
import com.tencent.bk.job.common.esb.sdk.BkApiV2Client;
import com.tencent.bk.job.common.model.dto.BkUserDTO;
import com.tencent.bk.job.common.paas.model.OpenApiTenant;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.common.util.http.HttpHelperFactory;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.message.BasicHeader;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tencent.bk.job.common.metrics.CommonMetricNames.USER_MANAGE_API;

@Slf4j
public class MockUserApiClient extends BkApiV2Client implements IUserApiClient {

    private final BkApiAuthorization authorization;

    public MockUserApiClient(BkApiGatewayProperties bkApiGatewayProperties,
                             AppProperties appProperties,
                             MeterRegistry meterRegistry,
                             TenantEnvService tenantEnvService) {
        super(meterRegistry,
            USER_MANAGE_API,
            bkApiGatewayProperties.getBkUser().getUrl(),
            HttpHelperFactory.getRetryableHttpHelper(),
            tenantEnvService
        );
        this.authorization = BkApiAuthorization.appAuthorization(appProperties.getCode(),
            appProperties.getSecret());
    }

    @Override
    public List<BkUserDTO> getAllUserList(String tenantId) {
        List<String> displayNames = Arrays.asList("a", "b", "c");
        List<BkUserDTO> users = displayNames.stream().map(s -> {
            BkUserDTO user = new BkUserDTO();
            user.setTenantId(tenantId);
            user.setUsername("uuid_" + tenantId + "_" + s);
            user.setDisplayName("display_" + tenantId + "_" + s);
            return user;
        }).collect(Collectors.toList());
        log.info("[MockUserApiClient] tenant: {}, users: {}",
            tenantId,
            users.stream().map(BkUserDTO::getUsername).collect(Collectors.toList()));
        return users;
    }

    @Override
    public List<OpenApiTenant> listAllTenant() {
        OpenApiResponse<List<OpenApiTenant>> response = requestBkUserApi(
            "list_tenant",
            OpenApiRequestInfo
                .builder()
                .method(HttpMethodEnum.GET)
                .uri("/api/v3/open/tenants")
                .addHeader(new BasicHeader(JobCommonHeaders.BK_TENANT_ID, TenantIdConstants.SYSTEM_TENANT_ID))
                .authorization(authorization)
                .build(),
            request -> doRequest(request, new TypeReference<OpenApiResponse<List<OpenApiTenant>>>() {
            })
        );

        return response.getData();
    }

    @Override
    public BkUserDTO getUserByUsername(String username) {
        return null;
    }

    @Override
    public Map<String, BkUserDTO> listUsersByUsernames(Collection<String> usernames) {
        return Collections.emptyMap();
    }

    @Override
    public void logError(String message, Object... objects) {
        log.error(message, objects);
    }

}
