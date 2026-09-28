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

package com.tencent.bk.job.file.worker.service;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.dto.CommonCredential;
import com.tencent.bk.job.common.util.http.HttpUrlSafetyUtils;
import com.tencent.bk.job.file.worker.cos.JobTencentInnerCOSClient;
import com.tencent.bk.job.file.worker.model.req.BaseReq;
import com.tencent.bk.job.file_gateway.consts.FileSourceInfoConsts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class COSBaseService {

    public JobTencentInnerCOSClient getCOSClientFromBaseReq(BaseReq req) {
        CommonCredential credential = req.getCredential();
        Map<String, Object> fileSourceInfoMap = req.getFileSourceInfoMap();
        log.debug("req={}", req);
        String endPointDomain = getEndPointDomain(req);
        checkEndPointDomain(endPointDomain);
        return new JobTencentInnerCOSClient(credential.getAccessKey(), credential.getSecretKey(),
            endPointDomain, fileSourceInfoMap.get("app_id").toString());
    }

    public String getEndPointDomain(BaseReq req) {
        Map<String, Object> fileSourceInfoMap = req.getFileSourceInfoMap();
        return (String) fileSourceInfoMap.get(FileSourceInfoConsts.KEY_COS_END_POINT_DOMAIN);
    }

    private void checkEndPointDomain(String endPointDomain) {
        String host = HttpUrlSafetyUtils.parseHttpUrlOrBareHost(endPointDomain);
        if (host == null || HttpUrlSafetyUtils.isResolvedToDangerousAddress(host,
            HttpUrlSafetyUtils.DEFAULT_HOST_RESOLVER)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME,
                new String[]{FileSourceInfoConsts.KEY_COS_END_POINT_DOMAIN});
        }
    }
}
