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

package com.tencent.bk.job.common.cc.sdk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.cc.config.CmdbConfig;
import com.tencent.bk.job.common.cc.model.bizset.BizInfo;
import com.tencent.bk.job.common.cc.model.bizset.BizSetInfo;
import com.tencent.bk.job.common.cc.model.bizset.BizSetScope;
import com.tencent.bk.job.common.cc.model.bizset.Page;
import com.tencent.bk.job.common.cc.model.bizset.SearchBizInBusinessReq;
import com.tencent.bk.job.common.cc.model.bizset.SearchBizInBusinessSetResp;
import com.tencent.bk.job.common.cc.model.bizset.SearchBizSetReq;
import com.tencent.bk.job.common.cc.model.bizset.SearchBizSetResp;
import com.tencent.bk.job.common.cc.model.req.ResourceWatchReq;
import com.tencent.bk.job.common.cc.model.result.BizSetEventDetail;
import com.tencent.bk.job.common.cc.model.result.BizSetRelationEventDetail;
import com.tencent.bk.job.common.cc.model.result.ResourceWatchResult;
import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.esb.config.EsbConfig;
import com.tencent.bk.job.common.esb.model.EsbReq;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.sdk.AbstractEsbSdkClient;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.util.http.HttpHelperFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpPost;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * cmdb API Client - 业务集相关
 */
@Slf4j
public class BizSetCmdbClient extends AbstractEsbSdkClient implements IBizSetCmdbClient {

    private final String cmdbSupplierAccount;

    private static final String SEARCH_BUSINESS_SET = "/api/c/compapi/v2/cc/list_business_set/";
    private static final String SEARCH_BIZ_IN_BUSINESS_SET = "/api/c/compapi/v2/cc/list_business_in_business_set/";
    private static final String RESOURCE_WATCH = "/api/c/compapi/v2/cc/resource_watch/";

    public BizSetCmdbClient(EsbConfig esbConfig, CmdbConfig cmdbConfig) {
        super(esbConfig.getEsbUrl(), esbConfig.getAppCode(),
            esbConfig.getAppSecret(), null, esbConfig.isUseEsbTestEnv());
        this.cmdbSupplierAccount = cmdbConfig.getDefaultSupplierAccount();
    }

    public <T extends EsbReq> T makeCmdbBaseReq(Class<T> reqClass) {
        return makeBaseReqByWeb(reqClass, null, "admin", cmdbSupplierAccount);
    }

    /**
     * 查询业务集数量
     *
     * @return 业务集数量
     */
    public int searchBizSetCount() {
        SearchBizSetReq req = makeCmdbBaseReq(SearchBizSetReq.class);
        Page page = new Page();
        page.setStart(0);
        page.setLimit(0);
        page.setEnableCount(true);
        req.setPage(page);
        req.setFilter(null);
        try {
            EsbResp<SearchBizSetResp> resp = getEsbRespByReq(
                HttpPost.METHOD_NAME,
                SEARCH_BUSINESS_SET,
                req,
                new TypeReference<EsbResp<SearchBizSetResp>>() {
                });
            if (!resp.getResult()) {
                throw new InternalException(ErrorCode.CMDB_API_DATA_ERROR, null);
            }
            return resp.getData().getCount();
        } catch (Exception e) {
            throw new InternalException(e, ErrorCode.CMDB_API_DATA_ERROR, null);
        }
    }

    /**
     * 查询全部业务集信息
     *
     * @return 业务集信息列表
     */
    public List<BizSetInfo> searchAllBizSet() {
        int bizSetCount = searchBizSetCount();
        log.info("{} bizSet found in cmdb", bizSetCount);
        // 分批查询
        int limit = 500;
        int start = 0;
        List<BizSetInfo> bizSetInfoList = new ArrayList<>();
        while (start < bizSetCount) {
            bizSetInfoList.addAll(searchBizSet(start, limit));
            start += limit;
        }
        return bizSetInfoList;
    }

