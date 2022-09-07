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

import com.tencent.bk.job.manage.dao.notify.EsbUserInfoDAO;
import com.tencent.bk.job.manage.model.dto.notify.EsbUserInfoDTO;
import com.tencent.bk.job.manage.service.impl.WatchableSendMsgService;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.util.List;
import java.util.Set;

@Builder
@Slf4j
public class SendNotifyTask implements Runnable {

    // 发送消息失败后的最大重试次数
    private final int NOTIFY_MAX_RETRY_COUNT = 1;

    private WatchableSendMsgService watchableSendMsgService;
    private EsbUserInfoDAO esbUserInfoDAO;

    private final Long appId;
    private final long createTimeMillis;
    private final String msgType;
    private final String sender;
    private final Set<String> receivers;
    private final String title;
    private final String content;

    public void bindService(WatchableSendMsgService watchableSendMsgService,
                            EsbUserInfoDAO esbUserInfoDAO) {
        this.watchableSendMsgService = watchableSendMsgService;
        this.esbUserInfoDAO = esbUserInfoDAO;
    }

    @Override
    public void run() {
        if (CollectionUtils.isEmpty(receivers)) {
            logInvalidReceivers();
            return;
        }
        try {
            boolean result = sendMsgWithRetry();
            if (result) {
                logSendSuccess();
            } else {
                handleSendFail(null);
            }
        } catch (Exception e) {
            handleSendFail(e);
        }
    }

    private void logInvalidReceivers() {
        log.warn("receivers is null or empty, skip, msgType={},title={}", msgType, title);
    }

    private boolean sendMsgWithRetry() {
        int count = 0;
        boolean result = false;
        while (!result && count < NOTIFY_MAX_RETRY_COUNT) {
            count += 1;
            try {
                watchableSendMsgService.sendMsg(
                    appId,
                    createTimeMillis,
                    msgType,
                    sender,
                    receivers,
                    title,
                    content
                );
                result = true;
            } catch (Exception e) {
                log.error("Fail to sendMsg", e);
            }
        }
        return result;
    }

    private void logSendSuccess() {
        log.info("Success to send notify:({},{},{})", String.join(",", receivers), msgType, title);
    }

    private void handleSendFail(Exception e) {
        List<EsbUserInfoDTO> validUsers = esbUserInfoDAO.listEsbUserInfo(receivers);
        if (validUsers.isEmpty()) {
            // 收信人已全部离职/某些渠道不支持平台用户
            logIgnoreToSend();
            return;
        }
        FormattingTuple msg = MessageFormatter.arrayFormat(
            "Fail to send notify:({},{},{},{})",
            new Object[]{
                String.join(",", receivers),
                msgType,
                title,
                content
            }
        );
        if (e != null) {
            log.error(msg.getMessage(), e);
        } else {
            log.error(msg.getMessage());
        }
    }

    private void logIgnoreToSend() {
        log.info("Ignore to send notify:({},{},{})", String.join(",", receivers), msgType, title);
    }

}
