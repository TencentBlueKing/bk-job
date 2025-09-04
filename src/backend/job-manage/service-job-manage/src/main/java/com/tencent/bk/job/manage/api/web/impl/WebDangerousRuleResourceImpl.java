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

package com.tencent.bk.job.manage.api.web.impl;

import com.tencent.bk.audit.annotations.ActionAuditRecord;
import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.job.common.audit.constants.EventContentConstants;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.manage.api.web.WebDangerousRuleResource;
import com.tencent.bk.job.manage.model.dto.globalsetting.DangerousRuleDTO;
import com.tencent.bk.job.manage.model.query.DangerousRuleQuery;
import com.tencent.bk.job.manage.model.web.request.globalsetting.AddOrUpdateDangerousRuleReq;
import com.tencent.bk.job.manage.model.web.request.globalsetting.MoveDangerousRuleReq;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.DangerousRuleVO;
import com.tencent.bk.job.manage.service.DangerousRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
public class WebDangerousRuleResourceImpl implements WebDangerousRuleResource {
    private final DangerousRuleService dangerousRuleService;

    @Autowired
    public WebDangerousRuleResourceImpl(DangerousRuleService dangerousRuleService) {
        this.dangerousRuleService = dangerousRuleService;
    }

    @Override
    @AuditEntry(actionId = ActionId.HIGH_RISK_DETECT_RULE)
    @ActionAuditRecord(
        actionId = ActionId.HIGH_RISK_DETECT_RULE,
        content = EventContentConstants.VIEW_HIGH_RISK_DETECT_RULE
    )
    public Response<List<DangerousRuleVO>> listDangerousRules(String username,
                                                              String expression,
                                                              String description,
                                                              List<Byte> scriptTypeList,
                                                              List<Byte> action) {
        DangerousRuleQuery query = DangerousRuleQuery.builder()
            .expression(expression)
            .description(description)
            .scriptTypeList(scriptTypeList)
            .action(action)
            .build();
        return Response.buildSuccessResp(dangerousRuleService.listDangerousRules(query));
    }


    @Override
    @AuditEntry(actionId = ActionId.HIGH_RISK_DETECT_RULE)
    public Response<DangerousRuleVO> createDangerousRule(String username, AddOrUpdateDangerousRuleReq req) {
        DangerousRuleDTO dangerousRule = dangerousRuleService.createDangerousRule(username, req);
        return Response.buildSuccessResp(dangerousRule.toVO());
    }

    @Override
    @AuditEntry(actionId = ActionId.HIGH_RISK_DETECT_RULE)
    public Response<DangerousRuleVO> updateDangerousRule(String username,
                                                         Long id,
                                                         AddOrUpdateDangerousRuleReq req) {
        req.setId(id);
        DangerousRuleDTO dangerousRule = dangerousRuleService.updateDangerousRule(username, req);
        return Response.buildSuccessResp(dangerousRule.toVO());
    }

    @Override
    @AuditEntry(actionId = ActionId.HIGH_RISK_DETECT_RULE)
    public Response<Integer> moveDangerousRule(String username, MoveDangerousRuleReq req) {
        return Response.buildSuccessResp(dangerousRuleService.moveDangerousRule(username, req));
    }

    @Override
    @AuditEntry(actionId = ActionId.HIGH_RISK_DETECT_RULE)
    public Response<Integer> deleteDangerousRuleById(String username, Long id) {
        return Response.buildSuccessResp(dangerousRuleService.deleteDangerousRuleById(username, id));
    }
}
