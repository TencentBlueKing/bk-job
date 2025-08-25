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

package com.tencent.bk.job.manage.api.iam.impl;

import com.tencent.bk.audit.utils.json.JsonSchemaUtils;
import com.tencent.bk.job.common.iam.util.IamRespUtil;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.tenant.TenantService;
import com.tencent.bk.job.manage.model.dto.ScriptBasicDTO;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbScriptV3DTO;
import com.tencent.bk.job.manage.model.query.ScriptQuery;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.PublicScriptService;
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO;
import com.tencent.bk.sdk.iam.dto.callback.request.IamSearchCondition;
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.FetchResourceTypeSchemaResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.SearchInstanceResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class PublicScriptCallbackHelper extends AbstractScriptCallbackHelper {
    private final PublicScriptService publicScriptService;

    @Autowired
    public PublicScriptCallbackHelper(PublicScriptService publicScriptService,
                                      ApplicationService applicationService,
                                      AppScopeMappingService appScopeMappingService,
                                      TenantService tenantService) {
        super(applicationService, appScopeMappingService, tenantService);
        this.publicScriptService = publicScriptService;
    }

    @Override
    protected ListInstanceResponseDTO listInstanceResp(CallbackRequestDTO callbackRequest) {
        ScriptQuery scriptQuery = buildBasicScriptQuery(callbackRequest);
        PageData<ScriptDTO> scriptDTOPageData = publicScriptService.listPageScript(scriptQuery);

        return IamRespUtil.getListInstanceRespFromPageData(scriptDTOPageData, this::convert);
    }


    @Override
    protected SearchInstanceResponseDTO searchInstanceResp(CallbackRequestDTO callbackRequest) {

        ScriptQuery scriptQuery = buildBasicScriptQuery(callbackRequest);
        scriptQuery.setName(callbackRequest.getFilter().getKeyword());
        PageData<ScriptDTO> accountDTOPageData = publicScriptService.listPageScript(scriptQuery);

        return IamRespUtil.getSearchInstanceRespFromPageData(accountDTOPageData, this::convert);
    }

    @Override
    protected CallbackBaseResponseDTO fetchInstanceResp(CallbackRequestDTO callbackRequest) {
        IamSearchCondition searchCondition = IamSearchCondition.fromReq(callbackRequest);
        List<String> scriptIdList = searchCondition.getIdList();
        List<ScriptBasicDTO> scriptBasicDTOList = publicScriptService.listScriptBasicInfoByScriptIds(scriptIdList);
        return buildFetchInstanceResp(scriptIdList, scriptBasicDTOList);
    }

    @Override
    boolean isPublicScript() {
        return true;
    }

    public CallbackBaseResponseDTO doCallback(String tenantId, CallbackRequestDTO callbackRequest) {
        return baseCallback(tenantId, callbackRequest);
    }

    @Override
    protected FetchResourceTypeSchemaResponseDTO fetchResourceTypeSchemaResp(
        CallbackRequestDTO callbackRequest) {
        FetchResourceTypeSchemaResponseDTO resp = new FetchResourceTypeSchemaResponseDTO();
        resp.setData(JsonSchemaUtils.generateJsonSchema(EsbScriptV3DTO.class));
        return resp;
    }
}
