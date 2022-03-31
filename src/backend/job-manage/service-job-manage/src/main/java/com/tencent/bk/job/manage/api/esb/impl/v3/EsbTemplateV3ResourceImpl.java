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

package com.tencent.bk.job.manage.api.esb.impl.v3;

import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.job.v3.EsbPageDataV3;
import com.tencent.bk.job.common.esb.util.EsbDTOAppScopeMappingHelper;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.BusinessAuthService;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.manage.api.esb.v3.EsbTemplateV3Resource;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetTemplateListV3Request;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbTemplateBasicInfoV3DTO;
import com.tencent.bk.job.manage.model.query.TaskTemplateQuery;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

/**
 * @since 15/10/2020 18:08
 */
@Slf4j
@RestController
public class EsbTemplateV3ResourceImpl implements EsbTemplateV3Resource {
    private final TaskTemplateService taskTemplateService;
    private final BusinessAuthService businessAuthService;
    private final AppScopeMappingService appScopeMappingService;

    @Autowired
    public EsbTemplateV3ResourceImpl(TaskTemplateService taskTemplateService,
                                     BusinessAuthService businessAuthService,
                                     AppScopeMappingService appScopeMappingService) {
        this.taskTemplateService = taskTemplateService;
        this.businessAuthService = businessAuthService;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    public EsbResp<EsbPageDataV3<EsbTemplateBasicInfoV3DTO>> getTemplateList(String username,
                                                                             String appCode,
                                                                             Long bizId,
                                                                             String scopeType,
                                                                             String scopeId,
                                                                             String creator,
                                                                             String name,
                                                                             Long createTimeStart,
                                                                             Long createTimeEnd,
                                                                             String lastModifyUser,
                                                                             Long lastModifyTimeStart,
                                                                             Long lastModifyTimeEnd,
                                                                             Integer start,
                                                                             Integer length) {
        EsbGetTemplateListV3Request request = new EsbGetTemplateListV3Request();
        request.setUserName(username);
        request.setAppCode(appCode);
        request.setBizId(bizId);
        request.setScopeType(scopeType);
        request.setScopeId(scopeId);
        request.setCreator(creator);
        request.setName(name);
        request.setCreateTimeStart(createTimeStart);
        request.setLastModifyUser(lastModifyUser);
        request.setCreateTimeEnd(createTimeEnd);
        request.setLastModifyTimeEnd(lastModifyTimeEnd);
        request.setLastModifyTimeStart(lastModifyTimeStart);
        request.setStart(start);
        request.setLength(length);
        return getTemplateListUsingPost(request);
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_get_job_template_list"})
    public EsbResp<EsbPageDataV3<EsbTemplateBasicInfoV3DTO>> getTemplateListUsingPost(
        EsbGetTemplateListV3Request request) {
        request.fillAppResourceScope(appScopeMappingService);
        ValidateResult checkResult = checkRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Get template list, request is illegal!");
            throw new InvalidParamException(checkResult);
        }
        long appId = request.getAppId();

        AuthResult authResult =
            businessAuthService.authAccessBusiness(
                request.getUserName(), new AppResourceScope(request.getAppId()));
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        if (request.getStart() != null) {
            baseSearchCondition.setStart(request.getStart());
        } else {
            baseSearchCondition.setStart(0);
        }
        if (request.getLength() != null) {
            baseSearchCondition.setLength(request.getLength());
        } else {
            baseSearchCondition.setLength(20);
        }
        baseSearchCondition.setCreator(request.getCreator());
        baseSearchCondition.setLastModifyUser(request.getLastModifyUser());

        baseSearchCondition.setCreateTimeStart(request.getCreateTimeStart());
        baseSearchCondition.setCreateTimeEnd(request.getCreateTimeEnd());
        baseSearchCondition.setLastModifyTimeStart(request.getLastModifyTimeStart());
        baseSearchCondition.setLastModifyTimeEnd(request.getLastModifyTimeEnd());

        TaskTemplateQuery query = TaskTemplateQuery.builder().appId(appId).name(request.getName())
            .baseSearchCondition(baseSearchCondition).build();

        PageData<TaskTemplateInfoDTO> pageTemplates =
            taskTemplateService.listPageTaskTemplatesBasicInfo(query, null);
        EsbPageDataV3<EsbTemplateBasicInfoV3DTO> esbPageData = EsbPageDataV3.from(pageTemplates,
            this::convertToEsbTemplateBasicInfo);
        return EsbResp.buildSuccessResp(esbPageData);
    }


    private ValidateResult checkRequest(EsbGetTemplateListV3Request request) {
        // TODO 暂不校验，后面补上
        return ValidateResult.pass();
    }

    private EsbTemplateBasicInfoV3DTO convertToEsbTemplateBasicInfo(TaskTemplateInfoDTO taskTemplate) {
        EsbTemplateBasicInfoV3DTO result = new EsbTemplateBasicInfoV3DTO();
        result.setId(taskTemplate.getId());
        EsbDTOAppScopeMappingHelper.fillEsbAppScopeDTOByAppId(taskTemplate.getAppId(), result);
        result.setName(taskTemplate.getName());
        result.setCreator(taskTemplate.getCreator());
        result.setLastModifyUser(taskTemplate.getLastModifyUser());
        result.setCreateTime(taskTemplate.getCreateTime());
        result.setLastModifyTime(taskTemplate.getLastModifyTime());
        return result;
    }
}
