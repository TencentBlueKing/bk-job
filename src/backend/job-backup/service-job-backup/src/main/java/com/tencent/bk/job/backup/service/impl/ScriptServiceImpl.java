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

package com.tencent.bk.job.backup.service.impl;

import com.tencent.bk.job.backup.client.ServiceScriptResourceClient;
import com.tencent.bk.job.backup.client.WebScriptResourceClient;
import com.tencent.bk.job.backup.service.ScriptService;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.manage.model.inner.ServiceScriptDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @since 6/8/2020 22:01
 */
@Slf4j
@Service
public class ScriptServiceImpl implements ScriptService {
    private final WebScriptResourceClient webScriptResourceClient;
    private final ServiceScriptResourceClient serviceScriptResourceClient;

    @Autowired
    public ScriptServiceImpl(WebScriptResourceClient webScriptResourceClient,
                             ServiceScriptResourceClient serviceScriptResourceClient) {
        this.webScriptResourceClient = webScriptResourceClient;
        this.serviceScriptResourceClient = serviceScriptResourceClient;
    }

    @Override
    public ServiceScriptDTO getScriptInfoById(String username, Long appId, Long scriptVersionId) {
        try {
            InternalResponse<ServiceScriptDTO> scriptByVersionIdResponse =
                serviceScriptResourceClient.getScriptByAppIdAndScriptVersionId(username, appId, scriptVersionId);
            if (scriptByVersionIdResponse != null) {
                if (scriptByVersionIdResponse.getCode() == 0) {
                    return scriptByVersionIdResponse.getData();
                }
            }
        } catch (Exception e) {
            log.error("Error while getting script info by id!|{}|{}|{}", username, appId, scriptVersionId, e);
        }
        return null;
    }
}
