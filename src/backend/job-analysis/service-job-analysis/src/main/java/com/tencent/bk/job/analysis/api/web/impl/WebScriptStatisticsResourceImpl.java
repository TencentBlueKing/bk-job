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

package com.tencent.bk.job.analysis.api.web.impl;

import com.tencent.bk.audit.annotations.ActionAuditRecord;
import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.job.analysis.api.web.WebScriptStatisticsResource;
import com.tencent.bk.job.analysis.model.web.ScriptCiteStatisticVO;
import com.tencent.bk.job.analysis.service.ScriptStatisticService;
import com.tencent.bk.job.common.audit.constants.EventContentConstants;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.date.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class WebScriptStatisticsResourceImpl implements WebScriptStatisticsResource {

    private final ScriptStatisticService scriptStatisticService;
    private final AppScopeMappingService appScopeMappingService;

    @Autowired
    public WebScriptStatisticsResourceImpl(ScriptStatisticService scriptStatisticService,
                                           AppScopeMappingService appScopeMappingService) {
        this.scriptStatisticService = scriptStatisticService;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    @AuditEntry(actionId = ActionId.DASHBOARD_VIEW)
    @ActionAuditRecord(
        actionId = ActionId.DASHBOARD_VIEW,
        content = EventContentConstants.VIEW_ANALYSIS_DASHBOARD
    )
    public Response<ScriptCiteStatisticVO> scriptCiteInfo(String username, List<String> scopes, String date) {
        if (StringUtils.isBlank(date)) {
            date = DateUtils.getCurrentDateStr();
        }
        ScriptCiteStatisticVO scriptCiteStatisticVO = scriptStatisticService.scriptCiteInfo(getAppIdList(scopes), date);
        return Response.buildSuccessResp(scriptCiteStatisticVO);
    }

    private List<Long> getAppIdList(List<String> scopes) {
        if (CollectionUtils.isNotEmpty(scopes)) {
            return scopes.stream().map(scope -> {
                String[] scopeParts = scope.split(":");
                ResourceScope resourceScope = new ResourceScope(scopeParts[0], scopeParts[1]);
                return appScopeMappingService.getAppIdByScope(resourceScope);
            }).collect(Collectors.toList());
        } else {
            return null;
        }
    }
}
