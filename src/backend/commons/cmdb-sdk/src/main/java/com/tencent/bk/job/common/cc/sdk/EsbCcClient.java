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
import com.tencent.bk.job.common.cc.config.CcConfig;
import com.tencent.bk.job.common.cc.model.AppRoleDTO;
import com.tencent.bk.job.common.cc.model.BaseConditionDTO;
import com.tencent.bk.job.common.cc.model.BriefTopologyDTO;
import com.tencent.bk.job.common.cc.model.BusinessInfoDTO;
import com.tencent.bk.job.common.cc.model.CcCloudAreaInfoDTO;
import com.tencent.bk.job.common.cc.model.CcCloudIdDTO;
import com.tencent.bk.job.common.cc.model.CcDynamicGroupDTO;
import com.tencent.bk.job.common.cc.model.CcGroupDTO;
import com.tencent.bk.job.common.cc.model.CcGroupHostPropDTO;
import com.tencent.bk.job.common.cc.model.CcHostInfoDTO;
import com.tencent.bk.job.common.cc.model.CcInstanceDTO;
import com.tencent.bk.job.common.cc.model.CcObjAttributeDTO;
import com.tencent.bk.job.common.cc.model.ConditionDTO;
import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
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
import com.tencent.bk.job.common.cc.model.req.ListBizHostsTopoReq;
import com.tencent.bk.job.common.cc.model.req.ListHostsWithoutBizReq;
import com.tencent.bk.job.common.cc.model.req.Page;
import com.tencent.bk.job.common.cc.model.req.ResourceWatchReq;
import com.tencent.bk.job.common.cc.model.req.SearchHostDynamicGroupReq;
import com.tencent.bk.job.common.cc.model.req.input.GetHostByIpInput;
import com.tencent.bk.job.common.cc.model.response.CcCountInfo;
import com.tencent.bk.job.common.cc.model.result.AppEventDetail;
import com.tencent.bk.job.common.cc.model.result.ExecuteDynamicGroupHostResult;
import com.tencent.bk.job.common.cc.model.result.FindHostBizRelationsResult;
import com.tencent.bk.job.common.cc.model.result.FindModuleHostRelationResult;
import com.tencent.bk.job.common.cc.model.result.GetBizInternalModuleResult;
import com.tencent.bk.job.common.cc.model.result.HostEventDetail;
import com.tencent.bk.job.common.cc.model.result.HostRelationEventDetail;
import com.tencent.bk.job.common.cc.model.result.ListBizHostResult;
import com.tencent.bk.job.common.cc.model.result.ListBizHostsTopoResult;
import com.tencent.bk.job.common.cc.model.result.ListHostsWithoutBizResult;
import com.tencent.bk.job.common.cc.model.result.ResourceWatchResult;
import com.tencent.bk.job.common.cc.model.result.SearchAppResult;
import com.tencent.bk.job.common.cc.model.result.SearchCloudAreaResult;
import com.tencent.bk.job.common.cc.model.result.SearchDynamicGroupResult;
import com.tencent.bk.job.common.cc.util.TopologyUtil;
import com.tencent.bk.job.common.cc.util.VersionCompatUtil;
import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.config.EsbConfig;
import com.tencent.bk.job.common.esb.model.EsbReq;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.sdk.AbstractEsbSdkClient;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.gse.service.QueryAgentStatusClient;
import com.tencent.bk.job.common.model.dto.ApplicationHostInfoDTO;
import com.tencent.bk.job.common.model.dto.ApplicationInfoDTO;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.common.model.dto.PageDTO;
import com.tencent.bk.job.common.util.FlowController;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.Utils;
import com.tencent.bk.job.common.util.http.AbstractHttpHelper;
import com.tencent.bk.job.common.util.http.LongRetryableHttpHelper;
import com.tencent.bk.job.common.util.http.RetryableHttpHelper;
import com.tencent.bk.job.common.util.json.JsonMapper;
import com.tencent.bk.job.common.util.json.JsonUtils;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.springframework.util.StopWatch;

import java.net.UnknownHostException;
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
 * ESB-CC接口调用客户端
 */
@Slf4j
public class EsbCcClient extends AbstractEsbSdkClient implements CcClient {

    /**
     * CMDB API 处理请求成功
     */
    private static final int RESULT_OK = 0;

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
    private static final JsonMapper JSON_MAPPER = JsonMapper.nonDefaultMapper();
    private static final ConcurrentHashMap<String, Pair<InstanceTopologyDTO, Long>> bizInstTopoMap =
        new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ReentrantLock> bizInstTopoLockMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Pair<InstanceTopologyDTO, Long>> bizInternalTopoMap =
        new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ReentrantLock> bizInternalTopoLockMap = new ConcurrentHashMap<>();
    public static ThreadPoolExecutor threadPoolExecutor = null;
    public static ThreadPoolExecutor longTermThreadPoolExecutor = null;
    public static CcConfig ccConfig = null;
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
    private MeterRegistry meterRegistry;
    private LoadingCache<String, InstanceTopologyDTO> bizInstCompleteTopologyCache = CacheBuilder.newBuilder()
        .maximumSize(1000).expireAfterWrite(30, TimeUnit.SECONDS).
            build(new CacheLoader<String, InstanceTopologyDTO>() {
                      @Override
                      public InstanceTopologyDTO load(String searchKey) throws Exception {
                          String[] keys = searchKey.split(":");
                          return getBizInstCompleteTopology(Long.parseLong(keys[0]), keys[1], keys[2]);
                      }
                  }
            );

    public EsbCcClient(EsbConfig esbConfig, CcConfig ccConfig, QueryAgentStatusClient queryAgentStatusClient,
                       MeterRegistry meterRegistry) {
        this(esbConfig, ccConfig, null, queryAgentStatusClient, meterRegistry);
    }

    public EsbCcClient(EsbConfig esbConfig, CcConfig ccConfig, String lang,
                       QueryAgentStatusClient queryAgentStatusClient, MeterRegistry meterRegistry) {
        super(esbConfig.getEsbUrl(), esbConfig.getAppCode(), esbConfig.getAppSecret(), lang,
            esbConfig.isUseEsbTestEnv());
        this.defaultSupplierAccount = ccConfig.getDefaultSupplierAccount();
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
        initThreadPoolExecutor(ccConfig.getCmdbQueryThreadsNum(), ccConfig.getFindHostRelationLongTermConcurrency());
    }

    private static ServiceException caughtException(Exception e, int code) {
        if (e instanceof UnknownHostException) {
            code = ErrorCode.CMDB_UNREACHABLE_SERVER;
        }
        return new ServiceException(code);
    }

    public static void setCcConfig(CcConfig ccConfig) {
        EsbCcClient.ccConfig = ccConfig;
    }

    public void setQueryAgentStatusClient(QueryAgentStatusClient queryAgentStatusClient) {
        this.queryAgentStatusClient = queryAgentStatusClient;
    }

    private String getKey(InstanceTopologyDTO node) {
        return node.getObjectId() + "_" + node.getInstanceId();
    }

    /**
     * 将低一级的子节点合并入拓扑树中
     *
     * @param tp
     * @param child
     * @return
     */
    private InstanceTopologyDTO mergeChildIntoTopology(InstanceTopologyDTO tp, InstanceTopologyDTO child) {
        if (child == null) {
            return tp;
        }
        Set<String> tpChildSet = tp.getChild().stream().map(this::getKey).collect(Collectors.toSet());
        if (tpChildSet.contains(getKey(child))) {
            //找到tp1中这个child进行合并更新
            for (int i = 0; i < tp.getChild().size(); i++) {
                if (tp.getChild().get(i).getInstanceId().equals(child.getInstanceId())) {
                    tp.getChild().set(i, mergeTopology(tp.getChild().get(i), child));
                    break;
                }
            }
        } else {
            tp.getChild().add(child);
        }
        return tp;
    }

