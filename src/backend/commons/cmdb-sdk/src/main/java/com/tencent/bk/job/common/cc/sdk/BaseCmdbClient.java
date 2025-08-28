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

package com.tencent.bk.job.common.cc.sdk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.cc.config.CmdbConfig;
import com.tencent.bk.job.common.cc.constants.CmdbMetricNames;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.HttpMethodEnum;
import com.tencent.bk.job.common.esb.config.AppProperties;
import com.tencent.bk.job.common.esb.config.BkApiGatewayProperties;
import com.tencent.bk.job.common.esb.model.EsbReq;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.OpenApiRequestInfo;
import com.tencent.bk.job.common.esb.sdk.BkApiV1Client;
import com.tencent.bk.job.common.exception.InternalCmdbException;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.paas.user.IVirtualAdminAccountProvider;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.common.util.ApiUtil;
import com.tencent.bk.job.common.util.FlowController;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.http.HttpHelper;
import com.tencent.bk.job.common.util.http.HttpHelperFactory;
import com.tencent.bk.job.common.util.http.HttpMetricUtil;
import com.tencent.bk.job.common.util.http.JobHttpRequestRetryHandler;
import com.tencent.bk.job.common.util.json.JsonUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * CMDB API 基础实现
 */
@Slf4j
public class BaseCmdbClient extends BkApiV1Client {
    protected static final String SEARCH_BIZ_INST_TOPO = "/api/v3/find/topoinst/biz/{bk_biz_id}";
    protected static final String GET_BIZ_INTERNAL_MODULE = "/api/v3/topo/internal/{bk_supplier_account}/{bk_biz_id}";
    protected static final String LIST_BIZ_HOSTS = "/api/v3/hosts/app/{bk_biz_id}/list_hosts";
    protected static final String LIST_HOSTS_WITHOUT_BIZ = "/api/v3/hosts/list_hosts_without_app";
    protected static final String FIND_HOST_BIZ_RELATIONS = "/api/v3/hosts/modules/read";
    protected static final String FIND_MODULE_HOST_RELATION = "/api/v3/findmany/module_relation/bk_biz_id/{bk_biz_id}";
    protected static final String RESOURCE_WATCH = "/api/v3/event/watch/resource/{bk_resource}";
    protected static final String SEARCH_BUSINESS = "/api/v3/biz/search/{bk_supplier_account}";
    protected static final String GET_CLOUD_AREAS = "/api/v3/findmany/cloudarea";
    protected static final String GET_OBJ_ATTRIBUTES = "/api/v3/find/objectattr";
    protected static final String GET_TOPO_NODE_PATHS = "/api/v3/cache/find/cache/topo/node_path/biz/{bk_biz_id}";
    protected static final String SEARCH_DYNAMIC_GROUP = "/api/v3/dynamicgroup/search/{bk_biz_id}";
    protected static final String EXECUTE_DYNAMIC_GROUP = "/api/v3/dynamicgroup/data/{bk_biz_id}/{id}";
    protected static final String GET_BIZ_BRIEF_CACHE_TOPO = "/api/v3/cache/find/cache/topo/brief/biz/{bk_biz_id}";

    // 容器相关 API
    protected static final String LIST_KUBE_CONTAINER_BY_TOPO = "/api/v3/findmany/kube/container/by_topo";
    protected static final String GET_BIZ_KUBE_CACHE_TOPO = "/api/v3/cache/find/biz/kube/topo";
    protected static final String LIST_KUBE_CLUSTER = "/api/v3/findmany/kube/cluster";
    protected static final String LIST_KUBE_NAMESPACE = "/api/v3/findmany/kube/namespace";
    protected static final String LIST_KUBE_WORKLOAD = "/api/v3/findmany/kube/workload/{kind}";

    // 业务集相关 API
    protected static final String SEARCH_BUSINESS_SET = "/api/v3/findmany/biz_set";
    protected static final String SEARCH_BIZ_IN_BUSINESS_SET = "/api/v3/find/biz_set/biz_list";

