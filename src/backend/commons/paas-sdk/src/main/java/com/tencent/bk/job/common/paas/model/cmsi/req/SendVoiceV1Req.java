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

package com.tencent.bk.job.common.paas.model.cmsi.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.paas.model.NotifyMessageDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Cmsi接口发送语音消息的请求
 */
@Getter
@Setter
public class SendVoiceV1Req extends CmsiSendMsgV1BasicReq {

    /**
     * 自动语音读字信息
     */
    @JsonProperty("auto_read_message")
    private String autoReadMessage;

    /**
     * 待通知的电话列表
     */
    @JsonProperty("receiver")
    private List<String> receiver;

    /**
     * 待通知的用户列表，包含用户名，用户需在蓝鲸平台注册，多个以逗号分隔，若 receiver、receiver__username 同时存在，以 receiver 为准
     */
    @JsonProperty("receiver__username")
    private List<String> receiverUsername;

    public static SendVoiceV1Req fromNotifyMessageDTO(NotifyMessageDTO notifyMessageDTO) {
        SendVoiceV1Req sendVoiceV1Req = new SendVoiceV1Req();
        sendVoiceV1Req.setReceiverUsername(notifyMessageDTO.getReceiverUsername());
        sendVoiceV1Req.setAutoReadMessage(notifyMessageDTO.getContent());
        return sendVoiceV1Req;
    }

}
