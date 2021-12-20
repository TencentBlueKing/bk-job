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

package com.tencent.bk.job.common.iam.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.esb.model.EsbReq;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.sdk.AbstractEsbSdkClient;
import com.tencent.bk.job.common.iam.dto.AuthByPathReq;
import com.tencent.bk.job.common.iam.dto.BatchAuthByPathReq;
import com.tencent.bk.job.common.iam.dto.EsbIamAction;
import com.tencent.bk.job.common.iam.dto.EsbIamAuthedPolicy;
import com.tencent.bk.job.common.iam.dto.EsbIamBatchAuthedPolicy;
import com.tencent.bk.job.common.iam.dto.EsbIamBatchPathResource;
import com.tencent.bk.job.common.iam.dto.EsbIamResource;
import com.tencent.bk.job.common.iam.dto.EsbIamSubject;
import com.tencent.bk.job.common.iam.dto.GetApplyUrlRequest;
import com.tencent.bk.job.common.iam.dto.RegisterResourceRequest;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.sdk.iam.constants.SystemId;
import com.tencent.bk.sdk.iam.dto.action.ActionDTO;
import com.tencent.bk.sdk.iam.dto.resource.ResourceDTO;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpPost;

import java.util.List;

/**
 * @since 16/6/2020 21:37
 */
@Slf4j
public class EsbIamClient extends AbstractEsbSdkClient implements IIamClient {

    private static final String API_GET_APPLY_URL = "/api/c/compapi/v2/iam/application/";
    private static final String API_REGISTER_RESOURCE_URL =
        "/api/c/compapi/v2/iam/authorization/resource_creator_action/";
    private static final String API_AUTH_BY_PATH_URL =
        "/api/c/compapi/v2/iam/authorization/path/";
    private static final String API_BATCH_AUTH_BY_PATH_URL =
        "/api/c/compapi/v2/iam/authorization/batch_path/";

    public EsbIamClient(String esbHostUrl, String appCode, String appSecret, boolean useEsbTestEnv) {
        super(esbHostUrl, appCode, appSecret, null, useEsbTestEnv);
    }

    public EsbIamClient(String esbHostUrl, String appCode, String appSecret, String lang, boolean useEsbTestEnv) {
        super(esbHostUrl, appCode, appSecret, lang, useEsbTestEnv);
    }

    @Override
    public String getApplyUrl(List<ActionDTO> actionList) {
        GetApplyUrlRequest getApplyUrlRequest = makeBaseReqByWeb(
            GetApplyUrlRequest.class,
            null,
            "admin",
            "superadmin"
        );
        getApplyUrlRequest.setSystem(SystemId.JOB);
        getApplyUrlRequest.setAction(actionList);
        EsbResp<String> esbResp = requestIamApi(
            HttpPost.METHOD_NAME,
            API_GET_APPLY_URL,
            getApplyUrlRequest,
            new TypeReference<EsbResp<String>>() {
            });
        return esbResp.getData();
    }

    @Override
    public boolean registerResource(String id, String name, String type, String creator, List<ResourceDTO> ancestor) {
        RegisterResourceRequest registerResourceRequest =
            makeBaseReqByWeb(RegisterResourceRequest.class, null, creator, "0");
        registerResourceRequest.setSystem(SystemId.JOB);
        registerResourceRequest.setId(id);
        registerResourceRequest.setName(name);
        registerResourceRequest.setType(type);
        registerResourceRequest.setCreator(creator);
        if (ancestor != null && ancestor.size() > 0) {
            registerResourceRequest.setAncestor(ancestor);
        }
        EsbResp<Boolean> esbResp = requestIamApi(
            HttpPost.METHOD_NAME,
            API_REGISTER_RESOURCE_URL,
            registerResourceRequest,
            new TypeReference<EsbResp<Boolean>>() {
            });
        return esbResp.getData();
    }

    @Override
    public EsbIamAuthedPolicy authByPath(
        EsbIamAction esbIamAction,
        EsbIamSubject esbIamSubject,
        List<EsbIamResource> esbIamResources
    ) {
        AuthByPathReq authByPathReq =
            makeBaseReqByWeb(AuthByPathReq.class, null, "admin", "superadmin");
        authByPathReq.setAction(esbIamAction);
        authByPathReq.setSubject(esbIamSubject);
        authByPathReq.setResources(esbIamResources);

        EsbResp<EsbIamAuthedPolicy> esbResp = requestIamApi(
            HttpPost.METHOD_NAME,
            API_AUTH_BY_PATH_URL,
            authByPathReq,
            new TypeReference<EsbResp<EsbIamAuthedPolicy>>() {
            });
        return esbResp.getData();
    }

    @Override
    public List<EsbIamBatchAuthedPolicy> batchAuthByPath(
        List<EsbIamAction> esbIamActions,
        EsbIamSubject esbIamSubject,
        List<EsbIamBatchPathResource> esbIamBatchPathResources,
        Long expiredAt
    ) {
        BatchAuthByPathReq batchAuthByPathReq =
            makeBaseReqByWeb(BatchAuthByPathReq.class, null, "admin", "superadmin");
        batchAuthByPathReq.setActions(esbIamActions);
        batchAuthByPathReq.setSubject(esbIamSubject);
        batchAuthByPathReq.setResources(esbIamBatchPathResources);
        batchAuthByPathReq.setExpiredAt(expiredAt);
        EsbResp<List<EsbIamBatchAuthedPolicy>> esbResp = requestIamApi(
            HttpPost.METHOD_NAME,
            API_BATCH_AUTH_BY_PATH_URL,
            batchAuthByPathReq,
            new TypeReference<EsbResp<List<EsbIamBatchAuthedPolicy>>>() {
            });
        return esbResp.getData();
    }

    /**
     * 通过ESB请求权限中心API的统一入口，监控数据埋点位置
     *
     * @param method        Http方法
     * @param uri           请求地址
     * @param reqBody       请求体内容
     * @param typeReference 指定了返回值类型的EsbResp TypeReference对象
     * @param <R>           泛型：返回值类型
     * @return 返回值类型实例
     */
    private <R> EsbResp<R> requestIamApi(String method,
                                         String uri,
                                         EsbReq reqBody,
                                         TypeReference<EsbResp<R>> typeReference) {
        try {
            JobContextUtil.setHttpMetricName(CommonMetricNames.ESB_IAM_API_HTTP);
            JobContextUtil.addHttpMetricTag(Tag.of("api_name", uri));
            return getEsbRespByReq(method, uri, reqBody, typeReference);
        } finally {
            JobContextUtil.clearHttpMetricTags();
        }
    }

    @Override
    protected <T extends EsbReq> String buildPostBody(T params) {
        return JsonUtils.toNonEmptyJson(params);
    }
}
