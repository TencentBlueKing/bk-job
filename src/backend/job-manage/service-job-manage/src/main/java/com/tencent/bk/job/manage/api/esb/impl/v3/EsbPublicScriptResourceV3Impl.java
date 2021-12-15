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
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.manage.api.esb.v3.EsbPublicScriptV3Resource;
import com.tencent.bk.job.manage.api.esb.v3.EsbScriptV3Resource;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetScriptListV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetScriptVersionDetailV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetScriptVersionListV3Req;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbScriptV3DTO;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbScriptVersionDetailV3DTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class EsbPublicScriptResourceV3Impl implements EsbPublicScriptV3Resource {
    private final EsbScriptV3Resource esbScriptV3Resource;

    @Autowired
    public EsbPublicScriptResourceV3Impl(EsbScriptV3Resource esbScriptV3Resource) {
        this.esbScriptV3Resource = esbScriptV3Resource;
    }

    @Override
    public EsbResp<EsbPageDataV3<EsbScriptV3DTO>> getPublicScriptList(String username,
                                                                      String appCode,
                                                                      String name,
                                                                      Integer scriptLanguage,
                                                                      Integer start,
                                                                      Integer length) {
        EsbGetScriptListV3Req request = new EsbGetScriptListV3Req();
        request.setUserName(username);
        request.setAppCode(appCode);
        request.setName(name);
        request.setScriptLanguage(scriptLanguage);
        request.setStart(start);
        request.setLength(length);
        return getPublicScriptListUsingPost(request);
    }

    @Override
    public EsbResp<EsbPageDataV3<EsbScriptVersionDetailV3DTO>> getPublicScriptVersionList(String username,
                                                                                          String appCode,
                                                                                          String scriptId,
                                                                                          boolean returnScriptContent,
                                                                                          Integer start,
                                                                                          Integer length) {
        EsbGetScriptVersionListV3Req request = new EsbGetScriptVersionListV3Req();
        request.setUserName(username);
        request.setAppCode(appCode);
        request.setScriptId(scriptId);
        request.setReturnScriptContent(returnScriptContent);
        request.setStart(start);
        request.setLength(length);
        return getPublicScriptVersionListUsingPost(request);
    }

    @Override
    public EsbResp<EsbScriptVersionDetailV3DTO> getPublicScriptVersionDetail(String username,
                                                                             String appCode,
                                                                             Long scriptVersionId,
                                                                             String scriptId,
                                                                             String version) {
        EsbGetScriptVersionDetailV3Req request = new EsbGetScriptVersionDetailV3Req();
        request.setUserName(username);
        request.setAppCode(appCode);
        request.setId(scriptVersionId);
        request.setScriptId(scriptId);
        request.setVersion(version);
        return getPublicScriptVersionDetailUsingPost(request);
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_get_public_script_list"})
    public EsbResp<EsbPageDataV3<EsbScriptV3DTO>> getPublicScriptListUsingPost(EsbGetScriptListV3Req request) {
        request.setAppId(JobConstants.PUBLIC_APP_ID);
        return esbScriptV3Resource.getScriptListUsingPost(request);
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_get_public_script_version_list"})
    public EsbResp<EsbPageDataV3<EsbScriptVersionDetailV3DTO>> getPublicScriptVersionListUsingPost(
        EsbGetScriptVersionListV3Req request
    ) {
        request.setAppId(JobConstants.PUBLIC_APP_ID);
        return esbScriptV3Resource.getScriptVersionListUsingPost(request);
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_get_public_script_version_detail"})
    public EsbResp<EsbScriptVersionDetailV3DTO> getPublicScriptVersionDetailUsingPost(EsbGetScriptVersionDetailV3Req request) {
        request.setAppId(JobConstants.PUBLIC_APP_ID);
        return esbScriptV3Resource.getScriptVersionDetailUsingPost(request);
    }
}
