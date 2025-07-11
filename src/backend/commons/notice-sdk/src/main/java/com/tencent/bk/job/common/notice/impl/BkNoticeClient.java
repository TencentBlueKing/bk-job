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

package com.tencent.bk.job.common.notice.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.HttpMethodEnum;
import com.tencent.bk.job.common.esb.config.AppProperties;
import com.tencent.bk.job.common.esb.config.BkApiGatewayProperties;
import com.tencent.bk.job.common.esb.model.BkApiAuthorization;
import com.tencent.bk.job.common.esb.model.EsbReq;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.OpenApiRequestInfo;
import com.tencent.bk.job.common.esb.sdk.BkApiClient;
import com.tencent.bk.job.common.exception.HttpStatusException;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.notice.IBkNoticeClient;
import com.tencent.bk.job.common.notice.exception.BkNoticeException;
import com.tencent.bk.job.common.notice.model.AnnouncementDTO;
import com.tencent.bk.job.common.notice.model.BkNoticeApp;
import com.tencent.bk.job.common.util.http.HttpHelperFactory;
import com.tencent.bk.job.common.util.http.HttpMetricUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

import java.util.List;

@SuppressWarnings("SameParameterValue")
public class BkNoticeClient extends BkApiClient implements IBkNoticeClient {

    private static final String URI_REGISTER_APPLICATION = "/apigw/v1/register/";
    private static final String URI_GET_CURRENT_ANNOUNCEMENTS = "/apigw/v1/announcement/get_current_announcements/";

    private final AppProperties appProperties;
    private final BkApiAuthorization authorization;

    public BkNoticeClient(MeterRegistry meterRegistry,
                          AppProperties appProperties,
                          BkApiGatewayProperties bkApiGatewayProperties) {
        super(
            meterRegistry,
            CommonMetricNames.BK_NOTICE_API,
            getBkNoticeUrlSafely(bkApiGatewayProperties),
            HttpHelperFactory.getDefaultHttpHelper()
        );
        this.appProperties = appProperties;
        authorization = BkApiAuthorization.appAuthorization(appProperties.getCode(), appProperties.getSecret());
    }

    private static String getBkNoticeUrlSafely(BkApiGatewayProperties bkApiGatewayProperties) {
        if (bkApiGatewayProperties == null || bkApiGatewayProperties.getBkNotice() == null) {
            return null;
        }
        return bkApiGatewayProperties.getBkNotice().getUrl();
    }

    @Override
    public BkNoticeApp registerApplication() {
        EsbResp<BkNoticeApp> resp = requestBkNoticeApi(
            HttpMethodEnum.POST,
            URI_REGISTER_APPLICATION,
            null,
            new TypeReference<EsbResp<BkNoticeApp>>() {
            },
            true
        );
        return resp.getData();
    }

    @Override
    public List<AnnouncementDTO> getCurrentAnnouncements(String bkLanguage, Integer offset, Integer limit) {
        EsbResp<List<AnnouncementDTO>> resp = requestBkNoticeApi(
            HttpMethodEnum.GET,
            buildUriWithParams(bkLanguage, offset, limit),
            null,
            new TypeReference<EsbResp<List<AnnouncementDTO>>>() {
            },
            true
        );
        return resp.getData();
    }

    private String buildUriWithParams(String bkLanguage, Integer offset, Integer limit) {
        StringBuilder sb = new StringBuilder();
        sb.append(URI_GET_CURRENT_ANNOUNCEMENTS);
        sb.append("?platform=");
        sb.append(appProperties.getCode());
        if (StringUtils.isNotBlank(bkLanguage)) {
            sb.append("&language=");
            sb.append(bkLanguage);
        }
        if (offset != null) {
            sb.append("&offset=");
            sb.append(offset);
        }
        if (limit != null) {
            sb.append("&limit=");
            sb.append(limit);
        }
        return sb.toString();
    }

    /**
     * 通过ESB请求消息通知中心API的统一入口，监控数据埋点位置
     *
     * @param method        Http方法
     * @param uri           请求地址
     * @param reqBody       请求体内容
     * @param typeReference 指定了返回值类型的EsbResp TypeReference对象
     * @param <R>           泛型：返回值类型
     * @return 返回值类型实例
     */
    private <R> EsbResp<R> requestBkNoticeApi(HttpMethodEnum method,
                                              String uri,
                                              EsbReq reqBody,
                                              TypeReference<EsbResp<R>> typeReference,
                                              Boolean idempotent) {
        try {
            HttpMetricUtil.setHttpMetricName(CommonMetricNames.BK_NOTICE_API_HTTP);
            HttpMetricUtil.addTagForCurrentMetric(Tag.of("api_name", uri));
            OpenApiRequestInfo<Object> requestInfo = OpenApiRequestInfo
                .builder()
                .method(method)
                .uri(uri)
                .body(reqBody)
                .authorization(authorization)
                .setIdempotent(idempotent)
                .build();
            return doRequest(requestInfo, typeReference);
        } catch (InternalException e) {
            // 接口不存在的场景需要使用指定错误码以便前端兼容处理
            if (e.getCause() instanceof HttpStatusException) {
                HttpStatusException httpStatusException = (HttpStatusException) e.getCause();
                if (httpStatusException.getHttpStatus() == HttpStatus.SC_NOT_FOUND) {
                    throw new BkNoticeException(e, ErrorCode.BK_NOTICE_API_NOT_FOUND, new String[]{uri});
                }
            }
            throw new BkNoticeException(e, ErrorCode.BK_NOTICE_API_DATA_ERROR, null);
        } catch (Exception e) {
            throw new BkNoticeException(e, ErrorCode.BK_NOTICE_API_DATA_ERROR, null);
        } finally {
            HttpMetricUtil.clearHttpMetric();
        }
    }
}
