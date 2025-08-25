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

package com.tencent.bk.job.common.iam.service;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.util.IamUtil;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.tenant.TenantService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.sdk.iam.constants.CommonResponseCode;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO;
import com.tencent.bk.sdk.iam.dto.callback.request.IamSearchCondition;
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.FetchResourceTypeSchemaResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListAttributeResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListAttributeValueResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceByPolicyResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.SearchInstanceResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.lang.NonNull;

import java.util.Map;

@Slf4j
public abstract class BaseIamCallbackService {

    protected final AppScopeMappingService appScopeMappingService;
    protected final TenantService tenantService;

    public BaseIamCallbackService(AppScopeMappingService appScopeMappingService, TenantService tenantService) {
        this.appScopeMappingService = appScopeMappingService;
        this.tenantService = tenantService;
    }

    @SuppressWarnings("SameParameterValue")
    private CallbackBaseResponseDTO getFailResp(Long code, String message) {
        CallbackBaseResponseDTO respDTO = new CallbackBaseResponseDTO();
        respDTO.setCode(code);
        respDTO.setMessage(message);
        return respDTO;
    }

    private CallbackBaseResponseDTO getNotFoundResp(String message) {
        return getFailResp(CommonResponseCode.NOT_FOUND, message);
    }

    private String genNotExistMessage(String id) {
        return String.format("cannot find resource by id %s, may be deleted", id);
    }

    protected void logNotExistId(@NonNull Object id) {
        log.warn(genNotExistMessage(id.toString()));
    }

    protected void logBuildInstanceFailure(Object rawInstance, Exception e) {
        FormattingTuple msg = MessageFormatter.format("Fail to buildInstance for {}", rawInstance);
        log.warn(msg.getMessage(), e);
    }

    public CallbackBaseResponseDTO getNotFoundRespById(String id) {
        logNotExistId(id);
        return getNotFoundResp(genNotExistMessage(id));
    }

    /**
     * 根据Job内部业务Id获取映射为CMDB scope，生成对应的权限中心path节点
     *
     * @param appId         Job内部业务ID
     * @param appIdScopeMap 内部业务Id与Scope映射表
     * @return Path节点
     */
    public PathInfoDTO getPathNodeByAppId(Long appId, Map<Long, ResourceScope> appIdScopeMap) {
        ResourceScope resourceScope = appIdScopeMap.get(appId);
        if (resourceScope == null) {
            FormattingTuple msg = MessageFormatter.format("Cannot find scope by appId {}", appId);
            log.warn(msg.getMessage());
            throw new InternalException(msg.getMessage(), ErrorCode.INTERNAL_ERROR);
        }
        PathInfoDTO rootNode = new PathInfoDTO();
        rootNode.setType(IamUtil.getIamResourceTypeIdForResourceScope(resourceScope));
        rootNode.setId(resourceScope.getId());
        return rootNode;
    }

    protected abstract ListInstanceResponseDTO listInstanceResp(CallbackRequestDTO callbackRequest);

    protected abstract SearchInstanceResponseDTO searchInstanceResp(CallbackRequestDTO callbackRequest);

    protected abstract CallbackBaseResponseDTO fetchInstanceResp(CallbackRequestDTO callbackRequest);

    protected abstract FetchResourceTypeSchemaResponseDTO fetchResourceTypeSchemaResp(
        CallbackRequestDTO callbackRequest);

    public CallbackBaseResponseDTO baseCallback(String tenantId, CallbackRequestDTO callbackRequest) {
        if (log.isDebugEnabled()) {
            // tenantId通过IamCallbackAspect切面提取，并放入JobContext中，此处打印用于对比是否有逻辑遗漏
            log.debug("tenantId={}, tenantIdInJobContext={}", tenantId, JobContextUtil.getTenantId());
        }
        CallbackBaseResponseDTO response;
        switch (callbackRequest.getMethod()) {
            case LIST_INSTANCE:
                response = listInstanceResp(callbackRequest);
                break;
            case FETCH_INSTANCE_INFO:
                response = fetchInstanceResp(callbackRequest);
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
            case FETCH_RESOURCE_TYPE_SCHEMA:
                response = fetchResourceTypeSchemaResp(callbackRequest);
                break;
            default:
                log.error("Unknown callback method!|{}|{}|{}|{}", callbackRequest.getMethod(),
                    callbackRequest.getType(), callbackRequest.getFilter(), callbackRequest.getPage());
                response = new CallbackBaseResponseDTO();
        }
        return response;
    }

    /**
     * 从查询条件中提取Job业务ID
     *
     * @param searchCondition 查询条件
     * @return Job业务ID
     */
    protected Long getAppIdBySearchCondition(IamSearchCondition searchCondition) {
        ResourceScope resourceScope = extractResourceScopeCondition(searchCondition);
        Long appId = appScopeMappingService.getAppIdByScope(resourceScope);
        checkAppIdInTenant(appId, resourceScope);
        return appId;
    }

    /**
     * 从IAM查询条件中提取ResourceScope相关的查询条件
     *
     * @param searchCondition 查询条件
     * @return 资源范围对象
     * @throws InvalidParamException 如果查询条件中未包含ResourceScope
     */
    private ResourceScope extractResourceScopeCondition(IamSearchCondition searchCondition) {
        if (StringUtils.isNotBlank(searchCondition.getParentResourceTypeId())) {
            ResourceTypeEnum iamResourceType =
                ResourceTypeEnum.getByResourceTypeId(searchCondition.getParentResourceTypeId());
            return IamUtil.getResourceScopeFromIamResource(iamResourceType, searchCondition.getParentResourceId());
        } else {
            throw new InvalidParamException(
                ErrorCode.MISSING_PARAM_WITH_PARAM_NAME,
                new String[]{
                    "parentResourceTypeId,parentResourceId"
                }
            );
        }
    }

    /**
     * 检查Job业务ID是否在当前租户内
     *
     * @param appId         Job业务ID
     * @param resourceScope 资源范围
     * @throws InvalidParamException 如果appId为空或不在当前租户内
     */
    private void checkAppIdInTenant(Long appId, ResourceScope resourceScope) {
        if (appId == null || appId <= 0L) {
            throw new InvalidParamException(
                ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{
                    "parentResourceTypeId,parentResourceId",
                    "Cannot find appId by " + resourceScope.toBasicStr()
                }
            );
        }
        String tenantId = tenantService.getTenantIdByAppId(appId);
        if (StringUtils.isBlank(tenantId)) {
            log.warn("Cannot find tenantId for appId: {}", appId);
            return;
        }
        String currentTenantId = JobContextUtil.getTenantId();
        if (!tenantId.equals(currentTenantId)) {
            log.warn(
                "tenantId of appId={} is {}, currentTenantId is {}, forbidden",
                appId,
                tenantId,
                currentTenantId
            );
            throw new InvalidParamException(
                ErrorCode.ACCESS_CROSS_TENANT_FORBIDDEN,
                new String[]{
                    currentTenantId,
                    resourceScope.toBasicStr(),
                    tenantId
                }
            );
        }
    }
}
