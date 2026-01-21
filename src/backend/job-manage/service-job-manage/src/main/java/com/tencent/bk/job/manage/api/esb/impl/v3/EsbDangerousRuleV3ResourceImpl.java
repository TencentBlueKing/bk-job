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

package com.tencent.bk.job.manage.api.esb.impl.v3;

import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.audit.annotations.AuditRequestBody;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.api.common.constants.EnableStatusEnum;
import com.tencent.bk.job.manage.api.esb.v3.EsbDangerousRuleV3Resource;
import com.tencent.bk.job.manage.model.dto.globalsetting.DangerousRuleDTO;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbCreateDangerousRuleV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetDangerousRuleV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbManageDangerousRuleV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbUpdateDangerousRuleV3Req;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbDangerousRuleStatusV3DTO;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbDangerousRuleV3DTO;
import com.tencent.bk.job.manage.model.query.DangerousRuleQuery;
import com.tencent.bk.job.manage.model.web.request.globalsetting.AddOrUpdateDangerousRuleReq;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.DangerousRuleVO;
import com.tencent.bk.job.manage.service.CurrentTenantDangerousRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class EsbDangerousRuleV3ResourceImpl implements EsbDangerousRuleV3Resource {
    private final CurrentTenantDangerousRuleService currentTenantDangerousRuleService;

    @Autowired
    public EsbDangerousRuleV3ResourceImpl(CurrentTenantDangerousRuleService currentTenantDangerousRuleService) {
        this.currentTenantDangerousRuleService = currentTenantDangerousRuleService;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_create_dangerous_rule"})
    @AuditEntry(actionId = ActionId.HIGH_RISK_DETECT_RULE)
    public EsbResp<EsbDangerousRuleV3DTO> createDangerousRule(String username,
                                                              String appCode,
                                                              @AuditRequestBody EsbCreateDangerousRuleV3Req request) {
        User user = JobContextUtil.getUser();
        AddOrUpdateDangerousRuleReq req = new AddOrUpdateDangerousRuleReq();
        req.setExpression(request.getExpression());
        req.setScriptTypeList(request.getScriptTypeList());
        req.setDescription(request.getDescription());
        req.setAction(request.getAction());
        DangerousRuleDTO dangerousRule = currentTenantDangerousRuleService.createDangerousRule(user, req);
        return EsbResp.buildSuccessResp(dangerousRule.toEsbDangerousRuleV3DTO());
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_update_dangerous_rule"})
    @AuditEntry(actionId = ActionId.HIGH_RISK_DETECT_RULE)
    public EsbResp<EsbDangerousRuleV3DTO> updateDangerousRule(String username,
                                                              String appCode,
                                                              @AuditRequestBody EsbUpdateDangerousRuleV3Req request) {
        User user = JobContextUtil.getUser();
        AddOrUpdateDangerousRuleReq req = new AddOrUpdateDangerousRuleReq();
        DangerousRuleDTO dangerousRuleDTO = currentTenantDangerousRuleService.getDangerousRuleById(request.getId());
        req.setId(request.getId());
        req.setExpression(request.getExpression());
        req.setScriptTypeList(request.getScriptTypeList());
        req.setDescription(request.getDescription());
        req.setAction(request.getAction());
        req.setStatus(dangerousRuleDTO.getStatus());
        DangerousRuleDTO updateDangerousRuleDTO = currentTenantDangerousRuleService.updateDangerousRule(user, req);
        return EsbResp.buildSuccessResp(updateDangerousRuleDTO.toEsbDangerousRuleV3DTO());
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_delete_dangerous_rule"})
    @AuditEntry(actionId = ActionId.HIGH_RISK_DETECT_RULE)
    public EsbResp deleteDangerousRule(String username,
                                       String appCode,
                                       @AuditRequestBody EsbManageDangerousRuleV3Req request) {
        User user = JobContextUtil.getUser();
        currentTenantDangerousRuleService.deleteDangerousRuleById(user, request.getId());
        return EsbResp.buildSuccessResp(null);
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_get_dangerous_rule_list"})
    @AuditEntry(actionId = ActionId.HIGH_RISK_DETECT_RULE)
    public EsbResp<List<EsbDangerousRuleV3DTO>> getDangerousRuleListUsingPost(
        String username,
        String appCode,
        @AuditRequestBody EsbGetDangerousRuleV3Req request) {
        DangerousRuleQuery query = DangerousRuleQuery.builder()
            .expression(request.getExpression())
            .description(request.getDescription())
            .scriptTypeList(request.getScriptTypeList())
            .action(request.getAction() != null ? Collections.singletonList(request.getAction().byteValue()) : null)
            .build();
        List<DangerousRuleVO> dangerousRuleVOS = currentTenantDangerousRuleService.listDangerousRules(query);
        List<EsbDangerousRuleV3DTO> esbDangerousRuleV3DTOList = dangerousRuleVOS.stream()
            .map(DangerousRuleVO::toEsbDangerousRuleV3DTO)
            .collect(Collectors.toList());
        return EsbResp.buildSuccessResp(esbDangerousRuleV3DTOList);
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_enable_dangerous_rule"})
    @AuditEntry(actionId = ActionId.HIGH_RISK_DETECT_RULE)
    public EsbResp<EsbDangerousRuleStatusV3DTO> enableDangerousRule(String username,
                                                              String appCode,
                                                              @AuditRequestBody EsbManageDangerousRuleV3Req request) {
        User user = JobContextUtil.getUser();
        DangerousRuleDTO dangerousRuleDTO = currentTenantDangerousRuleService.updateDangerousRuleStatus(user,
            request.getId(),
            EnableStatusEnum.ENABLED);
        return EsbResp.buildSuccessResp(dangerousRuleDTO.toEsbDangerousRuleStatusV3DTO());
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_disable_dangerous_rule"})
    @AuditEntry(actionId = ActionId.HIGH_RISK_DETECT_RULE)
    public EsbResp<EsbDangerousRuleStatusV3DTO> disableDangerousRule(
        String username,
        String appCode,
        @AuditRequestBody EsbManageDangerousRuleV3Req request) {
        User user = JobContextUtil.getUser();
        DangerousRuleDTO dangerousRuleDTO = currentTenantDangerousRuleService.updateDangerousRuleStatus(user,
            request.getId(),
            EnableStatusEnum.DISABLED);
        return EsbResp.buildSuccessResp(dangerousRuleDTO.toEsbDangerousRuleStatusV3DTO());
    }
}
