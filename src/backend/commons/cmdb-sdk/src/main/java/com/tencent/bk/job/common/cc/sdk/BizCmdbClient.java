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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.tencent.bk.job.common.cc.config.CmdbConfig;
import com.tencent.bk.job.common.cc.exception.CmdbException;
import com.tencent.bk.job.common.cc.model.AppRoleDTO;
import com.tencent.bk.job.common.cc.model.BriefTopologyDTO;
import com.tencent.bk.job.common.cc.model.BusinessInfoDTO;
import com.tencent.bk.job.common.cc.model.CcCloudAreaInfoDTO;
import com.tencent.bk.job.common.cc.model.CcCloudIdDTO;
import com.tencent.bk.job.common.cc.model.CcDynamicGroupDTO;
import com.tencent.bk.job.common.cc.model.CcHostInfoDTO;
import com.tencent.bk.job.common.cc.model.CcInstanceDTO;
import com.tencent.bk.job.common.cc.model.CcObjAttributeDTO;
import com.tencent.bk.job.common.cc.model.DynamicGroupHostPropDTO;
import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
import com.tencent.bk.job.common.cc.model.TopoNodePathDTO;
import com.tencent.bk.job.common.cc.model.bizset.BizFilter;
import com.tencent.bk.job.common.cc.model.container.ContainerDTO;
import com.tencent.bk.job.common.cc.model.container.ContainerDetailDTO;
import com.tencent.bk.job.common.cc.model.container.KubeClusterDTO;
import com.tencent.bk.job.common.cc.model.container.KubeNamespaceDTO;
import com.tencent.bk.job.common.cc.model.container.KubeNodeID;
import com.tencent.bk.job.common.cc.model.container.KubeTopologyDTO;
import com.tencent.bk.job.common.cc.model.container.KubeWorkloadDTO;
import com.tencent.bk.job.common.cc.model.container.PodDTO;
import com.tencent.bk.job.common.cc.model.filter.BaseRuleDTO;
import com.tencent.bk.job.common.cc.model.filter.ComposeRuleDTO;
import com.tencent.bk.job.common.cc.model.filter.IRule;
import com.tencent.bk.job.common.cc.model.filter.PropertyFilterDTO;
import com.tencent.bk.job.common.cc.model.filter.Rule;
import com.tencent.bk.job.common.cc.model.filter.RuleConditionEnum;
import com.tencent.bk.job.common.cc.model.filter.RuleOperatorEnum;
import com.tencent.bk.job.common.cc.model.query.KubeClusterQuery;
import com.tencent.bk.job.common.cc.model.query.NamespaceQuery;
import com.tencent.bk.job.common.cc.model.query.WorkloadQuery;
import com.tencent.bk.job.common.cc.model.req.CmdbPageReq;
import com.tencent.bk.job.common.cc.model.req.ExecuteDynamicGroupReq;
import com.tencent.bk.job.common.cc.model.req.FindHostBizRelationsReq;
import com.tencent.bk.job.common.cc.model.req.FindModuleHostRelationReq;
import com.tencent.bk.job.common.cc.model.req.GetAppReq;
import com.tencent.bk.job.common.cc.model.req.GetBizInstTopoReq;
import com.tencent.bk.job.common.cc.model.req.GetBizInternalModuleReq;
import com.tencent.bk.job.common.cc.model.req.GetBizKubeCacheTopoReq;
import com.tencent.bk.job.common.cc.model.req.GetBriefCacheTopoReq;
import com.tencent.bk.job.common.cc.model.req.GetCloudAreaInfoReq;
import com.tencent.bk.job.common.cc.model.req.GetObjAttributeReq;
import com.tencent.bk.job.common.cc.model.req.GetTopoNodePathReq;
import com.tencent.bk.job.common.cc.model.req.ListBizHostReq;
import com.tencent.bk.job.common.cc.model.req.ListHostsWithoutBizReq;
import com.tencent.bk.job.common.cc.model.req.ListKubeClusterReq;
import com.tencent.bk.job.common.cc.model.req.ListKubeContainerByTopoReq;
import com.tencent.bk.job.common.cc.model.req.ListKubeNamespaceReq;
import com.tencent.bk.job.common.cc.model.req.ListKubeWorkloadReq;
import com.tencent.bk.job.common.cc.model.req.Page;
import com.tencent.bk.job.common.cc.model.req.ResourceWatchReq;
import com.tencent.bk.job.common.cc.model.req.SearchHostDynamicGroupReq;
import com.tencent.bk.job.common.cc.model.req.input.GetHostByIpInput;
import com.tencent.bk.job.common.cc.model.response.CountInfo;
import com.tencent.bk.job.common.cc.model.result.BaseCcSearchResult;
import com.tencent.bk.job.common.cc.model.result.BizEventDetail;
import com.tencent.bk.job.common.cc.model.result.ExecuteDynamicGroupHostResult;
import com.tencent.bk.job.common.cc.model.result.FindModuleHostRelationResult;
import com.tencent.bk.job.common.cc.model.result.GetBizInternalModuleResult;
import com.tencent.bk.job.common.cc.model.result.HostBizRelationDTO;
import com.tencent.bk.job.common.cc.model.result.HostEventDetail;
import com.tencent.bk.job.common.cc.model.result.HostProp;
import com.tencent.bk.job.common.cc.model.result.HostRelationEventDetail;
import com.tencent.bk.job.common.cc.model.result.HostWithModules;
import com.tencent.bk.job.common.cc.model.result.ListBizHostResult;
import com.tencent.bk.job.common.cc.model.result.ListHostsWithoutBizResult;
import com.tencent.bk.job.common.cc.model.result.ModuleProp;
import com.tencent.bk.job.common.cc.model.result.ResourceWatchResult;
import com.tencent.bk.job.common.cc.model.result.SearchAppResult;
import com.tencent.bk.job.common.cc.model.result.SearchCloudAreaResult;
import com.tencent.bk.job.common.cc.model.result.SearchDynamicGroupResult;
import com.tencent.bk.job.common.cc.util.TopologyUtil;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.HttpMethodEnum;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.esb.config.AppProperties;
import com.tencent.bk.job.common.esb.config.BkApiGatewayProperties;
import com.tencent.bk.job.common.esb.model.EsbReq;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.InternalCmdbException;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.model.error.ErrorType;
import com.tencent.bk.job.common.paas.user.IVirtualAdminAccountProvider;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.common.util.CollectionUtil;
import com.tencent.bk.job.common.util.FlowController;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.PageUtil;
import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.common.util.Utils;
import com.tencent.bk.job.common.util.http.HttpHelperFactory;
import com.tencent.bk.job.common.util.json.JsonUtils;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * CMDB API 调用客户端
 */
@Slf4j
public class BizCmdbClient extends BaseCmdbClient implements IBizCmdbClient {

    private static final ConcurrentHashMap<Long, Pair<InstanceTopologyDTO, Long>> bizInstTopoMap =
        new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, ReentrantLock> bizInstTopoLockMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Pair<InstanceTopologyDTO, Long>> bizInternalTopoMap =
        new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, ReentrantLock> bizInternalTopoLockMap = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor threadPoolExecutor;
    private final ThreadPoolExecutor longTermThreadPoolExecutor;

    private final LoadingCache<Pair<String, Long>, InstanceTopologyDTO> bizInstCompleteTopologyCache =
        CacheBuilder.newBuilder()
            .maximumSize(1000).expireAfterWrite(30, TimeUnit.SECONDS).
            build(new CacheLoader<Pair<String, Long>, InstanceTopologyDTO>() {
                      @Override
                      public InstanceTopologyDTO load(@SuppressWarnings("NullableProblems") Pair<String, Long> key) {
                          String tenantId = key.getLeft();
                          Long bizId = key.getRight();
                          return getBizInstCompleteTopology(tenantId, bizId);
                      }
                  }
            );

    public BizCmdbClient(AppProperties appProperties,
                         BkApiGatewayProperties bkApiGatewayProperties,
                         CmdbConfig cmdbConfig,
                         String lang,
                         ThreadPoolExecutor threadPoolExecutor,
                         ThreadPoolExecutor longTermThreadPoolExecutor,
                         FlowController flowController,
                         MeterRegistry meterRegistry,
                         TenantEnvService tenantEnvService,
                         IVirtualAdminAccountProvider virtualAdminAccountProvider) {
        super(
            flowController,
            appProperties,
            bkApiGatewayProperties,
            cmdbConfig,
            meterRegistry,
            tenantEnvService,
            virtualAdminAccountProvider,
            lang
        );
        this.threadPoolExecutor = threadPoolExecutor;
        this.longTermThreadPoolExecutor = longTermThreadPoolExecutor;
    }

