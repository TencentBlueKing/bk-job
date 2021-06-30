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

package com.tencent.bk.job.file_gateway.api.iam;

import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import com.tencent.bk.job.file_gateway.service.FileSourceService;
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
public class IamFileSourceCallbackResourceImpl implements IamFileSourceCallbackResource {

    private final FileSourceService fileSourceService;

    @Autowired
    public IamFileSourceCallbackResourceImpl(FileSourceService fileSourceService) {
        this.fileSourceService = fileSourceService;
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
                // 文件源列表实现
                List<Long> appIdList = searchCondition.getAppIdList();
                List<String> idStrList = searchCondition.getIdList();
                List<Integer> fileSourceIdList = null;
                if (idStrList != null) {
                    fileSourceIdList = idStrList.parallelStream().map(Integer::parseInt).collect(Collectors.toList());
                }
                List<FileSourceDTO> fileSourceDTOList = fileSourceService.listWorkTableFileSource(
                    appIdList,
                    fileSourceIdList,
                    searchCondition.getStart().intValue(),
                    searchCondition.getLength().intValue());
                Long totalCount = fileSourceService.countWorkTableFileSource(appIdList, fileSourceIdList).longValue();
                List<InstanceInfoDTO> instanceInfoList = fileSourceDTOList.parallelStream().map(fileSourceDTO -> {
                    InstanceInfoDTO tmpInstanceInfo = new InstanceInfoDTO();
                    tmpInstanceInfo.setId(fileSourceDTO.getId().toString());
                    tmpInstanceInfo.setDisplayName(fileSourceDTO.getAlias());
                    return tmpInstanceInfo;
                }).collect(Collectors.toList());

                ListInstanceResponseDTO instanceResponse = new ListInstanceResponseDTO();
                instanceResponse.setCode(0L);
                BaseDataResponseDTO<InstanceInfoDTO> baseDataResponse = new BaseDataResponseDTO<>();
                baseDataResponse.setResult(instanceInfoList);
                baseDataResponse.setCount(totalCount);
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
                        // 文件源详情查询实现
                        FileSourceDTO fileSourceDTO = fileSourceService.getFileSourceById(Integer.parseInt(instanceId));
                        if (fileSourceDTO == null) {
                            instanceInfo.setDisplayName("Unknown(may be deleted)");
                            log.warn("Unexpected fileSourceId:{} passed by iam", instanceId);
                        } else {
                            instanceInfo.setDisplayName(fileSourceDTO.getAlias());
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
