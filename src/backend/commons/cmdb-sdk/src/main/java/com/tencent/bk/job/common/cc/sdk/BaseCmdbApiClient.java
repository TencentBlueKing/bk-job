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
import com.tencent.bk.job.common.esb.config.EsbProperties;
import com.tencent.bk.job.common.esb.constants.ApiGwType;
import com.tencent.bk.job.common.esb.model.BkApiAuthorization;
import com.tencent.bk.job.common.esb.model.EsbReq;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.OpenApiRequestInfo;
import com.tencent.bk.job.common.esb.sdk.BkApiClient;
import com.tencent.bk.job.common.exception.InternalCmdbException;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.util.ApiUtil;
import com.tencent.bk.job.common.util.FlowController;
import com.tencent.bk.job.common.util.http.HttpHelper;
import com.tencent.bk.job.common.util.http.HttpHelperFactory;
import com.tencent.bk.job.common.util.http.HttpMetricUtil;
import com.tencent.bk.job.common.util.http.WatchableHttpHelper;
import com.tencent.bk.job.common.util.json.JsonUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * CMDB API 基础实现
 */
@Slf4j
public class BaseCmdbApiClient {
    protected static final String SEARCH_BIZ_INST_TOPO = "/api/c/compapi/v2/cc/search_biz_inst_topo/";
    protected static final String GET_BIZ_INTERNAL_MODULE = "/api/c/compapi/v2/cc/get_biz_internal_module/";
    protected static final String LIST_BIZ_HOSTS = "/api/c/compapi/v2/cc/list_biz_hosts/";
    protected static final String LIST_HOSTS_WITHOUT_BIZ = "/api/c/compapi/v2/cc/list_hosts_without_biz/";
    protected static final String FIND_HOST_BIZ_RELATIONS = "/api/c/compapi/v2/cc/find_host_biz_relations/";
    protected static final String LIST_BIZ_HOSTS_TOPO = "/api/c/compapi/v2/cc/list_biz_hosts_topo/";
    protected static final String FIND_MODULE_HOST_RELATION = "/api/c/compapi/v2/cc/find_module_host_relation/";
    protected static final String RESOURCE_WATCH = "/api/c/compapi/v2/cc/resource_watch/";
    protected static final String SEARCH_BUSINESS = "/api/c/compapi/v2/cc/search_business/";
    protected static final String GET_CLOUD_AREAS = "/api/c/compapi/v2/cc/search_cloud_area/";
    protected static final String GET_OBJ_ATTRIBUTES = "/api/c/compapi/v2/cc/search_object_attribute/";
    protected static final String GET_TOPO_NODE_PATHS = "/api/c/compapi/v2/cc/find_topo_node_paths/";
    protected static final String SEARCH_DYNAMIC_GROUP = "/api/c/compapi/v2/cc/search_dynamic_group/";
    protected static final String EXECUTE_DYNAMIC_GROUP = "/api/c/compapi/v2/cc/execute_dynamic_group/";
    protected static final String GET_BIZ_BRIEF_CACHE_TOPO = "/api/c/compapi/v2/cc/get_biz_brief_cache_topo/";

    // 容器相关 API
    protected static final String LIST_KUBE_CONTAINER_BY_TOPO = "/api/v3/findmany/kube/container/by_topo";
    protected static final String GET_BIZ_KUBE_CACHE_TOPO = "/api/v3/cache/find/biz/kube/topo";
    protected static final String LIST_KUBE_CLUSTER = "/api/v3/findmany/kube/cluster";
    protected static final String LIST_KUBE_NAMESPACE = "/api/v3/findmany/kube/namespace";
    protected static final String LIST_KUBE_WORKLOAD = "/api/v3/findmany/kube/workload/{kind}";

    // 业务集相关 API
    protected static final String SEARCH_BUSINESS_SET = "/api/c/compapi/v2/cc/list_business_set/";
    protected static final String SEARCH_BIZ_IN_BUSINESS_SET = "/api/c/compapi/v2/cc/list_business_in_business_set/";

    private static final Map<String, String> interfaceNameMap = new HashMap<>();

    protected final String cmdbSupplierAccount;
    protected final BkApiAuthorization cmdbBkApiAuthorization;

    /**
     * 对整个应用中所有的CMDB调用进行限流
     */
    protected final FlowController globalFlowController;
    protected final CmdbConfig cmdbConfig;
    /**
     * CMDB ESB API 客户端
     */
    protected BkApiClient esbCmdbApiClient;
    /**
     * CMDB 蓝鲸网关 API 客户端
     */
    protected BkApiClient apiGwCmdbApiClient;

