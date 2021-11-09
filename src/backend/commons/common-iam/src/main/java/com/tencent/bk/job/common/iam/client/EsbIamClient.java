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
import com.tencent.bk.job.common.constant.ErrorCode;
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
import com.tencent.bk.job.common.iam.dto.GetApplyUrlResponse;
import com.tencent.bk.job.common.iam.dto.RegisterResourceRequest;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.sdk.iam.constants.SystemId;
import com.tencent.bk.sdk.iam.dto.action.ActionDTO;
import com.tencent.bk.sdk.iam.dto.resource.ResourceDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
        String respStr = null;
        try {
            respStr = doHttpPost(API_GET_APPLY_URL, getApplyUrlRequest);
        } catch (Exception e) {
            log.error("Error while requesting esb api {}|{}", API_GET_APPLY_URL, getApplyUrlRequest, e);
            return null;
        }
        if (StringUtils.isBlank(respStr)) {
            log.error("{}|{}|response empty", ErrorCode.PAAS_API_DATA_ERROR, API_GET_APPLY_URL);
            return null;
        }
        EsbResp<GetApplyUrlResponse> esbResp =
            JsonUtils.fromJson(respStr, new TypeReference<EsbResp<GetApplyUrlResponse>>() {
            });
        if (esbResp == null) {
            log.error("{}|{}|response empty", ErrorCode.PAAS_API_DATA_ERROR, API_GET_APPLY_URL);
            return null;
        } else if (esbResp.getCode() != 0) {
            log.error("{}|{}|code={}|msg={}", ErrorCode.PAAS_API_DATA_ERROR, API_GET_APPLY_URL,
                esbResp.getCode(), esbResp.getMessage());
            return null;
        }
        if (esbResp.getData() != null) {
            return esbResp.getData().getUrl();
        } else {
            log.error("Empty response|{}|{}|code={}|msg={}", ErrorCode.PAAS_API_DATA_ERROR, API_GET_APPLY_URL,
                esbResp.getCode(), esbResp.getMessage());
            return null;
        }
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

        String respStr = null;
        try {
            respStr = doHttpPost(API_REGISTER_RESOURCE_URL, registerResourceRequest);
        } catch (Exception e) {
            log.error("Error while requesting esb api {}|{}", API_REGISTER_RESOURCE_URL, registerResourceRequest, e);
            return false;
        }
        if (StringUtils.isBlank(respStr)) {
            log.error("{}|{}|response empty", ErrorCode.PAAS_API_DATA_ERROR, API_REGISTER_RESOURCE_URL);
            return false;
        }
        EsbResp esbResp = JsonUtils.fromJson(respStr, new TypeReference<EsbResp>() {
        });
        if (esbResp == null) {
            log.error("{}|{}|response empty", ErrorCode.PAAS_API_DATA_ERROR, API_REGISTER_RESOURCE_URL);
            return false;
        } else if (esbResp.getCode() != 0) {
            log.error("{}|{}|code={}|msg={}", ErrorCode.PAAS_API_DATA_ERROR, API_REGISTER_RESOURCE_URL,
                esbResp.getCode(), esbResp.getMessage());
            return false;
        }
        return true;
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

        EsbResp<EsbIamAuthedPolicy> esbResp = getEsbRespByReq(HttpPost.METHOD_NAME,
            API_AUTH_BY_PATH_URL, authByPathReq, new TypeReference<EsbResp<EsbIamAuthedPolicy>>() {
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
        EsbResp<List<EsbIamBatchAuthedPolicy>> esbResp = getEsbRespByReq(
            HttpPost.METHOD_NAME,
            API_BATCH_AUTH_BY_PATH_URL,
            batchAuthByPathReq,
            new TypeReference<EsbResp<List<EsbIamBatchAuthedPolicy>>>() {
            });
        return esbResp.getData();
    }

    @Override
    protected <T extends EsbReq> String buildPostBody(T params) {
        return JsonUtils.toNonEmptyJson(params);
    }
}
