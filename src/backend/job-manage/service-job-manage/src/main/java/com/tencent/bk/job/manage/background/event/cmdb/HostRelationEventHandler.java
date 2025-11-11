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

package com.tencent.bk.job.manage.background.event.cmdb;

import com.tencent.bk.job.common.cc.model.req.ResourceWatchReq;
import com.tencent.bk.job.common.cc.model.result.HostRelationEventDetail;
import com.tencent.bk.job.common.cc.model.result.ResourceEvent;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import com.tencent.bk.job.manage.dao.NoTenantHostDAO;
import com.tencent.bk.job.manage.manager.host.HostCache;
import com.tencent.bk.job.manage.metrics.CmdbEventSampler;
import com.tencent.bk.job.manage.metrics.MetricsConstants;
import com.tencent.bk.job.manage.model.dto.HostTopoDTO;
import com.tencent.bk.job.manage.service.ApplicationService;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.util.StopWatch;

import java.util.concurrent.BlockingQueue;

@Slf4j
public class HostRelationEventHandler extends EventsHandler<HostRelationEventDetail> {

    /**
     * 单个Handler自身的线程资源成本
     */
    public static final int SINGLE_HANDLER_THREAD_RESOURCE_COST = 1;
    private final ApplicationService applicationService;
    private final NoTenantHostDAO noTenantHostDAO;
    private final HostTopoDAO hostTopoDAO;
    private final HostCache hostCache;

    public HostRelationEventHandler(Tracer tracer,
                                    CmdbEventSampler cmdbEventSampler,
                                    BlockingQueue<ResourceEvent<HostRelationEventDetail>> queue,
                                    ApplicationService applicationService,
                                    NoTenantHostDAO noTenantHostDAO,
                                    HostTopoDAO hostTopoDAO,
                                    HostCache hostCache,
                                    String tenantId) {
        super(queue, tracer, cmdbEventSampler, tenantId);
        this.applicationService = applicationService;
        this.noTenantHostDAO = noTenantHostDAO;
        this.hostTopoDAO = hostTopoDAO;
        this.hostCache = hostCache;
    }

    @Override
    void handleEvent(ResourceEvent<HostRelationEventDetail> event) {
        handleOneEvent(event);
    }

    @Override
    Tags getEventHandleExtraTags() {
        return Tags.of(
            MetricsConstants.TAG_KEY_CMDB_EVENT_TYPE,
            MetricsConstants.TAG_VALUE_CMDB_EVENT_TYPE_HOST_RELATION
        );
    }

    @Override
    String getSpanName() {
        return "handleHostRelationEvent";
    }

    private void handleOneEvent(ResourceEvent<HostRelationEventDetail> event) {
        log.info("start to handle host relation event:{}", JsonUtils.toJson(event));
        StopWatch watch = new StopWatch();
        watch.start("handleOneEventIndeed");
        handleOneEventIndeed(event);
        watch.stop();
        if (watch.getTotalTimeMillis() > 3000) {
            log.warn("PERF:SLOW:handle hostRelationEvent:" + watch.prettyPrint());
        } else {
            log.debug("handle hostRelationEvent:" + watch.prettyPrint());
        }
    }

    private void setDefaultLastTimeForHostTopoIfNeed(ResourceEvent<HostRelationEventDetail> event,
                                                     HostTopoDTO hostTopoDTO) {
        if (hostTopoDTO.getLastTime() == null || hostTopoDTO.getLastTime() < 0) {
            hostTopoDTO.setLastTime(event.getCreateTime());
        }
    }

    private void handleOneEventIndeed(ResourceEvent<HostRelationEventDetail> event) {
        String eventType = event.getEventType();
        HostTopoDTO hostTopoDTO = HostTopoDTO.fromHostRelationEvent(event.getDetail());
        setDefaultLastTimeForHostTopoIfNeed(event, hostTopoDTO);
        switch (eventType) {
            case ResourceWatchReq.EVENT_TYPE_CREATE:
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
                break;
            case ResourceWatchReq.EVENT_TYPE_DELETE:
                // 删除拓扑数据
                int deletedHostTopoNum = hostTopoDAO.deleteHostTopoBeforeOrEqualLastTime(
                    hostTopoDTO.getHostId(),
                    hostTopoDTO.getBizId(),
                    hostTopoDTO.getSetId(),
                    hostTopoDTO.getModuleId(),
                    hostTopoDTO.getLastTime()
                );
                if (deletedHostTopoNum > 0) {
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
                } else {
                    log.warn("no hostTopo deleted, delete event may expire for long time");
                }
                break;
            default:
                break;
        }
    }

    /**
     * 将主机拓扑表中的拓扑数据同步至主机表
     *
     * @param hostTopoDTO 主机拓扑信息
     * @return 受影响的主机数量
     */
    private int updateTopoToHost(HostTopoDTO hostTopoDTO) {
        // 若主机存在需将拓扑信息同步至主机信息冗余字段
        int affectedHostNum = noTenantHostDAO.syncHostTopo(hostTopoDTO.getHostId());
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
        ApplicationHostDTO host = noTenantHostDAO.getHostById(hostTopoDTO.getHostId());
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
