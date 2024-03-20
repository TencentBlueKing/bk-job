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
import com.tencent.bk.job.common.cc.model.bizset.BizSetFilter;
import com.tencent.bk.job.common.cc.model.bizset.BizSetInfo;
import com.tencent.bk.job.common.cc.model.bizset.BizSetScope;
import com.tencent.bk.job.common.cc.model.bizset.Page;
import com.tencent.bk.job.common.cc.model.bizset.Rule;
import com.tencent.bk.job.common.cc.model.bizset.SearchBizInBusinessReq;
import com.tencent.bk.job.common.cc.model.bizset.SearchBizInBusinessSetResp;
import com.tencent.bk.job.common.cc.model.bizset.SearchBizSetReq;
import com.tencent.bk.job.common.cc.model.bizset.SearchBizSetResp;
import com.tencent.bk.job.common.cc.model.filter.RuleConditionEnum;
import com.tencent.bk.job.common.cc.model.filter.RuleOperatorEnum;
import com.tencent.bk.job.common.cc.model.req.ResourceWatchReq;
import com.tencent.bk.job.common.cc.model.result.BizSetEventDetail;
import com.tencent.bk.job.common.cc.model.result.BizSetRelationEventDetail;
import com.tencent.bk.job.common.cc.model.result.ResourceWatchResult;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.HttpMethodEnum;
import com.tencent.bk.job.common.esb.config.AppProperties;
import com.tencent.bk.job.common.esb.config.BkApiGatewayProperties;
import com.tencent.bk.job.common.esb.config.EsbProperties;
import com.tencent.bk.job.common.esb.constants.ApiGwType;
import com.tencent.bk.job.common.esb.model.EsbReq;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.InternalCmdbException;
import com.tencent.bk.job.common.util.FlowController;
import com.tencent.bk.job.common.util.http.HttpHelperFactory;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * cmdb API Client - 业务集相关
 */
@Slf4j
public class BizSetCmdbClient extends BaseCmdbApiClient implements IBizSetCmdbClient {


    public BizSetCmdbClient(AppProperties appProperties,
                            EsbProperties esbProperties,
                            BkApiGatewayProperties bkApiGatewayProperties,
                            CmdbConfig cmdbConfig,
                            FlowController flowController,
                            MeterRegistry meterRegistry) {
        super(flowController, appProperties, esbProperties,
            bkApiGatewayProperties, cmdbConfig, meterRegistry, null);
    }

