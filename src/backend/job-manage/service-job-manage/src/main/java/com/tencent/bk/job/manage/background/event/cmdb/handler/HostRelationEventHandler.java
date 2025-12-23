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
import com.tencent.bk.job.common.cc.model.result.HostRelationEventDetail;
import com.tencent.bk.job.common.cc.model.result.ResourceEvent;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.config.JobManageConfig;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import com.tencent.bk.job.manage.manager.host.HostCache;
import com.tencent.bk.job.manage.metrics.CmdbEventSampler;
import com.tencent.bk.job.manage.metrics.MetricsConstants;
import com.tencent.bk.job.manage.model.dto.HostTopoDTO;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.host.NoTenantHostService;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.util.StopWatch;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * 主机关系事件处理器
 */
@Slf4j
public class HostRelationEventHandler extends AsyncEventHandler<HostRelationEventDetail> {

    /**
     * 单个Handler的额外线程数
     */
    public static final int SINGLE_HANDLER_EXTRA_THREAD_NUM = 1;
    /**
     * CMDB事件指标数据采样器
     */
    private final CmdbEventSampler cmdbEventSampler;
    /**
     * 业务服务
     */
    private final ApplicationService applicationService;
    /**
     * 租户无关的主机服务
     */
    private final NoTenantHostService noTenantHostService;
    /**
     * 主机拓扑数据操作对象
     */
    private final HostTopoDAO hostTopoDAO;
    /**
     * 主机缓存
     */
    private final HostCache hostCache;

    public HostRelationEventHandler(Tracer tracer,
                                    CmdbEventSampler cmdbEventSampler,
                                    ApplicationService applicationService,
                                    NoTenantHostService noTenantHostService,
                                    HostTopoDAO hostTopoDAO,
                                    HostCache hostCache,
                                    JobManageConfig jobManageConfig,
                                    String tenantId) {
        super(
            new LinkedBlockingQueue<>(jobManageConfig.getHostRelationEventQueueSize()),
            tracer,
            cmdbEventSampler,
            tenantId
        );
        this.cmdbEventSampler = cmdbEventSampler;
        this.applicationService = applicationService;
        this.noTenantHostService = noTenantHostService;
        this.hostTopoDAO = hostTopoDAO;
        this.hostCache = hostCache;
        registerQueueMetrics();
    }

    @Override
    public int getExtraThreadNum() {
        return SINGLE_HANDLER_EXTRA_THREAD_NUM;
    }

    @Override
    Iterable<Tag> getEventHandleExtraTags() {
        return Tags.of(
            MetricsConstants.TAG_KEY_CMDB_EVENT_TYPE,
            MetricsConstants.TAG_VALUE_CMDB_EVENT_TYPE_HOST_RELATION
        );
    }

    @Override
    void handleEventInternal(ResourceEvent<HostRelationEventDetail> event) {
        handleOneEventAndRecordTime(event);
    }

    /**
     * 注册事件队列相关指标数据
     */
    private void registerQueueMetrics() {
        String handlerName = "HostRelationEventHandler";
        Iterable<Tag> tags = buildHostRelationEventHandlerTags(handlerName);
        cmdbEventSampler.registerEventQueueToGauge(queue, tags);
    }

    /**
     * 构建主机关系事件处理器相关指标数据的维度标签
     *
     * @param handlerName 处理器名称
     * @return 维度标签
     */
    private Iterable<Tag> buildHostRelationEventHandlerTags(String handlerName) {
        return Tags.of(
            MetricsConstants.TAG_KEY_CMDB_EVENT_TYPE, MetricsConstants.TAG_VALUE_CMDB_EVENT_TYPE_HOST_RELATION,
            MetricsConstants.TAG_KEY_CMDB_HOST_EVENT_HANDLER_NAME, handlerName
        );
    }

    /**
     * 处理单个主机关系事件并记录耗时
     *
     * @param event 主机关系事件
     */
    private void handleOneEventAndRecordTime(ResourceEvent<HostRelationEventDetail> event) {
        log.info("start to handle host relation event:{}", JsonUtils.toJson(event));
        StopWatch watch = new StopWatch();
        watch.start("handleOneEventIndeed");
        handleOneEvent(event);
        watch.stop();
        if (watch.getTotalTimeMillis() > 3000) {
            log.warn("PERF:SLOW:handle hostRelationEvent:" + watch.prettyPrint());
        } else {
            log.debug("handle hostRelationEvent:" + watch.prettyPrint());
        }
    }

    /**
     * 处理单个主机关系事件
     *
     * @param event 主机关系事件
     */
    private void handleOneEvent(ResourceEvent<HostRelationEventDetail> event) {
        String eventType = event.getEventType();
        HostTopoDTO hostTopoDTO = HostTopoDTO.fromHostRelationEvent(event.getDetail());
        setDefaultLastTimeForHostTopoIfNeed(event, hostTopoDTO);
        switch (eventType) {
            case ResourceWatchReq.EVENT_TYPE_CREATE:
                handleCreateEvent(hostTopoDTO);
                break;
            case ResourceWatchReq.EVENT_TYPE_DELETE:
                handleDeleteEvent(hostTopoDTO);
                break;
            default:
                break;
        }
    }