    /**
     * 查询业务集信息
     *
     * @return 业务集信息列表
     */
    private List<BizSetInfo> searchBizSet(int start, int limit) {
        SearchBizSetReq req = makeCmdbBaseReq(SearchBizSetReq.class);
        Page page = new Page();
        page.setEnableCount(false);
        page.setStart(start);
        page.setLimit(limit);
        req.setPage(page);
        req.setFilter(null);
        try {
            EsbResp<SearchBizSetResp> resp = getEsbRespByReq(
                HttpPost.METHOD_NAME,
                SEARCH_BUSINESS_SET,
                req,
                new TypeReference<EsbResp<SearchBizSetResp>>() {
                });
            if (!resp.getResult() || resp.getData() == null) {
                throw new InternalException(ErrorCode.CMDB_API_DATA_ERROR, null);
            }
            return resp.getData().getBizSetList();
        } catch (Exception e) {
            throw new InternalException(e, ErrorCode.CMDB_API_DATA_ERROR, null);
        }
    }

    /**
     * 查询业务集中的业务数量
     *
     * @param bizSetId 业务集ID
     * @return 业务数量
     */
    private int searchBizCountInBusinessSet(long bizSetId) {
        SearchBizInBusinessReq req = makeCmdbBaseReq(SearchBizInBusinessReq.class);
        req.setBizSetId(bizSetId);
        Page page = new Page();
        page.setEnableCount(true);
        page.setStart(0);
        page.setLimit(0);
        req.setPage(page);
        try {
            EsbResp<SearchBizInBusinessSetResp> resp = getEsbRespByReq(
                HttpPost.METHOD_NAME,
                SEARCH_BIZ_IN_BUSINESS_SET,
                req,
                new TypeReference<EsbResp<SearchBizInBusinessSetResp>>() {
                });
            if (!resp.getResult()) {
                throw new InternalException(ErrorCode.CMDB_API_DATA_ERROR, null);
            }
            return resp.getData().getCount();
        } catch (Exception e) {
            throw new InternalException(e, ErrorCode.CMDB_API_DATA_ERROR, null);
        }
    }

    /**
     * 查询业务集内业务信息
     *
     * @return 业务ID列表
     */
    public List<BizInfo> searchBizInBizSet(long bizSetId) {
        int bizCount = searchBizCountInBusinessSet(bizSetId);
        log.info("{} biz found in bizSet {} from cmdb", bizCount, bizSetId);
        // 分批查询
        int limit = 500;
        int start = 0;
        List<BizInfo> bizInfoList = new ArrayList<>();
        while (start < bizCount) {
            bizInfoList.addAll(searchBizInBizSet(bizSetId, start, limit));
            start += limit;
        }
        return bizInfoList;
    }

    private List<BizInfo> searchBizInBizSet(long bizSetId, int start, int limit) {
        SearchBizInBusinessReq req = makeCmdbBaseReq(SearchBizInBusinessReq.class);
        req.setBizSetId(bizSetId);
        Page page = new Page();
        page.setEnableCount(false);
        page.setStart(start);
        page.setLimit(limit);
        req.setPage(page);
        try {
            EsbResp<SearchBizInBusinessSetResp> resp = getEsbRespByReq(
                HttpPost.METHOD_NAME,
                SEARCH_BIZ_IN_BUSINESS_SET,
                req,
                new TypeReference<EsbResp<SearchBizInBusinessSetResp>>() {
                });
            if (!resp.getResult() || resp.getData() == null) {
                throw new InternalException(ErrorCode.CMDB_API_DATA_ERROR, null);
            }
            return resp.getData().getBizList();
        } catch (Exception e) {
            throw new InternalException(e, ErrorCode.CMDB_API_DATA_ERROR, null);
        }
    }

    private ApplicationDTO convertToMatchAllApp(BizSetInfo bizSetInfo) {
        ApplicationDTO appInfoDTO = convertToAppWithoutType(bizSetInfo);
        appInfoDTO.setAppType(AppTypeEnum.ALL_APP);
        appInfoDTO.setSubAppIds(null);
        return appInfoDTO;
    }