    static {
        interfaceNameMap.put(SEARCH_BIZ_INST_TOPO, "search_biz_inst_topo");
        interfaceNameMap.put(GET_BIZ_INTERNAL_MODULE, "get_biz_internal_module");
        interfaceNameMap.put(LIST_BIZ_HOSTS, "list_biz_hosts");
        interfaceNameMap.put(LIST_HOSTS_WITHOUT_BIZ, "list_hosts_without_biz");
        interfaceNameMap.put(FIND_HOST_BIZ_RELATIONS, "find_host_biz_relations");
        interfaceNameMap.put(LIST_BIZ_HOSTS_TOPO, "list_biz_hosts_topo");
        interfaceNameMap.put(FIND_MODULE_HOST_RELATION, "find_module_host_relation");
        interfaceNameMap.put(RESOURCE_WATCH, "resource_watch");
        interfaceNameMap.put(SEARCH_BUSINESS, "search_business");
        interfaceNameMap.put(GET_CLOUD_AREAS, "search_cloud_area");
        interfaceNameMap.put(GET_OBJ_ATTRIBUTES, "search_object_attribute");
        interfaceNameMap.put(GET_TOPO_NODE_PATHS, "find_topo_node_paths");
        interfaceNameMap.put(SEARCH_DYNAMIC_GROUP, "search_dynamic_group");
        interfaceNameMap.put(EXECUTE_DYNAMIC_GROUP, "execute_dynamic_group");
        interfaceNameMap.put(GET_BIZ_BRIEF_CACHE_TOPO, "get_biz_brief_cache_topo");
        interfaceNameMap.put(SEARCH_BUSINESS_SET, "list_business_set");
        interfaceNameMap.put(LIST_KUBE_CONTAINER_BY_TOPO, "list_kube_container_by_topo");
        interfaceNameMap.put(GET_BIZ_KUBE_CACHE_TOPO, "get_biz_kube_cache_topo");
    }

    protected BaseCmdbApiClient(FlowController flowController,
                                AppProperties appProperties,
                                EsbProperties esbProperties,
                                BkApiGatewayProperties bkApiGatewayProperties,
                                CmdbConfig cmdbConfig,
                                MeterRegistry meterRegistry,
                                String lang) {
        WatchableHttpHelper httpHelper = HttpHelperFactory.getRetryableHttpHelper();
        this.esbCmdbApiClient = new BkApiClient(meterRegistry,
            CmdbMetricNames.CMDB_API_PREFIX,
            esbProperties.getService().getUrl(),
            httpHelper,
            lang
        );
        this.esbCmdbApiClient.setLogger(LoggerFactory.getLogger(this.getClass()));
        this.apiGwCmdbApiClient = new BkApiClient(meterRegistry,
            CmdbMetricNames.CMDB_API_PREFIX,
            bkApiGatewayProperties.getCmdb().getUrl(),
            httpHelper,
            lang
        );
        this.apiGwCmdbApiClient.setLogger(LoggerFactory.getLogger(this.getClass()));
        this.globalFlowController = flowController;
        this.cmdbConfig = cmdbConfig;
        this.cmdbSupplierAccount = cmdbConfig.getDefaultSupplierAccount();
        this.cmdbBkApiAuthorization = BkApiAuthorization.appAuthorization(
            appProperties.getCode(), appProperties.getSecret(), "admin");
    }


    protected <T extends EsbReq> T makeCmdbBaseReq(Class<T> reqClass) {
        return EsbReq.buildRequest(reqClass, cmdbSupplierAccount);
    }

    protected <R> EsbResp<R> requestCmdbApi(ApiGwType apiGwType,
                                            HttpMethodEnum method,
                                            String uri,
                                            String queryParams,
                                            EsbReq reqBody,
                                            TypeReference<EsbResp<R>> typeReference) {
        return requestCmdbApi(apiGwType, method, uri, queryParams, reqBody, typeReference, null);
    }

    protected <R> EsbResp<R> requestCmdbApi(ApiGwType apiGwType,
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
                .queryParams(queryParams)
                .body(reqBody)
                .authorization(cmdbBkApiAuthorization)
                .build();
            return getApiClientByApiGwType(apiGwType).doRequest(requestInfo, typeReference, httpHelper);
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

    private BkApiClient getApiClientByApiGwType(ApiGwType apiGwType) {
        switch (apiGwType) {
            case ESB:
                return esbCmdbApiClient;
            case BK_APIGW:
                return apiGwCmdbApiClient;
            default:
                log.error("BkApiClient for type: {} not found", apiGwType.name());
                throw new InternalException(ErrorCode.INTERNAL_ERROR);
        }
    }
}
