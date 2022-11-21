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

package com.tencent.bk.job.manage.service.impl.sync;

import com.tencent.bk.job.common.cc.model.req.ResourceWatchReq;
import com.tencent.bk.job.common.cc.model.result.HostEventDetail;
import com.tencent.bk.job.common.cc.model.result.ResourceEvent;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.gse.constants.AgentStatusEnum;
import com.tencent.bk.job.common.gse.service.AgentStateClient;
import com.tencent.bk.job.common.gse.v2.model.resp.AgentState;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.manager.host.HostCache;
import com.tencent.bk.job.manage.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

@Slf4j
public class HostEventsHandler extends EventsHandler<HostEventDetail> {

    private final ApplicationService applicationService;
    private final ApplicationHostDAO applicationHostDAO;
    private final AgentStateClient agentStateClient;
    private final HostCache hostCache;

    HostEventsHandler(BlockingQueue<ResourceEvent<HostEventDetail>> queue,
                      ApplicationService applicationService,
                      ApplicationHostDAO applicationHostDAO,
                      AgentStateClient agentStateClient,
                      HostCache hostCache) {
        super(queue);
        this.applicationService = applicationService;
        this.applicationHostDAO = applicationHostDAO;
        this.agentStateClient = agentStateClient;
        this.hostCache = hostCache;
    }

    @Override
    void handleEvent(ResourceEvent<HostEventDetail> event) {
        handleOneEventRelatedToApp(event);
    }

    private void handleOneEventRelatedToApp(ResourceEvent<HostEventDetail> event) {
        try {
            log.info("start to handle host event:{}", JsonUtils.toJson(event));
            handleOneEventIndeed(event);
        } catch (Throwable t) {
            FormattingTuple msg = MessageFormatter.format("Fail to handle hostEvent:{}", event);
            log.error(msg.getMessage(), t);
        }
    }

    private void handleOneEventIndeed(ResourceEvent<HostEventDetail> event) {
        String eventType = event.getEventType();
        ApplicationHostDTO hostInfoDTO = HostEventDetail.toHostInfoDTO(event.getDetail());
        switch (eventType) {
            case ResourceWatchReq.EVENT_TYPE_CREATE:
            case ResourceWatchReq.EVENT_TYPE_UPDATE:
                // 去除没有IP的主机信息
                if (StringUtils.isBlank(hostInfoDTO.getDisplayIp())) {
                    deleteHostWithoutIp(hostInfoDTO);
                    break;
                }
                // 找出Agent有效的IP，并设置Agent状态
                updateIpAndAgentStatus(hostInfoDTO);
                // 更新DB中的主机数据
                createOrUpdateHostInDB(hostInfoDTO);
                // 更新缓存中的主机数据
                updateHostCache(hostInfoDTO);
                break;
            case ResourceWatchReq.EVENT_TYPE_DELETE:
                handleHostDelete(hostInfoDTO);
                break;
            default:
                break;
        }
    }

    private void deleteHostWithoutIp(ApplicationHostDTO hostInfoDTO) {
        int affectedRowNum = applicationHostDAO.deleteBizHostInfoById(null, hostInfoDTO.getHostId());
        log.info(
            "{} host deleted, id={} ,ip={}",
            affectedRowNum,
            hostInfoDTO.getHostId(),
            hostInfoDTO.getIp()
        );
    }

    private List<String> buildAgentIdByMultiIp(Long cloudId, String multiIp) {
        if (StringUtils.isBlank(multiIp)) {
            return Collections.emptyList();
        }
        String[] ipArr = multiIp.split(",");
        List<String> agentIdList = new ArrayList<>();
        for (String ip : ipArr) {
            agentIdList.add(cloudId + ":" + ip);
        }
        return agentIdList;
    }

    private void updateIpAndAgentStatus(ApplicationHostDTO hostInfoDTO) {
        if (StringUtils.isNotBlank(hostInfoDTO.getAgentId())) {
            AgentState agentState = agentStateClient.getAgentState(hostInfoDTO.getAgentId());
            if (agentState != null) {
                hostInfoDTO.setGseAgentStatus(agentState.getStatusCode());
            }
        } else {
            // 处理多IP的情况
            String multiIp = hostInfoDTO.getDisplayIp();
            List<String> agentIdList = buildAgentIdByMultiIp(hostInfoDTO.getCloudAreaId(), multiIp);
            if (CollectionUtils.isEmpty(agentIdList)) {
                return;
            }
            Map<String, AgentState> agentStateMap = agentStateClient.batchGetAgentState(agentIdList);
            String validAgentId = agentIdList.get(0);
            AgentState validAgentState = agentStateMap.get(validAgentId);
            for (Map.Entry<String, AgentState> entry : agentStateMap.entrySet()) {
                String agentId = entry.getKey();
                AgentState agentState = entry.getValue();
                if (AgentStatusEnum.isAgentAlive(agentState)) {
                    validAgentId = agentId;
                    validAgentState = agentState;
                    break;
                }
            }
            if (!AgentStatusEnum.isAgentAlive(validAgentState)) {
                log.warn("cannot find agent alive of multiIp:{}", multiIp);
                return;
            }
            hostInfoDTO.setIp(validAgentId.split(":")[1]);
            if (validAgentState != null) {
                hostInfoDTO.setGseAgentStatus(validAgentState.getStatusCode());
            }
        }
    }

    private void createOrUpdateHostInDB(ApplicationHostDTO hostInfoDTO) {
        try {
            if (applicationHostDAO.existAppHostInfoByHostId(hostInfoDTO.getHostId())) {
                // 只更新事件中的主机属性与agent状态
                applicationHostDAO.updateHostAttrsById(hostInfoDTO);
            } else {
                hostInfoDTO.setBizId(JobConstants.PUBLIC_APP_ID);
                int affectedNum = applicationHostDAO.insertHostWithoutTopo(hostInfoDTO);
                log.info("insert host: id={}, affectedNum={}", hostInfoDTO.getHostId(), affectedNum);
            }
        } catch (Throwable t) {
            log.error("handle host event fail", t);
        } finally {
            // 从拓扑表向主机表同步拓扑数据
            int affectedNum = applicationHostDAO.syncHostTopo(hostInfoDTO.getHostId());
            log.info("hostTopo synced: hostId={}, affectedNum={}", hostInfoDTO.getHostId(), affectedNum);
        }
    }

    private void updateHostCache(ApplicationHostDTO hostInfoDTO) {
        hostInfoDTO = applicationHostDAO.getHostById(hostInfoDTO.getHostId());
        if (hostInfoDTO.getBizId() != null && hostInfoDTO.getBizId() > 0) {
            // 只更新常规业务的主机到缓存
            if (applicationService.existBiz(hostInfoDTO.getBizId())) {
                hostCache.addOrUpdateHost(hostInfoDTO);
                log.info("host cache updated: hostId:{}", hostInfoDTO.getHostId());
            }
        }
    }

    private void handleHostDelete(ApplicationHostDTO hostInfoDTO) {
        int affectedRowNum = applicationHostDAO.deleteBizHostInfoById(null, hostInfoDTO.getHostId());
        log.info(
            "{} host deleted, id={} ,ip={}",
            affectedRowNum,
            hostInfoDTO.getHostId(),
            hostInfoDTO.getIp()
        );
        hostCache.deleteHost(hostInfoDTO);
    }
}
