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

package com.tencent.bk.job.common.cc.sdk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.bk.job.common.cc.config.CmdbConfig;
import com.tencent.bk.job.common.cc.exception.CmdbException;
import com.tencent.bk.job.common.cc.model.AppRoleDTO;
import com.tencent.bk.job.common.cc.model.BaseRuleDTO;
import com.tencent.bk.job.common.cc.model.BriefTopologyDTO;
import com.tencent.bk.job.common.cc.model.BusinessInfoDTO;
import com.tencent.bk.job.common.cc.model.CcCloudAreaInfoDTO;
import com.tencent.bk.job.common.cc.model.CcCloudIdDTO;
import com.tencent.bk.job.common.cc.model.CcDynamicGroupDTO;
import com.tencent.bk.job.common.cc.model.CcGroupHostPropDTO;
import com.tencent.bk.job.common.cc.model.CcHostInfoDTO;
import com.tencent.bk.job.common.cc.model.CcInstanceDTO;
import com.tencent.bk.job.common.cc.model.CcObjAttributeDTO;
import com.tencent.bk.job.common.cc.model.ComposeRuleDTO;
import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
import com.tencent.bk.job.common.cc.model.PropertyFilterDTO;
import com.tencent.bk.job.common.cc.model.TopoNodePathDTO;
import com.tencent.bk.job.common.cc.model.req.ExecuteDynamicGroupReq;
import com.tencent.bk.job.common.cc.model.req.FindHostBizRelationsReq;
import com.tencent.bk.job.common.cc.model.req.FindModuleHostRelationReq;
import com.tencent.bk.job.common.cc.model.req.GetAppReq;
import com.tencent.bk.job.common.cc.model.req.GetBizInstTopoReq;
import com.tencent.bk.job.common.cc.model.req.GetBizInternalModuleReq;
import com.tencent.bk.job.common.cc.model.req.GetBriefCacheTopoReq;
import com.tencent.bk.job.common.cc.model.req.GetCloudAreaInfoReq;
import com.tencent.bk.job.common.cc.model.req.GetObjAttributeReq;
import com.tencent.bk.job.common.cc.model.req.GetTopoNodePathReq;
import com.tencent.bk.job.common.cc.model.req.ListBizHostReq;
import com.tencent.bk.job.common.cc.model.req.ListHostsWithoutBizReq;
import com.tencent.bk.job.common.cc.model.req.Page;
import com.tencent.bk.job.common.cc.model.req.ResourceWatchReq;
import com.tencent.bk.job.common.cc.model.req.SearchHostDynamicGroupReq;
import com.tencent.bk.job.common.cc.model.req.input.GetHostByIpInput;
import com.tencent.bk.job.common.cc.model.response.CcCountInfo;
import com.tencent.bk.job.common.cc.model.result.BizEventDetail;
import com.tencent.bk.job.common.cc.model.result.ExecuteDynamicGroupHostResult;
import com.tencent.bk.job.common.cc.model.result.FindModuleHostRelationResult;
import com.tencent.bk.job.common.cc.model.result.GetBizInternalModuleResult;
import com.tencent.bk.job.common.cc.model.result.HostBizRelationDTO;
import com.tencent.bk.job.common.cc.model.result.HostEventDetail;
import com.tencent.bk.job.common.cc.model.result.HostRelationEventDetail;
import com.tencent.bk.job.common.cc.model.result.ListBizHostResult;
import com.tencent.bk.job.common.cc.model.result.ListHostsWithoutBizResult;
import com.tencent.bk.job.common.cc.model.result.ResourceWatchResult;
import com.tencent.bk.job.common.cc.model.result.SearchAppResult;
import com.tencent.bk.job.common.cc.model.result.SearchCloudAreaResult;
import com.tencent.bk.job.common.cc.model.result.SearchDynamicGroupResult;
import com.tencent.bk.job.common.cc.util.TopologyUtil;
import com.tencent.bk.job.common.cc.util.VersionCompatUtil;
import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.esb.config.EsbConfig;
import com.tencent.bk.job.common.esb.model.EsbReq;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.sdk.AbstractEsbSdkClient;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.gse.service.QueryAgentStatusClient;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.common.model.dto.PageDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.model.error.ErrorType;
import com.tencent.bk.job.common.util.ApiUtil;
import com.tencent.bk.job.common.util.FlowController;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.common.util.Utils;
import com.tencent.bk.job.common.util.http.ExtHttpHelper;
import com.tencent.bk.job.common.util.http.HttpHelperFactory;
import com.tencent.bk.job.common.util.http.HttpMetricUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * ESB-CMDB接口调用客户端
 */
@Slf4j
public class BizCmdbClient extends AbstractEsbSdkClient implements IBizCmdbClient {

    private static final String SEARCH_BIZ_INST_TOPO = "/api/c/compapi/v2/cc/search_biz_inst_topo/";
    private static final String GET_BIZ_INTERNAL_MODULE = "/api/c/compapi/v2/cc/get_biz_internal_module/";
    private static final String LIST_BIZ_HOSTS = "/api/c/compapi/v2/cc/list_biz_hosts/";
    private static final String LIST_HOSTS_WITHOUT_BIZ = "/api/c/compapi/v2/cc/list_hosts_without_biz/";
    private static final String FIND_HOST_BIZ_RELATIONS = "/api/c/compapi/v2/cc/find_host_biz_relations/";
    private static final String LIST_BIZ_HOSTS_TOPO = "/api/c/compapi/v2/cc/list_biz_hosts_topo/";
    private static final String FIND_MODULE_HOST_RELATION = "/api/c/compapi/v2/cc/find_module_host_relation/";
    private static final String RESOURCE_WATCH = "/api/c/compapi/v2/cc/resource_watch/";
    private static final String SEARCH_BUSINESS = "/api/c/compapi/v2/cc/search_business/";
    private static final String GET_CLOUD_AREAS = "/api/c/compapi/v2/cc/search_cloud_area/";
    private static final String GET_OBJ_ATTRIBUTES = "/api/c/compapi/v2/cc/search_object_attribute/";
    private static final String GET_TOPO_NODE_PATHS = "/api/c/compapi/v2/cc/find_topo_node_paths/";
    private static final String SEARCH_DYNAMIC_GROUP = "/api/c/compapi/v2/cc/search_dynamic_group/";
    private static final String EXECUTE_DYNAMIC_GROUP = "/api/c/compapi/v2/cc/execute_dynamic_group/";
    private static final String GET_BIZ_BRIEF_CACHE_TOPO = "/api/c/compapi/v2/cc/get_biz_brief_cache_topo/";

