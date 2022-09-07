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

package com.tencent.bk.job.common.paas.user;


import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.metrics.EsbMetricTags;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.sdk.AbstractEsbSdkClient;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.dto.BkUserDTO;
import com.tencent.bk.job.common.model.error.ErrorType;
import com.tencent.bk.job.common.paas.exception.PaasException;
import com.tencent.bk.job.common.paas.metrics.PaaSMetricTags;
import com.tencent.bk.job.common.paas.model.EsbListUsersResult;
import com.tencent.bk.job.common.paas.model.EsbNotifyChannelDTO;
import com.tencent.bk.job.common.paas.model.GetEsbNotifyChannelReq;
import com.tencent.bk.job.common.paas.model.GetUserListReq;
import com.tencent.bk.job.common.paas.model.PostSendMsgReq;
import com.tencent.bk.job.common.util.http.HttpMetricUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 标准企业版对接paas api 客户端
 */
@Slf4j
public class EEPaasClient extends AbstractEsbSdkClient implements IPaasClient {

    private static final Integer ESB_CODE_RATE_LIMIT_RESTRICTION_BY_STAGE = 1642902;
    private static final Integer ESB_CODE_RATE_LIMIT_RESTRICTION_BY_RESOURCE = 1642903;

    private static final String API_GET_USER_LIST = "/api/c/compapi/v2/usermanage/list_users/";
    private static final String API_GET_NOTIFY_CHANNEL_LIST = "/api/c/compapi/cmsi/get_msg_type/";
    private static final String API_POST_SEND_MSG = "/api/c/compapi/cmsi/send_msg/";

    private static final HashMap<String, AtomicInteger> todayMsgStatisticsMap = new HashMap<>();

    private final MeterRegistry meterRegistry;

