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

package com.tencent.bk.job.common.cc.sdk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.cc.config.CmdbConfig;
import com.tencent.bk.job.common.cc.model.bizset.Page;
import com.tencent.bk.job.common.cc.model.filter.CmdbFilter;
import com.tencent.bk.job.common.cc.model.response.CountInfo;
import com.tencent.bk.job.common.cc.model.tenantset.ListTenantSetReq;
import com.tencent.bk.job.common.cc.model.tenantset.TenantSetInfo;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.HttpMethodEnum;
import com.tencent.bk.job.common.constant.TenantIdConstants;
import com.tencent.bk.job.common.esb.config.AppProperties;
import com.tencent.bk.job.common.esb.config.BkApiGatewayProperties;
import com.tencent.bk.job.common.esb.model.EsbReq;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.InternalCmdbException;
import com.tencent.bk.job.common.paas.user.IVirtualAdminAccountProvider;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.common.util.FlowController;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * cmdb API Client - 租户集相关
 */
@Slf4j
public class TenantSetCmdbClient extends BaseCmdbClient implements ITenantSetCmdbClient {


    public TenantSetCmdbClient(AppProperties appProperties,
                               BkApiGatewayProperties bkApiGatewayProperties,
                               CmdbConfig cmdbConfig,
                               FlowController flowController,
                               MeterRegistry meterRegistry,
                               TenantEnvService tenantEnvService,
                               IVirtualAdminAccountProvider virtualAdminAccountProvider) {
        super(
            flowController,
            appProperties,
            bkApiGatewayProperties,
            cmdbConfig,
            meterRegistry,
            tenantEnvService,
            virtualAdminAccountProvider,
            null
        );
    }

    /**
     * 查询租户集数量
     *
     * @return 租户集数量
     */
    public int listTenantSetCount() {
        ListTenantSetReq req = EsbReq.buildRequest(ListTenantSetReq.class, cmdbSupplierAccount);
        Page page = new Page();
        page.setStart(0);
        page.setLimit(0);
        page.setEnableCount(true);
        req.setPage(page);
        req.setFilter(null);
        try {
            EsbResp<CountInfo<TenantSetInfo>> resp = requestCmdbApi(
                TenantIdConstants.SYSTEM_TENANT_ID,
                HttpMethodEnum.POST,
                LIST_TENANT_SET,
                null,
                req,
                new TypeReference<EsbResp<CountInfo<TenantSetInfo>>>() {
                });
            if (!resp.getResult()) {
                throw new InternalCmdbException(ErrorCode.CMDB_API_DATA_ERROR, null);
            }
            return resp.getData().getCount();
        } catch (Exception e) {
            throw new InternalCmdbException(e, ErrorCode.CMDB_API_DATA_ERROR, null);
        }
    }

    /**
     * 查询全部租户集信息
     *
     * @return 租户集信息列表
     */
    @Override
    public List<TenantSetInfo> listAllTenantSet() {
        int tenantSetCount = listTenantSetCount();
        log.info("{} tenantSet found in cmdb", tenantSetCount);
        // 分批查询
        int limit = 500;
        int start = 0;
        List<TenantSetInfo> tenantSetInfoList = new ArrayList<>();
        while (start < tenantSetCount) {
            tenantSetInfoList.addAll(listTenantSet(null, start, limit));
            start += limit;
        }
        return tenantSetInfoList;
    }

    /**
     * 查询租户集信息
     *
     * @param filter 查询条件
     * @param start  分页起始
     * @param limit  每页大小
     * @return 租户集信息列表
     */
    private List<TenantSetInfo> listTenantSet(CmdbFilter filter, int start, int limit) {
        ListTenantSetReq req = makeCmdbBaseReq(ListTenantSetReq.class);
        Page page = new Page();
        page.setEnableCount(false);
        page.setStart(start);
        page.setLimit(limit);
        req.setPage(page);
        req.setFilter(filter);
        try {
            EsbResp<CountInfo<TenantSetInfo>> resp = requestCmdbApi(
                TenantIdConstants.SYSTEM_TENANT_ID,
                HttpMethodEnum.POST,
                LIST_TENANT_SET,
                null,
                req,
                new TypeReference<EsbResp<CountInfo<TenantSetInfo>>>() {
                });
            if (!resp.getResult() || resp.getData() == null) {
                throw new InternalCmdbException(ErrorCode.CMDB_API_DATA_ERROR, null);
            }
            return resp.getData().getInfo();
        } catch (Exception e) {
            throw new InternalCmdbException(e, ErrorCode.CMDB_API_DATA_ERROR, null);
        }
    }
}