    private static final Map<String, String> interfaceNameMap = new HashMap<>();
    private static final ConcurrentHashMap<Long, Pair<InstanceTopologyDTO, Long>> bizInstTopoMap =
        new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, ReentrantLock> bizInstTopoLockMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Pair<InstanceTopologyDTO, Long>> bizInternalTopoMap =
        new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, ReentrantLock> bizInternalTopoLockMap = new ConcurrentHashMap<>();
    public static ThreadPoolExecutor threadPoolExecutor = null;
    public static ThreadPoolExecutor longTermThreadPoolExecutor = null;
    public static CmdbConfig cmdbConfig = null;
    /**
     * 对整个应用中所有的CMDB调用进行限流
     */
    private static FlowController globalFlowController = null;

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
    }

    protected String defaultSupplierAccount;
    protected String defaultUin = "admin";
    private QueryAgentStatusClient queryAgentStatusClient;
    private final MeterRegistry meterRegistry;
    private final LoadingCache<Long, InstanceTopologyDTO> bizInstCompleteTopologyCache = CacheBuilder.newBuilder()
        .maximumSize(1000).expireAfterWrite(30, TimeUnit.SECONDS).
            build(new CacheLoader<Long, InstanceTopologyDTO>() {
                      @Override
                      public InstanceTopologyDTO load(Long bizId) {
                          return getBizInstCompleteTopology(bizId);
                      }
                  }
            );

    public BizCmdbClient(EsbConfig esbConfig, CmdbConfig cmdbConfig, QueryAgentStatusClient queryAgentStatusClient,
                         MeterRegistry meterRegistry) {
        this(esbConfig, cmdbConfig, null, queryAgentStatusClient, meterRegistry);
    }

    public BizCmdbClient(EsbConfig esbConfig, CmdbConfig cmdbConfig, String lang,
                         QueryAgentStatusClient queryAgentStatusClient, MeterRegistry meterRegistry) {
        super(esbConfig.getEsbUrl(), esbConfig.getAppCode(), esbConfig.getAppSecret(), lang,
            esbConfig.isUseEsbTestEnv());
        this.defaultSupplierAccount = cmdbConfig.getDefaultSupplierAccount();
        this.queryAgentStatusClient = queryAgentStatusClient;
        this.meterRegistry = meterRegistry;
    }

    public static void setGlobalFlowController(FlowController flowController) {
        globalFlowController = flowController;
    }

    private static void initThreadPoolExecutor(int cmdbQueryThreadsNum, int longTermCmdbQueryThreadsNum) {
        threadPoolExecutor = new ThreadPoolExecutor(cmdbQueryThreadsNum, cmdbQueryThreadsNum, 180L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(cmdbQueryThreadsNum * 4), (r, executor) -> {
            //使用请求的线程直接拉取数据
            log.error("cmdb request runnable rejected, use current thread({}), plz add more threads",
                Thread.currentThread().getName());
            r.run();
        });
        longTermThreadPoolExecutor = new ThreadPoolExecutor(longTermCmdbQueryThreadsNum, longTermCmdbQueryThreadsNum,
            180L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(longTermCmdbQueryThreadsNum * 4), (r, executor) -> {
            //使用请求的线程直接拉取数据
            log.warn("cmdb long term request runnable rejected, use current thread({}), plz add more threads",
                Thread.currentThread().getName());
            r.run();
        });
    }

    public static void init() {
        initThreadPoolExecutor(cmdbConfig.getCmdbQueryThreadsNum(),
            cmdbConfig.getFindHostRelationLongTermConcurrency());
    }

    public static void setCcConfig(CmdbConfig cmdbConfig) {
        BizCmdbClient.cmdbConfig = cmdbConfig;
    }

    public void setQueryAgentStatusClient(QueryAgentStatusClient queryAgentStatusClient) {
        this.queryAgentStatusClient = queryAgentStatusClient;
    }

    @Override
    public InstanceTopologyDTO getBizInstCompleteTopology(long bizId) {
        InstanceTopologyDTO completeTopologyDTO;
        if (cmdbConfig.getEnableInterfaceBriefCacheTopo()) {
            completeTopologyDTO = getBriefCacheTopo(bizId);
        } else {
            InstanceTopologyDTO topologyDTO = getBizInstTopologyWithoutInternalTopo(bizId);
            InstanceTopologyDTO internalTopologyDTO = getBizInternalModule(bizId);
            internalTopologyDTO.setObjectName(topologyDTO.getObjectName());
            internalTopologyDTO.setInstanceName(topologyDTO.getInstanceName());
            completeTopologyDTO = TopologyUtil.mergeTopology(internalTopologyDTO, topologyDTO);
        }
        return completeTopologyDTO;
    }

    public InstanceTopologyDTO getCachedBizInstCompleteTopology(long bizId) {
        try {
            return bizInstCompleteTopologyCache.get(bizId);
        } catch (ExecutionException e) {
            throw new InternalException(e, ErrorCode.CMDB_API_DATA_ERROR, null);
        }
    }

    @Override
    public InstanceTopologyDTO getBizInstTopology(long bizId) {
        return getCachedBizInstCompleteTopology(bizId);
    }

    public InstanceTopologyDTO getBizInstTopologyWithoutInternalTopo(long bizId) {
        if (cmdbConfig.getEnableLockOptimize()) {
            return getBizInstTopologyWithoutInternalTopoWithLock(bizId);
        } else {
            return getBizInstTopologyWithoutInternalTopoFromCMDB(bizId);
        }
    }

    public InstanceTopologyDTO getBizInstTopologyWithoutInternalTopoWithLock(long bizId) {
        ReentrantLock lock;
        if (bizInstTopoMap.containsKey(bizId)
            && bizInstTopoMap.get(bizId).getRight() > System.currentTimeMillis() - 30 * 1000) {
            return bizInstTopoMap.get(bizId).getLeft();
        } else {
            lock = bizInstTopoLockMap.computeIfAbsent(bizId, s -> new ReentrantLock());
            lock.lock();
            try {
                if (bizInstTopoMap.containsKey(bizId)
                    && bizInstTopoMap.get(bizId).getRight() > System.currentTimeMillis() - 30 * 1000) {
                    return bizInstTopoMap.get(bizId).getLeft();
                } else {
                    InstanceTopologyDTO topo = getBizInstTopologyWithoutInternalTopoFromCMDB(bizId);
                    bizInstTopoMap.put(bizId, Pair.of(topo, System.currentTimeMillis()));
                    return topo;
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public InstanceTopologyDTO getBriefCacheTopo(long bizId) {
        GetBriefCacheTopoReq req = makeBaseReq(GetBriefCacheTopoReq.class, defaultUin, defaultSupplierAccount);
        req.setBizId(bizId);
        EsbResp<BriefTopologyDTO> esbResp = requestCmdbApi(HttpGet.METHOD_NAME, GET_BIZ_BRIEF_CACHE_TOPO, req,
            new TypeReference<EsbResp<BriefTopologyDTO>>() {
            });
        return TopologyUtil.convert(esbResp.getData());
    }

    public InstanceTopologyDTO getBizInstTopologyWithoutInternalTopoFromCMDB(long bizId) {
        GetBizInstTopoReq req = makeBaseReq(GetBizInstTopoReq.class, defaultUin, defaultSupplierAccount);
        req.setBizId(bizId);
        EsbResp<List<InstanceTopologyDTO>> esbResp = requestCmdbApi(HttpGet.METHOD_NAME, SEARCH_BIZ_INST_TOPO,
            req, new TypeReference<EsbResp<List<InstanceTopologyDTO>>>() {
            });
        if (esbResp.getData().size() > 0) {
            return esbResp.getData().get(0);
        } else {
            return null;
        }
    }

    public <R> EsbResp<R> requestCmdbApi(String method,
                                         String uri,
                                         EsbReq reqBody,
                                         TypeReference<EsbResp<R>> typeReference) {
        return requestCmdbApi(method, uri, reqBody, typeReference, null);
    }

    public <R> EsbResp<R> requestCmdbApi(String method,
                                         String uri,
                                         EsbReq reqBody,
                                         TypeReference<EsbResp<R>> typeReference,
                                         ExtHttpHelper httpHelper) {

        String resourceId = ApiUtil.getApiNameByUri(interfaceNameMap, uri);
        if (cmdbConfig != null && cmdbConfig.getEnableFlowControl()) {
            if (globalFlowController != null) {
                log.debug("Flow control resourceId={}", resourceId);
                long startTime = System.currentTimeMillis();
                globalFlowController.acquire(resourceId);
                long duration = System.currentTimeMillis() - startTime;
                if (duration >= 5000) {
                    log.warn("Request resource {} wait flowControl for {}ms", resourceId, duration);
                } else if (duration >= 1000) {
                    log.info("Request resource {} wait flowControl for {}ms", resourceId, duration);
                }
            } else {
                log.debug("globalFlowController not set, ignore this time");
            }
        }
        long start = System.nanoTime();
        String status = "none";
        try {
            HttpMetricUtil.setHttpMetricName(CommonMetricNames.ESB_CMDB_API_HTTP);
            HttpMetricUtil.addTagForCurrentMetric(Tag.of("api_name", uri));
            EsbResp<R> esbResp = getEsbRespByReq(method, uri, reqBody, typeReference, httpHelper);
            status = "ok";
            return esbResp;
        } catch (Throwable e) {
            String reqStr = JsonUtils.toJsonWithoutSkippedFields(reqBody);
            String errorMsg = "Fail to request CMDB data|method=" + method + "|uri=" + uri + "|reqStr=" + reqStr;
            log.error(errorMsg, e);
            status = "error";
            throw new InternalException(e.getMessage(), e, ErrorCode.CMDB_API_DATA_ERROR);
        } finally {
            HttpMetricUtil.clearHttpMetric();
            long end = System.nanoTime();
            meterRegistry.timer(CommonMetricNames.ESB_CMDB_API, "api_name", uri, "status", status)
                .record(end - start, TimeUnit.NANOSECONDS);
        }
    }

    @Override
    public InstanceTopologyDTO getBizInternalModule(long bizId) {
        if (cmdbConfig.getEnableLockOptimize()) {
            return getBizInternalModuleWithLock(bizId);
        } else {
            return getBizInternalModuleFromCMDB(bizId);
        }
    }

    /**
     * 防止参数完全相同的请求在并发时多次请求CMDB，降低对CMDB的请求量
     */
    public InstanceTopologyDTO getBizInternalModuleWithLock(long bizId) {
        ReentrantLock lock;
        if (bizInternalTopoMap.containsKey(bizId)
            && bizInternalTopoMap.get(bizId).getRight() > System.currentTimeMillis() - 30 * 1000) {
            return bizInternalTopoMap.get(bizId).getLeft();
        } else {
            lock = bizInternalTopoLockMap.computeIfAbsent(bizId, s -> new ReentrantLock());
            lock.lock();
            try {
                if (bizInternalTopoMap.containsKey(bizId)
                    && bizInternalTopoMap.get(bizId).getRight() > System.currentTimeMillis() - 30 * 1000) {
                    return bizInternalTopoMap.get(bizId).getLeft();
                } else {
                    InstanceTopologyDTO topo = getBizInternalModuleFromCMDB(bizId);
                    bizInternalTopoMap.put(bizId, Pair.of(topo, System.currentTimeMillis()));
                    return topo;
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public InstanceTopologyDTO getBizInternalModuleFromCMDB(long bizId) {
        GetBizInternalModuleReq req = makeBaseReq(GetBizInternalModuleReq.class, defaultUin, defaultSupplierAccount);
        req.setBizId(bizId);
        EsbResp<GetBizInternalModuleResult> esbResp = requestCmdbApi(HttpGet.METHOD_NAME,
            GET_BIZ_INTERNAL_MODULE, req, new TypeReference<EsbResp<GetBizInternalModuleResult>>() {
            });
        GetBizInternalModuleResult setInfo = esbResp.getData();
        //将结果转换为拓扑树
        InstanceTopologyDTO setNode = new InstanceTopologyDTO();
        setNode.setObjectId("set");
        setNode.setObjectName("Set");
        setNode.setInstanceId(setInfo.getSetId());
        setNode.setInstanceName(setInfo.getSetName());
        List<InstanceTopologyDTO> childList = new ArrayList<>();
        List<GetBizInternalModuleResult.Module> modules = setInfo.getModule();
        if (modules != null && !modules.isEmpty()) {
            for (GetBizInternalModuleResult.Module module : modules) {
                InstanceTopologyDTO childModule = new InstanceTopologyDTO();
                childModule.setObjectId("module");
                childModule.setObjectName("Module");
                childModule.setInstanceId(module.getModuleId());
                childModule.setInstanceName(module.getModuleName());
                childList.add(childModule);
            }
        }
        setNode.setChild(childList);
        InstanceTopologyDTO bizNode = new InstanceTopologyDTO();
        bizNode.setObjectId("biz");
        bizNode.setInstanceId(bizId);
        childList = new ArrayList<>();
        childList.add(setNode);
        bizNode.setChild(childList);
        return bizNode;
    }

    @Override
    public List<ApplicationHostDTO> getHosts(long bizId, List<CcInstanceDTO> ccInstList) {
        return getHostsByTopology(bizId, ccInstList);
    }

    @Override
    public List<ApplicationHostDTO> getHostsByTopology(long bizId, List<CcInstanceDTO> ccInstList) {
        StopWatch watch = new StopWatch("getHostsByTopology");
        watch.start("getCachedBizInstCompleteTopology");
        InstanceTopologyDTO appCompleteTopology = getCachedBizInstCompleteTopology(bizId);
        watch.stop();

        watch.start("findModuleIdsFromTopo");
        Set<Long> moduleIdSet = new HashSet<>();

        for (CcInstanceDTO ccInstanceDTO : ccInstList) {
            //找到不含空闲机拓扑中对应节点
            InstanceTopologyDTO topologyDTO = TopologyUtil.findNodeFromTopo(appCompleteTopology, ccInstanceDTO);
            if (topologyDTO != null) {
                //找出拓扑节点下的所有module
                List<Long> moduleIdList = TopologyUtil.findModuleIdsFromTopo(topologyDTO);
                moduleIdSet.addAll(moduleIdList);
            }
        }
        watch.stop();

        //根据module找主机
        watch.start("findHostByModule");
        List<ApplicationHostDTO> applicationHostDTOList = findHostByModule(bizId,
            new ArrayList<>(moduleIdSet));
        watch.stop();

        if (watch.getTotalTimeMillis() > 1000L) {
            log.warn("Get hosts by topo is slow, bizId: {}, ccInsts: {}, watchInfo: {}", bizId, ccInstList,
                watch.prettyPrint());
        }
        return applicationHostDTOList;
    }

    @Override
    public List<ApplicationHostDTO> findHostByModule(long bizId, List<Long> moduleIdList) {
        //moduleId分批
        List<ApplicationHostDTO> resultList = new ArrayList<>();
        int batchSize = 200;
        int start = 0;
        int end = start + batchSize;
        int moduleIdSize = moduleIdList.size();
        end = Math.min(end, moduleIdSize);
        do {
            List<Long> moduleIdSubList = moduleIdList.subList(start, end);
            if (moduleIdSubList.size() > 0) {
                // 使用find_module_host_relation接口
                resultList.addAll(findModuleHostRelationConcurrently(bizId, moduleIdSubList));
            }
            start += batchSize;
            end = start + batchSize;
            end = Math.min(end, moduleIdSize);
        } while (start < moduleIdSize);
        return resultList;
    }

    private FindModuleHostRelationResult getHostsByReq(FindModuleHostRelationReq req) {
        EsbResp<FindModuleHostRelationResult> esbResp = requestCmdbApi(HttpPost.METHOD_NAME,
            FIND_MODULE_HOST_RELATION, req, new TypeReference<EsbResp<FindModuleHostRelationResult>>() {
            });
        return esbResp.getData();
    }

    private FindModuleHostRelationReq genFindModuleHostRelationReq(long bizId, List<Long> moduleIdList, int start,
                                                                   int limit) {
        FindModuleHostRelationReq req = makeBaseReq(FindModuleHostRelationReq.class, defaultUin,
            defaultSupplierAccount);
        req.setBizId(bizId);
        req.setModuleIdList(moduleIdList);
        Page page = new Page(start, limit);
        req.setPage(page);
        return req;
    }

    /**
     * 并发：按模块加载主机
     *
     * @param bizId        cmdb业务ID
     * @param moduleIdList 模块ID列表
     * @return 主机列表
     */
    private List<ApplicationHostDTO> findModuleHostRelationConcurrently(long bizId, List<Long> moduleIdList) {
        if (moduleIdList == null || moduleIdList.isEmpty()) {
            return Collections.emptyList();
        }

        List<ApplicationHostDTO> applicationHostDTOList;
        int start = 0;
        //已调优
        int limit = 500;
        FindModuleHostRelationReq req = genFindModuleHostRelationReq(bizId, moduleIdList, start, limit);
        //先拉一次获取总数
        FindModuleHostRelationResult pageData = getHostsByReq(req);
        List<FindModuleHostRelationResult.HostWithModules> hostWithModulesList = pageData.getRelation();
        LinkedBlockingQueue<FindModuleHostRelationResult.HostWithModules> resultQueue =
            new LinkedBlockingQueue<>(hostWithModulesList);
        // 如果该页未达到limit，说明是最后一页
        if (pageData.getCount() <= limit) {
            log.info("bizId {}:{} hosts in total, {} hosts indeed", bizId, pageData.getCount(), resultQueue.size());
        } else if (pageData.getCount() > limit && hostWithModulesList.size() <= limit) {
            int totalCount = pageData.getCount() - limit;
            List<Future<?>> futures = new ArrayList<>();
            Long startTime = System.currentTimeMillis();
            while (totalCount > 0) {
                start += limit;
                FindModuleHostRelationTask task = new FindModuleHostRelationTask(resultQueue,
                    genFindModuleHostRelationReq(bizId, moduleIdList, start, limit),
                    JobContextUtil.getRequestId());
                if (totalCount > 10000) {
                    //主机数太多，防止将CMDB拉挂了
                    Future<?> future = longTermThreadPoolExecutor.submit(task);
                    futures.add(future);
                } else {
                    // 默认采用多个并发线程拉取
                    Future<?> future = threadPoolExecutor.submit(task);
                    futures.add(future);
                }
                totalCount -= limit;
            }
            futures.forEach(it -> {
                while (!it.isDone()) {
                    ThreadUtils.sleep(100);
                }
            });
            Long endTime = System.currentTimeMillis();
            log.info("find module hosts concurrently time consuming:" + (endTime - startTime));
        } else {
            //limit参数不起效，可能拉到了全量数据，直接跳出
            log.warn("bizId {}:{} hosts in total, {} hosts indeed, CMDB interface params invalid", bizId,
                pageData.getCount(), resultQueue.size());
        }
        Long startTime = System.currentTimeMillis();
        applicationHostDTOList = convertToHostInfoDTOList(bizId, new ArrayList<>(resultQueue));
        Long endTime = System.currentTimeMillis();
        long timeConsuming = endTime - startTime;
        if (timeConsuming >= 1000) {
            log.info("convertToHostInfoDTOList time consuming:" + timeConsuming);
        }
        return applicationHostDTOList;
    }

    private void fillAgentInfo(
        ApplicationHostDTO applicationHostDTO,
        FindModuleHostRelationResult.HostProp host
    ) {
        String multiIp = host.getIp();
        multiIp = multiIp.trim();
        if (queryAgentStatusClient != null) {
            if (multiIp.contains(",")) {
                Pair<String, Boolean> pair = queryAgentStatusClient.getHostIpWithAgentStatus(multiIp,
                    host.getCloudAreaId());
                if (pair != null) {
                    log.debug("query agent status:{}:{}", pair.getLeft(), pair.getRight());
                    String ipWithCloudId = pair.getLeft();
                    applicationHostDTO.setGseAgentAlive(pair.getRight());
                    if (ipWithCloudId.contains(":")) {
                        String[] arr = ipWithCloudId.split(":");
                        applicationHostDTO.setCloudAreaId(Long.parseLong(arr[0]));
                        applicationHostDTO.setIp(arr[1]);
                    } else {
                        applicationHostDTO.setIp(ipWithCloudId);
                    }
                } else {
                    log.warn("Fail to get agentStatus, host={}", JsonUtils.toJson(host));
                }
            } else {
                applicationHostDTO.setGseAgentAlive(false);
                applicationHostDTO.setCloudAreaId(host.getCloudAreaId());
                applicationHostDTO.setIp(multiIp);
            }
        } else {
            log.warn("queryAgentStatusClient==null, please check!");
            List<String> ipList = Utils.getNotBlankSplitList(multiIp, ",");
            if (ipList.size() > 0) {
                applicationHostDTO.setIp(ipList.get(0));
            } else {
                log.warn("no available ip after queryAgentStatusClient");
            }
        }
    }

    private ApplicationHostDTO convertToHostInfoDTO(
        Long bizId,
        FindModuleHostRelationResult.HostWithModules hostWithModules
    ) {
        FindModuleHostRelationResult.HostProp host = hostWithModules.getHost();
        String multiIp = host.getIp();
        if (multiIp != null) {
            multiIp = multiIp.trim();
        } else {
            log.warn("multiIp is null, bizId={}, host={}", bizId, hostWithModules);
        }
        //包装为ApplicationHostInfoDTO
        ApplicationHostDTO applicationHostDTO = new ApplicationHostDTO();
        applicationHostDTO.setBizId(bizId);
        applicationHostDTO.setDisplayIp(multiIp);
        applicationHostDTO.setCloudAreaId(host.getCloudAreaId());
        applicationHostDTO.setHostId(host.getHostId());
        fillAgentInfo(applicationHostDTO, host);
        List<FindModuleHostRelationResult.ModuleProp> modules = hostWithModules.getModules();
        for (FindModuleHostRelationResult.ModuleProp module : modules) {
            if (module == null || null == module.getModuleId()) {
                log.warn("invalid host:" + JsonUtils.toJson(applicationHostDTO));
            }
        }
        List<FindModuleHostRelationResult.ModuleProp> validModules =
            hostWithModules.getModules().stream().filter(Objects::nonNull).collect(Collectors.toList());
        applicationHostDTO.setModuleId(
            validModules.stream()
                .map(FindModuleHostRelationResult.ModuleProp::getModuleId)
                .collect(Collectors.toList()));
        applicationHostDTO.setSetId(
            validModules.stream()
                .map(FindModuleHostRelationResult.ModuleProp::getSetId)
                .collect(Collectors.toList()));
        applicationHostDTO.setModuleType(validModules.stream().map(it -> {
            try {
                return Long.parseLong(it.getModuleType());
            } catch (Exception e) {
                return 0L;
            }
        }).collect(Collectors.toList()));
        applicationHostDTO.setIpDesc(host.getHostName());
        String os = host.getOs();
        if (os != null && os.length() > 512) {
            applicationHostDTO.setOs(os.substring(0, 512));
        } else {
            applicationHostDTO.setOs(os);
        }
        applicationHostDTO.setOsType(host.getOsType());
        return applicationHostDTO;
    }

    private List<ApplicationHostDTO> convertToHostInfoDTOList(
        long bizId,
        List<FindModuleHostRelationResult.HostWithModules> hostWithModulesList) {
        List<ApplicationHostDTO> applicationHostDTOList = new ArrayList<>();
        Set<String> ipSet = new HashSet<>();
        for (FindModuleHostRelationResult.HostWithModules hostWithModules : hostWithModulesList) {
            FindModuleHostRelationResult.HostProp host = hostWithModules.getHost();
            if (host == null) {
                log.warn("host=null,hostWithTopoInfo={}", JsonUtils.toJson(hostWithModules));
                continue;
            }
            ipSet.add(host.getCloudAreaId() + ":" + host.getIp());
            String multiIp = host.getIp();
            if (!StringUtils.isBlank(multiIp)) {
                ApplicationHostDTO applicationHostDTO = convertToHostInfoDTO(bizId, hostWithModules);
                applicationHostDTOList.add(applicationHostDTO);
            } else {
                log.info("bk_host_innerip is blank, ignore, hostId={},host={}", host.getHostId(),
                    JsonUtils.toJson(host));
            }
        }
        log.info("ipSet.size=" + ipSet.size());
        return applicationHostDTOList;
    }

    private ApplicationHostDTO convertHost(long bizId, CcHostInfoDTO hostInfo) {
        ApplicationHostDTO ipInfo = new ApplicationHostDTO();
        ipInfo.setHostId(hostInfo.getHostId());
        // 部分从cmdb同步过来的资源没有ip，需要过滤掉
        if (StringUtils.isBlank(hostInfo.getIp())) {
            return null;
        }

        if (hostInfo.getOs() != null && hostInfo.getOs().length() > 512) {
            ipInfo.setOs(hostInfo.getOs().substring(0, 512));
        } else {
            ipInfo.setOs(hostInfo.getOs());
        }
        if (hostInfo.getCloudId() != null) {
            ipInfo.setCloudAreaId(hostInfo.getCloudId());
        } else {
            log.warn("Host does not have cloud area id!|{}", hostInfo);
            return null;
        }

        if (queryAgentStatusClient != null) {
            ipInfo.setIp(queryAgentStatusClient.getHostIpByAgentStatus(hostInfo.getIp(), hostInfo.getCloudId()));
        } else {
            ipInfo.setIp(hostInfo.getIp());
        }
        ipInfo.setBizId(bizId);
        ipInfo.setIpDesc(hostInfo.getHostName());

        return ipInfo;
    }

    @Override
    public List<ApplicationDTO> getAllBizApps() {
        List<ApplicationDTO> appList = new ArrayList<>();
        int limit = 200;
        int start = 0;
        boolean isLastPage = false;
        String orderField = "bk_biz_id";
        while (!isLastPage) {
            GetAppReq req = makeBaseReq(GetAppReq.class, defaultUin, defaultSupplierAccount);
            PageDTO page = new PageDTO(start, limit, orderField);
            req.setPage(page);
            EsbResp<SearchAppResult> esbResp = requestCmdbApi(HttpPost.METHOD_NAME, SEARCH_BUSINESS, req,
                new TypeReference<EsbResp<SearchAppResult>>() {
                });
            SearchAppResult data = esbResp.getData();
            if (data == null) {
                appList.clear();
                throw new InternalException("Data is null", ErrorCode.CMDB_API_DATA_ERROR);
            }
            List<BusinessInfoDTO> businessInfos = data.getInfo();
            if (businessInfos != null && !businessInfos.isEmpty()) {
                for (BusinessInfoDTO businessInfo : businessInfos) {
                    if (businessInfo.getDefaultApp() == 0) {
                        appList.add(convertToAppInfo(businessInfo));
                    }
                }
                start += businessInfos.size();
            }
            // 如果该页未达到limit，说明是最后一页
            if (businessInfos == null || businessInfos.size() < limit) {
                isLastPage = true;
            }
        }
        return appList;
    }

    private ApplicationDTO convertToAppInfo(BusinessInfoDTO businessInfo) {
        ApplicationDTO appInfo = new ApplicationDTO();
        appInfo.setName(businessInfo.getBizName());
        appInfo.setMaintainers(VersionCompatUtil.convertMaintainers(businessInfo.getMaintainers()));
        appInfo.setBkSupplierAccount(businessInfo.getSupplierAccount());
        appInfo.setTimeZone(businessInfo.getTimezone());
        appInfo.setScope(new ResourceScope(ResourceScopeTypeEnum.BIZ, businessInfo.getBizId().toString()));
        appInfo.setAppType(AppTypeEnum.NORMAL);
        appInfo.setOperateDeptId(businessInfo.getOperateDeptId());
        appInfo.setLanguage(businessInfo.getLanguage());
        return appInfo;
    }

    @Override
    public ApplicationDTO getBizAppById(long bizId) {
        GetAppReq req = makeBaseReq(GetAppReq.class, defaultUin, defaultSupplierAccount);
        Map<String, Object> conditionMap = new HashMap<>();
        conditionMap.put("bk_biz_id", bizId);
        req.setCondition(conditionMap);
        EsbResp<SearchAppResult> esbResp = requestCmdbApi(HttpPost.METHOD_NAME, SEARCH_BUSINESS, req,
            new TypeReference<EsbResp<SearchAppResult>>() {
            });
        SearchAppResult data = esbResp.getData();
        if (data == null) {
            throw new InternalException("data is null", ErrorCode.CMDB_API_DATA_ERROR);
        }
        List<BusinessInfoDTO> businessInfos = data.getInfo();
        if (businessInfos == null || businessInfos.isEmpty()) {
            throw new InternalException("data is null", ErrorCode.CMDB_API_DATA_ERROR);
        }
        return convertToAppInfo(businessInfos.get(0));
    }

    @Override
    public List<CcDynamicGroupDTO> getDynamicGroupList(long bizId) {
        SearchHostDynamicGroupReq req = makeBaseReq(SearchHostDynamicGroupReq.class, defaultUin,
            defaultSupplierAccount);
        req.setBizId(bizId);
        int start = 0;
        int limit = 200;
        req.getPage().setStart(start);
        req.getPage().setLimit(limit);
        List<CcDynamicGroupDTO> ccDynamicGroupList = new ArrayList<>();
        boolean isLastPage = false;
        while (!isLastPage) {
            req.getPage().setStart(start);

            EsbResp<SearchDynamicGroupResult> esbResp = requestCmdbApi(HttpPost.METHOD_NAME,
                SEARCH_DYNAMIC_GROUP, req, new TypeReference<EsbResp<SearchDynamicGroupResult>>() {
                });
            if (!esbResp.getResult()) {
                // 由于参数问题导致的CMDB返回数据异常
                throw new CmdbException(
                    ErrorType.FAILED_PRECONDITION,
                    ErrorCode.FAIL_TO_FIND_DYNAMIC_GROUP_BY_BIZ,
                    new Object[]{bizId, esbResp.getMessage()}
                );
            }
            SearchDynamicGroupResult ccRespData = esbResp.getData();
            if (ccRespData != null) {
                List<CcDynamicGroupDTO> groupInfos = ccRespData.getInfo();
                ccDynamicGroupList.addAll(groupInfos);
                start += groupInfos.size();
                // 如果该页未达到limit，说明是最后一页
                if (groupInfos.size() < limit) {
                    isLastPage = true;
                }
            }
        }
        return ccDynamicGroupList;
    }

    private List<CcGroupHostPropDTO> convertToCcGroupHostPropList(List<CcHostInfoDTO> hostInfoList) {
        List<CcGroupHostPropDTO> ccGroupHostList = new ArrayList<>();
        for (CcHostInfoDTO ccHostInfo : hostInfoList) {
            if (ccHostInfo.getCloudId() == null || ccHostInfo.getCloudId() < 0) {
                log.warn(
                    "host(id={},ip={}) does not have cloud area, ignore",
                    ccHostInfo.getHostId(),
                    ccHostInfo.getIp()
                );
            } else if (StringUtils.isBlank(ccHostInfo.getIp())) {
                log.warn(
                    "host(id={},ip={}) ip invalid, ignore",
                    ccHostInfo.getHostId(),
                    ccHostInfo.getIp()
                );
            } else {
                ccGroupHostList.add(convertToCcHost(ccHostInfo));
            }
        }
        return ccGroupHostList;
    }

    @Override
    public List<CcGroupHostPropDTO> getDynamicGroupIp(long bizId, String groupId) {
        ExecuteDynamicGroupReq req = makeBaseReq(ExecuteDynamicGroupReq.class, defaultUin, defaultSupplierAccount);
        req.setBizId(bizId);
        req.setGroupId(groupId);
        int limit = 200;
        int start = 0;
        req.getPage().setLimit(limit);

        List<CcGroupHostPropDTO> ccGroupHostList = new ArrayList<>();
        boolean isLastPage = false;
        while (!isLastPage) {
            req.getPage().setStart(start);
            EsbResp<ExecuteDynamicGroupHostResult> esbResp = requestCmdbApi(HttpPost.METHOD_NAME,
                EXECUTE_DYNAMIC_GROUP, req, new TypeReference<EsbResp<ExecuteDynamicGroupHostResult>>() {
                });
            ExecuteDynamicGroupHostResult ccRespData = esbResp.getData();
            if (!esbResp.getResult()) {
                // 由于参数问题导致的CMDB返回数据异常
                throw new CmdbException(
                    ErrorType.FAILED_PRECONDITION,
                    ErrorCode.FAIL_TO_FIND_HOST_BY_DYNAMIC_GROUP,
                    new String[]{groupId, esbResp.getMessage()}
                );
            }
            if (ccRespData != null) {
                List<CcHostInfoDTO> hostInfoList = ccRespData.getInfo();
                ccGroupHostList.addAll(convertToCcGroupHostPropList(hostInfoList));
                start += hostInfoList.size();
                // 如果该页未达到limit，说明是最后一页
                if (hostInfoList.size() < limit) {
                    isLastPage = true;
                }
            } else {
                log.warn("ccRespData is null");
                isLastPage = true;
            }
        }
        return ccGroupHostList;
    }

    private CcGroupHostPropDTO convertToCcHost(CcHostInfoDTO ccHostInfo) {
        CcGroupHostPropDTO ccGroupHostPropDTO = new CcGroupHostPropDTO();
        ccGroupHostPropDTO.setId(ccHostInfo.getHostId());
        ccGroupHostPropDTO.setName(ccHostInfo.getHostName());
        ccGroupHostPropDTO.setIp(ccHostInfo.getIp());
        CcCloudIdDTO ccCloudIdDTO = new CcCloudIdDTO();
        // 仅使用CloudId其余属性未用到，暂不设置
        ccCloudIdDTO.setInstanceId(ccHostInfo.getCloudId());
        ccGroupHostPropDTO.setCloudIdList(Collections.singletonList(ccCloudIdDTO));
        return ccGroupHostPropDTO;
    }

    @Override
    public List<CcCloudAreaInfoDTO> getCloudAreaList() {
        List<CcCloudAreaInfoDTO> appCloudAreaList = new ArrayList<>();
        boolean isLastPage = false;
        int limit = 200;
        int start = 0;
        while (!isLastPage) {
            GetCloudAreaInfoReq req = makeBaseReq(GetCloudAreaInfoReq.class, defaultUin, defaultSupplierAccount);
            PageDTO page = new PageDTO(start, limit, null);
            req.setPage(page);
            req.setCondition(Collections.emptyMap());
            EsbResp<SearchCloudAreaResult> esbResp = requestCmdbApi(HttpPost.METHOD_NAME, GET_CLOUD_AREAS, req,
                new TypeReference<EsbResp<SearchCloudAreaResult>>() {
                });
            SearchCloudAreaResult data = esbResp.getData();
            if (data == null) {
                appCloudAreaList.clear();
                return appCloudAreaList;
            }
            List<CcCloudAreaInfoDTO> cloudAreaInfoList = data.getInfo();
            if (CollectionUtils.isNotEmpty(cloudAreaInfoList)) {
                appCloudAreaList.addAll(cloudAreaInfoList);
            }
            // 如果该页未达到limit，说明是最后一页
            if (cloudAreaInfoList == null || cloudAreaInfoList.size() < limit) {
                isLastPage = true;
            } else {
                start += limit;
            }
        }
        return appCloudAreaList;
    }

    @Override
    public List<ApplicationHostDTO> listBizHosts(long bizId, Collection<IpDTO> ipList) {
        if (ipList == null || ipList.isEmpty()) {
            return Collections.emptyList();
        }
        List<ApplicationHostDTO> appHosts = getHostByIp(new GetHostByIpInput(bizId, null, null,
            ipList.stream().map(IpDTO::getIp).collect(Collectors.toList())));
        if (appHosts == null || appHosts.isEmpty()) {
            return Collections.emptyList();
        }
        return appHosts.stream().filter(host ->
            ipList.contains(new IpDTO(host.getCloudAreaId(), host.getIp()))).collect(Collectors.toList());
    }

    @Override
    public List<HostBizRelationDTO> findHostBizRelations(String uin, List<Long> hostIdList) {
        FindHostBizRelationsReq req = makeBaseReq(FindHostBizRelationsReq.class, defaultUin, defaultSupplierAccount);
        req.setHostIdList(hostIdList);
        EsbResp<List<HostBizRelationDTO>> esbResp = requestCmdbApi(HttpPost.METHOD_NAME,
            FIND_HOST_BIZ_RELATIONS, req, new TypeReference<EsbResp<List<HostBizRelationDTO>>>() {
            });
        List<HostBizRelationDTO> results = esbResp.getData();
        if (esbResp.getData() == null) {
            return Collections.emptyList();
        }
        return results;
    }

    @Override
    public List<ApplicationHostDTO> getHostByIp(GetHostByIpInput input) {
        List<ApplicationHostDTO> hostInfoList = new ArrayList<>();
        ListBizHostReq req = makeBaseReq(ListBizHostReq.class, defaultUin, defaultSupplierAccount);
        req.setBizId(input.getBizId());
        PropertyFilterDTO condition = new PropertyFilterDTO();
        condition.setCondition("AND");
        input.ipList.removeIf(StringUtils::isBlank);
        BaseRuleDTO baseRuleDTO = new BaseRuleDTO();
        baseRuleDTO.setField("bk_host_innerip");
        baseRuleDTO.setOperator("in");
        baseRuleDTO.setValue(input.ipList);
        condition.addRule(baseRuleDTO);
        req.setCondition(condition);

        int limit = 200;
        int start = 0;
        boolean isLastPage = false;
        while (!isLastPage) {
            PageDTO page = new PageDTO(start, limit, "");
            req.setPage(page);
            EsbResp<ListBizHostResult> esbResp = requestCmdbApi(HttpPost.METHOD_NAME, LIST_BIZ_HOSTS, req,
                new TypeReference<EsbResp<ListBizHostResult>>() {
                });
            ListBizHostResult pageData = esbResp.getData();
            if (esbResp.getData() == null) {
                return Collections.emptyList();
            }
            for (CcHostInfoDTO hostInfo : pageData.getInfo()) {
                start++;
                ApplicationHostDTO host = convertHost(input.getBizId(), hostInfo);
                if (host != null) {
                    hostInfoList.add(host);
                }
            }
            // 如果该页未达到limit，说明是最后一页
            if (pageData.getInfo().size() < limit) {
                isLastPage = true;
            }
        }
        return hostInfoList;
    }

    @Override
    public ApplicationHostDTO getHostByIp(Long cloudAreaId, String ip) {
        ListHostsWithoutBizReq req = makeBaseReq(ListHostsWithoutBizReq.class, defaultUin, defaultSupplierAccount);
        PropertyFilterDTO condition = new PropertyFilterDTO();
        condition.setCondition("AND");
        BaseRuleDTO ipRule = new BaseRuleDTO();
        ipRule.setField("bk_host_innerip");
        ipRule.setOperator("equal");
        ipRule.setValue(ip);
        condition.addRule(ipRule);
        BaseRuleDTO bkCloudIdRule = new BaseRuleDTO();
        bkCloudIdRule.setField("bk_cloud_id");
        bkCloudIdRule.setOperator("equal");
        bkCloudIdRule.setValue(cloudAreaId);
        condition.addRule(bkCloudIdRule);
        req.setCondition(condition);

        List<ApplicationHostDTO> hosts = listHostsWithoutBiz(req);
        return hosts.size() > 0 ? hosts.get(0) : null;
    }

    @Override
    public List<ApplicationHostDTO> listHostsByIps(List<IpDTO> hostIps) {
        ListHostsWithoutBizReq req = makeBaseReq(ListHostsWithoutBizReq.class, defaultUin, defaultSupplierAccount);
        PropertyFilterDTO condition = new PropertyFilterDTO();
        condition.setCondition("OR");
        Map<Long, List<IpDTO>> hostGroups = groupHostsByCloudAreaId(hostIps);
        hostGroups.forEach((bkCloudId, hosts) -> {
            ComposeRuleDTO hostRule = new ComposeRuleDTO();
            hostRule.setCondition("AND");
            BaseRuleDTO bkCloudIdRule = new BaseRuleDTO();
            bkCloudIdRule.setField("bk_cloud_id");
            bkCloudIdRule.setOperator("equal");
            bkCloudIdRule.setValue(bkCloudId);
            hostRule.addRule(bkCloudIdRule);

            BaseRuleDTO ipRule = new BaseRuleDTO();
            ipRule.setField("bk_host_innerip");
            ipRule.setOperator("in");
            ipRule.setValue(hosts.stream().map(IpDTO::getIp).collect(Collectors.toList()));
            hostRule.addRule(ipRule);

            condition.addRule(hostRule);
        });
        req.setCondition(condition);

        return listHostsWithoutBiz(req);
    }

    private List<ApplicationHostDTO> listHostsWithoutBiz(ListHostsWithoutBizReq req) {
        int limit = 500;
        int start = 0;
        PageDTO page = new PageDTO(start, limit, "");
        req.setPage(page);
        EsbResp<ListHostsWithoutBizResult> esbResp = requestCmdbApi(HttpPost.METHOD_NAME, LIST_HOSTS_WITHOUT_BIZ,
            req, new TypeReference<EsbResp<ListHostsWithoutBizResult>>() {
            });
        ListHostsWithoutBizResult pageData = esbResp.getData();
        if (esbResp.getData() == null || CollectionUtils.isEmpty(esbResp.getData().getInfo())) {
            return Collections.emptyList();
        }

        List<ApplicationHostDTO> hosts =
            pageData.getInfo().stream().map(host -> convertHost(-1, host)).collect(Collectors.toList());

        // 设置主机业务信息
        setBizRelationInfo(hosts);

        return hosts;
    }

    private void setBizRelationInfo(List<ApplicationHostDTO> hosts) {
        List<Long> hostIds = hosts.stream().map(ApplicationHostDTO::getHostId).collect(Collectors.toList());
        List<HostBizRelationDTO> hostBizRelations = findHostBizRelations(null, hostIds);
        Map<Long, List<HostBizRelationDTO>> hostBizRelationMap =
            hostBizRelations.stream().collect(
                Collectors.groupingBy(HostBizRelationDTO::getHostId));
        // 设置bizId/moduleId/setId
        hosts.forEach(host -> {
            List<HostBizRelationDTO> relations = hostBizRelationMap.get(host.getHostId());
            if (CollectionUtils.isEmpty(relations)) {
                return;
            }
            host.setBizId(relations.get(0).getBizId());
            List<Long> moduleIds = new ArrayList<>();
            List<Long> setIds = new ArrayList<>();
            relations.forEach(relation -> {
                moduleIds.add(relation.getModuleId());
                setIds.add(relation.getSetId());
            });
            host.setModuleId(moduleIds);
            host.setSetId(setIds);
        });
    }

    private Map<Long, List<IpDTO>> groupHostsByCloudAreaId(List<IpDTO> hostIps) {
        return hostIps.stream().collect(Collectors.groupingBy(IpDTO::getCloudAreaId));
    }

    @Override
    public List<CcObjAttributeDTO> getObjAttributeList(String objId) {
        GetObjAttributeReq req = makeBaseReqByWeb(
            GetObjAttributeReq.class, null, defaultUin, defaultSupplierAccount);
        req.setObjId(objId);
        EsbResp<List<CcObjAttributeDTO>> esbResp = requestCmdbApi(HttpPost.METHOD_NAME, GET_OBJ_ATTRIBUTES, req,
            new TypeReference<EsbResp<List<CcObjAttributeDTO>>>() {
            });
        return esbResp.getData();
    }

    @Override
    public Set<String> getAppUsersByRole(Long bizId, String role) {
        CcCountInfo searchResult;
        GetAppReq req = makeBaseReqByWeb(GetAppReq.class, null, defaultUin, defaultSupplierAccount);
        Map<String, Object> condition = new HashMap<>();
        condition.put("bk_biz_id", bizId);
        req.setCondition(condition);
        req.setFields(Collections.singletonList(role));
        EsbResp<CcCountInfo> esbResp = requestCmdbApi(HttpPost.METHOD_NAME, SEARCH_BUSINESS, req,
            new TypeReference<EsbResp<CcCountInfo>>() {
            });
        searchResult = esbResp.getData();

        if (searchResult == null || searchResult.getInfo() == null || searchResult.getInfo().isEmpty()) {
            return Collections.emptySet();
        }
        List<Map<String, Object>> infoList = searchResult.getInfo();
        StringBuilder roleValueStrBuilder = new StringBuilder();
        try {
            //只取了role这一个property的值
            Map<String, Object> props = infoList.get(0);
            props.forEach((key, value) -> {
                    if (key.equals(role)) {
                        roleValueStrBuilder.append((String) value);
                    }
                }
            );
        } catch (Exception ignore) {
        }
        String roleValueStr = roleValueStrBuilder.toString();
        if (roleValueStr.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> userSet = new HashSet<>();
        for (String user : roleValueStr.split(",")) {
            if (StringUtils.isNotBlank(user)) {
                userSet.add(user);
            }
        }

        return userSet;
    }

    @Override
    public List<AppRoleDTO> getAppRoleList() {
        List<CcObjAttributeDTO> esbObjAttributeDTO = getObjAttributeList("biz");
        return esbObjAttributeDTO.stream().filter(it ->
            it.getBkPropertyGroup().equals("role")
        ).map(it -> new AppRoleDTO(
            it.getBkPropertyId(),
            it.getBkPropertyName(),
            it.getCreator())
        ).collect(Collectors.toList());
    }

    @Override
    public List<InstanceTopologyDTO> getTopoInstancePath(GetTopoNodePathReq getTopoNodePathReq) {
        GetTopoNodePathReq req = makeBaseReq(GetTopoNodePathReq.class, defaultUin, defaultSupplierAccount);

        // 由于cmdb传入业务节点(topo根节点)会报错，所以job自己处理
        List<InstanceTopologyDTO> nonAppNodes = new ArrayList<>();
        List<Long> appNodes = new ArrayList<>();
        for (InstanceTopologyDTO topoNode : getTopoNodePathReq.getTopoNodes()) {
            if ("biz".equals(topoNode.getObjectId())) {
                appNodes.add(topoNode.getInstanceId());
            } else {
                nonAppNodes.add(topoNode);
            }
        }

        req.setTopoNodes(nonAppNodes);
        req.setBizId(getTopoNodePathReq.getBizId());

        List<InstanceTopologyDTO> hierarchyTopoList = new ArrayList<>();
        if (!nonAppNodes.isEmpty()) {
            EsbResp<List<TopoNodePathDTO>> esbResp = requestCmdbApi(HttpPost.METHOD_NAME, GET_TOPO_NODE_PATHS,
                req, new TypeReference<EsbResp<List<TopoNodePathDTO>>>() {
                });
            if (esbResp == null || esbResp.getData() == null || esbResp.getData().isEmpty()) {
                return Collections.emptyList();
            }
            List<TopoNodePathDTO> nodePathList = esbResp.getData();

            nodePathList.forEach(nodePath -> {
                InstanceTopologyDTO hierarchyTopo = new InstanceTopologyDTO();
                hierarchyTopo.setObjectId(nodePath.getObjectId());
                hierarchyTopo.setInstanceId(nodePath.getInstanceId());
                hierarchyTopo.setInstanceName(nodePath.getObjectName());
                if (!CollectionUtils.isEmpty(nodePath.getTopoPaths())) {
                    List<InstanceTopologyDTO> parents = nodePath.getTopoPaths().get(0);
                    if (!CollectionUtils.isEmpty(parents)) {
                        Collections.reverse(parents);
                        parents.forEach(hierarchyTopo::addParent);
                    }
                }
                hierarchyTopoList.add(hierarchyTopo);
            });
        }
        if (!appNodes.isEmpty()) {
            appNodes.forEach(bizId -> {
                InstanceTopologyDTO hierarchyTopo = new InstanceTopologyDTO();
                hierarchyTopo.setObjectId("biz");
                hierarchyTopo.setInstanceId(bizId);
                hierarchyTopoList.add(hierarchyTopo);
            });
        }
        return hierarchyTopoList;
    }

    @Override
    public ResourceWatchResult<HostEventDetail> getHostEvents(Long startTime, String cursor) {
        ResourceWatchReq req = makeBaseReqByWeb(
            ResourceWatchReq.class, null, defaultUin, defaultSupplierAccount);
        req.setFields(Arrays.asList("bk_host_id", "bk_host_innerip", "bk_host_name", "bk_os_name", "bk_os_type",
            "bk_cloud_id"));
        req.setResource("host");
        req.setCursor(cursor);
        req.setStartTime(startTime);
        EsbResp<ResourceWatchResult<HostEventDetail>> esbResp = requestCmdbApi(HttpPost.METHOD_NAME, RESOURCE_WATCH,
            req, new TypeReference<EsbResp<ResourceWatchResult<HostEventDetail>>>() {
            }, HttpHelperFactory.getLongRetryableHttpHelper());
        return esbResp.getData();
    }

    @Override
    public ResourceWatchResult<HostRelationEventDetail> getHostRelationEvents(Long startTime, String cursor) {
        ResourceWatchReq req = makeBaseReqByWeb(
            ResourceWatchReq.class, null, defaultUin, defaultSupplierAccount);
        req.setFields(Arrays.asList("bk_host_id", "bk_biz_id", "bk_set_id", "bk_module_id"));
        req.setResource("host_relation");
        req.setCursor(cursor);
        req.setStartTime(startTime);
        EsbResp<ResourceWatchResult<HostRelationEventDetail>> esbResp = requestCmdbApi(HttpPost.METHOD_NAME,
            RESOURCE_WATCH, req, new TypeReference<EsbResp<ResourceWatchResult<HostRelationEventDetail>>>() {
            }, HttpHelperFactory.getLongRetryableHttpHelper());
        return esbResp.getData();
    }

    @Override
    public ResourceWatchResult<BizEventDetail> getAppEvents(Long startTime, String cursor) {
        ResourceWatchReq req = makeBaseReqByWeb(
            ResourceWatchReq.class, null, defaultUin, defaultSupplierAccount);
        req.setFields(Arrays.asList("bk_biz_id", "bk_biz_name", "bk_biz_maintainer", "bk_supplier_account",
            "time_zone", "bk_operate_dept_id", "bk_operate_dept_name", "language"));
        req.setResource("biz");
        req.setCursor(cursor);
        req.setStartTime(startTime);
        EsbResp<ResourceWatchResult<BizEventDetail>> esbResp = requestCmdbApi(HttpPost.METHOD_NAME, RESOURCE_WATCH,
            req, new TypeReference<EsbResp<ResourceWatchResult<BizEventDetail>>>() {
            }, HttpHelperFactory.getLongRetryableHttpHelper());
        return esbResp.getData();
    }

    class FindModuleHostRelationTask implements Runnable {
        //结果队列
        LinkedBlockingQueue<FindModuleHostRelationResult.HostWithModules> resultQueue;
        FindModuleHostRelationReq req;
        String requestId;

        FindModuleHostRelationTask(LinkedBlockingQueue<FindModuleHostRelationResult.HostWithModules> resultQueue,
                                   FindModuleHostRelationReq req, String requestId) {
            this.resultQueue = resultQueue;
            this.req = req;
            this.requestId = requestId;
        }

        @Override
        public void run() {
            JobContextUtil.setRequestId(requestId);
            try {
                resultQueue.addAll(getHostsByReq(req).getRelation());
            } catch (Exception e) {
                log.error("FindModuleHostRelationTask fail:", e);
            }
        }
    }
}