    /**
     * 查询业务集数量
     *
     * @return 业务集数量
     */
    public int searchBizSetCount() {
        SearchBizSetReq req = EsbReq.buildRequest(SearchBizSetReq.class, cmdbSupplierAccount);
        Page page = new Page();
        page.setStart(0);
        page.setLimit(0);
        page.setEnableCount(true);
        req.setPage(page);
        req.setFilter(null);
        try {
            EsbResp<SearchBizSetResp> resp = requestCmdbApi(
                ApiGwType.ESB,
                HttpMethodEnum.POST,
                SEARCH_BUSINESS_SET,
                null,
                req,
                new TypeReference<EsbResp<SearchBizSetResp>>() {
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
            bizSetInfoList.addAll(searchBizSet(null, start, limit));
            start += limit;
        }
        return bizSetInfoList;
    }

    /**
     * 查询业务集信息
     *
     * @return 业务集信息列表
     */
    public List<BizSetInfo> ListBizSetByIds(List<Long> bizSetIds) {
        BizSetFilter filter = new BizSetFilter();
        filter.setCondition(BizSetFilter.CONDITION_AND);
        Rule bizSetIdRule = new Rule();
        bizSetIdRule.setField("bk_biz_set_id");
        bizSetIdRule.setOperator(RuleOperatorEnum.IN.getOperator());
        bizSetIdRule.setValue(bizSetIds);
        filter.setRules(Collections.singletonList(bizSetIdRule));
        List<BizSetInfo> bizSetInfoList = searchBizSet(filter, 0, bizSetIds.size());

        if (bizSetInfoList == null) {
            return new ArrayList<>();
        }

        bizSetInfoList.forEach(bizSetInfo -> {
            // 查询业务集下包含的子业务(全业务除外)
            BizSetScope scope = bizSetInfo.getScope();
            if (scope != null && !scope.isMatchAll()) {
                List<BizInfo> bizList = searchBizInBizSet(bizSetInfo.getId());
                bizSetInfo.setBizList(bizList);
            }
        });
        return bizSetInfoList;
    }

    /**
     * 查询业务集信息
     *
     * @param filter 查询条件
     * @param start  分页起始
     * @param limit  每页大小
     * @return 业务集信息列表
     */
    private List<BizSetInfo> searchBizSet(BizSetFilter filter, int start, int limit) {
        SearchBizSetReq req = makeCmdbBaseReq(SearchBizSetReq.class);
        Page page = new Page();
        page.setEnableCount(false);
        page.setStart(start);
        page.setLimit(limit);
        req.setPage(page);
        req.setFilter(filter);
        try {
            EsbResp<SearchBizSetResp> resp = requestCmdbApi(
                ApiGwType.ESB,
                HttpMethodEnum.POST,
                SEARCH_BUSINESS_SET,
                null,
                req,
                new TypeReference<EsbResp<SearchBizSetResp>>() {
                });
            if (!resp.getResult() || resp.getData() == null) {
                throw new InternalCmdbException(ErrorCode.CMDB_API_DATA_ERROR, null);
            }
            return resp.getData().getBizSetList();
        } catch (Exception e) {
            throw new InternalCmdbException(e, ErrorCode.CMDB_API_DATA_ERROR, null);
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
            EsbResp<SearchBizInBusinessSetResp> resp = requestCmdbApi(
                ApiGwType.ESB,
                HttpMethodEnum.POST,
                SEARCH_BIZ_IN_BUSINESS_SET,
                null,
                req,
                new TypeReference<EsbResp<SearchBizInBusinessSetResp>>() {
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
            EsbResp<SearchBizInBusinessSetResp> resp = requestCmdbApi(
                ApiGwType.ESB,
                HttpMethodEnum.POST,
                SEARCH_BIZ_IN_BUSINESS_SET,
                null,
                req,
                new TypeReference<EsbResp<SearchBizInBusinessSetResp>>() {
                });
            if (!resp.getResult() || resp.getData() == null) {
                throw new InternalCmdbException(ErrorCode.CMDB_API_DATA_ERROR, null);
            }
            return resp.getData().getBizList();
        } catch (Exception e) {
            throw new InternalCmdbException(e, ErrorCode.CMDB_API_DATA_ERROR, null);
        }
    }

    @Override
    public List<BizSetInfo> listAllBizSets() {
        List<BizSetInfo> bizSetInfoList = searchAllBizSet();
        bizSetInfoList.forEach(bizSetInfo -> {
            // 查询业务集下包含的子业务(全业务除外)
            BizSetScope scope = bizSetInfo.getScope();
            if (scope != null && !scope.isMatchAll()) {
                List<BizInfo> bizList = searchBizInBizSet(bizSetInfo.getId());
                bizSetInfo.setBizList(bizList);
            }
        });
        return bizSetInfoList;
    }

    @Override
    public ResourceWatchResult<BizSetEventDetail> getBizSetEvents(Long startTime, String cursor) {
        ResourceWatchReq req = makeCmdbBaseReq(ResourceWatchReq.class);
        req.setFields(Arrays.asList(
            "bk_biz_set_id", "bk_biz_set_name", "bk_biz_maintainer",
            "bk_supplier_account", "time_zone", "language"));
        req.setResource("biz_set");
        req.setCursor(cursor);
        req.setStartTime(startTime);
        try {
            EsbResp<ResourceWatchResult<BizSetEventDetail>> resp = requestCmdbApi(
                ApiGwType.ESB,
                HttpMethodEnum.POST,
                RESOURCE_WATCH,
                null,
                req,
                new TypeReference<EsbResp<ResourceWatchResult<BizSetEventDetail>>>() {
                },
                HttpHelperFactory.getLongRetryableHttpHelper());
            if (!resp.getResult()) {
                throw new InternalCmdbException(ErrorCode.CMDB_API_DATA_ERROR, null);
            }
            return resp.getData();
        } catch (Exception e) {
            throw new InternalCmdbException(e, ErrorCode.CMDB_API_DATA_ERROR, null);
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
            EsbResp<ResourceWatchResult<BizSetRelationEventDetail>> resp = requestCmdbApi(
                ApiGwType.ESB,
                HttpMethodEnum.POST,
                RESOURCE_WATCH,
                null,
                req,
                new TypeReference<EsbResp<ResourceWatchResult<BizSetRelationEventDetail>>>() {
                },
                HttpHelperFactory.getLongRetryableHttpHelper());
            if (!resp.getResult()) {
                throw new InternalCmdbException(ErrorCode.CMDB_API_DATA_ERROR, null);
            }
            return resp.getData();
        } catch (Exception e) {
            throw new InternalCmdbException(e, ErrorCode.CMDB_API_DATA_ERROR, null);
        }
    }

    @Override
    public Set<String> listUsersByRole(Long bizSetId, String role) {
        if (!"bk_biz_maintainer".equals(role)) {
            log.warn("Unavailable role for biz set! role: {}", role);
            return Collections.emptySet();
        }

        BizSetInfo bizSet = queryBizSet(bizSetId);
        if (bizSet == null) {
            log.warn("BizSet: {} is not exist", bizSetId);
            return Collections.emptySet();
        }

        Set<String> userSet = new HashSet<>();
        String maintainers = bizSet.getMaintainer();
        if (StringUtils.isNotEmpty(maintainers)) {
            for (String user : maintainers.split(",")) {
                if (StringUtils.isNotBlank(user)) {
                    userSet.add(user);
                }
            }
        }

        return userSet;
    }

    @Override
    public BizSetInfo queryBizSet(Long bizSetId) {
        BizSetFilter filter = new BizSetFilter();
        filter.setCondition(RuleConditionEnum.AND.getCondition());
        Rule bizSetIdRule = new Rule();
        bizSetIdRule.setField("bk_biz_set_id");
        bizSetIdRule.setOperator("equal");
        bizSetIdRule.setValue(bizSetId);
        filter.setRules(Collections.singletonList(bizSetIdRule));
        List<BizSetInfo> results = searchBizSet(filter, 0, 1);
        return CollectionUtils.isEmpty(results) ? null : results.get(0);
    }
}
