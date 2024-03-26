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

package com.tencent.bk.job.manage.api.esb.impl.v3;

import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.gse.constants.AgentStateStatusEnum;
import com.tencent.bk.job.common.gse.v2.model.req.ListAgentStateReq;
import com.tencent.bk.job.common.gse.v2.model.resp.AgentState;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.manage.api.esb.v3.EsbAgentInfoV3Resource;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbQueryAgentInfoV3Req;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbAgentInfoV3DTO;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbQueryAgentInfoV3Resp;
import com.tencent.bk.job.manage.service.agent.status.ScopeAgentStatusService;
import com.tencent.bk.job.manage.service.host.HostService;
import com.tencent.bk.job.manage.service.host.ScopeHostService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.jooq.tools.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
public class EsbAgentInfoResourceV3Impl implements EsbAgentInfoV3Resource {

    private final HostService hostService;
    private final ScopeAgentStatusService scopeAgentStatusService;
    private final ScopeHostService scopeHostService;

    @Autowired
    public EsbAgentInfoResourceV3Impl(HostService hostService,
                                      ScopeAgentStatusService scopeAgentStatusService,
                                      ScopeHostService scopeHostService) {
        this.hostService = hostService;
        this.scopeAgentStatusService = scopeAgentStatusService;
        this.scopeHostService = scopeHostService;
    }

    @Override
    public EsbResp<EsbQueryAgentInfoV3Resp> queryAgentInfo(String username,
                                                           String appCode,
                                                           EsbQueryAgentInfoV3Req req) {
        ResourceScope resourceScope = new ResourceScope(req.getScopeType(), req.getScopeId());
        boolean needToUseGseV2 = scopeAgentStatusService.needToUseGseV2(resourceScope);

        List<Long> hostIdList = req.getHostIdList();
        List<Long> validHostIdList = scopeHostService.filterScopeHostIds(
            req.getAppResourceScope(),
            new HashSet<>(hostIdList)
        );
        hostIdList.removeAll(validHostIdList);
        if (CollectionUtils.isNotEmpty(hostIdList)) {
            log.warn("Ignore hostIds not in {}:{}", resourceScope, hostIdList);
        }
        Map<Long, String> hostIdAgentIdMap = generateHostIdAgentIdMap(needToUseGseV2, validHostIdList);

        ListAgentStateReq listAgentStateReq = new ListAgentStateReq();
        listAgentStateReq.setAgentIdList(new ArrayList<>(new HashSet<>(hostIdAgentIdMap.values())));
        List<AgentState> agentStateList = scopeAgentStatusService.listAgentState(resourceScope, listAgentStateReq);

        List<EsbAgentInfoV3DTO> agentInfoList = buildEsbAgentInfoList(
            agentStateList,
            validHostIdList,
            hostIdAgentIdMap,
            needToUseGseV2
        );

        EsbQueryAgentInfoV3Resp resp = new EsbQueryAgentInfoV3Resp();
        resp.setAgentInfoList(agentInfoList);
        return EsbResp.buildSuccessResp(resp);
    }

    private Map<Long, String> generateHostIdAgentIdMap(boolean needToUseGseV2, List<Long> hostIdList) {
        Map<Long, ApplicationHostDTO> hostInfoMap = hostService.listHostsByHostIds(hostIdList);
        Map<Long, String> hostIdAgentIdMap = new HashMap<>();
        if (needToUseGseV2) {
            // 使用AgentId
            for (Long hostId : hostIdList) {
                ApplicationHostDTO hostDTO = hostInfoMap.get(hostId);
                if (hostDTO != null) {
                    String agentId = hostDTO.getAgentId();
                    if (!StringUtils.isBlank(agentId)) {
                        hostIdAgentIdMap.put(hostId, agentId);
                    } else {
                        log.info("AgentId of host({}) is blank, ignore", hostDTO);
                    }
                } else {
                    log.info("Cannot find host by hostId:{}", hostId);
                }
            }
            return hostIdAgentIdMap;
        }
        // 使用CloudIp
        for (Long hostId : hostIdList) {
            ApplicationHostDTO hostDTO = hostInfoMap.get(hostId);
            if (hostDTO != null) {
                String cloudIp = hostDTO.getCloudIp();
                if (!StringUtils.isBlank(cloudIp)) {
                    hostIdAgentIdMap.put(hostId, cloudIp);
                } else {
                    log.info("cloudIp of host({}) is blank, ignore", hostDTO);
                }
            } else {
                log.info("Cannot find host by hostId:{}", hostId);
            }
        }
        return hostIdAgentIdMap;
    }

    private List<EsbAgentInfoV3DTO> buildEsbAgentInfoList(List<AgentState> agentStateList,
                                                          List<Long> hostIdList,
                                                          Map<Long, String> hostIdAgentIdMap,
                                                          boolean needToUseGseV2) {
        Map<String, AgentState> agentStateMap = new HashMap<>();
        for (AgentState agentState : agentStateList) {
            agentStateMap.put(agentState.getAgentId(), agentState);
        }
        List<EsbAgentInfoV3DTO> agentInfoList = new ArrayList<>();
        for (Long hostId : hostIdList) {
            String agentId = hostIdAgentIdMap.get(hostId);
            if (StringUtils.isBlank(agentId)) {
                log.info("Cannot find agentId by hostId={}", hostId);
                continue;
            }
            AgentState agentState = agentStateMap.get(agentId);
            if (agentState == null) {
                log.warn("Cannot find agentState from gse by hostId={}, agentId={}", hostId, agentId);
                continue;
            }
            EsbAgentInfoV3DTO agentInfoV3DTO = new EsbAgentInfoV3DTO();
            agentInfoV3DTO.setHostId(hostId);
            agentInfoV3DTO.setStatus(getEsbAgentStatus(agentState));
            String agentVersion = agentState.getVersion();
            if (StringUtils.isBlank(agentVersion)) {
                if (!needToUseGseV2) {
                    agentInfoV3DTO.setVersion("V1");
                }
            } else {
                agentInfoV3DTO.setVersion(agentVersion);
            }
            agentInfoList.add(agentInfoV3DTO);
        }
        return agentInfoList;
    }

    private Integer getEsbAgentStatus(AgentState agentState) {
        if (agentState.getStatusCode() == AgentStateStatusEnum.RUNNING.getValue()) {
            return 1;
        }
        return 0;
    }
}
