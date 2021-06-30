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

package com.tencent.bk.job.common.cc.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class CcConfig {

    @Value("${cmdb.default.supplier.account:0}")
    private String defaultSupplierAccount;

    @Value("${cmdb.query.threads.num:20}")
    private int cmdbQueryThreadsNum;

    @Value("${cmdb.interface.briefCacheTopo.enable:false}")
    private Boolean enableInterfaceBriefCacheTopo;

    @Value("${cmdb.interface.retry.enable:false}")
    private Boolean enableInterfaceRetry;

    @Value("${cmdb.interface.findHostRelation.longTerm.concurrency:20}")
    private Integer findHostRelationLongTermConcurrency;

    @Value("${cmdb.interface.optimize.lock.enable:false}")
    private Boolean enableLockOptimize;

    @Value("${cmdb.interface.flowControl.enable:false}")
    private Boolean enableFlowControl;

    @Value("${cmdb.interface.flowControl.precision:20}")
    private Integer flowControlPrecision;

    @Value("${cmdb.interface.flowControl.default.limit:500}")
    private Integer flowControlDefaultLimit;

    @Value("${cmdb.interface.flowControl.resources:get_biz_brief_cache_topo:1500}")
    private String flowControlResourcesStr;
}
