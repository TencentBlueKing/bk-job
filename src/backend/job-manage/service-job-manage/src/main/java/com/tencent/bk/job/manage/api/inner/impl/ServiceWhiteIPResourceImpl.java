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

package com.tencent.bk.job.manage.api.inner.impl;

import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.manage.api.inner.ServiceWhiteIPResource;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIPRecordDTO;
import com.tencent.bk.job.manage.model.inner.ServiceWhiteIPInfo;
import com.tencent.bk.job.manage.model.web.request.whiteip.WhiteIPRecordCreateUpdateReq;
import com.tencent.bk.job.manage.service.WhiteIPService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class ServiceWhiteIPResourceImpl implements ServiceWhiteIPResource {

    private final WhiteIPService whiteIPService;

    public ServiceWhiteIPResourceImpl(WhiteIPService whiteIPService) {
        this.whiteIPService = whiteIPService;
    }


    @Override
    public InternalResponse<List<String>> getWhiteIPActionScopes(Long appId, String ip, Long cloudAreaId, Long hostId) {
        return InternalResponse.buildSuccessResp(whiteIPService.getWhiteIPActionScopes(appId, ip, cloudAreaId, hostId));
    }

    @Override
    public InternalResponse<List<ServiceWhiteIPInfo>> listWhiteIPInfos() {
        return InternalResponse.buildSuccessResp(whiteIPService.listWhiteIPInfos());
    }

    @Override
    public InternalResponse<Long> saveWhiteIP(String username, WhiteIPRecordCreateUpdateReq createUpdateReq) {
        WhiteIPRecordDTO record;
        if (createUpdateReq.getId() != null && createUpdateReq.getId() > 0) {
            record = whiteIPService.updateWhiteIP(username, createUpdateReq);
        } else {
            record = whiteIPService.createWhiteIP(username, createUpdateReq);
        }
        return InternalResponse.buildSuccessResp(record.getId());
    }
}
