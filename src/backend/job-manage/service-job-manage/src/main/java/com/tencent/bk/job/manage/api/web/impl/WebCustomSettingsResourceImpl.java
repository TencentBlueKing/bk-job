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

package com.tencent.bk.job.manage.api.web.impl;

import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.manage.api.web.WebCustomSettingsResource;
import com.tencent.bk.job.manage.model.web.request.customsetting.BatchGetCustomSettingsReq;
import com.tencent.bk.job.manage.model.web.request.customsetting.DeleteCustomSettingsReq;
import com.tencent.bk.job.manage.model.web.request.customsetting.SaveCustomSettingsReq;
import com.tencent.bk.job.manage.service.impl.CustomSettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Slf4j
public class WebCustomSettingsResourceImpl implements WebCustomSettingsResource {

    private final CustomSettingsService customSettingsService;

    @Autowired
    public WebCustomSettingsResourceImpl(
        CustomSettingsService customSettingsService) {
        this.customSettingsService = customSettingsService;
    }

    @Override
    public Response<Map<String, Map<String, Object>>> saveCustomSettings(String username,
                                                                         AppResourceScope appResourceScope,
                                                                         String scopeType,
                                                                         String scopeId,
                                                                         SaveCustomSettingsReq req) {
        return Response.buildSuccessResp(customSettingsService.saveCustomSettings(username, appResourceScope, req));
    }

    @Override
    public Response<Map<String, Map<String, Object>>> batchGetCustomSettings(String username,
                                                                             AppResourceScope appResourceScope,
                                                                             String scopeType,
                                                                             String scopeId,
                                                                             BatchGetCustomSettingsReq req) {
        return Response.buildSuccessResp(customSettingsService.batchGetCustomSettings(username, appResourceScope, req));
    }

    @Override
    public Response<Integer> deleteCustomSettings(String username,
                                                  AppResourceScope appResourceScope,
                                                  String scopeType,
                                                  String scopeId,
                                                  DeleteCustomSettingsReq req) {
        return Response.buildSuccessResp(customSettingsService.deleteCustomSettings(username, appResourceScope, req));
    }
}
