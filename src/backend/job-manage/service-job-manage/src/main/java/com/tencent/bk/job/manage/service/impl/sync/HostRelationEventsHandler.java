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
import com.tencent.bk.job.common.cc.model.result.HostRelationEventDetail;
import com.tencent.bk.job.common.cc.model.result.ResourceEvent;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import com.tencent.bk.job.manage.manager.host.HostCache;
import com.tencent.bk.job.manage.model.dto.HostTopoDTO;
import com.tencent.bk.job.manage.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.util.StopWatch;

import java.util.concurrent.BlockingQueue;

@Slf4j
public class HostRelationEventsHandler extends EventsHandler<HostRelationEventDetail> {

    private final DSLContext dslContext;
    private final ApplicationService applicationService;
    private final ApplicationHostDAO applicationHostDAO;
    private final HostTopoDAO hostTopoDAO;
    private final HostCache hostCache;

    public HostRelationEventsHandler(BlockingQueue<ResourceEvent<HostRelationEventDetail>> queue,
                                     DSLContext dslContext,
                                     ApplicationService applicationService,
                                     ApplicationHostDAO applicationHostDAO,
                                     HostTopoDAO hostTopoDAO,
                                     HostCache hostCache) {
        super(queue);
        this.dslContext = dslContext;
        this.applicationService = applicationService;
        this.applicationHostDAO = applicationHostDAO;
        this.hostTopoDAO = hostTopoDAO;
        this.hostCache = hostCache;
    }

    @Override
    void handleEvent(ResourceEvent<HostRelationEventDetail> event) {
        handleOneEvent(event);
    }

    private void handleOneEvent(ResourceEvent<HostRelationEventDetail> event) {
        log.info("start to handle host relation event:{}", JsonUtils.toJson(event));
        HostTopoDTO hostTopoDTO = HostTopoDTO.fromHostRelationEvent(event.getDetail());
        Long appId = hostTopoDTO.getBizId();
        try {
            StopWatch watch = new StopWatch();
            watch.start("handleOneEventIndeed");
            handleOneEventIndeed(event);
            watch.stop();
            if (watch.getTotalTimeMillis() > 3000) {
                log.warn("PERF:SLOW:handle hostRelationEvent:" + watch.prettyPrint());
            } else {
                log.debug("handle hostRelationEvent:" + watch.prettyPrint());
            }
        } catch (Throwable t) {
            FormattingTuple msg = MessageFormatter.format(
                "Fail to handle hostRelationEvent of appId {}, event:{}",
                new String[]{
                    appId.toString(),
                    event.toString()
                }
            );
            log.error(msg.getMessage(), t);
        }
    }

    private void handleOneEventIndeed(ResourceEvent<HostRelationEventDetail> event) {
        String eventType = event.getEventType();
        HostTopoDTO hostTopoDTO = HostTopoDTO.fromHostRelationEvent(event.getDetail());
        switch (eventType) {
            case ResourceWatchReq.EVENT_TYPE_CREATE:
                // 插入拓扑数据
                hostTopoDAO.insertHostTopo(hostTopoDTO);
                // 同步拓扑数据至主机表冗余字段
                updateTopoToHost(hostTopoDTO);
                // 更新主机缓存
                updateHostCacheWhenRelCreated(hostTopoDTO);
                break;
            case ResourceWatchReq.EVENT_TYPE_DELETE:
                // 删除拓扑数据
                hostTopoDAO.deleteHostTopo(
                    hostTopoDTO.getHostId(),
                    hostTopoDTO.getBizId(),
                    hostTopoDTO.getSetId(),
                    hostTopoDTO.getModuleId()
                );
                // 同步拓扑数据至主机表冗余字段
                updateTopoToHost(hostTopoDTO);
                // 更新主机缓存
                updateHostCacheWhenRelationDeleted(hostTopoDTO);
                break;
            default:
                break;
        }
    }

    /**
     * 将主机拓扑表中的拓扑数据同步至主机表
     *
     * @param hostTopoDTO 主机拓扑信息
     */
    private void updateTopoToHost(HostTopoDTO hostTopoDTO) {
        // 若主机存在需将拓扑信息同步至主机信息冗余字段
        int affectedNum = applicationHostDAO.syncHostTopo(hostTopoDTO.getHostId());
        if (affectedNum > 0) {
            log.info("host topo synced: affectedNum={}", affectedNum);
        } else if (affectedNum == 0) {
            log.info("no host topo synced");
        } else {
            log.warn("cannot find hostInfo by hostId:{}, wait for host event or sync", hostTopoDTO.getHostId());
        }
    }

    /**
     * 当主机关系被创建时，更新缓存中的主机信息
     *
     * @param hostTopoDTO 主机拓扑信息
     */
    private void updateHostCacheWhenRelCreated(HostTopoDTO hostTopoDTO) {
        ApplicationHostDTO host = applicationHostDAO.getHostById(hostTopoDTO.getHostId());
        if (host != null && applicationService.existBiz(host.getBizId())) {
            hostCache.addOrUpdateHost(host);
            log.info("host cached updated: hostId={}", host.getHostId());
        }
    }

    /**
     * 当主机关系被删除时，更新缓存中的主机信息
     *
     * @param hostTopoDTO 主机拓扑信息
     */
    private void updateHostCacheWhenRelationDeleted(HostTopoDTO hostTopoDTO) {
        ApplicationHostDTO host = applicationHostDAO.getHostById(hostTopoDTO.getHostId());
        if (host == null) {
            return;
        }
        int curAppRelationCount = hostTopoDAO.countHostTopo(hostTopoDTO.getBizId(), hostTopoDTO.getHostId());
        int hostRelationCount = hostTopoDAO.countHostTopo(null, hostTopoDTO.getHostId());
        if (curAppRelationCount != 0) {
            return;
        }
        if (hostRelationCount == 0) {
            // 主机被移除
            hostCache.deleteHost(host);
            log.info("host cached deleted: hostId={}", host.getHostId());
        } else {
            // 主机被转移到其他业务下
            if (applicationService.existBiz(host.getBizId())) {
                hostCache.addOrUpdateHost(host);
                log.info("host cached updated: hostId={}", host.getHostId());
            }
        }
    }
}
