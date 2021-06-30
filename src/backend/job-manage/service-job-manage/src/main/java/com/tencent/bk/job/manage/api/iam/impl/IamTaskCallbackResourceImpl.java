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

import com.tencent.bk.job.common.iam.constant.ResourceId;
import com.tencent.bk.job.common.iam.controller.AbstractIamController;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.manage.api.iam.IamTaskCallbackResource;
import com.tencent.bk.job.manage.model.dto.TaskPlanQueryDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO;
import com.tencent.bk.sdk.iam.dto.callback.request.IamSearchCondition;
import com.tencent.bk.sdk.iam.dto.callback.response.*;
import com.tencent.bk.sdk.iam.helper.AuthHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 15/6/2020 15:41
 */
@Slf4j
@RestController
public class IamTaskCallbackResourceImpl extends AbstractIamController implements IamTaskCallbackResource {

    private final TaskTemplateService templateService;
    private final TaskPlanService planService;

    @Autowired
    public IamTaskCallbackResourceImpl(TaskTemplateService templateService, TaskPlanService planService,
                                       AuthHelper authHelper) {
        super(authHelper);
        this.templateService = templateService;
        this.planService = planService;
    }

    @Override
    public CallbackBaseResponseDTO templateCallback(CallbackRequestDTO callbackRequest) {
        log.debug("Receive iam callback|{}", callbackRequest);
        CallbackBaseResponseDTO response;
        IamSearchCondition searchCondition = IamSearchCondition.fromReq(callbackRequest);
        switch (callbackRequest.getMethod()) {
            case LIST_INSTANCE:
                log.debug("List instance request!|{}|{}|{}", callbackRequest.getType(), callbackRequest.getFilter(),
                    callbackRequest.getPage());
                TaskTemplateInfoDTO condition = new TaskTemplateInfoDTO();
                condition.setAppId(searchCondition.getAppIdList().get(0));
                BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
                baseSearchCondition.setStart(searchCondition.getStart().intValue());
                baseSearchCondition.setLength(searchCondition.getLength().intValue());

                PageData<TaskTemplateInfoDTO> templateInfoPageData =
                    templateService.listPageTaskTemplatesBasicInfo(condition, baseSearchCondition, null);

                List<InstanceInfoDTO> instanceInfoList =
                    templateInfoPageData.getData().parallelStream().map(templateInfo -> {
                        InstanceInfoDTO instanceInfo = new InstanceInfoDTO();
                        instanceInfo.setId(String.valueOf(templateInfo.getId()));
                        instanceInfo.setDisplayName(templateInfo.getName());
                        return instanceInfo;
                    }).collect(Collectors.toList());
                ListInstanceResponseDTO instanceResponse = new ListInstanceResponseDTO();
                instanceResponse.setCode(0L);
                BaseDataResponseDTO<InstanceInfoDTO> listInstanceResponseData = new BaseDataResponseDTO<>();
                listInstanceResponseData.setResult(instanceInfoList);
                listInstanceResponseData.setCount(templateInfoPageData.getTotal());
                instanceResponse.setData(listInstanceResponseData);
                response = instanceResponse;
                break;
            case FETCH_INSTANCE_INFO:
                log.debug("Fetch instance info request!|{}|{}|{}", callbackRequest.getType(),
                    callbackRequest.getFilter(), callbackRequest.getPage());

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

    @Override
    public CallbackBaseResponseDTO planCallback(CallbackRequestDTO callbackRequest) {
        log.debug("Receive iam callback|{}", callbackRequest);
        CallbackBaseResponseDTO response;
        IamSearchCondition searchCondition = IamSearchCondition.fromReq(callbackRequest);
        switch (callbackRequest.getMethod()) {
            case LIST_INSTANCE:
                log.debug("List instance request!|{}|{}|{}", callbackRequest.getType(), callbackRequest.getFilter(),
                    callbackRequest.getPage());
                TaskPlanQueryDTO condition = new TaskPlanQueryDTO();
                condition.setTemplateId(Long.valueOf(searchCondition.getAttribute().get(ResourceId.TEMPLATE).get(0)));
                BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
                baseSearchCondition.setStart(searchCondition.getStart().intValue());
                baseSearchCondition.setLength(searchCondition.getLength().intValue());

                PageData<TaskPlanInfoDTO> planInfoPageData =
                    planService.listPageTaskPlansBasicInfo(condition, baseSearchCondition, null);

                List<InstanceInfoDTO> instanceInfoList = planInfoPageData.getData().parallelStream().map(planInfo -> {
                    InstanceInfoDTO instanceInfo = new InstanceInfoDTO();
                    instanceInfo.setId(String.valueOf(planInfo.getId()));
                    instanceInfo.setDisplayName(planInfo.getName());
                    return instanceInfo;
                }).collect(Collectors.toList());
                ListInstanceResponseDTO instanceResponse = new ListInstanceResponseDTO();
                instanceResponse.setCode(0L);
                BaseDataResponseDTO<InstanceInfoDTO> listInstanceResponseData = new BaseDataResponseDTO<>();
                listInstanceResponseData.setResult(instanceInfoList);
                listInstanceResponseData.setCount(planInfoPageData.getTotal());
                instanceResponse.setData(listInstanceResponseData);
                response = instanceResponse;
                break;
            case FETCH_INSTANCE_INFO:
                log.debug("Fetch instance info request!|{}|{}|{}", callbackRequest.getType(),
                    callbackRequest.getFilter(), callbackRequest.getPage());

                List<Object> instanceAttributeInfoList = new ArrayList<>();
                for (String instanceId : searchCondition.getIdList()) {
                    try {
                        long id = Long.parseLong(instanceId);
                        InstanceInfoDTO instanceInfo = new InstanceInfoDTO();
                        instanceInfo.setId(instanceId);
                        instanceInfo.setDisplayName(planService.getPlanName(id));
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
