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
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.HttpMethodEnum;
import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.job.common.esb.config.AppProperties;
import com.tencent.bk.job.common.esb.config.BkApiGatewayProperties;
import com.tencent.bk.job.common.esb.metrics.EsbMetricTags;
import com.tencent.bk.job.common.esb.model.BkApiAuthorization;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.OpenApiRequestInfo;
import com.tencent.bk.job.common.esb.model.OpenApiResponse;
import com.tencent.bk.job.common.esb.sdk.BkApiV2Client;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InternalUserManageException;
import com.tencent.bk.job.common.model.dto.BkUserDTO;
import com.tencent.bk.job.common.paas.model.EsbListUsersResult;
import com.tencent.bk.job.common.paas.model.GetUserListReq;
import com.tencent.bk.job.common.paas.model.OpenApiTenant;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.common.util.http.HttpHelperFactory;
import com.tencent.bk.job.common.util.http.HttpMetricUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.message.BasicHeader;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static com.tencent.bk.job.common.metrics.CommonMetricNames.USER_MANAGE_API;
import static com.tencent.bk.job.common.metrics.CommonMetricNames.USER_MANAGE_API_HTTP;

/**
 * 用户管理 API 客户端
 */
@Slf4j
public class UserMgrApiClient extends BkApiV2Client {

    private static final String API_GET_USER_LIST = "/api/c/compapi/v2/usermanage/list_users/";

    private final BkApiAuthorization authorization;

    private final TenantEnvService tenantEnvService;

    public UserMgrApiClient(BkApiGatewayProperties bkApiGatewayProperties,
                            AppProperties appProperties,
                            MeterRegistry meterRegistry,
                            TenantEnvService tenantEnvService) {
        super(meterRegistry,
            USER_MANAGE_API,
            bkApiGatewayProperties.getBkUser().getUrl(),
            HttpHelperFactory.getRetryableHttpHelper()
        );
        this.authorization = BkApiAuthorization.appAuthorization(appProperties.getCode(),
            appProperties.getSecret());
        this.tenantEnvService = tenantEnvService;
    }

    public List<BkUserDTO> getAllUserList(String tenantId) {
        String fields = "id,username,display_name,logo";
        List<EsbListUsersResult> esbUserList;
        try {
            GetUserListReq req = buildGetUserListReq(fields);

            HttpMetricUtil.setHttpMetricName(USER_MANAGE_API_HTTP);
            HttpMetricUtil.addTagForCurrentMetric(
                Tag.of(EsbMetricTags.KEY_API_NAME, API_GET_USER_LIST)
            );
            EsbResp<List<EsbListUsersResult>> esbResp = doRequest(
                OpenApiRequestInfo.builder()
                    .method(HttpMethodEnum.GET)
                    .uri(API_GET_USER_LIST)
                    .queryParams(req.toUrlParams())
//                    .addHeader(new BasicHeader(JobCommonHeaders.BK_TENANT_ID, tenantId))
                    .authorization(authorization)
                    .build(),
                new TypeReference<EsbResp<List<EsbListUsersResult>>>() {
                }
            );
            esbUserList = esbResp.getData();
        } catch (Exception e) {
            String errorMsg = "Get " + API_GET_USER_LIST + " error";
            log.error(errorMsg, e);
            throw new InternalUserManageException(errorMsg, e, ErrorCode.BK_USER_MANAGE_API_ERROR);
        } finally {
            HttpMetricUtil.clearHttpMetric();
        }
        return convert(esbUserList);
    }

    private GetUserListReq buildGetUserListReq(String fields) {
        GetUserListReq req = new GetUserListReq();
        if (StringUtils.isNotBlank(fields)) {
            req.setFields(fields);
        }
        req.setPage(0L);
        req.setPageSize(0L);
        req.setNoPage(true);
        return req;
    }

    private List<BkUserDTO> convert(List<EsbListUsersResult> esbUserList) {
        if (CollectionUtils.isEmpty(esbUserList)) {
            return Collections.emptyList();
        }
        List<BkUserDTO> userList = new ArrayList<>();
        for (EsbListUsersResult esbUser : esbUserList) {
            BkUserDTO user = new BkUserDTO();
            user.setId(esbUser.getId());
            user.setUsername(esbUser.getUsername());
            user.setDisplayName(esbUser.getDisplayName());
            user.setLogo(esbUser.getLogo());
            userList.add(user);
        }
        return userList;
    }


    /**
     * 获取全量租户
     */
    public List<OpenApiTenant> listAllTenant() {
        OpenApiResponse<List<OpenApiTenant>> response = requestBkUserApi(
            "list_tenant",
            OpenApiRequestInfo
                .builder()
                .method(HttpMethodEnum.GET)
                .uri("/api/v3/open/tenants")
                .addHeader(new BasicHeader(JobCommonHeaders.BK_TENANT_ID, tenantEnvService.getDefaultTenantId()))
                .authorization(authorization)
                .build(),
            request -> doRequest(request, new TypeReference<OpenApiResponse<List<OpenApiTenant>>>() {
            })
        );

        return response.getData();
    }


    protected <T, R> OpenApiResponse<R> requestBkUserApi(
        String apiName,
        OpenApiRequestInfo<T> request,
        Function<OpenApiRequestInfo<T>, OpenApiResponse<R>> requestHandler) {

        try {
            HttpMetricUtil.setHttpMetricName(USER_MANAGE_API_HTTP);
            HttpMetricUtil.addTagForCurrentMetric(Tag.of("api_name", apiName));
            return requestHandler.apply(request);
        } catch (Throwable e) {
            String errorMsg = "Fail to request bk-user api|method=" + request.getMethod()
                + "|uri=" + request.getUri() + "|queryParams="
                + request.getQueryParams() + "|body="
                + JsonUtils.toJsonWithoutSkippedFields(JsonUtils.toJsonWithoutSkippedFields(request.getBody()));
            log.error(errorMsg, e);
            throw new InternalException(e.getMessage(), e, ErrorCode.BK_USER_MANAGE_API_ERROR);
        } finally {
            HttpMetricUtil.clearHttpMetric();
        }
    }

}
