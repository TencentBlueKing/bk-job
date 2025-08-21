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

package com.tencent.bk.job.common.paas.cmsi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.HttpMethodEnum;
import com.tencent.bk.job.common.esb.config.AppProperties;
import com.tencent.bk.job.common.esb.config.BkApiGatewayProperties;
import com.tencent.bk.job.common.esb.metrics.EsbMetricTags;
import com.tencent.bk.job.common.esb.model.OpenApiRequestInfo;
import com.tencent.bk.job.common.esb.model.OpenApiResponse;
import com.tencent.bk.job.common.esb.sdk.BkApiV2Client;
import com.tencent.bk.job.common.exception.InternalCmsiException;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.error.ErrorType;
import com.tencent.bk.job.common.paas.exception.PaasException;
import com.tencent.bk.job.common.paas.model.NotifyChannelDTO;
import com.tencent.bk.job.common.paas.model.NotifyChannelEnum;
import com.tencent.bk.job.common.paas.model.NotifyMessageDTO;
import com.tencent.bk.job.common.paas.model.cmsi.req.CmsiSendMsgV1BasicReq;
import com.tencent.bk.job.common.paas.model.cmsi.req.SendMailV1Req;
import com.tencent.bk.job.common.paas.model.cmsi.req.SendSmsV1Req;
import com.tencent.bk.job.common.paas.model.cmsi.req.SendVoiceV1Req;
import com.tencent.bk.job.common.paas.model.cmsi.req.SendWxV1Req;
import com.tencent.bk.job.common.paas.model.cmsi.resp.ApiGwCmsiChannelResp;
import com.tencent.bk.job.common.paas.user.IVirtualAdminAccountProvider;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.common.util.http.HttpHelperFactory;
import com.tencent.bk.job.common.util.http.HttpMetricUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;

import java.util.List;
import java.util.stream.Collectors;

import static com.tencent.bk.job.common.metrics.CommonMetricNames.ESB_CMSI_API;

/**
 * 消息通知 APIGW 客户端
 */
@Slf4j
public class CmsiApiGwClient extends BkApiV2Client implements ICmsiClient {

    private static final String API_GET_NOTIFY_CHANNEL_LIST = "/v1/channels/";
    private static final String API_SEND_MAIL = "/v1/send_mail/";
    private static final String API_SEND_SMS = "/v1/send_sms/";
    private static final String API_SEND_VOICE = "/v1/send_voice/";
    private static final String API_SEND_WEIXIN = "/v1/send_weixin/";

    private final AppProperties appProperties;
    private final IVirtualAdminAccountProvider virtualAdminAccountProvider;

    public CmsiApiGwClient(BkApiGatewayProperties bkApiGatewayProperties,
                           AppProperties appProperties,
                           MeterRegistry meterRegistry,
                           TenantEnvService tenantEnvService,
                           IVirtualAdminAccountProvider virtualAdminAccountProvider) {
        super(
            meterRegistry,
            ESB_CMSI_API,
            bkApiGatewayProperties.getCmsi().getUrl(),
            HttpHelperFactory.createHttpHelper(
                httpClientBuilder -> httpClientBuilder.addInterceptorLast(getLogBkApiRequestIdInterceptor())
            ),
            tenantEnvService
        );
        this.appProperties = appProperties;
        this.virtualAdminAccountProvider = virtualAdminAccountProvider;
    }

    @Override
    public List<NotifyChannelDTO> getNotifyChannelList(String tenantId) {
        List<ApiGwCmsiChannelResp> channelList = fetchAndGetChannelList(tenantId);
        return channelList.stream()
            .map(ApiGwCmsiChannelResp::toNotifyChannelDTO)
            .collect(Collectors.toList());
    }

