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

package com.tencent.bk.job.manage.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class JobManageConfig {

    /**
     * 功能开关 - 启用账号鉴权
     */
    @Value("${feature.toggle.auth-account.mode:enabled}")
    private String enableAuthAccountMode;

    /**
     * 账号鉴权灰度业务(用,分隔)
     */
    @Value("${feature.toggle.auth-account.gray.apps:}")
    private String accountAuthGrayApps;


    //---------------------------- Job Config ---------------------------------

    @Value("${job.edition:ee}")
    private String jobEdition;

    @Value("${job.web.url:}")
    private String jobWebUrl;

    @Value("${bk.helper.url:}")
    private String bkHelperUrl;

    @Value("${bk.doc.root:}")
    private String bkDocRoot;

    @Value("${bk.feedback.root:}")
    private String bkFeedBackRoot;

    @Value("${bk.ce.root:https://bk.tencent.com}")
    private String bkCERoot;

    @Value("${job.manage.sync.app.enabled:true}")
    private boolean enableSyncApp;

    @Value("${job.manage.sync.host.enabled:true}")
    private boolean enableSyncHost;

    @Value("${job.manage.sync.agentStatus.enabled:true}")
    private boolean enableSyncAgentStatus;

    @Value("${job.manage.sync.resource.watch.enabled:true}")
    private boolean enableResourceWatch;

    @Value("${job.manage.sync.hostEvent.handlerNum:3}")
    private int hostEventHandlerNum;

    @Value("${swagger.url:swagger.job.com}")
    private String swaggerUrl;

    //---------------------------- Cmdb Config ---------------------------------
    @Value("${cmdb.default.supplier.account:0}")
    private String defaultSupplierAccount;

    @Value("${cmdb.server.url:}")
    private String cmdbServerUrl;

    @Value("${cmdb.app.index.path:/#/{scopeType}/{scopeId}/index}")
    private String cmdbAppIndexPath;

    //---------------------------- Paas Config ---------------------------------
    @Value("${paas.server.url:}")
    private String paasServerUrl;

    @Value("${paas.nodeman.path:/o/bk_nodeman}")
    private String paasNodemanPath;

    //---------------------------- Nodeman Config ---------------------------------
    @Value("${nodeman.server.url:}")
    private String nodemanServerUrl;

    //---------------------------- Job Encryption Config ---------------------------------
    /**
     * Symmetric encryption password
     */
    @Value("${job.encrypt.password}")
    private String encryptPassword;
}
