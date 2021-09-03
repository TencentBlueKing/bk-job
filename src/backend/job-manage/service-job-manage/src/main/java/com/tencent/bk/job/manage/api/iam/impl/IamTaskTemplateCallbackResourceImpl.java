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

import com.tencent.bk.job.common.iam.util.IamRespUtil;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.manage.api.iam.IamTaskTemplateCallbackResource;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO;
import com.tencent.bk.sdk.iam.dto.callback.request.IamSearchCondition;
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListAttributeResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListAttributeValueResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceByPolicyResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.SearchInstanceResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
public class IamTaskTemplateCallbackResourceImpl implements IamTaskTemplateCallbackResource {

    private final TaskTemplateService templateService;

    @Autowired
    public IamTaskTemplateCallbackResourceImpl(TaskTemplateService templateService) {
        this.templateService = templateService;
    }

    private InstanceInfoDTO convert(TaskTemplateInfoDTO templateInfo) {
        InstanceInfoDTO instanceInfo = new InstanceInfoDTO();
        instanceInfo.setId(String.valueOf(templateInfo.getId()));
        instanceInfo.setDisplayName(templateInfo.getName());
        return instanceInfo;
    }

    private Pair<TaskTemplateInfoDTO, BaseSearchCondition> getBasicQueryCondition(CallbackRequestDTO callbackRequest) {
        IamSearchCondition searchCondition = IamSearchCondition.fromReq(callbackRequest);
        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(searchCondition.getStart().intValue());
        baseSearchCondition.setLength(searchCondition.getLength().intValue());

        TaskTemplateInfoDTO templateQuery = new TaskTemplateInfoDTO();
        templateQuery.setAppId(searchCondition.getAppIdList().get(0));
        return Pair.of(templateQuery, baseSearchCondition);
    }

    public ListInstanceResponseDTO listInstanceResp(CallbackRequestDTO callbackRequest) {
        Pair<TaskTemplateInfoDTO, BaseSearchCondition> basicQueryCond =
            getBasicQueryCondition(callbackRequest);

        TaskTemplateInfoDTO templateQuery = basicQueryCond.getLeft();
        BaseSearchCondition baseSearchCondition = basicQueryCond.getRight();
        PageData<TaskTemplateInfoDTO> templateDTOPageData = templateService.listPageTaskTemplatesBasicInfo(templateQuery,
            baseSearchCondition, null);

        return IamRespUtil.getListInstanceRespFromPageData(templateDTOPageData, this::convert);
    }

    public SearchInstanceResponseDTO searchInstanceResp(CallbackRequestDTO callbackRequest) {
        Pair<TaskTemplateInfoDTO, BaseSearchCondition> basicQueryCond =
            getBasicQueryCondition(callbackRequest);

        TaskTemplateInfoDTO templateQuery = basicQueryCond.getLeft();
        BaseSearchCondition baseSearchCondition = basicQueryCond.getRight();

        templateQuery.setName(callbackRequest.getFilter().getKeyword());
        PageData<TaskTemplateInfoDTO> templateDTOPageData = templateService.listPageTaskTemplatesBasicInfo(templateQuery,
            baseSearchCondition, null);

        return IamRespUtil.getSearchInstanceRespFromPageData(templateDTOPageData, this::convert);
    }

    @Override
    public CallbackBaseResponseDTO callback(CallbackRequestDTO callbackRequest) {
        CallbackBaseResponseDTO response;
        IamSearchCondition searchCondition = IamSearchCondition.fromReq(callbackRequest);
        switch (callbackRequest.getMethod()) {
            case LIST_INSTANCE:
                response = listInstanceResp(callbackRequest);
                break;
            case FETCH_INSTANCE_INFO:
                List<Object> instanceAttributeInfoList = new ArrayList<>();
                for (String instanceId : searchCondition.getIdList()) {
                    try {
                        long id = Long.parseLong(instanceId);
                        InstanceInfoDTO instanceInfo = new InstanceInfoDTO();
                        instanceInfo.setId(instanceId);
                        instanceInfo.setDisplayName(templateService.getTemplateName(id));
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
                response = new ListAttributeResponseDTO();
                response.setCode(0L);
                break;
            case LIST_ATTRIBUTE_VALUE:
                response = new ListAttributeValueResponseDTO();
                response.setCode(0L);
                break;
            case LIST_INSTANCE_BY_POLICY:
                response = new ListInstanceByPolicyResponseDTO();
                response.setCode(0L);
                break;
            case SEARCH_INSTANCE:
                response = searchInstanceResp(callbackRequest);
                break;
            default:
                log.error("Unknown callback method!|{}|{}|{}|{}", callbackRequest.getMethod(),
                    callbackRequest.getType(), callbackRequest.getFilter(), callbackRequest.getPage());
                response = new CallbackBaseResponseDTO();
        }
        return response;
    }
}
