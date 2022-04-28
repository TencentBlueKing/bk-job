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

package com.tencent.bk.job.upgrader.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.cc.model.bizset.BatchUpdateBizSetReq;
import com.tencent.bk.job.common.cc.model.bizset.BizSetFilter;
import com.tencent.bk.job.common.cc.model.bizset.BizSetInfo;
import com.tencent.bk.job.common.cc.model.bizset.CreateBizSetReq;
import com.tencent.bk.job.common.cc.model.bizset.Page;
import com.tencent.bk.job.common.cc.model.bizset.Rule;
import com.tencent.bk.job.common.cc.model.bizset.SearchBizSetReq;
import com.tencent.bk.job.common.cc.model.bizset.SearchBizSetResp;
import com.tencent.bk.job.common.esb.model.EsbReq;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.sdk.AbstractEsbSdkClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpPost;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class EsbCmdbClient extends AbstractEsbSdkClient {

    private final String cmdbSupplierAccount;

    private static final String FIELD_KEY_BIZ_SET_ID = "bk_biz_set_id";

    private static final String SEARCH_BUSINESS_SET = "/api/c/compapi/v2/cc/list_business_set/";
    private static final String CREATE_BUSINESS_SET = "/api/c/compapi/v2/cc/create_business_set/";
    private static final String BATCH_UPDATE_BUSINESS_SET = "/api/c/compapi/v2/cc/batch_update_business_set/";

    public EsbCmdbClient(String esbHostUrl,
                         String appCode,
                         String appSecret,
                         String lang,
                         String cmdbSupplierAccount) {
        super(esbHostUrl, appCode, appSecret, lang, false);
        this.cmdbSupplierAccount = cmdbSupplierAccount;
    }

    public <T extends EsbReq> T makeCmdbBaseReq(Class<T> reqClass) {
        return makeBaseReqByWeb(reqClass, null, "admin", cmdbSupplierAccount);
    }

    /**
     * 根据Id查询业务集信息
     *
     * @param id 业务集id
     * @return 业务集信息
     */
    public List<BizSetInfo> searchBizSetById(Long id) {
        SearchBizSetReq req = makeCmdbBaseReq(SearchBizSetReq.class);
        req.setPage(new Page());
        BizSetFilter filter = new BizSetFilter();
        filter.setCondition(BizSetFilter.CONDITION_AND);
        List<Rule> rules = new ArrayList<>();
        rules.add(new Rule(FIELD_KEY_BIZ_SET_ID, Rule.OPERATOR_EQUAL, id));
        filter.setRules(rules);
        req.setFilter(filter);
        EsbResp<SearchBizSetResp> resp = getEsbRespByReq(
            HttpPost.METHOD_NAME,
            SEARCH_BUSINESS_SET,
            req,
            new TypeReference<EsbResp<SearchBizSetResp>>() {
            });
        if (!resp.getResult() || resp.getData() == null) {
            return null;
        }
        return resp.getData().getBizSetList();
    }

    /**
     * 创建业务集
     *
     * @param createBizSetReq 业务集信息
     * @return 业务集id
     */
    public Long createBizSet(CreateBizSetReq createBizSetReq) {
        EsbResp<Long> resp = getEsbRespByReq(
            HttpPost.METHOD_NAME,
            CREATE_BUSINESS_SET,
            createBizSetReq,
            new TypeReference<EsbResp<Long>>() {
            });
        return resp.getData();
    }

    /**
     * 批量更新业务集
     *
     * @param batchUpdateBizSetReq 业务集更新请求信息
     * @return 业务集id
     */
    public Boolean batchUpdateBizSet(BatchUpdateBizSetReq batchUpdateBizSetReq) {
        EsbResp<Object> resp = getEsbRespByReq(
            HttpPost.METHOD_NAME,
            BATCH_UPDATE_BUSINESS_SET,
            batchUpdateBizSetReq,
            new TypeReference<EsbResp<Object>>() {
            });
        return resp.getResult();
    }

}
