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

import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.iam.service.BaseIamCallbackService;
import com.tencent.bk.job.common.iam.util.IamRespUtil;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.manage.api.iam.IamAccountCallbackResource;
import com.tencent.bk.job.manage.model.dto.AccountDTO;
import com.tencent.bk.job.manage.model.dto.AccountDisplayDTO;
import com.tencent.bk.job.manage.service.AccountService;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO;
import com.tencent.bk.sdk.iam.dto.callback.request.IamSearchCondition;
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.SearchInstanceResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@Slf4j
public class IamAccountCallbackResourceImpl extends BaseIamCallbackService implements IamAccountCallbackResource {
    private final AccountService accountService;
    private final ApplicationService applicationService;

    @Autowired
    public IamAccountCallbackResourceImpl(AccountService accountService,
                                          ApplicationService applicationService) {
        this.accountService = accountService;
        this.applicationService = applicationService;
    }

    private Pair<AccountDTO, BaseSearchCondition> getBasicQueryCondition(CallbackRequestDTO callbackRequest) {
        IamSearchCondition searchCondition = IamSearchCondition.fromReq(callbackRequest);
        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(searchCondition.getStart().intValue());
        baseSearchCondition.setLength(searchCondition.getLength().intValue());

        AccountDTO accountQuery = new AccountDTO();
        Long appId = applicationService.getAppIdByScope(extractResourceScopeCondition(searchCondition));
        accountQuery.setAppId(appId);
        return Pair.of(accountQuery, baseSearchCondition);
    }

    private InstanceInfoDTO convert(AccountDTO accountDTO) {
        InstanceInfoDTO instanceInfo = new InstanceInfoDTO();
        instanceInfo.setId(String.valueOf(accountDTO.getId()));
        instanceInfo.setDisplayName(accountDTO.getAlias());
        return instanceInfo;
    }

    @Override
    protected ListInstanceResponseDTO listInstanceResp(CallbackRequestDTO callbackRequest) {
        Pair<AccountDTO, BaseSearchCondition> basicQueryCond = getBasicQueryCondition(callbackRequest);

        AccountDTO accountQuery = basicQueryCond.getLeft();
        BaseSearchCondition baseSearchCondition = basicQueryCond.getRight();
        PageData<AccountDTO> accountDTOPageData = accountService.listPageAccount(accountQuery,
            baseSearchCondition);

        return IamRespUtil.getListInstanceRespFromPageData(accountDTOPageData, this::convert);
    }

    @Override
    protected SearchInstanceResponseDTO searchInstanceResp(CallbackRequestDTO callbackRequest) {

        Pair<AccountDTO, BaseSearchCondition> basicQueryCond = getBasicQueryCondition(callbackRequest);
        AccountDTO accountQuery = basicQueryCond.getLeft();
        BaseSearchCondition baseSearchCondition = basicQueryCond.getRight();

        accountQuery.setAlias(callbackRequest.getFilter().getKeyword());
        PageData<AccountDTO> accountDTOPageData = accountService.listPageAccount(accountQuery,
            baseSearchCondition);

        return IamRespUtil.getSearchInstanceRespFromPageData(accountDTOPageData, this::convert);
    }

    private InstanceInfoDTO buildInstance(String instanceId,
                                          AccountDisplayDTO accountDTO,
                                          Map<Long, ResourceScope> appIdScopeMap) {
        Long appId = accountDTO.getAppId();
        PathInfoDTO rootNode = getPathNodeByAppId(appId, appIdScopeMap);
        // 拓扑路径构建
        List<PathInfoDTO> path = new ArrayList<>();
        PathInfoDTO accountNode = new PathInfoDTO();
        accountNode.setType(ResourceTypeId.ACCOUNT);
        accountNode.setId(accountDTO.getId().toString());
        rootNode.setChild(accountNode);
        path.add(rootNode);
        // 实例组装
        InstanceInfoDTO instanceInfo = new InstanceInfoDTO();
        instanceInfo.setId(instanceId);
        instanceInfo.setDisplayName(accountDTO.getAlias());
        instanceInfo.setPath(path);
        return instanceInfo;
    }

    @Override
    protected CallbackBaseResponseDTO fetchInstanceResp(
        CallbackRequestDTO callbackRequest
    ) {
        IamSearchCondition searchCondition = IamSearchCondition.fromReq(callbackRequest);
        List<Object> instanceAttributeInfoList = new ArrayList<>();
        List<Long> accountIdList = new ArrayList<>();
        for (String instanceId : searchCondition.getIdList()) {
            try {
                long id = Long.parseLong(instanceId);
                accountIdList.add(id);
            } catch (NumberFormatException e) {
                String msg = MessageFormatter.format(
                    "Parse account id failed!|{}",
                    instanceId
                ).getMessage();
                log.error(msg, e);
            }
        }
        Map<Long, AccountDisplayDTO> accountInfoMap = accountService.getAccountDisplayInfoMapByIds(accountIdList);
        // Job app --> CMDB biz/businessSet转换
        Set<Long> appIdSet = new HashSet<>();
        accountInfoMap.values().forEach(accountDisplayDTO -> appIdSet.add(accountDisplayDTO.getAppId()));
        Map<Long, ResourceScope> appIdScopeMap = applicationService.getScopeByAppIds(appIdSet);
        for (String instanceId : searchCondition.getIdList()) {
            long id = Long.parseLong(instanceId);
            AccountDisplayDTO accountDTO = accountInfoMap.get(id);
            if (accountDTO == null) {
                logNotExistId(id);
                continue;
            }
            try {
                InstanceInfoDTO instanceInfo = buildInstance(instanceId, accountDTO, appIdScopeMap);
                instanceAttributeInfoList.add(instanceInfo);
            } catch (Exception e) {
                logBuildInstanceFailure(accountDTO, e);
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