    /**
     * 合并两颗层级相差小于2的拓扑树
     *
     * @param topologyDTOs
     * @return
     */
    private InstanceTopologyDTO mergeTopology(InstanceTopologyDTO... topologyDTOs) {
        try {
            return mergeTopologyIndeed(topologyDTOs);
        } catch (Exception e) {
            for (int i = 0; i < topologyDTOs.length; i++) {
                InstanceTopologyDTO topologyDTO = topologyDTOs[i];
                TopologyUtil.printTopo(topologyDTO);
                log.info("==============================");
            }
            throw new RuntimeException("fail to mergeTopology", e);
        }
    }

    private InstanceTopologyDTO mergeTopologyIndeed(InstanceTopologyDTO... topologyDTOs) {
        Map<String, Integer> weightMap = new HashMap<>();
        weightMap.put("biz", 1);
        weightMap.put("set", 2);
        weightMap.put("module", 3);
        if (topologyDTOs.length == 1) {
            return topologyDTOs[0];
        } else if (topologyDTOs.length == 2) {
            InstanceTopologyDTO tp1 = topologyDTOs[0];
            InstanceTopologyDTO tp2 = topologyDTOs[1];
            if (tp1 == null) {
                return tp2;
            }
            if (tp2 == null) {
                return tp1;
            }
            //根节点层级相同
            if (tp1.getObjectId().equals(tp2.getObjectId())) {
                //但实例不同，无法合并
                if (!tp1.getInstanceId().equals(tp2.getInstanceId())) {
                    throw new RuntimeException("can not merge differnt instances of same level");
                } else {
                    if (tp2.getChild() == null || tp2.getChild().isEmpty()) {
                        return tp1;
                    } else {
                        for (InstanceTopologyDTO child2 : tp2.getChild()) {
                            tp1 = mergeChildIntoTopology(tp1, child2);
                        }
                        return tp1;
                    }
                }
            } else if (Math.abs(weightMap.get(tp1.getObjectId()) - weightMap.get(tp2.getObjectId())) >= 2) {
                //相差两级及以上无法合并
                throw new RuntimeException("can not merge differnt instances beyond 2 levels");
            } else {
                //根节点相差一级
                if (weightMap.get(tp1.getObjectId()) < weightMap.get(tp2.getObjectId())) {
                    //tp2往tp1中合入
                    tp1 = mergeChildIntoTopology(tp1, tp2);
                    return tp1;
                } else {
                    //交换顺序再合并
                    return mergeTopology(tp2, tp1);
                }
            }
        } else {
            return mergeTopology(mergeTopology(Arrays.copyOf(topologyDTOs, topologyDTOs.length - 1)),
                topologyDTOs[topologyDTOs.length - 1]);
        }
    }

    /**
     * 获取业务完整topo（含内置模块空闲机/故障机等）
     *
     * @param appId
     * @param uin
     * @param owner
     * @return
     * @throws ServiceException
     */
    @Override
    public InstanceTopologyDTO getBizInstCompleteTopology(long appId, String owner,
                                                          String uin) throws ServiceException {
        InstanceTopologyDTO completeTopologyDTO;
        if (ccConfig.getEnableInterfaceBriefCacheTopo()) {
            log.debug("getBriefCacheTopo({},{},{})", appId, owner, uin);
            completeTopologyDTO = getBriefCacheTopo(appId, owner, uin);
        } else {
            InstanceTopologyDTO topologyDTO = getBizInstTopologyWithoutInternalTopo(appId, owner, uin);
            InstanceTopologyDTO internalTopologyDTO = getBizInternalModule(appId, owner, uin);
            completeTopologyDTO = mergeTopology(topologyDTO, internalTopologyDTO);
        }
        return completeTopologyDTO;
    }

    public InstanceTopologyDTO getCachedBizInstCompleteTopology(long appId, String owner,
                                                                String uin) throws ServiceException {
        try {
            return bizInstCompleteTopologyCache.get("" + appId + ":" + owner + ":" + uin);
        } catch (ExecutionException e) {
            throw new ServiceException(e, ErrorCode.CMDB_API_DATA_ERROR, "Fail to get " +
                "getCachedBizInstCompleteTopology");
        }
    }

    @Override
    public InstanceTopologyDTO getBizInstTopology(long appId, String owner, String uin) throws ServiceException {
        return getCachedBizInstCompleteTopology(appId, owner, uin);
    }

    public InstanceTopologyDTO getBizInstTopologyWithoutInternalTopo(long appId, String owner,
                                                                     String uin) throws ServiceException {
        if (ccConfig.getEnableLockOptimize()) {
            return getBizInstTopologyWithoutInternalTopoWithLock(appId, owner, uin);
        } else {
            return getBizInstTopologyWithoutInternalTopoFromCMDB(appId, owner, uin);
        }
    }

