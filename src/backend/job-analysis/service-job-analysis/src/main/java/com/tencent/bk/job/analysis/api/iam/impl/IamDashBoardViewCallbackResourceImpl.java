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

package com.tencent.bk.job.analysis.api.iam.impl;

import com.tencent.bk.job.analysis.api.iam.IamDashBoardViewCallbackResource;
import com.tencent.bk.job.analysis.consts.AnalysisConsts;
import com.tencent.bk.job.common.iam.constant.ResourceId;
import com.tencent.bk.job.common.iam.service.BaseIamCallbackService;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO;
import com.tencent.bk.sdk.iam.dto.callback.request.IamSearchCondition;
import com.tencent.bk.sdk.iam.dto.callback.response.BaseDataResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.SearchInstanceResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
public class IamDashBoardViewCallbackResourceImpl extends BaseIamCallbackService
    implements IamDashBoardViewCallbackResource {

    @Autowired
    public IamDashBoardViewCallbackResourceImpl() {
    }

    @Override
    protected ListInstanceResponseDTO listInstanceResp(CallbackRequestDTO callbackRequest) {
        List<InstanceInfoDTO> instanceInfoList = new ArrayList<>();
        // TODO:当前只有全局视图，待后续视图功能开发后更新这里
        InstanceInfoDTO tmpInstanceInfo = new InstanceInfoDTO();
        tmpInstanceInfo.setId(AnalysisConsts.GLOBAL_DASHBOARD_VIEW_ID);
        tmpInstanceInfo.setDisplayName(AnalysisConsts.GLOBAL_DASHBOARD_VIEW_NAME);
        instanceInfoList.add(tmpInstanceInfo);

        ListInstanceResponseDTO instanceResponse = new ListInstanceResponseDTO();
        instanceResponse.setCode(0L);
        BaseDataResponseDTO<InstanceInfoDTO> baseDataResponse = new BaseDataResponseDTO<>();
        baseDataResponse.setResult(instanceInfoList);
        baseDataResponse.setCount(1L);
        instanceResponse.setData(baseDataResponse);
        return instanceResponse;
    }

    @Override
    protected SearchInstanceResponseDTO searchInstanceResp(CallbackRequestDTO callbackRequest) {
        SearchInstanceResponseDTO instanceResponse = new SearchInstanceResponseDTO();
        String keyword = callbackRequest.getFilter().getKeyword();
        if (keyword == null || AnalysisConsts.GLOBAL_DASHBOARD_VIEW_NAME.contains(keyword)) {
            List<InstanceInfoDTO> instanceInfoList = new ArrayList<>();
            InstanceInfoDTO tmpInstanceInfo = new InstanceInfoDTO();
            tmpInstanceInfo.setId(AnalysisConsts.GLOBAL_DASHBOARD_VIEW_ID);
            tmpInstanceInfo.setDisplayName(AnalysisConsts.GLOBAL_DASHBOARD_VIEW_NAME);
            instanceInfoList.add(tmpInstanceInfo);

            instanceResponse.setCode(0L);
            BaseDataResponseDTO<InstanceInfoDTO> baseDataResponse = new BaseDataResponseDTO<>();
            baseDataResponse.setResult(instanceInfoList);
            baseDataResponse.setCount(1L);
            instanceResponse.setData(baseDataResponse);
        } else {
            instanceResponse.setCode(0L);
            BaseDataResponseDTO<InstanceInfoDTO> baseDataResponse = new BaseDataResponseDTO<>();
            baseDataResponse.setResult(new ArrayList<>());
            baseDataResponse.setCount(0L);
            instanceResponse.setData(baseDataResponse);
        }
        return instanceResponse;
    }

    @Override
    protected CallbackBaseResponseDTO fetchInstanceResp(
        CallbackRequestDTO callbackRequest
    ) {
        IamSearchCondition searchCondition = IamSearchCondition.fromReq(callbackRequest);
        List<Object> instanceAttributeInfoList = new ArrayList<>();
        for (String instanceId : searchCondition.getIdList()) {
            try {
                if (!AnalysisConsts.GLOBAL_DASHBOARD_VIEW_ID.equals(instanceId)) {
                    return getNotFoundRespById(instanceId);
                }
                // 拓扑路径构建
                List<PathInfoDTO> path = new ArrayList<>();
                PathInfoDTO rootNode = new PathInfoDTO();
                rootNode.setType(ResourceId.DASHBOARD_VIEW);
                rootNode.setId(AnalysisConsts.GLOBAL_DASHBOARD_VIEW_ID);
                path.add(rootNode);
                // 实例组装
                InstanceInfoDTO instanceInfo = new InstanceInfoDTO();
                instanceInfo.setId(instanceId);
                instanceInfo.setDisplayName(AnalysisConsts.GLOBAL_DASHBOARD_VIEW_NAME);
                instanceInfo.setPath(path);
                instanceAttributeInfoList.add(instanceInfo);
            } catch (NumberFormatException e) {
                log.error("Parse object id failed!|{}", instanceId, e);
            }
        }

        FetchInstanceInfoResponseDTO fetchInstanceInfoResponse = new FetchInstanceInfoResponseDTO();
        fetchInstanceInfoResponse.setCode(0L);
        fetchInstanceInfoResponse.setData(instanceAttributeInfoList);
        return fetchInstanceInfoResponse;
    }

    @Override
    public CallbackBaseResponseDTO callback(CallbackRequestDTO callbackRequest) {
        return baseCallback(callbackRequest);
    }
}
