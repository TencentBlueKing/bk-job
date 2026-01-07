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

import com.tencent.bk.job.common.cc.model.AppRoleDTO;
import com.tencent.bk.job.common.cc.model.CcCloudAreaInfoDTO;
import com.tencent.bk.job.common.cc.model.CcDynamicGroupDTO;
import com.tencent.bk.job.common.cc.model.CcInstanceDTO;
import com.tencent.bk.job.common.cc.model.CcObjAttributeDTO;
import com.tencent.bk.job.common.cc.model.DynamicGroupHostPropDTO;
import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
import com.tencent.bk.job.common.cc.model.container.ContainerDetailDTO;
import com.tencent.bk.job.common.cc.model.container.KubeClusterDTO;
import com.tencent.bk.job.common.cc.model.container.KubeNamespaceDTO;
import com.tencent.bk.job.common.cc.model.container.KubeTopologyDTO;
import com.tencent.bk.job.common.cc.model.container.KubeWorkloadDTO;
import com.tencent.bk.job.common.cc.model.query.KubeClusterQuery;
import com.tencent.bk.job.common.cc.model.query.NamespaceQuery;
import com.tencent.bk.job.common.cc.model.query.WorkloadQuery;
import com.tencent.bk.job.common.cc.model.req.GetTopoNodePathReq;
import com.tencent.bk.job.common.cc.model.req.ListKubeContainerByTopoReq;
import com.tencent.bk.job.common.cc.model.result.BizEventDetail;
import com.tencent.bk.job.common.cc.model.result.HostBizRelationDTO;
import com.tencent.bk.job.common.cc.model.result.HostEventDetail;
import com.tencent.bk.job.common.cc.model.result.HostRelationEventDetail;
import com.tencent.bk.job.common.cc.model.result.HostWithModules;
import com.tencent.bk.job.common.cc.model.result.ResourceWatchResult;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.retry.ExponentialBackoffRetryPolicy;
import com.tencent.bk.job.common.retry.RetryExecutor;
import com.tencent.bk.job.common.retry.metrics.RetryMetricsConstants;
import com.tencent.bk.job.common.retry.metrics.RetryMetricsRecorder;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 支持重试的 CMDB 业务客户端
 * <p>
 * 使用代理模式，对原有的 IBizCmdbClient 进行封装，为幂等查询接口添加指数退避重试能力
 * </p>
 */
@Slf4j
public class RetryableBizCmdbClient implements IBizCmdbClient {

    private final IBizCmdbClient delegate;
    private final RetryExecutor retryExecutor;

    public RetryableBizCmdbClient(IBizCmdbClient delegate,
                                  ExponentialBackoffRetryPolicy retryPolicy,
                                  RetryMetricsRecorder metricsRecorder) {
        this.delegate = delegate;
        this.retryExecutor = new RetryExecutor(
            retryPolicy,
            metricsRecorder,
            RetryMetricsConstants.TAG_VALUE_SYSTEM_CMDB
        );
    }

    @Override
    public InstanceTopologyDTO getBizInstTopology(String tenantId, long bizId) {
        return retryExecutor.executeWithRetry(
            () -> delegate.getBizInstTopology(tenantId, bizId),
            "getBizInstTopology"
        );
    }

    @Override
    public InstanceTopologyDTO getBizInstCompleteTopology(String tenantId, long bizId) {
        return retryExecutor.executeWithRetry(
            () -> delegate.getBizInstCompleteTopology(tenantId, bizId),
            "getBizInstCompleteTopology"
        );
    }

    @Override
    public InstanceTopologyDTO getBizInternalModule(String tenantId, long bizId) {
        return retryExecutor.executeWithRetry(
            () -> delegate.getBizInternalModule(tenantId, bizId),
            "getBizInternalModule"
        );
    }

    @Override
    public List<ApplicationHostDTO> getHosts(String tenantId, long bizId, List<CcInstanceDTO> ccInstList) {
        return retryExecutor.executeWithRetry(
            () -> delegate.getHosts(tenantId, bizId, ccInstList),
            "getHosts"
        );
    }