    /**
     * 防止参数完全相同的请求在并发时多次请求CMDB，降低对CMDB的请求量
     *
     * @param appId
     * @param owner
     * @param uin
     * @return
     * @throws ServiceException
     */
    public InstanceTopologyDTO getBizInstTopologyWithoutInternalTopoWithLock(long appId, String owner,
                                                                             String uin) throws ServiceException {
        String key = "" + appId + ":" + owner + ":" + uin;
        ReentrantLock lock = null;
        if (bizInstTopoMap.containsKey(key)
            && bizInstTopoMap.get(key).getRight() > System.currentTimeMillis() - 30 * 1000) {
            return bizInstTopoMap.get(key).getLeft();
        } else {
            lock = bizInstTopoLockMap.computeIfAbsent(key, s -> new ReentrantLock());
            lock.lock();
            try {
                if (bizInstTopoMap.containsKey(key)
                    && bizInstTopoMap.get(key).getRight() > System.currentTimeMillis() - 30 * 1000) {
                    return bizInstTopoMap.get(key).getLeft();
                } else {
                    InstanceTopologyDTO topo = getBizInstTopologyWithoutInternalTopoFromCMDB(appId, owner, uin);
                    bizInstTopoMap.put(key, Pair.of(topo, System.currentTimeMillis()));
                    return topo;
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public InstanceTopologyDTO getBriefCacheTopo(long appId, String owner, String uin) throws ServiceException {
        try {
            if (owner == null) {
                owner = defaultSupplierAccount;
            }
            if (uin == null) {
                uin = defaultUin;
            }
            GetBriefCacheTopoReq req = makeBaseReq(GetBriefCacheTopoReq.class, uin, owner);
            req.setAppId(appId);
            EsbResp<BriefTopologyDTO> esbResp = getEsbRespByReq(HttpGet.METHOD_NAME, GET_BIZ_BRIEF_CACHE_TOPO, req,
                new TypeReference<EsbResp<BriefTopologyDTO>>() {
                });
            return TopologyUtil.convert(esbResp.getData());
        } catch (Exception e) {
            log.error("{}|{}", ErrorCode.CMDB_API_DATA_ERROR, GET_BIZ_BRIEF_CACHE_TOPO, e);
            throw caughtException(e, ErrorCode.CMDB_API_DATA_ERROR);
        }
    }

    public InstanceTopologyDTO getBizInstTopologyWithoutInternalTopoFromCMDB(long appId, String owner,
                                                                             String uin) throws ServiceException {
        try {
            if (owner == null) {
                owner = defaultSupplierAccount;
            }
            if (uin == null) {
                uin = defaultUin;
            }
            GetBizInstTopoReq req = makeBaseReq(GetBizInstTopoReq.class, uin, owner);
            req.setAppId(appId);
            EsbResp<List<InstanceTopologyDTO>> esbResp = getEsbRespByReq(HttpGet.METHOD_NAME, SEARCH_BIZ_INST_TOPO,
                req, new TypeReference<EsbResp<List<InstanceTopologyDTO>>>() {
                });
            if (esbResp.getData().size() > 0) {
                return esbResp.getData().get(0);
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("{}|{}", ErrorCode.CMDB_API_DATA_ERROR, SEARCH_BIZ_INST_TOPO, e);
            throw caughtException(e, ErrorCode.CMDB_API_DATA_ERROR);
        }
    }

    public <T, R> R getEsbRespByReq(String method, String uri, EsbReq reqBody,
                                    TypeReference<R> typeReference) throws RuntimeException {
        if (ccConfig.getEnableInterfaceRetry()) {
            log.debug("using RetryableHttpHelper");
            return getEsbRespByReq(method, uri, reqBody, typeReference, new RetryableHttpHelper());
        } else {
            log.debug("using DefaultHttpHelper");
            return getEsbRespByReq(method, uri, reqBody, typeReference, null);
        }
    }

    public <T, R> R getEsbRespByReq(String method, String uri, EsbReq reqBody, TypeReference<R> typeReference,
                                    AbstractHttpHelper httpHelper) throws RuntimeException {
        if (ccConfig != null && ccConfig.getEnableFlowControl()) {
            if (globalFlowController != null) {
                String resourceId = uri;
                if (interfaceNameMap.containsKey(uri)) {
                    resourceId = interfaceNameMap.get(uri);
                }
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
        String reqStr = JsonUtils.toJsonWithoutSkippedFields(reqBody);
        String respStr = null;
        long start = System.nanoTime();
        String status = "none";
        try {
            if (method.equals(HttpGet.METHOD_NAME)) {
                respStr = doHttpGet(uri, reqBody, httpHelper);
            } else if (method.equals(HttpPost.METHOD_NAME)) {
                respStr = doHttpPost(uri, reqBody, httpHelper);
            }
            if (StringUtils.isBlank(respStr)) {
                log.error("fail:response is blank|method={}|uri={}|reqStr={}", method, uri, reqStr);
                throw new ServiceException(ErrorCode.CMDB_API_DATA_ERROR, "response is blank");
            } else {
                log.debug("success|method={}|uri={}|reqStr={}|respStr={}", method, uri, reqStr, respStr);
            }
            R result =
                JSON_MAPPER.fromJson(respStr, typeReference);
            EsbResp esbResp = (EsbResp) result;
            if (esbResp == null) {
                log.error("fail:esbResp is null after parse|method={}|uri={}|reqStr={}|respStr={}", method, uri,
                    reqStr, respStr);
                status = "error";
                throw new ServiceException(ErrorCode.CMDB_API_DATA_ERROR, "esbResp is null after parse");
            } else if (esbResp.getCode() != RESULT_OK) {
                log.error(
                    "fail:esbResp code!=0|esbResp.requestId={}|esbResp.code={}|esbResp" +
                        ".message={}|method={}|uri={}|reqStr={}|respStr={}"
                    , esbResp.getRequestId()
                    , esbResp.getCode()
                    , esbResp.getMessage()
                    , method, uri, reqStr, respStr
                );
                status = "error";
                throw new ServiceException(ErrorCode.CMDB_API_DATA_ERROR, "esbResp code!=0");
            }
            if (esbResp.getData() == null) {
                log.warn(
                    "warn:esbResp.getData() == null|esbResp.requestId={}|esbResp.code={}|esbResp" +
                        ".message={}|method={}|uri={}|reqStr={}|respStr={}"
                    , esbResp.getRequestId()
                    , esbResp.getCode()
                    , esbResp.getMessage()
                    , method, uri, reqStr, respStr
                );
            }
            status = "ok";
            return result;
        } catch (Exception e) {
            String errorMsg = "Fail to request CMDB data|method=" + method + "|uri=" + uri + "|reqStr=" + reqStr;
            log.error(errorMsg, e);
            status = "error";
            throw new ServiceException(ErrorCode.CMDB_API_DATA_ERROR, "Fail to request CMDB data");
        } finally {
            long end = System.nanoTime();
            meterRegistry.timer("cmdb.api", "api_name", uri, "status", status)
                .record(end - start, TimeUnit.NANOSECONDS);
        }
    }

    /**
     * 查询业务内置模块（空闲机/故障机等）
     *
     * @param appId
     * @param owner
     * @param uin
     * @return 只有一个Set，根节点为Set的Topo树
     * @throws ServiceException
     */
    @Override
    public InstanceTopologyDTO getBizInternalModule(long appId, String owner, String uin) throws ServiceException {
        if (ccConfig.getEnableLockOptimize()) {
            return getBizInternalModuleWithLock(appId, owner, uin);
        } else {
            return getBizInternalModuleFromCMDB(appId, owner, uin);
        }
    }

    /**
     * 防止参数完全相同的请求在并发时多次请求CMDB，降低对CMDB的请求量
     *
     * @param appId
     * @param owner
     * @param uin
     * @return
     * @throws ServiceException
     */
    public InstanceTopologyDTO getBizInternalModuleWithLock(long appId, String owner,
                                                            String uin) throws ServiceException {
        String key = "" + appId + ":" + owner + ":" + uin;
        ReentrantLock lock = null;
        if (bizInternalTopoMap.containsKey(key)
            && bizInternalTopoMap.get(key).getRight() > System.currentTimeMillis() - 30 * 1000) {
            return bizInternalTopoMap.get(key).getLeft();
        } else {
            lock = bizInternalTopoLockMap.computeIfAbsent(key, s -> new ReentrantLock());
            lock.lock();
            try {
                if (bizInternalTopoMap.containsKey(key)
                    && bizInternalTopoMap.get(key).getRight() > System.currentTimeMillis() - 30 * 1000) {
                    return bizInternalTopoMap.get(key).getLeft();
                } else {
                    InstanceTopologyDTO topo = getBizInternalModuleFromCMDB(appId, owner, uin);
                    bizInternalTopoMap.put(key, Pair.of(topo, System.currentTimeMillis()));
                    return topo;
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public InstanceTopologyDTO getBizInternalModuleFromCMDB(long appId, String owner,
                                                            String uin) throws ServiceException {
        try {
            if (owner == null) {
                owner = defaultSupplierAccount;
            }
            if (uin == null) {
                uin = defaultUin;
            }
            GetBizInternalModuleReq req = makeBaseReq(GetBizInternalModuleReq.class, uin, owner);
            req.setAppId(appId);
            EsbResp<GetBizInternalModuleResult> esbResp = getEsbRespByReq(HttpGet.METHOD_NAME,
                GET_BIZ_INTERNAL_MODULE, req, new TypeReference<EsbResp<GetBizInternalModuleResult>>() {
                });
            GetBizInternalModuleResult setInfo = esbResp.getData();
            //将结果转换为Topo树
            InstanceTopologyDTO instanceTopologyDTO = new InstanceTopologyDTO();
            instanceTopologyDTO.setObjectId("set");
            instanceTopologyDTO.setObjectName("Set");
            instanceTopologyDTO.setInstanceId(setInfo.getSetId());
            instanceTopologyDTO.setInstanceName(setInfo.getSetName());
            List<InstanceTopologyDTO> childList = new ArrayList<>();
            List<GetBizInternalModuleResult.Module> modules = setInfo.getModule();
            if (modules != null && !modules.isEmpty()) {
                modules.forEach(module -> {
                    InstanceTopologyDTO childModule = new InstanceTopologyDTO();
                    childModule.setObjectId("module");
                    childModule.setObjectName("Module");
                    childModule.setInstanceId(module.getModuleId());
                    childModule.setInstanceName(module.getModuleName());
                    childList.add(childModule);
                });
            }
            instanceTopologyDTO.setChild(childList);
            return instanceTopologyDTO;
        } catch (Exception e) {
            log.error("{}|{}", ErrorCode.CMDB_API_DATA_ERROR, GET_BIZ_INTERNAL_MODULE, e);
            throw caughtException(e, ErrorCode.CMDB_API_DATA_ERROR);
        }
    }


    @Override
    public List<ApplicationHostInfoDTO> getHosts(long appId, List<CcInstanceDTO> ccInstList) {
        return getHostsByTopology(appId, ccInstList);
    }

    @Override
    public List<ApplicationHostInfoDTO> getHostsByTopology(long appId, List<CcInstanceDTO> ccInstList) {
        StopWatch watch = new StopWatch("getHostsByTopology");
        watch.start("getCachedBizInstCompleteTopology");
        InstanceTopologyDTO appCompleteTopology = getCachedBizInstCompleteTopology(appId, defaultSupplierAccount,
            defaultUin);
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
        List<ApplicationHostInfoDTO> applicationHostInfoDTOList = findHostByModule(appId,
            new ArrayList<>(moduleIdSet), defaultUin, defaultSupplierAccount);
        watch.stop();

        if (watch.getTotalTimeMillis() > 1000L) {
            log.warn("Get hosts by topo is slow, appId: {}, ccInsts: {}, watchInfo: {}", appId, ccInstList,
                watch.prettyPrint());
        }
        return applicationHostInfoDTOList;
    }

    @Override
    public List<ApplicationHostInfoDTO> findHostByModule(long appId, List<Long> moduleIdList, String uin,
                                                         String owner) {
        //moduleId分批
        List<ApplicationHostInfoDTO> resultList = new ArrayList<>();
        int batchSize = 200;
        int start = 0;
        int end = start + batchSize;
        int moduleIdSize = moduleIdList.size();
        end = Math.min(end, moduleIdSize);
        do {
            List<Long> moduleIdSubList = moduleIdList.subList(start, end);
            if (moduleIdSubList.size() > 0) {
                // 使用find_module_host_relation接口
                resultList.addAll(findModuleHostRelationConcurrently(appId, moduleIdSubList, uin, owner));
            }
            start += batchSize;
            end = start + batchSize;
            end = Math.min(end, moduleIdSize);
        } while (start < moduleIdSize);
        return resultList;
    }

    private FindModuleHostRelationResult getHostsByReq(FindModuleHostRelationReq req) {
        EsbResp<FindModuleHostRelationResult> esbResp = getEsbRespByReq(HttpPost.METHOD_NAME,
            FIND_MODULE_HOST_RELATION, req, new TypeReference<EsbResp<FindModuleHostRelationResult>>() {
            });
        return esbResp.getData();
    }

    private FindModuleHostRelationReq genFindModuleHostRelationReq(long appId, List<Long> moduleIdList, int start,
                                                                   int limit, String uin, String owner) {
        FindModuleHostRelationReq req = makeBaseReq(FindModuleHostRelationReq.class, uin, owner);
        req.setAppId(appId);
        req.setModuleIdList(moduleIdList);
        Page page = new Page(start, limit);
        req.setPage(page);
        return req;
    }

    /**
     * 并发：按模块加载主机
     *
     * @param appId
     * @param moduleIdList
     * @param uin
     * @param owner
     * @return
     */
    public List<ApplicationHostInfoDTO> findModuleHostRelationConcurrently(long appId, List<Long> moduleIdList,
                                                                           String uin, String owner) {
        if (moduleIdList == null || moduleIdList.isEmpty()) {
            return Collections.emptyList();
        }
        owner = defaultSupplierAccount;
        uin = defaultUin;
        LinkedBlockingQueue<FindModuleHostRelationResult.HostWithModules> resultQueue = new LinkedBlockingQueue<>();

        List<ApplicationHostInfoDTO> applicationHostInfoDTOList;
        int start = 0;
        //已调优
        int limit = 500;
        FindModuleHostRelationReq req = genFindModuleHostRelationReq(appId, moduleIdList, start, limit, uin, owner);
        //先拉一次获取总数
        FindModuleHostRelationResult pageData = getHostsByReq(req);
        List<FindModuleHostRelationResult.HostWithModules> hostWithModulesList = pageData.getRelation();
        resultQueue.addAll(hostWithModulesList);
        // 如果该页未达到limit，说明是最后一页
        if (pageData.getCount() <= limit) {
            log.info("appId {}:{} hosts in total, {} hosts indeed", appId, pageData.getCount(), resultQueue.size());
        } else if (pageData.getCount() > limit && hostWithModulesList.size() <= limit) {
            int totalCount = pageData.getCount() - limit;
            List<Future> futures = new ArrayList<>();
            Long startTime = System.currentTimeMillis();
            while (totalCount > 0) {
                start += limit;
                FindModuleHostRelationTask task = new FindModuleHostRelationTask(resultQueue,
                    genFindModuleHostRelationReq(appId, moduleIdList, start, limit, uin, owner),
                    JobContextUtil.getRequestId());
                if (totalCount > 10000) {
                    //主机数太多，防止将CMDB拉挂了
                    Future future = longTermThreadPoolExecutor.submit(task);
                    futures.add(future);
                } else {
                    // 默认采用多个并发线程拉取
                    Future future = threadPoolExecutor.submit(task);
                    futures.add(future);
                }
                totalCount -= limit;
            }
            futures.forEach(it -> {
                try {
                    while (!it.isDone()) {
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    log.warn("sleep interrupted", e);
                }
            });
            Long endTime = System.currentTimeMillis();
            log.info("find module hosts concurrently time consuming:" + (endTime - startTime));
        } else {
            //limit参数不起效，可能拉到了全量数据，直接跳出
            log.warn("appId {}:{} hosts in total, {} hosts indeed, CMDB interface params invalid", appId,
                pageData.getCount(), resultQueue.size());
        }
        Long startTime = System.currentTimeMillis();
        applicationHostInfoDTOList = convertToHostInfoDTOList(appId, new ArrayList<>(resultQueue));
        Long endTime = System.currentTimeMillis();
        long timeConsuming = endTime - startTime;
        if (timeConsuming >= 1000) {
            log.info("convertToHostInfoDTOList time consuming:" + timeConsuming);
        }
        return applicationHostInfoDTOList;
    }

    private void fillAgentInfo(
        ApplicationHostInfoDTO applicationHostInfoDTO,
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
                    applicationHostInfoDTO.setGseAgentAlive(pair.getRight());
                    if (ipWithCloudId.contains(":")) {
                        String[] arr = ipWithCloudId.split(":");
                        applicationHostInfoDTO.setCloudAreaId(Long.parseLong(arr[0]));
                        applicationHostInfoDTO.setIp(arr[1]);
                    } else {
                        applicationHostInfoDTO.setIp(ipWithCloudId);
                    }
                } else {
                    log.warn("Fail to get agentStatus, host={}", JsonUtils.toJson(host));
                }
            } else {
                applicationHostInfoDTO.setGseAgentAlive(false);
                applicationHostInfoDTO.setCloudAreaId(host.getCloudAreaId());
                applicationHostInfoDTO.setIp(multiIp);
            }
        } else {
            log.warn("queryAgentStatusClient==null, please check!");
            List<String> ipList = Utils.getNotBlankSplitList(multiIp, ",");
            if (ipList.size() > 0) {
                applicationHostInfoDTO.setIp(ipList.get(0));
            } else {
                log.warn("no available ip after queryAgentStatusClient");
            }
        }
    }

    private ApplicationHostInfoDTO convertToHostInfoDTO(
        Long appId,
        FindModuleHostRelationResult.HostWithModules hostWithModules
    ) {
        FindModuleHostRelationResult.HostProp host = hostWithModules.getHost();
        String multiIp = host.getIp();
        if (multiIp != null) {
            multiIp = multiIp.trim();
        } else {
            log.warn("multiIp is null, appId={}, host={}", appId, hostWithModules);
        }
        //包装为ApplicationHostInfoDTO
        ApplicationHostInfoDTO applicationHostInfoDTO = new ApplicationHostInfoDTO();
        applicationHostInfoDTO.setAppId(appId);
        applicationHostInfoDTO.setDisplayIp(multiIp);
        applicationHostInfoDTO.setCloudAreaId(host.getCloudAreaId());
        applicationHostInfoDTO.setHostId(host.getHostId());
        fillAgentInfo(applicationHostInfoDTO, host);
        List<FindModuleHostRelationResult.ModuleProp> modules = hostWithModules.getModules();
        for (FindModuleHostRelationResult.ModuleProp module : modules) {
            if (module == null || null == module.getModuleId()) {
                log.warn("invalid host:" + JsonUtils.toJson(applicationHostInfoDTO));
            }
        }
        List<FindModuleHostRelationResult.ModuleProp> validModules =
            hostWithModules.getModules().stream().filter(Objects::nonNull).collect(Collectors.toList());
        applicationHostInfoDTO.setModuleId(
            validModules.stream()
                .map(FindModuleHostRelationResult.ModuleProp::getModuleId)
                .collect(Collectors.toList()));
        applicationHostInfoDTO.setSetId(
            validModules.stream()
                .map(FindModuleHostRelationResult.ModuleProp::getSetId)
                .collect(Collectors.toList()));
        applicationHostInfoDTO.setModuleType(validModules.stream().map(it -> {
            try {
                return Long.parseLong(it.getModuleType());
            } catch (Exception e) {
                return 0L;
            }
        }).collect(Collectors.toList()));
        applicationHostInfoDTO.setIpDesc(host.getHostName());
        String os = host.getOs();
        if (os != null && os.length() > 512) {
            applicationHostInfoDTO.setOs(os.substring(0, 512));
        } else {
            applicationHostInfoDTO.setOs(os);
        }
        applicationHostInfoDTO.setOsType(host.getOsType());
        return applicationHostInfoDTO;
    }

    private List<ApplicationHostInfoDTO> convertToHostInfoDTOList(
        long appId,
        List<FindModuleHostRelationResult.HostWithModules> hostWithModulesList) {
        List<ApplicationHostInfoDTO> applicationHostInfoDTOList = new ArrayList<>();
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
                ApplicationHostInfoDTO applicationHostInfoDTO = convertToHostInfoDTO(appId, hostWithModules);
                applicationHostInfoDTOList.add(applicationHostInfoDTO);
            } else {
                log.info("bk_host_innerip is blank, ignore, hostId={},host={}", host.getHostId(),
                    JsonUtils.toJson(host));
            }
        }
        log.info("ipSet.size=" + ipSet.size());
        return applicationHostInfoDTOList;
    }

    @Override
    public List<ListBizHostsTopoResult.HostInfo> listBizHostsTopo(long appId, String uin, String owner) {
        try {
            if (StringUtils.isBlank(owner)) {
                owner = defaultSupplierAccount;
            }
            if (StringUtils.isBlank(uin)) {
                uin = defaultUin;
            }
            List<ListBizHostsTopoResult.HostInfo> hostInfoList = new ArrayList<>();
            ListBizHostsTopoReq req = makeBaseReq(ListBizHostsTopoReq.class, uin, owner);
            req.setAppId(appId);

            int limit = 200;
            int start = 0;
            boolean isLastPage = false;
            while (!isLastPage) {
                Page page = new Page(start, limit);
                req.setPage(page);
                EsbResp<ListBizHostsTopoResult> esbResp = getEsbRespByReq(HttpPost.METHOD_NAME, LIST_BIZ_HOSTS_TOPO,
                    req, new TypeReference<EsbResp<ListBizHostsTopoResult>>() {
                    });
                ListBizHostsTopoResult pageData = esbResp.getData();
                hostInfoList.addAll(pageData.getInfo());
                // 如果该页未达到limit，说明是最后一页
                if (pageData.getInfo().size() < limit) {
                    isLastPage = true;
                } else {
                    start += limit;
                }
            }
            Set<String> ipSet = new HashSet<>();
            hostInfoList.forEach(hostInfo -> {
                ipSet.add(hostInfo.getHost().getCloudId() + ":" + hostInfo.getHost().getIp());
            });
            log.info("ipSet.size=" + ipSet.size());
            return hostInfoList;
        } catch (Exception e) {
            log.error("{}|{}", ErrorCode.CMDB_API_DATA_ERROR, LIST_BIZ_HOSTS_TOPO, e);
            throw caughtException(e, ErrorCode.CMDB_API_DATA_ERROR);
        }
    }

    private ApplicationHostInfoDTO convertHost(long appId, CcHostInfoDTO hostInfo) {
        ApplicationHostInfoDTO ipInfo = new ApplicationHostInfoDTO();
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
        ipInfo.setAppId(appId);
        ipInfo.setIpDesc(hostInfo.getHostName());

        return ipInfo;
    }

    /**
     * 从CC获取所有业务信息
     *
     * @return
     */
    @Override
    public List<ApplicationInfoDTO> getAllApps() {
        try {
            List<ApplicationInfoDTO> appList = new ArrayList<>();
            int limit = 200;
            int start = 0;
            boolean isLastPage = false;
            String orderField = "bk_biz_id";
            while (!isLastPage) {
                String owner = defaultSupplierAccount;
                GetAppReq req = makeBaseReq(GetAppReq.class, defaultUin, owner);
                PageDTO page = new PageDTO(start, limit, orderField);
                req.setPage(page);
                EsbResp<SearchAppResult> esbResp = getEsbRespByReq(HttpPost.METHOD_NAME, SEARCH_BUSINESS, req,
                    new TypeReference<EsbResp<SearchAppResult>>() {
                    });
                SearchAppResult data = esbResp.getData();
                if (data == null) {
                    appList.clear();
                    throw new ServiceException(ErrorCode.CMDB_API_DATA_ERROR, "data is null");
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
        } catch (Exception e) {
            log.error("{}|{}", ErrorCode.CMDB_API_DATA_ERROR, SEARCH_BUSINESS, e);
            throw caughtException(e, ErrorCode.CMDB_API_DATA_ERROR);
        }
    }

    private ApplicationInfoDTO convertToAppInfo(BusinessInfoDTO businessInfo) {
        ApplicationInfoDTO appInfo = new ApplicationInfoDTO();
        appInfo.setId(businessInfo.getAppId());
        appInfo.setName(businessInfo.getAppName());
        appInfo.setMaintainers(VersionCompatUtil.convertMaintainers(businessInfo.getMaintainers()));
        appInfo.setTimeZone(businessInfo.getTimezone());
        appInfo.setBkSupplierAccount(businessInfo.getSupplierAccount());
        appInfo.setAppType(AppTypeEnum.NORMAL);
        appInfo.setOperateDeptId(businessInfo.getOperateDeptId());
        appInfo.setLanguage(businessInfo.getLanguage());
        return appInfo;
    }

    /**
     * 获取用户的业务
     *
     * @param uin
     * @param owner
     * @return
     * @throws ServiceException
     */
    @Override
    public List<ApplicationInfoDTO> getAppByUser(String uin, String owner) throws ServiceException {
        try {
            List<ApplicationInfoDTO> appList = new ArrayList<>();
            boolean isLastPage = false;
            int limit = 200;
            int start = 0;
            String orderField = "bk_biz_id";
            while (!isLastPage) {
                GetAppReq req = makeBaseReq(GetAppReq.class, uin, owner);
                PageDTO page = new PageDTO(start, limit, orderField);
                req.setPage(page);
                Map<String, Object> conditionMap = new HashMap<>();
                Map<String, Object> filedPattern = new HashMap<>();
                filedPattern.put("$regex", uin);
                conditionMap.put("bk_biz_maintainer", filedPattern);
                req.setCondition(conditionMap);
                EsbResp<SearchAppResult> esbResp = getEsbRespByReq(HttpPost.METHOD_NAME, SEARCH_BUSINESS, req,
                    new TypeReference<EsbResp<SearchAppResult>>() {
                    });
                SearchAppResult data = esbResp.getData();
                if (data == null) {
                    appList.clear();
                    throw new ServiceException(ErrorCode.CMDB_API_DATA_ERROR, "data is null");
                }
                List<BusinessInfoDTO> businessInfos = data.getInfo();
                if (businessInfos != null && !businessInfos.isEmpty()) {
                    for (BusinessInfoDTO businessInfo : businessInfos) {
                        if (businessInfo.getDefaultApp() == 0) {
                            appList.add(convertToAppInfo(businessInfo));
                        }
                        start++;
                    }
                }
                // 如果该页未达到limit，说明是最后一页
                if (businessInfos == null || businessInfos.size() < limit) {
                    isLastPage = true;
                }
            }
            return appList;
        } catch (Exception e) {
            log.error("{}|{}", ErrorCode.CMDB_API_DATA_ERROR, SEARCH_BUSINESS, e);
            throw caughtException(e, ErrorCode.CMDB_API_DATA_ERROR);
        }
    }

    /**
     * 获取业务详细信息
     *
     * @param uin
     * @param appId
     * @param owner
     * @return
     */
    @Override
    public ApplicationInfoDTO getAppById(long appId, String owner, String uin) {
        try {
            owner = defaultSupplierAccount;
            uin = defaultUin;
            GetAppReq req = makeBaseReq(GetAppReq.class, uin, owner);
            Map<String, Object> conditionMap = new HashMap<>();
            conditionMap.put("bk_biz_id", appId);
            req.setCondition(conditionMap);
            EsbResp<SearchAppResult> esbResp = getEsbRespByReq(HttpPost.METHOD_NAME, SEARCH_BUSINESS, req,
                new TypeReference<EsbResp<SearchAppResult>>() {
                });
            SearchAppResult data = esbResp.getData();
            if (data == null) {
                throw new ServiceException(ErrorCode.CMDB_API_DATA_ERROR, "data == null");
            }
            List<BusinessInfoDTO> businessInfos = data.getInfo();
            if (businessInfos == null || businessInfos.isEmpty()) {
                throw new ServiceException(ErrorCode.CMDB_API_DATA_ERROR, "businessInfos == null || businessInfos" +
                    ".isEmpty()");
            }
            return convertToAppInfo(businessInfos.get(0));
        } catch (Exception e) {
            log.error("{}|{}", ErrorCode.CMDB_API_DATA_ERROR, SEARCH_BUSINESS, e);
            throw caughtException(e, ErrorCode.CMDB_API_DATA_ERROR);
        }
    }

    /**
     * 查询自定义分组
     *
     * @param owner
     * @param uin
     * @param appId
     * @return
     */
    @Override
    public List<CcGroupDTO> getCustomGroupList(long appId, String owner, String uin) {
        // Job只需要目标资源为主机类型的动态分组
        return getHostDynamicGroupListFromCMDB(appId, owner, uin);
    }

    public List<CcGroupDTO> getHostDynamicGroupListFromCMDB(long appId, String owner, String uin) {
        try {
            owner = defaultSupplierAccount;
            uin = defaultUin;
            SearchHostDynamicGroupReq req = makeBaseReq(SearchHostDynamicGroupReq.class, uin, owner);
            req.setAppId(appId);
            int start = 0;
            int limit = 200;
            req.getPage().setStart(start);
            req.getPage().setLimit(limit);
            List<CcDynamicGroupDTO> ccDynamicGroupList = new ArrayList<>();
            boolean isLastPage = false;
            while (!isLastPage) {
                req.getPage().setStart(start);

                EsbResp<SearchDynamicGroupResult> esbResp = getEsbRespByReq(HttpPost.METHOD_NAME,
                    SEARCH_DYNAMIC_GROUP, req, new TypeReference<EsbResp<SearchDynamicGroupResult>>() {
                    });
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
            List<CcGroupDTO> ccGroupDTOList =
                ccDynamicGroupList.parallelStream().map(this::convertToCcGroupDTO).collect(Collectors.toList());
            return ccGroupDTOList;
        } catch (Exception e) {
            log.error("{}|{}", ErrorCode.CMDB_API_DATA_ERROR, SEARCH_DYNAMIC_GROUP, e);
            return Collections.emptyList();
        }
    }

    private CcGroupDTO convertToCcGroupDTO(CcDynamicGroupDTO ccDynamicGroupDTO) {
        CcGroupDTO ccGroupDTO = new CcGroupDTO();
        ccGroupDTO.setAppId(ccDynamicGroupDTO.getAppId());
        ccGroupDTO.setId(ccDynamicGroupDTO.getId());
        ccGroupDTO.setName(ccDynamicGroupDTO.getName());
        return ccGroupDTO;
    }

    /**
     * 获取自定义分组下面的IP
     *
     * @param owner
     * @param uin
     * @param appId
     * @param groupId
     * @return
     */
    @Override
    public List<CcGroupHostPropDTO> getCustomGroupIp(long appId, String owner, String uin,
                                                     String groupId) throws ServiceException {
        return getDynamicGroupIpFromCMDB(appId, owner, uin, groupId);
    }

    public List<CcGroupHostPropDTO> getDynamicGroupIpFromCMDB(long appId, String owner, String uin,
                                                              String groupId) throws ServiceException {
        try {
            owner = defaultSupplierAccount;
            uin = defaultUin;
            ExecuteDynamicGroupReq req = makeBaseReq(ExecuteDynamicGroupReq.class, uin, owner);
            req.setAppId(appId);
            req.setGroupId(groupId);
            int limit = 200;
            int start = 0;
            req.getPage().setLimit(limit);

            List<CcGroupHostPropDTO> ccGroupHostList = new ArrayList<>();
            boolean isLastPage = false;
            while (!isLastPage) {
                req.getPage().setStart(start);
                EsbResp<ExecuteDynamicGroupHostResult> esbResp = getEsbRespByReq(HttpPost.METHOD_NAME,
                    EXECUTE_DYNAMIC_GROUP, req, new TypeReference<EsbResp<ExecuteDynamicGroupHostResult>>() {
                    });
                ExecuteDynamicGroupHostResult ccRespData = esbResp.getData();
                if (ccRespData != null) {
                    List<CcHostInfoDTO> hostInfoList = ccRespData.getInfo();
                    for (CcHostInfoDTO ccHostInfo : hostInfoList) {
                        if (ccHostInfo.getCloudId() == null || ccHostInfo.getCloudId() < 0) {
                            log.warn("host(id=" + ccHostInfo.getHostId()
                                + ",ip=" + ccHostInfo.getIp() + ") does not "
                                + "have cloud area, ignore");
                        } else if (StringUtils.isBlank(ccHostInfo.getIp())) {
                            log.warn("host(id=" + ccHostInfo.getHostId() + ",ip=" + ccHostInfo.getIp() + ") ip " +
                                "invalid, ignore");
                        } else {
                            ccGroupHostList.add(convertToCcHost(ccHostInfo));
                        }
                    }
                    start += hostInfoList.size();
                    // 如果该页未达到limit，说明是最后一页
                    if (hostInfoList.size() < limit) {
                        isLastPage = true;
                    }
                }
            }
            if (queryAgentStatusClient != null) {
                for (CcGroupHostPropDTO ccHost : ccGroupHostList) {
                    // 多IP,需要设置agent绑定的IP
                    ccHost.setIp(queryAgentStatusClient.getHostIpByAgentStatus(ccHost.getIp(),
                        ccHost.getCloudIdList().get(0).getInstanceId()));
                }
            }
            return ccGroupHostList;
        } catch (Exception e) {
            String errorMsg = String.format("Get host by dynamic group id %s fail", groupId);
            log.error(errorMsg, e);
            throw new ServiceException(ErrorCode.CMDB_API_DATA_ERROR);
        }
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
        try {
            List<CcCloudAreaInfoDTO> appCloudAreaList = new ArrayList<>();
            boolean isLastPage = false;
            int limit = 200;
            int start = 0;
            while (!isLastPage) {
                GetCloudAreaInfoReq req = makeBaseReq(GetCloudAreaInfoReq.class, defaultUin, defaultSupplierAccount);
                PageDTO page = new PageDTO(start, limit, null);
                req.setPage(page);
                req.setCondition(Collections.emptyMap());
                EsbResp<SearchCloudAreaResult> esbResp = getEsbRespByReq(HttpPost.METHOD_NAME, GET_CLOUD_AREAS, req,
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
        } catch (Exception e) {
            log.error("{}|{}", ErrorCode.CMDB_API_DATA_ERROR, GET_CLOUD_AREAS, e);
            throw caughtException(e, ErrorCode.CMDB_API_DATA_ERROR);
        }
    }

    @Override
    public List<ApplicationHostInfoDTO> listAppHosts(long appId, Collection<IpDTO> ipList) {
        if (ipList == null || ipList.isEmpty()) {
            return Collections.emptyList();
        }
        List<ApplicationHostInfoDTO> appHosts = getHostByIp(new GetHostByIpInput(appId, null, null,
            ipList.stream().map(IpDTO::getIp).collect(Collectors.toList())));
        if (appHosts == null || appHosts.isEmpty()) {
            return Collections.emptyList();
        }
        return appHosts.stream().filter(host ->
            ipList.contains(new IpDTO(host.getCloudAreaId(), host.getIp()))).collect(Collectors.toList());
    }

    @Override
    public List<FindHostBizRelationsResult> findHostBizRelations(String uin, List<Long> hostIdList) {
        try {
            String owner = defaultSupplierAccount;
            uin = defaultUin;
            FindHostBizRelationsReq req = makeBaseReq(FindHostBizRelationsReq.class, uin, owner);
            req.setHostIdList(hostIdList);
            EsbResp<List<FindHostBizRelationsResult>> esbResp = getEsbRespByReq(HttpPost.METHOD_NAME,
                FIND_HOST_BIZ_RELATIONS, req, new TypeReference<EsbResp<List<FindHostBizRelationsResult>>>() {
                });
            List<FindHostBizRelationsResult> results = esbResp.getData();
            if (esbResp.getData() == null) {
                return Collections.emptyList();
            }
            return results;
        } catch (Exception e) {
            log.error("{}|{}", ErrorCode.CMDB_API_DATA_ERROR, FIND_HOST_BIZ_RELATIONS, e);
            throw caughtException(e, ErrorCode.CMDB_API_DATA_ERROR);
        }
    }

    @Override
    public List<ApplicationHostInfoDTO> getHostByIpWithoutAppId(String uin, List<String> ipList) {
        //ipList分批
        List<ApplicationHostInfoDTO> resultList = new ArrayList<>();
        int batchSize = 500;
        int start = 0;
        int end = start + batchSize;
        int ipListSize = ipList.size();
        end = Math.min(end, ipListSize);
        do {
            List<String> ipSubList = ipList.subList(start, end);
            resultList.addAll(getHostByIpWithoutAppIdOnce(uin, ipSubList));
            start += batchSize;
            end = start + batchSize;
            end = Math.min(end, ipListSize);
        } while (start < ipListSize);
        return resultList;
    }

    private List<ApplicationHostInfoDTO> getHostByIpWithoutAppIdOnce(String uin, List<String> ipList) {
        try {
            String owner = defaultSupplierAccount;
            uin = defaultUin;
            List<ApplicationHostInfoDTO> hostInfoList = new ArrayList<>();
            ListHostsWithoutBizReq req = makeBaseReq(ListHostsWithoutBizReq.class, uin, owner);
            ConditionDTO condition = new ConditionDTO();
            condition.setCondition("AND");
            List<BaseConditionDTO> rules = new ArrayList<>();
            ipList.removeIf(StringUtils::isBlank);
            BaseConditionDTO baseConditionDTO = new BaseConditionDTO();
            baseConditionDTO.setField("bk_host_innerip");
            baseConditionDTO.setOperator("in");
            baseConditionDTO.setValue(ipList);
            rules.add(baseConditionDTO);
            condition.setRules(rules);
            req.setCondition(condition);

            int limit = 200;
            int start = 0;
            boolean isLastPage = false;
            while (!isLastPage) {
                PageDTO page = new PageDTO(start, limit, "");
                req.setPage(page);
                EsbResp<ListHostsWithoutBizResult> esbResp = getEsbRespByReq(HttpPost.METHOD_NAME,
                    LIST_HOSTS_WITHOUT_BIZ, req, new TypeReference<EsbResp<ListHostsWithoutBizResult>>() {
                    });
                ListHostsWithoutBizResult pageData = esbResp.getData();
                if (esbResp.getData() == null) {
                    return Collections.emptyList();
                }
                for (CcHostInfoDTO hostInfo : pageData.getInfo()) {
                    start++;
                    ApplicationHostInfoDTO host = convertHost(-1L, hostInfo);
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
        } catch (Exception e) {
            log.error("{}|{}", ErrorCode.CMDB_API_DATA_ERROR, LIST_HOSTS_WITHOUT_BIZ, e);
            throw caughtException(e, ErrorCode.CMDB_API_DATA_ERROR);
        }
    }

    @Override
    public List<ApplicationHostInfoDTO> getHostByIp(GetHostByIpInput input) {
        try {
            input.owner = defaultSupplierAccount;
            input.uin = defaultUin;
            List<ApplicationHostInfoDTO> hostInfoList = new ArrayList<>();
            ListBizHostReq req = makeBaseReq(ListBizHostReq.class, input.uin, input.owner);
            req.setAppId(input.appId);
            ConditionDTO condition = new ConditionDTO();
            condition.setCondition("AND");
            List<BaseConditionDTO> rules = new ArrayList<>();
            input.ipList.removeIf(StringUtils::isBlank);
            BaseConditionDTO baseConditionDTO = new BaseConditionDTO();
            baseConditionDTO.setField("bk_host_innerip");
            baseConditionDTO.setOperator("in");
            baseConditionDTO.setValue(input.ipList);
            rules.add(baseConditionDTO);
            condition.setRules(rules);
            req.setCondition(condition);

            int limit = 200;
            int start = 0;
            boolean isLastPage = false;
            while (!isLastPage) {
                PageDTO page = new PageDTO(start, limit, "");
                req.setPage(page);
                EsbResp<ListBizHostResult> esbResp = getEsbRespByReq(HttpPost.METHOD_NAME, LIST_BIZ_HOSTS, req,
                    new TypeReference<EsbResp<ListBizHostResult>>() {
                    });
                ListBizHostResult pageData = esbResp.getData();
                if (esbResp.getData() == null) {
                    return Collections.emptyList();
                }
                for (CcHostInfoDTO hostInfo : pageData.getInfo()) {
                    start++;
                    ApplicationHostInfoDTO host = convertHost(input.appId, hostInfo);
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
        } catch (Exception e) {
            log.error("{}|{}", ErrorCode.CMDB_API_DATA_ERROR, LIST_BIZ_HOSTS, e);
            throw caughtException(e, ErrorCode.CMDB_API_DATA_ERROR);
        }
    }

    @Override
    public ApplicationHostInfoDTO getHostByIp(Long cloudAreaId, String ip) {
        try {
            String owner = defaultSupplierAccount;
            String uin = defaultUin;
            ListHostsWithoutBizReq req = makeBaseReq(ListHostsWithoutBizReq.class, uin, owner);
            ConditionDTO condition = new ConditionDTO();
            condition.setCondition("AND");
            List<BaseConditionDTO> rules = new ArrayList<>();
            BaseConditionDTO ipCondition = new BaseConditionDTO();
            ipCondition.setField("bk_host_innerip");
            ipCondition.setOperator("equal");
            ipCondition.setValue(ip);
            BaseConditionDTO cloudAreaIdCondition = new BaseConditionDTO();
            cloudAreaIdCondition.setField("bk_cloud_id");
            cloudAreaIdCondition.setOperator("equal");
            cloudAreaIdCondition.setValue(cloudAreaId);
            rules.add(ipCondition);
            rules.add(cloudAreaIdCondition);
            condition.setRules(rules);
            req.setCondition(condition);

            int limit = 1;
            int start = 0;
            PageDTO page = new PageDTO(start, limit, "");
            req.setPage(page);
            EsbResp<ListHostsWithoutBizResult> esbResp = getEsbRespByReq(HttpPost.METHOD_NAME, LIST_HOSTS_WITHOUT_BIZ
                , req, new TypeReference<EsbResp<ListHostsWithoutBizResult>>() {
                });
            ListHostsWithoutBizResult pageData = esbResp.getData();
            if (esbResp.getData() == null || CollectionUtils.isEmpty(esbResp.getData().getInfo())) {
                return null;
            }
            return convertHost(-1L, pageData.getInfo().get(0));
        } catch (Exception e) {
            log.error("{}|{}", ErrorCode.CMDB_API_DATA_ERROR, LIST_HOSTS_WITHOUT_BIZ, e);
            throw caughtException(e, ErrorCode.CMDB_API_DATA_ERROR);
        }
    }

    @Override
    public List<CcObjAttributeDTO> getObjAttributeList(String objId) throws ServiceException {
        GetObjAttributeReq req = makeBaseReqByWeb(
            GetObjAttributeReq.class, null, defaultUin, defaultSupplierAccount);
        req.setObjId(objId);
        try {
            EsbResp<List<CcObjAttributeDTO>> esbResp = getEsbRespByReq(HttpPost.METHOD_NAME, GET_OBJ_ATTRIBUTES, req,
                new TypeReference<EsbResp<List<CcObjAttributeDTO>>>() {
                });
            return esbResp.getData();
        } catch (Exception e) {
            log.error("{}|{}", ErrorCode.CMDB_API_DATA_ERROR, GET_OBJ_ATTRIBUTES, e);
            throw caughtException(e, ErrorCode.CMDB_API_DATA_ERROR);
        }
    }

    @Override
    public Set<String> getAppUsersByRole(Long appId, String role) throws ServiceException {
        CcCountInfo searchResult;
        try {
            GetAppReq req = makeBaseReqByWeb(GetAppReq.class, null, defaultUin, defaultSupplierAccount);
            Map<String, Object> condition = new HashMap<>();
            condition.put("bk_biz_id", appId);
            req.setCondition(condition);
            req.setFields(Collections.singletonList(role));
            EsbResp<CcCountInfo> esbResp = getEsbRespByReq(HttpPost.METHOD_NAME, SEARCH_BUSINESS, req,
                new TypeReference<EsbResp<CcCountInfo>>() {
                });
            searchResult = esbResp.getData();
        } catch (Exception e) {
            log.error("{}|{}", ErrorCode.CMDB_API_DATA_ERROR, SEARCH_BUSINESS, e);
            throw caughtException(e, ErrorCode.CMDB_API_DATA_ERROR);
        }
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
    public List<AppRoleDTO> getAppRoleList() throws ServiceException {
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
        try {
            String owner = defaultSupplierAccount;
            String uin = defaultUin;
            GetTopoNodePathReq req = makeBaseReq(GetTopoNodePathReq.class, uin, owner);

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
            req.setAppId(getTopoNodePathReq.getAppId());

            List<InstanceTopologyDTO> hierarchyTopoList = new ArrayList<>();
            if (!nonAppNodes.isEmpty()) {
                EsbResp<List<TopoNodePathDTO>> esbResp = getEsbRespByReq(HttpPost.METHOD_NAME, GET_TOPO_NODE_PATHS,
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
                appNodes.forEach(appId -> {
                    InstanceTopologyDTO hierarchyTopo = new InstanceTopologyDTO();
                    hierarchyTopo.setObjectId("biz");
                    hierarchyTopo.setInstanceId(appId);
                    hierarchyTopoList.add(hierarchyTopo);
                });
            }
            return hierarchyTopoList;
        } catch (Exception e) {
            log.error("{}|{}", ErrorCode.CMDB_API_DATA_ERROR, GET_TOPO_NODE_PATHS, e);
            throw caughtException(e, ErrorCode.CMDB_API_DATA_ERROR);
        }
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
        EsbResp<ResourceWatchResult<HostEventDetail>> esbResp = getEsbRespByReq(HttpPost.METHOD_NAME, RESOURCE_WATCH,
            req, new TypeReference<EsbResp<ResourceWatchResult<HostEventDetail>>>() {
            }, new LongRetryableHttpHelper());
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
        EsbResp<ResourceWatchResult<HostRelationEventDetail>> esbResp = getEsbRespByReq(HttpPost.METHOD_NAME,
            RESOURCE_WATCH, req, new TypeReference<EsbResp<ResourceWatchResult<HostRelationEventDetail>>>() {
            }, new LongRetryableHttpHelper());
        return esbResp.getData();
    }

    @Override
    public ResourceWatchResult<AppEventDetail> getAppEvents(Long startTime, String cursor) {
        ResourceWatchReq req = makeBaseReqByWeb(
            ResourceWatchReq.class, null, defaultUin, defaultSupplierAccount);
        req.setFields(Arrays.asList("bk_biz_id", "bk_biz_name", "bk_biz_maintainer", "bk_supplier_account",
            "time_zone", "bk_operate_dept_id", "bk_operate_dept_name", "language"));
        req.setResource("biz");
        req.setCursor(cursor);
        req.setStartTime(startTime);
        EsbResp<ResourceWatchResult<AppEventDetail>> esbResp = getEsbRespByReq(HttpPost.METHOD_NAME, RESOURCE_WATCH,
            req, new TypeReference<EsbResp<ResourceWatchResult<AppEventDetail>>>() {
            }, new LongRetryableHttpHelper());
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
