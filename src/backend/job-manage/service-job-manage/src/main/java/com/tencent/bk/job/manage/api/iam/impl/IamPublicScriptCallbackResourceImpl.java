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

package com.tencent.bk.job.manage.api.iam.impl;

import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.manage.api.iam.IamPublicScriptCallbackResource;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.dto.ScriptQueryDTO;
import com.tencent.bk.job.manage.service.ScriptService;
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO;
import com.tencent.bk.sdk.iam.dto.callback.request.IamSearchCondition;
import com.tencent.bk.sdk.iam.dto.callback.response.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class IamPublicScriptCallbackResourceImpl implements IamPublicScriptCallbackResource {
    private ScriptService scriptService;

    @Autowired
    public IamPublicScriptCallbackResourceImpl(ScriptService scriptService) {
        this.scriptService = scriptService;
    }

    @Override
    public CallbackBaseResponseDTO callback(CallbackRequestDTO callbackRequest) {
        log.debug("Receive iam callback|{}", callbackRequest);
        CallbackBaseResponseDTO response;
        IamSearchCondition searchCondition = IamSearchCondition.fromReq(callbackRequest);
        switch (callbackRequest.getMethod()) {
            case LIST_INSTANCE:
                log.debug("List instance request!|{}|{}|{}", callbackRequest.getType(), callbackRequest.getFilter(),
                    callbackRequest.getPage());
                ScriptQueryDTO scriptQuery = new ScriptQueryDTO();
                scriptQuery.setPublicScript(true);
                BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
                baseSearchCondition.setStart(searchCondition.getStart().intValue());
                baseSearchCondition.setLength(searchCondition.getLength().intValue());

                PageData<ScriptDTO> scriptPageData =
                    scriptService.listPageScript(scriptQuery, baseSearchCondition);

                List<InstanceInfoDTO> instanceInfoList =
                    scriptPageData.getData().parallelStream().map(script -> {
                        InstanceInfoDTO instanceInfo = new InstanceInfoDTO();
                        instanceInfo.setId(String.valueOf(script.getId()));
                        instanceInfo.setDisplayName(script.getName());
                        return instanceInfo;
                    }).collect(Collectors.toList());
                ListInstanceResponseDTO instanceResponse = new ListInstanceResponseDTO();
                instanceResponse.setCode(0L);
                BaseDataResponseDTO<InstanceInfoDTO> baseDataResponse = new BaseDataResponseDTO<>();
                baseDataResponse.setResult(instanceInfoList);
                baseDataResponse.setCount(scriptPageData.getTotal());
                instanceResponse.setData(baseDataResponse);
                response = instanceResponse;
                break;
            case FETCH_INSTANCE_INFO:
                log.debug("Fetch instance info request!|{}|{}|{}", callbackRequest.getType(),
                    callbackRequest.getFilter(), callbackRequest.getPage());

                List<Object> instanceAttributeInfoList = new ArrayList<>();
                for (String instanceId : searchCondition.getIdList()) {
                    try {
                        InstanceInfoDTO instanceInfo = new InstanceInfoDTO();
                        instanceInfo.setId(instanceId);
                        ScriptDTO scriptDTO = scriptService.getScriptByScriptId(instanceId);
                        if (scriptDTO != null) {
                            instanceInfo.setDisplayName(scriptDTO.getName());
                        } else {
                            instanceInfo.setDisplayName("Unknown(may be deleted)");
                            log.warn("Unexpected scriptId:{} passed by iam", instanceId);
                        }
                        instanceAttributeInfoList.add(instanceInfo);
                    } catch (NumberFormatException e) {
                        log.error("Parse object id failed!|{}", instanceId, e);
                    }
                }

                FetchInstanceInfoResponseDTO fetchInstanceInfoResponse = new FetchInstanceInfoResponseDTO();
                fetchInstanceInfoResponse.setCode(0L);
                fetchInstanceInfoResponse.setData(instanceAttributeInfoList);

                response = fetchInstanceInfoResponse;
                break;
            case LIST_ATTRIBUTE:
                log.debug("List attribute request!|{}|{}|{}", callbackRequest.getType(), callbackRequest.getFilter(),
                    callbackRequest.getPage());
                response = new ListAttributeResponseDTO();
                response.setCode(0L);
                break;
            case LIST_ATTRIBUTE_VALUE:
                log.debug("List attribute value request!|{}|{}|{}", callbackRequest.getType(),
                    callbackRequest.getFilter(), callbackRequest.getPage());
                response = new ListAttributeValueResponseDTO();
                response.setCode(0L);
                break;
            case LIST_INSTANCE_BY_POLICY:
                log.debug("List instance by policy request!|{}|{}|{}", callbackRequest.getType(),
                    callbackRequest.getFilter(), callbackRequest.getPage());
                response = new ListInstanceByPolicyResponseDTO();
                response.setCode(0L);
                break;
            default:
                log.error("Unknown callback method!|{}|{}|{}|{}", callbackRequest.getMethod(),
                    callbackRequest.getType(), callbackRequest.getFilter(), callbackRequest.getPage());
                response = new CallbackBaseResponseDTO();
        }
        return response;
    }
}
