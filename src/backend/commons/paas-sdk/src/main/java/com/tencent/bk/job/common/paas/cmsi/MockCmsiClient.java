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

package com.tencent.bk.job.common.paas.cmsi;

import com.tencent.bk.job.common.esb.config.AppProperties;
import com.tencent.bk.job.common.esb.config.BkApiGatewayProperties;
import com.tencent.bk.job.common.esb.config.EsbProperties;
import com.tencent.bk.job.common.esb.model.BkApiAuthorization;
import com.tencent.bk.job.common.esb.sdk.BkApiV1Client;
import com.tencent.bk.job.common.paas.model.EsbNotifyChannelDTO;
import com.tencent.bk.job.common.paas.model.NotifyMessageDTO;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.common.util.http.HttpHelperFactory;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.tencent.bk.job.common.metrics.CommonMetricNames.ESB_CMSI_API;

public class MockCmsiClient extends BkApiV1Client implements ICmsiClient {

    private final BkApiAuthorization authorization;

    public MockCmsiClient(BkApiGatewayProperties apiGatewayProperties,
                          AppProperties appProperties,
                          MeterRegistry meterRegistry,
                          TenantEnvService tenantEnvService) {
        super(
            meterRegistry,
            ESB_CMSI_API,
            apiGatewayProperties.getCmsi().getUrl(),
            HttpHelperFactory.createHttpHelper(
                httpClientBuilder -> httpClientBuilder.addInterceptorLast(getLogBkApiRequestIdInterceptor())
            ),
            tenantEnvService
        );
        this.authorization = BkApiAuthorization.appAuthorization(appProperties.getCode(),
            appProperties.getSecret(), "admin");
    }

    @Override
    public List<EsbNotifyChannelDTO> getNotifyChannelList(String tenantId) {
        if ("system".equals(tenantId)) {
            List<EsbNotifyChannelDTO> channelList = new ArrayList<>();
            channelList.add(new EsbNotifyChannelDTO(
                "weixin",
                "微信",
                true,
                ""
            ));
            channelList.add(new EsbNotifyChannelDTO(
                "rtx",
                "企业微信",
                true,
                ""
            ));
            return channelList;
        } else if ("putongoa".equals(tenantId)) {
            List<EsbNotifyChannelDTO> channelList = new ArrayList<>();
            channelList.add(new EsbNotifyChannelDTO(
                "weixin",
                "微信",
                true,
                ""
            ));
            channelList.add(new EsbNotifyChannelDTO(
                "sms",
                "短信",
                true,
                ""
            ));
            return channelList;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void sendMsg(String msgType, NotifyMessageDTO notifyMessageDTO, String tenantId) {
        return;
    }
}
