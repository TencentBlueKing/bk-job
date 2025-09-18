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

package com.tencent.bk.job.common.iam.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.HttpMethodEnum;
import com.tencent.bk.job.common.esb.config.AppProperties;
import com.tencent.bk.job.common.esb.config.BkApiGatewayProperties;
import com.tencent.bk.job.common.esb.model.EsbReq;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.OpenApiRequestInfo;
import com.tencent.bk.job.common.esb.sdk.BkApiV1Client;
import com.tencent.bk.job.common.exception.InternalIamException;
import com.tencent.bk.job.common.iam.dto.EsbIamBatchAuthedPolicy;
import com.tencent.bk.job.common.iam.dto.GetApplyUrlRequest;
import com.tencent.bk.job.common.iam.dto.GetApplyUrlResponse;
import com.tencent.bk.job.common.iam.dto.RegisterResourceRequest;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.paas.user.IVirtualAdminAccountProvider;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.http.HttpHelperFactory;
import com.tencent.bk.job.common.util.http.HttpMetricUtil;
import com.tencent.bk.sdk.iam.constants.SystemId;
import com.tencent.bk.sdk.iam.dto.action.ActionDTO;
import com.tencent.bk.sdk.iam.dto.resource.ResourceDTO;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.tencent.bk.job.common.metrics.CommonMetricNames.IAM_API;

/**
 * 通过 APIGW 调用 IAM API 的客户端
 */
@Slf4j
public class ApiGwIamClient extends BkApiV1Client implements IIamClient {

    private static final String API_GET_APPLY_URL = "/api/v1/open/application/";
    private static final String API_REGISTER_RESOURCE_URL = "/api/v1/open/authorization/resource_creator_action/";

    private final AppProperties appProperties;
    private final IVirtualAdminAccountProvider virtualAdminAccountProvider;

    public ApiGwIamClient(MeterRegistry meterRegistry,
                          AppProperties appProperties,
                          BkApiGatewayProperties bkApiGatewayProperties,
                          TenantEnvService tenantEnvService,
                          IVirtualAdminAccountProvider virtualAdminAccountProvider) {
        super(
            meterRegistry,
            IAM_API,
            bkApiGatewayProperties.getBkIam().getUrl(),
            HttpHelperFactory.createHttpHelper(
                httpClientBuilder -> httpClientBuilder.addInterceptorLast(getLogBkApiRequestIdInterceptor())
            ),
            tenantEnvService
        );
        this.appProperties = appProperties;
        this.virtualAdminAccountProvider = virtualAdminAccountProvider;
    }

    @Override
    public String getApplyUrl(List<ActionDTO> actionList) {
        GetApplyUrlRequest getApplyUrlRequest = EsbReq.buildRequest(GetApplyUrlRequest.class, null);
        getApplyUrlRequest.setSystem(SystemId.JOB);
        getApplyUrlRequest.setAction(actionList);
        EsbResp<GetApplyUrlResponse> esbResp = requestIamApi(
            HttpMethodEnum.POST,
            API_GET_APPLY_URL,
            getApplyUrlRequest,
            new TypeReference<EsbResp<GetApplyUrlResponse>>() {
            });
        GetApplyUrlResponse data = esbResp.getData();
        if (data != null) {
            return data.getUrl();
        } else {
            return null;
        }
    }

    @Override
    public boolean registerResource(String id, String name, String type, String creator, List<ResourceDTO> ancestor) {
        RegisterResourceRequest registerResourceRequest = EsbReq.buildRequest(RegisterResourceRequest.class, null);
        registerResourceRequest.setSystem(SystemId.JOB);
        registerResourceRequest.setId(id);
        registerResourceRequest.setName(name);
        registerResourceRequest.setType(type);
        registerResourceRequest.setCreator(creator);
        if (ancestor != null && !ancestor.isEmpty()) {
            registerResourceRequest.setAncestor(ancestor);
        }
        EsbResp<List<EsbIamBatchAuthedPolicy>> esbResp = requestIamApi(
            HttpMethodEnum.POST,
            API_REGISTER_RESOURCE_URL,
            registerResourceRequest,
            new TypeReference<EsbResp<List<EsbIamBatchAuthedPolicy>>>() {
            });
        return esbResp.getResult();
    }

    /**
     * 通过ESB请求权限中心API的统一入口，监控数据埋点位置
     *
     * @param method        Http方法
     * @param uri           请求地址
     * @param reqBody       请求体内容
     * @param typeReference 指定了返回值类型的EsbResp TypeReference对象
     * @param <R>           泛型：返回值类型
     * @return 返回值类型实例
     */
    @SuppressWarnings("SameParameterValue")
    private <R> EsbResp<R> requestIamApi(HttpMethodEnum method,
                                         String uri,
                                         EsbReq reqBody,
                                         TypeReference<EsbResp<R>> typeReference) {
        try {
            String tenantId = JobContextUtil.getTenantId();
            HttpMetricUtil.setHttpMetricName(CommonMetricNames.IAM_API_HTTP);
            HttpMetricUtil.addTagForCurrentMetric(Tag.of("api_name", uri));
            OpenApiRequestInfo<Object> requestInfo = OpenApiRequestInfo
                .builder()
                .method(method)
                .uri(uri)
                .addHeader(buildTenantHeader(tenantId))
                .body(reqBody)
                .authorization(buildAuthorization(
                    appProperties, virtualAdminAccountProvider.getVirtualAdminUsername(tenantId)))
                .build();
            return doRequest(requestInfo, typeReference);
        } catch (Exception e) {
            throw new InternalIamException(e, ErrorCode.IAM_API_DATA_ERROR, null);
        } finally {
            HttpMetricUtil.clearHttpMetric();
        }
    }
}
