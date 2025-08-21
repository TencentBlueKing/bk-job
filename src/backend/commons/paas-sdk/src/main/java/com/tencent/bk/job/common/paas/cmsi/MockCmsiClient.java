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

import com.tencent.bk.job.common.paas.model.NotifyChannelDTO;
import com.tencent.bk.job.common.paas.model.NotifyMessageDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MockCmsiClient implements ICmsiClient {

    @Override
    public List<NotifyChannelDTO> getNotifyChannelList(String tenantId) {
        if ("system".equals(tenantId)) {
            List<NotifyChannelDTO> channelList = new ArrayList<>();
            channelList.add(new NotifyChannelDTO(
                "weixin",
                "微信",
                true,
                ""
            ));
            channelList.add(new NotifyChannelDTO(
                "rtx",
                "企业微信",
                true,
                ""
            ));
            return channelList;
        } else if ("putongoa".equals(tenantId)) {
            List<NotifyChannelDTO> channelList = new ArrayList<>();
            channelList.add(new NotifyChannelDTO(
                "weixin",
                "微信",
                true,
                ""
            ));
            channelList.add(new NotifyChannelDTO(
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
    }
}
