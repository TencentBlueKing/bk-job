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

package com.tencent.bk.job.manage.api.inner.impl;

import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.manage.api.inner.ServiceCheckScriptResource;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.model.dto.ScriptCheckResultItemDTO;
import com.tencent.bk.job.manage.model.inner.ServiceScriptCheckResultItemDTO;
import com.tencent.bk.job.manage.model.inner.request.ServiceCheckScriptRequest;
import com.tencent.bk.job.manage.service.ScriptCheckService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class ServiceCheckScriptResourceImpl implements ServiceCheckScriptResource {
    private final ScriptCheckService scriptCheckService;

    @Autowired
    public ServiceCheckScriptResourceImpl(ScriptCheckService scriptCheckService) {
        this.scriptCheckService = scriptCheckService;
    }

    @Override
    public InternalResponse<List<ServiceScriptCheckResultItemDTO>> check(ServiceCheckScriptRequest checkScriptRequest) {
        List<ScriptCheckResultItemDTO> checkResultItems = scriptCheckService.checkScriptWithDangerousRule(
            ScriptTypeEnum.valueOf(checkScriptRequest.getScriptType()), checkScriptRequest.getScriptContent());
        return InternalResponse.buildSuccessResp(checkResultItems.stream()
            .map(ScriptCheckResultItemDTO::toServiceScriptCheckResultDTO)
            .collect(Collectors.toList()));
    }
}