    /**
     * CMDB中某些主机拓扑数据由于历史原因存在lastTime字段为空的情况，使用事件创建时间作为其默认值
     *
     * @param event       事件
     * @param hostTopoDTO 主机拓扑数据
     */
    private void setDefaultLastTimeForHostTopoIfNeed(ResourceEvent<HostRelationEventDetail> event,
                                                     HostTopoDTO hostTopoDTO) {
        if (hostTopoDTO.getLastTime() == null || hostTopoDTO.getLastTime() < 0) {
            hostTopoDTO.setLastTime(event.getCreateTime());
        }
    }

    /**
     * 处理主机关系创建事件
     *
     * @param hostTopoDTO 主机拓扑对象
     */
    private void handleCreateEvent(HostTopoDTO hostTopoDTO) {
        // 插入拓扑数据
        int insertedHostTopoNum = hostTopoDAO.insertHostTopo(hostTopoDTO);
        // 同步拓扑数据至主机表冗余字段
        int affectedHostNum = updateTopoToHost(hostTopoDTO);
        // 更新主机缓存
        boolean cacheUpdated = updateOrDeleteHostCache(hostTopoDTO);
        log.info(
            "create event handle result: insertedHostTopoNum={}, affectedHostNum={}, cacheUpdated={}",
            insertedHostTopoNum,
            affectedHostNum,
            cacheUpdated
        );
    }

    /**
     * 处理主机关系删除事件
     *
     * @param hostTopoDTO 主机拓扑对象
     */
    private void handleDeleteEvent(HostTopoDTO hostTopoDTO) {
        // 删除拓扑数据
        int deletedHostTopoNum = hostTopoDAO.deleteHostTopoBeforeOrEqualLastTime(
            hostTopoDTO.getHostId(),
            hostTopoDTO.getBizId(),
            hostTopoDTO.getSetId(),
            hostTopoDTO.getModuleId(),
            hostTopoDTO.getLastTime()
        );
        if (deletedHostTopoNum == 0) {
            log.warn("no hostTopo deleted, delete event may expire for long time");
            return;
        }
        // 同步拓扑数据至主机表冗余字段
        int deleteEventAffectedHostNum = updateTopoToHost(hostTopoDTO);
        // 更新主机缓存
        boolean deleteEventCacheUpdated = updateOrDeleteHostCache(hostTopoDTO);
        log.info(
            "delete event handle result: deletedHostTopoNum={}, deleteEventAffectedHostNum={}," +
                " deleteEventCacheUpdated={}",
            deletedHostTopoNum,
            deleteEventAffectedHostNum,
            deleteEventCacheUpdated
        );
    }

    /**
     * 将主机拓扑表中的拓扑数据同步至主机表
     *
     * @param hostTopoDTO 主机拓扑信息
     * @return 受影响的主机数量
     */
    private int updateTopoToHost(HostTopoDTO hostTopoDTO) {
        // 若主机存在需将拓扑信息同步至主机信息冗余字段
        int affectedHostNum = noTenantHostService.syncHostTopo(hostTopoDTO.getHostId());
        if (affectedHostNum > 0) {
            log.info("host topo synced: affectedHostNum={}", affectedHostNum);
        } else if (affectedHostNum == 0) {
            log.info("no host topo synced");
        } else {
            log.warn("cannot find hostInfo by hostId:{}, wait for host event or sync", hostTopoDTO.getHostId());
        }
        return affectedHostNum;
    }

    /**
     * 更新或删除缓存中的主机信息
     *
     * @param hostTopoDTO 主机拓扑信息
     * @return 是否执行了更新/删除缓存动作
     */
    private boolean updateOrDeleteHostCache(HostTopoDTO hostTopoDTO) {
        ApplicationHostDTO host = noTenantHostService.getHostById(hostTopoDTO.getHostId());
        if (host == null) {
            log.info("host already deleted by others: hostId={}, ignore", hostTopoDTO.getHostId());
            return false;
        }
        if (host.getBizId() == JobConstants.PUBLIC_APP_ID) {
            hostCache.deleteHost(host);
            log.info("host cached deleted: hostId={}", host.getHostId());
            return true;
        }
        if (applicationService.existBiz(host.getBizId())) {
            hostCache.addOrUpdateHost(host);
            log.info("host cached updated: hostId={}", host.getHostId());
        } else {
            hostCache.deleteHost(host);
            log.info("host biz({}) not exist, host cached deleted: hostId={}", host.getBizId(), host.getHostId());
        }
        return true;
    }
}
