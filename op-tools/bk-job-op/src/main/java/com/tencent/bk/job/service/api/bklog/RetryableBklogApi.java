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

package com.tencent.bk.job.service.api.bklog;

import com.tencent.bk.job.config.BkApiGwProperties;
import com.tencent.bk.job.config.BkLogAuthProperties;
import com.tencent.bk.job.utils.RetryUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

public class RetryableBklogApi extends BkLogApi {

    private final int retryCnt;
    private final int retryInterval;

    public RetryableBklogApi(RestTemplate restTemplate,
                             BkLogAuthProperties bkLogAuthProperties,
                             BkApiGwProperties bkApiGwProperties) {
        super(restTemplate, bkLogAuthProperties, bkApiGwProperties);
        this.retryCnt = bkApiGwProperties.getBkLog().getRetryCount();
        this.retryInterval = bkApiGwProperties.getBkLog().getRetryInterval();
    }

    @Override
    public LogQueryResp logSearch(LogQueryReq logQueryReq) {
        return RetryUtils.executeWithRetry(
            () -> super.logSearch(logQueryReq),
            retryCnt,
            Duration.ofSeconds(retryInterval)
        );
    }
}
