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

package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.execute.client.ScriptResourceClient;
import com.tencent.bk.job.execute.service.ScriptService;
import com.tencent.bk.job.manage.model.inner.ServiceScriptDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ScriptServiceImpl implements ScriptService {
    private final ScriptResourceClient scriptResourceClient;

    public ScriptServiceImpl(ScriptResourceClient scriptResourceClient) {
        this.scriptResourceClient = scriptResourceClient;
    }

    @Override
    public ServiceScriptDTO getScriptByScriptVersionId(String username, long appId, long scriptVersionId)
        throws ServiceException {
        InternalResponse<ServiceScriptDTO> resp = scriptResourceClient.getScriptByAppIdAndScriptVersionId(username,
            appId, scriptVersionId);
//        if (!resp.isSuccess()) {
//            throw new ServiceException(resp.getCode(), resp.getErrorMsg());
//        }
        return resp.getData();
    }

    @Override
    public ServiceScriptDTO getScriptByScriptVersionId(long scriptVersionId) throws ServiceException {
        InternalResponse<ServiceScriptDTO> resp = scriptResourceClient.getScriptByScriptVersionId(scriptVersionId);
        if (!resp.isSuccess()) {
            throw new InternalException(resp.getCode());
        }
        return resp.getData();
    }

    @Override
    public ServiceScriptDTO getBasicScriptInfo(String scriptId) {
        return null;
    }

    @Override
    public ServiceScriptDTO getOnlineScriptVersion(String scriptId) {
        InternalResponse<ServiceScriptDTO> resp = scriptResourceClient.getOnlineScriptVersion(scriptId);
        if (!resp.isSuccess()) {
            throw new InternalException(resp.getCode());
        }
        return resp.getData();
    }
}