    @Override
    public InstanceTopologyDTO getBizInstCompleteTopology(String tenantId, long bizId) {
        InstanceTopologyDTO completeTopologyDTO;
        if (cmdbConfig.getEnableInterfaceBriefCacheTopo()) {
            completeTopologyDTO = getBriefCacheTopo(tenantId, bizId);
        } else {
            InstanceTopologyDTO topologyDTO = getBizInstTopologyWithoutInternalTopo(tenantId, bizId);
            InstanceTopologyDTO internalTopologyDTO = getBizInternalModule(tenantId, bizId);
            internalTopologyDTO.setObjectName(topologyDTO.getObjectName());
            internalTopologyDTO.setInstanceName(topologyDTO.getInstanceName());
            completeTopologyDTO = TopologyUtil.mergeTopology(internalTopologyDTO, topologyDTO);
        }
        return completeTopologyDTO;
    }

    public InstanceTopologyDTO getCachedBizInstCompleteTopology(String tenantId, long bizId) {
        try {
            return bizInstCompleteTopologyCache.get(Pair.of(tenantId, bizId));
        } catch (ExecutionException | UncheckedExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new InternalException(e, ErrorCode.INTERNAL_ERROR, null);
            }
        }
    }

    @Override
    public InstanceTopologyDTO getBizInstTopology(long bizId) {
        String tenantId = JobContextUtil.getTenantId();
        return getCachedBizInstCompleteTopology(tenantId, bizId);
    }

    public InstanceTopologyDTO getBizInstTopologyWithoutInternalTopo(String tenantId, long bizId) {
        if (cmdbConfig.getEnableLockOptimize()) {
            return getBizInstTopologyWithoutInternalTopoWithLock(tenantId, bizId);
        } else {
            return getBizInstTopologyWithoutInternalTopoFromCMDB(tenantId, bizId);
        }
    }

    public InstanceTopologyDTO getBizInstTopologyWithoutInternalTopoWithLock(String tenantId, long bizId) {
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
                    InstanceTopologyDTO topo = getBizInstTopologyWithoutInternalTopoFromCMDB(tenantId, bizId);
                    bizInstTopoMap.put(bizId, Pair.of(topo, System.currentTimeMillis()));
                    return topo;
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public InstanceTopologyDTO getBriefCacheTopo(String tenantId, long bizId) {
        GetBriefCacheTopoReq req = makeCmdbBaseReq(GetBriefCacheTopoReq.class);
        req.setBizId(bizId);
        String uri = GET_BIZ_BRIEF_CACHE_TOPO.replace("{bk_biz_id}", String.valueOf(bizId));
        EsbResp<BriefTopologyDTO> esbResp = requestCmdbApi(
            tenantId,
            HttpMethodEnum.GET,
            uri,
            req.toUrlParams(),
            null,
            new TypeReference<EsbResp<BriefTopologyDTO>>() {
            });
        return TopologyUtil.convert(esbResp.getData());
    }

    public InstanceTopologyDTO getBizInstTopologyWithoutInternalTopoFromCMDB(String tenantId, long bizId) {
        GetBizInstTopoReq req = makeCmdbBaseReq(GetBizInstTopoReq.class);
        req.setBizId(bizId);
        String uri = SEARCH_BIZ_INST_TOPO.replace("{bk_biz_id}", String.valueOf(bizId));
        EsbResp<List<InstanceTopologyDTO>> esbResp = requestCmdbApi(
            tenantId,
            HttpMethodEnum.GET,
            uri,
            req.toUrlParams(),
            null,
            new TypeReference<EsbResp<List<InstanceTopologyDTO>>>() {
            });
        if (!esbResp.getData().isEmpty()) {
            return esbResp.getData().get(0);
        } else {
            return null;
        }
    }


    @Override
    public InstanceTopologyDTO getBizInternalModule(String tenantId, long bizId) {
        if (cmdbConfig.getEnableLockOptimize()) {
            return getBizInternalModuleWithLock(tenantId, bizId);
        } else {
            return getBizInternalModuleFromCMDB(tenantId, bizId);
        }
    }

    /**
     * 防止参数完全相同的请求在并发时多次请求CMDB，降低对CMDB的请求量
     */
    public InstanceTopologyDTO getBizInternalModuleWithLock(String tenantId, long bizId) {
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
                    InstanceTopologyDTO topo = getBizInternalModuleFromCMDB(tenantId, bizId);
                    bizInternalTopoMap.put(bizId, Pair.of(topo, System.currentTimeMillis()));
                    return topo;
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public InstanceTopologyDTO getBizInternalModuleFromCMDB(String tenantId, long bizId) {
        GetBizInternalModuleReq req = makeCmdbBaseReq(GetBizInternalModuleReq.class);
        req.setBizId(bizId);
        String uri = GET_BIZ_INTERNAL_MODULE.replace("{bk_supplier_account}", req.getBkSupplierAccount());
        uri = uri.replace("{bk_biz_id}", String.valueOf(bizId));
        EsbResp<GetBizInternalModuleResult> esbResp = requestCmdbApi(
            tenantId,
            HttpMethodEnum.GET,
            uri,
            req.toUrlParams(),
            null,
            new TypeReference<EsbResp<GetBizInternalModuleResult>>() {
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
    public List<ApplicationHostDTO> getHosts(String tenantId, long bizId, List<CcInstanceDTO> ccInstList) {
        List<HostWithModules> hostWithModuleList = getHostRelationsByTopology(tenantId, bizId, ccInstList);
        return convertToHostInfoDTOList(bizId, hostWithModuleList);
    }

    @Override
    public List<HostWithModules> getHostRelationsByTopology(String tenantId,
                                                            long bizId,
                                                            List<CcInstanceDTO> ccInstList) {
        StopWatch watch = new StopWatch("getHostRelationsByTopology");
        watch.start("getCachedBizInstCompleteTopology");
        InstanceTopologyDTO appCompleteTopology = getCachedBizInstCompleteTopology(tenantId, bizId);
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
        watch.start("findHostRelationByModule");
        List<HostWithModules> hostWithModulesList = findHostRelationByModule(
            tenantId,
            bizId,
            new ArrayList<>(moduleIdSet)
        );
        watch.stop();

        if (watch.getTotalTimeMillis() > 1000L) {
            log.warn("Get hostRelations by topo is slow, bizId: {}, ccInsts: {}, watchInfo: {}", bizId, ccInstList,
                watch.prettyPrint());
        }
        hostWithModulesList = hostWithModulesList.stream().filter(hostWithModules -> {
            boolean valid = hostWithModules.getHost() != null && hostWithModules.getHost().getHostId() != null;
            if (!valid) {
                log.warn("Ignore hostWithModules because of null host/hostId:{}", hostWithModules);
            }
            if (hostWithModules.getModules() == null) {
                log.warn("Ignore hostWithModules because of null modules:{}", hostWithModules);
                valid = false;
            }
            return valid;
        }).peek(hostWithModules -> hostWithModules.getHost().setTenantId(tenantId)
        ).collect(Collectors.toList());
        return hostWithModulesList;
    }

    @Override
    public List<HostWithModules> findHostRelationByModule(String tenantId, long bizId, List<Long> moduleIdList) {
        //moduleId分批
        List<HostWithModules> resultList = new ArrayList<>();
        int batchSize = 200;
        int start = 0;
        int end = start + batchSize;
        int moduleIdSize = moduleIdList.size();
        end = Math.min(end, moduleIdSize);
        do {
            List<Long> moduleIdSubList = moduleIdList.subList(start, end);
            if (!moduleIdSubList.isEmpty()) {
                resultList.addAll(findModuleHostRelationConcurrently(tenantId, bizId, moduleIdSubList));
            }
            start += batchSize;
            end = start + batchSize;
            end = Math.min(end, moduleIdSize);
        } while (start < moduleIdSize);
        return resultList;
    }

    private FindModuleHostRelationResult getHostsByReq(String tenantId, FindModuleHostRelationReq req) {
        String uri = FIND_MODULE_HOST_RELATION.replace("{bk_biz_id}", String.valueOf(req.getBizId()));
        EsbResp<FindModuleHostRelationResult> esbResp = requestCmdbApi(
            tenantId,
            HttpMethodEnum.POST,
            uri,
            null,
            req,
            new TypeReference<EsbResp<FindModuleHostRelationResult>>() {
            });
        return esbResp.getData();
    }

    private FindModuleHostRelationReq genFindModuleHostRelationReq(long bizId, List<Long> moduleIdList, int start,
                                                                   int limit) {
        FindModuleHostRelationReq req = makeCmdbBaseReq(FindModuleHostRelationReq.class);
        req.setBizId(bizId);
        req.setModuleIdList(moduleIdList);
        Page page = new Page(start, limit);
        req.setPage(page);
        return req;
    }

    /**
     * 并发：按模块加载主机
     *
     * @param tenantId     租户ID
     * @param bizId        cmdb业务ID
     * @param moduleIdList 模块ID列表
     * @return 主机列表
     */
    private List<HostWithModules> findModuleHostRelationConcurrently(String tenantId,
                                                                     long bizId,
                                                                     List<Long> moduleIdList) {
        if (moduleIdList == null || moduleIdList.isEmpty()) {
            return Collections.emptyList();
        }
        int start = 0;
        //已调优
        int limit = 500;
        FindModuleHostRelationReq req = genFindModuleHostRelationReq(bizId, moduleIdList, start, limit);
        //先拉一次获取总数
        FindModuleHostRelationResult pageData = getHostsByReq(tenantId, req);
        List<HostWithModules> hostWithModulesList = pageData.getRelation();
        LinkedBlockingQueue<HostWithModules> resultQueue =
            new LinkedBlockingQueue<>(hostWithModulesList);
        // 如果该页未达到limit，说明是最后一页
        if (pageData.getCount() <= limit) {
            log.info("bizId {}:{} hosts in total, {} hosts indeed", bizId, pageData.getCount(), resultQueue.size());
        } else if (hostWithModulesList.size() <= limit) {
            int totalCount = pageData.getCount() - limit;
            List<Future<?>> futures = new ArrayList<>();
            Long startTime = System.currentTimeMillis();
            while (totalCount > 0) {
                start += limit;
                FindModuleHostRelationTask task = new FindModuleHostRelationTask(
                    tenantId,
                    resultQueue,
                    genFindModuleHostRelationReq(bizId, moduleIdList, start, limit),
                    JobContextUtil.getRequestId()
                );
                Future<?> future;
                if (totalCount > 10000) {
                    //主机数太多，防止将CMDB拉挂了
                    future = longTermThreadPoolExecutor.submit(task);
                } else {
                    // 默认采用多个并发线程拉取
                    future = threadPoolExecutor.submit(task);
                }
                futures.add(future);
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
        return new ArrayList<>(resultQueue);
    }

    private void fillAgentInfo(
        ApplicationHostDTO applicationHostDTO,
        HostProp host
    ) {
        String multiIp = host.getIp();
        multiIp = multiIp.trim();
        applicationHostDTO.setCloudAreaId(host.getCloudAreaId());
        List<String> ipList = Utils.getNotBlankSplitList(multiIp, ",");
        if (!ipList.isEmpty()) {
            applicationHostDTO.setIp(ipList.get(0));
        } else {
            log.warn("no available ip, raw multiIp={}", multiIp);
        }
    }

    private ApplicationHostDTO convertToHostInfoDTO(
        Long bizId,
        HostWithModules hostWithModules
    ) {
        HostProp host = hostWithModules.getHost();
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
        applicationHostDTO.setIpv6(host.getIpv6());
        applicationHostDTO.setAgentId(host.getAgentId());
        applicationHostDTO.setCloudAreaId(host.getCloudAreaId());
        applicationHostDTO.setHostId(host.getHostId());
        applicationHostDTO.setCloudVendorId(host.getCloudVendorId());
        fillAgentInfo(applicationHostDTO, host);
        List<ModuleProp> modules = hostWithModules.getModules();
        for (ModuleProp module : modules) {
            if (module == null || null == module.getModuleId()) {
                log.warn("invalid host:" + JsonUtils.toJson(applicationHostDTO));
            }
        }
        List<ModuleProp> validModules =
            hostWithModules.getModules().stream().filter(Objects::nonNull).collect(Collectors.toList());
        applicationHostDTO.setModuleId(
            validModules.stream()
                .map(ModuleProp::getModuleId)
                .collect(Collectors.toList()));
        applicationHostDTO.setSetId(
            validModules.stream()
                .map(ModuleProp::getSetId)
                .collect(Collectors.toList()));
        applicationHostDTO.setModuleType(validModules.stream().map(it -> {
            try {
                return Long.parseLong(it.getModuleType());
            } catch (Exception e) {
                return 0L;
            }
        }).collect(Collectors.toList()));
        applicationHostDTO.setHostName(host.getHostName());
        String osName = host.getOsName();
        if (osName != null && osName.length() > 512) {
            applicationHostDTO.setOsName(osName.substring(0, 512));
        } else {
            applicationHostDTO.setOsName(osName);
        }
        applicationHostDTO.setOsType(host.getOsType());
        return applicationHostDTO;
    }

    private List<ApplicationHostDTO> convertToHostInfoDTOList(
        long bizId,
        List<HostWithModules> hostWithModulesList
    ) {
        List<ApplicationHostDTO> applicationHostDTOList = new ArrayList<>();
        Set<String> ipSet = new HashSet<>();
        for (HostWithModules hostWithModules : hostWithModulesList) {
            HostProp host = hostWithModules.getHost();
            if (host == null) {
                log.warn("host=null,hostWithTopoInfo={}", JsonUtils.toJson(hostWithModules));
                continue;
            }
            ipSet.add(host.getCloudAreaId() + ":" + host.getIp());
            Long hostId = host.getHostId();
            if (hostId != null) {
                ApplicationHostDTO applicationHostDTO = convertToHostInfoDTO(bizId, hostWithModules);
                applicationHostDTOList.add(applicationHostDTO);
            } else {
                log.info("bk_host_id is null, ignore, host={}", JsonUtils.toJson(host));
            }
        }
        log.info("ipSet.size=" + ipSet.size());
        return applicationHostDTOList;
    }

    private ApplicationHostDTO convertHost(long bizId, CcHostInfoDTO ccHostInfo) {
        // 部分从cmdb同步过来的资源没有hostId，需要过滤掉
        if (ccHostInfo.getHostId() == null) {
            log.warn("host with no hostId ignored:{}", ccHostInfo);
            return null;
        }
        ApplicationHostDTO hostDTO = new ApplicationHostDTO();
        hostDTO.setHostId(ccHostInfo.getHostId());

        if (ccHostInfo.getOs() != null && ccHostInfo.getOs().length() > 512) {
            log.warn("osName truncated to 512, host={}", ccHostInfo);
            hostDTO.setOsName(ccHostInfo.getOs().substring(0, 512));
        } else {
            hostDTO.setOsName(ccHostInfo.getOs());
        }
        if (ccHostInfo.getCloudId() != null) {
            hostDTO.setCloudAreaId(ccHostInfo.getCloudId());
        } else {
            log.warn("Host does not have cloud area id!|{}", ccHostInfo);
            return null;
        }
        hostDTO.setDisplayIp(ccHostInfo.getInnerIp());
        hostDTO.setIp(ccHostInfo.getFirstIp());
        hostDTO.setIpv6(ccHostInfo.getIpv6());
        hostDTO.setAgentId(ccHostInfo.getAgentId());
        hostDTO.setBizId(bizId);
        hostDTO.setHostName(ccHostInfo.getHostName());
        hostDTO.setOsType(ccHostInfo.getOsType());
        hostDTO.setCloudVendorId(ccHostInfo.getCloudVendorId());
        Long lastTimeMills = null;
        if (StringUtils.isNotBlank(ccHostInfo.getLastTime())) {
            lastTimeMills = TimeUtil.parseIsoZonedTimeToMillis(ccHostInfo.getLastTime());
        }
        hostDTO.setLastTime(lastTimeMills);
        return hostDTO;
    }

    @Override
    public List<ApplicationDTO> getAllBizApps(String tenantId) {
        List<ApplicationDTO> appList = new ArrayList<>();
        int limit = 200;
        int start = 0;
        boolean isLastPage = false;
        String orderField = "bk_biz_id";
        while (!isLastPage) {
            GetAppReq req = makeCmdbBaseReq(GetAppReq.class);
            Page page = new Page(start, limit, orderField);
            req.setPage(page);
            String uri = SEARCH_BUSINESS.replace("{bk_supplier_account}", req.getBkSupplierAccount());
            EsbResp<SearchAppResult> esbResp = requestCmdbApi(
                tenantId,
                HttpMethodEnum.POST,
                uri,
                null,
                req,
                new TypeReference<EsbResp<SearchAppResult>>() {
                });
            SearchAppResult data = esbResp.getData();
            if (data == null) {
                appList.clear();
                throw new InternalCmdbException("Data is null", ErrorCode.CMDB_API_DATA_ERROR);
            }
            List<BusinessInfoDTO> businessInfos = data.getInfo();
            if (businessInfos != null && !businessInfos.isEmpty()) {
                for (BusinessInfoDTO businessInfo : businessInfos) {
                    if (businessInfo.getDeFault() == 0) {
                        ApplicationDTO applicationDTO = convertToAppInfo(
                            tenantId,
                            req.getBkSupplierAccount(),
                            businessInfo
                        );
                        appList.add(applicationDTO);
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

    private ApplicationDTO convertToAppInfo(String tenantId, String supplierAccount, BusinessInfoDTO businessInfo) {
        ApplicationDTO appInfo = new ApplicationDTO();
        appInfo.setTenantId(tenantId);
        appInfo.setName(businessInfo.getBizName());
        appInfo.setBkSupplierAccount(supplierAccount);
        appInfo.setTimeZone(businessInfo.getTimezone());
        appInfo.setScope(new ResourceScope(ResourceScopeTypeEnum.BIZ, businessInfo.getBizId().toString()));
        appInfo.setLanguage(businessInfo.getLanguage());
        appInfo.setDeFault(businessInfo.getDeFault());
        return appInfo;
    }

    private List<ApplicationDTO> convertToAppInfoList(String supplierAccount,
                                                      List<BusinessInfoDTO> businessInfoList) {
        List<ApplicationDTO> appInfoList = new ArrayList<>();
        for (BusinessInfoDTO businessInfo : businessInfoList) {
            ApplicationDTO appInfo = new ApplicationDTO();
            appInfo.setName(businessInfo.getBizName());
            appInfo.setBkSupplierAccount(supplierAccount);
            appInfo.setTimeZone(businessInfo.getTimezone());
            appInfo.setScope(new ResourceScope(ResourceScopeTypeEnum.BIZ, businessInfo.getBizId().toString()));
            appInfo.setLanguage(businessInfo.getLanguage());
            appInfo.setDeFault(businessInfo.getDeFault());
            appInfoList.add(appInfo);
        }
        return appInfoList;
    }

    @Override
    public List<ApplicationDTO> listBizAppByIds(String tenantId, List<Long> bizIds) {
        GetAppReq req = makeCmdbBaseReq(GetAppReq.class);
        // in查询filter
        BizFilter filter = new BizFilter();
        Rule rule = new Rule();
        rule.setField("bk_biz_id");
        rule.setOperator(RuleOperatorEnum.IN.getOperator());
        rule.setValue(bizIds);
        filter.setRules(Collections.singletonList(rule));
        filter.setCondition(BizFilter.CONDITION_AND);
        req.setBizFilter(filter);

        String uri = SEARCH_BUSINESS.replace("{bk_supplier_account}", req.getBkSupplierAccount());
        EsbResp<SearchAppResult> esbResp = requestCmdbApi(
            tenantId,
            HttpMethodEnum.POST,
            uri,
            null,
            req,
            new TypeReference<EsbResp<SearchAppResult>>() {
            });
        SearchAppResult data = esbResp.getData();
        if (data == null) {
            throw new InternalCmdbException("data is null", ErrorCode.CMDB_API_DATA_ERROR);
        }
        List<BusinessInfoDTO> businessInfos = data.getInfo();
        if (businessInfos == null || businessInfos.isEmpty()) {
            log.info("Query biz from cmdb through bizIds, return data is null, bizIdz={}", bizIds);
            return new ArrayList<>();
        }
        return convertToAppInfoList(req.getBkSupplierAccount(), businessInfos);
    }

    @Override
    public List<CcDynamicGroupDTO> getDynamicGroupList(long bizId) {
        SearchHostDynamicGroupReq req = makeCmdbBaseReq(SearchHostDynamicGroupReq.class);
        req.setBizId(bizId);
        int start = 0;
        int limit = 200;
        req.getPage().setStart(start);
        req.getPage().setLimit(limit);
        List<CcDynamicGroupDTO> ccDynamicGroupList = new ArrayList<>();
        boolean isLastPage = false;
        while (!isLastPage) {
            req.getPage().setStart(start);
            String uri = SEARCH_DYNAMIC_GROUP.replace("{bk_biz_id}", String.valueOf(bizId));
            EsbResp<SearchDynamicGroupResult> esbResp = requestCmdbApiUseContextTenantId(
                HttpMethodEnum.POST,
                uri,
                null,
                req,
                new TypeReference<EsbResp<SearchDynamicGroupResult>>() {
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

    private List<DynamicGroupHostPropDTO> convertToCcGroupHostPropList(List<CcHostInfoDTO> hostInfoList) {
        List<DynamicGroupHostPropDTO> ccGroupHostList = new ArrayList<>();
        for (CcHostInfoDTO ccHostInfo : hostInfoList) {
            if (ccHostInfo.getCloudId() == null || ccHostInfo.getCloudId() < 0) {
                log.warn(
                    "host(id={},ip={}) does not have cloud area, ignore",
                    ccHostInfo.getHostId(),
                    ccHostInfo.getInnerIp()
                );
            } else if (ccHostInfo.getHostId() == null) {
                log.warn("{} hostId is invalid, ignore", ccHostInfo);
            } else {
                ccGroupHostList.add(convertToCcHost(ccHostInfo));
            }
        }
        return ccGroupHostList;
    }

    @Override
    public List<DynamicGroupHostPropDTO> getDynamicGroupIp(long bizId, String groupId) {
        ExecuteDynamicGroupReq req = makeCmdbBaseReq(ExecuteDynamicGroupReq.class);
        req.setBizId(bizId);
        req.setGroupId(groupId);
        int limit = 200;
        int start = 0;
        req.getPage().setLimit(limit);

        List<DynamicGroupHostPropDTO> ccGroupHostList = new ArrayList<>();
        boolean isLastPage = false;
        while (!isLastPage) {
            req.getPage().setStart(start);
            String uri = EXECUTE_DYNAMIC_GROUP.replace("{bk_biz_id}", String.valueOf(bizId));
            uri = uri.replace("{id}", groupId);
            EsbResp<ExecuteDynamicGroupHostResult> esbResp = requestCmdbApiUseContextTenantId(
                HttpMethodEnum.POST,
                uri,
                null,
                req,
                new TypeReference<EsbResp<ExecuteDynamicGroupHostResult>>() {
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

    private DynamicGroupHostPropDTO convertToCcHost(CcHostInfoDTO ccHostInfo) {
        DynamicGroupHostPropDTO dynamicGroupHostPropDTO = new DynamicGroupHostPropDTO();
        dynamicGroupHostPropDTO.setId(ccHostInfo.getHostId());
        dynamicGroupHostPropDTO.setName(ccHostInfo.getHostName());
        dynamicGroupHostPropDTO.setInnerIp(ccHostInfo.getInnerIp());
        dynamicGroupHostPropDTO.setIpv6(ccHostInfo.getIpv6());
        dynamicGroupHostPropDTO.setAgentId(ccHostInfo.getAgentId());
        CcCloudIdDTO ccCloudIdDTO = new CcCloudIdDTO();
        // 仅使用CloudId其余属性未用到，暂不设置
        ccCloudIdDTO.setInstanceId(ccHostInfo.getCloudId());
        dynamicGroupHostPropDTO.setCloudIdList(Collections.singletonList(ccCloudIdDTO));
        return dynamicGroupHostPropDTO;
    }

    @Override
    public List<CcCloudAreaInfoDTO> getCloudAreaList(String tenantId) {
        return getCloudAreaByCondition(tenantId, null);
    }

    private List<CcCloudAreaInfoDTO> getCloudAreaByCondition(String tenantId, Map<String, Object> fieldConditions) {
        List<CcCloudAreaInfoDTO> appCloudAreaList = new ArrayList<>();
        boolean isLastPage = false;
        int limit = 200;
        int start = 0;
        while (!isLastPage) {
            GetCloudAreaInfoReq req = makeCmdbBaseReq(GetCloudAreaInfoReq.class);
            Page page = new Page(start, limit, null);
            req.setPage(page);
            if (fieldConditions != null && !fieldConditions.isEmpty()) {
                req.setCondition(fieldConditions);
            } else {
                req.setCondition(Collections.emptyMap());
            }
            EsbResp<SearchCloudAreaResult> esbResp = requestCmdbApi(
                tenantId,
                HttpMethodEnum.POST,
                GET_CLOUD_AREAS,
                null,
                req,
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
    public List<ApplicationHostDTO> listBizHosts(long bizId, Collection<HostDTO> ipList) {
        if (ipList == null || ipList.isEmpty()) {
            return Collections.emptyList();
        }
        List<ApplicationHostDTO> appHosts = getHostByIp(new GetHostByIpInput(bizId, null, null,
            ipList.stream().map(HostDTO::getIp).collect(Collectors.toList())));
        if (appHosts == null || appHosts.isEmpty()) {
            return Collections.emptyList();
        }
        return appHosts.stream().filter(host ->
            ipList.contains(new HostDTO(host.getCloudAreaId(), host.getIp()))).collect(Collectors.toList());
    }

    /**
     * 根据hostId查询主机业务关系信息
     *
     * @param hostIdList 主机id列表，数量<=500
     * @return 主机业务关系列表
     */
    @Override
    public List<HostBizRelationDTO> findHostBizRelations(String tenantId, List<Long> hostIdList) {
        FindHostBizRelationsReq req = makeCmdbBaseReq(FindHostBizRelationsReq.class);
        req.setHostIdList(hostIdList);
        EsbResp<List<HostBizRelationDTO>> esbResp = requestCmdbApi(
            tenantId,
            HttpMethodEnum.POST,
            FIND_HOST_BIZ_RELATIONS,
            null,
            req,
            new TypeReference<EsbResp<List<HostBizRelationDTO>>>() {
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
        ListBizHostReq req = makeCmdbBaseReq(ListBizHostReq.class);
        req.setBizId(input.getBizId());
        PropertyFilterDTO condition = new PropertyFilterDTO();
        condition.setCondition(RuleConditionEnum.AND.getCondition());
        input.ipList.removeIf(StringUtils::isBlank);
        condition.addRule(BaseRuleDTO.in("bk_host_innerip", input.ipList));
        req.setCondition(condition);

        String uri = LIST_BIZ_HOSTS.replace("{bk_biz_id}", String.valueOf(input.getBizId()));
        int limit = 200;
        int start = 0;
        boolean isLastPage = false;
        while (!isLastPage) {
            Page page = new Page(start, limit, "");
            req.setPage(page);
            EsbResp<ListBizHostResult> esbResp = requestCmdbApiUseContextTenantId(
                HttpMethodEnum.POST,
                uri,
                null,
                req,
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
    public List<ApplicationHostDTO> listHostsByCloudIps(String tenantId, List<String> cloudIps) {
        if (CollectionUtils.isEmpty(cloudIps)) {
            return Collections.emptyList();
        }
        ListHostsWithoutBizReq req = makeCmdbBaseReq(ListHostsWithoutBizReq.class);
        PropertyFilterDTO condition = new PropertyFilterDTO();
        condition.setCondition(RuleConditionEnum.OR.getCondition());
        Map<Long, List<String>> hostGroups = groupHostsByBkCloudId(cloudIps);
        hostGroups.forEach((bkCloudId, ips) -> {
            ComposeRuleDTO hostRule = new ComposeRuleDTO(RuleConditionEnum.AND.getCondition());
            hostRule.addRule(buildCloudIdRule(bkCloudId));
            hostRule.addRule(BaseRuleDTO.in("bk_host_innerip", ips));

            condition.addRule(hostRule);
        });
        req.setCondition(condition);

        return listHostsWithoutBiz(tenantId, req);
    }

    @Override
    public List<ApplicationHostDTO> listHostsByCloudIpv6s(String tenantId, List<String> cloudIpv6s) {
        if (CollectionUtils.isEmpty(cloudIpv6s)) {
            return Collections.emptyList();
        }
        ListHostsWithoutBizReq req = makeCmdbBaseReq(ListHostsWithoutBizReq.class);
        PropertyFilterDTO condition = new PropertyFilterDTO();
        condition.setCondition(RuleConditionEnum.OR.getCondition());
        Map<Long, List<String>> hostGroups = groupHostsByBkCloudId(cloudIpv6s);
        hostGroups.forEach((bkCloudId, ipv6s) ->
            ipv6s.forEach(ipv6 -> {
                ComposeRuleDTO hostRule = new ComposeRuleDTO(RuleConditionEnum.AND.getCondition());
                hostRule.addRule(buildCloudIdRule(bkCloudId));

                BaseRuleDTO ipv6Rule = buildIpv6Rule(ipv6);
                hostRule.addRule(ipv6Rule);
                condition.addRule(hostRule);
            }));
        req.setCondition(condition);
        return listHostsWithoutBiz(tenantId, req);
    }

    private BaseRuleDTO buildIpv6Rule(String ipv6) {
        // ipv6字段可能包含多个IPv6地址，故此处使用contains
        return BaseRuleDTO.contains("bk_host_innerip_v6", ipv6);
    }

    private BaseRuleDTO buildCloudIdRule(Long bkCloudId) {
        return BaseRuleDTO.equals("bk_cloud_id", bkCloudId);
    }

    private List<ApplicationHostDTO> listHostsWithoutBiz(String tenantId, ListHostsWithoutBizReq req) {
        int limit = 500;
        int start = 0;
        int total;
        List<ApplicationHostDTO> hosts = new ArrayList<>();
        do {
            Page page = new Page(start, limit, "");
            req.setPage(page);
            EsbResp<ListHostsWithoutBizResult> esbResp = requestCmdbApi(
                tenantId,
                HttpMethodEnum.POST,
                LIST_HOSTS_WITHOUT_BIZ,
                null,
                req,
                new TypeReference<EsbResp<ListHostsWithoutBizResult>>() {
                });
            ListHostsWithoutBizResult pageData = esbResp.getData();
            total = pageData.getCount();
            start += limit;
            if (CollectionUtils.isEmpty(esbResp.getData().getInfo())) {
                break;
            }

            hosts.addAll(pageData.getInfo().stream()
                .map(host -> convertHost(-1, host))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));

            // 设置主机业务信息
            setBizRelationInfo(tenantId, hosts);
        } while (start < total);

        return hosts;
    }

    private void setBizRelationInfo(String tenantId, List<ApplicationHostDTO> hosts) {
        List<Long> hostIds = hosts.stream().map(ApplicationHostDTO::getHostId).collect(Collectors.toList());
        List<HostBizRelationDTO> hostBizRelations = findHostBizRelations(tenantId, hostIds);
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

    private Map<Long, List<String>> groupHostsByBkCloudId(List<String> cloudIps) {
        Map<Long, List<String>> hostGroup = new HashMap<>();
        cloudIps.forEach(cloudIp -> {
            int i = cloudIp.indexOf(":");
            Long bkCloudId = Long.valueOf(cloudIp.substring(0, i));
            String ip = cloudIp.substring(i + 1);
            List<String> ipList = hostGroup.computeIfAbsent(bkCloudId, (k) -> new ArrayList<>());
            ipList.add(ip);
        });
        return hostGroup;
    }

    @Override
    public List<ApplicationHostDTO> listHostsByHostIds(String tenantId, List<Long> hostIds) {
        if (CollectionUtils.isEmpty(hostIds)) {
            return Collections.emptyList();
        }

        ListHostsWithoutBizReq req = makeCmdbBaseReq(ListHostsWithoutBizReq.class);
        PropertyFilterDTO condition = new PropertyFilterDTO();
        condition.setCondition(RuleConditionEnum.AND.getCondition());
        condition.addRule(BaseRuleDTO.in("bk_host_id", hostIds));
        req.setCondition(condition);
        return listHostsWithoutBiz(tenantId, req);
    }

    @Override
    public List<CcObjAttributeDTO> getObjAttributeList(String tenantId, String objId) {
        GetObjAttributeReq req = makeCmdbBaseReq(GetObjAttributeReq.class);
        req.setObjId(objId);
        EsbResp<List<CcObjAttributeDTO>> esbResp = requestCmdbApi(
            tenantId,
            HttpMethodEnum.POST,
            GET_OBJ_ATTRIBUTES,
            null,
            req,
            new TypeReference<EsbResp<List<CcObjAttributeDTO>>>() {
            });
        return esbResp.getData();
    }

    @Override
    public Set<String> listUsersByRole(Long bizId, String role) {
        CountInfo<Map<String, Object>> searchResult;
        GetAppReq req = makeCmdbBaseReq(GetAppReq.class);
        Map<String, Object> condition = new HashMap<>();
        condition.put("bk_biz_id", bizId);
        req.setCondition(condition);
        req.setFields(Collections.singletonList(role));
        String uri = SEARCH_BUSINESS.replace("{bk_supplier_account}", req.getBkSupplierAccount());
        EsbResp<CountInfo<Map<String, Object>>> esbResp = requestCmdbApiUseContextTenantId(
            HttpMethodEnum.POST,
            uri,
            null,
            req,
            new TypeReference<EsbResp<CountInfo<Map<String, Object>>>>() {
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
    public List<AppRoleDTO> listRoles(String tenantId) {
        List<CcObjAttributeDTO> esbObjAttributeDTO = getObjAttributeList(tenantId, "biz");
        return esbObjAttributeDTO.stream().filter(it ->
            it.getBkPropertyGroup().equals("role")
        ).map(it -> new AppRoleDTO(
            it.getBkPropertyId(),
            it.getBkPropertyName(),
            it.getCreator())
        ).collect(Collectors.toList());
    }

    @Override
    public Map<String, String> getCloudVendorIdNameMap(String tenantId) {
        List<CcObjAttributeDTO> esbObjAttributeDTO = getObjAttributeList(tenantId, "host");
        List<CcObjAttributeDTO> cloudVendorAttrList = esbObjAttributeDTO.stream().filter(it ->
            it.getBkPropertyId().equals("bk_cloud_vendor")
        ).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(cloudVendorAttrList)) {
            return Collections.emptyMap();
        }
        List<CcObjAttributeDTO.Option> optionList = parseOptionList(cloudVendorAttrList.get(0).getOption());
        Map<String, String> map = new HashMap<>();
        for (CcObjAttributeDTO.Option option : optionList) {
            map.put(option.getId(), option.getName());
        }
        return map;
    }

    private List<CcObjAttributeDTO.Option> parseOptionList(Object option) {
        return JsonUtils.fromJson(JsonUtils.toJson(option), new TypeReference<List<CcObjAttributeDTO.Option>>() {
        });
    }

    @Override
    public Map<String, String> getOsTypeIdNameMap(String tenantId) {
        List<CcObjAttributeDTO> esbObjAttributeDTO = getObjAttributeList(tenantId, "host");
        List<CcObjAttributeDTO> osTypeAttrList = esbObjAttributeDTO.stream().filter(it ->
            it.getBkPropertyId().equals("bk_os_type")
        ).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(osTypeAttrList)) {
            return Collections.emptyMap();
        }
        List<CcObjAttributeDTO.Option> optionList = parseOptionList(osTypeAttrList.get(0).getOption());
        Map<String, String> map = new HashMap<>();
        for (CcObjAttributeDTO.Option option : optionList) {
            map.put(option.getId(), option.getName());
        }
        return map;
    }

    @Override
    public List<InstanceTopologyDTO> getTopoInstancePath(GetTopoNodePathReq getTopoNodePathReq) {
        GetTopoNodePathReq req = makeCmdbBaseReq(GetTopoNodePathReq.class);

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
            String uri = GET_TOPO_NODE_PATHS.replace(
                "{bk_biz_id}",
                String.valueOf(getTopoNodePathReq.getBizId())
            );
            EsbResp<List<TopoNodePathDTO>> esbResp = requestCmdbApiUseContextTenantId(
                HttpMethodEnum.POST,
                uri,
                null,
                req,
                new TypeReference<EsbResp<List<TopoNodePathDTO>>>() {
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
                    List<InstanceTopologyDTO> parentNodeList = nodePath.getTopoPaths().get(0);
                    if (!CollectionUtils.isEmpty(parentNodeList)) {
                        Collections.reverse(parentNodeList);
                        parentNodeList.forEach(hierarchyTopo::addParent);
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
    public ResourceWatchResult<HostEventDetail> getHostEvents(String tenantId, Long startTime, String cursor) {
        ResourceWatchReq req = makeCmdbBaseReq(ResourceWatchReq.class);
        req.setFields(Arrays.asList("bk_host_id", "bk_host_innerip", "bk_host_innerip_v6", "bk_agent_id",
            "bk_host_name", "bk_os_name", "bk_os_type", "bk_cloud_id", "bk_cloud_vendor", "last_time"));
        req.setResource("host");
        req.setCursor(cursor);
        req.setStartTime(startTime);
        String uri = RESOURCE_WATCH.replace("{bk_resource}", req.getResource());
        EsbResp<ResourceWatchResult<HostEventDetail>> esbResp = requestCmdbApi(
            tenantId,
            HttpMethodEnum.POST,
            uri,
            null,
            req,
            new TypeReference<EsbResp<ResourceWatchResult<HostEventDetail>>>() {
            },
            HttpHelperFactory.getLongRetryableHttpHelper());
        return esbResp.getData();
    }

    @Override
    public ResourceWatchResult<HostRelationEventDetail> getHostRelationEvents(String tenantId,
                                                                              Long startTime,
                                                                              String cursor) {
        ResourceWatchReq req = makeCmdbBaseReq(ResourceWatchReq.class);
        req.setFields(Arrays.asList("bk_host_id", "bk_biz_id", "bk_set_id", "bk_module_id", "last_time"));
        req.setResource("host_relation");
        req.setCursor(cursor);
        req.setStartTime(startTime);
        String uri = RESOURCE_WATCH.replace("{bk_resource}", req.getResource());
        EsbResp<ResourceWatchResult<HostRelationEventDetail>> esbResp = requestCmdbApi(
            tenantId,
            HttpMethodEnum.POST,
            uri,
            null,
            req,
            new TypeReference<EsbResp<ResourceWatchResult<HostRelationEventDetail>>>() {
            },
            HttpHelperFactory.getLongRetryableHttpHelper());
        return esbResp.getData();
    }

    @Override
    public ResourceWatchResult<BizEventDetail> getAppEvents(String tenantId, Long startTime, String cursor) {
        ResourceWatchReq req = makeCmdbBaseReq(ResourceWatchReq.class);
        req.setFields(Arrays.asList("bk_biz_id", "bk_biz_name", "bk_supplier_account",
            "time_zone", "language", "default"));
        req.setResource("biz");
        req.setCursor(cursor);
        req.setStartTime(startTime);
        String uri = RESOURCE_WATCH.replace("{bk_resource}", req.getResource());
        EsbResp<ResourceWatchResult<BizEventDetail>> esbResp = requestCmdbApi(
            tenantId,
            HttpMethodEnum.POST,
            uri,
            null,
            req,
            new TypeReference<EsbResp<ResourceWatchResult<BizEventDetail>>>() {
            },
            HttpHelperFactory.getLongRetryableHttpHelper());
        return esbResp.getData();
    }

    class FindModuleHostRelationTask implements Runnable {
        // 租户ID
        String tenantId;
        // 结果队列
        LinkedBlockingQueue<HostWithModules> resultQueue;
        FindModuleHostRelationReq req;
        String requestId;

        FindModuleHostRelationTask(String tenantId,
                                   LinkedBlockingQueue<HostWithModules> resultQueue,
                                   FindModuleHostRelationReq req,
                                   String requestId) {
            this.tenantId = tenantId;
            this.resultQueue = resultQueue;
            this.req = req;
            this.requestId = requestId;
        }

        @Override
        public void run() {
            JobContextUtil.setRequestId(requestId);
            try {
                resultQueue.addAll(getHostsByReq(tenantId, req).getRelation());
            } catch (Exception e) {
                log.error("FindModuleHostRelationTask fail:", e);
            }
        }
    }

    /**
     * 根据业务 ID 查询容器拓扑（缓存)
     *
     * @param bizId 业务 ID
     * @return 容器拓扑
     */
    @Override
    public KubeTopologyDTO getBizKubeCacheTopo(long bizId) {
        GetBizKubeCacheTopoReq req = makeCmdbBaseReq(GetBizKubeCacheTopoReq.class);
        req.setBizId(bizId);

        EsbResp<KubeTopologyDTO> esbResp = requestCmdbApiUseContextTenantId(
            HttpMethodEnum.POST,
            GET_BIZ_KUBE_CACHE_TOPO,
            null,
            req,
            new TypeReference<EsbResp<KubeTopologyDTO>>() {
            });
        return esbResp.getData();
    }

    /**
     * 根据容器拓扑获取container信息(分页)
     *
     * @param req 请求
     * @return 容器列表（分页）
     */
    @Override
    public PageData<ContainerDetailDTO> listPageKubeContainerByTopo(ListKubeContainerByTopoReq req) {
        return listPageKubeContainerByTopo(req, true);
    }

    private PageData<ContainerDetailDTO> listPageKubeContainerByTopo(ListKubeContainerByTopoReq req,
                                                                     boolean withCount) {
        setSupplierAccount(req);
        req.setContainerFields(ContainerDTO.Fields.ALL);
        req.setPodFields(PodDTO.Fields.ALL);

        return listPage(
            req,
            withCount,
            cmdbPageReq -> requestCmdbApiUseContextTenantId(
                HttpMethodEnum.POST,
                LIST_KUBE_CONTAINER_BY_TOPO,
                null,
                cmdbPageReq,
                new TypeReference<EsbResp<BaseCcSearchResult<ContainerDetailDTO>>>() {
                })
        );
    }

    /**
     * 根据容器拓扑获取container信息
     *
     * @param req 请求
     * @return 容器列表
     */
    @Override
    public List<ContainerDetailDTO> listKubeContainerByTopo(ListKubeContainerByTopoReq req) {
        setSupplierAccount(req);
        req.setContainerFields(ContainerDTO.Fields.ALL);
        req.setPodFields(PodDTO.Fields.ALL);
        // 根据容器 ID 升序排列返回的数据，避免由于分页查询期间数据变更导致返回数据重复或者遗漏
        req.setPage(new Page(0, 500, ContainerDTO.Fields.ID));

        if (req.getNodeIdList() == null || req.getNodeIdList().size() <= 200) {
            return loopPageListKubeContainerByTopo(req);
        } else {
            // 超过 cmdb API 单次查询最大 node 数量限制，需要按照拓扑节点分批
            List<ListKubeContainerByTopoReq> batchReqs = partitionListKubeContainerByTopoReq(req);
            return batchReqs.stream()
                .flatMap(batchReq -> loopPageListKubeContainerByTopo(batchReq).stream())
                .distinct()
                .collect(Collectors.toList());
        }
    }

    private List<ContainerDetailDTO> loopPageListKubeContainerByTopo(ListKubeContainerByTopoReq req) {
        return PageUtil.queryAllWithLoopPageQueryInOrder(
            500,
            (ContainerDetailDTO latestElement) -> {
                if (latestElement == null) {
                    // 第一页使用原始的请求
                    return req;
                } else {
                    // 从第二页开始，需要构造 offset 条件，避免由于分页查询期间数据变更导致返回数据重复或者遗漏
                    return buildNextPageListKubeContainerByTopoReq(req, latestElement.getContainer().getId());
                }
            },
            pageReq -> listPageKubeContainerByTopo(pageReq, false),
            PageData::getData,
            container -> container
        );
    }

    private List<ListKubeContainerByTopoReq> partitionListKubeContainerByTopoReq(ListKubeContainerByTopoReq req) {
        List<ListKubeContainerByTopoReq> reqs = new ArrayList<>();
        List<List<KubeNodeID>> nodeIDListBatches =
            CollectionUtil.partitionCollection(req.getNodeIdList(), 200);
        nodeIDListBatches.forEach(nodeIDListBatch -> {
            ListKubeContainerByTopoReq newReq = new ListKubeContainerByTopoReq();
            newReq.setBkSupplierAccount(req.getBkSupplierAccount());
            newReq.setBizId(req.getBizId());
            newReq.setNodeIdList(nodeIDListBatch);
            newReq.setContainerFilter(req.getContainerFilter());
            newReq.setPodFilter(req.getPodFilter());
            newReq.setPage(req.getPage());
            newReq.setContainerFields(req.getContainerFields());
            newReq.setPodFields(req.getPodFields());
            reqs.add(newReq);
        });
        return reqs;
    }

    private ListKubeContainerByTopoReq buildNextPageListKubeContainerByTopoReq(
        ListKubeContainerByTopoReq originReq,
        long lastIdForCurrentPage
    ) {
        ListKubeContainerByTopoReq nextPageReq = new ListKubeContainerByTopoReq();
        nextPageReq.setPage(originReq.getPage());
        nextPageReq.setNodeIdList(originReq.getNodeIdList());
        nextPageReq.setPodFilter(originReq.getPodFilter());
        nextPageReq.setBizId(originReq.getBizId());
        nextPageReq.setBkSupplierAccount(originReq.getBkSupplierAccount());
        nextPageReq.setPodFields(originReq.getPodFields());
        nextPageReq.setContainerFields(originReq.getContainerFields());

        // 添加 container ID 作为分页查询 offset 条件
        PropertyFilterDTO rewriteContainerPropertyFilter = addRuleThenReCreate(originReq.getContainerFilter(),
            BaseRuleDTO.greaterThan(KubeNamespaceDTO.Fields.ID, lastIdForCurrentPage));
        nextPageReq.setContainerFilter(rewriteContainerPropertyFilter);

        return nextPageReq;
    }

    private void setSupplierAccount(EsbReq esbReq) {
        if (StringUtils.isEmpty(cmdbSupplierAccount)) {
            esbReq.setBkSupplierAccount("0");
        } else {
            esbReq.setBkSupplierAccount(cmdbSupplierAccount);
        }
    }

    /**
     * 根据容器ID批量查询容器
     *
     * @param bizId        CMDB 业务 ID
     * @param containerIds 容器 ID 集合
     * @return 容器列表
     */
    @Override
    public List<ContainerDetailDTO> listKubeContainerByIds(long bizId, Collection<Long> containerIds) {
        return listBizKubeContainerByContainerFieldWithInCondition(
            bizId, ContainerDTO.Fields.ID, containerIds);
    }

    /**
     * 根据容器ID批量查询容器
     *
     * @param bizId         CMDB 业务 ID
     * @param containerUIds 容器 UID 集合
     * @return 容器列表
     */
    @Override
    public List<ContainerDetailDTO> listKubeContainerByUIds(long bizId, Collection<String> containerUIds) {
        return listBizKubeContainerByContainerFieldWithInCondition(
            bizId, ContainerDTO.Fields.CONTAINER_UID, containerUIds);
    }

    private <T> List<ContainerDetailDTO> listBizKubeContainerByContainerFieldWithInCondition(
        long bizId,
        String containerField,
        Collection<T> fieldValues) {

        int maxFieldValuesPerBatch = 500;  // cmdb 限制每次查询传入的字段值
        List<List<T>> containerFieldValueBatches =
            CollectionUtil.partitionCollection(fieldValues, maxFieldValuesPerBatch);

        List<ContainerDetailDTO> containers = new ArrayList<>();

        containerFieldValueBatches.forEach(containerFieldValueBatch -> {
            ListKubeContainerByTopoReq req = makeCmdbBaseReq(ListKubeContainerByTopoReq.class);

            // 查询条件
            req.setBizId(bizId);
            PropertyFilterDTO containerFilter = new PropertyFilterDTO();
            containerFilter.setCondition(RuleConditionEnum.AND.getCondition());
            containerFilter.addRule(BaseRuleDTO.in(containerField, containerFieldValueBatch));
            req.setContainerFilter(containerFilter);

            // 返回参数设置
            req.setContainerFields(ContainerDTO.Fields.ALL);
            req.setPodFields(PodDTO.Fields.ALL);

            // 分页设置
            req.setPage(new Page(0, maxFieldValuesPerBatch));

            PageData<ContainerDetailDTO> pageData = listPageKubeContainerByTopo(req, false);
            if (CollectionUtils.isNotEmpty(pageData.getData())) {
                containers.addAll(pageData.getData());
            }
        });

        return containers;
    }

    @Override
    public List<KubeClusterDTO> listKubeClusters(KubeClusterQuery query) {
        ListKubeClusterReq req = makeCmdbBaseReq(ListKubeClusterReq.class);

        // 查询条件
        req.setBizId(query.getBizId());

        PropertyFilterDTO clusterPropFilter = new PropertyFilterDTO();
        clusterPropFilter.setCondition(RuleConditionEnum.AND.getCondition());
        if (CollectionUtils.isNotEmpty(query.getIds())) {
            clusterPropFilter.addRule(BaseRuleDTO.in(KubeClusterDTO.Fields.ID, query.getIds()));
        }
        if (CollectionUtils.isNotEmpty(query.getBkClusterUIDs())) {
            clusterPropFilter.addRule(BaseRuleDTO.in(KubeClusterDTO.Fields.CLUSTER_UID, query.getBkClusterUIDs()));
        }
        if (clusterPropFilter.hasRule()) {
            req.setFilter(clusterPropFilter);
        }

        // 返回参数设置
        req.setFields(KubeClusterDTO.Fields.ALL);

        // 根据集群 ID 升序排列返回的数据，避免由于分页查询期间数据变更导致返回数据重复或者遗漏
        req.setPage(new Page(0, 500, KubeClusterDTO.Fields.ID));

        return PageUtil.queryAllWithLoopPageQueryInOrder(
            500,
            (KubeClusterDTO latestElement) -> {
                if (latestElement == null) {
                    // 第一页使用原始的请求
                    return req;
                } else {
                    // 从第二页开始，需要构造 offset 条件，避免由于分页查询期间数据变更导致返回数据重复或者遗漏
                    return buildNextPageListKubeClusterReq(req, latestElement.getId());
                }
            },
            cmdbPageReq -> requestCmdbApiUseContextTenantId(
                HttpMethodEnum.POST,
                LIST_KUBE_CLUSTER,
                null,
                cmdbPageReq,
                new TypeReference<EsbResp<BaseCcSearchResult<KubeClusterDTO>>>() {
                }),
            resp -> resp.getData().getInfo(),
            cluster -> cluster
        );
    }

    private ListKubeClusterReq buildNextPageListKubeClusterReq(ListKubeClusterReq originReq,
                                                               long lastIdForCurrentPage) {
        ListKubeClusterReq nextPageReq = new ListKubeClusterReq();
        nextPageReq.setPage(originReq.getPage());
        nextPageReq.setBizId(originReq.getBizId());
        nextPageReq.setFields(originReq.getFields());
        nextPageReq.setBkSupplierAccount(originReq.getBkSupplierAccount());

        // 添加 cluster ID 作为分页查询 offset 条件
        PropertyFilterDTO rewritePropertyFilter = addRuleThenReCreate(originReq.getFilter(),
            BaseRuleDTO.greaterThan(KubeClusterDTO.Fields.ID, lastIdForCurrentPage));
        nextPageReq.setFilter(rewritePropertyFilter);

        return nextPageReq;
    }

    private <R> PageData<R> listPage(CmdbPageReq req,
                                     boolean withCount,
                                     Function<CmdbPageReq, EsbResp<BaseCcSearchResult<R>>> query) {
        setSupplierAccount(req);

        Page originPage = req.getPage();
        long count = 0;
        if (withCount) {
            // 设置为查询总数量的分页条件
            req.setPage(Page.buildQueryCountPage());

            // 查询总数量
            EsbResp<BaseCcSearchResult<R>> response = query.apply(req);
            count = response.getData().getCount();
        }

        // 查询数据
        req.setPage(originPage);
        EsbResp<BaseCcSearchResult<R>> response = query.apply(req);
        return new PageData<>(originPage.getStart(), originPage.getLimit(), count, response.getData().getInfo());
    }

    @Override
    public List<KubeNamespaceDTO> listKubeNamespaces(NamespaceQuery query) {
        ListKubeNamespaceReq req = makeCmdbBaseReq(ListKubeNamespaceReq.class);

        // 查询条件
        req.setBizId(query.getBizId());

        PropertyFilterDTO namespacePropFilter = new PropertyFilterDTO();
        namespacePropFilter.setCondition(RuleConditionEnum.AND.getCondition());
        if (CollectionUtils.isNotEmpty(query.getIds())) {
            namespacePropFilter.addRule(BaseRuleDTO.in(KubeNamespaceDTO.Fields.ID, query.getIds()));
        }
        if (CollectionUtils.isNotEmpty(query.getBkClusterIds())) {
            namespacePropFilter.addRule(BaseRuleDTO.in(KubeNamespaceDTO.Fields.BK_CLUSTER_ID, query.getBkClusterIds()));
        }
        if (CollectionUtils.isNotEmpty(query.getNames())) {
            namespacePropFilter.addRule(BaseRuleDTO.in(KubeNamespaceDTO.Fields.NAME, query.getNames()));
        }
        if (namespacePropFilter.hasRule()) {
            req.setFilter(namespacePropFilter);
        }

        // 返回参数设置
        req.setFields(KubeNamespaceDTO.Fields.ALL);

        // 根据 namespace ID 升序排列返回的数据，避免由于分页查询期间数据变更导致返回数据重复或者遗漏
        req.setPage(new Page(0, 500, KubeNamespaceDTO.Fields.ID));

        return PageUtil.queryAllWithLoopPageQueryInOrder(
            500,
            (KubeNamespaceDTO latestElement) -> {
                if (latestElement == null) {
                    // 第一页使用原始的请求
                    return req;
                } else {
                    // 从第二页开始，需要构造 offset 条件，避免由于分页查询期间数据变更导致返回数据重复或者遗漏
                    return buildNextPageListKubeNamespaceReq(req, latestElement.getId());
                }
            },
            cmdbPageReq -> requestCmdbApiUseContextTenantId(
                HttpMethodEnum.POST,
                LIST_KUBE_NAMESPACE,
                null,
                cmdbPageReq,
                new TypeReference<EsbResp<BaseCcSearchResult<KubeNamespaceDTO>>>() {
                }),
            resp -> resp.getData().getInfo(),
            namespace -> namespace
        );
    }

    private ListKubeNamespaceReq buildNextPageListKubeNamespaceReq(ListKubeNamespaceReq originReq,
                                                                   long lastIdForCurrentPage) {
        ListKubeNamespaceReq nextPageReq = new ListKubeNamespaceReq();
        nextPageReq.setPage(originReq.getPage());
        nextPageReq.setBizId(originReq.getBizId());
        nextPageReq.setFields(originReq.getFields());
        nextPageReq.setBkSupplierAccount(originReq.getBkSupplierAccount());

        // 添加 namespace ID 作为分页查询 offset 条件
        PropertyFilterDTO rewritePropertyFilter = addRuleThenReCreate(originReq.getFilter(),
            BaseRuleDTO.greaterThan(KubeNamespaceDTO.Fields.ID, lastIdForCurrentPage));
        nextPageReq.setFilter(rewritePropertyFilter);

        return nextPageReq;
    }

    @Override
    public List<KubeWorkloadDTO> listKubeWorkloads(WorkloadQuery query) {
        ListKubeWorkloadReq req = makeCmdbBaseReq(ListKubeWorkloadReq.class);

        // 查询条件
        req.setBizId(query.getBizId());
        req.setKind(query.getKind());
        // 根据 Workload ID 升序排列返回的数据，避免由于分页查询期间数据变更导致返回数据重复或者遗漏
        req.setPage(new Page(0, 500, KubeWorkloadDTO.Fields.ID));

        PropertyFilterDTO workloadPropFilter = new PropertyFilterDTO();
        workloadPropFilter.setCondition(RuleConditionEnum.AND.getCondition());
        if (CollectionUtils.isNotEmpty(query.getIds())) {
            workloadPropFilter.addRule(BaseRuleDTO.in(KubeWorkloadDTO.Fields.ID, query.getIds()));
        }
        if (CollectionUtils.isNotEmpty(query.getBkClusterIds())) {
            workloadPropFilter.addRule(BaseRuleDTO.in(KubeWorkloadDTO.Fields.BK_CLUSTER_ID, query.getBkClusterIds()));
        }
        if (CollectionUtils.isNotEmpty(query.getBkNamespaceIds())) {
            workloadPropFilter.addRule(BaseRuleDTO.in(KubeWorkloadDTO.Fields.BK_NAMESPACE_ID,
                query.getBkNamespaceIds()));
        }
        if (CollectionUtils.isNotEmpty(query.getNames())) {
            workloadPropFilter.addRule(BaseRuleDTO.in(KubeWorkloadDTO.Fields.NAME, query.getNames()));
        }
        if (workloadPropFilter.hasRule()) {
            req.setFilter(workloadPropFilter);
        }

        // 返回参数设置
        req.setFields(KubeWorkloadDTO.Fields.ALL);

        String requestUrl = LIST_KUBE_WORKLOAD.replace("{kind}", query.getKind());

        return PageUtil.queryAllWithLoopPageQueryInOrder(
            500,
            (KubeWorkloadDTO latestElement) -> {
                if (latestElement == null) {
                    // 第一页使用原始的请求
                    return req;
                } else {
                    // 从第二页开始，需要构造 offset 条件，避免由于分页查询期间数据变更导致返回数据重复或者遗漏
                    return buildNextPageListKubeWorkloadReq(req, latestElement.getId());
                }
            },
            cmdbPageReq -> requestCmdbApiUseContextTenantId(
                HttpMethodEnum.POST,
                requestUrl,
                null,
                cmdbPageReq,
                new TypeReference<EsbResp<BaseCcSearchResult<KubeWorkloadDTO>>>() {
                }),
            resp -> resp.getData().getInfo(),
            workload -> {
                // cmdb API 返回的数据没有包含 kind 信息，需要补全
                workload.setKind(req.getKind());
                return workload;
            }
        );
    }

    private ListKubeWorkloadReq buildNextPageListKubeWorkloadReq(ListKubeWorkloadReq originReq,
                                                                 long lastIdForCurrentPage) {
        ListKubeWorkloadReq nextPageReq = new ListKubeWorkloadReq();
        nextPageReq.setPage(originReq.getPage());
        nextPageReq.setBkSupplierAccount(originReq.getBkSupplierAccount());
        nextPageReq.setKind(originReq.getKind());
        nextPageReq.setBizId(originReq.getBizId());
        nextPageReq.setFields(originReq.getFields());

        // 添加 Workload ID 作为分页查询 offset 条件
        PropertyFilterDTO rewritePropertyFilter = addRuleThenReCreate(originReq.getFilter(),
            BaseRuleDTO.greaterThan(KubeWorkloadDTO.Fields.ID, lastIdForCurrentPage));
        nextPageReq.setFilter(rewritePropertyFilter);

        return nextPageReq;
    }

    private PropertyFilterDTO addRuleThenReCreate(PropertyFilterDTO originPropertyFilter, IRule rule) {
        PropertyFilterDTO newPropertyFilter = new PropertyFilterDTO();
        newPropertyFilter.setCondition(RuleConditionEnum.AND.getCondition());
        List<IRule> newRules = new ArrayList<>();
        if (originPropertyFilter != null) {
            if (originPropertyFilter.getCondition().equals(RuleConditionEnum.OR.getCondition())) {
                // 目前场景下属性过滤仅支持 AND
                throw new IllegalStateException("Unexpected condition for property filter");
            }
            newRules.addAll(originPropertyFilter.getRules());
        }
        newRules.add(rule);
        newPropertyFilter.setRules(newRules);

        return newPropertyFilter;
    }
}
