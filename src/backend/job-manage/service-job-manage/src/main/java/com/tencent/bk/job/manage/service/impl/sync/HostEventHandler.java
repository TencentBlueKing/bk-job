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
import com.tencent.bk.job.common.gse.service.AgentStateClient;
import com.tencent.bk.job.common.gse.v2.model.resp.AgentState;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.metrics.CmdbEventSampler;
import com.tencent.bk.job.manage.metrics.MetricsConstants;
import com.tencent.bk.job.manage.service.host.HostService;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.cloud.sleuth.Tracer;

import java.util.concurrent.BlockingQueue;

@Slf4j
public class HostEventHandler extends EventsHandler<HostEventDetail> {

    private final HostService hostService;
    private final AgentStateClient agentStateClient;

    HostEventHandler(Tracer tracer,
                     CmdbEventSampler cmdbEventSampler,
                     BlockingQueue<ResourceEvent<HostEventDetail>> queue,
                     HostService hostService,
                     AgentStateClient agentStateClient) {
        super(queue, tracer, cmdbEventSampler);
        this.hostService = hostService;
        this.agentStateClient = agentStateClient;
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

    private void setDefaultLastTimeForHostIfNeed(ResourceEvent<HostEventDetail> event, ApplicationHostDTO hostInfoDTO) {
        if (hostInfoDTO.getLastTime() == null || hostInfoDTO.getLastTime() < 0) {
            log.warn(
                "HostEvent lastTime is invalid({}), use event create time({}) to update host",
                hostInfoDTO.getLastTime(),
                event.getCreateTime()
            );
            hostInfoDTO.setLastTime(event.getCreateTime());
        }
    }

    private void handleOneEventIndeed(ResourceEvent<HostEventDetail> event) {
        String eventType = event.getEventType();
        ApplicationHostDTO hostInfoDTO = HostEventDetail.toHostInfoDTO(event.getDetail());
        setDefaultLastTimeForHostIfNeed(event, hostInfoDTO);
        switch (eventType) {
            case ResourceWatchReq.EVENT_TYPE_CREATE:
            case ResourceWatchReq.EVENT_TYPE_UPDATE:
                // 忽略没有hostId的主机事件
                if (hostInfoDTO.getHostId() == null) {
                    log.warn("Ignore hostEvent without hostId:{}", event);
                    break;
                }
                // 尝试设置Agent状态
                tryToUpdateAgentStatus(hostInfoDTO);
                // 更新DB与缓存中的主机数据
                hostService.createOrUpdateHostBeforeLastTime(hostInfoDTO);
                break;
            case ResourceWatchReq.EVENT_TYPE_DELETE:
                hostService.deleteHostBeforeLastTime(hostInfoDTO);
                break;
            default:
                break;
        }
    }

    private void tryToUpdateAgentStatus(ApplicationHostDTO hostInfoDTO) {
        try {
            String agentId = StringUtils.isNotBlank(hostInfoDTO.getAgentId()) ?
                hostInfoDTO.getAgentId() : hostInfoDTO.getCloudIp();
            AgentState agentState = agentStateClient.getAgentState(agentId);
            if (agentState != null) {
                hostInfoDTO.setGseAgentStatus(agentState.getStatusCode());
            }
        } catch (Exception e) {
            FormattingTuple msg = MessageFormatter.format(
                "Fail to updateAgentStatus, host={}",
                hostInfoDTO
            );
            log.warn(msg.getMessage(), e);
        }
    }

}
