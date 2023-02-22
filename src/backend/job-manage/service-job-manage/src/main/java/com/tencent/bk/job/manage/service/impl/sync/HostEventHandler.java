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
import com.tencent.bk.job.common.gse.service.AgentStateClient;
import com.tencent.bk.job.common.gse.v2.model.resp.AgentState;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.manager.host.HostCache;
import com.tencent.bk.job.manage.metrics.CmdbEventSampler;
import com.tencent.bk.job.manage.metrics.MetricsConstants;
import com.tencent.bk.job.manage.service.ApplicationService;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.sleuth.Tracer;

import java.util.concurrent.BlockingQueue;

@Slf4j
public class HostEventHandler extends EventsHandler<HostEventDetail> {

    private final ApplicationService applicationService;
    private final ApplicationHostDAO applicationHostDAO;
    private final AgentStateClient agentStateClient;
    private final HostCache hostCache;

    HostEventHandler(Tracer tracer,
                     CmdbEventSampler cmdbEventSampler,
                     BlockingQueue<ResourceEvent<HostEventDetail>> queue,
                     ApplicationService applicationService,
                     ApplicationHostDAO applicationHostDAO,
                     AgentStateClient agentStateClient,
                     HostCache hostCache) {
        super(queue, tracer, cmdbEventSampler);
        this.applicationService = applicationService;
        this.applicationHostDAO = applicationHostDAO;
        this.agentStateClient = agentStateClient;
        this.hostCache = hostCache;
    }

    @Override
    void handleEvent(ResourceEvent<HostEventDetail> event) {
        handleOneEventRelatedToApp(event);
    }

    @Override
    Tags getEventHandleExtraTags() {
        return Tags.of(MetricsConstants.TAG_KEY_CMDB_EVENT_TYPE, MetricsConstants.TAG_VALUE_CMDB_EVENT_TYPE_HOST);
    }

    @Override
    String getSpanName() {
        return "handleHostEvent";
    }

    private void handleOneEventRelatedToApp(ResourceEvent<HostEventDetail> event) {
        log.info("start to handle host event:{}", JsonUtils.toJson(event));
        handleOneEventIndeed(event);
    }


    private void handleOneEventIndeed(ResourceEvent<HostEventDetail> event) {
        String eventType = event.getEventType();
        ApplicationHostDTO hostInfoDTO = HostEventDetail.toHostInfoDTO(event.getDetail());
        switch (eventType) {
            case ResourceWatchReq.EVENT_TYPE_CREATE:
            case ResourceWatchReq.EVENT_TYPE_UPDATE:
                // 忽略没有hostId的主机事件
                if (hostInfoDTO.getHostId() == null) {
                    log.warn("Ignore hostEvent without hostId:{}", event);
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

    private void updateIpAndAgentStatus(ApplicationHostDTO hostInfoDTO) {
        String agentId = StringUtils.isNotBlank(hostInfoDTO.getAgentId()) ?
            hostInfoDTO.getAgentId() : hostInfoDTO.getCloudIp();
        AgentState agentState = agentStateClient.getAgentState(agentId);
        if (agentState != null) {
            hostInfoDTO.setGseAgentStatus(agentState.getStatusCode());
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