    // 租户集相关 API
    protected static final String LIST_TENANT_SET = "/api/v3/findmany/tenant_set";

    private static final Map<String, String> interfaceNameMap = new HashMap<>();

    protected final String cmdbSupplierAccount;
    protected final AppProperties appProperties;
    protected final IVirtualAdminAccountProvider virtualAdminAccountProvider;

    /**
     * 对整个应用中所有的CMDB调用进行限流
     */
    protected final FlowController globalFlowController;
    protected final CmdbConfig cmdbConfig;
    protected final HttpHelper cmdbHttpHelper;

    static {
        interfaceNameMap.put(SEARCH_BIZ_INST_TOPO, "search_biz_inst_topo");
        interfaceNameMap.put(GET_BIZ_INTERNAL_MODULE, "get_biz_internal_module");
        interfaceNameMap.put(LIST_BIZ_HOSTS, "list_biz_hosts");
        interfaceNameMap.put(LIST_HOSTS_WITHOUT_BIZ, "list_hosts_without_biz");
        interfaceNameMap.put(FIND_HOST_BIZ_RELATIONS, "find_host_biz_relations");
        interfaceNameMap.put(FIND_MODULE_HOST_RELATION, "find_module_host_relation");
        interfaceNameMap.put(RESOURCE_WATCH, "resource_watch");
        interfaceNameMap.put(SEARCH_BUSINESS, "search_business");
        interfaceNameMap.put(GET_CLOUD_AREAS, "search_cloud_area");
        interfaceNameMap.put(GET_OBJ_ATTRIBUTES, "search_object_attribute");
        interfaceNameMap.put(GET_TOPO_NODE_PATHS, "find_topo_node_paths");
        interfaceNameMap.put(SEARCH_DYNAMIC_GROUP, "search_dynamic_group");
        interfaceNameMap.put(EXECUTE_DYNAMIC_GROUP, "execute_dynamic_group");
        interfaceNameMap.put(GET_BIZ_BRIEF_CACHE_TOPO, "get_biz_brief_cache_topo");
        interfaceNameMap.put(SEARCH_BUSINESS_SET, "search_business_set");
        interfaceNameMap.put(SEARCH_BIZ_IN_BUSINESS_SET, "search_biz_in_business_set");
        interfaceNameMap.put(LIST_KUBE_CLUSTER, "list_kube_cluster");
        interfaceNameMap.put(LIST_KUBE_NAMESPACE, "list_kube_namespace");
        interfaceNameMap.put(LIST_KUBE_WORKLOAD, "list_kube_workload");
        interfaceNameMap.put(LIST_KUBE_CONTAINER_BY_TOPO, "list_kube_container_by_topo");
        interfaceNameMap.put(GET_BIZ_KUBE_CACHE_TOPO, "get_biz_kube_cache_topo");
    }

    protected BaseCmdbClient(FlowController flowController,
                             AppProperties appProperties,
                             BkApiGatewayProperties bkApiGatewayProperties,
                             CmdbConfig cmdbConfig,
                             MeterRegistry meterRegistry,
                             TenantEnvService tenantEnvService,
                             IVirtualAdminAccountProvider virtualAdminAccountProvider,
                             String lang) {
        super(
            meterRegistry,
            CmdbMetricNames.CMDB_API_PREFIX,
            bkApiGatewayProperties.getCmdb().getUrl(),
            HttpHelperFactory.getLongRetryableHttpHelper(),
            lang,
            tenantEnvService
        );
        this.setLogger(LoggerFactory.getLogger(this.getClass()));
        this.globalFlowController = flowController;
        this.cmdbConfig = cmdbConfig;
        this.cmdbSupplierAccount = cmdbConfig.getDefaultSupplierAccount();
        this.appProperties = appProperties;
        this.cmdbHttpHelper = HttpHelperFactory.createHttpHelper(
            15000,
            15000,
            35000,
            500,
            1000,
            60,
            true,
            new JobHttpRequestRetryHandler(),
            httpClientBuilder -> httpClientBuilder.addInterceptorLast(getLogBkApiRequestIdInterceptor())
        );
        this.virtualAdminAccountProvider = virtualAdminAccountProvider;
    }


