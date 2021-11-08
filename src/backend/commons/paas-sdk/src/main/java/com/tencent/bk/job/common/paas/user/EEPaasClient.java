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
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.sdk.AbstractEsbSdkClient;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.model.dto.BkUserDTO;
import com.tencent.bk.job.common.paas.model.EsbListUsersResult;
import com.tencent.bk.job.common.paas.model.EsbNotifyChannelDTO;
import com.tencent.bk.job.common.paas.model.GetEsbNotifyChannelReq;
import com.tencent.bk.job.common.paas.model.GetUserListReq;
import com.tencent.bk.job.common.paas.model.PostSendMsgReq;
import com.tencent.bk.job.common.util.json.JsonUtils;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
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

    /**
     * 获取用户列表
     *
     * @param lookupField
     * @param exactLookups
     * @param fuzzyLookups
     * @param page
     * @param pageSize
     * @param uin
     * @return
     */
    @Override
    public List<BkUserDTO> getUserList(String fields, String lookupField,
                                       String exactLookups, String fuzzyLookups,
                                       long page, long pageSize, boolean noPage,
                                       String bkToken, String uin) {
        List<EsbListUsersResult> oriUsers;
        try {
            GetUserListReq req = makeBaseReqByWeb(GetUserListReq.class, null, uin, null);

            if (StringUtils.isNotBlank(fields)) {
                req.setFields(fields);
            }

            if (page > 0) {
                req.setPage(page);
            }
            if (pageSize > 0) {
                req.setPageSize(pageSize);
            }
            if (StringUtils.isNotBlank(lookupField)) {
                req.setLookupField(lookupField);
                if (StringUtils.isNotBlank(exactLookups)) {
                    req.setExactLookups(exactLookups);
                } else if (StringUtils.isNotBlank(fuzzyLookups)) {
                    req.setFuzzyLookups(fuzzyLookups);
                } else {
                    return null;
                }
            }
            req.setNoPage(noPage);

            String respStr = doHttpGet(API_GET_USER_LIST, req);
            if (StringUtils.isBlank(respStr)) {
                log.error("{}|response empty", API_GET_USER_LIST);
                return null;
            }
            EsbResp<List<EsbListUsersResult>> esbResp = JsonUtils.fromJson(respStr,
                new TypeReference<EsbResp<List<EsbListUsersResult>>>() {
                });
            if (esbResp == null || esbResp.getCode() != 0) {
                log.error("Get {} error, response is null or response code is not success", API_GET_USER_LIST);
                return null;
            }
            oriUsers = esbResp.getData();
        } catch (Exception e) {
            String errorMsg = "Get " + API_GET_USER_LIST + " error";
            log.error(errorMsg, e);
            throw new InternalException(e, ErrorCode.PAAS_API_DATA_ERROR, errorMsg);
        }
        if (oriUsers == null || oriUsers.isEmpty()) {
            return null;
        }
        List<BkUserDTO> users = new ArrayList<>();
        for (EsbListUsersResult oriUser : oriUsers) {
            BkUserDTO user = new BkUserDTO();
            user.setId(oriUser.getId());
            user.setUsername(oriUser.getUsername());
            user.setDisplayName(oriUser.getDisplayName());
            user.setLogo(oriUser.getLogo());
            user.setUid(oriUser.getUid());
            users.add(user);
        }
        return users;
    }

    @Override
    public List<EsbNotifyChannelDTO> getNotifyChannelList(String uin) {
        GetEsbNotifyChannelReq req = makeBaseReqByWeb(GetEsbNotifyChannelReq.class, null, uin, null);
        String respStr = null;
        try {
            respStr = doHttpGet(API_GET_NOTIFY_CHANNEL_LIST, req);
        } catch (Exception e) {
            log.error("Fail to get notify channel list:", e);
            return null;
        }
        if (StringUtils.isBlank(respStr)) {
            log.error("Get {} error, response is null", API_GET_NOTIFY_CHANNEL_LIST);
            return null;
        }
        EsbResp<List<EsbNotifyChannelDTO>> esbResp = JsonUtils.fromJson(respStr,
            new TypeReference<EsbResp<List<EsbNotifyChannelDTO>>>() {
            });
        if (esbResp == null || esbResp.getCode() != 0) {
            log.error("Get {} error, response is null or response code is not success", API_GET_NOTIFY_CHANNEL_LIST);
            return null;
        }
        return esbResp.getData();
    }

    @Override
    public Boolean sendMsg(
        String msgType,
        String sender,
        Set<String> receiverList,
        String title,
        String content
    ) throws Exception {
        PostSendMsgReq req = makeBaseReqByWeb(PostSendMsgReq.class, null, "admin", "superadmin");
        if (title == null || title.isEmpty()) {
            title = "Default Title";
        }
        req.setMsgType(msgType);
        req.setSender(sender);
        req.setReceiverUsername(String.join(",", receiverList));
        req.setTitle(title);
        req.setContent(content);
        String respStr;
        long start = System.nanoTime();
        String status = "none";
        String uri = API_POST_SEND_MSG;
        try {
            respStr = doHttpPost(uri, req);

            if (StringUtils.isBlank(respStr)) {
                log.error("Get {} error, response is null", API_POST_SEND_MSG);
                status = "fail";
                return false;
            }
            EsbResp<Object> esbResp = JsonUtils.fromJson(respStr,
                new TypeReference<EsbResp<Object>>() {
                });
            if (esbResp == null) {
                log.warn("{}|req={}|respStr={}|esbResp == null after json parse",
                    uri, JsonUtils.toJsonWithoutSkippedFields(req), respStr);
                status = "fail";
                return false;
            } else if (esbResp.getCode() != 0) {
                Integer code = esbResp.getCode();
                log.warn("{}|requestId={}|code={}|msg={}|esbResp.getCode() != 0",
                    uri, esbResp.getRequestId(), esbResp.getCode(), esbResp.getMessage());
                if (code.equals(ESB_CODE_RATE_LIMIT_RESTRICTION_BY_STAGE)
                    || code.equals(ESB_CODE_RATE_LIMIT_RESTRICTION_BY_RESOURCE)) {
                    status = "over_rate";
                } else {
                    status = "fail";
                }
                return false;
            }
            status = "success";
            return true;
        } catch (Exception e) {
            log.error("Fail to request {}", uri, e);
            status = "error";
            return false;
        } finally {
            long end = System.nanoTime();
            meterRegistry.timer("cmsi.api", "api_name", API_POST_SEND_MSG,
                "status", status, "msg_type", msgType)
                .record(end - start, TimeUnit.NANOSECONDS);
            String key = "today.msg." + msgType + "." + status;
            AtomicInteger valueWrapper = todayMsgStatisticsMap.computeIfAbsent(key,
                str -> new AtomicInteger(0));
            Integer value = valueWrapper.incrementAndGet();
            log.debug("statistics:{}->{}", key, value);
            meterRegistry.gauge(key, valueWrapper);
        }
    }
}