    private ApplicationDTO convertToBizSetApp(BizSetInfo bizSetInfo) {
        ApplicationDTO appInfoDTO = convertToAppWithoutType(bizSetInfo);
        appInfoDTO.setAppType(AppTypeEnum.APP_SET);
        List<BizInfo> bizList = searchBizInBizSet(bizSetInfo.getId());
        appInfoDTO.setSubAppIds(bizList.parallelStream()
            .map(BizInfo::getId).collect(Collectors.toList()));
        return appInfoDTO;
    }

    private ApplicationDTO convertToAppWithoutType(BizSetInfo bizSetInfo) {
        ApplicationDTO appInfoDTO = new ApplicationDTO();
        appInfoDTO.setBkSupplierAccount(bizSetInfo.getSupplierAccount());
        appInfoDTO.setName(bizSetInfo.getName());
        appInfoDTO.setMaintainers(bizSetInfo.getMaintainer());
        appInfoDTO.setOperateDeptId(bizSetInfo.getOperateDeptId());
        appInfoDTO.setTimeZone(bizSetInfo.getTimezone());
        appInfoDTO.setScope(
            new ResourceScope(ResourceScopeTypeEnum.BIZ_SET, bizSetInfo.getId().toString()));
        return appInfoDTO;
    }

    @Override
    public List<ApplicationDTO> getAllBizSetApps() {
        List<BizSetInfo> bizSetInfoList = searchAllBizSet();
        return bizSetInfoList.parallelStream().map(bizSetInfo -> {
            // 查询业务集下包含的子业务
            BizSetScope scope = bizSetInfo.getScope();
            if (scope != null && scope.isMatchAll()) {
                // 全业务
                return convertToMatchAllApp(bizSetInfo);
            }
            return convertToBizSetApp(bizSetInfo);
        }).collect(Collectors.toList());
    }

    @Override
    public ResourceWatchResult<BizSetEventDetail> getBizSetEvents(Long startTime, String cursor) {
        ResourceWatchReq req = makeCmdbBaseReq(ResourceWatchReq.class);
        req.setFields(Arrays.asList("bk_biz_set_id", "bk_biz_set_name", "bk_biz_maintainer", "time_zone",
            "language"));
        req.setResource("biz_set");
        req.setCursor(cursor);
        req.setStartTime(startTime);
        try {
            EsbResp<ResourceWatchResult<BizSetEventDetail>> resp = getEsbRespByReq(
                HttpPost.METHOD_NAME,
                RESOURCE_WATCH,
                req,
                new TypeReference<EsbResp<ResourceWatchResult<BizSetEventDetail>>>() {
                },
                HttpHelperFactory.getLongRetryableHttpHelper());
            if (!resp.getResult()) {
                throw new InternalException(ErrorCode.CMDB_API_DATA_ERROR, null);
            }
            return resp.getData();
        } catch (Exception e) {
            throw new InternalException(e, ErrorCode.CMDB_API_DATA_ERROR, null);
        }
    }

    @Override
    public ResourceWatchResult<BizSetRelationEventDetail> getBizSetRelationEvents(Long startTime, String cursor) {
        ResourceWatchReq req = makeCmdbBaseReq(ResourceWatchReq.class);
        req.setFields(Arrays.asList("bk_biz_set_id", "bk_biz_ids"));
        req.setResource("biz_set_relation");
        req.setCursor(cursor);
        req.setStartTime(startTime);
        try {
            EsbResp<ResourceWatchResult<BizSetRelationEventDetail>> resp = getEsbRespByReq(
                HttpPost.METHOD_NAME,
                RESOURCE_WATCH,
                req,
                new TypeReference<EsbResp<ResourceWatchResult<BizSetRelationEventDetail>>>() {
                },
                HttpHelperFactory.getLongRetryableHttpHelper());
            if (!resp.getResult()) {
                throw new InternalException(ErrorCode.CMDB_API_DATA_ERROR, null);
            }
            return resp.getData();
        } catch (Exception e) {
            throw new InternalException(e, ErrorCode.CMDB_API_DATA_ERROR, null);
        }
    }
}