    protected <T extends EsbReq> T makeCmdbBaseReq(Class<T> reqClass) {
        return EsbReq.buildRequest(reqClass, cmdbSupplierAccount);
    }

    protected <R> EsbResp<R> requestCmdbApiUseContextTenantId(HttpMethodEnum method,
                                                              String uri,
                                                              String queryParams,
                                                              EsbReq reqBody,
                                                              TypeReference<EsbResp<R>> typeReference) {
        return requestCmdbApiUseContextTenantId(method, uri, queryParams, reqBody, typeReference, cmdbHttpHelper);
    }

    protected <R> EsbResp<R> requestCmdbApiUseContextTenantId(HttpMethodEnum method,
                                                              String uri,
                                                              String queryParams,
                                                              EsbReq reqBody,
                                                              TypeReference<EsbResp<R>> typeReference,
                                                              HttpHelper httpHelper) {
        String tenantId = JobContextUtil.getTenantId();
        if (StringUtils.isNotBlank(tenantId)) {
            return requestCmdbApi(tenantId, method, uri, queryParams, reqBody, typeReference, httpHelper);
        } else {
            throw new InternalException("tenantId is blank", ErrorCode.INTERNAL_ERROR);
        }
    }

    protected <R> EsbResp<R> requestCmdbApi(String tenantId,
                                            HttpMethodEnum method,
                                            String uri,
                                            String queryParams,
                                            EsbReq reqBody,
                                            TypeReference<EsbResp<R>> typeReference) {
        return requestCmdbApi(tenantId, method, uri, queryParams, reqBody, typeReference, cmdbHttpHelper);
    }

    protected <R> EsbResp<R> requestCmdbApi(String tenantId,
                                            HttpMethodEnum method,
                                            String uri,
                                            String queryParams,
                                            EsbReq reqBody,
                                            TypeReference<EsbResp<R>> typeReference,
                                            HttpHelper httpHelper) {
        String apiName = ApiUtil.getApiNameByUri(interfaceNameMap, uri);
        if (cmdbConfig != null && cmdbConfig.getEnableFlowControl()) {
            if (globalFlowController != null && globalFlowController.isReady()) {
                log.debug("Flow control apiName={}", apiName);
                long startTime = System.currentTimeMillis();
                globalFlowController.acquire(apiName);
                long duration = System.currentTimeMillis() - startTime;
                if (duration >= 5000) {
                    log.warn("Request resource {} wait flowControl for {}ms", apiName, duration);
                } else if (duration >= 1000) {
                    log.info("Request resource {} wait flowControl for {}ms", apiName, duration);
                }
            } else {
                log.debug("globalFlowController not set or not ready, ignore this time");
            }
        }
        try {
            HttpMetricUtil.setHttpMetricName(CommonMetricNames.ESB_CMDB_API_HTTP);
            HttpMetricUtil.addTagForCurrentMetric(Tag.of("api_name", apiName));
            OpenApiRequestInfo<Object> requestInfo = OpenApiRequestInfo
                .builder()
                .method(method)
                .uri(uri)
                .addHeader(buildTenantHeader(tenantId))
                .queryParams(queryParams)
                .body(reqBody)
                .authorization(buildAuthorization(
                    appProperties, virtualAdminAccountProvider.getVirtualAdminUsername(tenantId)))
                .build();
            return doRequest(requestInfo, typeReference, httpHelper);
        } catch (Throwable e) {
            String errorMsg = "Fail to request CMDB data|method=" + method + "|uri=" + uri + "|queryParams="
                + queryParams + "|body="
                + JsonUtils.toJsonWithoutSkippedFields(JsonUtils.toJsonWithoutSkippedFields(reqBody));
            log.error(errorMsg, e);
            throw new InternalCmdbException(e.getMessage(), e, ErrorCode.CMDB_API_DATA_ERROR);
        } finally {
            HttpMetricUtil.clearHttpMetric();
        }
    }


}