    @Override
    public List<HostWithModules> getHostRelationsByTopology(String tenantId, long bizId,
                                                            List<CcInstanceDTO> ccInstList) {
        return retryExecutor.executeWithRetry(
            () -> delegate.getHostRelationsByTopology(tenantId, bizId, ccInstList),
            "getHostRelationsByTopology"
        );
    }

    @Override
    public List<HostWithModules> findHostRelationByModule(String tenantId, long bizId, List<Long> moduleIdList) {
        return retryExecutor.executeWithRetry(
            () -> delegate.findHostRelationByModule(tenantId, bizId, moduleIdList),
            "findHostRelationByModule"
        );
    }

    @Override
    public List<ApplicationDTO> getAllBizApps(String tenantId) {
        return retryExecutor.executeWithRetry(
            () -> delegate.getAllBizApps(tenantId),
            "getAllBizApps"
        );
    }

    @Override
    public List<ApplicationDTO> listBizAppByIds(String tenantId, List<Long> bizIds) {
        return retryExecutor.executeWithRetry(
            () -> delegate.listBizAppByIds(tenantId, bizIds),
            "listBizAppByIds"
        );
    }

    @Override
    public List<CcDynamicGroupDTO> getDynamicGroupList(String tenantId, long bizId) {
        return retryExecutor.executeWithRetry(
            () -> delegate.getDynamicGroupList(tenantId, bizId),
            "getDynamicGroupList"
        );
    }

    @Override
    public List<DynamicGroupHostPropDTO> getDynamicGroupIp(String tenantId, long bizId, String groupId) {
        return retryExecutor.executeWithRetry(
            () -> delegate.getDynamicGroupIp(tenantId, bizId, groupId),
            "getDynamicGroupIp"
        );
    }

    @Override
    public List<CcCloudAreaInfoDTO> getCloudAreaList(String tenantId) {
        return retryExecutor.executeWithRetry(
            () -> delegate.getCloudAreaList(tenantId),
            "getCloudAreaList"
        );
    }

    @Override
    public List<ApplicationHostDTO> listHostsByCloudIps(String tenantId, List<String> cloudIps) {
        return retryExecutor.executeWithRetry(
            () -> delegate.listHostsByCloudIps(tenantId, cloudIps),
            "listHostsByCloudIps"
        );
    }

    @Override
    public List<ApplicationHostDTO> listHostsByCloudIpv6s(String tenantId, List<String> cloudIpv6s) {
        return retryExecutor.executeWithRetry(
            () -> delegate.listHostsByCloudIpv6s(tenantId, cloudIpv6s),
            "listHostsByCloudIpv6s"
        );
    }

    @Override
    public List<ApplicationHostDTO> listHostsByHostIds(String tenantId, List<Long> hostIds) {
        return retryExecutor.executeWithRetry(
            () -> delegate.listHostsByHostIds(tenantId, hostIds),
            "listHostsByHostIds"
        );
    }

    @Override
    public List<HostBizRelationDTO> findHostBizRelations(String tenantId, List<Long> hostIdList) {
        return retryExecutor.executeWithRetry(
            () -> delegate.findHostBizRelations(tenantId, hostIdList),
            "findHostBizRelations"
        );
    }

    @Override
    public List<CcObjAttributeDTO> getObjAttributeList(String tenantId, String objId) {
        return retryExecutor.executeWithRetry(
            () -> delegate.getObjAttributeList(tenantId, objId),
            "getObjAttributeList"
        );
    }

    @Override
    public Set<String> listUsersByRole(String tenantId, Long bizId, String role) {
        return retryExecutor.executeWithRetry(
            () -> delegate.listUsersByRole(tenantId, bizId, role),
            "listUsersByRole"
        );
    }

    @Override
    public List<AppRoleDTO> listRoles(String tenantId) {
        return retryExecutor.executeWithRetry(
            () -> delegate.listRoles(tenantId),
            "listRoles"
        );
    }

