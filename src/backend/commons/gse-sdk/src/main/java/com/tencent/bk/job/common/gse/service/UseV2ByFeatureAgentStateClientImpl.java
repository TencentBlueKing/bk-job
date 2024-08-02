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

package com.tencent.bk.job.common.gse.service;

import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.gse.service.model.HostAgentStateQuery;
import com.tencent.bk.job.common.gse.util.AgentStateUtil;
import com.tencent.bk.job.common.gse.v2.model.resp.AgentState;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.common.util.toggle.ToggleEvaluateContext;
import com.tencent.bk.job.common.util.toggle.ToggleStrategyContextParams;
import com.tencent.bk.job.common.util.toggle.feature.FeatureIdConstants;
import com.tencent.bk.job.common.util.toggle.feature.FeatureToggle;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class UseV2ByFeatureAgentStateClientImpl implements AgentStateClient {

    private final BizHostInfoQueryService bizHostInfoQueryService;
    private final SingleChannelAgentStateClientImpl gseV1AgentStateClient;
    private final SingleChannelAgentStateClientImpl gseV2AgentStateClient;

    public UseV2ByFeatureAgentStateClientImpl(GseV1AgentStateClientImpl gseV1AgentStateClient,
                                              GseV2AgentStateClientImpl gseV2AgentStateClient,
                                              BizHostInfoQueryService bizHostInfoQueryService) {
        this.gseV1AgentStateClient = gseV1AgentStateClient;
        this.gseV2AgentStateClient = gseV2AgentStateClient;
        this.bizHostInfoQueryService = bizHostInfoQueryService;
    }

    @Override
    public String getEffectiveAgentId(HostAgentStateQuery hostAgentStateQuery) {
        if (hostAgentStateQuery.getBizId() == null) {
            fillBizIdByHostId(hostAgentStateQuery);
        }
        if (needToUseGseV2(hostAgentStateQuery)) {
            fillAgentIdIfNeed(hostAgentStateQuery);
            return hostAgentStateQuery.getAgentId();
        }
        fillCloudIpIfNeed(hostAgentStateQuery);
        return hostAgentStateQuery.getCloudIp();
    }

    @Override
    public AgentState getAgentState(HostAgentStateQuery hostAgentStateQuery) {
        // 填充需要的字段
        String effectiveAgentId = getEffectiveAgentId(hostAgentStateQuery);
        if (StringUtils.isBlank(effectiveAgentId)) {
            return null;
        }
        if (needToUseGseV2(hostAgentStateQuery)) {
            return gseV2AgentStateClient.getAgentState(hostAgentStateQuery);
        }
        return gseV1AgentStateClient.getAgentState(hostAgentStateQuery);
    }

    private void fillBizIdByHostId(HostAgentStateQuery hostAgentStateQuery) {
        Long hostId = hostAgentStateQuery.getHostId();
        Map<Long, Long> hostIdBizIdMap = bizHostInfoQueryService.queryBizIdsByHostId(
            Collections.singletonList(hostId)
        );
        Long bizId = hostIdBizIdMap.get(hostId);
        if (log.isDebugEnabled()) {
            log.debug("queryBizIdByHostId, hostId={}, bizId={}", hostId, bizId);
        }
        hostAgentStateQuery.setBizId(bizId);
    }

    private void fillAgentIdIfNeed(HostAgentStateQuery hostAgentStateQuery) {
        if (StringUtils.isNotBlank(hostAgentStateQuery.getAgentId())) {
            return;
        }
        Long hostId = hostAgentStateQuery.getHostId();
        Map<Long, String> hostIdAgentIdMap = bizHostInfoQueryService.queryAgentIdsByHostId(
            Collections.singletonList(hostId)
        );
        String agentId = hostIdAgentIdMap.get(hostId);
        if (log.isDebugEnabled()) {
            log.debug("queryAgentIdByHostId, hostId={}, agentId={}", hostId, agentId);
        }
        hostAgentStateQuery.setAgentId(agentId);
    }

    private void fillCloudIpIfNeed(HostAgentStateQuery hostAgentStateQuery) {
        if (StringUtils.isNotBlank(hostAgentStateQuery.getCloudIp())) {
            return;
        }
        Long hostId = hostAgentStateQuery.getHostId();
        Map<Long, String> hostIdCloudIpMap = bizHostInfoQueryService.queryCloudIpsByHostId(
            Collections.singletonList(hostId)
        );
        String cloudIp = hostIdCloudIpMap.get(hostId);
        if (log.isDebugEnabled()) {
            log.debug("queryCloudIpByHostId, hostId={}, cloudIp={}", hostId, cloudIp);
        }
        hostAgentStateQuery.setCloudIp(cloudIp);
    }

    private boolean needToUseGseV2(HostAgentStateQuery hostAgentStateQuery) {
        ResourceScope resourceScope = new ResourceScope(
            ResourceScopeTypeEnum.BIZ.getValue(),
            String.valueOf(hostAgentStateQuery.getBizId())
        );
        ToggleEvaluateContext toggleEvaluateContext =
            ToggleEvaluateContext.builder()
                .addContextParam(ToggleStrategyContextParams.CTX_PARAM_RESOURCE_SCOPE, resourceScope);

        return FeatureToggle.checkFeature(
            FeatureIdConstants.FEATURE_AGENT_STATUS_GSE_V2,
            toggleEvaluateContext
        );
    }

    @Override
    public Map<String, AgentState> batchGetAgentState(List<HostAgentStateQuery> hostAgentStateQueryList) {
        // 1.筛选出缺少业务ID的主机
        List<HostAgentStateQuery> queryWithValidBizIdList = new ArrayList<>();
        List<HostAgentStateQuery> queryWithInvalidBizIdList = new ArrayList<>();
        List<HostAgentStateQuery> queryWithInvalidBizAndHostIdList = new ArrayList<>();
        for (HostAgentStateQuery query : hostAgentStateQueryList) {
            if (query.getBizId() == null || query.getBizId() <= 0) {
                if (query.getHostId() == null || query.getHostId() <= 0) {
                    queryWithInvalidBizAndHostIdList.add(query);
                } else {
                    queryWithInvalidBizIdList.add(query);
                }
            } else {
                queryWithValidBizIdList.add(query);
            }
        }
        if (!queryWithInvalidBizAndHostIdList.isEmpty()) {
            log.warn(
                "Ignore {} queryHosts with invalid bizId and hostId:{}",
                queryWithInvalidBizAndHostIdList.size(),
                queryWithInvalidBizAndHostIdList
            );
        }
        List<HostAgentStateQuery> queryWithNotFoundBizIdList = new ArrayList<>();
        fillBizIdsByHostId(queryWithInvalidBizIdList);
        for (HostAgentStateQuery query : queryWithInvalidBizIdList) {
            if (query.getBizId() == null || query.getBizId() <= 0) {
                queryWithNotFoundBizIdList.add(query);
            } else {
                queryWithValidBizIdList.add(query);
            }
        }
        if (!queryWithNotFoundBizIdList.isEmpty()) {
            log.warn(
                "Ignore {} queryHosts with not found bizId:{}",
                queryWithNotFoundBizIdList.size(),
                queryWithNotFoundBizIdList
            );
        }
        Pair<List<String>, List<String>> pair = classifyQueryAndGetAgentIdList(queryWithValidBizIdList);
        List<String> cloudIpList = pair.getLeft();
        List<String> agentIdList = pair.getRight();
        log.info(
            "Use {} cloudIps(empty value filtered) and {} agentIds(empty value filtered) to query agent status",
            cloudIpList.size(),
            agentIdList.size()
        );
        Map<String, AgentState> agentStateMap = gseV1AgentStateClient.batchGetAgentStateConcurrent(cloudIpList);
        agentStateMap.putAll(gseV2AgentStateClient.batchGetAgentStateConcurrent(agentIdList));
        if (log.isDebugEnabled()) {
            log.debug("agentStateMap={}", JsonUtils.toJson(agentStateMap));
        }
        return agentStateMap;
    }

    private void fillBizIdsByHostId(List<HostAgentStateQuery> queryList) {
        if (CollectionUtils.isEmpty(queryList)) {
            return;
        }
        List<Long> hostIdList = queryList.stream().map(HostAgentStateQuery::getHostId).collect(Collectors.toList());
        Map<Long, Long> hostIdBizIdMap = bizHostInfoQueryService.queryBizIdsByHostId(hostIdList);
        if (hostIdBizIdMap.isEmpty()) {
            log.warn("Found empty hostIdBizIdMap by hostIdList:{}", hostIdList);
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug(
                "queryBizIdsByHostId, hostIdList={}, hostIdBizIdMap={}",
                hostIdList,
                JsonUtils.toJson(hostIdBizIdMap)
            );
        }
        for (HostAgentStateQuery query : queryList) {
            Long hostId = query.getHostId();
            Long bizId = hostIdBizIdMap.get(hostId);
            if (bizId != null && bizId > 0) {
                query.setBizId(bizId);
            } else {
                log.warn("Found unexpected bizId:{} by hostId:{}", bizId, hostId);
            }
        }
    }

    private Pair<List<String>, List<String>> classifyQueryAndGetAgentIdList(List<HostAgentStateQuery> queryList) {
        if (CollectionUtils.isEmpty(queryList)) {
            return Pair.of(Collections.emptyList(), Collections.emptyList());
        }
        List<HostAgentStateQuery> v1QueryList = new ArrayList<>();
        List<HostAgentStateQuery> v2QueryList = new ArrayList<>();
        for (HostAgentStateQuery query : queryList) {
            if (needToUseGseV2(query)) {
                v2QueryList.add(query);
            } else {
                v1QueryList.add(query);
            }
        }
        log.info("v1QueryList.size={},v2QueryList.size={}", v1QueryList.size(), v2QueryList.size());
        List<String> cloudIpList = new ArrayList<>();
        if (!v1QueryList.isEmpty()) {
            fillCloudIpsIfNeed(v1QueryList);
        }
        List<String> agentIdList = new ArrayList<>();
        if (!v2QueryList.isEmpty()) {
            fillAgentIdsIfNeed(v2QueryList);
        }
        if (log.isDebugEnabled()) {
            log.debug("classifyQueryAndGetAgentIdList, v1QueryList={}, v2QueryList={}", v1QueryList, v2QueryList);
        }
        for (HostAgentStateQuery hostAgentStateQuery : v1QueryList) {
            String cloudIp = hostAgentStateQuery.getCloudIp();
            if (StringUtils.isNotBlank(cloudIp)) {
                cloudIpList.add(cloudIp);
            }
        }
        for (HostAgentStateQuery hostAgentStateQuery : v2QueryList) {
            String agentId = hostAgentStateQuery.getAgentId();
            if (StringUtils.isNotBlank(agentId)) {
                agentIdList.add(agentId);
            }
        }
        return Pair.of(cloudIpList, agentIdList);
    }

    private void fillCloudIpsIfNeed(List<HostAgentStateQuery> hostAgentStateQueryList) {
        if (CollectionUtils.isEmpty(hostAgentStateQueryList)) {
            return;
        }
        List<HostAgentStateQuery> queryWithoutCloudIpList = new ArrayList<>();
        List<Long> needQueryHostIdList = new ArrayList<>();
        for (HostAgentStateQuery query : hostAgentStateQueryList) {
            if (StringUtils.isBlank(query.getCloudIp())) {
                queryWithoutCloudIpList.add(query);
                needQueryHostIdList.add(query.getHostId());
            }
        }
        if (CollectionUtils.isEmpty(needQueryHostIdList)) {
            return;
        }
        Map<Long, String> hostIdCloudIpMap = queryCloudIpsByHostId(needQueryHostIdList);
        if (hostIdCloudIpMap.isEmpty()) {
            log.warn("Found empty hostIdCloudIpMap by needQueryHostIdList:{}", needQueryHostIdList);
            return;
        }
        List<Long> hostIdWithBlankCloudIpList = new ArrayList<>();
        for (HostAgentStateQuery query : queryWithoutCloudIpList) {
            Long hostId = query.getHostId();
            String cloudIp = hostIdCloudIpMap.get(hostId);
            if (StringUtils.isNotBlank(cloudIp)) {
                query.setCloudIp(cloudIp);
            } else {
                hostIdWithBlankCloudIpList.add(hostId);
            }
        }
        if (!hostIdWithBlankCloudIpList.isEmpty()) {
            log.warn(
                "Found unexpected blank cloudIp by {} hostIds:{}",
                hostIdWithBlankCloudIpList.size(),
                hostIdWithBlankCloudIpList
            );
        }
    }

    private Map<Long, String> queryCloudIpsByHostId(List<Long> hostIdList) {
        if (CollectionUtils.isEmpty(hostIdList)) {
            return new HashMap<>();
        }
        Map<Long, String> hostIdCloudIpMap = bizHostInfoQueryService.queryCloudIpsByHostId(hostIdList);
        if (log.isDebugEnabled()) {
            log.debug(
                "queryCloudIpsByHostId, hostIdList={}, hostIdCloudIpMap={}",
                hostIdList,
                JsonUtils.toJson(hostIdCloudIpMap)
            );
        }
        return hostIdCloudIpMap;
    }

    private void fillAgentIdsIfNeed(List<HostAgentStateQuery> hostAgentStateQueryList) {
        if (CollectionUtils.isEmpty(hostAgentStateQueryList)) {
            return;
        }
        List<HostAgentStateQuery> queryWithoutAgentIdList = new ArrayList<>();
        List<Long> needQueryHostIdList = new ArrayList<>();
        for (HostAgentStateQuery query : hostAgentStateQueryList) {
            if (StringUtils.isBlank(query.getAgentId())) {
                queryWithoutAgentIdList.add(query);
                needQueryHostIdList.add(query.getHostId());
            }
        }
        if (CollectionUtils.isEmpty(needQueryHostIdList)) {
            return;
        }
        Map<Long, String> hostIdAgentIdMap = queryAgentIdsByHostId(needQueryHostIdList);
        if (hostIdAgentIdMap.isEmpty()) {
            log.warn("Found empty hostIdAgentIdMap by needQueryHostIdList:{}", needQueryHostIdList);
            return;
        }
        List<Long> hostIdWithBlankAgentIdList = new ArrayList<>();
        for (HostAgentStateQuery query : queryWithoutAgentIdList) {
            Long hostId = query.getHostId();
            String agentId = hostIdAgentIdMap.get(hostId);
            if (StringUtils.isNotBlank(agentId)) {
                query.setAgentId(agentId);
            } else {
                hostIdWithBlankAgentIdList.add(hostId);
            }
        }
        if (!hostIdWithBlankAgentIdList.isEmpty()) {
            log.warn(
                "Found unexpected blank agentId by {} hostIds:{}",
                hostIdWithBlankAgentIdList.size(),
                hostIdWithBlankAgentIdList
            );
        }
    }

    private Map<Long, String> queryAgentIdsByHostId(List<Long> hostIdList) {
        if (CollectionUtils.isEmpty(hostIdList)) {
            return new HashMap<>();
        }
        Map<Long, String> hostIdAgentIdMap = bizHostInfoQueryService.queryAgentIdsByHostId(hostIdList);
        if (log.isDebugEnabled()) {
            log.debug(
                "queryAgentIdsByHostId, hostIdList={}, hostIdAgentIdMap={}",
                hostIdList,
                JsonUtils.toJson(hostIdAgentIdMap)
            );
        }
        return hostIdAgentIdMap;
    }

    @Override
    public Map<String, Boolean> batchGetAgentAliveStatus(List<HostAgentStateQuery> hostAgentStateQueryList) {
        Map<String, AgentState> agentStateMap = batchGetAgentState(hostAgentStateQueryList);
        return AgentStateUtil.batchGetAgentAliveStatus(agentStateMap);
    }
}