    public EEPaasClient(
        String esbHostUrl,
        String appCode,
        String appSecret,
        String lang,
        boolean useEsbTestEnv,
        MeterRegistry meterRegistry
    ) {
        super(esbHostUrl, appCode, appSecret, lang, useEsbTestEnv);
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void resetTodayStatistics() {
        todayMsgStatisticsMap.forEach((key, value) -> value.set(0));
    }

    @Override
    public List<BkUserDTO> getUserList(String fields,
                                       String bkToken,
                                       String uin) {
        List<EsbListUsersResult> esbUserList;
        try {
            GetUserListReq req = buildGetUserListReq(uin, fields);

            HttpMetricUtil.setHttpMetricName(CommonMetricNames.ESB_USER_MANAGE_API_HTTP);
            HttpMetricUtil.addTagForCurrentMetric(
                Tag.of(EsbMetricTags.KEY_API_NAME, API_GET_USER_LIST)
            );
            EsbResp<List<EsbListUsersResult>> esbResp = getEsbRespByReq(
                HttpGet.METHOD_NAME,
                API_GET_USER_LIST,
                req,
                new TypeReference<EsbResp<List<EsbListUsersResult>>>() {
                }
            );
            esbUserList = esbResp.getData();
        } catch (Exception e) {
            String errorMsg = "Get " + API_GET_USER_LIST + " error";
            log.error(errorMsg, e);
            throw new InternalException(errorMsg, e, ErrorCode.USER_MANAGE_API_ACCESS_ERROR);
        } finally {
            HttpMetricUtil.clearHttpMetric();
        }
        return convert(esbUserList);
    }

    private GetUserListReq buildGetUserListReq(String uin, String fields) {
        GetUserListReq req = makeBaseReqByWeb(GetUserListReq.class, null, uin, null);
        if (StringUtils.isNotBlank(fields)) {
            req.setFields(fields);
        }
        req.setPage(0L);
        req.setPageSize(0L);
        req.setNoPage(true);
        return req;
    }

    private List<BkUserDTO> convert(List<EsbListUsersResult> esbUserList) {
        if (CollectionUtils.isEmpty(esbUserList)) {
            return Collections.emptyList();
        }
        List<BkUserDTO> userList = new ArrayList<>();
        for (EsbListUsersResult esbUser : esbUserList) {
            BkUserDTO user = new BkUserDTO();
            user.setId(esbUser.getId());
            user.setUsername(esbUser.getUsername());
            user.setDisplayName(esbUser.getDisplayName());
            user.setLogo(esbUser.getLogo());
            user.setUid(esbUser.getUid());
            userList.add(user);
        }
        return userList;
    }

    @Override
    public List<EsbNotifyChannelDTO> getNotifyChannelList(String uin) {
        GetEsbNotifyChannelReq req = makeBaseReqByWeb(GetEsbNotifyChannelReq.class, null, uin, null);
        try {
            HttpMetricUtil.setHttpMetricName(CommonMetricNames.ESB_CMSI_API_HTTP);
            HttpMetricUtil.addTagForCurrentMetric(
                Tag.of(EsbMetricTags.KEY_API_NAME, API_GET_NOTIFY_CHANNEL_LIST)
            );
            EsbResp<List<EsbNotifyChannelDTO>> esbResp = getEsbRespByReq(
                HttpGet.METHOD_NAME,
                API_GET_NOTIFY_CHANNEL_LIST,
                req,
                new TypeReference<EsbResp<List<EsbNotifyChannelDTO>>>() {
                }
            );
            return esbResp.getData();
        } finally {
            HttpMetricUtil.clearHttpMetric();
        }
    }

    @Override
    public void sendMsg(
        String msgType,
        String sender,
        Set<String> receivers,
        String title,
        String content
    ) {
        PostSendMsgReq req = buildSendMsgReq(msgType, sender, receivers, title, content);
        long start = System.nanoTime();
        String status = EsbMetricTags.VALUE_STATUS_NONE;
        String uri = API_POST_SEND_MSG;
        try {
            HttpMetricUtil.setHttpMetricName(CommonMetricNames.ESB_CMSI_API_HTTP);
            HttpMetricUtil.addTagForCurrentMetric(Tag.of(EsbMetricTags.KEY_API_NAME, uri));
            EsbResp<Object> esbResp = getEsbRespByReq(
                HttpPost.METHOD_NAME,
                uri,
                req,
                new TypeReference<EsbResp<Object>>() {
                }
            );

            if (esbResp.getResult() == null || !esbResp.getResult() || esbResp.getCode() != 0) {
                status = checkRespAndGetStatus(uri, esbResp);
                throw new PaasException(
                    ErrorType.FAILED_PRECONDITION,
                    ErrorCode.CMSI_FAIL_TO_SEND_MSG,
                    new Object[]{
                        esbResp.getCode().toString(),
                        esbResp.getMessage()
                    });
            }
            status = EsbMetricTags.VALUE_STATUS_SUCCESS;
        } catch (PaasException e) {
            throw e;
        } catch (Exception e) {
            log.error("Fail to request {}", uri, e);
            status = EsbMetricTags.VALUE_STATUS_ERROR;
            throw new PaasException(e, ErrorType.FAILED_PRECONDITION, ErrorCode.CMSI_API_ACCESS_ERROR, new Object[]{});
        } finally {
            HttpMetricUtil.clearHttpMetric();
            recordMetrics(start, status, msgType);
        }
    }

    private String checkRespAndGetStatus(String uri, EsbResp<?> esbResp) {
        Integer code = esbResp.getCode();
        log.warn(
            "{}|requestId={}|result={}|code={}|msg={}|esbResp.getCode() != 0",
            uri,
            esbResp.getRequestId(),
            esbResp.getResult(),
            esbResp.getCode(),
            esbResp.getMessage()
        );
        if (code.equals(ESB_CODE_RATE_LIMIT_RESTRICTION_BY_STAGE)
            || code.equals(ESB_CODE_RATE_LIMIT_RESTRICTION_BY_RESOURCE)) {
            return EsbMetricTags.VALUE_STATUS_OVER_RATE;
        } else {
            return EsbMetricTags.VALUE_STATUS_FAIL;
        }
    }

    private PostSendMsgReq buildSendMsgReq(String msgType,
                                           String sender,
                                           Set<String> receivers,
                                           String title,
                                           String content) {
        PostSendMsgReq req = makeBaseReqByWeb(PostSendMsgReq.class, null, "admin", "superadmin");
        if (title == null || title.isEmpty()) {
            title = "Default Title";
        }
        req.setMsgType(msgType);
        req.setSender(sender);
        req.setReceiverUsername(String.join(",", receivers));
        req.setTitle(title);
        req.setContent(content);
        return req;
    }

    private void recordMetrics(long startTimeNanos, String status, String msgType) {
        long end = System.nanoTime();
        meterRegistry.timer(
            CommonMetricNames.ESB_CMSI_API,
            EsbMetricTags.KEY_API_NAME, API_POST_SEND_MSG,
            EsbMetricTags.KEY_STATUS, status,
            PaaSMetricTags.KEY_MSG_TYPE, msgType
        ).record(end - startTimeNanos, TimeUnit.NANOSECONDS);
        String key = "today.msg." + msgType + "." + status;
        AtomicInteger valueWrapper = todayMsgStatisticsMap.computeIfAbsent(key,
            str -> new AtomicInteger(0));
        Integer value = valueWrapper.incrementAndGet();
        log.debug("statistics:{}->{}", key, value);
        meterRegistry.gauge(key, valueWrapper);
    }
}
