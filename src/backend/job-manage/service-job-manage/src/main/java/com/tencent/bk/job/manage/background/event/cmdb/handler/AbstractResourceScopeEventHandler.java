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
import com.tencent.bk.job.common.cc.model.result.ResourceEvent;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.manage.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;

/**
 * 资源范围实体（业务、业务集等）类别的事件处理器（在调用线程中处理事件）
 */
@Slf4j
abstract class AbstractResourceScopeEventHandler<T> implements CmdbEventHandler<T> {

    private final ApplicationService applicationService;
    private final String tenantId;

    public AbstractResourceScopeEventHandler(ApplicationService applicationService,
                                             String tenantId) {
        this.applicationService = applicationService;
        this.tenantId = tenantId;
    }

    /**
     * 通过事件详情构造通用Job业务对象
     *
     * @param eventDetail 事件详情
     * @return 通用Job业务对象
     */
    abstract ApplicationDTO buildApp(T eventDetail);

    @Override
    public void handleEvent(ResourceEvent<T> event) {
        ApplicationDTO newApp = buildApp(event.getDetail());
        newApp.setTenantId(tenantId);
        ApplicationDTO oldApp = queryAppByScope(newApp.getScope());
        String eventType = event.getEventType();
        switch (eventType) {
            case ResourceWatchReq.EVENT_TYPE_CREATE:
                handleCreateEvent(oldApp, newApp);
                break;
            case ResourceWatchReq.EVENT_TYPE_UPDATE:
                handleUpdateEvent(oldApp, newApp);
                break;
            case ResourceWatchReq.EVENT_TYPE_DELETE:
                handleDeleteEvent(oldApp);
                break;
            default:
                log.warn("Ignore unknown type event:{}", event);
                break;
        }
    }

    @Override
    public int getExtraThreadNum() {
        // 业务/业务集信息为低频变动信息，在监听线程直接处理，无需额外消耗线程
        return 0;
    }

    /**
     * 查出本地已经存在的业务/业务集信息
     *
     * @param scope 资源范围实体（类型与ID的组合，例如：["biz",1]）
     * @return 通用的Job业务对象（覆盖业务/业务集等多种ResourceScope）
     */
    private ApplicationDTO queryAppByScope(ResourceScope scope) {
        try {
            return applicationService.getAppByScopeIncludingDeleted(scope);
        } catch (NotFoundException e) {
            return null;
        }
    }

    private void handleCreateEvent(ApplicationDTO oldApp, ApplicationDTO newApp) {
        if (oldApp == null) {
            applicationService.createApp(newApp);
        } else {
            // 已存在业务/业务集的Create事件，仅更新属性
            updateProps(oldApp, newApp);
            applicationService.updateApp(oldApp);
            // 如果本地已被软删除，将其恢复
            if (oldApp.isDeleted()) {
                applicationService.restoreDeletedApp(oldApp.getId());
            }
        }
    }

    private void handleUpdateEvent(ApplicationDTO oldApp, ApplicationDTO newApp) {
        if (oldApp != null) {
            updateProps(oldApp, newApp);
            applicationService.updateApp(oldApp);
        } else {
            // 不存在的业务/业务集（已归档）的Update事件，忽略
            log.info("ignore update event of invalid scope:{}", newApp.getScope());
        }
    }

    private void handleDeleteEvent(ApplicationDTO oldApp) {
        if (oldApp != null) {
            applicationService.deleteApp(oldApp.getId());
        } else {
            log.info("ignore delete event of not exist scope");
        }
    }

    /**
     * 更新需要关注的Job业务属性
     *
     * @param oldApp 旧的Job业务信息
     * @param newApp 新的Job业务信息
     */
    private void updateProps(ApplicationDTO oldApp, ApplicationDTO newApp) {
        oldApp.setName(newApp.getName());
        oldApp.setBkSupplierAccount(newApp.getBkSupplierAccount());
        oldApp.setLanguage(newApp.getLanguage());
        oldApp.setTimeZone(newApp.getTimeZone());
    }
}
