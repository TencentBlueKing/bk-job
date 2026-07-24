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

package com.tencent.bk.job.common.paas.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.HttpMethodEnum;
import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.job.common.constant.TenantIdConstants;
import com.tencent.bk.job.common.esb.config.AppProperties;
import com.tencent.bk.job.common.esb.config.BkApiGatewayProperties;
import com.tencent.bk.job.common.esb.model.BkApiAuthorization;
import com.tencent.bk.job.common.esb.model.OpenApiRequestInfo;
import com.tencent.bk.job.common.esb.model.OpenApiResponse;
import com.tencent.bk.job.common.esb.sdk.BkApiV2Client;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.paas.model.OpenApiTenant;
import com.tencent.bk.job.common.paas.model.SimpleUserInfo;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.common.util.http.HttpHelperFactory;
import com.tencent.bk.job.common.util.http.HttpMetricUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.message.BasicHeader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tencent.bk.job.common.metrics.CommonMetricNames.USER_MANAGE_API;
import static com.tencent.bk.job.common.metrics.CommonMetricNames.USER_MANAGE_API_HTTP;

/**
 * 用户管理 API 客户端
 */
@Slf4j
public class UserMgrApiClient extends BkApiV2Client implements IUserApiClient {

    private static final String API_BATCH_LOOKUP_VIRTUAL_USER = "/api/v3/open/tenant/virtual-users/-/lookup/";
    private static final String API_BATCH_QUERY_USER_DISPLAY_INFO = "/api/v3/open/tenant/users/-/display_info/";
    private static final String LOOKUP_FIELD_BK_USERNAME = "bk_username";
    private static final String LOOKUP_FIELD_LOGIN_NAME = "login_name";
    private static final Integer MAX_QUERY_BATCH_SIZE = 100;

    private final BkApiAuthorization authorization;

    public UserMgrApiClient(BkApiGatewayProperties bkApiGatewayProperties,
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

    /**
     * 获取全量租户
     */
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

    /**
     * 获取指定租户下的虚拟账号的信息
     */
    @Override
    public List<SimpleUserInfo> batchGetVirtualUserByLoginName(String tenantId, String loginName) {
        return batchLookupVirtualUsers(tenantId, loginName, LOOKUP_FIELD_LOGIN_NAME);
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

    @Override
    public SimpleUserInfo getUserByUsername(String tenantId, String username) {
        List<SimpleUserInfo> users = listUsersByUsernames(tenantId, Collections.singletonList(username));
        if (CollectionUtils.isEmpty(users)) {
            return null;
        }
        return users.get(0);
    }

    /**
     * 通过指定的 username 查出对应的自然人用户信息
     */
    @Override
    public List<SimpleUserInfo> listUsersByUsernames(String tenantId, Collection<String> usernames) {
        return listUsersByUsernamesInternal(tenantId, usernames, false);
    }

    /**
     * 批量查询用户信息，自然人查不到时补充查询虚拟用户（如通知黑名单保存）
     */
    @Override
    public List<SimpleUserInfo> listUsersByUsernamesIncludingVirtual(String tenantId, Collection<String> usernames) {
        return listUsersByUsernamesInternal(tenantId, usernames, true);
    }

    private List<SimpleUserInfo> listUsersByUsernamesInternal(String tenantId,
                                                              Collection<String> usernames,
                                                              boolean includeVirtual) {
        if (CollectionUtils.isEmpty(usernames)) {
            return Collections.emptyList();
        }
        List<SimpleUserInfo> userResult = new ArrayList<>();
        List<String> usernameList = new ArrayList<>(usernames);
        List<List<String>> userPages = Lists.partition(usernameList, MAX_QUERY_BATCH_SIZE);
        for (List<String> userPage : userPages) {
            List<SimpleUserInfo> naturalUsers = listNaturalUsersByBkUsernames(tenantId, userPage);
            List<SimpleUserInfo> userInfos = naturalUsers == null ? new ArrayList<>() : new ArrayList<>(naturalUsers);
            if (includeVirtual) {
                Set<String> resolvedUsernames = collectResolveUsernames(userInfos);
                List<String> notFoundUsernames = userPage.stream()
                    .filter(username -> !resolvedUsernames.contains(username))
                    .collect(Collectors.toList());
                appendVirtualUsers(tenantId, notFoundUsernames, LOOKUP_FIELD_BK_USERNAME, userInfos, resolvedUsernames);
            }

            if (CollectionUtils.isNotEmpty(userInfos)) {
                userResult.addAll(userInfos);
            }
        }
        return userResult;
    }

    private Set<String> collectResolveUsernames(List<SimpleUserInfo> userInfos) {
        Set<String> resolvedUsernames = new HashSet<>();
        for (SimpleUserInfo userInfo : userInfos) {
            if (userInfo.isNotEmpty()) {
                resolvedUsernames.add(userInfo.getBkUsername());
            }
        }
        return resolvedUsernames;
    }

    private void appendVirtualUsers(String tenantId,
                                    List<String> lookups,
                                    String lookupField,
                                    List<SimpleUserInfo> userInfos,
                                    Set<String> resolvedUsernames) {
        if (CollectionUtils.isEmpty(lookups)) {
            return;
        }
        List<SimpleUserInfo> virtualUsers = batchLookupVirtualUsers(
            tenantId,
            String.join(",", lookups),
            lookupField
        );
        if (CollectionUtils.isEmpty(virtualUsers)) {
            return;
        }
        for (SimpleUserInfo virtualUser : virtualUsers) {
            if (virtualUser.isEmpty()) {
                continue;
            }
            userInfos.add(virtualUser);
            resolvedUsernames.add(virtualUser.getBkUsername());
        }
    }

    private List<SimpleUserInfo> batchLookupVirtualUsers(String tenantId, String lookups, String lookupField) {
        OpenApiResponse<List<SimpleUserInfo>> resp = requestBkUserApi(
            "batch_lookup_virtual_user",
            OpenApiRequestInfo
                .builder()
                .method(HttpMethodEnum.GET)
                .uri(API_BATCH_LOOKUP_VIRTUAL_USER)
                .addHeader(buildTenantHeader(tenantId))
                .addQueryParam("lookups", lookups)
                .addQueryParam("lookup_field", lookupField)
                .authorization(authorization)
                .build(),
            req -> doRequest(req, new TypeReference<OpenApiResponse<List<SimpleUserInfo>>>() {
            })
        );
        return resp.getData();
    }

    /**
     * 按 bk_username 批量查询自然人展示信息
     */
    private List<SimpleUserInfo> listNaturalUsersByBkUsernames(String tenantId, List<String> bkUsernames) {
        OpenApiResponse<List<SimpleUserInfo>> resp = requestBkUserApi(
            "batch_query_user",
            OpenApiRequestInfo
                .builder()
                .method(HttpMethodEnum.GET)
                .uri(API_BATCH_QUERY_USER_DISPLAY_INFO)
                .addHeader(buildTenantHeader(tenantId))
                .addQueryParam("bk_usernames", String.join(",", bkUsernames))
                .authorization(authorization)
                .build(),
            req -> doRequest(req, new TypeReference<OpenApiResponse<List<SimpleUserInfo>>>() {})
        );
        return resp.getData();
    }

}
