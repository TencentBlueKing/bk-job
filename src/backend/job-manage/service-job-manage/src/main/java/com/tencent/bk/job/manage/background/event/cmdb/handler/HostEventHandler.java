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

package com.tencent.bk.job.manage.background.event.cmdb.handler;

import com.tencent.bk.job.common.cc.model.req.ResourceWatchReq;
import com.tencent.bk.job.common.cc.model.result.HostEventDetail;
import com.tencent.bk.job.common.cc.model.result.ResourceEvent;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.gse.service.AgentStateClient;
import com.tencent.bk.job.common.gse.service.model.HostAgentStateQuery;
import com.tencent.bk.job.common.gse.v2.model.resp.AgentState;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.config.GseConfig;
import com.tencent.bk.job.manage.metrics.CmdbEventSampler;
import com.tencent.bk.job.manage.metrics.MetricsConstants;
import com.tencent.bk.job.manage.service.host.NoTenantHostService;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.sleuth.Tracer;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * 主机事件处理，在一个独立的线程中不断从队列中获取事件并处理
 */
@Slf4j
public class HostEventHandler extends AsyncEventHandler<HostEventDetail> {

    /**
     * 租户无关的主机服务
     */
    private final NoTenantHostService noTenantHostService;
    /**
     * 主机Agent状态查询客户端
     */
    private final AgentStateClient agentStateClient;
    /**
     * 业务相关CMDB接口调用客户端
     */
    private final IBizCmdbClient bizCmdbClient;

    public HostEventHandler(Tracer tracer,
                            CmdbEventSampler cmdbEventSampler,
                            BlockingQueue<ResourceEvent<HostEventDetail>> queue,
                            NoTenantHostService noTenantHostService,
                            @Qualifier(GseConfig.MANAGE_BEAN_AGENT_STATE_CLIENT)
                            AgentStateClient agentStateClient,
                            IBizCmdbClient bizCmdbClient,
                            String tenantId) {
        super(queue, tracer, cmdbEventSampler, tenantId);
        this.noTenantHostService = noTenantHostService;
        this.agentStateClient = agentStateClient;
        this.bizCmdbClient = bizCmdbClient;
    }

    @Override
    public int getExtraThreadNum() {
        // 使用1个额外的线程处理主机事件
        return 1;
    }

    @Override
    Iterable<Tag> getEventHandleExtraTags() {
        return Tags.of(MetricsConstants.TAG_KEY_CMDB_EVENT_TYPE, MetricsConstants.TAG_VALUE_CMDB_EVENT_TYPE_HOST);
    }

    @Override
    void handleEventInternal(ResourceEvent<HostEventDetail> event) {
        handleOneEvent(event);
    }

    /**
     * 处理单个主机事件
     *
     * @param event 主机事件
     */
    private void handleOneEvent(ResourceEvent<HostEventDetail> event) {
        log.info("start to handle host event:{}", JsonUtils.toJson(event));
        String eventType = event.getEventType();
        ApplicationHostDTO hostInfoDTO = HostEventDetail.toHostInfoDTO(tenantId, event.getDetail());
        setDefaultLastTimeForHostIfNeed(event, hostInfoDTO);
        switch (eventType) {
            case ResourceWatchReq.EVENT_TYPE_CREATE:
            case ResourceWatchReq.EVENT_TYPE_UPDATE:
                handleCreateOrUpdateEvent(event, hostInfoDTO);
                break;
            case ResourceWatchReq.EVENT_TYPE_DELETE:
                handleDeleteEvent(hostInfoDTO);
                break;
            default:
                break;
        }
    }

    /**
     * 处理创建或更新事件
     *
     * @param event       事件
     * @param hostInfoDTO 主机数据
     */
    private void handleCreateOrUpdateEvent(ResourceEvent<HostEventDetail> event, ApplicationHostDTO hostInfoDTO) {
        // 忽略没有hostId的主机事件
        if (hostInfoDTO.getHostId() == null) {
            log.warn("Ignore hostEvent without hostId:{}", event);
            return;
        }
        // 尝试设置Agent状态
        Integer agentStatus = tryToUpdateAgentStatus(hostInfoDTO);
        // 更新DB与缓存中的主机数据
        Pair<Boolean, Integer> pair = noTenantHostService.createOrUpdateHostBeforeLastTime(hostInfoDTO);
        int affectedNum = pair.getRight();
        if (affectedNum > 0) {
            log.info("{} host affected, created:{}", affectedNum, pair.getLeft());
            return;
        }
        // 如果未能成功更新本地主机数据，说明事件可能有较高延迟，需要从CMDB查询最新主机信息并再次更新
        log.info("No host affected after handle by lastTime, query latest host from cmdb and update");
        queryHostFromCmdbAndUpdate(hostInfoDTO, agentStatus);
    }

    /**
     * 处理删除事件
     *
     * @param hostInfoDTO 主机数据
     */
    private void handleDeleteEvent(ApplicationHostDTO hostInfoDTO) {
        int deletedNum = noTenantHostService.deleteHostBeforeOrEqualLastTime(hostInfoDTO);
        log.info("delete host:{}, deletedNum={}", hostInfoDTO, deletedNum);
    }

    /**
     * 从CMDB查询最新主机信息并更新
     *
     * @param hostInfoDTO 主机数据
     * @param agentStatus 主机Agent状态
     */
    private void queryHostFromCmdbAndUpdate(ApplicationHostDTO hostInfoDTO, Integer agentStatus) {
        List<ApplicationHostDTO> hostList = bizCmdbClient.listHostsByHostIds(
            tenantId,
            Collections.singletonList(hostInfoDTO.getHostId())
        );
        if (CollectionUtils.isEmpty(hostList)) {
            // 机器在CMDB中已不存在，忽略
            log.info("host not exist in cmdb:{}, ignore", hostInfoDTO);
            return;
        }
        hostInfoDTO = hostList.get(0);
        hostInfoDTO.setGseAgentStatus(agentStatus);
        int affectedNum = noTenantHostService.updateHostAttrsByHostId(hostInfoDTO);
        log.info("update host attrs:{}, affectedNum={}", hostInfoDTO, affectedNum);
        // 更新缓存
        if (affectedNum > 0) {
            noTenantHostService.loadHostFromDbToCache(hostInfoDTO.getHostId());
        }
    }

    /**
     * CMDB中某些主机由于历史原因存在lastTime字段为空的情况，使用事件创建时间作为其默认值
     *
     * @param event       事件
     * @param hostInfoDTO 主机数据
     */
    private void setDefaultLastTimeForHostIfNeed(ResourceEvent<HostEventDetail> event, ApplicationHostDTO hostInfoDTO) {
        if (hostInfoDTO.getLastTime() != null && hostInfoDTO.getLastTime() > 0) {
            return;
        }
        log.warn(
            "HostEvent lastTime is invalid({}), use event create time({}) to update host",
            hostInfoDTO.getLastTime(),
            event.getCreateTime()
        );
        hostInfoDTO.setLastTime(event.getCreateTime());
    }

    /**
     * 尝试更新主机的Agent状态
     *
     * @param hostInfoDTO 主机信息
     * @return 最终主机的Agent状态
     */
    private Integer tryToUpdateAgentStatus(ApplicationHostDTO hostInfoDTO) {
        try {
            AgentState agentState = agentStateClient.getAgentState(HostAgentStateQuery.from(hostInfoDTO));
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
        return hostInfoDTO.getGseAgentStatus();
    }

}