    @Override
    public Map<String, String> getCloudVendorIdNameMap(String tenantId) {
        return retryExecutor.executeWithRetry(
            () -> delegate.getCloudVendorIdNameMap(tenantId),
            "getCloudVendorIdNameMap"
        );
    }

    @Override
    public Map<String, String> getOsTypeIdNameMap(String tenantId) {
        return retryExecutor.executeWithRetry(
            () -> delegate.getOsTypeIdNameMap(tenantId),
            "getOsTypeIdNameMap"
        );
    }

    @Override
    public List<InstanceTopologyDTO> getTopoInstancePath(GetTopoNodePathReq getTopoNodePathReq) {
        return retryExecutor.executeWithRetry(
            () -> delegate.getTopoInstancePath(getTopoNodePathReq),
            "getTopoInstancePath"
        );
    }

    @Override
    public ResourceWatchResult<HostEventDetail> getHostEvents(String tenantId, Long startTime, String cursor) {
        return retryExecutor.executeWithRetry(
            () -> delegate.getHostEvents(tenantId, startTime, cursor),
            "getHostEvents"
        );
    }

    @Override
    public ResourceWatchResult<HostRelationEventDetail> getHostRelationEvents(String tenantId, Long startTime,
                                                                              String cursor) {
        return retryExecutor.executeWithRetry(
            () -> delegate.getHostRelationEvents(tenantId, startTime, cursor),
            "getHostRelationEvents"
        );
    }

    @Override
    public ResourceWatchResult<BizEventDetail> getBizEvents(String tenantId, Long startTime, String cursor) {
        return retryExecutor.executeWithRetry(
            () -> delegate.getBizEvents(tenantId, startTime, cursor),
            "getBizEvents"
        );
    }

    @Override
    public List<ContainerDetailDTO> listKubeContainerByIds(long bizId, Collection<Long> containerIds) {
        return retryExecutor.executeWithRetry(
            () -> delegate.listKubeContainerByIds(bizId, containerIds),
            "listKubeContainerByIds"
        );
    }

    @Override
    public List<ContainerDetailDTO> listKubeContainerByTopo(ListKubeContainerByTopoReq req) {
        return retryExecutor.executeWithRetry(
            () -> delegate.listKubeContainerByTopo(req),
            "listKubeContainerByTopo"
        );
    }

    @Override
    public List<KubeClusterDTO> listKubeClusters(KubeClusterQuery query) {
        return retryExecutor.executeWithRetry(
            () -> delegate.listKubeClusters(query),
            "listKubeClusters"
        );
    }

    @Override
    public List<KubeNamespaceDTO> listKubeNamespaces(NamespaceQuery query) {
        return retryExecutor.executeWithRetry(
            () -> delegate.listKubeNamespaces(query),
            "listKubeNamespaces"
        );
    }

    @Override
    public List<KubeWorkloadDTO> listKubeWorkloads(WorkloadQuery query) {
        return retryExecutor.executeWithRetry(
            () -> delegate.listKubeWorkloads(query),
            "listKubeWorkloads"
        );
    }

    @Override
    public KubeTopologyDTO getBizKubeCacheTopo(long bizId) {
        return retryExecutor.executeWithRetry(
            () -> delegate.getBizKubeCacheTopo(bizId),
            "getBizKubeCacheTopo"
        );
    }

    @Override
    public PageData<ContainerDetailDTO> listPageKubeContainerByTopo(ListKubeContainerByTopoReq req) {
        return retryExecutor.executeWithRetry(
            () -> delegate.listPageKubeContainerByTopo(req),
            "listPageKubeContainerByTopo"
        );
    }

    @Override
    public List<ContainerDetailDTO> listKubeContainerByUIds(long bizId, Collection<String> containerUIds) {
        return retryExecutor.executeWithRetry(
            () -> delegate.listKubeContainerByUIds(bizId, containerUIds),
            "listKubeContainerByUIds"
        );
    }
}
