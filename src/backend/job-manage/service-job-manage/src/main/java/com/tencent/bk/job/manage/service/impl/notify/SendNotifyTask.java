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

package com.tencent.bk.job.manage.service.impl.notify;

import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.dao.notify.EsbUserInfoDAO;
import com.tencent.bk.job.manage.model.dto.notify.EsbUserInfoDTO;
import com.tencent.bk.job.manage.service.PaaSService;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.util.List;
import java.util.Set;

@Builder
@Slf4j
public class SendNotifyTask implements Runnable {

    private PaaSService paaSService;
    private EsbUserInfoDAO esbUserInfoDAO;
    //结果队列
    private final String requestId;
    private final String msgType;
    private final String sender;
    private final Set<String> receivers;
    private final String title;
    private final String content;

    public SendNotifyTask(String requestId,
                          String msgType,
                          String sender,
                          Set<String> receivers,
                          String title,
                          String content) {
        this.requestId = requestId;
        this.msgType = msgType;
        this.sender = sender;
        this.receivers = receivers;
        this.title = title;
        this.content = content;
    }

    public void bindService(PaaSService paaSService,
                            EsbUserInfoDAO esbUserInfoDAO) {
        this.paaSService = paaSService;
        this.esbUserInfoDAO = esbUserInfoDAO;
    }

    @Override
    public void run() {
        if (receivers == null || receivers.isEmpty()) {
            log.warn("receiverList is null or empty, skip, msgType={},title={},content={}", msgType, title, content);
            return;
        }
        JobContextUtil.setRequestId(requestId);
        try {
            int count = 0;
            Boolean sendResult = false;
            while (!sendResult && count < 1) {
                count += 1;
                sendResult = paaSService.sendMsg(msgType, sender, receivers, title, content);
                if (sendResult == null) {
                    sendResult = false;
                }
            }
            if (sendResult) {
                log.info("Success to send notify:({},{},{})", String.join(",", receivers), msgType, title);
            } else {
                List<EsbUserInfoDTO> validUsers = esbUserInfoDAO.listEsbUserInfo(receivers, null);
                if (validUsers.isEmpty()) {
                    // 收信人已全部离职/某些渠道不支持平台用户
                    log.info("Ignore to send notify:({},{},{})", String.join(",", receivers), msgType, title);
                } else {
                    log.error(
                        "Fail to send notify:({},{},{},{})",
                        String.join(",", receivers),
                        msgType,
                        title,
                        content
                    );
                }
            }
        } catch (Exception e) {
            List<EsbUserInfoDTO> validUsers = esbUserInfoDAO.listEsbUserInfo(receivers, null);
            if (validUsers.isEmpty()) {
                // 收信人已全部离职/某些渠道不支持平台用户
                log.info("Ignore to send notify:({},{},{})", String.join(",", receivers), msgType, title);
            } else {
                FormattingTuple msg = MessageFormatter.format(
                    "Fail to send notify:({},{},{},{})",
                    new String[]{
                        String.join(",", receivers),
                        msgType,
                        title,
                        content
                    }
                );
                log.error(msg.getMessage(), e);
            }
        }
    }
}
