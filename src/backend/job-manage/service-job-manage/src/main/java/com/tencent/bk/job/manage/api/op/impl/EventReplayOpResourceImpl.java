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

package com.tencent.bk.job.manage.api.op.impl;

import com.tencent.bk.job.common.cc.model.result.HostEventDetail;
import com.tencent.bk.job.common.cc.model.result.ResourceEvent;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.manage.api.op.EventReplayOpResource;
import com.tencent.bk.job.manage.background.event.cmdb.CmdbEventManager;
import com.tencent.bk.job.manage.background.event.cmdb.TenantHostEventWatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class EventReplayOpResourceImpl implements EventReplayOpResource {

    private final CmdbEventManager cmdbEventManager;

    @Autowired
    public EventReplayOpResourceImpl(CmdbEventManager cmdbEventManager) {
        this.cmdbEventManager = cmdbEventManager;
    }

    @Override
    public Response<Boolean> replayHostEvent(String username, String tenantId, ResourceEvent<HostEventDetail> event) {
        TenantHostEventWatcher tenantHostEventWatcher = cmdbEventManager.getTenantHostEventWatcher(tenantId);
        if (tenantHostEventWatcher == null) {
            return Response.buildSuccessResp(false);
        }
        tenantHostEventWatcher.handleEvent(event);
        return Response.buildSuccessResp(true);
    }

}