    @Override
    public void sendMsg(String msgType, NotifyMessageDTO notifyMessageDTO, String tenantId) {
        CmsiSendMsgV1BasicReq req;
        String uri;

        if (NotifyChannelEnum.isMail(msgType)) {
            req = buildSendMailReq(notifyMessageDTO);
            uri = API_SEND_MAIL;
        } else if (NotifyChannelEnum.isSms(msgType)) {
            req = buildSendSmsReq(notifyMessageDTO);
            uri = API_SEND_SMS;
        } else if (NotifyChannelEnum.isVoice(msgType)) {
            req = buildSendVoiceReq(notifyMessageDTO);
            uri = API_SEND_VOICE;
        } else if (NotifyChannelEnum.isWeixin(msgType)) {
            req = buildSendWxReq(notifyMessageDTO);
            uri = API_SEND_WEIXIN;
        } else {
            // 未定义的渠道
            throw new InternalException(ErrorCode.CMSI_UNKNOWN_CHANNEL, new Object[]{msgType});
        }

        try {
            HttpMetricUtil.setHttpMetricName(CommonMetricNames.ESB_CMSI_API_HTTP);
            HttpMetricUtil.addTagForCurrentMetric(Tag.of(EsbMetricTags.KEY_API_NAME, uri));
            OpenApiResponse<Object> resp = doRequest(
                OpenApiRequestInfo.builder()
                    .method(HttpMethodEnum.POST)
                    .uri(uri)
                    .body(req)
                    .addHeader(buildTenantHeader(tenantId))
                    .authorization(buildAuthorization(
                        appProperties, virtualAdminAccountProvider.getVirtualAdminUsername(tenantId)))
                    .build(),
                new TypeReference<OpenApiResponse<Object>>() {
                }
            );

            if (resp.getError() != null) {
                throw new PaasException(
                    ErrorType.INTERNAL,
                    ErrorCode.CMSI_FAIL_TO_SEND_MSG,
                    new Object[]{
                        resp.getError().getCode(),
                        resp.getError().getMessage()
                    });
            }
        } catch (PaasException e) {
            throw e;
        } catch (Exception e) {
            String msg = MessageFormatter.format(
                "Fail to request {}",
                uri
            ).getMessage();
            log.error(msg, e);
            throw new PaasException(e, ErrorType.INTERNAL, ErrorCode.CMSI_API_ACCESS_ERROR, new Object[]{});
        } finally {
            HttpMetricUtil.clearHttpMetric();
        }
    }

    private List<ApiGwCmsiChannelResp> fetchAndGetChannelList(String tenantId) {
        try {
            HttpMetricUtil.setHttpMetricName(CommonMetricNames.ESB_CMSI_API_HTTP);
            HttpMetricUtil.addTagForCurrentMetric(
                Tag.of(EsbMetricTags.KEY_API_NAME, API_GET_NOTIFY_CHANNEL_LIST)
            );
            OpenApiResponse<List<ApiGwCmsiChannelResp>> resp = doRequest(
                OpenApiRequestInfo.builder()
                    .method(HttpMethodEnum.GET)
                    .uri(API_GET_NOTIFY_CHANNEL_LIST)
                    .addHeader(buildTenantHeader(tenantId))
                    .authorization(
                        buildAuthorization(
                            appProperties, virtualAdminAccountProvider.getVirtualAdminUsername(tenantId)))
                    .build(),
                new TypeReference<OpenApiResponse<List<ApiGwCmsiChannelResp>>>() {
                }
            );
            return resp.getData();
        } catch (Exception e) {
            String errorMsg = "Get " + API_GET_NOTIFY_CHANNEL_LIST + " error";
            log.error(errorMsg, e);
            throw new InternalCmsiException(errorMsg, e, ErrorCode.CMSI_MSG_CHANNEL_DATA_ERROR);
        } finally {
            HttpMetricUtil.clearHttpMetric();
        }
    }

    private SendMailV1Req buildSendMailReq(NotifyMessageDTO notifyMessageDTO) {
        return SendMailV1Req.fromNotifyMessageDTO(notifyMessageDTO);
    }

    private SendSmsV1Req buildSendSmsReq(NotifyMessageDTO notifyMessageDTO) {
        return SendSmsV1Req.fromNotifyMessageDTO(notifyMessageDTO);
    }

    private SendVoiceV1Req buildSendVoiceReq(NotifyMessageDTO notifyMessageDTO) {
        return SendVoiceV1Req.fromNotifyMessageDTO(notifyMessageDTO);
    }

    private SendWxV1Req buildSendWxReq(NotifyMessageDTO notifyMessageDTO) {
        return SendWxV1Req.fromNotifyMessageDTO(notifyMessageDTO);
    }
}
