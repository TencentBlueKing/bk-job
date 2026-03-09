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
import com.tencent.bk.job.common.cc.model.result.BizSetRelationEventDetail;
import com.tencent.bk.job.common.cc.model.result.ResourceEvent;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.model.dto.ApplicationAttrsDO;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.manage.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 业务集关系事件处理器（在调用线程中处理事件）
 */
@Slf4j
public class BizSetRelationEventHandler implements CmdbEventHandler<BizSetRelationEventDetail> {
    private final ApplicationService applicationService;

    public BizSetRelationEventHandler(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @Override
    public void handleEvent(ResourceEvent<BizSetRelationEventDetail> event) {
        String eventType = event.getEventType();
        switch (eventType) {
            case ResourceWatchReq.EVENT_TYPE_UPDATE:
                handleUpdateEvent(event.getDetail());
                break;
            default:
                // CMDB接口声明中业务集关系只有更新类型的事件，其余类型可忽略
                log.info("Ignore {} event", eventType);
                break;
        }
    }

    private void handleUpdateEvent(BizSetRelationEventDetail eventDetail) {
        Long bizSetId = eventDetail.getBizSetId();
        List<Long> latestSubBizIds = eventDetail.getBizIds();
        ApplicationDTO localApp = applicationService.getAppByScopeIncludingDeleted(
            new ResourceScope(ResourceScopeTypeEnum.BIZ_SET.getValue(), String.valueOf(bizSetId))
        );
        if (localApp == null || localApp.isDeleted()) {
            return;
        }
        ApplicationAttrsDO attrs = localApp.getAttrs();
        if (attrs == null) {
            attrs = new ApplicationAttrsDO();
            localApp.setAttrs(attrs);
        }
        attrs.setSubBizIds(latestSubBizIds);
        applicationService.updateApp(localApp);
        log.info("BizSet event handled: subBizIds of {} changed to {}", localApp.getScope(), latestSubBizIds);
    }

    @Override
    public int getExtraThreadNum() {
        // 业务集关系信息为低频变动信息，在监听线程直接处理，无需额外消耗线程
        return 0;
    }
}
